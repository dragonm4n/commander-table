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
  assert r['status']!='error',r.get('error')
  if predicate(r):return r
  time.sleep(.2)
 raise AssertionError('Timed out waiting for spectator state')
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
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
