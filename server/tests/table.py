"""Alpha 0.4 regression: recovery, attachments, native library choices, tokens and rounds.
Usage: python table.py JAR ASSETS SCENARIO_CLASSES
Uses the real Forge engine and HTTP API, with fixture setup over test-process stdin.
"""
import concurrent.futures, json, os, secrets, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path
jar,assets,classes=sys.argv[1:4]
admin=secrets.token_urlsafe(24);log=Path('/workspace/table-04.log') if Path('/workspace').exists() else Path('table-04.log')
cp=os.pathsep.join([classes,jar,str(Path(jar).parent/'lib/*')])
process=subprocess.Popen(['java','-Xmx3G','-cp',cp,'table.TableScenarioServer','--assets',assets,'--port','8792'],stdin=subprocess.PIPE,stdout=log.open('w'),stderr=subprocess.STDOUT,text=True,env={**os.environ,'TABLE_ADMIN_KEY':admin})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,token=None,body=None,expected=None):
 req=urllib.request.Request('http://127.0.0.1:8792'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with http.open(req,timeout=10) as r:
   assert expected is None or r.status==expected, f'Expected {expected}, got {r.status}'
   return json.load(r)
 except urllib.error.HTTPError as e:
  data=json.load(e)
  if expected is not None and e.code==expected:return data
  raise AssertionError(f'{e.code}: {data.get("error")}')
def route(s,op):return '/api/rooms/'+s['room']+'/'+op
def state(s):
 r=call(route(s,'state'),s['token']);assert r['status']!='error',r.get('error');return r.get('game',{})
def act(s,g,**b):
 try:return call(route(s,'action'),s['token'],dict(b,controlVersion=g['controlVersion'],requestId=secrets.token_hex(12)))
 except AssertionError as e:
  if any(x in str(e) for x in ['changed','update','Wait','choice','answered']):return None
  raise
def own(g,seat):return next(p for p in g['players'] if p['seat']==seat)
def cards(g,seat,zone='Battlefield'):return own(g,seat)['zones'][zone]['cards']
def named(g,seat,name,zone='Battlefield'):return next((c for c in cards(g,seat,zone) if c['name']==name),None)
def priority(g):return not g.get('decision') and g.get('ok',{}).get('enabled') and g.get('message','').startswith('Priority:')
def advance(s,g):
 d=g.get('decision');msg=g.get('message','')
 if d:
  b=dict(action='answer',decisionId=d['id'])
  if d['kind']=='number':b['value']=d['min']
  elif d['kind']=='text':b['value']='Test'
  elif d['kind']=='allocation':b['values']=[d['max']]+[0]*(len(d['options'])-1)
  else:b['selected']=[o['id'] for o in d['options'][:d['min']]]
  return act(s,g,**b)
 if 'Who would you like to start' in msg:return act(s,g,action='player',playerId=own(g,0)['id'])
 if 'discard' in msg.lower():
  c=next((c for c in cards(g,s['seat'],'Hand') if c.get('selectable') and not c.get('selected')),None)
  if c:return act(s,g,action='card',cardId=c['id'])
 if g.get('ok',{}).get('enabled'):return act(s,g,action='ok')
 if 'Pay Mana Cost' in msg:return
 if g.get('cancel',{}).get('enabled'):return act(s,g,action='cancel')
def resume(s):return call(route(s,'resume'),body={'resumeKey':s['resumeKey']})
def same(a,b):assert all(a[k]==b[k] for k in ['seat','room','token','resumeKey']), 'Rejoin did not preserve the exact session'
try:
 for _ in range(180):
  if process.poll() is not None:raise AssertionError('Server exited')
  try:assert call('/api/health')['version']=='alpha-0.4';break
  except (OSError,AssertionError):time.sleep(.25)
 else:raise AssertionError('Server did not start')
 hostkey=secrets.token_urlsafe(24)
 host=call('/api/rooms',admin,{'name':'Alpha 0.4 regression','player':'Tester 0','resumeKey':hostkey})
 same(host,call('/api/rooms',admin,{'player':'Retry','resumeKey':hostkey}))
 key=secrets.token_urlsafe(24);payload={'player':'Tester 1','invite':host['invite'],'resumeKey':key}
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
  replies=list(pool.map(lambda _:call(route(host,'join'),body=payload),range(2)))
 guest=replies[0];same(guest,replies[1]);assert guest['seat']==1
 call(route(guest,'deck'),guest['token'],{'deck':'Feline Ferocity'})
 call(route(guest,'disconnect'),guest['token'],{})
 before=call(route(host,'state'),host['token']);assert before['seats'][1]['ready'] and not before['seats'][1]['connected']
 same(guest,resume(guest));same(guest,call(route(host,'join'),body=payload))
 bad=secrets.token_urlsafe(24);call(route(host,'resume'),body={'resumeKey':bad},expected=403)
 temporary=call(route(host,'join'),body={'player':'Tester 1','invite':host['invite']});assert temporary['seat']==2
 call(route(temporary,'leave'),temporary['token'],{});call(route(temporary,'resume'),body={'resumeKey':temporary['resumeKey']},expected=403);call(route(temporary,'state'),temporary['token'],expected=403)
 sessions=[host,guest]
 for i in range(2,4):sessions.append(call(route(host,'join'),body={'player':f'Tester {i}','invite':host['invite']}))
 assert [s['seat'] for s in sessions]==[0,1,2,3]
 for s in sessions:call(route(s,'deck'),s['token'],{'deck':'Feline Ferocity'})
 public=call(route(host,'state'),host['token']);assert 'resumeKey' not in json.dumps(public) and 'token' not in json.dumps(public)
 assert all(s['commander'] for s in public['seats'])
 print('PASS: concurrent join retries use one seat; disconnect/rejoin keeps deck and readiness; release revokes access; same names cannot claim a seat',flush=True)
 call(route(host,'start'),host['token'],{})
 deadline=time.time()+60
 while time.time()<deadline:
  g=state(host)
  if g.get('activeSeat')==0 and g.get('phase')=='MAIN1' and priority(g):break
  for s in sessions:
   v=state(s)
   if v.get('players'):advance(s,v)
  time.sleep(.12)
 else:raise AssertionError('Initial main phase not reached')
 assert g['turn']==1 and g['round']==1, f'Initial round incorrect: {g["turn"]}, {g["round"]}'
 call(route(host,'join'),body={'invite':host['invite'],'player':'New player'},expected=400)
 call(route(guest,'leave'),guest['token'],{},expected=400)
 process.stdin.write('fixture\n');process.stdin.flush()
 for _ in range(60):
  g=state(host)
  if named(g,0,'Bonesplitter') and named(g,1,'Pacifism'):break
  time.sleep(.1)
 else:raise AssertionError('Fixture not installed')
 bear=named(g,0,'Grizzly Bears')['id'];aura=named(g,1,'Pacifism');assert aura['attachedTo']['id']==bear and aura['controllerSeat']==1
 print('PASS: foreign-controlled aura projects its public attachment and controller',flush=True)
 stage='equip';started=set();rejoined=False;searched=False;rounds={1:1};library_seen=False
 deadline=time.time()+130
 while time.time()<deadline:
  g=state(host);stack=g.get('stack',[])
  rounds[g['turn']]=g['round']
  if stage=='equip' and named(g,0,'Bonesplitter').get('attachedTo',{}).get('id')==bear:
   print('PASS: native Equip resolves and attaches the equipment',flush=True);stage='aura'
  if stage=='aura' and named(g,0,'Rancor') and named(g,0,'Rancor').get('attachedTo',{}).get('id')==bear:
   assert named(g,0,'Grizzly Bears')['power']==6
   print('PASS: cast aura targets and attaches to the equipped creature; native power updates',flush=True);stage='tokens'
  if stage=='tokens' and len([c for c in cards(g,0) if c.get('token')])==2 and not stack:
   snapshot=[{'seat':p['seat'],'zones':{'Battlefield':p['zones']['Battlefield']}} for p in g['players']]
   (log.parent/'table-04-battlefield.json').write_text(json.dumps(snapshot))
   print('PASS: native token creation exports two individually addressable identical tokens',flush=True);stage='search'
  if stage=='search' and searched and not stack and named(g,0,'Rampant Growth','Graveyard'):
   assert len([c for c in cards(g,0) if c['name']=='Forest'])==9,'Chosen basic land did not enter the battlefield'
   assert not own(g,0).get('lastAction'),'Resolved search notice remained visible'
   print('PASS: library choice resolved into the battlefield and cleared its stack announcement',flush=True);stage='bounce'
  if stage=='bounce' and named(g,0,'Grizzly Bears','Hand') and not stack:
   assert not named(g,0,'Bonesplitter').get('attachedTo'),'Equipment stayed attached after its host left'
   assert named(g,1,'Pacifism','Graveyard'),'Aura did not leave the battlefield with its host'
   print('PASS: native bounce detaches equipment and removes the Aura from the battlefield',flush=True);stage='rounds'
  if stage=='rounds' and g['turn']>=5:
   assert all(rounds[t]==1 for t in range(1,5)),str(rounds)
   assert g['round']==2 and g['activeSeat']==0,str(rounds)
   assert not named(g,0,'Bonesplitter').get('attachedTo')
   assert rejoined and library_seen
   print('PASS: equipment detachment exported; four individual turns stay on Round 1, next rotation is Round 2',flush=True);break
  for s in sessions:
   v=state(s);d=v.get('decision');seat=s['seat']
   for p in v['players']:
    if p['seat']!=seat:assert not p['zones']['Hand']['cards'],'PRIVATE HAND LEAK'
    if p['seat']!=seat:assert not p['zones']['Library']['cards'],'PRIVATE LIBRARY LEAK'
   if seat==0 and stage=='search' and d and d.get('presentation')=='library':
    library_seen=True;assert all(not o['card'].get('hidden') for o in d['options'] if o.get('card')),'Authorized search options are hidden'
    if not rejoined:
     call(route(host,'disconnect'),host['token'],{});same(host,resume(host));restored=state(host)
     assert restored['decision']['id']==d['id'] and restored['decision']['options']==d['options'],'Pending search did not survive reconnect'
     same(guest,resume(guest));assert len([p for p in call(route(host,'state'),host['token'])['seats'] if p['human']])==4
     print('PASS: in-game rejoin restores the same seat, private hand and pending library popup',flush=True);rejoined=True
    if d['kind']=='reveal':act(s,v,action='answer',decisionId=d['id'],selected=[])
    else:
     option=next(o for o in d['options'] if o.get('card',{}).get('name')=='Forest');act(s,v,action='answer',decisionId=d['id'],selected=[option['id']]);searched=True
    continue
   if seat==0 and stage in ('equip','aura','bounce') and stage in started and not d and not priority(v):
    target=named(v,0,'Grizzly Bears')
    if target and target.get('selectable') and not target.get('selected'):
     act(s,v,action='card',cardId=bear);continue
   if seat==0 and stage in ('equip','aura','tokens','search','bounce') and stage not in started and priority(v) and not v.get('stack'):
    name={'equip':'Bonesplitter','aura':'Rancor','tokens':'Raise the Alarm','search':'Rampant Growth','bounce':'Unsummon'}[stage]
    c=named(v,0,name,'Battlefield' if stage=='equip' else 'Hand');assert c,f'{name} unavailable'
    if act(s,v,action='card',cardId=c['id']) is not None:started.add(stage)
    continue
   advance(s,v)
  time.sleep(.15)
 else:
  print('LAST STATE',stage,g.get('message'),g.get('decision'),g.get('turn'),g.get('phase'),flush=True)
  raise AssertionError('Regression timed out')
finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 lines=[s for s in log.read_text(errors='replace').splitlines() if 'Host key:' not in s and 'Chave do anfitrião:' not in s]
 errors=[i for i,s in enumerate(lines) if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 if errors:print('\n'.join(lines[errors[0]:errors[0]+45]),flush=True)
 assert not errors,'Forge reported an error in the table regression'
