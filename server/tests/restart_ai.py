"""Repeated early AI-match restarts using the production JAR, retaining the host session."""
import json,os,secrets,subprocess,sys,time,urllib.request
from pathlib import Path
package=Path(sys.argv[1]).resolve();key=secrets.token_urlsafe(24)
log=Path('server/target/restart-ai.log')
process=subprocess.Popen(['java','-Xmx2G','-jar',str(package/'commander-table.jar'),'--assets',str(package/'forge'),'--port','8795'],stdout=log.open('w'),stderr=subprocess.STDOUT,env={**os.environ,'TABLE_ADMIN_KEY':key})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,token=None,body=None):
 req=urllib.request.Request('http://127.0.0.1:8795'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 with http.open(req,timeout=10) as response:return json.load(response)
def wait(predicate):
 until=time.time()+40
 while time.time()<until:
  r=call(base+'/state',host['token'])
  if predicate(r):return r
  time.sleep(.2)
 subprocess.run(['jstack',str(process.pid)],stdout=Path('server/target/restart-ai-threads.txt').open('w'),timeout=10)
 raise AssertionError(f"Restart stalled: {r['status']}, {r.get('game',{}).get('turn')}, {r.get('game',{}).get('over')}")
try:
 for _ in range(200):
  try:call('/api/health');break
  except OSError:time.sleep(.2)
 host=call('/api/rooms',key,{'name':'Restart test','player':'Host'});base='/api/rooms/'+host['room']
 call(base+'/mode',host['token'],{'aiOnly':True})
 for i in range(3):
  call(base+'/start',host['token'],{})
  wait(lambda r:r['status']=='playing' and r.get('game',{}).get('turn',0)>0)
  print('Started AI match',i+1,flush=True)
  call(base+'/restart',host['token'],{})
  r=wait(lambda r:r['status']=='lobby')
  assert 'game' not in r
  print('PASS: AI match',i+1,'returned to the same lobby',flush=True)
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 errors=[s for s in log.read_text(errors='replace').splitlines() if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 assert not errors,errors
