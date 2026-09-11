"""Native series lifecycle with deterministic outcomes; not a strength benchmark."""
import json,os,secrets,subprocess,sys,time,urllib.request,urllib.error
from pathlib import Path
pkg=Path(sys.argv[1]).resolve();classes=Path(sys.argv[2]).resolve();key=secrets.token_urlsafe(24)
log=Path('server/target/ai-series-engine.log')
cp=os.pathsep.join([str(classes),str(pkg/'commander-table.jar'),str(pkg/'lib/*')])
process=subprocess.Popen(['java','-Xmx3G','-cp',cp,'table.SeriesScenarioServer','--assets',str(pkg/'forge'),'--port','8796'],stdout=log.open('w',encoding='utf-8'),stderr=subprocess.STDOUT,env={**os.environ,'TABLE_ADMIN_KEY':key})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,token=None,body=None):
 req=urllib.request.Request('http://127.0.0.1:8796'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 with http.open(req,timeout=10) as r:return json.load(r)
def wait(host,predicate,timeout=80):
 end=time.time()+timeout
 while time.time()<end:
  r=call('/api/rooms/'+host['room']+'/state',host['token'])
  assert r['status']!='error',r.get('error')
  if predicate(r):return r
  time.sleep(.15)
 raise AssertionError(('Timeout',r['status'],r.get('series')))
try:
 for _ in range(200):
  try:call('/api/health');break
  except OSError:time.sleep(.2)
 host=call('/api/rooms',key,{'name':'Series test','player':'Host'});base='/api/rooms/'+host['room']
 call(base+'/mode',host['token'],{'aiOnly':True})
 names=['Alpha','Beta','Gamma','Delta']
 call(base+'/start',host['token'],{'seriesGames':3,'fast':True,'aiNames':names})
 r=wait(host,lambda r:r.get('series',{}).get('completed')==3)
 assert r['status']=='finished' and not r['series']['running']
 assert [s['name'] for s in r['seats']]==names
 assert [p['wins'] for p in r['series']['players']]==[1,1,0,0]
 assert r['series']['draws']==1 and all(p['games']==3 for p in r['series']['players'])
 time.sleep(3)
 r=call(base+'/state',host['token']);assert r['status']=='finished' and r['series']['completed']==3,'Unexpected native replay after series completion/draw'
 print('PASS: three native games including a draw, custom names, scores and exact stop count',flush=True)
 call(base+'/restart',host['token'],{});r=wait(host,lambda r:r['status']=='lobby');assert r['series']['completed']==3
 print('PASS: lobby retains results and authenticated room',flush=True)
 stop=host;base='/api/rooms/'+stop['room']
 call(base+'/mode',stop['token'],{'aiOnly':True})
 call(base+'/start',stop['token'],{'seriesGames':20,'fast':False,'aiNames':names})
 wait(stop,lambda r:r['status']=='playing')
 call(base+'/series-stop',stop['token'],{});r=call(base+'/state',stop['token']);assert r['series']['stopRequested']
 call(base+'/restart',stop['token'],{});r=wait(stop,lambda r:r['status']=='lobby')
 assert not r['series']['running'] and r['series']['completed']==0 and all(p['wins']==0 for p in r['series']['players'])
 print('PASS: stop request and interruption preserve results without a false winner',flush=True)
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 errors=[s for s in log.read_text(encoding='utf-8',errors='replace').splitlines() if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 assert not errors,errors
