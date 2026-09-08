import type {CSSProperties} from 'react';
import {Link2} from 'lucide-react';
import type {Card} from '@/lib/table';
import type {PermanentPile as Pile} from '@/lib/battlefield';
import {TableCard} from './table-card';
export default function PermanentPile({pile,disabled,onPlay,onInspect,onPreview,onOpen}:{pile:Pile;disabled:boolean;onPlay:(card:Card)=>void;onInspect:(card:Card)=>void;onPreview:(card:Card|null)=>void;onOpen:(ids:number[],title:string)=>void}){
 const {root,attachments,members}=pile,group=members.length>1,shown=attachments.slice(0,3);
 const open=()=>onOpen(group?members.map(c=>c.id):[root,...attachments].map(c=>c.id),group?`${root.name} ×${members.length}`:`${root.name} — attachments`);
 return <div className={`permanent-pile ${attachments.length?'has-attachments':''} ${group?'token-group':''}`} style={{'--layers':shown.length} as CSSProperties} data-attached-ids={attachments.map(c=>c.id).join(' ')} data-token-ids={members.map(c=>c.id).join(' ')}>
  {shown.map((card,i)=><div className="attached-layer" key={card.id} style={{'--layer':i+1,zIndex:shown.length-i} as CSSProperties}><TableCard card={card} disabled={disabled} onPlay={onPlay} onInspect={onInspect} onPreview={onPreview}/></div>)}
  <div className="pile-front"><TableCard card={root} disabled={group?false:disabled} onPlay={group?open:onPlay} onInspect={onInspect} onPreview={onPreview}/></div>
  {(group||attachments.length>0)&&<button className="pile-count" onClick={open} title={group?'Choose a token in this group':'View and interact with attached cards'} aria-label={group?`Open ${members.length} ${root.name} tokens`:`View ${attachments.length} attachments on ${root.name}`}>{group?`×${members.length}`:<><Link2 size={11}/>{attachments.length}</>}</button>}
  {root.attachedTo&&<span className="attachment-label" title={'Attached to '+root.attachedTo.name}><Link2 size={10}/>{root.attachedTo.name}</span>}
 </div>;
}
