import {Layers3} from 'lucide-react';
import {useEffect,useState} from 'react';
import {Dialog,DialogContent,DialogTitle,DialogDescription} from './ui/dialog';
import {Card,Game} from '@/lib/table';
import DecisionPanel from './decision-panel';

export default function PriorityDock({game,busy,canAct,ownTurn=false,onAction,onInspect,onStack}:{game:Game;busy:boolean;canAct:boolean;ownTurn?:boolean;onAction:(body:Record<string,unknown>)=>void;onInspect:(card:Card)=>void;onStack:()=>void}){
 const [endTurn,setEndTurn]=useState<{version:number;turn:number}|null>(null);
 useEffect(()=>{setEndTurn(null)},[game.controlVersion,game.turn,ownTurn]);
 const cancel=()=>{if(ownTurn&&game.cancelEndsTurn)setEndTurn({version:game.controlVersion,turn:game.turn});else onAction({action:'cancel'});};
 const active=!!game.decision||game.ok.enabled||game.cancel.enabled;
 const targetOption=(id:number)=>game.decision?.kind==='stack'?game.decision.options.find(o=>o.stackId===id):undefined;
 return <section className={`priority-dock ${active?'needs-action':''} ${ownTurn?'own-turn':''}`} aria-label="Priority and choices">
  <Dialog open={!!endTurn} onOpenChange={open=>!open&&setEndTurn(null)}><DialogContent><DialogTitle>End your turn?</DialogTitle><DialogDescription>You will pass for the rest of this turn. Finish any plays you want to make first.</DialogDescription><div className="prompt-buttons"><button autoFocus onClick={()=>setEndTurn(null)}>Keep playing</button><button className="primary" disabled={busy||!canAct||endTurn?.version!==game.controlVersion||endTurn?.turn!==game.turn||!ownTurn||!game.cancel.enabled} onClick={()=>{setEndTurn(null);onAction({action:'cancel'});}}>End my turn</button></div></DialogContent></Dialog>
  {game.stack?.length>0&&<div className="stack-strip"><button className="stack-label" onClick={onStack}><Layers3 size={14}/>Stack · {game.stack.length}</button>{game.stack.map((item,i)=><button key={item.id} data-stack-id={item.id} className={`stack-chip ${targetOption(item.id)?'stack-selectable':''}`} disabled={busy} title={`${targetOption(item.id)?'Choose as target: ':''}${item.text}${item.targets?.length?' → '+item.targets.map(t=>t.name).join(', '):''}`} onClick={()=>{const option=targetOption(item.id);if(option&&game.decision)onAction({action:'answer',decisionId:game.decision.id,selected:[option.id]});else onInspect(item.card)}}><span>{i===0?'TOP':i+1}</span>{item.card.name||'Ability'}{targetOption(item.id)?<small>Choose as target</small>:item.targets?.length?<small>→ {item.targets.map(t=>t.name).join(', ')}</small>:null}</button>)}</div>}
  {game.decision&&(game.decision.presentation==='library'||game.decision.options.length>6)?<p className="library-pending">Your choice is pending. Return to the choice window to finish.</p>:game.decision?<DecisionPanel key={game.decision.id} decision={game.decision} busy={busy} onAnswer={body=>onAction({action:'answer',...body})} onInspect={onInspect}/>:<div className="priority-line"><div><span className="eyebrow">{active?'YOUR PRIORITY / ACTION':'WAITING FOR THE TABLE'}</span><p aria-live="polite">{game.message||'Waiting for Forge…'}</p></div><div className="prompt-buttons"><button className="primary" disabled={!canAct||!game.ok.enabled} onClick={()=>onAction({action:'ok'})}>{game.ok.label||'Confirm'}</button><button disabled={!canAct||!game.cancel.enabled} onClick={cancel}>{game.cancel.label||'Cancel'}</button></div></div>}
 </section>;
}
