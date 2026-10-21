"""Batch92: source-backed literals and closed reviewed choices up to11 buttons."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate
from prepare_npc88 import ROOT,SOURCE,DATA,CORPUS,structured as base_structured,neutral,unsafe

def structured(r):return base_structured(r) or r['speaker']=='Brian' and any(x in r['source'] for x in ('(Soul Campfire Badge)','(Materials)'))

SPEAKERS={'Offea':18,'Pablo':18,'Professor Robot':18,'Rhanora':18,'Brynmor':16,'Chak':17,'Chuk':17,'Enigma':17,'Etc':17,'Frankie':17,'Fred':17,'Grog':17,'Igor':17,'Kiara':17,'The Handler':17,'Agatha':16,'Arba':16,'Blacksmith':16,'Brian':13,'Feast Chef Ted':16,'Jaeger':16,'Mage Emissary':16,'Safari Manager':16,'"Hunter" Tobias':15,'Arch':15,'Bramass Beastslayer':15,'Chef':15,'Einary':15,'Generow':13,'Haymitch':15,'Lys':15,'Nemo':15,'Queen Mismyla':15,'Rabbit Cousin':15,'Reed':15,'Scholar Alluin':15,'Treasure Hunter':15}
LITERAL_EXCEPTIONS=set()
EXTRACTION_PENDING={"[NPC] Brynmor: It's definitely still floating about in the waters of the Abandoned Quarry somewhere...":'Published line has leading space before NPC tag;not a supported exact layout', '[NPC] Generow: (gifts} Unique Gifts! Wow!/':'Malformed variable and slash-joined two NPC messages in publication','[NPC] Generow: Click to claim your reward!':'Same malformed slash-joined publication;standalone runtime layout not certified'}
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-18_4')
AUDIT=ROOT/'build/npc91-dispatch-4d3e2130de7146d6bbcf2b475c90ea37/0.15.91-dev/DISPATCH_ROWS.jsonl'

def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--choices',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');p.add_argument('--refresh-only',action='store_true');a=p.parse_args()
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion92.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==8144,'Immutable91 baseline changed'
 selected=[r for r in queue if r['speaker'] in SPEAKERS]
 eligible=[r for r in selected if r['source'] not in known and r['source'] not in EXTRACTION_PENDING and (not structured(r) or r['source'] in LITERAL_EXCEPTIONS) and not neutral(r) and not unsafe(r)]
 assert len(selected)==671 and Counter(r['speaker'] for r in eligible)==SPEAKERS
 old={r['source']:r for r in rows(AUDIT)}
 assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible if r['source'] not in LITERAL_EXCEPTIONS),'No unreachable/already-firing literals'
 assert not LITERAL_EXCEPTIONS&old.keys(),'This one exception is outside the generic safe-literal dispatch probe'
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids]
 groups=defaultdict(list)
 for o in occurrences.values():
  if o['block_id'] in ids and o['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
 previous_path=PREVIOUS/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';assert sha(previous_path)==json.loads((PREVIOUS/'HASHES.json').read_text(encoding='utf-8'))['src/main/java/ru/wither/skyru/ChatNotificationTranslator.java']
 known_choices={tuple(json.loads('['+x+']')):json.loads('['+y+']') for x,y in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',previous_path.read_text(encoding='utf-8'))};assert len(known_choices)==232
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
  assert len(titles)<=50
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.92 (read-only)'}),timeout=45) as stream:response=json.load(stream)['query']['pages']
  actual={r['title']:r['revisions'][0]['revid'] for r in response};differences={k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}
  write(SOURCE/'NPC_0.15.92_PUBLIC_REVISION.json',{'checked_client_date':datetime.now().date().isoformat(),'checked_at':datetime.now().astimezone().isoformat(),'api_urls':[url],'matches_pinned_corpus':actual==expected,'private_or_unpublished_dialogue_known':False,'pages':response,'expected':expected,'differences':differences})
  if differences:
   delta_path=SOURCE/'NPC_0.15.92_REVISION_DELTA.json';delta=json.loads(delta_path.read_text(encoding='utf-8-sig'))
   assert delta['accepted_for_batch92'] and delta['queue_sha256']==sha(CORPUS/'translation_queue.jsonl')
   assert differences=={r['page']:{'pinned':r['pinned_revision'],'current':r['current_revision']} for r in delta['records']} and all(r['npc_utterances_and_conditions_unchanged'] for r in delta['records'])
   revision=json.loads((SOURCE/'NPC_0.15.92_PUBLIC_REVISION.json').read_text(encoding='utf-8-sig'));revision.update(accepted_after_exact_delta_review=True,delta_review_sha256=sha(delta_path));write(SOURCE/'NPC_0.15.92_PUBLIC_REVISION.json',revision)
  print('API revisions checked:',len(pages),'pages;reviewed differences:',len(differences))
 if a.refresh_only:return
 lookup={r['source']:r for r in eligible};records=[];seen=set();authored=[]
 for name in SPEAKERS:
  path=SOURCE/('NPC92_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
  if not path.exists():assert a.validate_only,path;continue
  for line in path.read_text(encoding='utf-8-sig').splitlines():
   if not line or line.startswith('#'):continue
   f=line.split('|');assert len(f) in (3,4),line
   speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
   assert speaker==name and source in lookup and source not in seen,(name,source);seen.add(source)
   assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
   allowed={'SkyBlock','Hypixel','Minecraft','Abiphone','MVP','VIP','I','II','III','IV','V','VI','VII','VIII','IX','X','XI','XII','XIII','XIV','XV','XVI','XVII','XVIII','XIX','XX','x','scg'}
   if speaker=='"Hunter" Tobias':allowed|={'sorry','I','am','very','Tobias','and','regret','my','actions','deeply'} # Literal chat triggers,with Russian explanations.
   if speaker=='Professor Robot':allowed.add('FTX')
   if speaker=='Frankie':allowed.add('VPN')
   for token in re.findall('[A-Za-z]+',russian):assert token in allowed and token in re.findall('[A-Za-z]+',english),(source,token)
   definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
   records.append({'source_key':f'npc92.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
 print('Validated authored',len(records),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
 if a.validate_only:return
 assert len(records)==591 and seen==lookup.keys()
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.92_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in seen_choices and key not in known_choices and len(original)==len(target) and any(not unsafe(o) for o in groups[key]),key
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 assert seen_choices=={key for key,items in groups.items() if key and key not in known_choices and any(not unsafe(o) for o in items)}
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.92.json',{'scope':'Independent ordered labels,not live certification.','records':choices,'retained_groups':[{'source_labels':list(key),'target_labels':known_choices[key],'all_occurrences':items} for key,items in groups.items() if key in known_choices],'heading_only_excluded':groups.get((),[]),'historical_excluded':[o for key,items in groups.items() if key and key not in seen_choices and key not in known_choices for o in items],'blocks':blocks})
 pending=[r for r in selected if r['source'] not in known and r['source'] not in LITERAL_EXCEPTIONS and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)];retained=[r for r in selected if r['source'] in known]
 extraction=[{'source':r['source'],'reason':EXTRACTION_PENDING[r['source']],'published_row':r} for r in selected if r['source'] in EXTRACTION_PENDING]
 assert len(selected)==len(records)+len(pending)+len(neutrals)+len(excluded)+len(retained)+len(extraction)
 (SOURCE/'NPC_0.15.92_REVIEWED.tsv').write_text('# Combined authored source;edit individual NPC92 files.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
 write(DATA/'npc-expansion92.json',{'format_version':1,'language':'ru','scope':'591 authored exact literals across37 speaker labels. Public pinned sources,not full private/live coverage. Tobias literal chat triggers retained with Russian explanations. Three malformed/extraction candidates explicitly pending.','records':records})
 markers=[{'page':r['title'],'revision':r['revision'],'page_historical_templates':re.findall(r'\{\{Historical[^}]*\}\}',r['raw_wikitext'],re.I),'removed_table_or_heading':bool(re.search(r'^.*(?:Removed|Historical).*$',r['raw_wikitext'],re.M))} for r in pages]
 write(SOURCE/'NPC_SOURCES_0.15.92.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'raw_archive_marker_candidates_not_live_certification':markers,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'extraction_pending':extraction,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[]})
 write(SOURCE/'NPC_PROGRESS_0.15.92.json',{'corpus_unique_rows':len(queue),'exact_resource_before':8144,'exact_resource_after':8735,'after_percent':100*8735/len(queue),'without_confirmed_exact_key':7509,'new_reviewed_literal_rows':591,'explicit_literal_exceptions':sorted(LITERAL_EXCEPTIONS),'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'extraction_pending':len(extraction),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'retained_dynamic_results':0,'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion92.json'),'installed_game_modified':False,'game_fps_measured':False})
 print('Prepared591;partition',{'selected':len(selected),'pending':len(pending),'extraction':len(extraction),'neutral':len(neutrals),'excluded':len(excluded),'retained':len(retained)},'blocks',len(blocks),'pages',len(pages),'choices',len(choices),'slots',sum(len(r['source_labels']) for r in choices))
if __name__=='__main__':main()
