"""Batch89: independent authored translations against immutable full public sources."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate
from prepare_npc88 import ROOT,SOURCE,DATA,CORPUS,structured,neutral,unsafe

SPEAKERS={'Alchemage':25,'Aranya':24,'Aura':27,'Mayor Aura':32,'Bruuh':32,'Corm':25,'Don Expresso':24,'Fairylosopher':30,'Gerald':26,'Gemma':29,'Grandpa Wolf':21,'Gustave':24,'Junker Joel':26,'Nall':25,'Marco':27,'Molbert':30,'Pesthunter Phillip':26,'Rabbit Bro':26,'Rabbit Daddy':24,'Rabbit Dog':32,'Rabbit Granny':21,'Rabbit Sis':30,'Rabbit Uncle':31,'Rhys':30,'Jacquelle':31,'Spider Tamer':24,'Suus':32}
RUNTIME_RETAINED={'[NPC] Pesthunter Phillip: Howdy, Player!'}
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-18_1')
AUDIT=ROOT/'build/npc88-dispatch-431da92cce9d4bde9a0f501276a142c0/0.15.88-dev/DISPATCH_ROWS.jsonl'

def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--choices',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');p.add_argument('--refresh-only',action='store_true');a=p.parse_args()
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion89.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==6050,'Immutable88 baseline changed'
 selected=[r for r in queue if r['speaker'] in SPEAKERS]
 eligible=[r for r in selected if r['source'] not in known and r['source'] not in RUNTIME_RETAINED and not structured(r) and not neutral(r) and not unsafe(r)]
 assert len(selected)==757 and Counter(r['speaker'] for r in eligible)==SPEAKERS
 old={r['source']:r for r in rows(AUDIT)}
 assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible),'Do not add unreachable or already-firing literals'
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids]
 assert len(blocks)==363
 groups=defaultdict(list)
 for o in occurrences.values():
  if o['block_id'] in ids and o['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
 previous_path=PREVIOUS/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java'
 assert sha(previous_path)==json.loads((PREVIOUS/'HASHES.json').read_text(encoding='utf-8'))['src/main/java/ru/wither/skyru/ChatNotificationTranslator.java']
 known_choices={tuple(json.loads('['+x+']')):json.loads('['+y+']') for x,y in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',previous_path.read_text(encoding='utf-8'))}
 assert len(known_choices)==183
 if a.choices:
  for key,items in groups.items():print(('OLD' if key in known_choices else 'UNSAFE' if all(unsafe(r) for r in items) else 'NEW')+' '+json.dumps(key,ensure_ascii=False))
  print('Group count',len(groups));return
 if a.inspect:
  if a.context:
   specific={occurrences[i]['block_id'] for r in selected if r['speaker']==a.inspect for i in r['occurrence_ids']}
   for b in [b for b in blocks if b['block_id'] in specific][a.start:a.start+a.limit]:print(json.dumps({k:b[k] for k in ('block_id','page','headings','condition_context_raw','template_raw')},ensure_ascii=False))
  else:
   for i,r in enumerate([r for r in eligible if r['speaker']==a.inspect][a.start:a.start+a.limit],a.start):print(str(i)+' | '+r['source_legacy_line']+' | '+' / '.join(r['headings']))
  return
 titles={b['page'] for b in blocks};pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages}
 if not a.validate_only:
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.89 (read-only)'}),timeout=45) as stream:response=json.load(stream)['query']['pages']
  actual={r['title']:r['revisions'][0]['revid'] for r in response}
  differences={k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}
  proof=json.loads((SOURCE/'NPC_0.15.89_REVISION_DELTA.json').read_text(encoding='utf-8'))
  assert differences=={'Pesthunter Phillip':{'pinned':859295,'current':859508}}
  assert proof['accepted_for_batch89'] and proof['only_expected_replacement_of_info_needed'] and proof['all_previous_dialogues_and_conditions_unchanged'] and proof['new_dialogue_count']==1 and proof['queue_sha256']==sha(CORPUS/'translation_queue.jsonl')
  write(SOURCE/'NPC_0.15.89_PUBLIC_REVISION.json',{'checked_client_date':datetime.now().date().isoformat(),'checked_at':datetime.now().astimezone().isoformat(),'api_urls':[url],'matches_pinned_corpus':actual==expected,'accepted_after_exact_delta_review':True,'delta_review_sha256':sha(SOURCE/'NPC_0.15.89_REVISION_DELTA.json'),'private_or_unpublished_dialogue_known':False,'pages':response,'expected':expected,'differences':differences})
  print('API28 unchanged pages;one reviewed page with a newly captured denial line')
 if a.refresh_only:return
 lookup={r['source']:r for r in eligible};records=[];seen=set();authored=[]
 for name in SPEAKERS:
  path=SOURCE/('NPC89_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
  if not path.exists():assert a.validate_only,path;continue
  for line in path.read_text(encoding='utf-8-sig').splitlines():
   if not line or line.startswith('#'):continue
   f=line.split('|');assert len(f) in (3,4),line
   speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
   assert speaker==name and source in lookup and source not in seen,(name,source);seen.add(source)
   assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
   allowed={'SkyBlock','Hypixel','Abiphone','MVP','VIP','I','II','III','IV','V','VI','VII','VIII','IX','X','XII','x'}
   for token in re.findall('[A-Za-z]+',russian):assert token in allowed and token in re.findall('[A-Za-z]+',english),(source,token)
   definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
   records.append({'source_key':f'npc89.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
 print('Validated authored',len(records),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
 if a.validate_only:return
 assert len(records)==734 and seen==lookup.keys()
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.89_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in seen_choices and key not in known_choices and len(original)==len(target)
  assert any(not unsafe(o) for o in groups[key]),key
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 assert seen_choices=={key for key,items in groups.items() if key and key not in known_choices and any(not unsafe(o) for o in items)}
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.89.json',{'scope':'Independent ordered button labels;old groups retained;unpublished/structural groups separately counted. Not live certification.','records':choices,'retained_groups':[{'source_labels':list(key),'target_labels':known_choices[key],'all_occurrences':items} for key,items in groups.items() if key in known_choices],'heading_only_excluded':groups.get((),[]),'historical_excluded':[o for key,items in groups.items() if key and key not in seen_choices and key not in known_choices for o in items],'blocks':blocks})
 pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)];retained=[r for r in selected if r['source'] in known];runtime_retained=[r for r in selected if r['source'] in RUNTIME_RETAINED]
 assert len(runtime_retained)==1 and all(old[r['source']]['flat_changed_not_quality_proof'] and old[r['source']]['published_style_changed_not_quality_proof'] for r in runtime_retained)
 assert len(selected)==len(records)+len(pending)+len(neutrals)+len(excluded)+len(retained)+len(runtime_retained)
 (SOURCE/'NPC_0.15.89_REVIEWED.tsv').write_text('# Combined authored source; edit individual NPC89 files.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
 write(DATA/'npc-expansion89.json',{'format_version':1,'language':'ru','scope':'734 authored exact literals across27 speaker labels;public sources,not full private/live coverage. Already-firing and structured rows separately counted.','records':records})
 write(SOURCE/'NPC_SOURCES_0.15.89.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[{'source':r['source'],'published_row':r,'before_flat':old[r['source']]['flat_result'],'before_colored':old[r['source']]['published_style_result']} for r in runtime_retained]})
 write(SOURCE/'NPC_PROGRESS_0.15.89.json',{'corpus_unique_rows':len(queue),'exact_resource_before':6050,'exact_resource_after':6784,'after_percent':100*6784/len(queue),'without_confirmed_exact_key':9460,'new_reviewed_literal_rows':734,'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'retained_dynamic_results':len(runtime_retained),'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion89.json'),'installed_game_modified':False,'game_fps_measured':False})
 print('Prepared734;partition',{'selected':len(selected),'pending':len(pending),'neutral':len(neutrals),'excluded':len(excluded),'retained':len(retained),'runtime_retained':len(runtime_retained)},'choices',len(choices),'slots',sum(len(r['source_labels']) for r in choices))
if __name__=='__main__':main()
