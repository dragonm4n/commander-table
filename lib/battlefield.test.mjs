import test from 'node:test';
import assert from 'node:assert/strict';
import {battlefieldPiles} from './battlefield.ts';
const card=(id,more={})=>({id,name:'Soldier',token:true,power:1,toughness:1,type:'Creature — Soldier',...more});
const player=(seat,cards)=>({seat,zones:{Battlefield:{count:cards.length,cards}}});
test('identical tokens form an interactive group with every individual ID',()=>{
 const piles=battlefieldPiles([player(0,[1,2,3,4,5].map(id=>card(id)))]).get(0);
 assert.equal(piles.length,1);assert.deepEqual(piles[0].members.map(c=>c.id),[1,2,3,4,5]);
});
test('tokens with different relevant game states or controllers remain separate',()=>{
 const variants=[{}, {tapped:true},{damage:1},{counters:{'+1/+1':1}},{attacking:true},{blocking:true},{sick:true},{selected:true},{selectable:true},{actionable:true},{power:2},{text:'Vigilance'},{tokenImages:['tset/2']},{transformed:true}];
 const piles=battlefieldPiles([player(0,variants.map((v,i)=>card(i,v))),player(1,[card(100)])]);
 assert.equal(piles.get(0).length,variants.length);assert.equal(piles.get(1).length,1);
});
test('equipment and foreign-controlled auras appear beneath their host, nested attachments included',()=>{
 const host=card(1,{token:false}),equip=card(2,{token:false,name:'Equipment',attachedTo:{kind:'card',id:1}}),aura=card(3,{token:false,name:'Aura',controllerSeat:1,attachedTo:{kind:'card',id:1}}),nested=card(4,{token:false,attachedTo:{kind:'card',id:2}});
 const piles=battlefieldPiles([player(0,[host,equip,nested]),player(1,[aura])]);
 assert.equal(piles.get(0).length,1);assert.deepEqual(piles.get(0)[0].attachments.map(c=>c.id),[2,4,3]);assert.equal(piles.get(1)?.length??0,0);
 assert.equal(piles.get(0)[0].attachments[2].controllerSeat,1);
});
test('attached tokens, face-down cards and tokens bearing attachments never merge',()=>{
 const piles=battlefieldPiles([player(0,[card(1),card(2),card(3,{hidden:true}),card(4,{hidden:true}),card(5,{token:false,attachedTo:{kind:'card',id:1}}),card(6,{attachedTo:{kind:'player',seat:1}})])]).get(0);
 assert.equal(piles.length,5);assert.equal(piles.find(p=>p.root.id===1).attachments.length,1);
});
test('detached cards and cards whose host is missing stay visible as independent permanents',()=>{
 const piles=battlefieldPiles([player(0,[card(1,{token:false}),card(2,{token:false,attachedTo:{kind:'card',id:99}})])]).get(0);
 assert.deepEqual(piles.map(p=>p.root.id),[1,2]);
});
