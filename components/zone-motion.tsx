"use client";
import {useEffect,useRef,useState} from 'react';

/** Animate actual arrivals, including a draw and discard in the same snapshot. No card identities. */
export default function ZoneMotion({events,zone}:{events?:{id:number;zone:string}[];zone:string}){
 const seen=useRef(new Set<number>());
 const [pulse,setPulse]=useState<{id:number;count:number}|null>(null);
 useEffect(()=>{
  const incoming=(events??[]).filter(e=>e.zone===zone&&!seen.current.has(e.id));
  incoming.forEach(e=>seen.current.add(e.id));
  if(incoming.length)setPulse({id:incoming.at(-1)!.id,count:incoming.length});
 },[events,zone]);
 useEffect(()=>{if(!pulse)return;const timer=setTimeout(()=>setPulse(null),1400);return()=>clearTimeout(timer)},[pulse]);
 return pulse?<span key={pulse.id} className="zone-motion" aria-label={`${pulse.count} card${pulse.count===1?'':'s'} moved to ${zone}`}><i/>+{pulse.count}</span>:null;
}
