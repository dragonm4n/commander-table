import test from 'node:test';
import assert from 'node:assert/strict';
import {combatArrows} from './combat.ts';
const card=id=>({kind:'card',id,seat:0,name:`Card ${id}`});
const defender={kind:'player',id:9,seat:1,name:'Defender'};
const attack={kind:'attack',source:card(1),target:defender};
const block=id=>({kind:'block',source:card(id),target:card(1)});

test('unblocked attack keeps its actual defender, including planeswalkers',()=>{
 assert.deepEqual(combatArrows([attack]),[attack]);
 const walker={...attack,target:card(9)};
 assert.deepEqual(combatArrows([walker]),[walker]);
});
test('provisional block redirects red arrow without changing the engine data',()=>{
 const combat=[attack,block(2)];
 assert.deepEqual(combatArrows(combat),[{...attack,target:card(2)},block(2)]);
 assert.equal(combat[0].target,defender);
 assert.deepEqual(combatArrows([attack]),[attack]);
});
test('multiple blockers each receive an attack arrow; unrelated attackers stay intact',()=>{
 const other={...attack,source:card(7)};
 assert.deepEqual(combatArrows([attack,block(2),block(3),other]),[
  {...attack,target:card(2)},{...attack,target:card(3)},block(2),block(3),other
 ]);
 assert.deepEqual(combatArrows(),[]);
});
