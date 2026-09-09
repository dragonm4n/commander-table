"""Four native AIs, read-only host, elimination and retained victory state.
Usage: python spectator.py PACKAGE SCENARIO_CLASSES
"""
import json, os, secrets, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path
package, classes = map(lambda p: Path(p).resolve(), sys.argv[1:3])
key=secrets.token_urlsafe(24)
log=Path('server/target/spectator.log')
cp=os.pathsep.join([str(classes),str(package/'commander-table.jar'),str(package/'lib/*')])
process=subprocess.Popen(['java','-Xmx3G','-cp',cp,'table.SpectatorScenarioServer','--assets',str(package/'forge'),'--port','8793'],stdin=subprocess.PIPE,stdout=log.open('w'),stderr=subprocess.STDOUT,text=True,env={**os.environ,'TABLE_ADMIN_KEY':key})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
announcements={}
def call(path,token=None,body=None):
 req=urllib.request.Request('http://127.0.0.1:8793'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 with http.open(req,timeout=10) as response:return json.load(response)
def rejected(path,token,body):
 try:call(path,token,body)
 except urllib.error.HTTPError as error:assert error.code==400;return
 raise AssertionError('Unauthorized action accepted')
def wait(predicate,timeout=90):
 until=time.time()+timeout
 while time.time()<until:
  r=call(base+'/state',host['token'])
  for player in r.get('game',{}).get('players',[]):
   action=player.get('lastAction')
   if action:announcements[action['id']]=action['at']
  assert r['status']!='error',r.get('error')
  if predicate(r):return r
  time.sleep(.2)
 raise AssertionError(f"Timed out: status={r['status']}, turn={r.get('game',{}).get('turn')}, over={r.get('game',{}).get('over')}, message={r.get('game',{}).get('message')}")
try:
 for _ in range(160):
  assert process.poll() is None,'Server exited'
  try:call('/api/health');break
  except OSError:time.sleep(.25)
 host=call('/api/rooms',key,{'name':'Spectator test','player':'Observer'})
 base='/api/rooms/'+host['room']
 guest=call(base+'/join',body={'player':'Guest','invite':host['invite']})
 rejected(base+'/mode',guest['token'],{'aiOnly':True})
 rejected(base+'/mode',host['token'],{'aiOnly':True})
 call(base+'/leave',guest['token'],{})
 r=call(base+'/mode',host['token'],{'aiOnly':True})
 assert r['aiOnly'] and not any(s['human'] for s in r['seats'])
 rejected(base+'/join',None,{'player':'Guest','invite':host['invite']})
 call(base+'/start',host['token'],{})
 r=wait(lambda r:r.get('game',{}).get('turn',0)>=2)
 rejected(base+'/action',host['token'],{'action':'concede','requestId':secrets.token_hex(12),'controlVersion':r['game']['controlVersion']})
 for p in r['game']['players']:
  assert not p['zones']['Hand']['cards'] and not p['zones']['Library']['cards']
 visible=[c for p in r['game']['players'] for c in p['zones']['Battlefield']['cards']]
 assert visible and any(not c['hidden'] for c in visible),'Spectator battlefield hidden'
 print('PASS: four AIs advance with a read-only host; public board and private hands',flush=True)
 process.stdin.write('monarch0\n');process.stdin.flush()
 r=wait(lambda r:any(p.get('monarch') and p['seat']==0 for p in r.get('game',{}).get('players',[])),180)
 process.stdin.write('monarch1\n');process.stdin.flush()
 r=wait(lambda r:any(p.get('monarch') and p['seat']==1 for p in r.get('game',{}).get('players',[])),180)
 assert sum(bool(p.get('monarch')) for p in r['game']['players'])==1
 print('PASS: monarch crown follows the native game owner',flush=True)
 wait(lambda r:len(announcements)>=2,180)
 times=sorted(announcements.values())
 assert len(times)>=2,'No cast announcements observed'
 assert all(b-a>=2900 for a,b in zip(times,times[1:])),times
 print('PASS: cast/ability announcements have at least three seconds between them',flush=True)
 process.stdin.write('eliminate\n');process.stdin.flush()
 r=wait(lambda r:any(p['seat']==1 and p['lost'] for p in r.get('game',{}).get('players',[])))
 assert len(r['game']['players'])==4,'Eliminated player disappeared from projection'
 print('PASS: defeated player remains in its battlefield seat',flush=True)
 process.stdin.write('finish\n');process.stdin.flush()
 r=wait(lambda r:r['status']=='finished' and r.get('game',{}).get('over'))
 assert r['game']['winnerSeat']==0 and r['game']['winner']
 time.sleep(1)
 r=call(base+'/state',host['token'])
 assert r['status']=='finished' and r['game']['winnerSeat']==0
 print('PASS: winner and final table survive AI match cleanup',flush=True)
 call(base+'/restart',host['token'],{})
 r=wait(lambda r:r['status']=='lobby')
 assert 'game' not in r and r['id']==host['room'] and r['aiOnly']
 call(base+'/start',host['token'],{})
 r=wait(lambda r:r['status']=='playing' and r.get('game',{}).get('turn',0)>0)
 call(base+'/restart',host['token'],{})
 r=wait(lambda r:r['status']=='lobby')
 assert 'game' not in r
 print('PASS: finished and active AI matches restart in the same authenticated room',flush=True)
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 errors=[line for line in log.read_text(errors='replace').splitlines() if 'Exception' in line or 'Projection:' in line or 'Error in ' in line]
 assert not errors,errors
