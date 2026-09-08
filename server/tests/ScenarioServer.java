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
public final class ScenarioServer {
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
       Player host=game.getPlayers().get(0),other=game.getPlayers().get(2);
       for(int i=0;i<8;i++)put(game,host,"Plains",ZoneType.Battlefield);
       for(int i=0;i<8;i++)put(game,host,"Mountain",ZoneType.Battlefield);
       for(String name:new String[]{"The One Ring","Blade Splicer","Lightning Bolt","Lightning Bolt"})put(game,host,name,ZoneType.Hand);
       put(game,other,"Grizzly Bears",ZoneType.Battlefield);
       put(game,game.getPlayers().get(1),"Grizzly Bears",ZoneType.Battlefield);
       Card privateCard=put(game,other,"Opt",ZoneType.Hand);
       var hiddenTarget=room.seats[0].gui.target(privateCard.getView());
       if(!"zone".equals(hiddenTarget.get("kind"))||hiddenTarget.containsKey("id")||"Opt".equals(hiddenTarget.get("name")))throw new AssertionError("Private target identity leaked");
       System.out.println("PASS: private target projection omits card name and ID");
      }finally{game.getTriggerHandler().setSuppressAllTriggers(false);}
      // Entering without casting must NOT grant The One Ring's protection.
      put(game,game.getPlayers().get(3),"The One Ring",ZoneType.Battlefield);
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
