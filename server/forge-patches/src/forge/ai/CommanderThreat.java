/* Commander Table AI adjustments. GPL-3.0-or-later. */
package forge.ai;

import forge.game.GameType;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

/** Public-board Commander heuristics; never inspect an opponent's hidden cards. */
public final class CommanderThreat {
    private CommanderThreat() { }

    public static boolean applies(Player ai) {
        return ai.getGame().getRules().hasAppliedVariant(GameType.Commander);
    }

    public static int lowLifeBonus(Player opponent) {
        if (opponent.cantLoseForZeroOrLessLife()) {
            return 0;
        }
        // Low life is an opportunity, not evidence that this player is the main threat.
        int deficit = Math.max(0, Math.min(20, opponent.getStartingLife()) - opponent.getLife());
        return Math.min(100, deficit * 5);
    }

    public static int attackBonus(Player ai, Player defender) {
        return commanderPressure(ai, defender, false);
    }

    public static int dangerBonus(Player ai, Player opponent) {
        return commanderPressure(opponent, ai, true);
    }

    /** Recognize direct, self-triggered token/draw rewards without requiring manual SVars. */
    public static boolean hasSelfCombatReward(Card card, boolean onAttack) {
        if (!applies(card.getController())) return false;
        for (var trigger : card.getTriggers()) {
            if (onAttack) {
                if (trigger.getMode() != forge.game.trigger.TriggerType.Attacks
                        || !"Card.Self".equals(trigger.getParam("ValidCard"))) continue;
            } else if (trigger.getMode() != forge.game.trigger.TriggerType.DamageDone
                    || !"True".equals(trigger.getParam("CombatDamage"))
                    || !"Card.Self".equals(trigger.getParam("ValidSource"))
                    || !"Player".equals(trigger.getParam("ValidTarget"))) continue;
            var ability = trigger.ensureAbility();
            if (ability == null || ability.usesTargeting()) continue;
            if (ability.getApi() == forge.game.ability.ApiType.Token
                    && (!ability.hasParam("TokenOwner") || "You".equals(ability.getParam("TokenOwner")))) return true;
            if (ability.getApi() == forge.game.ability.ApiType.Draw
                    && "You".equals(ability.getParam("Defined"))) return true;
        }
        return false;
    }

    private static int commanderPressure(Player attacker, Player defender, boolean nextTurn) {
        if (!applies(attacker) || defender.cantLoseCheck(GameLossReason.CommanderDamage)) {
            return 0;
        }
        int best = 0;
        // Use controller, not owner: a stolen commander still deals commander damage.
        for (Card card : attacker.getCreaturesInPlay()) {
            if (!card.isCommander() || !(nextTurn
                    ? CombatUtil.canAttackNextTurn(card, defender) : CombatUtil.canAttack(card, defender))) {
                continue;
            }
            int damage = ComputerUtilCombat.damageIfUnblocked(card, defender, null, true);
            if (damage <= 0) {
                continue;
            }
            int previous = defender.getCommanderDamage(card);
            int pressure = Math.min(20, previous) * 5;
            // Do not claim a lethal opportunity through a legal blocker or an attack tax.
            // Trample, coordinated attacks and future untaps are left to native combat logic.
            boolean blockable = CombatUtil.canBeBlocked(card, null, defender)
                    && defender.getCreaturesInPlay().stream().anyMatch(blocker ->
                            CombatUtil.canBlock(blocker, nextTurn) && CombatUtil.canBlock(card, blocker));
            if (previous + damage >= 21 && !blockable
                    && CombatUtil.getAttackCost(attacker.getGame(), card, defender) == null) {
                pressure += nextTurn ? 700 : 900;
            }
            // Damage from partners must never be added together.
            best = Math.max(best, pressure);
        }
        return best;
    }
}
