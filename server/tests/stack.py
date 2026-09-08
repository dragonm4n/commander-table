"""Real stack targeting, counterspells, delayed mana and public UI metadata over HTTP.
Usage: python stack.py JAR ASSETS SCENARIO_CLASSES
Compile StackScenarioServer.java separately; fixture entry point is never shipped in the JAR.
"""
import json, os, secrets, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path

jar,assets,classes=sys.argv[1:4]
admin=secrets.token_urlsafe(24)
log=Path('/workspace/table-stack.log') if Path('/workspace').exists() else Path('table-stack.log')
cp=os.pathsep.join([classes,jar,str(Path(jar).parent/'lib/*')])
process=subprocess.Popen(['java','-Xmx3G','-cp',cp,'table.StackScenarioServer','--assets',assets,'--port','8791'],stdin=subprocess.PIPE,stdout=log.open('w'),stderr=subprocess.STDOUT,text=True,env={**os.environ,'TABLE_ADMIN_KEY':admin})
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
 host=call('/api/rooms',admin,{'name':'Stack regression','player':'Tester 0'});sessions=[host]
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
  if 'FIXTURE READY' in log.read_text() and any(c['name']=='Blade Splicer' for c in cards(g,0,'Hand')):break
  time.sleep(.1)
 else:raise AssertionError('Fixture not installed')
 assert any(c.get('counters',{}).get('+1/+1')==2 for c in cards(g,3)), 'Counter label is not +1/+1: '+str([(c['name'],c.get('counters')) for c in cards(g,3)])
 print('PASS: +1/+1 counter label and quantity',flush=True)
 cycle=1;cast_started=set();drain_started=set();counter_started=False;drain_edge=False;counter_edge=False;three_stack=False;captured_images=[];tome_started=False;tome_announced=False;tome_seen=False;drain_announced=False;mana_spent=False;manalith_started=False
 deadline=time.time()+120
 while time.time()<deadline:
  g=state(host);stack=g.get('stack',[])
  announcement=own(g,0).get('lastAction') or {}
  if announcement.get('kind')=='activate' and announcement.get('card',{}).get('name')=='Jayemdae Tome':
   assert any(x['id']==announcement['stackId'] for x in stack),'Announcement is not linked to a stack item'
   tome_announced=True
  for p in g['players']:
   if (p.get('lastAction') or {}).get('card',{}).get('name')=='Mana Drain':drain_announced=True
  if tome_announced and len(cards(g,0,'Hand'))==1 and not stack:
   assert not own(g,0).get('lastAction'),'Resolved ability announcement remained visible'
   if not tome_seen:print('PASS: activated announcement links to its stack item and disappears after the draw resolves',flush=True)
   tome_seen=True
  names=[x['card']['name'] for x in stack]
  if len(stack)>=3:three_stack=True
  for item in stack:
   for t in item.get('targets',[]):
    if item['card']['name']=='Mana Drain' and t['kind']=='stack' and t['name']=='Blade Splicer':drain_edge=True
    if item['card']['name']=='Counterspell' and t['kind']=='stack' and t['name']=='Mana Drain':counter_edge=True
  if cycle==1 and not stack and any(c['name']=='Blade Splicer' for c in cards(g,0,'Graveyard')):
   assert drain_edge,'Mana Drain did not export its stack target'
   assert not cards(g,0) or not any(c.get('token') for c in cards(g,0)), 'Countered spell created an ETB token'
   assert drain_announced,'Cast announcement missing while pending'
   assert all(not p.get('lastAction') for p in g['players']),'Resolved or countered announcements remained visible'
   print('PASS: Mana Drain targets/counters Blade Splicer; no ETB; resolved and countered notices cleared',flush=True);cycle=2
  if cycle==2 and any(c.get('token') for c in cards(g,0)):
   assert counter_edge and three_stack, 'Counterspell response was not represented correctly'
   token=next(c for c in cards(g,0) if c.get('token'));assert token.get('tokenImages'), 'No token printing image metadata'
   captured_images=token['tokenImages'];(log.parent/'token-image-paths.json').write_text(json.dumps(captured_images))
   assert any(e['name']=='token' for e in g.get('sounds',[])), 'Forge token sound event missing'
   print('PASS: Counterspell counters Mana Drain; original spell resolves and creates token; sound event',flush=True)
   print('TOKEN PRINTING:',captured_images,flush=True);cycle=3
  if cycle==3 and g.get('activeSeat')==1 and g.get('phase')=='MAIN1' and not stack:
   mana=next(m['amount'] for m in own(g,1)['mana'] if m['color']==32)
   if mana:
    assert mana==3, f'Expected exactly 3 delayed colorless mana, got {mana}'
    print('PASS: Mana Drain adds exactly 3 colorless mana at next main phase; countered Drain adds none',flush=True);cycle=4
  if cycle==4 and any(c['name']=='Manalith' for c in cards(g,1)):
   assert mana_spent and tome_seen,'Missing manual colorless payment or activated announcement'
   assert next(m['amount'] for m in own(g,1)['mana'] if m['color']==32)==0
   assert not any(c.get('tapped') for c in cards(g,1) if c.get('land')),'Paid using lands instead of the generated mana'
   print('PASS: generated colorless mana pays for Manalith, including the mana button; islands remain untapped',flush=True)
   break
  for session in sessions:
   v=state(session);seat=session['seat'];d=v.get('decision');vstack=v.get('stack',[]);vsnames=[x['card']['name'] for x in vstack]
   for p in v['players']:
    if p['seat']!=seat:assert not p['zones']['Hand']['cards'], 'PRIVATE HAND LEAK'
   if d and d['kind']=='stack':
    wanted='Blade Splicer' if seat==1 else 'Mana Drain'
    option=next((o for o in d['options'] if o.get('card',{}).get('name')==wanted),None)
    assert option and 'stackId' in option, f'Cannot choose {wanted}: {d}'
    if d['id'] not in seen:
     seen.add(d['id']);act(session,v,action='answer',decisionId=d['id'],selected=[option['id']])
    continue
   priority=not d and v.get('ok',{}).get('enabled') and v.get('message','').startswith('Priority:')
   if cycle==3 and seat==0 and priority and not vstack and not tome_started:
    tome=next(c for c in cards(v,0) if c['name']=='Jayemdae Tome')
    if act(session,v,action='card',cardId=tome['id']) is not None:tome_started=True
    continue
   if cycle==4 and seat==1 and priority and not vstack and not manalith_started:
    card=next(c for c in cards(v,1,'Hand') if c['name']=='Manalith')
    if act(session,v,action='card',cardId=card['id']) is not None:manalith_started=True
    continue
   if cycle==4 and seat==1 and manalith_started and not mana_spent and 'Pay Mana Cost' in v.get('message',''):
    if act(session,v,action='mana',color=32) is not None:mana_spent=True
    continue
   # Keep this observation window open: the next client in the loop must not
   # advance straight out of the main phase before the host observes its pool.
   if cycle==3 and seat==1 and v.get('activeSeat')==1 and v.get('phase')=='MAIN1' and not vstack and priority:continue
   cardname=None
   if cycle<=2 and seat==0 and cycle not in cast_started and priority and not vstack:
    cardname='Blade Splicer'
   elif cycle<=2 and seat==1 and cycle not in drain_started and priority and 'Blade Splicer' in vsnames:
    cardname='Mana Drain'
   elif cycle==2 and seat==2 and not counter_started and priority and 'Mana Drain' in vsnames:
    cardname='Counterspell'
   if cardname:
    card=next(c for c in cards(v,seat,'Hand') if c['name']==cardname)
    if act(session,v,action='card',cardId=card['id']) is not None:
     if seat==0:cast_started.add(cycle)
     elif seat==1:drain_started.add(cycle)
     else:counter_started=True
    print('CAST',cycle,seat,cardname,flush=True)
   else:advance(session,v)
  time.sleep(.18)
 else:
  print('FINAL:',g.get('turn'),g.get('phase'),g.get('activeSeat'),[(p['seat'],p['mana']) for p in g['players']],flush=True)
  raise AssertionError('Stack regression timed out in cycle '+str(cycle))

finally:
 process.terminate()
 try:process.wait(timeout=5)
 except subprocess.TimeoutExpired:process.kill()
 lines=[s for s in log.read_text(errors='replace').splitlines() if 'Host key:' not in s and 'Chave do anfitrião:' not in s]
 errors=[i for i,s in enumerate(lines) if 'Exception' in s or 'Projection:' in s or 'Error in ' in s]
 if errors:print('FORGE ERRORS:\n'+'\n'.join(lines[errors[0]:errors[0]+55]),flush=True)
 assert not errors,'Forge reported an error during the stack regression'
