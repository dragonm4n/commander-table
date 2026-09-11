"""Apply the Commander Table AI overlay to a copy of the pinned Forge sources.

Usage: python server/apply_forge_patches.py PATH_TO_FORGE
Checks every expected source fragment before writing; safe to run again.
"""
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
ai = root / 'forge-ai/src/main/java/forge/ai'
changes = {
    'ability/CharmAi.java': [(
        '''        List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);''',
        '''        if (CommanderThreat.applies(ai) && "Akroma's Will".equals(source.getName())
                && !sa.isTrigger() && !CommanderThreat.considerAkromasWill(ai)) {
            sa.setChosenList(null);
            sa.setSubAbility(null);
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);''')],
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
changes['ability/ChangeZoneAi.java'] = [('''        final CardCollectionView combined = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));''', '''        Card needed = forge.ai.CommanderThreat.neededLand(ai, list);
        if (needed != null) return needed;
        final CardCollectionView combined = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));''')]
changes['ComputerUtilAbility.java'] = [('''            a1 += getSpellAbilityPriority(a);''', '''            a1 += CommanderThreat.commanderPriority(a);
            b1 += CommanderThreat.commanderPriority(b);
            a1 += getSpellAbilityPriority(a);''')]
changes['ComputerUtilCard.java'] = [('''        return creatureEvaluator.evaluateCreature(c);''', '''        return creatureEvaluator.evaluateCreature(c) + (c.isCommander() && CommanderThreat.applies(c.getController()) ? 35 : 0);'''), ('''        return creatureEvaluator.evaluateCreature(c, considerPT, considerCMC);''', '''        return creatureEvaluator.evaluateCreature(c, considerPT, considerCMC) + (c.isCommander() && CommanderThreat.applies(c.getController()) ? 35 : 0);''')]
native_changes = {
 'forge-game/src/main/java/forge/game/ability/effects/CopyPermanentEffect.java': [('''                    for (Player p : AbilityUtils.getDefinedPlayers(host, sa.getParam("ForEach"), sa)) {''', '''                    for (Player p : AbilityUtils.getDefinedPlayers(host, sa.getParam("ForEach"), sa)) {
                        if (p.hasLost() || !game.getPlayers().contains(p)) continue;''')],
 'forge-gui/src/main/java/forge/player/PlayerControllerHuman.java': [('''            prompt = "Cumulative upkeep for " + sa.getHostCard();
        }
        return PlaySpellAbility.payCostDuringAbilityResolve(this, player, cost, sa, prompt);''', '''            prompt = "Cumulative upkeep for " + sa.getHostCard();
        }
        if (!cost.isMandatory() && !getGui().confirm(sa.getHostCard().getView(),
                sa.getHostCard().getName() + " — Pay " + cost.toString() + "?\\n" + sa.getStackDescription(),
                false, List.of("Pay " + cost.toString(), "Don't pay"))) return false;
        return PlaySpellAbility.payCostDuringAbilityResolve(this, player, cost, sa, prompt);''')],
 'forge-game/src/main/java/forge/game/ability/effects/ChangeZoneEffect.java': [('''            if ((origin.contains(ZoneType.Library) && !ZoneType.Library.equals(destination) && !defined && shuffleMandatory)''', '''            if (origin.contains(ZoneType.Library) && !defined && !movedCards.isEmpty()) {
                boolean publicChoice = (((!ZoneType.Battlefield.equals(destination) && !changeType.isEmpty() && !changeType.equals("Card"))
                        || sa.hasParam("Reveal")) && !sa.hasParam("NoReveal"));
                // Report actual zones after replacement effects, using the resolved library position.
                var byZone = movedCards.stream().filter(c -> c != null && c.getZone() != null)
                    .collect(java.util.stream.Collectors.groupingBy(c -> c.getZone().getZoneType()));
                for (var entry : byZone.entrySet()) {
                    String place = entry.getKey().toString();
                    if (entry.getKey() == ZoneType.Library && destination == ZoneType.Library)
                        place += " (" + (libraryPos == 0 ? "top" : libraryPos == -1 ? "bottom" : "position " + (libraryPos + 1)) + ")";
                    game.fireEvent(new forge.game.event.GameEventTutorChoice(source.getView(), player.getView(),
                        entry.getValue().stream().map(Card::getView).toList(), place, publicChoice));
                }
            }
            if ((origin.contains(ZoneType.Library) && !ZoneType.Library.equals(destination) && !defined && shuffleMandatory)''')]
}
for name, replacements in native_changes.items():
    path = root / name
    source = path.read_text(encoding='utf-8')
    for old, new in replacements:
        if new in source: continue
        if source.count(old) != 1: raise SystemExit(f'Unsupported native source: {path}')
        source = source.replace(old, new, 1)
    pending[path] = source
event = root / 'forge-game/src/main/java/forge/game/event/GameEventTutorChoice.java'
pending[event] = '''/* Commander Table. GPL-3.0-or-later. */
package forge.game.event;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import java.util.List;
/** Identities are private unless revealed by the resolving effect. */
public record GameEventTutorChoice(CardView source, PlayerView player, List<CardView> cards,
        String destination, boolean revealed) implements GameEvent {
    @Override public <T> T visit(IGameEventVisitor<T> visitor) { return null; }
}
'''
hosted = root / 'forge-gui/src/main/java/forge/gamemodes/match/HostedMatch.java'
hosted_source = hosted.read_text(encoding='utf-8')
if 'setAutoContinue(boolean value)' not in hosted_source:
    marker = '    private Runnable endGameHook = null;'
    branch = '            if (humanCount == 0) {\n                // ... if no human players, let AI decide next game'
    if hosted_source.count(marker) != 1 or hosted_source.count(branch) != 1:
        raise SystemExit('Unsupported HostedMatch source for controlled AI series')
    hosted_source = hosted_source.replace(marker, marker + '\n    private boolean autoContinue = true;\n    public void setAutoContinue(boolean value) { autoContinue = value; }')
    hosted_source = hosted_source.replace(branch, branch.replace('humanCount == 0', 'humanCount == 0 && autoContinue'))
pending[hosted] = hosted_source
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
