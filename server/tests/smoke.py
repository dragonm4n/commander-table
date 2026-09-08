"""Exercise the real Forge process through HTTP, without a browser.
Usage: python smoke.py PATH_TO_JAR PATH_TO_FORGE_ASSETS
"""
import json, os, secrets, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path
jar,assets=sys.argv[1:3]
admin=secrets.token_urlsafe(24)
log=Path('/workspace/table-integration.log') if Path('/workspace').exists() else Path('table-integration.log')
process=subprocess.Popen(['java','-Xmx3G','-jar',jar,'--assets',assets,'--port','8788'],stdout=log.open('w'),stderr=subprocess.STDOUT,env={**os.environ,'TABLE_ADMIN_KEY':admin})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,token=None,body=None):
 req=urllib.request.Request('http://127.0.0.1:8788'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with http.open(req,timeout=10) as r:return json.load(r)
 except urllib.error.HTTPError as e:raise AssertionError(f'{path}: {e.code} {e.read().decode()}')
def state(s):return call('/api/rooms/'+s['room']+'/state',s['token'])
def action(s,g,**body):return call('/api/rooms/'+s['room']+'/action',s['token'],{'controlVersion':g['controlVersion'],'requestId':secrets.token_hex(12),**body})
try:
 for _ in range(100):
  if process.poll() is not None:raise RuntimeError('Forge exited on startup')
  try:health=call('/api/health');break
  except (OSError,AssertionError):time.sleep(.3)
 else:raise RuntimeError('Forge did not become ready')
 print('HEALTH:',health['engine'],len(health['decks']),'precons',flush=True)
 try:call('/api/rooms','incorrect-key',{})
 except AssertionError as e:assert '403' in str(e)
 else:raise AssertionError('Unauthenticated room creation was allowed')
 host=call('/api/rooms',admin,{'name':'Integration table','player':'Human A'})
 guest=call('/api/rooms/'+host['room']+'/join',body={'player':'Human B','invite':host['invite']})
 assert guest['seat']==1 and not guest.get('invite')
 for s in [host,guest]:call('/api/rooms/'+s['room']+'/deck',s['token'],{'deck':'Feline Ferocity'})
 call('/api/rooms/'+host['room']+'/start',host['token'],{})
 ai_played=False;human_land=False;seen_decisions=set();actions=0;seen=[]
 until=time.time()+100
 while time.time()<until:
  for s in [host,guest]:
   r=state(s)
   assert r['status']!='error',r.get('error')
   g=r.get('game',{})
   if not g.get('players'):continue
   for p in g['players']:
    if p['seat']!=s['seat']:assert p['zones']['Hand']['cards']==[], 'PRIVATE HAND LEAK'
    assert p['zones']['Library']['cards']==[], 'PRIVATE LIBRARY LEAK'
   own=next(p for p in g['players'] if p['seat']==s['seat'])
   if any(p['seat']>=2 and p['zones']['Battlefield']['cards'] for p in g['players']):ai_played=True
   d=g.get('decision');verb=None
   if d:
    if d['id'] in seen_decisions:continue
    seen_decisions.add(d['id']);print('CHOICE',s['seat'],d['kind'],d['message'][:95],[o['label'][:60] for o in d['options'][:5]],flush=True)
    verb={'action':'answer','decisionId':d['id']}
    if d['kind']=='allocation':verb['values']=[d['max']]+[0]*(len(d['options'])-1)
    elif d['kind']=='number':verb['value']=d['min']
    elif d['kind']=='text':verb['value']='Test'
    else:
     ids=[o['id'] for o in d['options'][:d['min']]]
     verb['selected']=ids
   elif r['status']=='playing':
    signature=(s['seat'],g.get('turn'),g.get('phase'),g.get('message'),g.get('ok'))
    label=str(signature)
    if label not in seen:
     seen.append(label)
     if len(seen)<30:print('INPUT',s['seat'],g.get('turn'),g.get('phase'),g.get('message','')[:120],g.get('ok'),flush=True)
    if 'Who would you like to start' in g.get('message',''):
     verb={'action':'player','playerId':own['id']}
    if g.get('activeSeat')==s['seat'] and g.get('phase')=='MAIN1':
     land=next((c for c in own['zones']['Hand']['cards'] if c.get('land') ),None)
     if land and not any(c.get('land') for c in own['zones']['Battlefield']['cards']):verb={'action':'card','cardId':land['id']}
    if not verb and 'discard' in g.get('message','').lower():
     discard=next((c for c in own['zones']['Hand']['cards'] if c.get('selectable') and not c.get('selected')),None)
     if discard:verb={'action':'card','cardId':discard['id']}
    if not verb and g.get('ok',{}).get('enabled'):verb={'action':'ok'}
    elif not verb and g.get('cancel',{}).get('enabled'):verb={'action':'cancel'}
    human_land=human_land or any(c.get('land') for c in own['zones']['Battlefield']['cards'])
   if verb:
    try:action(s,g,**verb);actions+=1
    except AssertionError as e:
     if not any(x in str(e) for x in ['changed','update','Wait','choice','answered']):print('ACTION ERROR',str(e)[:250],flush=True)
  if ai_played and human_land:
   print('PASS: two human clients + two native AIs; private hands/libraries; human land play; AI permanents;',actions,'actions',flush=True)
   break
  time.sleep(.35)
 else:
  for s in [host,guest]:
   r=state(s);g=r.get('game',{});print('FINAL',s['seat'],r['status'],g.get('turn'),g.get('phase'),g.get('message'), 'players',len(g.get('players',[])),flush=True)
  raise AssertionError(f'Game did not reach expected actions: ai_played={ai_played}, human_land={human_land}')
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 lines=[s for s in log.read_text(errors='replace').splitlines() if 'Host key:' not in s and 'Chave do anfitrião:' not in s]
 errors=[i for i,s in enumerate(lines) if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 if errors:print('FORGE ERRORS:\n'+'\n'.join(lines[errors[0]:errors[0]+40]),flush=True)

 assert not errors,'Forge reported an error in the smoke regression'
