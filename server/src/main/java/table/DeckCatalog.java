/* GPL-3.0-or-later. Original precons and explicit, auditable AI adaptations. */
package table;
import com.google.gson.*;
import forge.deck.*;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.model.FModel;
import java.nio.file.*;
import java.util.*;
import static table.TableServer.*;

public final class DeckCatalog {
 static final Map<String,Map<String,Object>> metadata=new LinkedHashMap<>();
 public static Map<String,Deck> load(Path assets)throws Exception{
  var catalog=assets.resolve("res/commander-table");
  var entries=JsonParser.parseString(Files.readString(catalog.resolve("catalog.json"))).getAsJsonArray();
  var decks=new LinkedHashMap<String,Deck>();metadata.clear();
  for(var value:entries){
   var entry=value.getAsJsonObject();String id=entry.get("id").getAsString();
   var file=(entry.get("bundled").getAsBoolean()?assets.resolve("res/quest/precons"):catalog.resolve("precons")).resolve(entry.get("file").getAsString());
   Deck original=DeckSerializer.fromFile(file.toFile());validate(original,id);
   decks.put(id,original);metadata.put(id,obj("kind","original","base",id,"source",entry.get("source").getAsString(),"changes",List.of()));
   String adaptedId=id+" — AI adapted";Deck adapted=new Deck(adaptedId);
   for(var section:original)for(var card:section.getValue())adapted.getOrCreate(section.getKey()).add(card.getKey(),card.getValue());
   var changes=new ArrayList<Object>();
   for(var replacement:entry.getAsJsonObject("replacements").entrySet()){
    String from=replacement.getKey(),to=replacement.getValue().getAsString();
    PaperCard old=null;int copies=0;
    for(var card:adapted.getMain())if(card.getKey().getName().equals(from)){old=card.getKey();copies=card.getValue();break;}
    require(old!=null,"Missing replacement source: "+id+" / "+from);
    require(old.getRules().getAiHints().getRemAIDecks(),"Replacement source is not AI-flagged: "+from);
    PaperCard next=FModel.getMagicDb().getCommonCards().getCard(to);
    require(next!=null&&!next.getRules().getAiHints().getRemAIDecks(),"Unavailable or AI-flagged replacement: "+to);
    adapted.getMain().remove(old,copies);adapted.getMain().add(next,copies);changes.add(obj("from",from,"to",to,"count",copies));
   }
   validate(adapted,adaptedId);
   decks.put(adaptedId,adapted);metadata.put(adaptedId,obj("kind","adapted","base",id,"source",entry.get("source").getAsString(),"changes",changes));
  }
  return decks;
 }
 private static void validate(Deck deck,String id){
  require(deck!=null&&!deck.getCommanders().isEmpty(),"Invalid deck: "+id);
  String issue=GameType.Commander.getDeckFormat().getDeckConformanceProblem(deck);
  require(issue==null,"Invalid Commander deck "+id+": "+issue);
 }
 public static Map<String,Object> coverage(Deck deck){
  int total=0,flagged=0;var warnings=new ArrayList<Object>();
  for(var section:List.of(DeckSection.Main,DeckSection.Commander)){
   var pool=deck.get(section);if(pool==null)continue;
   for(var entry:pool){total+=entry.getValue();if(entry.getKey().getRules().getAiHints().getRemAIDecks()){
    flagged+=entry.getValue();warnings.add(obj("name",entry.getKey().getName(),"count",entry.getValue(),"commander",section==DeckSection.Commander));
   }}
  }
  return obj("percent",total==0?0:Math.round(1000.0*(total-flagged)/total)/10.0,"total",total,"flagged",flagged,"warnings",warnings);
 }
 public static Map<String,Object> describe(String id,Deck deck){
  var result=obj("id",id,"name",id,"commander",String.join(" / ",deck.getCommanders().stream().map(PaperCard::getName).toList()),"ai",coverage(deck));
  result.putAll(metadata.getOrDefault(id,obj("kind","custom")));return result;
 }
}
