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
 static void akromasWill() {
  board();check(!CommanderThreat.considerAkromasWill(ai),"Keep Akroma's Will on an empty board");
  Card c=put(ai,"Grizzly Bears");check(!CommanderThreat.considerAkromasWill(ai),"Keep Will during quiet main phase");
  var will=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Akroma's Will"),ai);
  var spell=will.getFirstSpellAbility();spell.setActivatingPlayer(ai);
  check(!SpellApiToAi.Converter.get(spell).canPlayWithSubs(ai,spell).willingToPlay(),"Native Charm AI respects the quiet-phase gate");
  game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS,ai);
  var combat=new forge.game.combat.Combat(ai);game.getPhaseHandler().setCombat(combat);
  check(!CommanderThreat.considerAkromasWill(ai),"Do not spend Will with no declared attackers");
  combat.addAttacker(c,weak);
  check(!CommanderThreat.considerAkromasWill(ai),"Save Will rather than doubling two damage at 40 life");
  weak.setLife(4,null);check(CommanderThreat.considerAkromasWill(ai),"Consider a small attacker when doubling is lethal");
  weak.setLife(40,null);var second=put(ai,"Grizzly Bears");combat.addAttacker(second,weak);
  check(CommanderThreat.considerAkromasWill(ai),"Consider Will for a meaningful attacking board");
  game.getPhaseHandler().devModeSet(PhaseType.MAIN2,ai);check(!CommanderThreat.considerAkromasWill(ai),"Keep Will after combat");
 }
 static void myriadLivingPlayers() {
  board();Card attacker=put(ai,"Grizzly Bears");
  var combat=new forge.game.combat.Combat(ai);game.getPhaseHandler().setCombat(combat);combat.addAttacker(attacker,weak);
  other.concede();
  var copy=forge.game.ability.AbilityFactory.getAbility("DB$ CopyPermanent | Defined$ Self | ForEach$ OppNonDefendingPlayer | TokenTapped$ True | TokenAttacking$ RememberedPlayer | CleanupForEach$ True",attacker);
  copy.setActivatingPlayer(ai);new forge.game.ability.effects.CopyPermanentEffect().resolve(copy);
  var tokens=ai.getCreaturesInPlay().stream().filter(Card::isToken).toList();
  check(tokens.size()==1,"Myriad copies only for the surviving non-defending opponent");
  check(combat.getDefenderByAttacker(tokens.get(0))==threat,"Myriad token attacks the living opponent");
 }
 static void manaAndCommander() {
  board();
  Card lathril=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Lathril, Blade of the Elves"),ai);lathril.setCommander(true);
  game.getAction().moveTo(ZoneType.Command,lathril,null,AbilityKey.newMap());
  put(ai,"Forest");
  Card forest=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Forest"),ai);
  Card swamp=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Swamp"),ai);
  check(CommanderThreat.neededLand(ai,List.of(forest,swamp))==swamp,"Fetch missing commander color instead of another Forest");
  var fetch=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Rampant Growth"),ai).getFirstSpellAbility();fetch.setActivatingPlayer(ai);
  check(forge.ai.ability.ChangeZoneAi.chooseCardToHiddenOriginChangeZone(ZoneType.Battlefield,List.of(ZoneType.Library),fetch,new forge.game.card.CardCollection(List.of(forest,swamp)),ai,ai)==swamp,"Native land tutor uses missing-color evaluation");
  var spell=lathril.getFirstSpellAbility();spell.setActivatingPlayer(ai);
  check(CommanderThreat.commanderPriority(spell)==3,"Bounded commander casting preference");
  var elf=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves"),ai).getFirstSpellAbility();elf.setActivatingPlayer(ai);
  check(CommanderThreat.commanderPriority(elf)==1,"Lathril's explicit Elf strategy gets a bounded preference");
  var unrelated=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears"),ai).getFirstSpellAbility();unrelated.setActivatingPlayer(ai);
  check(CommanderThreat.commanderPriority(unrelated)==0,"Unrelated creatures do not receive the strategy bonus");
  var bear=put(ai,"Grizzly Bears");int baseline=ComputerUtilCard.evaluateCreature(bear);bear.setCommander(true);
  check(ComputerUtilCard.evaluateCreature(bear)==baseline+35,"Protect commander value in native creature comparisons");
  put(ai,"Swamp");check(CommanderThreat.neededLand(ai,List.of(forest,swamp))==null,"Balanced colors retain native fallback");
  Card demand=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Counterspell"),ai);
  game.getAction().moveTo(ZoneType.Hand,demand,null,AbilityKey.newMap());put(ai,"Island");
  Card island=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Island"),ai);
  check(CommanderThreat.neededLand(ai,List.of(forest,island))==island,"Double-blue hand cost needs a second blue source");
  put(ai,"Island");check(CommanderThreat.neededLand(ai,List.of(forest,island))==null,"Do not overfetch already satisfied blue pips");
 }
 static void run() {
  myriadLivingPlayers();
  manaAndCommander();
  akromasWill();
  attackDiagnostics();
  blocking();
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
 static void attackDiagnostics(){
  for(int repetition=0;repetition<20;repetition++)for(int blockers=0;blockers<3;blockers++){
   board();var c=commander(ai,"Lathril, Blade of the Elves");weak.setLife(35,null);
   for(int j=0;j<blockers;j++)put(weak,"Baneslayer Angel");
   game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS,ai);
   var combat=new forge.game.combat.Combat(ai);new AiAttackController(ai).declareAttackers(combat);
   check(combat.isAttacking(c)==(blockers<2),"Lathril attacks through a single first-striker, respects two lethal blockers");
  }
  board();var c=commander(ai,"Lathril, Blade of the Elves");
  check(CommanderThreat.hasSelfCombatReward(c,false),"Recognize Lathril's combat-damage tokens");
  check(!CommanderThreat.hasSelfCombatReward(c,true),"Lathril requires damage, not just declaring an attack");
  var captain=commander(ai,"Captain Lannery Storm");
  check(CommanderThreat.hasSelfCombatReward(captain,true),"Recognize a self attack trigger creating Treasure");
  check(!CommanderThreat.hasSelfCombatReward(put(ai,"Grizzly Bears"),true),"Vanilla creature has no extra attack reward");
 }

 static Card token(){
  Card c=new Card(game.nextCardId(),game);c.setName("Soldier Token");c.setOwner(ai);
  c.setGamePieceType(forge.card.GamePieceType.TOKEN);c.addType("Creature");c.addType("Soldier");c.setBasePower(1);c.setBaseToughness(1);
  return game.getAction().moveTo(ZoneType.Battlefield,c,null,AbilityKey.newMap());
 }
 static forge.game.combat.Combat attack(Card attacker){
  game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS,threat);
  var combat=new forge.game.combat.Combat(threat);combat.addAttacker(attacker,ai);return combat;
 }
 static void blocking(){
  for(int i=0;i<20;i++){
   board();var big=put(threat,"Gigantosaurus");big.setBasePower(8);big.setBaseToughness(8);var small=token();
   var combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
   check(combat.getBlockers(big).contains(small),"Use a spare vanilla 1/1 token to stop an 8/8 at 40 life");
  }
  board();var big=put(threat,"Gigantosaurus");var small=token();big.addIntrinsicKeyword("Trample");
  var combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Do not spend a token for a single point against trample at 40 life");
  board();big=put(threat,"Gigantosaurus");small=token();big.addIntrinsicKeyword("Flying");
  combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Ground token cannot block a flyer");
  board();big=put(threat,"Gigantosaurus");small=token();big.addIntrinsicKeyword("Menace");
  combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Do not assign an illegal single blocker against menace");
  board();big=put(threat,"Grizzly Bears");small=token();
  combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Preserve the token against a small hit at 40 life");
  board();big=put(threat,"Gigantosaurus");small=token();small.setTapped(true);
  combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Tapped token cannot block");
  board();big=put(threat,"Gigantosaurus");small=put(ai,"Llanowar Elves");small.setGamePieceType(forge.card.GamePieceType.TOKEN);
  combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Preserve a token copy with a mana ability at 40 life");
  board();big=put(threat,"Thorn Elemental");small=token();
  combat=attack(big);new AiBlockController(ai,false).assignBlockersForCombat(combat);
  check(combat.getBlockers(big).isEmpty(),"Do not waste the token when damage can bypass blockers");
 }
}
