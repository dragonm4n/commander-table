package table;
import java.util.Set;
public final class RoundTrackerTest {
 static final Set<Integer> ALL=Set.of(0,1,2,3);
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  var r=new RoundTracker();
  for(int turn=1;turn<=9;turn++){r.begin(turn,(turn-1)%4,false,ALL);check(r.value()==(turn-1)/4+1,"Full rotation at "+turn);r.begin(turn,(turn-1)%4,false,ALL);check(r.value()==(turn-1)/4+1,"Duplicate subscriber");}
  var e=new RoundTracker();e.begin(1,2,false,ALL);e.begin(2,2,true,ALL);e.begin(3,3,true,ALL);e.begin(4,3,false,ALL);e.begin(5,0,false,ALL);e.begin(6,1,false,ALL);check(e.value()==1,"Extra turns counted as rounds");e.begin(7,1,true,ALL);check(e.value()==1,"Last player's extra turn ended round");e.begin(8,2,false,ALL);check(e.value()==2,"Rotated first player");
  var s=new RoundTracker();s.begin(1,0,false,ALL);s.begin(2,2,false,ALL);s.begin(3,3,false,ALL);s.begin(4,0,false,ALL);check(s.value()==2,"Skipped turn prevented rotation");
  var d=new RoundTracker();d.begin(1,0,false,ALL);d.begin(2,1,false,ALL);d.begin(3,2,false,Set.of(0,1,2));d.begin(4,0,false,Set.of(0,1,2));check(d.value()==2,"Eliminated player prevented rotation");
  System.out.println("PASS: rounds, different starting seats, duplicate events, extra turns, skipped turns and elimination");
 }
}
