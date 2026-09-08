"use client";
import {useState} from 'react';
import {Dialog,DialogContent,DialogTitle,DialogDescription} from './ui/dialog';
import DecisionPanel from './decision-panel';
import {CardArt} from './table-card';
import type {Card,Decision} from '@/lib/table';
export default function DecisionModal({decision,busy,error,onAnswer}:{decision:Decision;busy:boolean;error?:string;onAnswer:(body:Record<string,unknown>)=>void}){
 const [preview,setPreview]=useState<Card|undefined>(decision.options.find(o=>o.card)?.card);
 return <Dialog open><DialogContent className="library-choice-modal" showCloseButton={false} onEscapeKeyDown={e=>e.preventDefault()} onPointerDownOutside={e=>e.preventDefault()}>
  <DialogTitle>{decision.kind==='reveal'?'Look at cards':decision.kind==='order'?'Order cards':'Choose from library'}</DialogTitle><DialogDescription>{decision.message}</DialogDescription>
  <div className="library-choice-layout"><DecisionPanel decision={decision} busy={busy} onAnswer={onAnswer} onInspect={setPreview} popup/>
   <aside className="choice-preview">{preview&&<><CardArt card={preview} large/><b>{preview.name}</b><small>{preview.type}</small><p>{preview.text}</p></>}</aside>
  </div>
  {error&&<p className="dialog-error" role="alert">{error}</p>}
 </DialogContent></Dialog>;
}
