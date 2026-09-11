/* Native Alpha 0.6 choice fixture. Never shipped in the production JAR. GPL-3.0-or-later. */
package table;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.ability.AbilityKey;
import forge.game.zone.ZoneType;
import java.io.*;

public final class ChoicesScenarioServer {
 public static void main(String[] args)throws Exception {
  TableServer.main(args);
  new Thread(()->{try{
   var in=new BufferedReader(new InputStreamReader(System.in));
   while(in.readLine()!=null){var room=TableServer.ROOMS.values().iterator().next();var game=room.hosted.getGame();
    game.getAction().invoke(()->{
     game.getTriggerHandler().setSuppressAllTriggers(true);
     try{
      for(var p:game.getPlayers())for(var zone:java.util.List.of(ZoneType.Hand,ZoneType.Battlefield))
       for(Card c:new CardCollection(p.getCardsIn(zone)))game.getAction().moveTo(ZoneType.Exile,c,null,AbilityKey.newMap());
      var host=game.getPlayers().get(0);
      for(String land:java.util.List.of("Plains","Island","Swamp","Mountain","Forest"))for(int i=0;i<5;i++)ScenarioServer.put(game,host,land,ZoneType.Battlefield);
      for(String spell:java.util.List.of("Opt","Preordain","Cultivate","Demonic Tutor","Boros Charm"))ScenarioServer.put(game,host,spell,ZoneType.Hand);
      ScenarioServer.put(game,game.getPlayers().get(1),"Rhystic Study",ZoneType.Battlefield);
     }finally{game.getTriggerHandler().setSuppressAllTriggers(false);}
     room.refresh();System.out.println("CHOICES FIXTURE READY");
    });
   }
  }catch(Exception e){e.printStackTrace();System.exit(2);}},"choice-fixture").start();
 }
}
