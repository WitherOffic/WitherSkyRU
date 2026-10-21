"""Batch93 compiler: authored literals, pinned full contexts and exact ordered choices."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate
from prepare_npc92 import ROOT,SOURCE,DATA,CORPUS,structured as base_structured,neutral as base_neutral,unsafe
def structured(r):return base_structured(r) or r['source']=='[NPC] Hina: Congratulations Player.'
def neutral(r):return base_neutral(r) or r['source']=='[NPC] Kuudra Archeologist: ∫ 2x - 2x^5 + 7x^3 dx'

SPEAKERS={'Adventurer Saul':13,'Alwin':13,'Azax':13,'Barbarian Guard':12,'Beekeeper Bartholomew':12,'Charlie':12,'Clerk Seraphine':14,'Cold Enjoyer':13,'Cryptosis':12,'Dax':11,'Erihann':14,'Ezekiel':13,'Fetchur':11,'Gunther':12,'Hina':13,'Julian':11,'Keran':11,'Kheharad':14,'King Yolkar':14,'Kuudra Archeologist':11,'Lazy Miner':14,'Liz':11,'Mad Redstone Engineer':11,'Maddox the Slayer':14,'Mage Guard':13,'Michael':13,'Mole':13,'Oden':13,'Old Timer':14,'Pam':12,'Rosemary':11,'Rulenor':13,'Seffea':14,'Silnar':11,"Sor'Hen":14,'Spaceman':13,'Taylor':11,'Terry':13,'Trevor':14,'Tyzzo':13,'Ulyn':13,'Weird Sailor':12,'Xalx':12}
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-18_5')
AUDIT=ROOT/'build/npc92-dispatch-3358e43dc3384efea9df6f02247d9a26/0.15.92-dev/DISPATCH_ROWS.jsonl'

def selection():
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion93.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==8735,'Immutable92 baseline changed'
 selected=[r for r in queue if r['speaker'] in SPEAKERS]
 eligible=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and not unsafe(r)]
 assert len(selected)==638 and Counter(r['speaker'] for r in eligible)==SPEAKERS
 old={r['source']:r for r in rows(AUDIT)}
 assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible)
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids]
 return queue,known,selected,eligible,occurrences,blocks

def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--choices',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');p.add_argument('--refresh-only',action='store_true');a=p.parse_args()
 queue,known,selected,eligible,occurrences,blocks=selection();ids={b['block_id'] for b in blocks};groups=defaultdict(list)
 for o in occurrences.values():
  if o['block_id'] in ids and o['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
 previous_path=PREVIOUS/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';assert sha(previous_path)==json.loads((PREVIOUS/'HASHES.json').read_text())['src/main/java/ru/wither/skyru/ChatNotificationTranslator.java']
 known_choices={tuple(json.loads('['+x+']')):json.loads('['+y+']') for x,y in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',previous_path.read_text(encoding='utf-8'))};assert len(known_choices)==246
 if a.choices:
  for key,items in groups.items():print(('OLD' if key in known_choices else 'UNSAFE' if all(unsafe(r) for r in items) else 'NEW')+' '+json.dumps(key,ensure_ascii=False))
  print('Group count',len(groups));return
 if a.inspect:
  names=a.inspect.split('|')
  if a.context:
   specific={occurrences[i]['block_id'] for r in selected if r['speaker'] in names for i in r['occurrence_ids']}
   for b in [b for b in blocks if b['block_id'] in specific][a.start:a.start+a.limit]:print(json.dumps({k:b[k] for k in ('block_id','page','headings','condition_context_raw','template_raw')},ensure_ascii=False))
  else:
   for r in [r for r in eligible if r['speaker'] in names][a.start:a.start+a.limit]:print(r['source_legacy_line'])
  return
 titles={b['page'] for b in blocks};pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages}
 if not a.validate_only:
  assert len(titles)<=50
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.93 (read-only)'}),timeout=45) as stream:response=json.load(stream)['query']['pages']
  actual={r['title']:r['revisions'][0]['revid'] for r in response};differences={k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}
  write(SOURCE/'NPC_0.15.93_PUBLIC_REVISION.json',{'checked_at':datetime.now().astimezone().isoformat(),'api_urls':[url],'matches_pinned_corpus':actual==expected,'private_or_unpublished_dialogue_known':False,'pages':response,'expected':expected,'differences':differences})
  if differences:
   delta_path=SOURCE/'NPC_0.15.93_REVISION_DELTA.json';delta=json.loads(delta_path.read_text(encoding='utf-8-sig'))
   assert delta['accepted_for_batch93'] and delta['queue_sha256']==sha(CORPUS/'translation_queue.jsonl')
   assert differences=={r['page']:{'pinned':r['pinned_revision'],'current':r['current_revision']} for r in delta['records']} and all(r['npc_utterances_and_conditions_unchanged'] for r in delta['records'])
   revision=json.loads((SOURCE/'NPC_0.15.93_PUBLIC_REVISION.json').read_text());revision.update(accepted_after_exact_delta_review=True,delta_review_sha256=sha(delta_path));write(SOURCE/'NPC_0.15.93_PUBLIC_REVISION.json',revision)
  print('API revisions checked:',len(pages),'pages')
 if a.refresh_only:return
 lookup={r['source']:r for r in eligible};records=[];seen=set();authored=[]
 for name in SPEAKERS:
  path=SOURCE/('NPC93_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
  if not path.exists():assert a.validate_only,path;continue
  for line in path.read_text(encoding='utf-8-sig').splitlines():
   if not line or line.startswith('#'):continue
   f=line.split('|');assert len(f) in (3,4),line
   speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
   assert speaker==name and source in lookup and source not in seen,(name,source);seen.add(source)
   if source!='[NPC] Weird Sailor: im goin 2 da circus':assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
   allowed={'SkyBlock','Hypixel','Minecraft','Abiphone','MVP','VIP','I','II','III','IV','V','VI','VII','VIII','IX','X','XI','XII','XIII','XIV','XV','XVI','XVII','XVIII','XIX','XX','x','scg','Stonks'}
   for token in re.findall('[A-Za-z]+',russian):assert token in allowed and token in re.findall('[A-Za-z]+',english),(source,token)
   definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
   records.append({'source_key':f'npc93.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
 print('Validated authored',len(records),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
 if a.validate_only:return
 assert len(records)==541 and seen==lookup.keys()
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.93_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in seen_choices and key not in known_choices and len(original)==len(target) and any(not unsafe(o) for o in groups[key]),key
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 assert seen_choices=={key for key,items in groups.items() if key and key not in known_choices and any(not unsafe(o) for o in items)}
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.93.json',{'scope':'Independent ordered labels,not live certification.','records':choices,'retained_groups':[{'source_labels':list(key),'target_labels':known_choices[key],'all_occurrences':items} for key,items in groups.items() if key in known_choices],'heading_only_excluded':groups.get((),[]),'historical_excluded':[o for key,items in groups.items() if key and key not in seen_choices and key not in known_choices for o in items],'blocks':blocks})
 pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)];retained=[r for r in selected if r['source'] in known]
 assert len(selected)==len(records)+len(pending)+len(neutrals)+len(excluded)+len(retained)
 (SOURCE/'NPC_0.15.93_REVIEWED.tsv').write_text('# Combined authored source;edit individual NPC93 files.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
 write(DATA/'npc-expansion93.json',{'format_version':1,'language':'ru','scope':'541 authored exact literals across43 speaker labels. Pinned public sources,not full private/live coverage.','records':records})
 markers=[{'page':r['title'],'revision':r['revision'],'page_historical_templates':re.findall(r'\{\{Historical[^}]*\}\}',r['raw_wikitext'],re.I),'removed_table_or_heading':bool(re.search(r'^.*(?:Removed|Historical).*$',r['raw_wikitext'],re.M))} for r in pages]
 write(SOURCE/'NPC_SOURCES_0.15.93.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'raw_archive_marker_candidates_not_live_certification':markers,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'extraction_pending':[],'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[]})
 write(SOURCE/'NPC_PROGRESS_0.15.93.json',{'corpus_unique_rows':len(queue),'exact_resource_before':8735,'exact_resource_after':9276,'after_percent':100*9276/len(queue),'without_confirmed_exact_key':6968,'new_reviewed_literal_rows':541,'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion93.json'),'installed_game_modified':False,'game_fps_measured':False})
 license='NPC93 public-source attribution (CC BY-NC-SA 3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n'
 license+='\n'.join(f"{r['title']} | revision {r['revision']} | {r['source_url']}" for r in pages)+'\n'
 (ROOT/'src/main/resources/META-INF/licenses/NPC93-SOURCES.txt').write_text(license,encoding='utf-8')
 print('Prepared541;partition',{'selected':len(selected),'pending':len(pending),'neutral':len(neutrals),'excluded':len(excluded),'retained':len(retained)},'blocks',len(blocks),'pages',len(pages),'choices',len(choices),'slots',sum(len(r['source_labels']) for r in choices))
if __name__=='__main__':main()
