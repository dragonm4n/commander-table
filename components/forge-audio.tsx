"use client";
import {useEffect,useRef,useState} from 'react';
import {Volume2,VolumeX} from 'lucide-react';
import {Game} from '@/lib/table';

export default function ForgeAudio({game,roomId}:{game?:Game;roomId?:string}){
 const [enabled,setEnabled]=useState(false),[error,setError]=useState('');const context=useRef<AudioContext|null>(null),cursor=useRef(0),buffers=useRef(new Map<string,Promise<AudioBuffer>>()),recent=useRef(new Map<string,number>());
 useEffect(()=>{cursor.current=0;},[roomId]);
 useEffect(()=>()=>{void context.current?.close();},[]);
 const toggle=async()=>{if(enabled){setEnabled(false);await context.current?.suspend();return;}try{const ctx=context.current??new AudioContext();context.current=ctx;await ctx.resume();setError('');setEnabled(true);}catch{setError('The browser could not enable audio.');}};
 useEffect(()=>{
  const sounds=game?.sounds??[],last=sounds.at(-1)?.id??0,previous=cursor.current;cursor.current=last;
  if(!enabled||!previous||!context.current)return;
  const ctx=context.current;
  for(const sound of sounds.filter(s=>s.id>previous).slice(-8)){
   if(!/^[a-z_]+$/.test(sound.name))continue;const now=Date.now();if(now-(recent.current.get(sound.name)||0)<200)continue;recent.current.set(sound.name,now);
   let buffer=buffers.current.get(sound.name);if(!buffer){buffer=fetch(`/sounds/${sound.name}.mp3`).then(r=>{if(!r.ok)throw new Error('Audio unavailable');return r.arrayBuffer()}).then(b=>ctx.decodeAudioData(b));buffers.current.set(sound.name,buffer);}
   buffer.then(b=>{if(ctx.state!=='running')return;const source=ctx.createBufferSource(),gain=ctx.createGain();source.buffer=b;gain.gain.value=.25;source.connect(gain);gain.connect(ctx.destination);source.start();}).catch(()=>{});
  }
 },[game?.sounds,enabled]);
 return <button onClick={toggle} aria-pressed={enabled} title={error||'Enable or mute Forge sound effects'} aria-label={enabled?'Mute Forge sound effects':'Enable Forge sound effects'}>{enabled?<Volume2 size={16}/>:<VolumeX size={16}/>}<span>Sound</span></button>;
}
