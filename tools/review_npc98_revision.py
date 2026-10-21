"""Read-only exact old/current diffs; no corpus denominator or source rewrite."""
import json,difflib,urllib.parse,urllib.request
from prepare_npc98 import SOURCE,CORPUS
from prepare_npc78 import write,sha
from review_npc92_revision import section
from export_npc_corpus77 import templates
PAIRS={'Bonzo':(852604,859492),'The Watcher':(856107,859488)}
ids=[v for pair in PAIRS.values() for v in pair]
url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,ids)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-revision-review/0.15.98 (read-only)'}),timeout=45) as stream:data=json.load(stream)
items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(ids)
records=[]
for name,(a,b) in PAIRS.items():
 old,current=[items[k]['slots']['main']['content'] for k in (a,b)]
 diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(a),tofile=str(b),lineterm=''))
 dialog=lambda text:[raw for x,y,n,raw in templates(text) if n.lower()=='dialogue']
 r={'page':name,'pinned_revision':a,'current_revision':b,'full_diff':diff,'entire_dialogue_section_including_conditions_unchanged':section(old)==section(current),'all_dialogue_templates_unchanged':dialog(old)==dialog(current)}
 assert r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged']
 expected=old.replace('{{mcat|b}}','{{boss}}')
 if name=='The Watcher':expected=expected.replace('{{MobCategory|b}}','{{boss}}').replace('{{MobCategory|mb}}','{{miniboss}}').replace('{{MobCategory|boss}}','{{boss}}')
 assert expected==current,'Any further change requires separate review'
 records.append(r);print(name);print(diff);print('Dialogue/conditions unchanged:',r['entire_dialogue_section_including_conditions_unchanged'],r['all_dialogue_templates_unchanged'])
write(SOURCE/'NPC_0.15.98_REVISION_DELTA.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'api_url':url,'full_revisions':data,'records':records,'accepted_for_batch98':True,'review':'Only boss/miniboss category-template substitutions outside Dialogue. Complete Dialogue sections and conditions byte-identical. Not live certification.'})
