/* GPL-3.0-or-later. One subscriber per table, including tables with several humans. */
package table;
import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.PlayerView;
import java.util.HashSet;

public final class TableProgress {
 private final TableServer.Room room;
 TableProgress(TableServer.Room room){
  this.room=room;
  // Forge runs the start hook after entering turn one, so seed that turn once.
  var phase=room.hosted.getGame().getPhaseHandler();
  track(phase.getTurn(),PlayerView.get(phase.getPlayerTurn()));
 }
 @Subscribe public void turn(GameEventTurnBegan event){
  track(event.turnNumber(),event.turnOwner());room.refresh();
 }
 private void track(int turn,PlayerView owner){
  var game=room.hosted.getGame();var alive=new HashSet<Integer>();
  for(var player:game.getPlayers())if(!player.hasLost())alive.add(room.seatFor(PlayerView.get(player)));
  var current=game.getPhaseHandler().getPlayerTurn();
  room.rounds.begin(turn,room.seatFor(owner),current!=null&&current.isExtraTurn(),alive);
 }
}
