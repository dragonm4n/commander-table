import {phases} from '@/lib/table';

const shortNames=['DES','MAN','COM','P1','INI','ATQ','BLQ','D1','DAN','FIM','P2','FIM','LIM'];
export default function PhaseTrack({phase}:{phase:string}){
 const entries=Object.entries(phases),active=entries.findIndex(([id])=>id===phase);
 return <ol className="phase-track" aria-label="Etapas do turno">{entries.map(([id,label],i)=><li key={id} className={`${i===active?'current':''} ${i<active?'past':''}`} aria-current={i===active?'step':undefined} title={`${i+1}. ${label}${i===active?' — etapa atual':''}`}><span className="phase-dot"/><span aria-hidden="true">{shortNames[i]}</span><span className="sr-only">{label}</span></li>)}</ol>;
}
