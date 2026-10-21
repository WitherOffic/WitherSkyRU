"""Pinned-source batch87 compiler. All Russian targets and style anchors are authored separately."""
import argparse,json,re,urllib.parse,urllib.request
from collections import Counter,defaultdict
from datetime import datetime
from pathlib import Path
from prepare_npc78 import rows,sha,write,anchors,validate
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
SPEAKERS={'Detective Amog':156,'Oruo The Almighty':56,'David Hunterborough':56,'Argofay Bughunter':57,'Sirih':48,'Kysha':47,'Loras':47,'Guber':35,'Master Tao':35,'Odger':36,'Sorcerer Okron':36,'Velyna':36,'Cowboy Nick':28}
def body(r):return r['source'].split(': ',1)[1]
def structured(r):return r['variable_definition'] or bool(re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body(r)))
def neutral(r):return not re.search('[A-Za-z]',body(r))
def unsafe(r):return any(r[k] for k in ('historical_heading','alpha_page','unresolved_markup','nonstandard_tag_layout'))
def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--validate-only',action='store_true');a=p.parse_args()
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion87.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==4686,'Check baseline before writes'
 selected=[r for r in queue if r['speaker'] in SPEAKERS]
 eligible=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and not unsafe(r)]
 assert Counter(r['speaker'] for r in eligible)==SPEAKERS,Counter(r['speaker'] for r in eligible)
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for file in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/file) if b['block_id'] in ids]
 if a.inspect:
  if a.context:
   specific={occurrences[i]['block_id'] for r in selected if r['speaker']==a.inspect for i in r['occurrence_ids']}
   for b in [b for b in blocks if b['block_id'] in specific][a.start:a.start+a.limit]:print(json.dumps({k:b[k] for k in ('block_id','page','headings','condition_context_raw','template_raw')},ensure_ascii=False))
  else:
   for i,r in enumerate([r for r in eligible if r['speaker']==a.inspect][a.start:a.start+a.limit],a.start):print(str(i)+' | '+r['source_legacy_line']+' | '+' / '.join(r['headings']))
  return
 records=[];seen=set();authored=[];lookup={r['source']:r for r in eligible}
 for name in SPEAKERS:
  path=SOURCE/('NPC87_'+re.sub('[^A-Z0-9]+','_',name.upper())+'_REVIEWED.tsv')
  if not path.exists():assert a.validate_only,path;continue
  for line in path.read_text(encoding='utf-8-sig').splitlines():
   if not line or line.startswith('#'):continue
   f=line.split('|');assert len(f) in (3,4),line
   speaker,english,russian=f[:3];s=f'[NPC] {speaker}: {english}';t=f'[NPC] {speaker}: {russian}'
   assert speaker==name and s in lookup and s not in seen,(path,s);seen.add(s)
   assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),s
   allowed={'SkyBlock','Hypixel','Abiphone','MVP','VIP'}
   if speaker=='Detective Amog' and english=='You should try to shout "Help Wizardman!"':allowed|={'Help','Wizardman'} # Exact server-trigger phrase must remain typeable.
   if speaker=='Sorcerer Okron':allowed|={'Validus','Fortis','Pernimius','Conclamatus','Vis','Robur','Firmitas','Potentia'} # Proper Latin glyph names match existing item titles.
   for token in re.findall('[A-Za-z]+',russian):assert token in allowed and token in re.findall('[A-Za-z]+',english),(s,token)
   definitions=anchors(f[3]) if len(f)==4 else [];validate(s,t,definitions)
   records.append({'source_key':f'npc87.{len(records)}','speaker':speaker,'tag':'NPC','source':s,'translation':t,'style_anchors':definitions,'source_url':lookup[s]['source_url'],'evidence':'public_wiki_revision_not_live_packet'});authored.append(line)
 print('Validated authored',len(records),'of',len(eligible),dict(Counter(r['speaker'] for r in records)))
 if a.validate_only:return
 assert seen==lookup.keys() and len(records)==673
 titles={b['page'] for b in blocks};pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages}
 response=[];urls=[]
 for at in range(0,len(titles),30):
  url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)[at:at+30]),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'});urls.append(url)
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-local-review/0.15.87 (read-only)'}),timeout=45) as stream:response.extend(json.load(stream)['query']['pages'])
 actual={r['title']:r['revisions'][0]['revid'] for r in response}
 write(SOURCE/'NPC_0.15.87_PUBLIC_REVISION.json',{'checked_client_date':datetime.now().date().isoformat(),'checked_at':datetime.now().astimezone().isoformat(),'api_urls':urls,'matches_pinned_corpus':actual==expected,'private_or_unpublished_dialogue_known':False,'pages':response,'expected':expected,'differences':{k:{'pinned':expected.get(k),'current':actual.get(k)} for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)}})
 assert actual==expected,'Changed revisions require exact manual review before generating runtime data'
 groups=defaultdict(list)
 for occurrence in occurrences.values():
  if occurrence['block_id'] in ids and occurrence['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',occurrence['source']))].append(occurrence)
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.87_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in seen_choices and len(original)==len(target)
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 assert len(choices)==63 and sum(len(r['source_labels']) for r in choices)==67
 assert set(groups)-seen_choices=={('YES','NO'),("(rabbit's name) nibbed at them.",),('Click HERE to break barricade.',)}
 assert all(r['historical_heading'] for r in groups[('Click HERE to break barricade.',)])
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.87.json',{'scope':'63 exact ordered groups,67 label slots including one unchanged ellipsis;64 distinct translated label texts,one old group retained,one actual rabbit-name placeholder and one removed branch excluded.','records':choices,'retained_groups':[{'source_labels':['YES','NO'],'all_occurrences':groups[('YES','NO')]}],'structural_excluded':groups[("(rabbit's name) nibbed at them.",)],'historical_excluded':groups[('Click HERE to break barricade.',)],'blocks':blocks})
 print('Choice layouts',sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choices))
 pending=[r for r in selected if r['source'] not in known and structured(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and unsafe(r)];retained=[r for r in selected if r['source'] in known]
 assert len(selected)==len(records)+len(pending)+len(neutrals)+len(excluded)+len(retained)
 (SOURCE/'NPC_0.15.87_REVIEWED.tsv').write_text('# Combined authored source; edit individual NPC87 files.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
 write(DATA/'npc-expansion87.json',{'format_version':1,'language':'ru','scope':'673 authored exact literals across13 speaker labels; public publication,not private/live completeness. Structural,neutral and previous keys separately counted.','records':records})
 write(SOURCE/'NPC_SOURCES_0.15.87.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[]})
 write(SOURCE/'NPC_PROGRESS_0.15.87.json',{'corpus_unique_rows':len(queue),'exact_resource_before':4686,'exact_resource_after':5359,'after_percent':100*5359/len(queue),'without_confirmed_exact_key':10885,'new_reviewed_literal_rows':673,'new_by_speaker':SPEAKERS,'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion87.json'),'installed_game_modified':False,'game_fps_measured':False})
 print('Prepared',len(records),'new;',len(selected),'selected;',len(pending),'structured;',len(neutrals),'neutral;',len(excluded),'unsafe;',len(retained),'retained')
if __name__=='__main__':main()
