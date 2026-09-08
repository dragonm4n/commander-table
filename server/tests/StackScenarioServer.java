/* Test-only fixture loader. Never included in the production JAR. GPL-3.0-or-later. */
package table;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Starts the unmodified HTTP entry point. stdin installs a deterministic board on Forge's game thread. */
public final class StackScenarioServer {
 public static void main(String[] args)throws Exception {
  TableServer.main(args);
  new Thread(()->{
   try {
    BufferedReader in=new BufferedReader(new InputStreamReader(System.in));String line;
    while((line=in.readLine())!=null)if(line.equals("fixture")){
     TableServer.Room room=TableServer.ROOMS.values().iterator().next();
     Game game=room.hosted.getGame();
     game.getAction().invoke(()->{
      game.getTriggerHandler().setSuppressAllTriggers(true);
      try {
       for(Player p:game.getPlayers()){
        for(Card c:new CardCollection(p.getCardsIn(ZoneType.Hand)))game.getAction().moveTo(ZoneType.Exile,c,null,AbilityKey.newMap());
        for(Card c:new CardCollection(p.getCardsIn(ZoneType.Battlefield)))game.getAction().moveTo(ZoneType.Exile,c,null,AbilityKey.newMap());
       }
       Player host=game.getPlayers().get(0),drain=game.getPlayers().get(1),counter=game.getPlayers().get(2);
       for(int i=0;i<10;i++)put(game,host,"Plains",ZoneType.Battlefield);
       for(int i=0;i<8;i++){put(game,drain,"Island",ZoneType.Battlefield);put(game,counter,"Island",ZoneType.Battlefield);}
       for(int i=0;i<2;i++){put(game,host,"Blade Splicer",ZoneType.Hand);put(game,drain,"Mana Drain",ZoneType.Hand);}
       put(game,counter,"Counterspell",ZoneType.Hand);
       put(game,host,"Jayemdae Tome",ZoneType.Battlefield);
       put(game,drain,"Manalith",ZoneType.Hand);
       Card worker=put(game,game.getPlayers().get(3),"Arcbound Worker",ZoneType.Battlefield);
       var counters=com.google.common.collect.HashMultiset.<forge.game.card.CounterType>create();
       counters.add(forge.game.card.CounterEnumType.P1P1,2);worker.setCounters(counters);
      }finally{game.getTriggerHandler().setSuppressAllTriggers(false);}
      room.refresh();System.out.println("FIXTURE READY");
     });
    }
   }catch(Exception e){e.printStackTrace();System.exit(2);}
  },"test-fixture-input").start();
 }
 static Card put(Game game,Player owner,String name,ZoneType zone){
  Card c=Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name),owner);
  return game.getAction().moveTo(zone,c,null,AbilityKey.newMap());
 }
}
