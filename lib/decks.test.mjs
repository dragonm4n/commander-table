import test from 'node:test';
import assert from 'node:assert/strict';
import {readDecks,saveImportedDeck,importDeckBackup,exportDeckBackup} from './decks.ts';

class BrowserStorage {
 data=new Map();
 getItem(key){return this.data.get(key)??null;}
 setItem(key,value){this.data.set(key,value);}
}
const deck={name:'My Muzzio',commander:'Muzzio, Visionary Architect',list:'1 Sol Ring\n98 Island'};

test('saved import survives a new storage reader and updating it does not duplicate the list',()=>{
 const disk=new BrowserStorage();
 const saved=saveImportedDeck(deck,disk);
 assert.deepEqual(readDecks(disk),saved);
 const renamed=saveImportedDeck({...deck,name:'Muzzio artifacts'},disk);
 assert.equal(renamed.length,1);
 assert.equal(renamed[0].id,saved[0].id);
 assert.equal(readDecks(disk)[0].name,'Muzzio artifacts');
});

test('backup restores the full list on another browser and merges without duplicating decks',()=>{
 const first=new BrowserStorage(),second=new BrowserStorage();
 const original=saveImportedDeck(deck,first),backup=exportDeckBackup(original);
 saveImportedDeck({...deck,name:'Other list',list:'99 Island'},second);
 const restored=importDeckBackup(backup,second);
 assert.equal(restored.length,2);
 const found=restored.find(d=>d.list===deck.list);
 assert.equal(found.commander,deck.commander);
 assert.equal(found.name,deck.name);
 assert.notEqual(found.id,original[0].id);
 assert.deepEqual(importDeckBackup(backup,second),restored);
});

test('invalid backup or full storage preserves existing decks',()=>{
 const disk=new BrowserStorage(),original=saveImportedDeck(deck,disk);
 const invalid=JSON.stringify({format:'commander-table-decks',version:1,decks:[{...original[0],list:''}]});
 assert.throws(()=>importDeckBackup(invalid,disk));
 assert.deepEqual(readDecks(disk),original);
 disk.setItem=()=>{throw new Error('QuotaExceededError');};
 assert.throws(()=>saveImportedDeck({...deck,list:'99 Island'},disk));
 assert.deepEqual(readDecks(disk),original);
});
