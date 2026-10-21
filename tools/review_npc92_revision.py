"""Read-only complete revision diffs; never rewrite the frozen corpus."""
import difflib,json,re,urllib.parse,urllib.request
from prepare_npc78 import write,sha
from prepare_npc88 import SOURCE,CORPUS
from export_npc_corpus77 import templates

PAIRS={'Pablo':(845174,859970),'Arch':(812539,859640)}
def section(text):
 start=re.search(r'^==\s*Dialogue\s*==\s*$',text,re.M);assert start
 following=re.search(r'^==[^=].*?==\s*$',text[start.end():],re.M)
 return text[start.start():start.end()+following.start() if following else len(text)]
def main():
 ids=[x for pair in PAIRS.values() for x in pair]
 url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,ids)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-revision-review/0.15.92 (read-only)'}),timeout=45) as stream:data=json.load(stream)
 items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(ids);records=[]
 for name,(a,b) in PAIRS.items():
  old,current=[items[k]['slots']['main']['content'] for k in (a,b)]
  diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(a),tofile=str(b),lineterm=''))
  dialog=lambda text:[raw for x,y,n,raw in templates(text) if n.lower()=='dialogue']
  record={'page':name,'pinned_revision':a,'current_revision':b,'full_diff':diff,'entire_dialogue_section_including_conditions_unchanged':section(old)==section(current),'all_dialogue_templates_unchanged':dialog(old)==dialog(current),'old_dialogue_count':len(dialog(old)),'current_dialogue_count':len(dialog(current))}
  if name=='Pablo':
   assert current.count('&aYou claimed a &oThe Soup&a!')==1 and current.replace('&aYou claimed a &oThe Soup&a!','&aYou claimed a The Soup!')==old
   record.update(only_reviewed_non_npc_reward_style_change=True,npc_utterances_and_conditions_unchanged=True)
  else:
   assert record['entire_dialogue_section_including_conditions_unchanged'] and record['all_dialogue_templates_unchanged']
   record.update(only_changes_outside_dialogue=True,npc_utterances_and_conditions_unchanged=True)
  records.append(record);print(name);print(diff);print('Same full Dialogue section:',record['entire_dialogue_section_including_conditions_unchanged'])
 write(SOURCE/'NPC_0.15.92_REVISION_DELTA.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'api_url':url,'full_revisions':data,'records':records,'accepted_for_batch92':True,'scope':'Read-only full revisions and diffs;all NPC utterances and conditions unchanged. Pablo only non-NPC reward italics;Arch changes outside Dialogue. Not a corpus rewrite or live certification'})
if __name__=='__main__':main()
