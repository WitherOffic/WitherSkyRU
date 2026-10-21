"""Pinned-source batch88 compiler. All Russian targets and style anchors are authored separately."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
SPEAKERS={'Elise':118,'Ubik Von Neumann':106,'Marcia':33,'Master Tactician Fink':27,'Ramero':32,'Joliet':25,'Mollim':35,'Pounce':35,'Ryan':35,'Vincent':35,'Gris':34,'Redstone Miner':34,'Rogier':34,'Sirius':31,'Baar':39,'Chuckleton':38}
RUNTIME_RETAINED={'[NPC] Sirius: No one made any bids!',"[NPC] Sirius: No one made any bids! Let's move on to the next item."}
def body(r):return r['source'].split(': ',1)[1]
def structured(r):return r['variable_definition'] or bool(re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body(r)))
def neutral(r):return not re.search('[A-Za-z]',body(r))
def unsafe(r):return any(r[k] for k in ('historical_heading','alpha_page','unresolved_markup','nonstandard_tag_layout'))
def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--choices',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');a=p.parse_args()
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion88.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==5359,'Check baseline before writes'
 selected=[r for r in queue if r['speaker'] in SPEAKERS]
 eligible=[r for r in selected if r['source'] not in known and r['source'] not in RUNTIME_RETAINED and not structured(r) and not neutral(r) and not unsafe(r)]
 assert Counter(r['speaker'] for r in eligible)==SPEAKERS,Counter(r['speaker'] for r in eligible)
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for file in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/file) if b['block_id'] in ids]
 groups=defaultdict(list)
 for occurrence in occurrences.values():
  if occurrence['block_id'] in ids and occurrence['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',occurrence['source']))].append(occurrence)
 previous=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-17_11');previous_path=previous/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java'
 assert sha(previous_path)==json.loads((previous/'HASHES.json').read_text(encoding='utf-8'))['src/main/java/ru/wither/skyru/ChatNotificationTranslator.java']
 known_choices={tuple(json.loads('['+x+']')):json.loads('['+y+']') for x,y in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',previous_path.read_text(encoding='utf-8'))}
 assert len(known_choices)==135
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
 records=[];seen=set();authored=[];lookup={r['source']:r for r in eligible}
 for name in SPEAKERS:
  path=SOURCE/('NPC88_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
  if not path.exists():assert a.validate_only,path;continue
  for line in path.read_text(encoding='utf-8-sig').splitlines():
   if not line or line.startswith('#'):continue
   f=line.split('|');assert len(f) in (3,4),line
   speaker,english,russian=f[:3];s=f'[NPC] {speaker}: {english}';t=f'[NPC] {speaker}: {russian}'
   assert speaker==name and s in lookup and s not in seen,(path,s);seen.add(s)
   assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),s
   allowed={'SkyBlock','Hypixel','Abiphone','MVP','VIP'}
   if speaker=='Ubik Von Neumann':allowed.add('IQ') # Conventional stat/item abbreviation,not untranslated prose.
   if speaker in {'Ryan','Redstone Miner'}:allowed|={'I','II','III','V','IX','XIV','XXII','x'} # Published tier numerals and stack notation.
   for token in re.findall('[A-Za-z]+',russian):assert token in allowed and token in re.findall('[A-Za-z]+',english),(s,token)
   definitions=anchors(f[3]) if len(f)==4 else [];validate(s,t,definitions)
   records.append({'source_key':f'npc88.{len(records)}','speaker':speaker,'tag':'NPC','source':s,'translation':t,'style_anchors':definitions,'source_url':lookup[s]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
 print('Validated authored',len(records),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
 if a.validate_only:return
 assert seen==lookup.keys() and len(records)==691
 titles={b['page'] for b in blocks};pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages}
 response=[];urls=[]
 for at in range(0,len(titles),30):
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)[at:at+30]),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'});urls.append(url)
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.88 (read-only)'}),timeout=45) as stream:response.extend(json.load(stream)['query']['pages'])
 actual={r['title']:r['revisions'][0]['revid'] for r in response}
 write(SOURCE/'NPC_0.15.88_PUBLIC_REVISION.json',{'checked_client_date':datetime.now().date().isoformat(),'checked_at':datetime.now().astimezone().isoformat(),'api_urls':urls,'matches_pinned_corpus':actual==expected,'private_or_unpublished_dialogue_known':False,'pages':response,'expected':expected,'differences':{k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}})
 assert actual==expected,'Changed revisions require exact manual review before generating runtime data'
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.88_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in seen_choices and key not in known_choices and len(original)==len(target)
  assert any(not unsafe(o) for o in groups[key]),key
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 expected_choices={key for key,items in groups.items() if key and key not in known_choices and any(not unsafe(o) for o in items)}
 assert seen_choices==expected_choices,(seen_choices^expected_choices)
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.88.json',{'scope':'Exact independently authored ordered groups;old groups retained from verified87source,heading-only publication excluded. Separate from NPC corpus keys.','records':choices,'retained_groups':[{'source_labels':list(key),'target_labels':known_choices[key],'all_occurrences':items} for key,items in groups.items() if key in known_choices],'heading_only_excluded':groups.get((),[]),'historical_excluded':[o for key,items in groups.items() if key and key not in seen_choices and key not in known_choices for o in items],'blocks':blocks})
 print('Choice groups',len(choices),'slots',sum(len(r['source_labels']) for r in choices),'layouts',sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choices))
 pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)];retained=[r for r in selected if r['source'] in known]
 old_dispatch={r['source']:r for r in rows(ROOT/'build/npc87-dispatch-e34157338f4645af9683d63e4cad8e86/0.15.87-dev/DISPATCH_ROWS.jsonl')}
 runtime_retained=[r for r in selected if r['source'] in RUNTIME_RETAINED];assert len(runtime_retained)==2
 for r in runtime_retained:assert old_dispatch[r['source']]['flat_changed_not_quality_proof'] and old_dispatch[r['source']]['published_style_changed_not_quality_proof']
 assert len(selected)==len(records)+len(pending)+len(neutrals)+len(excluded)+len(retained)+len(runtime_retained)
 (SOURCE/'NPC_0.15.88_REVIEWED.tsv').write_text('# Combined authored source; edit individual NPC88 files.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
 write(DATA/'npc-expansion88.json',{'format_version':1,'language':'ru','scope':'691 authored exact literals across16 speaker labels; public publication,not private/live completeness. Two previous dynamic results retained without redundant keys. Structural,neutral and old rows separately counted.','records':records})
 write(SOURCE/'NPC_SOURCES_0.15.88.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[{'source':r['source'],'published_row':r,'before_flat':old_dispatch[r['source']]['flat_result'],'before_colored':old_dispatch[r['source']]['published_style_result']} for r in runtime_retained]})
 write(SOURCE/'NPC_PROGRESS_0.15.88.json',{'corpus_unique_rows':len(queue),'exact_resource_before':5359,'exact_resource_after':6050,'after_percent':100*6050/len(queue),'without_confirmed_exact_key':10194,'new_reviewed_literal_rows':691,'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'retained_dynamic_results':2,'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion88.json'),'installed_game_modified':False,'game_fps_measured':False})
 print('Prepared',len(records),'new;',len(selected),'selected;',len(pending),'structured;',len(neutrals),'neutral;',len(excluded),'unsafe;',len(retained),'retained')
if __name__=='__main__':main()
