"use client";
import {useRef,useState} from 'react';
import {Download,Upload,LibraryBig} from 'lucide-react';
import {SavedDeck,readDecks,importDeckBackup,exportDeckBackup} from '@/lib/decks';

export default function SavedDecks({onChoose}:{onChoose:(deck:SavedDeck)=>void}){
 const [decks,setDecks]=useState<SavedDeck[]>(()=>{try{return readDecks()}catch{return []}}),[error,setError]=useState('');const input=useRef<HTMLInputElement>(null);
 const backup=()=>{const url=URL.createObjectURL(new Blob([exportDeckBackup(decks)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download='commander-table-decks.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
 return <section className="saved-decks"><p className="small-note">Your decks are saved in this browser. Export a backup before switching computers or tunnel addresses.</p><div className="deck-backup"><button onClick={backup} disabled={!decks.length}><Download size={14}/>Export backup</button><button onClick={()=>input.current?.click()}><Upload size={14}/>Import backup</button><input hidden ref={input} type="file" accept="application/json,.json" onChange={async e=>{const f=e.target.files?.[0];if(!f)return;setError('');try{if(f.size>4_000_000)throw new Error('The backup is too large.');setDecks(importDeckBackup(await f.text()))}catch(err){setError(err instanceof Error?err.message:'Could not import the backup.')}e.target.value='';}}/></div>{error&&<p className="dialog-error" role="alert">{error}</p>}<div className="saved-deck-list">{decks.map(deck=><button key={deck.id} onClick={()=>onChoose(deck)}><LibraryBig size={20}/><span><b>{deck.name}</b><small>{deck.commander}</small></span><span>Use deck →</span></button>)}{!decks.length&&<p className="empty-note">Imported decks are saved here after you confirm them.</p>}</div></section>;
}
