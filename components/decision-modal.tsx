"use client";
import {useState} from 'react';
import {Dialog,DialogContent,DialogTitle,DialogDescription} from './ui/dialog';
import DecisionPanel,{emptyDraft} from './decision-panel';
import ZoomableCardArt from './zoomable-card-art';
import {Eye,PanelTopOpen} from 'lucide-react';
import type {Card,Decision} from '@/lib/table';
export default function DecisionModal({decision,busy,error,onAnswer}:{decision:Decision;busy:boolean;error?:string;onAnswer:(body:Record<string,unknown>)=>void}){
 const [minimized,setMinimized]=useState(false);
 const [draft,setDraft]=useState(()=>emptyDraft(decision));
 const [preview,setPreview]=useState<Card|undefined>(decision.source??decision.options.find(o=>o.card)?.card);
 return <>{minimized&&<button className="restore-choice" onClick={()=>setMinimized(false)}><PanelTopOpen size={18}/>Return to choice · {draft.selected.length} selected</button>}<Dialog open={!minimized}><DialogContent className="library-choice-modal" showCloseButton={false} onEscapeKeyDown={e=>e.preventDefault()} onPointerDownOutside={e=>e.preventDefault()}>
  <DialogTitle>{decision.presentation==='ability'?`Ability choice · ${decision.source?.name??'Card'}`:decision.kind==='reveal'?'Look at cards':decision.kind==='order'?'Order cards':'Choose cards or options'}</DialogTitle><DialogDescription>{decision.message}</DialogDescription>
  <button className="view-battlefield" onClick={()=>setMinimized(true)}><Eye size={16}/>View battlefield</button><div className="library-choice-layout"><DecisionPanel decision={decision} busy={busy} onAnswer={onAnswer} onInspect={setPreview} draft={draft} onDraftChange={setDraft} popup/>
   <aside className="choice-preview">{preview&&<><ZoomableCardArt card={preview}/><b>{preview.name}</b><small>{preview.type}</small><p>{preview.text}</p></>}</aside>
  </div>
  {error&&<p className="dialog-error" role="alert">{error}</p>}
 </DialogContent></Dialog></>;
}
