"""Authored Catacombs literals. Pinned public corpus, not live/private completeness."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from prepare_npc93 import ROOT,SOURCE,DATA,CORPUS,structured,neutral,unsafe
from prepare_npc78 import rows,sha,write,anchors,validate

NAMES={'Bonzo','Goldor','Maxor','Necron','Sadan','Scarf','Storm','The Professor','The Watcher','The Wither King','Thorn'}
def unresolved(r):
 return bool(re.search(r'\((?:minion|name)\)',r['source'])) or r['source']=='[BOSS] Sadan: How did that Golem wake up? Carelessness from'
def selection():
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 for path in DATA.glob('*.json'):
  if path.name=='dungeon-expansion98.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==9276
 selected=[r for r in queue if r['speaker'] in NAMES]
 eligible=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and not unsafe(r) and not unresolved(r)]
 assert len(selected)==365 and len(eligible)==336
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for file in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/file) if b['block_id'] in ids]
 pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in {b['page'] for b in blocks}]
 return queue,known,selected,eligible,occurrences,blocks,pages

def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect',action='store_true');p.add_argument('--refresh',action='store_true');a=p.parse_args()
 queue,known,selected,eligible,occurrences,blocks,pages=selection()
 if a.inspect:
  for b in blocks:print(json.dumps(b,ensure_ascii=False))
  return
 if a.refresh:
  titles=sorted(r['title'] for r in pages)
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(titles),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-public-corpus-review/0.15.98 (read-only)'}),timeout=45) as stream:response=json.load(stream)['query']['pages']
  expected={r['title']:r['revision'] for r in pages};actual={r['title']:r['revisions'][0]['revid'] for r in response}
  write(SOURCE/'NPC_0.15.98_PUBLIC_REVISION.json',{'checked_at':datetime.now().astimezone().isoformat(),'api_url':url,'matches_pinned_corpus':actual==expected,'expected':expected,'actual':actual,'pages':response,'private_or_unpublished_dialogue_known':False})
  print('Revision comparison',actual==expected,{k:(expected.get(k),actual.get(k)) for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)})
  return
 lookup={r['source']:r for r in eligible};records=[];seen=set()
 for line in (SOURCE/'NPC_0.15.98_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  f=line.split('|');assert len(f) in (3,4),line
  speaker,english,russian=f[:3];source=f'[BOSS] {speaker}: {english}';target=f'[BOSS] {speaker}: {russian}'
  assert source in lookup and source not in seen,(speaker,english);seen.add(source)
  assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
  assert not re.search('[A-Za-z]',russian),source
  definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
  records.append({'source_key':f'dungeon98.{len(records)}','tag':'BOSS','speaker':speaker,'source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'pinned_public_wiki_revision_not_live_packet'})
 assert seen==lookup.keys(),sorted(lookup.keys()-seen)
 retained=[r for r in selected if r['source'] in known]
 pending=[r for r in selected if r['source'] not in known and structured(r)]
 extraction=[r for r in selected if r['source'] not in known and not structured(r) and unresolved(r)]
 neutrals=[r for r in selected if r['source'] not in known and not structured(r) and not unresolved(r) and neutral(r)]
 excluded=[r for r in selected if r['source'] not in known and not structured(r) and not unresolved(r) and not neutral(r) and unsafe(r)]
 assert len(selected)==sum(map(len,(records,retained,pending,extraction,neutrals,excluded)))
 resource=DATA/'dungeon-expansion98.json'
 write(resource,{'format_version':1,'language':'ru','scope':'336 authored Catacombs boss literals. Pinned public corpus,not live/private completeness. Existing dungeon provider;no additional render/lookup pass.','records':records})
 markers=[{'page':r['title'],'revision':r['revision'],'page_historical_templates':re.findall(r'\{\{Historical[^}]*\}\}',r['raw_wikitext'],re.I),'removed_table_or_heading':bool(re.search(r'^.*(?:Removed|Historical).*$',r['raw_wikitext'],re.M))} for r in pages]
 write(SOURCE/'NPC_SOURCES_0.15.98.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'raw_archive_marker_candidates_not_live_certification':markers,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'extraction_pending':extraction,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[]})
 counts=dict(Counter(r['speaker'] for r in records));after=9276+len(records)
 write(SOURCE/'NPC_PROGRESS_0.15.98.json',{'corpus_unique_rows':len(queue),'exact_resource_before':9276,'exact_resource_after':after,'after_percent':100*after/len(queue),'without_confirmed_exact_key':len(queue)-after,'new_reviewed_literal_rows':len(records),'new_by_speaker':counts,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'extraction_pending':len(extraction),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'runtime_validation':'pending','new_resource_sha256':sha(resource),'installed_game_modified':False,'game_fps_measured':False})
 license='NPC98 public-source attribution (CC BY-NC-SA 3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n'+'\n'.join(f"{r['title']} | revision {r['revision']} | {r['source_url']}" for r in pages)+'\n'
 (ROOT/'src/main/resources/META-INF/licenses/NPC98-SOURCES.txt').write_text(license,encoding='utf-8')
 print('Prepared',len(records),counts,'partition',{'retained':len(retained),'pending':len(pending),'extraction':len(extraction),'neutral':len(neutrals),'unsafe':len(excluded)},'pages',len(pages),'blocks',len(blocks))
if __name__=='__main__':main()
