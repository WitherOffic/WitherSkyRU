"""Read-only,full-source comparison for the one changed public page in batch89."""
import difflib,json,urllib.parse,urllib.request
from prepare_npc78 import write,sha
from prepare_npc88 import SOURCE,CORPUS
from export_npc_corpus77 import templates

def main():
 revisions=(859295,859508)
 url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','revids':'|'.join(map(str,revisions)),'prop':'revisions','rvprop':'ids|timestamp|content','rvslots':'main','format':'json','formatversion':'2'})
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-source-diff/0.15.89 (read-only)'}),timeout=45) as stream:data=json.load(stream)
 items={r['revid']:r for p in data['query']['pages'] for r in p['revisions']};assert set(items)==set(revisions)
 old,current=[items[k]['slots']['main']['content'] for k in revisions]
 diff='\n'.join(difflib.unified_diff(old.splitlines(),current.splitlines(),fromfile=str(revisions[0]),tofile=str(revisions[1]),lineterm=''))
 def dialogue(text):return [raw for a,b,name,raw in templates(text) if name.lower()=='dialogue']
 old_dialogues,new_dialogues=dialogue(old),dialogue(current)
 # Compare the complete Dialogue section,including conditions and tables,not just utterances.
 import re
 def section(text):
  start=re.search(r'^==\s*Dialogue\s*==\s*$',text,re.M);assert start
  following=re.search(r'^==[^=].*?==\s*$',text[start.end():],re.M)
  return text[start.start():start.end()+following.start() if following else len(text)]
 addition="|{{Dialogue|\n&e[NPC] &6Pesthunter Phillip&f: &fUnfortunate. The &2 Pests &faren't gonna hunt themselves!\n}}"
 assert current.count(addition)==1 and current.replace(addition,'|{{InfoNeeded}}')==old,'Only the independently reviewed replacement of InfoNeeded is accepted'
 new_only=[r for r in new_dialogues if r not in old_dialogues]
 assert len(old_dialogues)==12 and len(new_dialogues)==13 and len(new_only)==1 and [r for r in new_dialogues if r not in new_only]==old_dialogues
 proof={'scope':'One newly published dialogue replaced InfoNeeded;all other full-page text byte-identical. Not a silent corpus rewrite.','page':'Pesthunter Phillip','pinned_revision':revisions[0],'current_revision':revisions[1],'api_url':url,'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'dialogue_templates_unchanged':False,'entire_dialogue_section_including_conditions_unchanged':False,'all_previous_dialogues_and_conditions_unchanged':True,'only_expected_replacement_of_info_needed':True,'new_dialogue_count':1,'new_raw_dialogues':new_only,'dialogue_template_count':len(old_dialogues),'full_revisions':data,'unified_diff':diff,'accepted_for_batch89':True}
 write(SOURCE/'NPC_0.15.89_REVISION_DELTA.json',proof)
 print(diff);print('Previously published12dialogues unchanged;one new denial dialogue captured outside frozen denominator')
if __name__=='__main__':main()
