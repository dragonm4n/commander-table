import {phases} from '@/lib/table';
const shortNames=['UNT','UPK','DRW','M1','BEG','ATK','BLK','FS','DMG','END','M2','END','CLN'];
export default function PhaseTrack({phase}:{phase:string}){
 const entries=Object.entries(phases),active=entries.findIndex(([id])=>id===phase);
 return <ol className="phase-track" aria-label="Turn phases">{entries.map(([id,label],i)=><li key={id} className={`${i===active?'current':''} ${i<active?'past':''}`} aria-current={i===active?'step':undefined} title={`${i+1}. ${label}${i===active?' — current step':''}`}><span className="phase-dot"/><span aria-hidden="true">{shortNames[i]}</span><span className="sr-only">{label}</span></li>)}</ol>;
}
