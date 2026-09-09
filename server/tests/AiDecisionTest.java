/* Real Forge decision scenarios. Test-only, GPL-3.0-or-later. */
package table;

import forge.ai.*;
import forge.deck.Deck;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.*;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.localinstance.properties.ForgePreferences.FPref;
import java.util.*;

public final class AiDecisionTest {
 static Game game;
 static Player ai, weak, threat, other;
 static int checks;
 public static void main(String[] args) {
  try {
   System.setProperty("java.awt.headless", "true");
   GuiBase.setInterface(HeadlessPlatform.create(args[0]));
   FModel.initialize(null, p -> {p.setPref(FPref.DECKGEN_CARDBASED,false);return null;});
   run();
   System.out.println("PASS: " + checks + " real-engine AI decision checks");
   System.exit(0);
  } catch(Throwable error) { error.printStackTrace(); System.exit(1); }
 }
 static void board() {
  var rules=new GameRules(GameType.Commander);rules.addAppliedVariant(GameType.Commander);
  var players=new ArrayList<RegisteredPlayer>();
  for(int i=0;i<4;i++){
   var lobby=new LobbyPlayerAi("AI "+i,Set.of());lobby.setAiProfile("Default");
   var registered=new RegisteredPlayer(new Deck());registered.setPlayer(lobby);registered.setStartingLife(40);players.add(registered);
  }
  game=new Game(players,rules,new Match(rules,players,"AI decisions"));
  ai=game.getPlayers().get(0);weak=game.getPlayers().get(1);threat=game.getPlayers().get(2);other=game.getPlayers().get(3);
  game.getTriggerHandler().setSuppressAllTriggers(true);
  game.getPhaseHandler().devModeSet(PhaseType.MAIN1,ai);
  for(Player p:game.getPlayers())p.setLife(40,null);
 }
 static Card put(Player owner,String name) {
  Card c=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name),owner);
  c=game.getAction().moveTo(ZoneType.Battlefield,c,null,AbilityKey.newMap());c.setSickness(false);return c;
 }
 static Card commander(Player owner,String name) {Card c=put(owner,name);c.setCommander(true);return c;}
 static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);checks++;}
 static void choose(Player expected,String message) {
  for(int i=0;i<30;i++)check(AiAttackController.choosePreferredDefenderPlayer(ai,true)==expected,message);
 }
 static void run() {
  board();weak.setLife(1,null);
  for(int i=0;i<8;i++)put(threat,"Sol Ring");
  choose(threat,"Visible resource threat should outrank an empty board at one life");

  board();var c=commander(ai,"Rafiq of the Many");threat.addCommanderDamage(c,18);
  choose(threat,"Choose the opponent vulnerable to this commander's lethal damage");
  c.setTapped(true);
  check(CommanderThreat.attackBonus(ai,threat)==0,"Tapped commander cannot attack now");
  c.setTapped(false);c.setSickness(true);
  check(CommanderThreat.attackBonus(ai,threat)==0,"Sick commander cannot attack now");
  c.setSickness(false);put(threat,"Grizzly Bears");
  check(CommanderThreat.attackBonus(ai,threat)<900,"Legal blocker prevents an open lethal bonus");

  board();var first=commander(ai,"Rafiq of the Many");var second=commander(ai,"Isamaru, Hound of Konda");
  threat.addCommanderDamage(first,10);threat.addCommanderDamage(second,10);
  check(CommanderThreat.attackBonus(ai,threat)<900,"Commander damage must not be summed across partners");

  board();c=commander(threat,"Rafiq of the Many");ai.addCommanderDamage(c,18);
  check(CommanderThreat.dangerBonus(ai,threat)>=700,"Incoming commander lethal increases threat");
  choose(threat,"Defender ranking must recognize commander danger at high life");

  board();c=commander(ai,"Rafiq of the Many");threat.addCommanderDamage(c,18);put(threat,"Platinum Angel");
  check(CommanderThreat.attackBonus(ai,threat)==0,"Cannot-lose effect prevents a commander elimination bonus");

  board();c=commander(ai,"Rafiq of the Many");threat.addCommanderDamage(c,18);put(threat,"Propaganda");
  check(CommanderThreat.attackBonus(ai,threat)<900,"Attack tax is not a free lethal opportunity");

  board();c=commander(ai,"Rafiq of the Many");threat.addCommanderDamage(c,18);
  c.setOwner(other);
  check(CommanderThreat.attackBonus(ai,threat)>=900,"Stolen commander uses its controller and individual damage history");
 }
}
