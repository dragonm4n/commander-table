"use client";
import {useEffect,useState,useRef} from 'react';
import {Sparkles} from 'lucide-react';
import {CardArt} from './table-card';
import type {Card,Player} from '@/lib/table';

export default function SpectatorAnnouncement({action,actions,name,onInspect}:{action:Player['lastAction'];actions?:Player['announcements'];name:string;onInspect:(card:Card)=>void}){
 const [queue,setQueue]=useState<NonNullable<Player['lastAction']>[]>([]);
 const seen=useRef(new Set<number>());
 useEffect(()=>{
  const incoming=(actions??(action?[action]:[])).filter(a=>!seen.current.has(a.id));
  incoming.forEach(a=>seen.current.add(a.id));
  if(incoming.length)setQueue(old=>[...old,...incoming].slice(0,12));
 },[actions,action]);
 const visible=queue[0];
 useEffect(()=>{
  if(!visible)return;
  const timer=setTimeout(()=>setQueue(old=>old.slice(1)),4000);
  return()=>clearTimeout(timer);
 },[visible?.id]);
 if(!visible)return null;
 return <button className="spectator-announcement" onClick={()=>onInspect(visible.card)} aria-label={`${name}: ${visible.text}`}>
  <CardArt card={visible.card}/><span><small><Sparkles size={14}/>{name} {visible.kind==='tutor'?'searches':visible.kind==='cast'?'casts':visible.kind==='activate'?'activates':'triggers'}</small><strong>{visible.card.name}</strong><span>{visible.text}</span></span>
 </button>;
}
