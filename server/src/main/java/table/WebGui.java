/* Commander Table. Copyright 2026. GPL-3.0-or-later. */
package table;
import com.google.gson.*;
import forge.game.*;
import forge.game.card.*;
import forge.game.player.*;
import forge.game.phase.PhaseType;
import forge.game.spellability.*;
import forge.game.zone.ZoneType;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import java.util.*;
import java.util.concurrent.*;
import static table.TableServer.*;

public final class WebGui extends BaseWebGui {
 final TableServer.Room room;final int seat;
 WebEvents events;
 public volatile Map<String,Object> snapshot=new LinkedHashMap<>();
 public volatile Decision prompt;public volatile int controlVersion=0;
 private volatile String message="Waiting for the game",okLabel="Confirm",cancelLabel="Cancel";
 private volatile boolean okEnabled=false,cancelEnabled=false;
 private final Map<Integer,CardView> knownCards=new ConcurrentHashMap<>();
 static final ITriggerEvent CLICK=new ITriggerEvent(){public int getButton(){return 1;}public int getX(){return 0;}public int getY(){return 0;}};
 WebGui(TableServer.Room r,int s){room=r;seat=s;}
 public synchronized void refresh(){
  try{
   GameView g=getGameView();Map<String,Object> state=obj("controlVersion",controlVersion,"message",message,"ok",obj("label",okLabel,"enabled",okEnabled),"cancel",obj("label",cancelLabel,"enabled",cancelEnabled),"decision",prompt==null?null:prompt.publicData);
   if(g!=null){state.put("turn",g.getTurn());state.put("round",room.rounds.value());state.put("phase",g.getPhase()==null?"":g.getPhase().name());state.put("activeSeat",room.seatFor(g.getPlayerTurn()));state.put("over",g.isGameOver());state.put("winner",g.getWinningPlayerName());
    List<Object> players=new ArrayList<>();PlayerView viewer=getCurrentPlayer();
    for(PlayerView p:g.getPlayers()){
     Map<String,Object> zones=new LinkedHashMap<>();for(ZoneType z:List.of(ZoneType.Battlefield,ZoneType.Hand,ZoneType.Library,ZoneType.Command,ZoneType.Graveyard,ZoneType.Exile)){
      List<Object> cards=new ArrayList<>();for(CardView c:p.getCards(z).threadSafeIterable()){
       if((z==ZoneType.Hand||z==ZoneType.Library)&&!c.canBeShownTo(viewer))continue;
       cards.add(card(c,false));
      }zones.put(z.name(),obj("count",p.getZoneSize(z),"cards",cards));
     }
     List<Object> commanders=new ArrayList<>();for(PlayerView owner:g.getPlayers())if(owner.getCommanders()!=null)for(CardView c:owner.getCommanders())commanders.add(obj("name",c.getName(),"from",room.seatFor(owner),"damage",p.getCommanderDamage(c),"casts",owner.getCommanderCast(c)));
     List<Object> mana=new ArrayList<>();for(byte color:forge.card.mana.ManaAtom.MANATYPES)mana.add(obj("color",color,"amount",p.getMana(color)));
     List<Object> statuses=new ArrayList<>();
     if(p.getKeywords()!=null)for(var keyword:p.getKeywords().getValues())statuses.add(obj("name",keyword.original(),"title",keyword.title(),"text",keyword.reminderText()));
     players.add(obj("seat",room.seatFor(p),"id",p.getId(),"name",p.getName(),"life",p.getLife(),"lost",p.getHasLost(),"zones",zones,"mana",mana,"counters",counters(p),"commanders",commanders,"statuses",statuses,"lastAction",events==null?null:events.lastAction(room.seatFor(p))));
    }state.put("players",players);
    List<Object> stack=new ArrayList<>();for(StackItemView item:g.getStack()){
     Set<Map<String,Object>> targets=new LinkedHashSet<>();
     for(StackItemView part=item;part!=null;part=part.getSubInstance()){
      if(part.getTargetCards()!=null)for(CardView target:part.getTargetCards())targets.add(target(target));
      if(part.getTargetPlayers()!=null)for(PlayerView target:part.getTargetPlayers())targets.add(target(target));
     }
     targets.addAll(stackTargets(item.getId()));
     stack.add(obj("id",item.getId(),"text",item.getText(),"card",card(item.getSourceCard(),false),"seat",room.seatFor(item.getActivatingPlayer()),"targets",targets,"ability",item.isAbility(),"trigger",item.isTrigger()));
    }state.put("stack",stack);
    List<Object> combat=new ArrayList<>();var fight=g.getCombat();
    if(fight!=null)for(CardView attacker:fight.getAttackers()){
     GameEntityView defender=fight.getDefender(attacker);
     if(defender!=null)combat.add(obj("kind","attack","source",target(attacker),"target",target(defender)));
     if(fight.getBlockers(attacker)!=null)for(CardView blocker:fight.getBlockers(attacker))combat.add(obj("kind","block","source",target(blocker),"target",target(attacker)));
    }state.put("combat",combat);state.put("sounds",events==null?List.of():events.sounds());
    // Only public log categories: never export debug/private information or arbitrary engine objects.
    Set<GameLogEntryType> types=EnumSet.of(GameLogEntryType.TURN,GameLogEntryType.MULLIGAN,GameLogEntryType.LAND,GameLogEntryType.STACK_ADD,GameLogEntryType.STACK_RESOLVE,GameLogEntryType.COMBAT,GameLogEntryType.DAMAGE,GameLogEntryType.LIFE,GameLogEntryType.GAME_OUTCOME);
    List<String> logs=new ArrayList<>();for(GameLogEntry e:g.getGameLog().getLogEntriesForTypes(types)){logs.add(e.message());if(logs.size()>=100)break;}state.put("log",logs);
   }snapshot=state;
  }catch(RuntimeException e){System.err.println("Projection: "+e);e.printStackTrace();}
 }
 List<Map<String,Object>> stackTargets(int id){
  List<Map<String,Object>> result=new ArrayList<>();
  if(room.hosted==null||room.hosted.getGame()==null)return result;
  var stack=room.hosted.getGame().getStack();
  for(var instance:stack)if(instance.getId()==id)for(var part=instance;part!=null;part=part.getSubInstance())for(var spell:part.getTargetChoices().getTargetSpells()){
   var target=stack.getInstanceMatchingSpellAbilityID(spell);if(target!=null)result.add(obj("kind","stack","id",target.getId(),"seat",room.seatFor(target.getView().getActivatingPlayer()),"name",spell.getHostCard().getName()));
  }
  return result;
 }
 Map<String,Object> target(GameEntityView entity){
  if(entity instanceof PlayerView p)return obj("kind","player","id",p.getId(),"seat",room.seatFor(p),"name",p.getName());
  CardView c=(CardView)entity;String zone=c.getZone()==null?"":c.getZone().name();int controller=room.seatFor(c.getController());
  // A target in a private zone must not reveal its identity or a trackable card ID.
  if((c.getZone()==ZoneType.Hand||c.getZone()==ZoneType.Library)&&!c.canBeShownTo(getCurrentPlayer()))return obj("kind","zone","seat",controller,"zone",zone,"name","Card in a hidden zone");
  return obj("kind","card","id",c.getId(),"seat",controller,"zone",zone,"name",card(c,false).get("name"));
 }
 Map<String,Integer> counters(GameEntityView c){Map<String,Integer> r=new LinkedHashMap<>();if(c.getCounters()!=null)for(CounterType t:c.getCounters().elementSet())r.put(t.getName(),c.getCounters(t));return r;}
 Map<String,Object> card(CardView c,boolean explicitlyRevealed){
  if(c==null)return obj("name","","hidden",true);
  knownCards.put(c.getId(),c);PlayerView viewer=getCurrentPlayer();
  boolean hidden=!explicitlyRevealed&&(!c.canBeShownTo(viewer)||!c.canFaceDownBeShownTo(viewer));
  Map<String,Object> r=obj("id",c.getId(),"hidden",hidden,"name",hidden?"Face-down card":c.getName(),"tapped",c.isTapped(),"attacking",c.isAttacking(),"blocking",c.isBlocking(),"selected",isHighlighted(c),"selectable",isSelectable(c),"actionable",isWeaklySelectable(c),"zone",c.getZone()==null?"":c.getZone().name(),"counters",counters(c),"controllerSeat",room.seatFor(c.getController()));
  if(c.getEntityAttachedTo()!=null)r.put("attachedTo",target(c.getEntityAttachedTo()));
  if(!hidden){CardView.CardStateView v=c.getCurrentState();r.putAll(obj("imageName",c.getOracleName()==null?c.getName():c.getOracleName(),"type",v.getType().toString(),"text",c.getText(),"mana",v.getManaCost().toString(),"creature",v.isCreature(),"land",v.isLand(),"power",v.getPower(),"toughness",v.getToughness(),"loyalty",v.getLoyalty(),"sick",c.isSick(),"token",c.isToken(),"commander",c.isCommander(),"transformed",v.getState()==forge.card.CardStateName.Backside,"damage",c.getDamage()));
   if(c.isToken()){String key=v.getImageKey(viewer==null?List.of():List.of(viewer));r.put("tokenImages",TokenArt.paths(key));if(key!=null&&key.startsWith("c:"))r.put("imageName",key.substring(2).split("\\|")[0]);}
  }return r;
 }
 @Override public void setGameView(GameView g){super.setGameView(g);refresh();}
 @Override protected void updateCurrentPlayer(PlayerView p){refresh();}
 @Override public void openView(TrackableCollection<PlayerView> p){refresh();}
 @Override public void showPromptMessage(PlayerView p,String text,CardView c){message=text;controlVersion++;refresh();}
 @Override public void updateButtons(PlayerView p,String a,String b,boolean ea,boolean eb,boolean focus){okLabel=a;cancelLabel=b;okEnabled=ea;cancelEnabled=eb;controlVersion++;refresh();}
 @Override public void updateCards(Iterable<CardView> cs){refresh();}
 @Override public void updateZones(Iterable<PlayerZoneUpdate> z){refresh();}
 @Override public void refreshField(){refresh();}
 @Override public void refreshCardDetails(Iterable<CardView> cards){refresh();}
 @Override public void updateDependencies(){refresh();}
 @Override public void updateStack(){refresh();}
 @Override public void updateTurn(PlayerView p){refresh();}
 @Override public void updatePhase(boolean save){refresh();}
 @Override public void updateLives(Iterable<PlayerView> p){refresh();}
 @Override public void updateManaPool(Iterable<PlayerView> p){refresh();}
 @Override public void showCombat(){refresh();}
 @Override public void finishGame(){room.status="finished";room.refresh();}
 @Override public void flashIncorrectAction(){message="That action is not valid right now. "+message;refresh();}
 @Override public void showErrorDialog(String msg,String title){message=title+": "+msg;refresh();}
 @Override public void message(String msg,String title){message=msg;refresh();}
 @Override public boolean isUiSetToSkipPhase(PlayerView p,PhaseType t){return false;}
 @Override public void setSelectables(Iterable<CardView> c,int min,int max){super.setSelectables(c,min,max);refresh();}
 @Override public void clearSelectables(){super.clearSelectables();refresh();}
 @Override public void setHighlighted(Iterable<GameEntityView> c,boolean b){super.setHighlighted(c,b);refresh();}
 @Override public void setWeaklySelectable(Iterable<CardView> c){super.setWeaklySelectable(c);refresh();}
 @Override public void clearWeaklySelectable(){super.clearWeaklySelectable();refresh();}

 public void action(JsonObject b){
  require(prompt==null,"Answer the pending choice.");require(integer(b,"controlVersion",-1)==controlVersion,"The situation changed; try again.");
  var ctl=getGameController();require(ctl!=null,"Player controls are unavailable.");String a=str(b,"action","");
  switch(a){
   case "ok":require(okEnabled,"Confirmation is unavailable.");ctl.selectButtonOk();break;
   case "cancel":require(cancelEnabled,"Cancellation is unavailable.");ctl.selectButtonCancel();break;
   case "card":CardView c=knownCards.get(integer(b,"cardId",-1));require(c!=null,"Card not found.");require(c.canBeShownTo(getCurrentPlayer())||c.getZone()==ZoneType.Battlefield,"Card is unavailable.");ctl.selectCard(c,null,CLICK);break;
   case "player":int target=integer(b,"playerId",-1);PlayerView p=null;for(PlayerView x:getGameView().getPlayers())if(x.getId()==target)p=x;require(p!=null,"Player not found.");ctl.selectPlayer(p,CLICK);break;
   case "mana":int color=integer(b,"color",-1);require(Set.of(1,2,4,8,16,32).contains(color),"Invalid mana type.");ctl.useMana((byte)color);break;
   case "attackAll":ctl.alphaStrike();break;
   case "undo":ctl.undoLastAction();break;
   case "concede":ctl.concede();break;
   default:throw new IllegalArgumentException("Unknown action.");
  }refresh();
 }
 static final class Decision {
  final String id=key();final List<?> options;final int min,max;final String kind;final CompletableFuture<JsonObject> future=new CompletableFuture<>();final Map<String,Object> publicData;
  Decision(WebGui gui,String kind,String text,List<?> choices,int min,int max,FSerializableFunction display){
   this.options=choices==null?List.of():new ArrayList<>(choices);this.kind=kind.equals("select")&&this.options.stream().anyMatch(c->c instanceof StackItemView)?"stack":kind;this.min=min;this.max=max;
   List<Object> opts=new ArrayList<>();
   for(int i=0;i<this.options.size();i++){
    Object c=this.options.get(i);String label=display==null?String.valueOf(c):String.valueOf(display.apply(c));Map<String,Object> o=obj("id",i,"label",label);
    if(c instanceof StackItemView item){o.put("stackId",item.getId());o.put("card",gui.card(item.getSourceCard(),false));o.put("seat",gui.room.seatFor(item.getActivatingPlayer()));}
    if(c instanceof CardView cv){Map<String,Object> visible=gui.card(cv,kind.equals("reveal"));o.put("card",visible);if(Boolean.TRUE.equals(visible.get("hidden")))o.put("label","Face-down card");}opts.add(o);
   }
   publicData=obj("id",id,"kind",this.kind,"message",text,"min",min,"max",max,"options",opts,"presentation",this.options.stream().anyMatch(c->c instanceof CardView cv&&cv.getZone()==ZoneType.Library)?"library":"dock");
  }
 }
 JsonObject ask(String kind,String msg,List<?> choices,int min,int max,FSerializableFunction display){
  Decision d=new Decision(this,kind,msg,choices,min,max,display);prompt=d;controlVersion++;refresh();
  try{return d.future.get();}catch(Exception e){throw new IllegalStateException("Choice interrupted.",e);}finally{if(prompt==d)prompt=null;controlVersion++;refresh();}
 }
 public void answer(JsonObject b){Decision d=prompt;require(d!=null&&d.id.equals(str(b,"decisionId","")),"This choice has already been answered or changed.");
  if(d.kind.equals("number")){int v=integer(b,"value",Integer.MIN_VALUE);require(v>=d.min&&v<=d.max,"Number is outside the allowed range.");}
  else if(d.kind.equals("text")){require(str(b,"value","").length()<=500,"The answer is too long.");}
  else if(d.kind.equals("allocation")){JsonArray values=b.has("values")?b.getAsJsonArray("values"):new JsonArray();require(values.size()==d.options.size(),"Enter all amounts.");int total=0;for(JsonElement e:values){int v=e.getAsInt();require(v>=d.min&&v<=d.max,"Invalid quantity.");total+=v;}require(total==d.max,"Assign exactly "+d.max+".");}
  else {JsonArray ids=b.has("selected")?b.getAsJsonArray("selected"):new JsonArray();require(ids.size()>=d.min&&ids.size()<=d.max,"Choose between "+d.min+" and "+d.max+" options.");Set<Integer> used=new HashSet<>();for(JsonElement e:ids){int id=e.getAsInt();require(id>=0&&id<d.options.size()&&used.add(id),"Invalid or repeated option.");}}
  require(d.future.complete(b),"This choice has already been answered.");
 }
 <T> List<T> choose(String kind,String msg,List<T> options,int min,int max,FSerializableFunction<T,String> display){if(options==null||options.isEmpty())return List.of();int lo=Math.max(0,min),hi=max<0?options.size():Math.min(max,options.size());JsonObject reply=ask(kind,msg,options,lo,Math.max(lo,hi),display);List<T> out=new ArrayList<>();for(JsonElement i:reply.getAsJsonArray("selected"))out.add(options.get(i.getAsInt()));return out;}
 @Override public <T> List<T> getChoices(String msg,int min,int max,List<T> choices,List<T> selected,FSerializableFunction<T,String> display){return choose("select",msg,choices,min,max,display);}
 @Override public SpellAbilityView getAbilityToPlay(CardView card,List<SpellAbilityView> a,ITriggerEvent e){
  // Same distinction as Forge's CMatchUI: null event is an engine-supplied
  // choice (including mandatory triggers), not an attempt to activate a card.
  // Triggered abilities report canPlay=false because they cannot be activated.
  List<SpellAbilityView> choices=e==null?a:a.stream().filter(SpellAbilityView::canPlay).toList();
  if(choices.isEmpty())return null;
  if(choices.size()==1&&(e==null||!choices.get(0).promptIfOnlyPossibleAbility()))return choices.get(0);
  return choose("select","Choose an ability",choices,1,1,null).get(0);
 }
 @Override public boolean showConfirmDialog(String m,String t,String yes,String no,boolean d){return choose("select",t+" — "+m,List.of(yes,no),1,1,null).get(0).equals(yes);}
 @Override public boolean confirm(CardView c,String q,boolean d,List<String> opts){return choose("select",q,opts,1,1,null).get(0).equals(opts.get(0));}
 @Override public int showOptionDialog(String m,String t,FSkinProp icon,List<String> opts,int d){return opts.indexOf(choose("select",t+" — "+m,opts,1,1,null).get(0));}
 @Override public String showInputDialog(String m,String t,FSkinProp icon,String initial,List<String> opts,boolean numeric){if(opts!=null&&!opts.isEmpty())return choose("select",m,opts,1,1,null).get(0);return str(ask(numeric?"number":"text",t+" — "+m,List.of(),0,numeric?1000000:500,null),"value","");}
 @Override public Integer getInteger(String m,int min,int max,boolean desc){return ask("number",m,List.of(),min,max,null).get("value").getAsInt();}
 @Override public Integer getInteger(String m,int min,int max,int cutoff){return getInteger(m,min,max,false);}
 @Override public <T> IGuiGame.OrderResult<T> order(String title,String top,int remainingMin,int remainingMax,List<T> source,List<T> dest,CardView reference,boolean sb,boolean remember){List<T> all=new ArrayList<>(source);if(dest!=null)all.addAll(dest);int min=remainingMax<0?0:Math.max(0,all.size()-remainingMax),max=remainingMin<0?all.size():Math.min(all.size(),all.size()-remainingMin);return new IGuiGame.OrderResult<>(choose("order",title+" — "+top,all,min,max,null),false);}
 @Override public GameEntityView chooseSingleEntityForEffect(String msg,List<? extends GameEntityView> options,DelayedReveal reveal,boolean optional){if(reveal!=null)reveal(reveal.getMessagePrefix(),new ArrayList<>(reveal.getCards()));List<? extends GameEntityView> a=choose("select",msg,options,optional?0:1,1,null);return a.isEmpty()?null:a.get(0);}
 @Override public List<GameEntityView> chooseEntitiesForEffect(String msg,List<? extends GameEntityView> opts,int min,int max,DelayedReveal reveal){if(reveal!=null)reveal(reveal.getMessagePrefix(),new ArrayList<>(reveal.getCards()));return new ArrayList<>(choose("select",msg,opts,min,max,null));}
 @Override public <T> void reveal(String msg,List<T> items){choose("reveal",msg,items,0,0,null);}
 @Override public List<CardView> manipulateCardList(String title,Iterable<CardView> cards,Iterable<CardView> movable,boolean top,boolean bottom,boolean anywhere){List<CardView> all=new ArrayList<>(),move=new ArrayList<>();cards.forEach(all::add);movable.forEach(move::add);List<CardView> ordered=choose("order",title+" — select cards in the desired order",move,move.size(),move.size(),null);boolean putTop=top&&(!bottom||showConfirmDialog("Put these cards on top?","Order","Top","Bottom",true));all.removeAll(move);if(putTop)all.addAll(0,ordered);else all.addAll(ordered);return all;}
 @Override public Map<CardView,Integer> assignCombatDamage(CardView attacker,List<CardView> blockers,int damage,GameEntityView defender,boolean override,boolean maySkip){
  if(damage<=0)return Map.of();
  List<CardView> options=new ArrayList<>(blockers);boolean divide=attacker.getCurrentState().hasDivideDamage()&&override;
  if(defender!=null&&(attacker.getCurrentState().hasTrample()||divide))options.add(null);
  if(options.isEmpty()){Map<CardView,Integer> single=new HashMap<>();single.put(null,damage);return single;}
  while(true){JsonObject a=ask("allocation","Assign "+damage+" damage from "+attacker.getName()+". Assign lethal damage to blockers before hitting the defender.",options,0,damage,c->c==null?defender.getName():((CardView)c).getName());
   Map<CardView,Integer> result=new HashMap<>();int i=0;boolean alive=false,valid=true;
   for(JsonElement v:a.getAsJsonArray("values")){CardView c=options.get(i++);int n=v.getAsInt();result.put(c,n);if(!divide&&alive&&(!override||c==null)&&n>0)valid=false;
    if(c!=null){int lethal=Math.max(0,c.getLethalDamage());if(attacker.getCurrentState().hasDeathtouch()&&!c.getCurrentState().isPlaneswalker())lethal=Math.min(1,lethal);alive|=n<lethal;}}
   if(valid)return result;message="Invalid assignment: assign lethal damage before moving to the next target.";
  }
 }
 @Override public Map<Object,Integer> assignGenericAmount(CardView source,Map<Object,Integer> target,int amount,boolean atLeastOne,String label){
  if(amount<=0)return Map.of();List<Object> opts=new ArrayList<>(target.keySet());
  while(true){JsonObject a=ask("allocation",label+" — respect each target's limit",opts,atLeastOne?1:0,amount,c->String.valueOf(c)+" (max. "+(target.get(c)==null?amount:target.get(c))+")");Map<Object,Integer> out=new HashMap<>();int i=0;boolean valid=true;
   for(JsonElement v:a.getAsJsonArray("values")){Object option=opts.get(i++);int n=v.getAsInt();if(target.get(option)!=null&&n>target.get(option))valid=false;out.put(option,n);}if(valid)return out;
  }
 }
}
