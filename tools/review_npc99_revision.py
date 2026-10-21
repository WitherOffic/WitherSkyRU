"""Exact pinned/current Honey diff; preserve complete revisions and never rewrite corpus."""
import json,difflib,urllib.parse,urllib.request
from prepare_npc99 import SOURCE,CORPUS
from prepare_npc78 import write,sha
from review_npc92_revision import section
from export_npc_corpus77 import templates
ids=[828537,859857]
url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,ids)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-revision-review/0.15.99 (read-only)'}),timeout=45) as stream:data=json.load(stream)
items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(ids)
old,current=[items[i]['slots']['main']['content'] for i in ids]
diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(ids[0]),tofile=str(ids[1]),lineterm=''))
dialog=lambda t:[raw for x,y,n,raw in templates(t) if n.lower()=='dialogue']
record={'page':'Honey','pinned_revision':ids[0],'current_revision':ids[1],'full_diff':diff,'entire_dialogue_section_including_conditions_unchanged':section(old)==section(current),'all_dialogue_templates_unchanged':dialog(old)==dialog(current)}
print(diff);print('Dialogue/conditions unchanged:',record['entire_dialogue_section_including_conditions_unchanged'],record['all_dialogue_templates_unchanged'])
assert current=='{{See also|Pots of Honeycomb}}\n'+old
assert record['entire_dialogue_section_including_conditions_unchanged'] and record['all_dialogue_templates_unchanged']
write(SOURCE/'NPC_0.15.99_REVISION_DELTA.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'api_url':url,'full_revisions':data,'records':[record],'accepted_for_batch99':True,'review':'Only a See also navigation link was added before the character infobox. Complete Dialogue section including conditions and templates unchanged. Not live certification.'})
