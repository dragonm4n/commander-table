import test from 'node:test';
import assert from 'node:assert/strict';
import {activeSession,rememberSession,rememberedSession,disconnectSession,forgetSession,joinKey,privateRejoinLink} from './sessions.ts';
class Storage {data=new Map();getItem(k){return this.data.get(k)??null;}setItem(k,v){this.data.set(k,v);}removeItem(k){this.data.delete(k);}}
const session={server:'https://table.example',room:'room',token:'private-token',seat:2,resumeKey:'a'.repeat(32)};
test('closing the tab or disconnecting preserves the same seat and credential for another tab',()=>{
 const disk=new Storage(),tab=new Storage();rememberSession(session,disk,tab);assert.deepEqual(activeSession(tab),session);
 disconnectSession(tab);assert.equal(activeSession(tab),null);assert.deepEqual(rememberedSession(session.server,session.room,disk),session);
 const newTab=new Storage();rememberSession(rememberedSession(session.server,session.room,disk),disk,newTab);assert.deepEqual(activeSession(newTab),session);
});
test('join key is persisted before the HTTP call and remains identical across retries',()=>{
 const disk=new Storage();const first=joinKey(session.server,'new-room',disk);assert.match(first,/^[a-zA-Z0-9_-]{32,80}$/);assert.equal(joinKey(session.server,'new-room',disk),first);
 rememberSession(session,disk,new Storage());assert.equal(joinKey(session.server,session.room,disk),session.resumeKey);
});
test('servers and rooms have separate saved sessions; a refreshed credential replaces its predecessor',()=>{
 const disk=new Storage(),tab=new Storage(),other={...session,server:'https://other.example',token:'other'};
 rememberSession(session,disk,tab);rememberSession(other,disk,tab);rememberSession({...session,token:'updated'},disk,tab);
 assert.equal(rememberedSession(session.server,session.room,disk).token,'updated');assert.equal(rememberedSession(other.server,other.room,disk).token,'other');assert.equal(rememberedSession(session.server,'missing',disk),null);
});
test('explicit release forgets the seat and invalidates the pending join key without deleting other rooms',()=>{
 const disk=new Storage(),tab=new Storage(),other={...session,room:'other',token:'other'};
 const key=joinKey(session.server,session.room,disk);rememberSession(session,disk,tab);rememberSession(other,disk,tab);forgetSession(session,disk,tab);
 assert.equal(rememberedSession(session.server,session.room,disk),null);assert.notEqual(joinKey(session.server,session.room,disk),key);assert.deepEqual(activeSession(tab),other);
});
test('private rejoin link carries only the seat recovery secret in the fragment',()=>{
 const link=new URL(privateRejoinLink(session)),hash=new URLSearchParams(link.hash.slice(1));assert.equal(link.search,'');assert.equal(hash.get('resume'),session.resumeKey);assert.equal(hash.get('room'),session.room);assert.equal(link.href.includes(session.token),false);
});
test('corrupt storage is recoverable and a failed persistent write cannot replace the active seat',()=>{
 const disk=new Storage(),tab=new Storage();disk.setItem('commander-table-connections-v2','broken');disk.setItem('commander-table-join-keys-v1','null');assert.equal(rememberedSession(undefined,undefined,disk),null);forgetSession(session,disk,tab);assert.ok(joinKey(session.server,session.room,disk));
 rememberSession(session,disk,tab);disk.setItem=()=>{throw new Error('Full');};assert.throws(()=>rememberSession({...session,token:'replacement'},disk,tab));assert.equal(activeSession(tab).token,session.token);
});
