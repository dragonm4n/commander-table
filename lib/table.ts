export type Card = {id:number;name:string;hidden?:boolean;imageName?:string;type?:string;text?:string;mana?:string;power?:number;toughness?:number;loyalty?:string;creature?:boolean;land?:boolean;tapped?:boolean;attacking?:boolean;blocking?:boolean;selected?:boolean;selectable?:boolean;actionable?:boolean;sick?:boolean;token?:boolean;commander?:boolean;transformed?:boolean;damage?:number;counters?:Record<string,number>;zone?:string;tokenImages?:string[]};
export type Zone = {count:number;cards:Card[]};
export type Player = {seat:number;id:number;name:string;life:number;lost:boolean;zones:Record<string,Zone>;mana:{color:number;amount:number}[];counters:Record<string,number>;commanders:{name:string;from:number;damage:number;casts:number}[];lastAction?:{id:number;kind:string;card:Card;text:string};statuses?:{name:string;title:string;text:string}[]};
export type Decision = {id:string;kind:string;message:string;min:number;max:number;options:{id:number;label:string;card?:Card;stackId?:number;seat?:number}[]};
export type Target = {kind:'card'|'player'|'zone'|'stack';id?:number;seat:number;zone?:string;name:string};
export type StackItem = {id:number;text:string;card:Card;seat:number;targets?:Target[];ability?:boolean;trigger?:boolean};
export type Game = {controlVersion:number;message:string;ok:{label:string;enabled:boolean};cancel:{label:string;enabled:boolean};decision?:Decision;turn:number;phase:string;activeSeat:number;over:boolean;winner:string;players:Player[];stack:StackItem[];combat?:{kind:'attack'|'block';source:Target;target:Target}[];log:string[];sounds?:{id:number;name:string}[]};
export type Room = {id:string;name:string;status:string;error?:string;mySeat:number;seats:{name:string;human:boolean;ready:boolean;connected:boolean;deck:string;commander:string}[];game?:Game;chat:{name:string;message:string;at:number}[]};
export type Session = {room:string;seat:number;token:string;invite?:string;server:string};
export type Health = {ready:boolean;engine:string;decks:{id:string;name:string;commander:string}[]};
export const phases:Record<string,string>={UNTAP:'Desvirar',UPKEEP:'Manutenção',DRAW:'Compra',MAIN1:'Principal 1',COMBAT_BEGIN:'Início do combate',COMBAT_DECLARE_ATTACKERS:'Atacantes',COMBAT_DECLARE_BLOCKERS:'Bloqueadores',COMBAT_FIRST_STRIKE_DAMAGE:'Dano de iniciativa',COMBAT_DAMAGE:'Dano de combate',COMBAT_END:'Fim do combate',MAIN2:'Principal 2',END_OF_TURN:'Etapa final',CLEANUP:'Limpeza'};
export const zoneNames:Record<string,string>={Command:'Comando',Library:'Grimório',Graveyard:'Cemitério',Exile:'Exílio',Hand:'Mão',Battlefield:'Campo de batalha'};
export function serverAddress(value:string){const url=new URL(value.trim());if(url.protocol!=='https:'&&!(url.protocol==='http:'&&['localhost','127.0.0.1','[::1]'].includes(url.hostname)))throw new Error('Use um endereço HTTPS. Para jogar neste computador, http://localhost:8787 também funciona.');if(url.username||url.password||url.search||url.hash)throw new Error('Informe somente o endereço do servidor, sem senha ou parâmetros.');return url.origin+url.pathname.replace(/\/$/,'');}
export async function request<T>(server:string,path:string,token?:string,body?:unknown,signal?:AbortSignal):Promise<T>{
 try {const response=await fetch(server+path,{method:body===undefined?'GET':'POST',headers:{Accept:'application/json',...(token?{Authorization:'Bearer '+token}:{}),...(body!==undefined?{'Content-Type':'application/json'}:{})},body:body===undefined?undefined:JSON.stringify(body),signal:signal??AbortSignal.timeout(15000),cache:'no-store'});const result=await response.json() as {error?:string};if(!response.ok)throw new Error(result.error||'O servidor recusou a solicitação.');return result as T;}catch(e){if(e instanceof TypeError)throw new Error('Sem conexão com o servidor. Confira o endereço, mantenha o Forge e o túnel abertos e tente novamente.');throw e;}
}
export const SESSION_KEY='commander-table-session-v1';
export function saveSession(value:Session){sessionStorage.setItem(SESSION_KEY,JSON.stringify(value));}

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
