"""Read-only current/pinned revision comparison; acceptance requires separately reviewed exact deltas."""
import difflib,json,urllib.parse,urllib.request
from prepare_npc78 import write,sha
from prepare_npc88 import SOURCE,CORPUS
from review_npc92_revision import section
from export_npc_corpus77 import templates
PAIRS={'Mage Guard':(852815,859587),'Barbarian Guard':(852572,859588),'Spaceman':(859470,859732)}
def main():
 ids=[x for p in PAIRS.values() for x in p];url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,ids)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-revision-review/0.15.93 (read-only)'}),timeout=45) as stream:data=json.load(stream)
 items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(ids);records=[]
 for name,(a,b) in PAIRS.items():
  old,current=[items[k]['slots']['main']['content'] for k in (a,b)]
  diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(a),tofile=str(b),lineterm=''))
  dialog=lambda t:[raw for x,y,n,raw in templates(t) if n.lower()=='dialogue']
  record={'page':name,'pinned_revision':a,'current_revision':b,'full_diff':diff,'entire_dialogue_section_including_conditions_unchanged':section(old)==section(current),'all_dialogue_templates_unchanged':dialog(old)==dialog(current),'old_dialogue_count':len(dialog(old)),'current_dialogue_count':len(dialog(current))}
  assert record['entire_dialogue_section_including_conditions_unchanged'] and record['all_dialogue_templates_unchanged']
  if name=='Spaceman':assert old.replace('Appears as a [[The Garden]] [[Garden Visitors|Visitor]].','Appears as a [[Garden Visitor]].')==current
  else:
   faction='mage' if name=='Mage Guard' else 'barbarian'
   assert old.replace('If a player has -1000 or lower {{Reputation|'+faction+'}}, or if the player attacks the guard at least three times','If a player attacks the guard at least three times')==current
  record.update(only_reviewed_changes_outside_dialogue=True,npc_utterances_and_conditions_unchanged=True);records.append(record);print(name);print(diff)
 write(SOURCE/'NPC_0.15.93_REVISION_DELTA.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'api_url':url,'full_revisions':data,'records':records,'accepted_for_batch93':True,'scope':'Exact reviewed changes only: two Behavior paragraphs remove reputation condition;Spaceman location link. Complete Dialogue sections and conditions identical;not live certification or corpus rewrite.'})
if __name__=='__main__':main()
