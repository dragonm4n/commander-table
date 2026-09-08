export type SavedDeck={id:string;name:string;commander:string;list:string;updated:number};
const KEY='commander-table-decks-v1';
function validate(value:unknown):SavedDeck[]{
 if(!Array.isArray(value)||value.length>100)throw new Error('Backups can contain up to 100 decks.');
 const ids=new Set<string>();return value.map(v=>{
  if(!v||typeof v.id!=='string'||!v.id||v.id.length>100||ids.has(v.id)||typeof v.name!=='string'||!v.name.trim()||v.name.length>80||typeof v.commander!=='string'||!v.commander.trim()||v.commander.length>500||typeof v.list!=='string'||!v.list.trim()||v.list.length>30000||!Number.isFinite(v.updated))throw new Error('The backup contains an invalid deck.');
  ids.add(v.id);return {id:v.id,name:v.name,commander:v.commander,list:v.list,updated:v.updated};
 });
}
export function readDecks(storage:Storage=localStorage):SavedDeck[]{const raw=storage.getItem(KEY);return raw?validate(JSON.parse(raw)):[];}
export function writeDecks(decks:SavedDeck[],storage:Storage=localStorage){storage.setItem(KEY,JSON.stringify(validate(decks)));return decks;}
export function saveImportedDeck(draft:Pick<SavedDeck,'name'|'commander'|'list'>,storage:Storage=localStorage){
 const existing=readDecks(storage),same=existing.find(d=>d.commander.trim()===draft.commander.trim()&&d.list.trim()===draft.list.trim());
 const record={...draft,name:draft.name.trim()||draft.commander.split('\n')[0].slice(0,80),id:same?.id||crypto.randomUUID(),updated:Date.now()};
 return writeDecks([record,...existing.filter(d=>d.id!==record.id)],storage);
}
export function importDeckBackup(text:string,storage:Storage=localStorage){
 if(text.length>4_000_000)throw new Error('The backup is too large.');
 const parsed=JSON.parse(text);if(parsed.format!=='commander-table-decks'||parsed.version!==1)throw new Error('Unrecognized backup format.');
 const incoming=validate(parsed.decks),merged=readDecks(storage);
 for(const deck of incoming)if(!merged.some(d=>d.commander.trim()===deck.commander.trim()&&d.list.trim()===deck.list.trim()))merged.push({...deck,id:crypto.randomUUID()});
 return writeDecks(merged,storage);
}
export function exportDeckBackup(decks:SavedDeck[]){return JSON.stringify({format:'commander-table-decks',version:1,decks:validate(decks)},null,2);}
