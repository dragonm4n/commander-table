import {Layers3} from 'lucide-react';
import {Card,Game} from '@/lib/table';
import DecisionPanel from './decision-panel';

export default function PriorityDock({game,busy,canAct,onAction,onInspect,onStack}:{game:Game;busy:boolean;canAct:boolean;onAction:(body:Record<string,unknown>)=>void;onInspect:(card:Card)=>void;onStack:()=>void}){
 const active=!!game.decision||game.ok.enabled||game.cancel.enabled;
 const targetOption=(id:number)=>game.decision?.kind==='stack'?game.decision.options.find(o=>o.stackId===id):undefined;
 return <section className={`priority-dock ${active?'needs-action':''}`} aria-label="Priority and choices">
  {game.stack?.length>0&&<div className="stack-strip"><button className="stack-label" onClick={onStack}><Layers3 size={14}/>Stack · {game.stack.length}</button>{game.stack.map((item,i)=><button key={item.id} data-stack-id={item.id} className={`stack-chip ${targetOption(item.id)?'stack-selectable':''}`} disabled={busy} title={`${targetOption(item.id)?'Choose as target: ':''}${item.text}${item.targets?.length?' → '+item.targets.map(t=>t.name).join(', '):''}`} onClick={()=>{const option=targetOption(item.id);if(option&&game.decision)onAction({action:'answer',decisionId:game.decision.id,selected:[option.id]});else onInspect(item.card)}}><span>{i===0?'TOP':i+1}</span>{item.card.name||'Ability'}{targetOption(item.id)?<small>Choose as target</small>:item.targets?.length?<small>→ {item.targets.map(t=>t.name).join(', ')}</small>:null}</button>)}</div>}
  {game.decision?.presentation==='library'?<p className="library-pending">Choose cards in the library window.</p>:game.decision?<DecisionPanel key={game.decision.id} decision={game.decision} busy={busy} onAnswer={body=>onAction({action:'answer',...body})} onInspect={onInspect}/>:<div className="priority-line"><div><span className="eyebrow">{active?'YOUR PRIORITY / ACTION':'WAITING FOR THE TABLE'}</span><p aria-live="polite">{game.message||'Waiting for Forge…'}</p></div><div className="prompt-buttons"><button className="primary" disabled={!canAct||!game.ok.enabled} onClick={()=>onAction({action:'ok'})}>{game.ok.label||'Confirm'}</button><button disabled={!canAct||!game.cancel.enabled} onClick={()=>onAction({action:'cancel'})}>{game.cancel.label||'Cancel'}</button></div></div>}
 </section>;
}
