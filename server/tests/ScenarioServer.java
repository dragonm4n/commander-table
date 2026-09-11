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
       put(game,other,"Sol Ring",ZoneType.Battlefield);put(game,other,"Arcane Signet",ZoneType.Battlefield);
       for(int i=0;i<8;i++)put(game,host,"Plains",ZoneType.Battlefield);
       for(int i=0;i<12;i++)put(game,host,"Mountain",ZoneType.Battlefield);
       for(String name:new String[]{"Angel of the Ruins","The One Ring","Blade Splicer","Lightning Bolt","Lightning Bolt"})put(game,host,name,ZoneType.Hand);
       put(game,other,"Grizzly Bears",ZoneType.Battlefield);
       put(game,game.getPlayers().get(1),"Grizzly Bears",ZoneType.Battlefield);
       Card privateCard=put(game,other,"Opt",ZoneType.Hand);
       var hiddenTarget=room.seats[0].gui.target(privateCard.getView());
       if(!"zone".equals(hiddenTarget.get("kind"))||hiddenTarget.containsKey("id")||"Opt".equals(hiddenTarget.get("name")))throw new AssertionError("Private target identity leaked");
       System.out.println("PASS: private target projection omits card name and ID");
       Card hideaway=put(game,host,"Mosswort Bridge",ZoneType.Battlefield);
       Card tucked=put(game,host,"Sol Ring",ZoneType.Exile);tucked.turnFaceDown(true);tucked.setExiledWith(hideaway);
       var publicExile=room.seats[1].gui.card(tucked.getView(),false);
       if(!Boolean.TRUE.equals(publicExile.get("hidden"))||publicExile.containsKey("text")||publicExile.containsKey("imageName")
          ||!publicExile.containsKey("exiledWith"))throw new AssertionError("Linked face-down exile was missing or leaked identity");
       System.out.println("PASS: linked face-down exile exposes its source without private identity");
       Card incidental=put(game,host,"Soul-Guide Lantern",ZoneType.Battlefield);
       Card banished=put(game,other,"Forest",ZoneType.Exile);banished.setExiledWith(incidental);
       if(room.seats[0].gui.card(banished.getView(),false).containsKey("exiledWith"))throw new AssertionError("Incidental exile incorrectly grouped beneath source");
       incidental.addUntilLeavesBattlefield(banished);
       if(!room.seats[0].gui.card(banished.getView(),false).containsKey("exiledWith"))throw new AssertionError("Return-on-leaving link missing");
       var observer=room.seats[0].gui;
       var search=new forge.game.event.GameEventTutorChoice(hideaway.getView(),other.getView(),java.util.List.of(privateCard.getView()),"Hand",false);
       observer.events.receive(search);room.seats[2].gui.events.receive(search);
       var publicAnnouncements=observer.events.announcements(2);
       if(publicAnnouncements.toString().contains("Opt")||!publicAnnouncements.toString().contains("hidden card"))throw new AssertionError("Unrevealed tutor leaked card");
       if(!room.seats[2].gui.events.announcements(2).toString().contains("Opt"))throw new AssertionError("Tutor owner cannot see own choice");
       observer.events.receive(new forge.game.event.GameEventTutorChoice(hideaway.getView(),other.getView(),java.util.List.of(privateCard.getView()),"Library (top)",true));
       if(!observer.events.announcements(2).toString().contains("Opt")||!observer.events.announcements(2).toString().contains("Library (top)"))throw new AssertionError("Revealed tutor choice/destination missing");
       System.out.println("PASS: incidental and return-on-leaving exile, private and revealed tutor projections");

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
