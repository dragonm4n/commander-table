/* Commander Table. Copyright 2026. GPL-3.0-or-later. */
package table;
import com.google.gson.*;
import com.sun.net.httpserver.*;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.game.*;
import forge.game.player.*;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.deck.*;
import forge.deck.io.DeckSerializer;
import forge.ai.LobbyPlayerAi;
import forge.player.LobbyPlayerHuman;
import forge.gamemodes.match.HostedMatch;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

public final class TableServer {
 public static final Gson JSON=new GsonBuilder().disableHtmlEscaping().create();
 static final SecureRandom RANDOM=new SecureRandom();
 static final Map<String,Room> ROOMS=new ConcurrentHashMap<>();
 static final Map<String,Deck> DECKS=new LinkedHashMap<>();
 static String adminKey,assets; static volatile boolean ready=false;
 public static String key(){byte[] b=new byte[24];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 public static Map<String,Object> obj(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
 public static String str(JsonObject o,String k,String fallback){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():fallback;}
 public static int integer(JsonObject o,String k,int fallback){return o.has(k)?o.get(k).getAsInt():fallback;}
 static void require(boolean b,String message){if(!b)throw new IllegalArgumentException(message);}
 public static void main(String[] args)throws Exception{
  Map<String,String> flags=new HashMap<>();for(int i=0;i+1<args.length;i+=2)flags.put(args[i],args[i+1]);
  assets=Paths.get(flags.getOrDefault("--assets","forge")).toAbsolutePath().toString();
  int port=Integer.parseInt(flags.getOrDefault("--port","8787"));
  adminKey=System.getenv("TABLE_ADMIN_KEY");if(adminKey==null||adminKey.length()<20)adminKey=key();
  System.setProperty("java.awt.headless","true");
  Thread.setDefaultUncaughtExceptionHandler((t,e)->{System.err.println("Erro em "+t.getName()+": "+e);e.printStackTrace();for(Room r:ROOMS.values())if(r.status.equals("playing")||r.status.equals("starting")){r.status="error";r.error="O motor interrompeu esta partida: "+e.getClass().getSimpleName()+". Consulte o log do servidor.";}});
  GuiBase.setInterface(HeadlessPlatform.create(assets));
  System.out.println("Carregando cartas e regras do Forge...");
  FModel.initialize(null,p->{p.setPref(FPref.UI_SELECT_FROM_CARD_DISPLAYS,false);p.setPref(FPref.PLAYER_NAME,"Commander Table");p.setPref(FPref.UI_ENABLE_SOUNDS,false);p.setPref(FPref.UI_ENABLE_MUSIC,false);p.setPref(FPref.DECKGEN_CARDBASED,false);p.setPref(FPref.UI_ENABLE_AI_CHEATS,false);p.setPref(FPref.MATCH_AI_TIMEOUT,"10");p.setPref(FPref.UI_LANGUAGE,"pt-BR");return null;});
  Path precons=Paths.get(assets,"res","quest","precons");
  for(String name:List.of("Feline Ferocity","Plunder the Graves","Draconic Domination","Vampiric Bloodlust","Swell the Host","Seize Control")){
   Path p=precons.resolve(name+".dck");if(Files.exists(p)){Deck d=DeckSerializer.fromFile(p.toFile());if(d!=null&&!d.getCommanders().isEmpty())DECKS.put(name,d);}
  }
  require(!DECKS.isEmpty(),"Nenhum deck de Commander foi carregado.");
  HttpServer server=HttpServer.create(new InetSocketAddress(flags.getOrDefault("--bind","127.0.0.1"),port),0);
  server.setExecutor(Executors.newFixedThreadPool(16));server.createContext("/",TableServer::handle);server.start();ready=true;
  System.out.println("\nCOMMANDER TABLE — servidor pronto");
  System.out.println("Local: http://localhost:"+port);
  System.out.println("Chave do anfitrião: "+adminKey);
  System.out.println("Deixe esta janela aberta durante a partida. Compartilhe o convite da sala, não a chave do anfitrião.");
 }
 static void handle(HttpExchange x)throws java.io.IOException{
  Headers h=x.getResponseHeaders();h.set("Access-Control-Allow-Origin","*");h.set("Access-Control-Allow-Methods","GET,POST,OPTIONS");h.set("Access-Control-Allow-Headers","Content-Type,Authorization");h.set("Access-Control-Allow-Private-Network","true");h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");
  try{
   if(x.getRequestMethod().equals("OPTIONS")){x.sendResponseHeaders(204,-1);return;}
   String path=x.getRequestURI().getPath();
   if(!path.startsWith("/api/")){
    Path root=Paths.get(assets).getParent().resolve("web").toAbsolutePath().normalize();
    Path file=root.resolve(path.equals("/")?"index.html":path.substring(1)).normalize();
    require(file.startsWith(root)&&Files.isRegularFile(file),"Arquivo não encontrado.");
    String ext=file.getFileName().toString();String mime=ext.endsWith(".html")?"text/html":ext.endsWith(".js")?"text/javascript":ext.endsWith(".css")?"text/css":ext.endsWith(".mp3")?"audio/mpeg":ext.endsWith(".svg")?"image/svg+xml":ext.endsWith(".png")?"image/png":"application/octet-stream";
    h.set("Content-Type",mime+"; charset=utf-8");h.set("Referrer-Policy","no-referrer");h.set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' https://*.scryfall.io https://*.scryfall.com data:; connect-src 'self' https: http://localhost:* http://127.0.0.1:*; font-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'");
    x.sendResponseHeaders(200,Files.size(file));Files.copy(file,x.getResponseBody());return;
   }
   String method=x.getRequestMethod();String bearer=Optional.ofNullable(x.getRequestHeaders().getFirst("Authorization")).orElse("").replaceFirst("^Bearer ","");
   byte[] bytes=x.getRequestBody().readNBytes(120001);require(bytes.length<=120000,"Solicitação muito grande.");
   JsonObject body=bytes.length==0?new JsonObject():JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
   if(path.equals("/api/health")&&method.equals("GET")){send(x,200,obj("ready",ready,"engine","Forge","version","alpha-0.3","revision","53a103721d627ecb76a2ea52b2febe894844f288","decks",DECKS.entrySet().stream().map(e->obj("id",e.getKey(),"name",e.getKey(),"commander",e.getValue().getCommanders().get(0).getName())).toList()));return;}
   if(path.equals("/api/rooms")&&method.equals("POST")){
    if(!secure(adminKey,bearer)){send(x,403,obj("error","Chave do anfitrião inválida."));return;}
    require(ROOMS.values().stream().filter(r->!r.status.equals("finished")&&!r.status.equals("error")).count()<1,"Este servidor já tem uma sala ativa. Encerre-a antes de criar outra.");
    Room r=new Room(str(body,"name","Mesa de Commander"),str(body,"player","Anfitrião"));ROOMS.put(r.id,r);send(x,201,r.session(0));return;
   }
   String[] p=path.split("/");require(p.length>=4&&p[1].equals("api")&&p[2].equals("rooms"),"Caminho desconhecido.");Room r=ROOMS.get(p[3]);require(r!=null,"Sala não encontrada. O servidor pode ter sido reiniciado.");
   String op=p.length>4?p[4]:"state";
   if(op.equals("join")&&method.equals("POST")){require(secure(r.invite,str(body,"invite","")),"Convite inválido.");synchronized(r){require(r.status.equals("lobby"),"A partida já começou. Entre antes de iniciar uma nova partida.");int seat=-1;for(int i=1;i<4;i++)if(r.seats[i].token==null){seat=i;break;}require(seat>=0,"A mesa já tem quatro jogadores.");r.seats[seat].name=clean(str(body,"player","Convidado"),30);r.seats[seat].token=key();r.seats[seat].seen=System.currentTimeMillis();send(x,200,r.session(seat));}return;}
   int seat=-1;for(int i=0;i<4;i++)if(secure(r.seats[i].token,bearer)){seat=i;break;}if(seat<0){send(x,403,obj("error","A sessão não pertence a esta sala. Use o convite para entrar."));return;}
   Seat player=r.seats[seat];player.seen=System.currentTimeMillis();
   if(op.equals("state")&&method.equals("GET")){send(x,200,r.state(seat));return;}
   require(method.equals("POST"),"Método inválido.");
   if(op.equals("leave")){synchronized(r){require(r.status.equals("lobby"),"Só é possível liberar um assento antes da partida.");if(seat==0)r.status="finished";else {player.token=null;player.name="IA "+(seat+1);player.ready=false;}}send(x,200,obj("ok",true));return;}
   if(op.equals("deck")){synchronized(r){require(r.status.equals("lobby"),"O deck só pode mudar antes da partida.");int target=integer(body,"seat",seat);require(target>=0&&target<4&&(target==seat||(seat==0&&r.seats[target].token==null)),"Você não controla esse assento.");Deck d;if(body.has("list")){d=importDeck(str(body,"list",""),str(body,"commander",""));}else {String name=str(body,"deck","");require(DECKS.containsKey(name),"Deck não encontrado.");d=DECKS.get(name);}r.seats[target].deck=d;r.seats[target].ready=true;}send(x,200,r.state(seat));return;}
   if(op.equals("start")){require(seat==0,"Somente o anfitrião inicia a partida.");synchronized(r){require(r.status.equals("lobby"),"A partida já foi iniciada.");for(Seat s:r.seats)require(s.token==null||s.ready,"Todos os jogadores precisam confirmar o deck.");r.status="starting";new Thread(r::start,"start-"+r.id).start();}send(x,200,r.state(seat));return;}
   if(op.equals("chat")){String msg=clean(str(body,"message",""),500);require(!msg.isBlank(),"Mensagem vazia.");synchronized(r.chat){r.chat.add(obj("name",player.name,"message",msg,"at",System.currentTimeMillis()));if(r.chat.size()>100)r.chat.remove(0);}send(x,200,obj("ok",true));return;}
   if(op.equals("action")){
    require(player.gui!=null&&r.status.equals("playing"),"A partida ainda não está em andamento.");
    String actionId=str(body,"requestId","");require(actionId.matches("[a-zA-Z0-9_-]{8,80}"),"Identificador da ação inválido.");
    synchronized(player){if(player.processed.contains(actionId)){send(x,200,obj("ok",true,"duplicate",true));return;}require(System.currentTimeMillis()-player.lastAction>70,"Aguarde antes da próxima ação.");player.lastAction=System.currentTimeMillis();player.processed.add(actionId);if(player.processed.size()>500)player.processed.remove(player.processed.iterator().next());}
    if(str(body,"action","").equals("answer")){player.gui.answer(body);send(x,200,obj("ok",true));return;}
    WebGui gui=player.gui;require(gui.prompt==null,"Responda à escolha pendente primeiro.");require(integer(body,"controlVersion",-1)==gui.controlVersion,"A mesa mudou. Aguarde a atualização e tente novamente.");
    CompletableFuture<Void> done=new CompletableFuture<>();SwingUtilities.invokeLater(()->{try{gui.action(body);done.complete(null);}catch(Throwable e){gui.showErrorDialog(e.getMessage()==null?"Ação inválida.":e.getMessage(),"Ação");done.completeExceptionally(e);}});try{done.get(150,TimeUnit.MILLISECONDS);}catch(TimeoutException pending){send(x,202,obj("ok",true,"queued",true));return;}send(x,200,obj("ok",true));return;
   }
   throw new IllegalArgumentException("Operação desconhecida.");
  }catch(Exception e){Throwable cause=e instanceof ExecutionException&&e.getCause()!=null?e.getCause():e;send(x,cause instanceof IllegalArgumentException?400:500,obj("error",cause.getMessage()==null?"Não foi possível concluir a operação.":cause.getMessage()));}finally{x.close();}
 }
 static boolean secure(String a,String b){return a!=null&&b!=null&&java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
 static String clean(String s,int max){return s.replaceAll("[\\p{Cntrl}]", " ").strip().substring(0,Math.min(s.replaceAll("[\\p{Cntrl}]", " ").strip().length(),max));}
 static void send(HttpExchange x,int status,Object body)throws java.io.IOException{byte[] bytes=JSON.toJson(body).getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.sendResponseHeaders(status,bytes.length);x.getResponseBody().write(bytes);}
 static Deck importDeck(String text,String commanders){
  require(text.length()<30000,"Lista muito grande.");require(!commanders.isBlank(),"Informe o comandante; se houver parceiro, use uma linha por comandante.");
  Deck deck=new Deck("Meu deck");List<String> missing=new ArrayList<>();
  for(String c:commanders.split("\\n")){String n=c.strip().replaceFirst("^1[xX]?\\s+","");PaperCard pc=FModel.getMagicDb().getCommonCards().getCard(n);if(pc==null)missing.add(n);else deck.getOrCreate(DeckSection.Commander).add(pc,1);}
  for(String line:text.split("\\n")){String l=line.strip();if(l.isBlank()||l.startsWith("//")||l.startsWith("["))continue;java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d+)[xX]?\\s+(.+)$").matcher(l);require(m.matches(),"Formato esperado: 1 Nome da carta. Linha: "+l);int count=Integer.parseInt(m.group(1));require(count>0&&count<=100,"Quantidade inválida.");String n=m.group(2).replaceFirst("\\s+\\([A-Za-z0-9]+\\).*$","").split("\\|")[0].strip();PaperCard pc=FModel.getMagicDb().getCommonCards().getCard(n);if(pc==null)missing.add(n);else if(!deck.getCommanders().stream().anyMatch(c->c.getName().equals(pc.getName())))deck.getMain().add(pc,count);}
  require(missing.isEmpty(),"Cartas não implementadas/encontradas no Forge: "+String.join(", ",missing));
  String issue=GameType.Commander.getDeckFormat().getDeckConformanceProblem(deck);require(issue==null,issue==null?"":issue);return deck;
 }
 public static final class Seat {String name;String token;Deck deck;boolean ready;long seen,lastAction;RegisteredPlayer registered;WebGui gui;Set<String> processed=new LinkedHashSet<>();Seat(String n,Deck d){name=n;deck=d;}}
 public static final class Room {
  final String id=key().substring(0,10),invite=key(),name;final Seat[] seats=new Seat[4];volatile String status="lobby",error="";HostedMatch hosted;final List<Map<String,Object>> chat=new ArrayList<>();
  Room(String n,String player){name=clean(n,60);List<Deck> decks=new ArrayList<>(DECKS.values());for(int i=0;i<4;i++)seats[i]=new Seat(i==0?clean(player,30):"IA "+(i+1),decks.get(i%decks.size()));seats[0].token=key();seats[0].seen=System.currentTimeMillis();}
  Map<String,Object> session(int i){return obj("room",id,"seat",i,"token",seats[i].token,"invite",i==0?invite:null);}
  Map<String,Object> state(int viewer){Map<String,Object> r=obj("id",id,"name",name,"status",status,"error",error,"mySeat",viewer,"seats",Arrays.stream(seats).map(s->obj("name",s.name,"human",s.token!=null,"ready",s.ready,"connected",s.token!=null&&System.currentTimeMillis()-s.seen<25000,"deck",s.deck.getName(),"commander",s.deck.getCommanders().get(0).getName())).toList());WebGui gui=seats[viewer].gui;if(gui!=null)r.put("game",gui.snapshot);synchronized(chat){r.put("chat",new ArrayList<>(chat));}return r;}
  void start(){try{
   hosted=new HostedMatch();List<RegisteredPlayer> players=new ArrayList<>();Map<RegisteredPlayer,IGuiGame> guis=new HashMap<>();
   for(int i=0;i<4;i++){Seat s=seats[i];s.registered=RegisteredPlayer.forCommander(s.deck);if(s.token!=null){s.registered.setPlayer(new LobbyPlayerHuman(s.name+" · "+(i+1)));s.gui=new WebGui(this,i);guis.put(s.registered,s.gui);}else {LobbyPlayerAi ai=new LobbyPlayerAi(s.name+" · "+(i+1),Set.of());ai.setAiProfile("Default");s.registered.setPlayer(ai);}players.add(s.registered);}
   hosted.setStartGameHook(()->{for(Seat s:seats)if(s.gui!=null){s.gui.events=new WebEvents(s.gui);hosted.getGame().subscribeToEvents(s.gui.events);}status="playing";refresh();});hosted.setEndGameHook(()->{status="finished";refresh();});
   GameRules rules=new GameRules(GameType.Commander);rules.setGamesPerMatch(1);rules.setAllowCheatShuffle(false);rules.setPlayForAnte(false);rules.setAISideboardingEnabled(false);
   hosted.startMatch(rules,Set.of(GameType.Commander),players,guis,null);status="playing";refresh();
  }catch(Throwable e){error="O Forge interrompeu a partida: "+e.getMessage();status="error";e.printStackTrace();refresh();}}
  void refresh(){for(Seat s:seats)if(s.gui!=null)s.gui.refresh();}
  int seatFor(PlayerView p){if(p==null)return -1;for(int i=0;i<4;i++)if(seats[i].registered!=null&&p.getLobbyPlayerName().equals(seats[i].registered.getPlayer().getName()))return i;return -1;}
 }
}
