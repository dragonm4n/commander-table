"""Apply the Commander Table AI overlay to a copy of the pinned Forge sources.

Usage: python server/apply_forge_patches.py PATH_TO_FORGE
Checks every expected source fragment before writing; safe to run again.
"""
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
ai = root / 'forge-ai/src/main/java/forge/ai'
changes = {
    'AiAttackController.java': [(
        '''                // TODO commander damage
                int lifeDeficit = lowLifeThreshold - life;
                score += lifeDeficit * lifeDeficit;''',
        '''                int lifeDeficit = lowLifeThreshold - life;
                score += CommanderThreat.applies(ai) ? CommanderThreat.lowLifeBonus(opp)
                        : lifeDeficit * lifeDeficit;'''), (
        '''            if (forCombatDmg) {
                if (opp.isMonarch()''',
        '''            if (forCombatDmg) {
                score += CommanderThreat.attackBonus(ai, opp);
                if (opp.isMonarch()''')],
    'ComputerUtil.java': [(
        '''            // TODO: Consider whether the opponent is likely to attack a bigger threat instead.''',
        '''            rating += CommanderThreat.dangerBonus(ai, opponent);
            // TODO: Consider whether the opponent is likely to attack a bigger threat instead.''')]
}
pending = {}
for name, replacements in changes.items():
    path = ai / name
    source = path.read_text(encoding='utf-8')
    for old, new in replacements:
        if new in source:
            continue
        if source.count(old) != 1:
            raise SystemExit(f'Unsupported Forge source: {path}. Use the revision in FORGE_REVISION.')
        source = source.replace(old, new, 1)
    pending[path] = source
helper = Path(__file__).parent / 'forge-patches/src/forge/ai/CommanderThreat.java'
pending[ai / helper.name] = helper.read_text(encoding='utf-8')
for path, source in pending.items():
    path.write_text(source, encoding='utf-8', newline='\n')
print('Commander Table AI overlay applied:', root)
