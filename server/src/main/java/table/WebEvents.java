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
 private final Map<Integer,Map<String,Object>> actions=new HashMap<>();
 private final Deque<Map<String,Object>> audio=new ArrayDeque<>();
 private long serial=0;
 WebEvents(WebGui gui){this.gui=gui;sounds=new EventVisualizer(gui.room.seats[gui.seat].registered.getPlayer());}
 synchronized Map<String,Object> lastAction(int seat){return actions.get(seat);}
 synchronized List<Map<String,Object>> sounds(){return new ArrayList<>(audio);}
 @Subscribe public void receive(GameEvent event){
  boolean changed=false;
  if(event instanceof GameEventSpellAbilityCast cast){
   int seat=gui.room.seatFor(cast.si().getActivatingPlayer());
   if(seat>=0){CardView card=cast.si().getSourceCard();String kind=cast.sa().isSpell()?"cast":cast.si().isTrigger()?"trigger":"activate";
    synchronized(this){actions.put(seat,obj("id",++serial,"kind",kind,"card",gui.card(card,false),"text",cast.si().getText()));}changed=true;
   }
  }
  // Sound names carry no card identity. Do not forward scripted/custom filenames.
  SoundEffectType sound=event.visit(sounds);
  if(sound!=null&&sound!=SoundEffectType.ScriptedEffect){String name=sound.getResourceFileName();
   if(name.matches("[a-z_]+")&&Files.isRegularFile(Paths.get(assets,"res","sound",name+".mp3"))){
    synchronized(this){audio.addLast(obj("id",++serial,"name",name));while(audio.size()>80)audio.removeFirst();}changed=true;
   }
  }
  if(changed)gui.refresh();
 }
}
