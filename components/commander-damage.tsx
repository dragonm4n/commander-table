"use client";
import {useState} from 'react';
import {Swords} from 'lucide-react';
import {Dialog,DialogContent,DialogTitle,DialogDescription} from './ui/dialog';
import type {Player} from '@/lib/table';

export default function CommanderDamage({player}:{player:Player}){
 const [open,setOpen]=useState(false);
 const received=player.commanders?.filter(c=>c.damage>0)??[];
 if(!received.length)return null;
 return <><button className="commander-damage" onClick={()=>setOpen(true)} title="Commander damage — highest individual total. Click for details." aria-label={`Commander damage for ${player.name}`}><Swords size={16}/><b>{Math.max(...received.map(c=>c.damage))}</b></button>
 <Dialog open={open} onOpenChange={setOpen}><DialogContent><DialogTitle>Commander damage · {player.name}</DialogTitle><DialogDescription>21 combat damage from the same commander causes a loss. Each commander is tracked separately.</DialogDescription>{received.map(c=><p key={`${c.from}-${c.name}`} className="damage-detail"><span>{c.name}</span><strong>{c.damage} / 21</strong></p>)}</DialogContent></Dialog></>;
}
