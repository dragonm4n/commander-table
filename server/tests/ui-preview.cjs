/* Compiled UI regression with synthetic API state; requires Playwright + Edge.
 * PLAYWRIGHT_MODULE may point to an installed Playwright package. GPL-3.0-or-later. */
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const assert=require('node:assert/strict');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../web');
const server=http.createServer((req,res)=>{
 const file=path.join(root,req.url==='/'?'index.html':req.url.split('?')[0]);
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const origin=`http://127.0.0.1:${server.address().port}`;
 const browser=await chromium.launch({headless:true,channel:'msedge'});
 try{
 const page=await browser.newPage({viewport:{width:1440,height:900}}),errors=[],actions=[];
 page.on('pageerror',e=>errors.push(e.message));
 const card=(id,name,seat)=>({id,name,creature:true,power:3,toughness:3,type:'Creature',zone:'Battlefield',controllerSeat:seat});
 const attacker=card(11,'Rafiq of the Many',0),blocker=card(12,'Grizzly Bears',1);
 const game={controlVersion:1,cancelEndsTurn:true,message:'Priority: Alice',ok:{label:'OK',enabled:true},cancel:{label:'End Turn',enabled:true},turn:5,round:2,phase:'MAIN1',activeSeat:0,over:false,winner:'',winnerSeat:-1,stack:[],log:[],combat:[],players:[0,1,2,3].map(i=>({id:i,seat:i,name:['Alice','Bob','Carol','Dan'][i],life:40,lost:false,mana:[],counters:{},commanders:[],zones:Object.fromEntries(['Battlefield','Hand','Library','Graveyard','Exile','Command'].map(z=>[z,{count:z==='Battlefield'&&i<2?1:0,cards:z==='Battlefield'?(i===0?[attacker]:i===1?[blocker]:[]):[]}]))}))};
 const room={id:'visual',name:'UI preview',status:'playing',mySeat:0,aiOnly:false,seats:game.players.map(p=>({name:p.name,human:true,ready:true,connected:true,deck:'Test',commander:'Rafiq'})),game,chat:[]};
 await page.route('**/api/**',route=>{
  const request=route.request();
  if(request.url().endsWith('/action')){actions.push(request.postDataJSON());return route.fulfill({json:{ok:true}});}
  return route.fulfill({json:request.url().endsWith('/health')?{ready:true,engine:'Forge',decks:[]}:room});
 });
 await page.route('https://**/*',route=>route.abort());
 await page.addInitScript(origin=>sessionStorage.setItem('commander-table-session-v1',JSON.stringify({server:origin,room:'visual',token:'visual-only',seat:0})),origin);
 await page.goto(origin);
 await page.waitForSelector('.priority-dock.own-turn');
 await page.getByRole('button',{name:'End Turn',exact:true}).click();
 await page.getByRole('dialog').waitFor();assert.equal(actions.length,0);
 await page.getByRole('button',{name:'Keep playing',exact:true}).click();assert.equal(actions.length,0);
 await page.getByRole('button',{name:'End Turn',exact:true}).click();
 await page.getByRole('button',{name:'End my turn',exact:true}).click();
 await page.waitForFunction(()=>!document.querySelector('[role="dialog"]'));
 assert.equal(actions.length,1);assert.equal(actions[0].action,'cancel');
 await page.getByRole('button',{name:'End Turn',exact:true}).click();
 game.controlVersion++;
 await page.waitForFunction(()=>!document.querySelector('[role="dialog"]'));
 assert.equal(actions.length,1,'Stale confirmation submitted an action');
 game.activeSeat=1;game.controlVersion++;
 await page.waitForFunction(()=>!document.querySelector('.priority-dock.own-turn'));
 await page.getByRole('button',{name:'End Turn',exact:true}).click();
 await page.waitForTimeout(100);assert.equal(actions.length,2,'Opponent turn unnecessarily confirmed');

 await page.locator('[data-card-id="11"] .card-zoom').click();
 await page.locator('.floating-card').getByRole('button',{name:'Zoom in card'}).click();
 assert.equal(await page.locator('.floating-card input[type="range"]').inputValue(),'125');
 await page.screenshot({path:path.resolve(__dirname,'../target/ui-zoom-desktop.png')});
 await page.getByRole('button',{name:'Close card',exact:true}).click();

 game.decision={id:'choice-1',kind:'order',presentation:'library',message:'Order these cards.',min:2,max:2,options:[attacker,blocker].map((c,id)=>({id,label:c.name,card:c}))};game.controlVersion++;
 await page.getByRole('dialog').waitFor();
 await page.locator('.decision-option').nth(1).click();await page.locator('.decision-option').nth(0).click();
 await page.getByRole('button',{name:'View battlefield',exact:true}).click();
 await page.waitForFunction(()=>!document.querySelector('[role="dialog"]'));
 assert.equal(await page.getByRole('dialog').count(),0);
 await page.getByRole('button',{name:'Return to choice · 2 selected',exact:true}).click();
 assert.equal(await page.locator('.decision-option.chosen').count(),2);
 assert.equal(await page.locator('.chosen-order>div').first().locator('span').textContent(),'1. Grizzly Bears');
 await page.getByRole('button',{name:'Confirm selection',exact:true}).click();
 await page.waitForTimeout(100);
 assert.deepEqual(actions.at(-1).selected,[1,0]);
 game.decision=undefined;game.controlVersion++;
 await page.waitForFunction(()=>!document.querySelector('[role="dialog"]'));
 // Browsing a zone uses the same zoom/minimize controls, without losing its context.
 game.players[0].zones.Graveyard={count:1,cards:[attacker]};game.controlVersion++;
 await page.waitForFunction(()=>document.querySelector('.seat-0 [data-zone="Graveyard"] b')?.textContent==='1');
 await page.locator('.seat-0 [data-zone="Graveyard"]').click();
 await page.getByRole('dialog').getByRole('button',{name:'Zoom in card'}).click();
 assert.equal(await page.getByRole('dialog').locator('input[type="range"]').inputValue(),'125');
 await page.getByRole('button',{name:'View battlefield',exact:true}).click();
 await page.waitForFunction(()=>!document.querySelector('[role="dialog"]'));
 await page.getByRole('button',{name:'Return to Graveyard · Alice',exact:true}).click();
 await page.getByRole('dialog').getByRole('button',{name:'Close',exact:true}).click();
 await page.waitForFunction(()=>!document.querySelector('[role="dialog"]'));
 game.players[1].lost=true;game.controlVersion++;
 await page.waitForSelector('.seat-1 .player-result.defeated');
 game.over=true;game.winner='Alice';game.winnerSeat=0;room.status='finished';game.controlVersion++;
 await page.waitForSelector('.seat-0 .player-result.victorious');
 assert.equal(await page.locator('.match-result strong').textContent(),'Alice wins!');
 await page.setViewportSize({width:390,height:844});
 await page.screenshot({path:path.resolve(__dirname,'../target/ui-outcome-mobile.png')});
 assert.deepEqual(errors,[]);
 console.log('PASS: end-turn confirmation/cancel/stale/opponent turn, zoom, minimized ordered choice, defeat and victory; no browser errors');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>server.close());
