"""Retain full public revision deltas; never silently update a frozen corpus."""
import json,difflib,urllib.parse,urllib.request
from prepare_npc102 import SOURCE,CORPUS
from prepare_npc78 import write,sha
from review_npc92_revision import section
from export_npc_corpus77 import templates
rev=json.loads((SOURCE/'NPC_0.15.102_PUBLIC_REVISION.json').read_text(encoding='utf-8'));pairs={k:(rev['expected'][k],v) for k,v in rev['actual'].items() if rev['expected'].get(k)!=v};ids=sorted({n for v in pairs.values() for n in v})
url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,ids)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-revision-review/0.15.102 (read-only)'}),timeout=45) as stream:data=json.load(stream)
items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(ids)
dialog=lambda t:[raw for x,y,n,raw in templates(t) if n.lower()=='dialogue'];records=[]
for page,pair in pairs.items():
 old,current=[items[i]['slots']['main']['content'] for i in pair];diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(pair[0]),tofile=str(pair[1]),lineterm=''))
 r={'page':page,'pinned_revision':pair[0],'current_revision':pair[1],'full_diff':diff,'entire_dialogue_section_including_conditions_unchanged':section(old)==section(current),'all_dialogue_templates_unchanged':dialog(old)==dialog(current),'old_dialogue_templates_retained':all(t in dialog(current) for t in dialog(old)),'added_templates':[t for t in dialog(current) if t not in dialog(old)]};records.append(r);print(page,diff,sep='\n')
assert all(r['old_dialogue_templates_retained'] for r in records)
write(SOURCE/'NPC_0.15.102_REVISION_DELTA.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'api_url':url,'full_revisions':data,'records':records,'accepted_for_batch102':True,'review':'All old dialogue templates remain verbatim. Four new constant lines will be reviewed separately; Jeff table spans changed and alpha/update markers added. Frozen corpus unchanged.'})
