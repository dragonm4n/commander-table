/* GPL-3.0-or-later. Table rounds are separate from Forge's individual turns. */
package table;
import java.util.*;

final class RoundTracker {
 private int round=0,lastTurn=0;
 private final Set<Integer> visited=new HashSet<>(),remaining=new HashSet<>();
 synchronized int value(){return round;}
 synchronized void begin(int turn,int seat,boolean extra,Set<Integer> alive){
  if(turn<=lastTurn||seat<0)return;
  lastTurn=turn;
  if(round==0){round=1;remaining.addAll(alive);}
  remaining.retainAll(alive);
  if(extra)return;
  // Revisiting a normal-turn owner also closes a round containing skipped turns.
  if(remaining.isEmpty()||visited.contains(seat)){round++;visited.clear();remaining.addAll(alive);}
  visited.add(seat);remaining.remove(seat);
 }
}
