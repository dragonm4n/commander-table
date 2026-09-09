"use client";
import {useState} from 'react';
import {Cpu} from 'lucide-react';
import type {AiCoverage as Coverage} from '@/lib/table';
import {Dialog,DialogContent,DialogTitle,DialogDescription} from './ui/dialog';
export default function AiCoverage({value}:{value?:Coverage}){
 const [open,setOpen]=useState(false);if(!value)return null;
 return <><button className="ai-coverage" onClick={()=>setOpen(true)} title="Percentage of cards without known Forge AI warnings; not measured decision accuracy."><Cpu size={13}/><span>AI card coverage</span><b>{value.percent}%</b></button>
 <Dialog open={open} onOpenChange={setOpen}><DialogContent><DialogTitle>AI card coverage · {value.percent}%</DialogTitle><DialogDescription>{value.total-value.flagged} of {value.total} cards have no known Forge AI warning. Counts include lands and the commander. This measures catalog coverage, not how often the AI makes the correct decision. Even 100% can include misplays.</DialogDescription>{value.warnings.length?<ul className="ai-warning-list">{value.warnings.map(c=><li key={c.name}>{c.count} × {c.name}{c.commander?' (commander)':''}</li>)}</ul>:<p>No cards in this deck carry the current Forge AI warning.</p>}</DialogContent></Dialog></>;
}
