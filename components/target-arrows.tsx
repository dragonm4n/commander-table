"use client";
import {useEffect,useState} from 'react';
import {Game,Target} from '@/lib/table';

type Edge={key:string;kind:'target'|'attack'|'block';from:Target;to:Target;label:string};
type Path={key:string;kind:Edge['kind'];d:string;label:string};

// Match public engine identities to visible table anchors; private zones have no card identity.
function anchor(target:Edge['from']){
 const selectors:string[]=[];
 if(target.kind==='stack')selectors.push(`[data-stack-id="${target.id}"]`);
 if(target.kind==='card')selectors.push(`[data-card-id="${target.id}"]`);
 if('zone' in target&&target.zone)selectors.push(`[data-zone-seat="${target.seat}"][data-zone="${target.zone}"]`);
 selectors.push(`[data-player-seat="${target.seat}"]`);
 for(const selector of selectors)for(const node of document.querySelectorAll<HTMLElement>(selector)){
  const rect=node.getBoundingClientRect();if(!rect.width||!rect.height)continue;
  let left=Math.max(0,rect.left),right=Math.min(window.innerWidth,rect.right),top=Math.max(0,rect.top),bottom=Math.min(window.innerHeight,rect.bottom);
  for(let parent=node.parentElement;parent;parent=parent.parentElement){
   const style=getComputedStyle(parent);if(/auto|scroll|hidden|clip/.test(style.overflow+style.overflowX+style.overflowY)){
    const clip=parent.getBoundingClientRect();left=Math.max(left,clip.left);right=Math.min(right,clip.right);top=Math.max(top,clip.top);bottom=Math.min(bottom,clip.bottom);
   }
  }
  if(right-left>8&&bottom-top>8)return {x:(left+right)/2,y:(top+bottom)/2,r:Math.min((right-left)/2,(bottom-top)/2,22)};
 }
 return null;
}

export default function TargetArrows({game,enabled}:{game:Game;enabled:boolean}){
 const [paths,setPaths]=useState<Path[]>([]);
 const edges:Edge[]=[...(game.stack??[]).flatMap(item=>(item.targets??[]).map((target,i)=>({key:`s${item.id}-${i}`,kind:'target' as const,from:{kind:'stack' as const,id:item.id,seat:item.seat,name:item.card.name},to:target,label:`${item.card.name}: alvo ${target.name}`}))),...(game.combat??[]).map((edge,i)=>({key:`c${i}`,kind:edge.kind,from:edge.source,to:edge.target,label:`${edge.source.name} ${edge.kind==='attack'?'ataca':'bloqueia'} ${edge.target.name}`}))];
 const encoded=JSON.stringify(edges);
 useEffect(()=>{
  if(!enabled){setPaths([]);return;}
  const current:Edge[]=JSON.parse(encoded);let frame=0;
  const measure=()=>{frame=0;setPaths(current.flatMap((edge,i)=>{const a=anchor(edge.from),b=anchor(edge.to);if(!a||!b)return [];const dx=b.x-a.x,dy=b.y-a.y,len=Math.hypot(dx,dy);if(len<12)return [];const ex=b.x-dx/len*(b.r+7),ey=b.y-dy/len*(b.r+7);const bend=Math.min(65,len*.15)*(i%2?-1:1);return [{key:edge.key,kind:edge.kind,label:edge.label,d:`M ${a.x} ${a.y} Q ${(a.x+ex)/2-dy/len*bend} ${(a.y+ey)/2+dx/len*bend} ${ex} ${ey}`}]}));};
  const schedule=()=>{if(!frame)frame=requestAnimationFrame(measure);};schedule();
  window.addEventListener('resize',schedule);document.addEventListener('scroll',schedule,true);
  const observer=new ResizeObserver(schedule);document.querySelectorAll('.battlefields,.hand-cards,.priority-dock').forEach(node=>observer.observe(node));
  return()=>{cancelAnimationFrame(frame);observer.disconnect();window.removeEventListener('resize',schedule);document.removeEventListener('scroll',schedule,true);};
 },[encoded,enabled,game.controlVersion]);
 return <svg className="target-arrows" aria-label="Alvos e combate" role="img"><defs>{['target','attack','block'].map(kind=><marker id={`arrow-${kind}`} key={kind} markerWidth="8" markerHeight="8" refX="6" refY="4" orient="auto" markerUnits="userSpaceOnUse"><path className={`arrow-${kind}`} d="M0,0 L8,4 L0,8 Z"/></marker>)}</defs>{paths.map(path=><g key={path.key}><path className="arrow-shadow" d={path.d}/><path className={`arrow-line arrow-${path.kind}`} d={path.d} markerEnd={`url(#arrow-${path.kind})`}><title>{path.label}</title></path></g>)}</svg>;
}
