/* Test-only deterministic outcomes. Exercises real lifecycle, not deck strength. */
package table;
import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEventTurnBegan;
import forge.game.GameEndReason;
import java.util.*;
public final class SeriesScenarioServer {
 public static void main(String[] args)throws Exception{
  TableServer.main(args);
  new Thread(()->{Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());while(true){try{
   for(var room:TableServer.ROOMS.values())synchronized(room){
    if(!room.status.equals("playing")||room.hosted==null||room.series==null||room.series.total==20)continue;
    var game=room.hosted.getGame();if(game==null||!seen.add(game))continue;
    int number=room.series.completed;
    game.subscribeToEvents(new Object(){boolean done;
     @Subscribe public void turn(GameEventTurnBegan event){if(done)return;done=true;
      if(number==1){for(var p:game.getRegisteredPlayers())p.intentionalDraw();game.setGameOver(GameEndReason.Draw);}
      else for(var p:game.getRegisteredPlayers())if(room.seatFor(p.getView())!=(number==0?0:1))p.concede();
     }
    });
   }
   Thread.sleep(30);
  }catch(Throwable e){e.printStackTrace();System.exit(2);}}},"series-test-fixture").start();
 }
}
