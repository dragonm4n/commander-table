"""Build a fresh runnable preview using an alpha 0.4 package's sources and libraries.

Requires JDK 17+ and an already built server/web. No Maven download is required.
Usage: python server/build_local.py EXISTING_PACKAGE NEW_OUTPUT_DIRECTORY
"""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

site = Path(__file__).resolve().parents[1]
package, output = (Path(arg).resolve() for arg in sys.argv[1:3])
if output.exists():
    raise SystemExit('Choose a new output directory; existing installations are never overwritten.')
if not (site / 'server/web/index.html').is_file():
    raise SystemExit('Build the web interface first: npm run build')
if not (package / 'source/forge/forge-ai/src/main/java/forge/ai/AiAttackController.java').is_file():
    raise SystemExit('The package must include the corresponding Forge sources.')
output.mkdir(parents=True)
native = output / 'source/forge'
print('Copying corresponding sources and runtime resources...', flush=True)
shutil.copytree(package / 'source/forge', native)
subprocess.run([sys.executable, str(site / 'server/apply_forge_patches.py'), str(native)], check=True)
shutil.copytree(package / 'lib', output / 'lib')
shutil.copytree(package / 'forge', output / 'forge')
shutil.copytree(site / 'server/catalog', output / 'forge/res/commander-table')
shutil.copytree(site / 'server/web', output / 'web')
for name in ['start-windows.bat', 'start.sh', 'tunnel-windows.bat', 'tunnel.sh', 'LICENSE', 'README.md', 'VALIDATION.md']:
    shutil.copy2(site / 'server' / name, output / name)
shutil.copy2(site / 'server/support/forge.profile.properties', output / 'forge/forge.profile.properties')
shutil.copy2(site / 'RELEASE-NOTES.md', output / 'RELEASE-NOTES.md')
shutil.copytree(site / 'server', native / 'forge-web', dirs_exist_ok=True,
                ignore=shutil.ignore_patterns('target', 'web', 'lib', '__pycache__'))
# Include tracked AND new authored UI files; exclude generated and ignored work.
names = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=site).decode().split('\0')
for name in names:
    if not name or name.startswith('server/') or not (site / name).is_file():
        continue
    dest = output / 'source/web-ui' / name
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(site / name, dest)
shutil.copy2(site / 'server/BUILD.md', output / 'source/BUILD.md')
build_root = site / 'server/target'
build_root.mkdir(parents=True, exist_ok=True)
build = Path(tempfile.mkdtemp(prefix='local-preview-', dir=build_root))
ai_classes, bridge_classes = build / 'ai', build / 'bridge'
ai_classes.mkdir(parents=True, exist_ok=True)
bridge_classes.mkdir(parents=True, exist_ok=True)
ai = native / 'forge-ai/src/main/java/forge/ai'
classpath = str(output / 'lib/*')
print('Compiling the AI overlay and Java bridge...', flush=True)
subprocess.run(['javac', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(ai_classes),
                *[str(ai / name) for name in ['AiAttackController.java', 'AiBlockController.java', 'ComputerUtil.java', 'CommanderThreat.java']]], check=True)
subprocess.run(['jar', '--update', '--file', str(output / 'lib/forge-ai-2.0.15-SNAPSHOT.jar'), '-C', str(ai_classes), '.'], check=True)
subprocess.run(['javac', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(bridge_classes),
                *map(str, (site / 'server/src/main/java/table').glob('*.java'))], check=True)
manifest = build / 'MANIFEST.MF'
# Manifest continuation lines must start with a space (the JAR parser has a line limit).
classpath_field = 'Class-Path: ' + ' '.join('lib/' + p.name for p in sorted((output / 'lib').glob('*.jar')))
wrapped = '\n '.join(classpath_field[i:i+70] for i in range(0, len(classpath_field), 70))
manifest.write_text('Manifest-Version: 1.0\nMain-Class: table.TableServer\n' + wrapped + '\n\n', encoding='utf-8')
subprocess.run(['jar', '--create', '--file', str(output / 'commander-table.jar'), '--manifest', str(manifest), '-C', str(bridge_classes), '.'], check=True)
print('Runnable preview:', output, flush=True)
