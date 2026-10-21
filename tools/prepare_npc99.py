"""Authored public NPC batch99; exact targets/styles and ordered choices, startup data only."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from prepare_npc93 import ROOT,SOURCE,DATA,CORPUS,structured as base_structured,neutral,unsafe
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate

SPEAKERS={name:10 for name in ['Argofay Trafficker','Chipper McPickaxe','Dirt Guy','Friendly Hiker','Gimley','Helia','Honey','Hornum','Pesthunter Pamela','Plate','Sargwyn','Shen Albedo','Shepherd','Talia','Tarwen','Tomioka','Tony','Udium']}
SPEAKERS.update({name:9 for name in ['Avorius','Bomin','Crag','Fake Enigma Soul','Geonathan Greatforge','Ike','Ribbit','Lift Operator','Nicole','Researcher Gilbert','Rock','Shen Rubedo','Sludge','Talbot','Pete','Vulcan','Windmill Operator','Worker Xavier']})
SPEAKERS['Centauri']=36
SPEAKERS['Helia']=9
SPEAKERS.update({'Keith':6,'Ede':6,'Kiermet':8})
PREVIOUS=ROOT/'build/npc98-dispatch-6d5ac00b53a44538a68ffaca7de85a86/0.15.98-dev'

def structured(r):return base_structured(r) or r['source']=="[NPC] Helia: Great job, Player! I look forward to seeing where your journey takes you next!"

def selection():
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion99.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==9612
 selected=[r for r in queue if r['speaker'] in SPEAKERS]
 eligible=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and not unsafe(r)]
 assert Counter(r['speaker'] for r in eligible)==SPEAKERS
 old={r['source']:r for r in rows(PREVIOUS/'DISPATCH_ROWS.jsonl')}
 assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible)
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for f in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/f) if b['block_id'] in ids]
 pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in {b['page'] for b in blocks}]
 return queue,known,selected,eligible,occurrences,blocks,pages

def choice_groups(occurrences,blocks):
 groups=defaultdict(list);ids={b['block_id'] for b in blocks}
 for o in occurrences.values():
  if o['block_id'] in ids and o['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
 # The current map may contain99 additions after registration; exclude them when
 # reproducing the same resource/provenance, using immutable93 delivery instead.
 previous=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-18_6')
 rel='src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';old_path=previous/rel
 assert sha(old_path)==json.loads((previous/'HASHES.json').read_text())[rel]
 known={tuple(json.loads('['+x+']')):json.loads('['+y+']') for x,y in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',old_path.read_text(encoding='utf-8'))}
 assert len(known)==257
 return groups,known

def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--choices',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--refresh',action='store_true');p.add_argument('--validate-only',action='store_true');a=p.parse_args()
 queue,known,selected,eligible,occurrences,blocks,pages=selection();groups,known_choices=choice_groups(occurrences,blocks)
 if a.choices:
  for key,items in groups.items():print(('OLD' if key in known_choices else 'UNSAFE' if all(unsafe(r) for r in items) else 'NEW')+' '+json.dumps(key,ensure_ascii=False))
  print('Group count',len(groups));return
 if a.inspect:
  names=set(a.inspect.split('|'));subset=[r for r in eligible if r['speaker'] in names]
  if a.context:
   ids={occurrences[i]['block_id'] for r in selected if r['speaker'] in names for i in r['occurrence_ids']}
   for b in [b for b in blocks if b['block_id'] in ids][a.start:a.start+a.limit]:print(json.dumps({k:b[k] for k in ('block_id','page','headings','condition_context_raw','template_raw')},ensure_ascii=False))
  else:
   for r in subset[a.start:a.start+a.limit]:print(r['source_legacy_line'])
  return
 if a.refresh:
  titles=sorted(r['title'] for r in pages);assert len(titles)<=50
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(titles),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-corpus-review/0.15.99 (read-only)'}),timeout=45) as stream:response=json.load(stream)['query']['pages']
  expected={r['title']:r['revision'] for r in pages};actual={r['title']:r['revisions'][0]['revid'] for r in response}
  write(SOURCE/'NPC_0.15.99_PUBLIC_REVISION.json',{'checked_at':datetime.now().astimezone().isoformat(),'api_url':url,'matches_pinned_corpus':actual==expected,'expected':expected,'actual':actual,'pages':response,'private_or_unpublished_dialogue_known':False})
  print('Revision comparison',actual==expected,{k:(expected.get(k),actual.get(k)) for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)});return
 lookup={r['source']:r for r in eligible};records=[];seen=set()
 for line in (SOURCE/'NPC_0.15.99_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  f=line.split('|');assert len(f) in (3,4),line
  speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
  assert source in lookup and source not in seen,(speaker,english);seen.add(source)
  assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
  for token in re.findall('[A-Za-z]+',russian):assert token in {'SkyBlock','Hypixel','Minecraft','Abiphone','VIP','MVP','I','II','III','IV','V','VI','VII','VIII','IX','X','x','centauri','SkyMart'} and token in re.findall('[A-Za-z]+',english),(source,token)
  definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
  records.append({'source_key':f'npc99.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'pinned_public_wiki_revision_not_live_packet'})
 print('Validated',len(records),'of',len(eligible))
 if a.validate_only:return
 assert seen==lookup.keys(),sorted(lookup.keys()-seen)
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.99_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in known_choices and key not in seen_choices and len(original)==len(target) and any(not unsafe(o) for o in groups[key]),key
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 assert seen_choices=={key for key,items in groups.items() if key and key not in known_choices and any(not unsafe(o) for o in items)}
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.99.json',{'scope':'Exact independently authored ordered groups. Not standalone unheaded buttons/live completeness.','records':choices,'retained_groups':[{'source_labels':list(key),'target_labels':known_choices[key],'all_occurrences':items} for key,items in groups.items() if key in known_choices],'heading_only_excluded':groups.get((),[]),'historical_excluded':[o for key,items in groups.items() if key and key not in seen_choices and key not in known_choices for o in items],'blocks':blocks})
 retained=[r for r in selected if r['source'] in known];pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)]
 assert len(selected)==sum(map(len,(records,retained,pending,neutrals,excluded)))
 resource=DATA/'npc-expansion99.json';write(resource,{'format_version':1,'language':'ru','scope':'397 authored exact literals across40 speaker labels. Pinned public sources,including Centauri alpha-server dialogue;not live/private completeness. Startup merge into existing story provider.','records':records})
 markers=[{'page':r['title'],'revision':r['revision'],'page_historical_templates':re.findall(r'\{\{Historical[^}]*\}\}',r['raw_wikitext'],re.I),'removed_table_or_heading':bool(re.search(r'^.*(?:Removed|Historical).*$',r['raw_wikitext'],re.M))} for r in pages]
 write(SOURCE/'NPC_SOURCES_0.15.99.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'raw_archive_marker_candidates_not_live_certification':markers,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'extraction_pending':[],'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[]})
 after=9612+len(records);write(SOURCE/'NPC_PROGRESS_0.15.99.json',{'corpus_unique_rows':len(queue),'exact_resource_before':9612,'exact_resource_after':after,'after_percent':100*after/len(queue),'without_confirmed_exact_key':len(queue)-after,'new_reviewed_literal_rows':len(records),'new_by_speaker':dict(Counter(r['speaker'] for r in records)),'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'new_choice_groups':len(choices),'new_choice_slots':sum(len(r['source_labels']) for r in choices),'runtime_validation':'pending','new_resource_sha256':sha(resource),'installed_game_modified':False,'game_fps_measured':False})
 license='NPC99 public-source attribution (CC BY-NC-SA 3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n'+'\n'.join(f"{r['title']} | revision {r['revision']} | {r['source_url']}" for r in pages)+'\n';(ROOT/'src/main/resources/META-INF/licenses/NPC99-SOURCES.txt').write_text(license,encoding='utf-8')
 print('Prepared',len(records),'partition',len(selected),len(retained),len(pending),len(neutrals),len(excluded),'pages',len(pages),'blocks',len(blocks),'choices',len(choices),'slots',sum(len(r['source_labels']) for r in choices))
if __name__=='__main__':main()
