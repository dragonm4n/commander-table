"use client";
import {useState} from 'react';
import {ArrowDown,ArrowUp,Check,Search} from 'lucide-react';
import {CardArt} from './table-card';
import type {Card,Decision} from '@/lib/table';
export default function DecisionPanel({decision:d,busy,onAnswer,onInspect,popup=false}:{decision:Decision;busy:boolean;onAnswer:(body:Record<string,unknown>)=>void;onInspect:(c:Card)=>void;popup?:boolean}){
 const [selected,setSelected]=useState<number[]>([]),[value,setValue]=useState(String(d.min)),[values,setValues]=useState<number[]>(d.options.map(()=>d.min)),[search,setSearch]=useState('');
 const allocation=d.kind==='allocation',number=d.kind==='number',text=d.kind==='text',reveal=d.kind==='reveal',order=d.kind==='order',stack=d.kind==='stack';
 const toggle=(id:number)=>{if(reveal)return;setSelected(old=>old.includes(id)?old.filter(i=>i!==id):d.max===1?[id]:old.length<d.max?[...old,id]:old)};
 const valid=allocation?values.reduce((a,b)=>a+b,0)===d.max&&values.every(v=>Number.isInteger(v)&&v>=d.min):number?Number.isInteger(Number(value))&&Number(value)>=d.min&&Number(value)<=d.max:text?value.length<=500:reveal||selected.length>=(stack?Math.max(1,d.min):d.min)&&selected.length<=d.max;
 const answer=()=>onAnswer({decisionId:d.id,...(allocation?{values}:number?{value:Number(value)}:text?{value}:{selected})});
 const move=(i:number,by:number)=>setSelected(old=>{const next=[...old];[next[i],next[i+by]]=[next[i+by],next[i]];return next});
 return <section className={`decision-panel ${stack?'stack-decision':''} ${popup?'popup-decision':''}`}>
  {!popup&&<><div className="eyebrow">YOUR CHOICE</div><h3>{d.message}</h3></>}
  {!reveal&&!number&&!text&&!allocation&&<p>{stack?'Choose a stack target and confirm.':<>Choose {d.min===d.max?d.min:`${d.min}–${d.max}`} {d.max===1?'option':'options'}.{order?' Click order determines card order.':''} <b>{selected.length} selected</b></>}</p>}
  {popup&&d.options.length>6&&<label className="decision-search"><Search size={16}/><input aria-label="Search available cards" placeholder="Search available cards…" value={search} onChange={e=>setSearch(e.target.value)}/></label>}
  {(number||text)&&<label>{number?`Value from ${d.min} to ${d.max}`:'Answer'}<input autoFocus type={number?'number':'text'} value={value} min={d.min} max={d.max} maxLength={500} onChange={e=>setValue(e.target.value)}/></label>}
  <div className="decision-options">{d.options.map((o,i)=>({o,i})).filter(({o})=>[o.label,o.card?.name,o.card?.type].join(' ').toLowerCase().includes(search.toLowerCase())).map(({o,i})=><div className="decision-row" key={o.id}>
   <button className={`decision-option ${selected.includes(o.id)?'chosen':''}`} disabled={busy||allocation} aria-pressed={reveal?undefined:selected.includes(o.id)} onClick={()=>reveal?o.card&&onInspect(o.card):toggle(o.id)} onMouseEnter={()=>o.card&&onInspect(o.card)} onFocus={()=>o.card&&onInspect(o.card)}>
    {selected.includes(o.id)&&<span className="choice-number">{order?selected.indexOf(o.id)+1:<Check size={13}/>}</span>}{(stack||popup)&&o.card&&<CardArt card={o.card}/>}<span>{o.label}</span>
   </button>{o.card&&<button className="small-button" onClick={()=>o.card&&onInspect(o.card)} aria-label={'Inspect '+o.label}>Inspect</button>}
   {allocation&&<input aria-label={'Amount for '+o.label} type="number" min={d.min} max={d.max} value={values[i]} onChange={e=>setValues(old=>old.map((n,j)=>j===i?Number(e.target.value):n))}/>}
  </div>)}</div>
  {order&&selected.length>1&&<div className="chosen-order">{selected.map((id,i)=><div key={id}><span>{i+1}. {d.options.find(o=>o.id===id)?.label}</span><button aria-label="Move up" disabled={i===0} onClick={()=>move(i,-1)}><ArrowUp size={13}/></button><button aria-label="Move down" disabled={i===selected.length-1} onClick={()=>move(i,1)}><ArrowDown size={13}/></button></div>)}</div>}
  {allocation&&<p>Assigned: {values.reduce((a,b)=>a+b,0)} / {d.max}</p>}
  <div className="decision-confirm"><button className="primary" disabled={busy||!valid} onClick={answer}>{busy?'Sending…':reveal?'Continue':stack?'Confirm target':'Confirm selection'}</button>{stack&&d.min===0&&<button className="cancel-target" disabled={busy} onClick={()=>onAnswer({decisionId:d.id,selected:[]})}>Cancel selection</button>}</div>
 </section>;
}
