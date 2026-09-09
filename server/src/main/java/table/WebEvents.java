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
 private long serial=0;
 WebEvents(WebGui gui){this.gui=gui;sounds=new EventVisualizer(gui.room.seats[gui.seat].registered.getPlayer());}
 synchronized Map<String,Object> lastAction(int seat){if(gui.room.aiOnly){var action=recent.get(seat);return action!=null&&System.currentTimeMillis()-(long)action.get("at")<5000?action:null;}Map<String,Object> result=null;for(var action:actions.values())if(Objects.equals(action.get("seat"),seat))result=action;return result;}
 synchronized List<Map<String,Object>> sounds(){return new ArrayList<>(audio);}
 @Subscribe public void receive(GameEvent event){
  boolean changed=false;
  if(event instanceof GameEventSpellAbilityCast cast){
   int seat=gui.room.seatFor(cast.si().getActivatingPlayer());
   if(seat>=0){CardView card=cast.si().getSourceCard();String kind=cast.sa().isSpell()?"cast":cast.si().isTrigger()?"trigger":"activate";
    synchronized(this){var action=obj("id",++serial,"at",System.currentTimeMillis(),"seat",seat,"stackId",cast.si().getId(),"kind",kind,"card",gui.card(card,false),"text",cast.si().getText());actions.put(cast.sa().getId(),action);recent.put(seat,action);}changed=true;
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
  if(gui.room.aiOnly){
   long delay=event instanceof GameEventSpellAbilityCast?3000:
       event instanceof GameEventLandPlayed||event instanceof GameEventSpellResolved?1000:
       event instanceof GameEventTurnPhase?400:0;
   if(delay>0){gui.refresh();long until=System.nanoTime()+delay*1_000_000;
    while(gui.room.status.equals("playing")&&System.nanoTime()<until){try{Thread.sleep(Math.min(100,Math.max(1,(until-System.nanoTime())/1_000_000)));}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}
   }
  }
 }
}
