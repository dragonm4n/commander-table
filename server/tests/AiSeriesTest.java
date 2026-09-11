package table;
import forge.deck.Deck;
import java.util.*;
public class AiSeriesTest {
 public static void main(String[] args){
  var seats=new TableServer.Seat[4];for(int i=0;i<4;i++)seats[i]=new TableServer.Seat("AI "+i,new Deck(i<2?"Same deck":"Deck "+i));
  var s=new AiSeries(3,true,seats);s.record(1,List.of(0),10,1000);s.record(1,List.of(0),10,1000);
  if(s.completed!=1)throw new AssertionError("Duplicate finish counted");
  s.record(2,List.of(),15,2000);s.record(3,List.of(1),12,3000);
  if(s.running||s.completed!=3||s.draws!=1)throw new AssertionError("Completion or draw totals");
  var decks=(List<Map<String,Object>>)s.data().get("decks");
  if(!decks.get(0).get("wins").equals(2)||!decks.get(0).get("games").equals(6))throw new AssertionError("Repeated deck aggregation");
  var stop=new AiSeries(10,false,seats);stop.stopRequested=true;stop.record(1,List.of(2),1,1);
  if(stop.running||stop.completed!=1)throw new AssertionError("Stop after current game must record it");
  var abort=new AiSeries(2,false,seats);abort.stop("Interrupted");abort.record(1,List.of(0),1,1);
  if(abort.completed!=0)throw new AssertionError("Interrupted result counted as a win");
  System.out.println("PASS: deduplication, draws, repeated decks, stop and interruption accounting");
 }
}
