"""Compare current and pinned public revisions without altering the frozen queue."""
import json,difflib,urllib.parse,urllib.request
from prepare_npc101 import SOURCE,CORPUS
from prepare_npc78 import write,sha
from review_npc92_revision import section
from export_npc_corpus77 import templates
ids=[852931,860232]
url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,ids)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-revision-review/0.15.101 (read-only)'}),timeout=45) as stream:data=json.load(stream)
items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(ids)
old,current=[items[i]['slots']['main']['content'] for i in ids]
dialog=lambda t:[raw for x,y,n,raw in templates(t) if n.lower()=='dialogue']
diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(ids[0]),tofile=str(ids[1]),lineterm=''))
r={'page':'Professor Wynd','pinned_revision':ids[0],'current_revision':ids[1],'full_diff':diff,'entire_dialogue_section_including_conditions_unchanged':section(old)==section(current),'all_dialogue_templates_unchanged':dialog(old)==dialog(current)}
print(diff);print('Dialogue including conditions unchanged:',r['entire_dialogue_section_including_conditions_unchanged'],r['all_dialogue_templates_unchanged'])
assert r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged']
write(SOURCE/'NPC_0.15.101_REVISION_DELTA.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'api_url':url,'full_revisions':data,'records':[r],'accepted_for_batch101':True,'review':'Entire dialogue section and all templates unchanged; change outside dialogue, not live coverage certification.'})
