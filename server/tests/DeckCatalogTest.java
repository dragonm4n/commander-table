/* Real card database/catalog validation. GPL-3.0-or-later. */
package table;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.localinstance.properties.ForgePreferences.FPref;
import java.nio.file.*;
import java.util.*;
public final class DeckCatalogTest {
 public static void main(String[] args)throws Exception{
  try{
   System.setProperty("java.awt.headless","true");GuiBase.setInterface(HeadlessPlatform.create(args[0]));
   FModel.initialize(null,p->{p.setPref(FPref.DECKGEN_CARDBASED,false);return null;});
   var decks=DeckCatalog.load(Path.of(args[1]));if(decks.size()!=24)throw new AssertionError("Expected 12 originals and 12 adaptations");
   var report=new ArrayList<Object>();
   for(var entry:decks.entrySet()){
    var coverage=DeckCatalog.coverage(entry.getValue());
    if(!coverage.get("total").equals(100))throw new AssertionError(entry.getKey()+" is not 100 cards");
    if(entry.getKey().endsWith(" — AI adapted")&&!coverage.get("flagged").equals(0))throw new AssertionError("Still flagged: "+entry.getKey());
    report.add(DeckCatalog.describe(entry.getKey(),entry.getValue()));
    System.out.println("PASS "+entry.getKey()+" "+coverage.get("percent")+"% unflagged");
   }
   Files.writeString(Path.of(args[2]),TableServer.JSON.toJson(report));System.exit(0);
  }catch(Throwable error){error.printStackTrace();System.exit(1);}
 }
}
