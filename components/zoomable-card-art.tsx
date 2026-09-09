"use client";
import {useState} from 'react';
import {ZoomIn,ZoomOut} from 'lucide-react';
import {CardArt} from './table-card';
import type {Card} from '@/lib/table';

export default function ZoomableCardArt({card}:{card:Card}){
 const [zoom,setZoom]=useState(100);
 return <div className="zoomable-art">
  <div className="art-viewport"><div className="art-scale" style={{width:`${zoom}%`}}><CardArt card={card} large/></div></div>
  <div className="art-zoom-controls"><button aria-label="Zoom out card" disabled={zoom<=100} onClick={()=>setZoom(z=>Math.max(100,z-25))}><ZoomOut size={16}/></button><input aria-label="Card image zoom" type="range" min="100" max="250" step="25" value={zoom} onChange={e=>setZoom(Number(e.target.value))}/><output>{zoom}%</output><button aria-label="Zoom in card" disabled={zoom>=250} onClick={()=>setZoom(z=>Math.min(250,z+25))}><ZoomIn size={16}/></button></div>
 </div>;
}
