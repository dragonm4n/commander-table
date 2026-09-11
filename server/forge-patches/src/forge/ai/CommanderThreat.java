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

    /** Compare colored pips in our hand/command zone with our own mana sources. */
    public static Card neededLand(Player ai, java.util.List<Card> candidates) {
        if (!applies(ai)) return null;
        java.util.Map<String,Integer> demand = new java.util.HashMap<>();
        var plans = forge.game.card.CardCollection.combine(ai.getCardsIn(forge.game.zone.ZoneType.Hand), ai.getCardsIn(forge.game.zone.ZoneType.Command));
        for (Card plan : plans) {
            if (plan.isLand()) continue;
            java.util.Map<String,Integer> pips = new java.util.HashMap<>();
            for (var shard : plan.getManaCost()) for (var color : shard.getColor()) {
                if (color == forge.card.MagicColor.Color.COLORLESS) continue;
                pips.merge(color.getBasicLandType(), 1, Integer::sum);
            }
            for (var entry : pips.entrySet()) {
                int sources = 0;
                for (Card available : ai.getCardsIn(forge.game.zone.ZoneType.Battlefield))
                    if (produces(available, entry.getKey())) sources++;
                for (Card available : ai.getCardsIn(forge.game.zone.ZoneType.Hand))
                    if (available.isLand() && produces(available, entry.getKey())) sources++;
                int missing = Math.max(0, entry.getValue() - sources);
                demand.merge(entry.getKey(), missing * (plan.isCommander() ? 6 : 3), Integer::sum);
            }
        }
        Card best = null; int score = 0;
        for (Card candidate : candidates) {
            int value = 0;
            for (var entry : demand.entrySet()) if (produces(candidate, entry.getKey())) value += entry.getValue();
            if (value > score) { best = candidate; score = value; }
        }
        return best; // Native balancing/dual-land logic handles ties with no unmet demand.
    }

    private static boolean produces(Card card, String basic) {
        if (card.getType().hasSubtype(basic)) return true;
        String symbol = switch(basic) {case "Plains" -> "W"; case "Island" -> "U"; case "Swamp" -> "B"; case "Mountain" -> "R"; case "Forest" -> "G"; default -> "C";};
        for (var mana : card.getManaAbilities()) if (mana.getManaPart() != null && mana.getManaPart().canProduce(symbol, mana)) return true;
        return false;
    }

    public static int commanderPriority(forge.game.spellability.SpellAbility sa) {
        Card c = sa.getHostCard();
        if (c == null || !applies(c.getController()) || !sa.isSpell()) return 0;
        // A bounded preference; native legality, emergency responses and cost checks remain in charge.
        if (c.isCommander()) return 3;
        // Use Forge's explicit deck-synergy metadata, rather than guessing from card names.
        if (c.getPaperCard() instanceof forge.item.PaperCard paper) {
            for (Card commander : forge.game.card.CardCollection.combine(
                    c.getController().getCardsIn(forge.game.zone.ZoneType.Command),
                    c.getController().getCardsIn(forge.game.zone.ZoneType.Battlefield))) {
                if (!commander.isCommander() || commander.getRules() == null) continue;
                var hints = commander.getRules().getAiHints().getDeckHints();
                if (hints != null && hints.filter(java.util.List.of(paper)).iterator().hasNext()) return 1;
            }
        }
        return 0;
    }

    /** Save this four-mana instant for an actual combat or a removal response.
     * This is a gate only: native mode, cost and threat evaluation still decides. */
    public static boolean considerAkromasWill(Player ai) {
        var game=ai.getGame();
        if(ai.getCreaturesInPlay().isEmpty())return false;
        if(!game.getStack().isEmpty())return true;
        var phase=game.getPhaseHandler().getPhase();
        if(phase!=forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS
                &&phase!=forge.game.phase.PhaseType.COMBAT_DECLARE_BLOCKERS)return false;
        var combat=game.getCombat();
        if(combat==null||game.getReplacementHandler().isPreventCombatDamageThisTurn())return false;
        int usefulPower=0;
        for(Card c:ai.getCreaturesInPlay()) {
            if(combat.isBlocking(c))return true; // Native evaluation checks whether the blocker needs saving.
            if(!combat.isAttacking(c)||c.getNetPower()<=0)continue;
            if(c.hasDoubleStrike()&&c.hasKeyword(forge.game.keyword.Keyword.FLYING))continue;
            usefulPower+=c.getNetPower();
            var defender=combat.getDefenderByAttacker(c);
            if(defender instanceof Player p) {
                int damage=ComputerUtilCombat.damageIfUnblocked(c,p,combat,true);
                if(!p.cantLoseForZeroOrLessLife()&&damage<p.getLife()&&damage*2>=p.getLife())return true;
                if(c.isCommander()&&p.getCommanderDamage(c)+damage<21&&p.getCommanderDamage(c)+damage*2>=21)return true;
            }
        }
        return usefulPower>=4;
    }

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
