import {useState} from 'react';
import {Swords,Shield,X} from 'lucide-react';
import type {Game,Room} from '@/lib/table';
export default function CombatGuide({game,seats,mySeat,onPlayer,canAct}:{game:Game;seats:Room['seats'];mySeat:number;onPlayer:(id:number)=>void;canAct:boolean}){
 const [dismissed,setDismissed]=useState('');
 const noticeKey=`${game.turn}:${game.phase}`;
 const attacking=game.phase==='COMBAT_DECLARE_ATTACKERS',blocking=game.phase==='COMBAT_DECLARE_BLOCKERS';
 if((!attacking&&!blocking)||dismissed===noticeKey)return null;
 const selected=game.players.find(p=>p.selectedDefender),own=game.activeSeat===mySeat;
 return <div className={`combat-guide ${blocking?'blocking-guide':''}`} role="status"><button className="combat-guide-close" aria-label="Dismiss combat notice" onClick={()=>setDismissed(noticeKey)}><X size={14}/></button>{attacking?<Swords size={23}/>:<Shield size={23}/>}<div><strong>{attacking?'Declare attackers':'Declare blockers'}</strong><span>{attacking?own?'Choose a defender, select your creatures, then confirm.':`${seats[game.activeSeat]?.name} is choosing attackers.`:'Select a blocker and the creature it blocks, then confirm. Arrows update as you choose.'}</span></div>{attacking&&own&&<div className="combat-defenders">{game.players.filter(p=>p.seat!==mySeat&&!p.lost).map(p=><button key={p.id} disabled={!canAct} aria-pressed={selected?.id===p.id} onClick={()=>onPlayer(p.id)}>{seats[p.seat]?.name}{selected?.id===p.id?' · target':''}</button>)}</div>}</div>;
}
