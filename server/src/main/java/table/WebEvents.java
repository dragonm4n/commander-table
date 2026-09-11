/* GPL-3.0-or-later. Browser notifications from public Forge events. */
package table;
import com.google.common.eventbus.Subscribe;
import forge.game.event.*;
import forge.game.card.CardView;
import forge.sound.EventVisualizer;
import forge.sound.SoundEffectType;
import java.nio.file.*;
import java.util.*;
import static table.TableServer.*;

public final class WebEvents {
 private final WebGui gui;
 private final EventVisualizer sounds;
 private final Map<Integer,Map<String,Object>> actions=new LinkedHashMap<>();
 private final Deque<Map<String,Object>> audio=new ArrayDeque<>();
 private final Map<Integer,Map<String,Object>> recent=new LinkedHashMap<>();
 private final Deque<Map<String,Object>> announcements=new ArrayDeque<>();
 private final Deque<Map<String,Object>> zoneMotions=new ArrayDeque<>();
 private long serial=0;
 WebEvents(WebGui gui){this.gui=gui;sounds=new EventVisualizer(gui.room.seats[gui.seat].registered.getPlayer());}
 synchronized Map<String,Object> lastAction(int seat){var action=recent.get(seat);return action!=null&&System.currentTimeMillis()-(long)action.get("at")<8000?action:null;}
 synchronized List<Map<String,Object>> sounds(){return new ArrayList<>(audio);}
 synchronized List<Map<String,Object>> zoneMotions(int seat){return zoneMotions.stream().filter(a->Objects.equals(a.get("seat"),seat)&&System.currentTimeMillis()-(long)a.get("at")<4000).toList();}
 synchronized List<Map<String,Object>> announcements(int seat){return announcements.stream().filter(a->Objects.equals(a.get("seat"),seat)&&System.currentTimeMillis()-(long)a.get("at")<15000).toList();}
 @Subscribe public void receive(GameEvent event){
  boolean changed=false;
  if(event instanceof GameEventCardChangeZone move && move.to()!=null && move.to().player()!=null){
   String zone=move.to().zoneType().name();
   if(Set.of("Hand","Graveyard","Exile").contains(zone)){
    synchronized(this){zoneMotions.addLast(obj("id",++serial,"at",System.currentTimeMillis(),"seat",gui.room.seatFor(move.to().player()),"zone",zone));while(zoneMotions.size()>128)zoneMotions.removeFirst();}changed=true;
   }
  }
  if(event instanceof GameEventTutorChoice choice){
   int seat=gui.room.seatFor(choice.player());
   if(seat>=0){
    List<String> names=new ArrayList<>();
    for(var chosen:choice.cards()){
     boolean publicZone=Set.of(forge.game.zone.ZoneType.Battlefield,forge.game.zone.ZoneType.Graveyard,forge.game.zone.ZoneType.Exile).contains(chosen.getZone())&&!chosen.isFaceDown();
     boolean visible=choice.revealed()||publicZone||(!gui.room.aiOnly&&choice.player().equals(gui.getCurrentPlayer()));
     names.add(visible?chosen.getName():"a hidden card");
    }
    synchronized(this){var action=obj("id",++serial,"at",System.currentTimeMillis(),"seat",seat,"kind","tutor","card",gui.card(choice.source(),false),"text","Chosen: "+String.join(", ",names)+" → "+choice.destination());announcements.addLast(action);while(announcements.size()>64)announcements.removeFirst();}changed=true;
   }
  }
  if(event instanceof GameEventSpellAbilityCast cast){
   int seat=gui.room.seatFor(cast.si().getActivatingPlayer());
   if(seat>=0){CardView card=cast.si().getSourceCard();String kind=cast.sa().isSpell()?"cast":cast.si().isTrigger()?"trigger":"activate";
    String description=cast.si().getText();
    if(gui.room.hosted!=null&&gui.room.hosted.getGame()!=null)for(var instance:gui.room.hosted.getGame().getStack())if(instance.getId()==cast.si().getId()){
     var modes=new ArrayList<String>();
     for(var part=instance.getSpellAbility().getSubAbility();part!=null;part=part.getSubAbility())
      if(part.hasSVar("CharmOrder"))modes.add(part.getDescription());
     if(!modes.isEmpty())description+="\nChosen modes: "+String.join(" • ",modes);
    }
    synchronized(this){var action=obj("id",++serial,"at",System.currentTimeMillis(),"seat",seat,"stackId",cast.si().getId(),"kind",kind,"card",gui.card(card,false),"text",description);actions.put(cast.sa().getId(),action);recent.put(seat,action);announcements.addLast(action);while(announcements.size()>64)announcements.removeFirst();}changed=true;
   }
  }
  if(event instanceof GameEventSpellResolved resolved){synchronized(this){changed|=actions.remove(resolved.spell().getId())!=null;}}
  if(event instanceof GameEventSpellRemovedFromStack removed){synchronized(this){changed|=actions.remove(removed.sa().getId())!=null;}}
  // Sound names carry no card identity. Do not forward scripted/custom filenames.
  SoundEffectType sound=event.visit(sounds);
  if(sound!=null&&sound!=SoundEffectType.ScriptedEffect){String name=sound.getResourceFileName();
   if(name.matches("[a-z_]+")&&Files.isRegularFile(Paths.get(assets,"res","sound",name+".mp3"))){
    synchronized(this){audio.addLast(obj("id",++serial,"name",name));while(audio.size()>80)audio.removeFirst();}changed=true;
   }
  }
  if(changed)gui.refresh();
  // Publish before waiting; never hold the snapshot/event locks while pacing.
  // Only the single spectator subscriber delays the engine, never human tables.
  if(gui.room.aiOnly&&!(gui.room.series!=null&&gui.room.series.fast)){
   long delay=event instanceof GameEventSpellAbilityCast?3000:
       event instanceof GameEventLandPlayed||event instanceof GameEventSpellResolved?1000:
       event instanceof GameEventTurnPhase?400:0;
   if(delay>0){gui.refresh();long until=System.nanoTime()+delay*1_000_000;
    while(gui.room.status.equals("playing")&&System.nanoTime()<until){try{Thread.sleep(Math.min(100,Math.max(1,(until-System.nanoTime())/1_000_000)));}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}
   }
  }
 }
}
