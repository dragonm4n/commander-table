/* Test-only outcome fixture. GPL-3.0-or-later. Never included in the production JAR. */
package table;
import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEventTurnBegan;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class SpectatorScenarioServer {
 public static void main(String[] args)throws Exception {
  TableServer.main(args);
  new Thread(()->{try{
   var input=new BufferedReader(new InputStreamReader(System.in));String line;
   while((line=input.readLine())!=null){
    var room=TableServer.ROOMS.values().stream().filter(r->r.status.equals("playing")).findFirst().orElseThrow();
    var game=room.hosted.getGame();final boolean finish=line.equals("finish");
    game.subscribeToEvents(new Object(){
     boolean done;
     @Subscribe public void turn(GameEventTurnBegan event){
      if(done)return;done=true;
      for(var player:game.getRegisteredPlayers())if(player.getId()==1||(finish&&player.getId()!=0))player.concede();
     }
    });
   }
  }catch(Exception e){e.printStackTrace();System.exit(2);}},"spectator-fixture").start();
 }
}
