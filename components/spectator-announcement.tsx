"use client";
import {useEffect,useState} from 'react';
import {Sparkles} from 'lucide-react';
import {CardArt} from './table-card';
import type {Card,Player} from '@/lib/table';

export default function SpectatorAnnouncement({action,name,onInspect}:{action:Player['lastAction'];name:string;onInspect:(card:Card)=>void}){
 const [visible,setVisible]=useState<Player['lastAction']>();
 useEffect(()=>{
  if(!action)return;
  setVisible(action);
 },[action?.id]);
 useEffect(()=>{
  if(!visible)return;
  const timer=setTimeout(()=>setVisible(undefined),4000);
  return()=>clearTimeout(timer);
 },[visible?.id]);
 if(!visible)return null;
 return <button className="spectator-announcement" onClick={()=>onInspect(visible.card)} aria-label={`${name}: ${visible.text}`}>
  <CardArt card={visible.card}/><span><small><Sparkles size={14}/>{name} {visible.kind==='cast'?'casts':visible.kind==='activate'?'activates':'triggers'}</small><strong>{visible.card.name}</strong><span>{visible.text}</span></span>
 </button>;
}
