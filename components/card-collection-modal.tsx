"use client";
import {useState} from 'react';
import {Dialog,DialogContent,DialogTitle,DialogDescription} from './ui/dialog';
import {TableCard} from './table-card';
import ZoomableCardArt from './zoomable-card-art';
import {Eye,PanelTopOpen} from 'lucide-react';
import type {Card} from '@/lib/table';
export default function CardCollectionModal({cards,title,description,disabled,onPlay,onClose}:{cards:Card[];title:string;description:string;disabled:boolean;onPlay:(card:Card)=>void;onClose:()=>void}){
 const [minimized,setMinimized]=useState(false);
 const [preview,setPreview]=useState<Card|undefined>(cards[0]),[search,setSearch]=useState('');
 const current=cards.find(c=>c.id===preview?.id)??cards[0];
 return <>{minimized&&<button className="restore-choice" onClick={()=>setMinimized(false)}><PanelTopOpen size={18}/>Return to {title}</button>}<Dialog open={!minimized} onOpenChange={open=>!open&&onClose()}><DialogContent className="card-collection-modal"><DialogTitle>{title}</DialogTitle><DialogDescription>{description}</DialogDescription>
  <button className="view-battlefield" onClick={()=>setMinimized(true)}><Eye size={16}/>View battlefield</button>
  {cards.length>6&&<input aria-label="Search cards" placeholder="Search cards…" value={search} onChange={e=>setSearch(e.target.value)}/>}
  <div className="library-choice-layout"><div className="zone-grid">{cards.filter(c=>c.name.toLowerCase().includes(search.toLowerCase())).map(c=><TableCard key={c.id} card={c} disabled={disabled} onPlay={card=>{onPlay(card);onClose();}} onInspect={setPreview} onPreview={c=>{if(c)setPreview(c);}}/>)}{!cards.length&&<p className="empty-note">No cards in this group.</p>}</div>
   <aside className="choice-preview">{current&&<><ZoomableCardArt card={current}/><b>{current.name}</b><small>{current.type}</small><p>{current.text}</p></>}</aside>
  </div>
 </DialogContent></Dialog></>;
}
