"use client";
import {useRef,useState} from 'react';
import {Download,Upload,LibraryBig} from 'lucide-react';
import {SavedDeck,readDecks,importDeckBackup,exportDeckBackup} from '@/lib/decks';

export default function SavedDecks({onChoose}:{onChoose:(deck:SavedDeck)=>void}){
 const [decks,setDecks]=useState<SavedDeck[]>(()=>{try{return readDecks()}catch{return []}}),[error,setError]=useState('');const input=useRef<HTMLInputElement>(null);
 const backup=()=>{const url=URL.createObjectURL(new Blob([exportDeckBackup(decks)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download='commander-table-decks.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
 return <section className="saved-decks"><p className="small-note">Seus decks ficam salvos neste navegador. Exporte um backup para levar a outro computador ou endereço de túnel.</p><div className="deck-backup"><button onClick={backup} disabled={!decks.length}><Download size={14}/>Exportar backup</button><button onClick={()=>input.current?.click()}><Upload size={14}/>Importar backup</button><input hidden ref={input} type="file" accept="application/json,.json" onChange={async e=>{const f=e.target.files?.[0];if(!f)return;setError('');try{if(f.size>4_000_000)throw new Error('O backup é muito grande.');setDecks(importDeckBackup(await f.text()))}catch(err){setError(err instanceof Error?err.message:'Não foi possível importar.')}e.target.value='';}}/></div>{error&&<p className="dialog-error" role="alert">{error}</p>}<div className="saved-deck-list">{decks.map(deck=><button key={deck.id} onClick={()=>onChoose(deck)}><LibraryBig size={20}/><span><b>{deck.name}</b><small>{deck.commander}</small></span><span>Usar lista →</span></button>)}{!decks.length&&<p className="empty-note">Ao confirmar uma lista importada, ela será salva aqui.</p>}</div></section>;
}
