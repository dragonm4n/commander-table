"""Assemble the runnable distribution with corresponding source.
Run after both builds: python server/distribute.py FORGE_CHECKOUT OUTPUT_DIRECTORY
"""
import shutil, subprocess, sys, zipfile
from pathlib import Path
site=Path(__file__).resolve().parents[1]
forge=Path(sys.argv[1]).resolve();out=Path(sys.argv[2]).resolve();dest=out/'Commander-Table'
dest.mkdir(parents=True,exist_ok=True)
shutil.copy2(forge/'forge-web/target/forge-web-2.0.15-SNAPSHOT.jar',dest/'commander-table.jar')
shutil.copytree(forge/'forge-web/target/lib',dest/'lib',dirs_exist_ok=True)
shutil.copytree(forge/'forge-gui/res',dest/'forge/res',dirs_exist_ok=True)
shutil.copytree(site/'server/web',dest/'web',dirs_exist_ok=True)
shutil.copy2(site/'server/support/forge.profile.properties',dest/'forge/forge.profile.properties')
for name in ['iniciar-windows.bat','iniciar.sh','tunel-windows.bat','tunel.sh','LEIA-ME.md','VALIDACAO.md','LICENSE']:
 shutil.copy2(site/'server'/name,dest/name)
source=dest/'source';native=source/'forge';native.mkdir(parents=True,exist_ok=True)
for module in ['forge-core','forge-game','forge-ai','forge-gui']:
 shutil.copytree(forge/module/'src',native/module/'src',dirs_exist_ok=True)
 shutil.copy2(forge/module/'pom.xml',native/module/'pom.xml')
for name in ['pom.xml','checkstyle.xml','LICENSE','README.md','CONTRIBUTING.md']:
 shutil.copy2(forge/name,native/name)
shutil.copytree(site/'server',native/'forge-web',ignore=shutil.ignore_patterns('web','target','lib','__pycache__'),dirs_exist_ok=True)
shutil.copy2(site/'server/BUILD.md',source/'BUILD.md')
ui=source/'web-ui'
files=subprocess.check_output(['git','ls-files','-z'],cwd=site).decode().split('\0')
for name in files:
 if not name or name.startswith('server/'):continue
 p=site/name
 if p.is_file():
  d=ui/name;d.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,d)
# The standalone build emits to server/web and needs an existing parent only.
(ui/'server').mkdir(exist_ok=True)
archive=out/'Commander-Table-alpha-0.3.zip'
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in sorted(dest.rglob('*')):
  if p.is_file() and 'profile' not in p.relative_to(dest).parts:z.write(p,p.relative_to(out))
with zipfile.ZipFile(archive) as z:
 assert z.testzip() is None
 assert 'Commander-Table/commander-table.jar' in z.namelist()
 assert 'Commander-Table/web/index.html' in z.namelist()
print(archive)
print(f'{archive.stat().st_size/1024**2:.1f} MiB; {len(zipfile.ZipFile(archive).infolist())} files; CRC verified')
