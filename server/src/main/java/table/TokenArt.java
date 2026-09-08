/* GPL-3.0-or-later. Resolve token printing metadata using Forge's own edition index. */
package table;
import forge.StaticData;
import forge.ImageKeys;
import forge.card.CardEdition;
import forge.util.ImageUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class TokenArt {
 private static final Map<String,List<String>> CACHE=new ConcurrentHashMap<>();
 static List<String> paths(String imageKey){return CACHE.computeIfAbsent(imageKey==null?"":imageKey,TokenArt::resolve);}
 private static List<String> resolve(String key){
  if(!key.startsWith(ImageKeys.TOKEN_PREFIX))return List.of();
  String face=key.endsWith(ImageKeys.BACKFACE_POSTFIX)?"back":"";
  if(!face.isEmpty())key=key.substring(0,key.length()-ImageKeys.BACKFACE_POSTFIX.length());
  String[] parts=key.substring(2).split("\\|");if(parts.length<2)return List.of();
  CardEdition edition=StaticData.instance().getEditions().get(parts[1]);if(edition==null)return List.of();
  List<String> result=new ArrayList<>();
  if(parts.length>2)result.add(ImageUtil.getScryfallTokenDownloadUrl(parts[2],edition.getTokensCode(),edition.getCardsLangCode(),face));
  else for(var token:edition.getTokens().get(parts[0]))if(token.collectorNumber()!=null&&!token.collectorNumber().isEmpty())result.add(ImageUtil.getScryfallTokenDownloadUrl(token.collectorNumber(),edition.getTokensCode(),edition.getCardsLangCode(),face));
  return List.copyOf(result);
 }
}
