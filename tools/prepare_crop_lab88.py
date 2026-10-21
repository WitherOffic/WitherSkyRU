"""Two screenshot-observed variable messages, validated against full public source pages."""
import json,urllib.request,urllib.parse
from prepare_npc88 import ROOT,SOURCE,CORPUS
from prepare_npc78 import rows,sha,write

def main():
 queue=rows(CORPUS/'translation_queue.jsonl');occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 blocks={r['block_id']:r for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for r in rows(CORPUS/name)}
 records=[];pages=set()
 for line in (SOURCE/'NPC88_CROP_LAB_DYNAMIC_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  speaker,source,target=line.split('|');source=f'[NPC] {speaker}: {source}';target=f'[NPC] {speaker}: {target}'
  assert source.count('<player>')==target.count('<player>')==1
  found=[r for r in queue if r['source']==source];assert len(found)==1 and found[0]['variable_definition']
  occurrences=[occ[key] for key in found[0]['occurrence_ids']];pages|={r['page'] for r in occurrences}
  records.append({'speaker':speaker,'source_template':source,'target_template':target,'counted_as_new_exact_key':False,'all_occurrences':occurrences,'blocks':[blocks[key] for key in sorted({r['block_id'] for r in occurrences})]})
 assert len(records)==2 and pages=={'Beth','Jake'}
 expected={r['title']:r['revision'] for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in pages}
 url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(pages)),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.88 (read-only)'}),timeout=45) as stream:result=json.load(stream)['query']['pages']
 actual={r['title']:r['revisions'][0]['revid'] for r in result};assert actual==expected,(actual,expected)
 from pathlib import Path
 screenshots=[Path('C:/Users/Wither/AppData/Local/Temp')/name for name in ('codex-clipboard-2d45852d-c076-4250-8144-8e1c29fdd23c.png','codex-clipboard-bc3b00cf-74a7-44c0-be5c-e83ee596ec4a.png')]
 assert all(p.is_file() for p in screenshots)
 write(SOURCE/'NPC_CROP_LAB_DYNAMIC_0.15.88.json',{'review_client_date':'2026-10-18','api_url':url,'matches_pinned_pages':True,'revision_pages':result,'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'screenshots':[{'name':p.name,'sha256':sha(p)} for p in screenshots],'records':records,'scope':'Two variable sentences observed by user; preserved player identity/head; not two new literal-corpus keys,not complete quest or live runtime validation.'})
 print('Crop lab:two source-backed variable lines;two screenshot hashes;Beth/Jake API revisions match;exact key count unchanged')
if __name__=='__main__':main()
