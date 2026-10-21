"""Pinned authored batch102: exact NPC literals and exact ordered choices, no runtime translation API."""
import argparse,json,re,urllib.parse,urllib.request
from pathlib import Path
from datetime import datetime
from collections import Counter,defaultdict
from prepare_npc101 import ROOT,SOURCE,DATA,CORPUS,structured as base_structured,neutral as base_neutral,unsafe,extraction as base_extraction
from prepare_npc78 import rows,sha,write,anchors,validate

SPEAKERS={'Mayor Aatrox': 3, 'Alabaster': 3, 'Argofay Bugshopper': 2, 'Argofay Doorholder': 3, 'Argofay Serialbather': 3, 'Argofay Tencounter': 1, 'Argofay Trailblazer': 3, 'Ashera': 3, 'Autonull': 3, 'Bartender': 1, 'Belanor': 1, 'Bingo': 1, 'Blobbercyst': 2, 'Bob': 1, 'Bruto': 1, 'Bubu': 2, 'Bylma': 1, 'Calvin': 3, 'Campfire Initiate': 3, 'Carly': 3, 'Carpenter': 1, 'Carrot King Avatar': 3, 'Cashier': 2, 'Chester': 3, 'Chicken': 2, 'Christopher': 2, 'Mayor Cole': 3, 'Counsellor': 3, 'Cow': 2, 'Cyndarin': 2, 'Dackinoru': 3, 'Dante Goon': 1, 'Darr': 1, 'Deer': 2, 'Deng': 1, 'Desperate Engineer': 1, 'Mayor Diana': 3, 'Mayor Diaz': 3, 'Dr. Phear': 1, '----': 1, 'Emissary Lissandra': 3, 'Emissary Carlton': 1, 'Emissary Ceanna': 3, 'Emissary Fraiser': 3, 'Emissary Sisko': 2, 'Emissary Wilson': 1, 'Finance Rabbit': 3, 'Mayor Finnegan': 3, 'Finplex': 2, 'Fisherman Gerald': 1, 'Forager Daniel': 3, 'Forager Rowan': 1, 'Forast': 3, 'Forger': 1, 'Mayor Foxy': 3, 'Frozen Alex': 2, 'Garlacius': 3, 'Ghost of Armorsmith': 3, 'Gladiator': 2, 'Golden Dragon': 3, 'Grandma': 1, 'Gregory': 2, 'Gwendolyn': 1, 'Gwynnie': 3, 'Hilda': 3, 'Hoppity': 1, 'Andrew': 2, 'Hunter Ava': 3, 'Hunter Jean': 3, 'Hunter McNeil': 3, 'Huntress Alissa': 3, 'Husbandry Merchant': 2, 'Iron Forger': 2, 'Ivan': 3, 'Ivy': 2, 'Jeff': 2, 'Jimmy': 1, 'Kalhuiki Door Guardian': 3, 'Kuudra Archaeologist': 3, 'Lazarus': 3, 'Lonely Philosopher': 2, 'Lost Wizard': 2, 'Ludleth': 3, 'Maggie': 1, 'Mayor Marina': 3, 'Marthos': 3, 'Martin': 3, 'Maths Enjoyer': 2, 'Maxim': 3, 'Melancholic Viking': 3, 'Mino': 3, 'Motes Grubber': 3, 'Mayor Nancy': 2, 'Minister Nancy': 2, 'Nene': 1, 'Noway': 3, 'Old Shaman Nyko': 3, 'Mayor Paul': 2, 'Pest Wrangler': 3, 'Pest Wrangler?': 3, 'Phillip': 1, 'Phaser': 3, 'Piglin Guard': 1, 'Rabbit Security': 3, 'Ravenous Rhino': 3, 'Researcher Betty': 3, 'Researcher Jade': 3, 'Researcher Jessica': 3, 'Researcher Kay': 1, 'Researcher Tay': 1, 'Researcher Timmy': 3, 'Researcher Tina': 3, 'Resident Neighbor': 2, 'Resistance Leader': 1, 'Rhanora Cat': 3, 'Roger': 3, 'Romero': 1, 'Roosterzilla': 2, 'Rusty': 2, 'Safari Shill': 2, 'Salesman': 2, 'Salvage Old Attributes': 3, 'Mayor Scorpius': 2, 'Scout Scardius': 3, 'Sheep': 3, 'Sherry': 3, 'Shifty': 1, '☣ Sick Farmer ☣': 1, 'Yoshua': 2, ' St. Jerry': 1, 'The Lotus Eater': 1, 'Unbound Explorer': 2, 'Uvia': 2, 'Vanessa': 2, 'Vargul': 2, 'Vesuvius': 2, 'Vinyl Collector': 3, 'Vreike': 3, 'Weaponsmith': 3, 'Wool Weaver': 1, 'yrreJ': 3, 'Ziri': 2}
ALL_SPEAKERS=[' St. Jerry', '----', 'Alabaster', 'Andrew', 'Argofay Bugshopper', 'Argofay Doorholder', 'Argofay Serialbather', 'Argofay Tencounter', 'Argofay Trailblazer', 'Ashera', 'Autonull', 'Bartender', 'Belanor', 'Bingo', 'Blobbercyst', 'Bob', 'Bruto', 'Brynmor', 'Bubu', 'Bylma', 'Calvin', 'Campfire Initiate', 'Carly', 'Carnival Leader', 'Carpenter', 'Carrot King Avatar', 'Cashier', 'Chester', 'Chicken', 'Christopher', 'Counsellor', 'Cow', 'Cyndarin', 'Dackinoru', 'Dalir', 'Dante Goon', 'Darr', 'Deer', 'Deng', 'Desperate Engineer', 'Dr. Phear', 'Emissary Carlton', 'Emissary Ceanna', 'Emissary Fraiser', 'Emissary Lissandra', 'Emissary Sisko', 'Emissary Wilson', 'Finance Rabbit', 'Finplex', 'Fisherman Gerald', 'Forager Daniel', 'Forager Rowan', 'Forast', 'Forger', 'Frosty', 'Frozen Alex', 'Garlacius', 'Generow', 'Ghost of Armorsmith', 'Gladiator', 'Golden Dragon', 'Grandma', 'Gregory', 'Gwendolyn', 'Gwynnie', 'Hilda', 'Hoppity', 'Hunter Ava', 'Hunter Jean', 'Hunter McNeil', 'Huntress Alissa', 'Husbandry Merchant', 'Iron Forger', 'Ivan', 'Ivy', 'Jeff', 'Jimmy', 'Kalhuiki Door Guardian', 'Kuudra Archaeologist', 'Lazarus', 'Lonely Philosopher', 'Lost Wizard', 'Lucius', 'Ludleth', 'Maggie', 'Marthos', 'Martin', 'Maths Enjoyer', 'Maxim', 'Mayor Aatrox', 'Mayor Cole', 'Mayor Diana', 'Mayor Diaz', 'Mayor Finnegan', 'Mayor Foxy', 'Mayor Marina', 'Mayor Nancy', 'Mayor Paul', 'Mayor Scorpius', 'Melancholic Viking', 'Minister Nancy', 'Mino', 'Motes Grubber', 'Nene', 'Noway', 'Old Shaman Nyko', 'Pest Wrangler', 'Pest Wrangler?', 'Pesthunter Phillip', 'Phaser', 'Phillip', 'Piglin Guard', 'Rabbit Security', 'Ravenous Rhino', 'Researcher Betty', 'Researcher Jade', 'Researcher Jessica', 'Researcher Kay', 'Researcher Tay', 'Researcher Timmy', 'Researcher Tina', 'Resident Neighbor', 'Resistance Leader', 'Rhanora Cat', 'Roger', 'Romero', 'Roosterzilla', 'Rusty', 'Safari Shill', 'Salesman', 'Salvage Old Attributes', 'Scout Scardius', 'Sheep', 'Sherry', 'Shifty', 'Sirius', 'The Lotus Eater', 'Unbound Explorer', 'Uvia', 'Vanessa', 'Vargul', 'Vesuvius', 'Vinyl Collector', 'Vreike', 'Weaponsmith', 'Wool Weaver', 'Yoshua', 'Ziri', '[Eye]', 'yrreJ', '☣ Sick Farmer ☣']
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_4')
AUDIT=ROOT/'build/npc101-dispatch-86979e7334cd487da5d0ec67b5547c34/0.15.101-dev/DISPATCH_ROWS.jsonl'
EXTRA_STRUCTURED={'[NPC] Phillip: Thanks for the Pest, Player!', '[NPC] [Eye]: Quit it!', '[NPC] Carnival Leader: Welcome to The Carnival, player!', '[NPC] Pesthunter Phillip: Howdy, Player!'}
MALFORMED={'[NPC] Mayor Nancy: Hi|', '[NPC] Generow: Click to claim your reward!', "[NPC] Frosty: I noticed you don't have a Snow Cannon, would you like to buy another?|text=", "[NPC] Brynmor: It's definitely still floating about in the waters of the Abandoned Quarry somewhere...", '[NPC] Generow: (gifts} Unique Gifts! Wow!/'}
NEUTRALS={'[NPC] Dante Goon: o7'}
REUSED={"[NPC] Dalir: You’ve now attended 4 meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!": {'flat_result': '[NPC] Dalir: Ты уже побывал на 4 встречах Культа Падшей звезды! В награду — Звездопад!', 'published_style_result': '[NPC] Dalir: Ты уже побывал на 4 встречах Культа Падшей звезды! В награду — Звездопад!'}, "[NPC] Dalir: You’ve now attended 5 meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!": {'flat_result': '[NPC] Dalir: Ты уже побывал на 5 встречах Культа Падшей звезды! В награду — Звездопад!', 'published_style_result': '[NPC] Dalir: Ты уже побывал на 5 встречах Культа Падшей звезды! В награду — Звездопад!'}, "[NPC] Dalir: You’ve now attended 6 meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!": {'flat_result': '[NPC] Dalir: Ты уже побывал на 6 встречах Культа Падшей звезды! В награду — Звездопад!', 'published_style_result': '[NPC] Dalir: Ты уже побывал на 6 встречах Культа Падшей звезды! В награду — Звездопад!'}, "[NPC] Sirius: No one made any bids! Let's move on to the next item.": {'flat_result': '[NPC] Sirius: Никто не сделал ставку! Перейдём к следующему лоту.', 'published_style_result': '[NPC] Sirius: Никто не сделал ставку! Перейдём к следующему лоту.'}, '[NPC] Sirius: No one made any bids!': {'flat_result': '[NPC] Sirius: Никто не сделал ставку!', 'published_style_result': '[NPC] Sirius: Никто не сделал ставку!'}, "[NPC] Lucius: You've purchased 7 items from the Dark Auction.": {'flat_result': '[NPC] Lucius: Ты купил 7 предметов на Тёмном аукционе.', 'published_style_result': '[NPC] Lucius: Ты купил 7 предметов на Тёмном аукционе.'}}
def structured(r):return base_structured(r) or r['source'] in EXTRA_STRUCTURED
def extraction(r):return base_extraction(r) or r['source'] in MALFORMED
def neutral(r):return base_neutral(r) or r['source'] in NEUTRALS
def reused(r):return r['source'] in REUSED
def choice_unsafe(r):return unsafe(r) or r['source']=='Select an option: [Yeah].'

def selection():
 queue=rows(CORPUS/'translation_queue.jsonl');known=defaultdict(list)
 assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 for path in sorted(DATA.glob('*.json')):
  if path.name=='npc-expansion102.json':continue
  for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].append(r)
 assert sum(r['source'] in known for r in queue)==10776
 selected=[r for r in queue if r['speaker'] in ALL_SPEAKERS and r['tag']=='NPC']
 eligible=[r for r in selected if r['source'] not in known and not structured(r) and not neutral(r) and not unsafe(r) and not extraction(r) and not reused(r)]
 assert Counter(r['speaker'] for r in eligible)==SPEAKERS,Counter(r['speaker'] for r in eligible)
 old={r['source']:r for r in rows(AUDIT)}
 assert all(r['source'] in old and not old[r['source']]['flat_changed_not_quality_proof'] and not old[r['source']]['published_style_changed_not_quality_proof'] for r in eligible)
 occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
 ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
 blocks=[b for f in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/f) if b['block_id'] in ids]
 pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in {b['page'] for b in blocks}]
 return queue,known,selected,eligible,occurrences,blocks,pages

def choice_groups(occurrences,blocks):
 ids={b['block_id'] for b in blocks};groups=defaultdict(list)
 for o in occurrences.values():
  if o['block_id'] in ids and o['source'].startswith('Select an option:'):groups[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
 rel='src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';path=PREVIOUS/rel;assert sha(path)==json.loads((PREVIOUS/'HASHES.json').read_text())[rel]
 known={tuple(json.loads('['+x+']')):json.loads('['+y+']') for x,y in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',path.read_text(encoding='utf-8'))};assert len(known)==282
 return groups,known

def main():
 p=argparse.ArgumentParser();p.add_argument('--inspect');p.add_argument('--context',action='store_true');p.add_argument('--choices',action='store_true');p.add_argument('--start',type=int,default=0);p.add_argument('--limit',type=int,default=100);p.add_argument('--refresh',action='store_true');p.add_argument('--validate-only',action='store_true');a=p.parse_args()
 queue,known,selected,eligible,occurrences,blocks,pages=selection();groups,known_choices=choice_groups(occurrences,blocks)
 if a.choices:
  for key,items in groups.items():print(('OLD' if key in known_choices else 'UNSAFE' if all(choice_unsafe(r) for r in items) else 'NEW')+' '+json.dumps(key,ensure_ascii=False))
  print('Groups',len(groups));return
 if a.inspect:
  names=set(a.inspect.split('|'))
  if a.context:
   ids={occurrences[i]['block_id'] for r in selected if r['speaker'] in names for i in r['occurrence_ids']}
   for b in [b for b in blocks if b['block_id'] in ids][a.start:a.start+a.limit]:print(json.dumps({k:b[k] for k in ('block_id','page','headings','condition_context_raw','template_raw')},ensure_ascii=False))
  else:
   for r in [r for r in eligible if r['speaker'] in names][a.start:a.start+a.limit]:print(r['source_legacy_line'])
  return
 if a.refresh:
  titles=sorted(r['title'] for r in pages);response=[];urls=[]
  for start in range(0,len(titles),30):
   url='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(titles[start:start+30]),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'});urls.append(url)
   with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'WitherSkyRU-public-corpus-review/0.15.102 (read-only)'}),timeout=45) as stream:response.extend(json.load(stream)['query']['pages'])
  expected={r['title']:r['revision'] for r in pages};actual={r['title']:r['revisions'][0]['revid'] for r in response}
  write(SOURCE/'NPC_0.15.102_PUBLIC_REVISION.json',{'checked_at':datetime.now().astimezone().isoformat(),'api_urls':urls,'matches_pinned_corpus':actual==expected,'expected':expected,'actual':actual,'pages':response,'private_or_unpublished_dialogue_known':False})
  print('Revision comparison',actual==expected,{k:(expected.get(k),actual.get(k)) for k in expected.keys()|actual.keys() if expected.get(k)!=actual.get(k)});return
 lookup={r['source']:r for r in eligible};records=[];seen=set()
 for line in (SOURCE/'NPC_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  f=line.split('|');assert len(f) in (3,4),line
  speaker,english,russian=f[:3];source=f'[NPC] {speaker}: {english}';target=f'[NPC] {speaker}: {russian}'
  assert source in lookup and source not in seen,(speaker,english);seen.add(source)
  assert re.findall('[0-9]+',english)==re.findall('[0-9]+',russian),source
  allowed={'SkyBlock','Hypixel','Minecraft','Abiphone','VIP','MVP','I','II','III','IV','V','VI','VII','VIII','IX','X','x','Stonks','SkyMart','TNT'}
  if source=='[NPC] Madame Eleanor Q. Goldsworth V: Then say "Eleanor is the best!"':allowed|={'Eleanor','is','the','best'}
  if source=="[NPC] Gavin: Don't tell mum this, but I just use /scg to open it.":allowed.add('scg')
  if source.startswith('[NPC] Maths Enjoyer:'):allowed|={'ln','AP'}
  if source.startswith('[NPC] Salesman:'):allowed|={'store','hypixel','net'}
  for token in re.findall('[A-Za-z]+',russian):assert token in allowed and token in re.findall('[A-Za-z]+',english),(source,token)
  definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
  records.append({'source_key':f'npc100.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':lookup[source]['source_url'],'evidence':'pinned_public_wiki_revision_not_live_packet'})
 print('Validated',len(records),'of',len(eligible))
 if a.validate_only:return
 assert seen==lookup.keys(),sorted(lookup.keys()-seen)
 choices=[];seen_choices=set()
 for line in (SOURCE/'NPC_CHOICES_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if not line or line.startswith('#'):continue
  original,target=map(json.loads,line.split('|'));key=tuple(original)
  assert key in groups and key not in known_choices and key not in seen_choices and len(original)==len(target) and any(not choice_unsafe(o) for o in groups[key]),key
  seen_choices.add(key);choices.append({'source_labels':original,'target_labels':target,'all_occurrences':groups[key]})
 assert seen_choices=={key for key,items in groups.items() if key and key not in known_choices and any(not choice_unsafe(o) for o in items)}
 write(SOURCE/'NPC_CHOICES_SOURCES_0.15.102.json',{'scope':'Exact authored ordered groups;not standalone unheaded buttons or live coverage.','records':choices,'retained_groups':[{'source_labels':list(key),'target_labels':known_choices[key],'all_occurrences':items} for key,items in groups.items() if key in known_choices],'heading_only_excluded':groups.get((),[]),'excluded_groups_not_runtime':[o for key,items in groups.items() if key and key not in seen_choices and key not in known_choices for o in items],'blocks':blocks})
 reused_rows=[r for r in selected if reused(r)];previous_outputs={r['source']:r for r in rows(AUDIT)}
 for r in reused_rows:assert all(previous_outputs[r['source']][k]==v for k,v in REUSED[r['source']].items())
 retained=[r for r in selected if r['source'] in known];pending=[r for r in selected if r['source'] not in known and structured(r)];malformed=[r for r in selected if r['source'] not in known and not structured(r) and extraction(r)];neutrals=[r for r in selected if r['source'] not in known and not structured(r) and not extraction(r) and neutral(r)];excluded=[r for r in selected if r['source'] not in known and not structured(r) and not extraction(r) and not neutral(r) and unsafe(r)]
 assert len(selected)==sum(map(len,(records,retained,pending,malformed,neutrals,excluded,reused_rows)))
 resource=DATA/'npc-expansion102.json';write(resource,{'format_version':1,'language':'ru','scope':f'{len(records)} authored NPC literals across{len(SPEAKERS)} labels. Pinned public sources including archival mayor material;not live/private completeness. Startup merge into existing story provider.','records':records})
 markers=[{'page':r['title'],'revision':r['revision'],'page_historical_templates':re.findall(r'\{\{Historical[^}]*\}\}',r['raw_wikitext'],re.I),'removed_table_or_heading':bool(re.search(r'^.*(?:Removed|Historical).*$',r['raw_wikitext'],re.M))} for r in pages]
 write(SOURCE/'NPC_SOURCES_0.15.102.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'source_pages':pages,'blocks':blocks,'raw_archive_marker_candidates_not_live_certification':markers,'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained],'pending_variables':pending,'extraction_pending':malformed,'neutral_unchanged':neutrals,'unsafe_excluded':excluded,'runtime_retained':[{'source':r['source'],'published_row':r,'before_flat':previous_outputs[r['source']]['flat_result'],'before_colored':previous_outputs[r['source']]['published_style_result']} for r in reused_rows]})
 after=10776+len(records);write(SOURCE/'NPC_PROGRESS_0.15.102.json',{'corpus_unique_rows':len(queue),'exact_resource_before':10776,'exact_resource_after':after,'after_percent':100*after/len(queue),'without_confirmed_exact_key':len(queue)-after,'new_reviewed_literal_rows':len(records),'new_by_speaker':dict(Counter(r['speaker'] for r in records)),'published_branch_rows':len(selected),'structured_sources_not_registered':len(pending),'neutral_unchanged_rows':len(neutrals),'unsafe_excluded_rows':len(excluded),'retained_keys':len(retained),'new_choice_groups':len(choices),'new_choice_slots':sum(len(r['source_labels']) for r in choices),'runtime_validation':'pending','new_resource_sha256':sha(resource),'installed_game_modified':False,'game_fps_measured':False})
 license='NPC102 public-source attribution (CC BY-NC-SA 3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n'+'\n'.join(f"{r['title']} | revision {r['revision']} | {r['source_url']}" for r in pages)+'\n';(ROOT/'src/main/resources/META-INF/licenses/NPC102-SOURCES.txt').write_text(license,encoding='utf-8')
 print('Prepared',len(records),'partition',len(selected),len(retained),len(pending),len(neutrals),len(excluded),'pages',len(pages),'blocks',len(blocks),'choices',len(choices),'slots',sum(len(r['source_labels']) for r in choices))
if __name__=='__main__':main()
