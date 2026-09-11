"""Native Alpha 0.6 scry, tax, tutor and modal-spell regression over HTTP.
Usage: python choices.py JAR ASSETS TEST_CLASSES
Compile ChoicesScenarioServer.java and ScenarioServer.java into TEST_CLASSES.
"""
import json, os, secrets, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path

jar,assets,classes=sys.argv[1:4]
admin=secrets.token_urlsafe(24)
log=Path('/workspace/table-choices.log') if Path('/workspace').exists() else Path('table-choices.log')
cp=os.pathsep.join([classes,jar,str(Path(jar).parent/'lib/*')])
process=subprocess.Popen(['java','-Xmx3G','-cp',cp,'table.ChoicesScenarioServer','--assets',assets,'--port','8791'],stdin=subprocess.PIPE,stdout=log.open('w'),stderr=subprocess.STDOUT,text=True,env={**os.environ,'TABLE_ADMIN_KEY':admin})
http=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def call(path,token=None,body=None):
 req=urllib.request.Request('http://127.0.0.1:8791'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
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
 host=call('/api/rooms',admin,{'name':'Alpha 0.6 choices','player':'Tester 0'});sessions=[host]
 for i in range(1,4):sessions.append(call('/api/rooms/'+host['room']+'/join',body={'player':f'Tester {i}','invite':host['invite']}))
 for s in sessions:call('/api/rooms/'+s['room']+'/deck',s['token'],{'deck':'Elven Empire'})
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
 for _ in range(80):
  g=state(host)
  if any(c['name']=='Boros Charm' for c in cards(g,0,'Hand')):break
  time.sleep(.1)
 else:raise AssertionError('Choice fixture not installed')
 stages=['Opt','Preordain','Cultivate','Demonic Tutor','Boros Charm'];index=0;cast=False;proof=set();tax_answers=0
 deadline=time.time()+130
 while time.time()<deadline and index<len(stages):
  g=state(host);name=stages[index]
  for seat in [0,1]:
   view=g if seat==0 else state(sessions[seat])
   for player in view.get('players',[]):
    for event in player.get('announcements',[]):
     if event['kind']=='tutor':
      if event['card']['name']=='Demonic Tutor':
       assert ('a hidden card' in event['text'])==(seat==1),event['text']
       proof.add('private tutor '+str(seat))
      if event['card']['name']=='Cultivate':
       assert 'hidden card' not in event['text'],event['text'];proof.add('revealed tutor')
     if event['card']['name']=='Boros Charm' and 'Chosen modes:' in event['text']:
      assert 'indestructible' in event['text'].lower();proof.add('modes')
  if cast and not g.get('decision') and not g.get('stack') and g.get('ok',{}).get('enabled') and g.get('message','').startswith('Priority:') and not any(c['name']==name for c in cards(g,0,'Hand')):
   print('PASS: resolved '+name,flush=True);index+=1;cast=False;continue
  d=g.get('decision')
  if d and d['id'] not in seen:
   labels=[o['label'] for o in d['options']]
   if any(label=="Don't pay" for label in labels):
    seen.add(d['id']);pay=name=='Opt';selected=next(o['id'] for o in d['options'] if (o['label'].startswith('Pay '))==pay)
    act(host,g,action='answer',decisionId=d['id'],selected=[selected]);proof.add('pay' if pay else 'decline');tax_answers+=1
   elif name=='Cultivate' and d['kind']!='reveal' and d.get('presentation')=='library' and d['max']>0:
    seen.add(d['id']);act(host,g,action='answer',decisionId=d['id'],selected=[o['id'] for o in d['options'][:min(2,d['max'])]])
   elif name=='Boros Charm' and any('indestructible' in label.lower() for label in labels):
    seen.add(d['id']);act(host,g,action='answer',decisionId=d['id'],selected=[next(o['id'] for o in d['options'] if 'indestructible' in o['label'].lower())])
   elif name=='Preordain' and d.get('presentation')=='library' and d['min']==0 and d['max']>0:
    seen.add(d['id']);act(host,g,action='answer',decisionId=d['id'],selected=[d['options'][0]['id']]);proof.add('scry split')
   else:advance(host,g)
  elif g.get('confirmation') and g['ok']['label']=='Top' and g['cancel']['label']=='Bottom':
   proof.add('scry modal');act(host,g,action='cancel')
  elif not cast and not g.get('stack') and g.get('ok',{}).get('enabled'):
   candidate=next((c for c in cards(g,0,'Hand') if c['name']==name),None)
   if candidate and act(host,g,action='card',cardId=candidate['id']) is not None:cast=True
  else:advance(host,g)
  for s in sessions[1:]:advance(s,state(s))
  time.sleep(.12)
 assert index==len(stages),f'Timed out at {stages[index]}: {g.get("message")} {g.get("decision")}'
 assert {'pay','decline','scry modal','scry split','private tutor 0','private tutor 1','revealed tutor','modes'}<=proof,proof
 print('PASS: native scry 1/2, pay/decline taxes, tutor privacy and destinations, chosen modes',flush=True)

finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 lines=[s for s in log.read_text(errors='replace').splitlines() if 'Host key:' not in s and 'Chave do anfitrião:' not in s]
 errors=[i for i,s in enumerate(lines) if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 if errors:print('FORGE ERRORS:\n'+'\n'.join(lines[errors[0]:errors[0]+55]),flush=True)
