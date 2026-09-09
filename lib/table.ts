export type Card = {id:number;name:string;hidden?:boolean;imageName?:string;type?:string;text?:string;mana?:string;power?:number;toughness?:number;loyalty?:string;creature?:boolean;land?:boolean;tapped?:boolean;attacking?:boolean;blocking?:boolean;selected?:boolean;selectable?:boolean;actionable?:boolean;sick?:boolean;token?:boolean;commander?:boolean;transformed?:boolean;damage?:number;counters?:Record<string,number>;zone?:string;tokenImages?:string[];controllerSeat?:number;attachedTo?:Target};
export type Zone = {count:number;cards:Card[]};
export type Player = {seat:number;id:number;name:string;life:number;lost:boolean;zones:Record<string,Zone>;mana:{color:number;amount:number}[];counters:Record<string,number>;commanders:{name:string;from:number;damage:number;casts:number}[];lastAction?:{id:number;stackId?:number;kind:string;card:Card;text:string};statuses?:{name:string;title:string;text:string}[]};
export type Decision = {id:string;kind:string;presentation?:'library'|'dock';message:string;min:number;max:number;options:{id:number;label:string;card?:Card;stackId?:number;seat?:number}[]};
export type Target = {kind:'card'|'player'|'zone'|'stack';id?:number;seat:number;zone?:string;name:string};
export type StackItem = {id:number;text:string;card:Card;seat:number;targets?:Target[];ability?:boolean;trigger?:boolean};
export type Game = {controlVersion:number;cancelEndsTurn?:boolean;winnerSeat?:number;message:string;ok:{label:string;enabled:boolean};cancel:{label:string;enabled:boolean};decision?:Decision;turn:number;round:number;phase:string;activeSeat:number;over:boolean;winner:string;players:Player[];stack:StackItem[];combat?:{kind:'attack'|'block';source:Target;target:Target}[];log:string[];sounds?:{id:number;name:string}[]};
export type Room = {id:string;name:string;status:string;error?:string;mySeat:number;aiOnly?:boolean;seats:{name:string;human:boolean;ready:boolean;connected:boolean;deck:string;commander:string}[];game?:Game;chat:{name:string;message:string;at:number}[]};
export type Session = {room:string;seat:number;token:string;invite?:string;resumeKey?:string;server:string};
export type Health = {ready:boolean;engine:string;decks:{id:string;name:string;commander:string}[]};
export const phases:Record<string,string>={UNTAP:'Untap',UPKEEP:'Upkeep',DRAW:'Draw',MAIN1:'Main 1',COMBAT_BEGIN:'Begin combat',COMBAT_DECLARE_ATTACKERS:'Attackers',COMBAT_DECLARE_BLOCKERS:'Blockers',COMBAT_FIRST_STRIKE_DAMAGE:'First strike',COMBAT_DAMAGE:'Combat damage',COMBAT_END:'End combat',MAIN2:'Main 2',END_OF_TURN:'End step',CLEANUP:'Cleanup'};
export const zoneNames:Record<string,string>={Command:'Command',Library:'Library',Graveyard:'Graveyard',Exile:'Exile',Hand:'Hand',Battlefield:'Battlefield'};
export function serverAddress(value:string){const url=new URL(value.trim());if(url.protocol!=='https:'&&!(url.protocol==='http:'&&['localhost','127.0.0.1','[::1]'].includes(url.hostname)))throw new Error('Use an HTTPS address. To play on this computer, http://localhost:8787 also works.');if(url.username||url.password||url.search||url.hash)throw new Error('Enter only the server address, without credentials or query parameters.');return url.origin+url.pathname.replace(/\/$/,'');}
export class ApiError extends Error {constructor(message:string,public status:number){super(message);this.name='ApiError';}}
export async function request<T>(server:string,path:string,token?:string,body?:unknown,signal?:AbortSignal):Promise<T>{
 try {const response=await fetch(server+path,{method:body===undefined?'GET':'POST',headers:{Accept:'application/json',...(token?{Authorization:'Bearer '+token}:{}),...(body!==undefined?{'Content-Type':'application/json'}:{})},body:body===undefined?undefined:JSON.stringify(body),signal:signal??AbortSignal.timeout(15000),cache:'no-store'});const result=await response.json() as {error?:string};if(!response.ok)throw new ApiError(result.error||'The server rejected the request.',response.status);return result as T;}catch(e){if(e instanceof TypeError)throw new Error('Cannot reach the server. Check the address, keep Forge and the tunnel running, and try again.');throw e;}
}
export {SESSION_KEY,rememberSession as saveSession} from './sessions';

type Art={front?:string;back?:string};
const artCache=new Map<string,Promise<Art>>();let artQueue:Promise<unknown>=Promise.resolve();
// One request at a time, including printing-specific token metadata. Repeated tokens share a cache entry.
export function getArt(name:string,tokenImages?:string[]):Promise<Art>{
 const paths=tokenImages?.filter(p=>p&&!p.includes('..')).slice(0,3)??[];
 const key=paths.length?'token:'+paths.join('|'):name.trim();if(!key)return Promise.resolve({});const cached=artCache.get(key);if(cached)return cached;
 const promise=artQueue.catch(()=>{}).then(async()=>{
  const urls=paths.length?paths.map(path=>{const url=new URL('https://api.scryfall.com/cards/'+path);url.searchParams.set('format','json');url.searchParams.delete('version');url.searchParams.delete('face');return url.href}):['https://api.scryfall.com/cards/named?exact='+encodeURIComponent(name)];
  for(const url of urls){try{const r=await fetch(url,{headers:{Accept:'application/json'},signal:AbortSignal.timeout(7000)});if(!r.ok){await new Promise(resolve=>setTimeout(resolve,140));continue;}const c=await r.json() as {image_uris?:{normal?:string};card_faces?:{image_uris?:{normal?:string}}[]};return {front:c.image_uris?.normal??c.card_faces?.[0]?.image_uris?.normal,back:c.card_faces?.[1]?.image_uris?.normal};}catch{}}
  return {};
 });
 artQueue=promise.then(()=>new Promise(resolve=>setTimeout(resolve,140)));artCache.set(key,promise);return promise;
}
