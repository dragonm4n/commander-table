"""Apply the Commander Table AI overlay to a copy of the pinned Forge sources.

Usage: python server/apply_forge_patches.py PATH_TO_FORGE
Checks every expected source fragment before writing; safe to run again.
"""
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
ai = root / 'forge-ai/src/main/java/forge/ai'
changes = {
    'AiBlockController.java': [(
        '''        // block requirements
''',
        '''        // Preserve life against large hits with otherwise unused, simple tokens.
        if (CommanderThreat.applies(ai) && !ComputerUtil.hasAFogEffect(ai, ai, checkingOther)) {
            for (Card attacker : attackersLeft) {
                if (combat.getDefenderByAttacker(attacker) != ai || !combat.getBlockers(attacker).isEmpty()
                        || attacker.hasKeyword(Keyword.TRAMPLE)
                        || StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(attacker)
                        || !CombatUtil.canAttackerBeBlockedWithAmount(attacker, 1, combat)
                        || ComputerUtilCombat.damageIfUnblocked(attacker, ai, combat, true) < 6
                        || ai.cantLoseForZeroOrLessLife()) continue;
                for (Card blocker : new ArrayList<>(blockersLeft)) {
                    if (!blocker.isToken() || blocker.isCommander() || blocker.getNetPower() > 1
                            || blocker.getNetToughness() > 1 || !blocker.getKeywords().isEmpty()
                            || blocker.getSpellAbilities().stream().anyMatch(ability -> !ability.isSpell()) || !blocker.getStaticAbilities().isEmpty()
                            || !blocker.getTriggers().isEmpty()
                            || !getPossibleBlockers(combat, attacker, blockersLeft, true).contains(blocker)
                            || CombatUtil.getBlockCost(ai.getGame(), blocker, attacker) != null) continue;
                    combat.addBlocker(attacker, blocker);
                    blockersLeft.remove(blocker);
                    break;
                }
            }
        }

        // block requirements
''')],
    'AiAttackController.java': [(
        '''if (aiAggression < 5 && !attacker.hasFirstStrike() && !attacker.hasDoubleStrike()
                        && ComputerUtilCombat.getTotalFirstStrikeBlockPower''',
        '''if (aiAggression < 5 && !attacker.hasFirstStrike() && !attacker.hasDoubleStrike()
                        && CombatUtil.canBeBlocked(attacker, this.blockers, combat)
                        && ComputerUtilCombat.getTotalFirstStrikeBlockPower'''), (
        '''hasAttackEffect = attacker.getSVar("HasAttackEffect").equals("TRUE") || attacker.hasKeyword(Keyword.ANNIHILATOR);''',
        '''hasAttackEffect = attacker.getSVar("HasAttackEffect").equals("TRUE") || attacker.hasKeyword(Keyword.ANNIHILATOR)
                    || CommanderThreat.hasSelfCombatReward(attacker, true);'''), (
        '''hasCombatEffect = attacker.getSVar("HasCombatEffect").equals("TRUE") || "Blocked".equals(attacker.getSVar("HasAttackEffect"))''',
        '''hasCombatEffect = attacker.getSVar("HasCombatEffect").equals("TRUE")
                    || CommanderThreat.hasSelfCombatReward(attacker, false)
                    || "Blocked".equals(attacker.getSVar("HasAttackEffect"))'''), (
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
