"""Real human-controller casting, ETB, targeting and expiry regression over HTTP.
Usage: python etb.py JAR ASSETS SCENARIO_CLASSES
Compile ScenarioServer.java separately; fixture entry point is never shipped in the JAR.
"""
import json, os, secrets, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path

jar,assets,classes=sys.argv[1:4]
admin=secrets.token_urlsafe(24)
log=Path('/workspace/table-etb.log') if Path('/workspace').exists() else Path('table-etb.log')
cp=os.pathsep.join([classes,jar,str(Path(jar).parent/'lib/*')])
process=subprocess.Popen(['java','-Xmx3G','-cp',cp,'table.ScenarioServer','--assets',assets,'--port','8789'],stdin=subprocess.PIPE,stdout=log.open('w'),stderr=subprocess.STDOUT,text=True,env={**os.environ,'TABLE_ADMIN_KEY':admin})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,token=None,body=None):
 req=urllib.request.Request('http://127.0.0.1:8789'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with http.open(req,timeout=10) as r:return json.load(r)
 except urllib.error.HTTPError as e:raise AssertionError(f'{e.code} {e.read().decode()}')
def state(s):
 r=call('/api/rooms/'+s['room']+'/state',s['token']);assert r['status']!='error',r.get('error');return r.get('game',{})
def act(s,g,**b):
 try:return call('/api/rooms/'+s['room']+'/action',s['token'],dict(b,controlVersion=g['controlVersion'],requestId=secrets.token_hex(12)))
 except AssertionError as e:
  if any(x in str(e) for x in ['changed','update','Wait','choice','answered']):return None
  raise
def own(g,seat):return next(p for p in g['players'] if p['seat']==seat)
def cards(g,seat,zone='Battlefield'):return own(g,seat)['zones'][zone]['cards']
def protection(g,seat):return any(s['name']=='Protection from everything' for s in own(g,seat).get('statuses',[]))
seen=set()
def advance(s,g):
 d=g.get('decision')
 if d:
  if d['id'] in seen:return
  seen.add(d['id']);print('DECISION',s['seat'],d['kind'],d['message'][:100],[o['label'][:70] for o in d['options'][:5]],flush=True)
  b={'action':'answer','decisionId':d['id']}
  if d['kind']=='allocation':b['values']=[d['max']]+[0]*(len(d['options'])-1)
  elif d['kind']=='number':b['value']=d['min']
  elif d['kind']=='text':b['value']='Test'
  else:b['selected']=[o['id'] for o in d['options'][:d['min']]]
  return act(s,g,**b)
 msg=g.get('message','')
 if 'Who would you like to start' in msg:return act(s,g,action='player',playerId=own(g,0)['id'])
 if 'discard' in msg.lower():
  c=next((c for c in cards(g,s['seat'],'Hand') if c.get('selectable') and not c.get('selected')),None)
  if c:return act(s,g,action='card',cardId=c['id'])
 if g.get('ok',{}).get('enabled'):return act(s,g,action='ok')
 if 'Pay Mana Cost' in msg:return
 if g.get('cancel',{}).get('enabled'):return act(s,g,action='cancel')

try:
 for _ in range(160):
  if process.poll() is not None:raise AssertionError('Server exited')
  try:call('/api/health');break
  except (OSError,AssertionError):time.sleep(.25)
 else:raise AssertionError('Server did not start')
 host=call('/api/rooms',admin,{'name':'ETB regression','player':'Tester 0'});sessions=[host]
 for i in range(1,4):sessions.append(call('/api/rooms/'+host['room']+'/join',body={'player':f'Tester {i}','invite':host['invite']}))
 for s in sessions:call('/api/rooms/'+s['room']+'/deck',s['token'],{'deck':'Feline Ferocity'})
 call('/api/rooms/'+host['room']+'/start',host['token'],{})
 deadline=time.time()+60
 while time.time()<deadline:
  g=state(host)
  if g.get('activeSeat')==0 and g.get('phase')=='MAIN1' and g.get('ok',{}).get('enabled'):break
  for s in sessions:
   v=state(s)
   if v.get('players'):advance(s,v)
  time.sleep(.15)
 else:raise AssertionError('No initial main phase')
 process.stdin.write('fixture\n');process.stdin.flush()
 for _ in range(50):
  g=state(host)
  if 'FIXTURE READY' in log.read_text() and any(c['name']=='The One Ring' for c in cards(g,0,'Hand')):break
  time.sleep(.1)
 else:raise AssertionError('Fixture not installed')
 assert not protection(g,3),'Uncast Ring gave protection'
 print('PASS: entering The One Ring without casting gives no protection',flush=True)
 stage='ring';started=set();ring_trigger=False;token_trigger=False;seen_targets=set();initial_turn=g['turn'];last='';protected_attempt=False;attack_step=0;block_step=0
 deadline=time.time()+160
 while time.time()<deadline:
  g=state(host)
  assert not protection(g,3),'Uncast Ring gave protection after priority passed'
  for p in g['players']:
   if p['seat']!=0:assert p['zones']['Hand']['cards']==[]
   assert p['zones']['Library']['cards']==[]
  sig=(stage,g.get('turn'),g.get('phase'),g.get('message'),[(x['text'][:120],x.get('targets')) for x in g.get('stack',[])])
  if str(sig)!=last:
   print('STATE',str(sig)[:900],flush=True);last=str(sig)
  for item in g.get('stack',[]):
   if any(t in item['text'].lower() for t in ['protection from everything']):ring_trigger=True
   if 'golem' in item['text'].lower():token_trigger=True
   for target in item.get('targets',[]):seen_targets.add(target['kind'])
  idle=not g.get('stack') and not g.get('decision') and g.get('ok',{}).get('enabled') and g.get('activeSeat')==0
  if stage=='ring' and protection(g,0):
   assert ring_trigger,'No Ring triggered ability was observed';print('PASS: cast Ring -> ETB stack -> visible protection',flush=True);stage='splicer'
  if stage=='splicer' and any(c.get('token') and c.get('power')==3 and c.get('toughness')==3 for c in cards(g,0)):
   assert token_trigger,'No token ETB was observed';print('PASS: Blade Splicer ETB -> actual 3/3 Golem token',flush=True);stage='bolt_player'
  if stage=='bolt_player' and own(g,2)['life']==37:
   assert 'player' in seen_targets;print('PASS: player target exported and Bolt dealt 3 damage',flush=True);stage='bolt_card'
  if stage=='bolt_card' and not any(c['name']=='Grizzly Bears' for c in cards(g,2)):
   assert 'card' in seen_targets;print('PASS: permanent target exported and Bears died',flush=True);stage='expiry'
  if stage=='expiry' and g.get('activeSeat')==0 and g['turn']>initial_turn and g.get('phase')=='MAIN1':
   assert not protection(g,0),'Ring protection did not expire';print('PASS: protection expired at controller next turn',flush=True);stage='combat'
  if stage=='combat' and any(e['kind']=='block' for e in g.get('combat',[])):
   assert any(e['kind']=='attack' for e in g['combat']);print('PASS: real attacker/defender and blocker/attacker edges exported',flush=True);break
  if stage not in ['expiry','combat']:assert g['turn']==initial_turn,'Spell was not completed: '+stage
  if stage in ['ring','splicer','bolt_player','bolt_card'] and stage not in started and idle:
   name={'ring':'The One Ring','splicer':'Blade Splicer','bolt_player':'Lightning Bolt','bolt_card':'Lightning Bolt'}[stage]
   c=next(c for c in cards(g,0,'Hand') if c['name']==name)
   if act(host,g,action='card',cardId=c['id']) is not None:started.add(stage);print('CAST',stage,name,flush=True)
   time.sleep(.2);continue
  if stage.startswith('bolt_') and stage in started and not g.get('decision') and ('target' in g.get('message','').lower()) and not g.get('stack'):
   if stage=='bolt_player':
    if not protected_attempt:
     act(host,g,action='player',playerId=own(g,0)['id']);time.sleep(.2);check=state(host)
     assert not check.get('stack') and ('cannot target this player' in check.get('message','').lower()),'Protected player was accepted as a target'
     print('PASS: protected player rejected as a Bolt target',flush=True);protected_attempt=True
    else:act(host,g,action='player',playerId=own(g,2)['id'])
   else:
    c=next(c for c in cards(g,2) if c['name']=='Grizzly Bears');act(host,g,action='card',cardId=c['id'])
  elif stage=='combat' and g.get('phase')=='COMBAT_DECLARE_ATTACKERS' and 'Priority:' not in g.get('message','') and not g.get('message','').startswith('Waiting') and attack_step<2:
   if attack_step==0:
    if act(host,g,action='player',playerId=own(g,1)['id']) is not None:attack_step=1
   else:
    token=next(c for c in cards(g,0) if c.get('token'))
    if act(host,g,action='card',cardId=token['id']) is not None:attack_step=2
  else:advance(host,g)
  for s in sessions[1:]:
   v=state(s)
   if stage=='combat' and s['seat']==1 and v.get('phase')=='COMBAT_DECLARE_BLOCKERS' and 'Priority:' not in v.get('message','') and not v.get('message','').startswith('Waiting') and block_step<2:
    c=next(c for c in cards(v,0) if c.get('token')) if block_step==0 else next(c for c in cards(v,1) if c['name']=='Grizzly Bears')
    if act(s,v,action='card',cardId=c['id']) is not None:block_step+=1
   else:advance(s,v)
  time.sleep(.15)
 else:raise AssertionError('ETB regression timed out at '+stage)
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 lines=[s for s in log.read_text(errors='replace').splitlines() if 'Host key:' not in s and 'Chave do anfitrião:' not in s]
 errors=[i for i,s in enumerate(lines) if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 if errors:print('FORGE ERRORS:\n'+'\n'.join(lines[errors[0]:errors[0]+55]),flush=True)
