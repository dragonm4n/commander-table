/* Test-only fixture entry point. Never shipped in the production JAR. GPL-3.0-or-later. */
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
public final class TableScenarioServer {
 public static void main(String[] args)throws Exception {
  TableServer.main(args);
  new Thread(()->{try{
   var input=new BufferedReader(new InputStreamReader(System.in));String line;
   while((line=input.readLine())!=null){
    var room=TableServer.ROOMS.values().iterator().next();var game=room.hosted.getGame();String command=line;
    game.getAction().invoke(()->{
     if(!command.equals("fixture"))return;
     game.getTriggerHandler().setSuppressAllTriggers(true);
     try{
      for(Player p:game.getPlayers())for(var zone:new ZoneType[]{ZoneType.Hand,ZoneType.Battlefield})for(Card c:new CardCollection(p.getCardsIn(zone)))game.getAction().moveTo(ZoneType.Exile,c,null,AbilityKey.newMap());
      var host=game.getPlayers().get(0);var other=game.getPlayers().get(1);
      for(int i=0;i<8;i++){put(game,host,"Forest",ZoneType.Battlefield);put(game,host,"Plains",ZoneType.Battlefield);}
      put(game,host,"Island",ZoneType.Battlefield);
      var bear=put(game,host,"Grizzly Bears",ZoneType.Battlefield);bear.setSickness(false);
      put(game,host,"Bonesplitter",ZoneType.Battlefield);
      var aura=put(game,other,"Pacifism",ZoneType.Battlefield);aura.attachToEntity(bear,null);
      for(String name:new String[]{"Rancor","Rampant Growth","Raise the Alarm","Unsummon"})put(game,host,name,ZoneType.Hand);
     }finally{game.getTriggerHandler().setSuppressAllTriggers(false);}
     room.refresh();System.out.println("FIXTURE READY");
    });
   }
  }catch(Exception e){e.printStackTrace();System.exit(2);}},"table-fixture").start();
 }
 static Card put(Game game,Player owner,String name,ZoneType zone){return game.getAction().moveTo(zone,Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name),owner),null,AbilityKey.newMap());}
}
