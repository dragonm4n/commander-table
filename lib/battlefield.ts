import type {Card,Player} from './table';
export type PermanentPile={root:Card;attachments:Card[];members:Card[]};
function tokenKey(card:Card){
 const counters=Object.entries(card.counters??{}).sort(([a],[b])=>a.localeCompare(b));
 return JSON.stringify([card.name,card.imageName,card.tokenImages,card.type,card.text,card.mana,card.power,card.toughness,card.loyalty,!!card.tapped,!!card.sick,card.damage??0,counters,!!card.attacking,!!card.blocking,!!card.transformed,card.controllerSeat,!!card.selected,!!card.selectable,!!card.actionable]);
}
export function battlefieldPiles(players:Player[]):Map<number,PermanentPile[]> {
 const entries=players.flatMap(p=>(p.zones.Battlefield?.cards??[]).map(card=>({card,seat:p.seat}))),byId=new Map(entries.map(e=>[e.card.id,e])),children=new Map<number,Card[]>();
 const parent=(card:Card)=>card.attachedTo?.kind==='card'&&byId.has(card.attachedTo.id!)?card.attachedTo.id:undefined;
 for(const {card} of entries){const host=parent(card);if(host!==undefined&&host!==card.id)children.set(host,[...(children.get(host)??[]),card]);}
 const attached=new Set<number>(),piles=new Map<number,PermanentPile[]>(),grouped=new Map<string,PermanentPile>();
 const add=(card:Card,seat:number)=>{
  const family:Card[]=[],seen=new Set([card.id]);
  const walk=(id:number)=>{for(const child of children.get(id)??[])if(!seen.has(child.id)){seen.add(child.id);attached.add(child.id);family.push(child);walk(child.id);}};walk(card.id);
  const pile={root:card,attachments:family,members:[card]};
  const key=card.token&&!card.hidden&&!card.attachedTo&&!family.length?seat+'|'+tokenKey(card):null;
  if(key&&grouped.has(key)){grouped.get(key)!.members.push(card);return;}
  if(key)grouped.set(key,pile);
  piles.set(seat,[...(piles.get(seat)??[]),pile]);
 };
 for(const {card,seat} of entries)if(parent(card)===undefined)add(card,seat);
 // Transitional snapshots must not drop a permanent even if a host is missing.
 for(const {card,seat} of entries)if(parent(card)!==undefined&&!attached.has(card.id)&&![...piles.values()].flat().some(p=>p.root.id===card.id))add(card,seat);
 return piles;
}
