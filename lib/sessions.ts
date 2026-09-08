import type {Session} from './table';

export const SESSION_KEY='commander-table-session-v1';
const CONNECTIONS='commander-table-connections-v2',JOIN_KEYS='commander-table-join-keys-v1';
function valid(v:unknown):v is Session {
 const s=v as Session;
 return !!s&&typeof s.server==='string'&&typeof s.room==='string'&&typeof s.token==='string'&&Number.isInteger(s.seat)&&s.seat>=0&&s.seat<4;
}
function all(storage:Storage):Session[]{
 try{const items=JSON.parse(storage.getItem(CONNECTIONS)||'[]');return Array.isArray(items)?items.filter(valid):[];}catch{return [];}
}
function joinKeys(storage:Storage):Record<string,string>{
 try{const keys=JSON.parse(storage.getItem(JOIN_KEYS)||'{}');return keys&&typeof keys==='object'&&!Array.isArray(keys)?keys:{};}catch{return {};}
}
export function rememberedSession(server?:string,room?:string,storage:Storage=localStorage){return all(storage).find(s=>(!server||s.server===server)&&(!room||s.room===room))??null;}
export function rememberSession(session:Session,storage:Storage=localStorage,tab:Storage=sessionStorage){
 // Persist the credential before changing the active view. Other tabs and a reopened browser can recover it.
 storage.setItem(CONNECTIONS,JSON.stringify([session,...all(storage).filter(s=>s.server!==session.server||s.room!==session.room)].slice(0,20)));
 tab.setItem(SESSION_KEY,JSON.stringify(session));
}
export function activeSession(tab:Storage=sessionStorage){try{const s=JSON.parse(tab.getItem(SESSION_KEY)||'null');return valid(s)?s:null;}catch{return null;}}
export function disconnectSession(tab:Storage=sessionStorage){tab.removeItem(SESSION_KEY);}
export function forgetSession(session:Session,storage:Storage=localStorage,tab:Storage=sessionStorage){
 storage.setItem(CONNECTIONS,JSON.stringify(all(storage).filter(s=>s.server!==session.server||s.room!==session.room)));
 const keys=joinKeys(storage);delete keys[session.server+'|'+session.room];storage.setItem(JOIN_KEYS,JSON.stringify(keys));
 if(activeSession(tab)?.token===session.token)disconnectSession(tab);
}
export function joinKey(server:string,room:string,storage:Storage=localStorage){
 const existing=rememberedSession(server,room,storage);if(existing?.resumeKey)return existing.resumeKey;
 const keys=joinKeys(storage);
 const id=server+'|'+room;
 if(!/^[a-zA-Z0-9_-]{32,80}$/.test(keys[id]||'')){keys[id]=crypto.randomUUID();storage.setItem(JOIN_KEYS,JSON.stringify(keys));}
 return keys[id];
}
export function privateRejoinLink(session:Session){return session.resumeKey?`${session.server}/#${new URLSearchParams({server:session.server,room:session.room,resume:session.resumeKey})}`:'';}
