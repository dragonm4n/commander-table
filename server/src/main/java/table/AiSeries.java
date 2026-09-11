/* GPL-3.0-or-later. Results for a bounded series of real four-AI games. */
package table;
import java.util.*;
import static table.TableServer.*;

final class AiSeries {
 final String id=key();final int total;final boolean fast;
 final List<Map<String,Object>> players=new ArrayList<>(),results=new ArrayList<>();
 final Set<Integer> recorded=new HashSet<>();
 boolean running=true,stopRequested=false;int completed=0,draws=0;String stopped="";
 AiSeries(int total,boolean fast,Seat[] seats){
  this.total=total;this.fast=fast;
  for(int i=0;i<seats.length;i++)players.add(obj("seat",i,"name",seats[i].aiName,"deck",seats[i].deck.getName(),"wins",0,"games",0));
 }
 void record(int gameId,List<Integer> winners,int turns,long millis){
  if(!running||!recorded.add(gameId))return;
  completed++;if(winners.isEmpty())draws++;
  for(var row:players){row.put("games",(int)row.get("games")+1);if(winners.contains(row.get("seat")))row.put("wins",(int)row.get("wins")+1);}
  results.add(obj("game",completed,"winners",new ArrayList<>(winners),"draw",winners.isEmpty(),"turns",turns,"seconds",millis/1000));
  if(completed>=total||stopRequested)running=false;
 }
 void stop(String reason){running=false;stopped=reason;}
 Map<String,Object> data(){
  Map<String,Map<String,Object>> decks=new LinkedHashMap<>();
  for(var player:players){String deck=(String)player.get("deck");var row=decks.computeIfAbsent(deck,k->obj("deck",k,"wins",0,"games",0));row.put("wins",(int)row.get("wins")+(int)player.get("wins"));row.put("games",(int)row.get("games")+(int)player.get("games"));}
  return obj("id",id,"total",total,"completed",completed,"draws",draws,"running",running,"stopRequested",stopRequested,"fast",fast,"stopped",stopped,"players",players.stream().map(LinkedHashMap::new).toList(),"decks",new ArrayList<>(decks.values()),"results",new ArrayList<>(results));
 }
}
