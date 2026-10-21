"""Additional public endpoints,independent boundaries and a real-JAR review probe."""
import argparse,json,re,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from audit_five_pass117 import ROOT,PUBLIC,Download,rows,jsonl,write,digest,stamp,COLOR,visible_field,json_strings
from export_public_text116 import templates,split_parameters,provisional_text,RESOURCE_ENDPOINTS

MELODY_UI=[
 ('Hymn to the Joy','Ода к радости'),('Frère Jacques','Братец Жак'),('Amazing Grace','О, благодать'),("Brahm's Lullaby",'Колыбельная Брамса'),('Happy Birthday to You','С днём рождения тебя'),('Greensleeves','Зелёные рукава'),('Geothermy?','Геотермия?'),('Minuet','Менуэт'),('Joy to the World','Радуйся, мир'),('Godly Imagination','Божественное воображение'),('La Vie en Rose','Жизнь в розовом цвете'),
 ('Easy Song','Лёгкая мелодия'),('Hard Song','Сложная мелодия'),('Expert Song','Мелодия для экспертов'),('Virtuoso Song','Мелодия для виртуозов'),('Best:','Лучший результат:'),('Best: 100%!','Лучший результат: 100%!'),('Click to select song!','Нажмите, чтобы выбрать мелодию!'),('Useful Tip!','Полезный совет!'),
 ('If you are having lag issues, try','Если игра подтормаживает, попробуйте'),('using middle click (clicking the','нажимать среднюю кнопку мыши'),('mousewheel).','(нажмите на колёсико).'),('You may also change your Minecraft','Также в настройках управления Minecraft'),('control for "Drop" to left-click.','можно назначить выбрасывание предмета на ЛКМ.'),('Finally, try using a larger GUI scale,','Ещё попробуйте увеличить масштаб интерфейса —'),('it helps a lot!','это очень помогает!'),
 ('Use the Harp furniture to complete','Поставьте арфу, чтобы играть'),('songs from the comfort of your','мелодии, не покидая свой'),('island!','остров!'),('Cost: 600,000 coins','Цена: 600,000 монет'),
]

def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root;client=Download(root)
 # Every endpoint is present in the official API documentation. News/challenges
 # add independent public text,not an imaginary NPC/script endpoint.
 endpoints=RESOURCE_ENDPOINTS+['resources/challenges','skyblock/news'];api_records=[];api_texts=[]
 for endpoint in endpoints:
  p=root/'official-api'/(endpoint.replace('/','-')+'.json');url='https://api.hypixel.net/v2/'+endpoint
  value=json.loads(client.get(url,p));assert value.get('success') is True
  old=PUBLIC/'hypixel-resources'/(endpoint.replace('/','-')+'.json')
  for path,s in json_strings(value):
   if visible_field(path) and re.search('[A-Za-z]',s):api_texts.append({'source':s,'json_path':list(path),'source_url':url,'retrieved_receipt':str(p.relative_to(root))+'.receipt.json','not_private_server_script':True})
  api_records.append({'endpoint':endpoint,'success':True,'sha256':digest(p.read_bytes()),'same_as116_saved_body':old.exists() and digest(old.read_bytes())==digest(p.read_bytes()),'same_parsed_JSON_as116':old.exists() and json.loads(old.read_text(encoding='utf-8'))==value,'prior_capture_exists':old.exists()})
 write(root/'PASS1_OFFICIAL_API.json',api_records);jsonl(root/'PASS1_OFFICIAL_API_TEXTS.jsonl',api_texts)
 client.get('https://api.hypixel.net/',root/'official-api'/'documentation.html')
 client.get('https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/',root/'official-wiki-closure.html')
 # Distinguish raw speaker occurrences outside Dialogue from those already visited.
 outside=[];npc_pages=set();npc_dialogue_pages=set();npc_types=Counter();raw_speakers=Counter();parser_seen=0
 for page in rows(PUBLIC/'source_pages.jsonl'):
  text=page['raw_wikitext'];ranges=[]
  for start,end,name,raw in templates(text):
   name=name.strip().casefold()
   if name=='dialogue':ranges.append((start,end));npc_dialogue_pages.add(page['title'])
   if name in ('npc','npc infobox','npcinfo','npc info','npc template','infobox npc','infobox/character'):npc_pages.add(page['title']);npc_types[name]+=1
  # Binary search would optimise this offline-only check,but max few ranges/page.
  for hit in __import__('audit_five_pass117').raw_tagged_lines(text):
   if not any(start<=hit['offset']<end for start,end in ranges):outside.append(hit|{'page':page['title'],'source_url':page['source_url'],'revision':page['revision'],'status':'outside_Dialogue_raw_markup_requires_context_review'})
   else:parser_seen+=1
 jsonl(root/'PASS2_TAGS_OUTSIDE_DIALOGUE.jsonl',outside)
 write(root/'PASS2_BOUNDARY_COUNTS.json',{'raw_tag_occurrences_inside_Dialogue':parser_seen,'raw_tag_occurrences_outside_Dialogue':len(outside),'pages_outside_Dialogue':len({r['page'] for r in outside}),'character_infobox_pages_not_all_interactable_NPCs':len(npc_pages),'pages_with_Dialogue':len(npc_dialogue_pages),'character_infobox_without_Dialogue':sorted(npc_pages-npc_dialogue_pages),'infobox_spellings_seen':dict(npc_types),'not_every_character_must_have_dialogue':True})
 # Catalogue universe comparison,without equating variants with official API IDs.
 repo=root/'repositories/NotEnoughUpdates--NotEnoughUpdates-REPO';commit=json.loads((repo/'commit.json').read_text())['sha'];archive=repo/(commit+'.zip');items={};special={}
 with zipfile.ZipFile(archive) as z:
  for member in z.namelist():
   name='/'.join(member.split('/')[1:])
   if name.startswith('items/') and name.endswith('.json'):
    value=json.loads(z.read(member));items[name[6:-5]]={'name':value.get('displayname'),'internalname':value.get('internalname'),'lore_lines':len(value.get('lore',[])),'json_sha256':digest(z.read(member))}
   if name in ('constants/enchants.json','constants/petnums.json','constants/pets.json','constants/abiphone.json','constants/rift_guide.json'):
    special[name]=json.loads(z.read(member))
 official=json.loads((root/'official-api/resources-skyblock-items.json').read_text());api_items={r['id']:r for r in official['items']}
 jsonl(root/'PASS4_ITEM_MANIFEST.jsonl',({'repo_item_key':k,**v,'official_API_same_ID':k in api_items} for k,v in sorted(items.items())))
 write(root/'PASS4_CATALOGUE_COMPARISON.json',{'official_API_item_IDs':len(api_items),'NEU_item_records_including_variants':len(items),'same_ID_intersection':len(set(items)&set(api_items)),'API_IDs_without_same_NEU_key':sorted(set(api_items)-set(items)),'NEU_variants_without_identical_API_ID_count':len(set(items)-set(api_items)),'no_claim_variants_are_missing_items':True,'retained_special_constants':list(special)})
 write(root/'PASS4_SPECIAL_CONSTANTS.json',special)
 # Unified review queue preserves the prior queue and adds repository/API findings.
 master={r['source']:r for r in rows(PUBLIC/'MASTER_REVIEW_QUEUE.jsonl')};initial=len(master);known=json.loads((root/'EXACT_WORKTREE_REFERENCE.json').read_text(encoding='utf-8'));new_sources=set()
 def add(s,kind,origin):
  if s not in master:master[s]={'source':s,'translation_ru':'','origins':[],'kinds':[],'status':'context_and_runtime_review_not_proven_missing_translation','automatically_added_to_mod':False};new_sources.add(s)
  r=master[s]
  if kind not in r['kinds']:r['kinds'].append(kind)
  r['origins'].append(origin)
 for r in rows(root/'PASS4_REPOSITORY_REVIEW_QUEUE.jsonl'):
  if not r['exact_worktree_records']:add(r['source'],'independent_repository_visible_field',{'repository_origins':r['origins']})
 for r in api_texts:
  if r['source'] not in known:add(r['source'],'official_api_visible_field',r)
 for s,t in MELODY_UI:add(s,'Melody_UI_editorial_review',{'page':'Melody/UI','source_url':'https://hypixelskyblock.minecraft.wiki/w/Melody/UI','translation_draft_ru':t,'not_yet_imported_or_runtime_verified':True})
 jsonl(root/'MASTER_REVIEW_QUEUE.jsonl',(master[s] for s in sorted(master)))
 write(root/'MASTER_REVIEW_DELTA.json',{'previous116_unique_sources':initial,'new_unique_review_sources':len(new_sources),'combined_unique_review_sources':len(master),'no_previous_source_dropped':set(r['source'] for r in rows(PUBLIC/'MASTER_REVIEW_QUEUE.jsonl'))<=master.keys(),'not_missing_game_line_count':True})
 # Finite real-JAR screening. A string that changed is not declared accurate.
 probes={}
 def probe(s,kind,layout=None):
  key=(s,kind)
  if key not in probes:probes[key]={'source':s,'kind':kind,'layouts':[]}
  if layout and COLOR.sub('',layout)==s and layout not in probes[key]['layouts']:probes[key]['layouts'].append(layout)
 for r in rows(root/'PASS4_REPOSITORY_REVIEW_QUEUE.jsonl'):
  if not r['exact_worktree_records']:
   probe(r['source'],'gui')
   for origin in r['origins']:probe(r['source'],'gui',origin['source'])
 for r in rows(PUBLIC/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl'):
  if not r['variable_or_markup_review']:probe(r['source'],'chat')
 for r in rows(PUBLIC/'REPORTED_NPC_PRIORITY_QUEUE.jsonl'):
  if not re.search(r'<[^>]+>|\{[^}]+\}',r['source']):probe(r['source'],'chat')
 for s,t in MELODY_UI:probe(s,'gui')
 for s in ('Collect Spruce Logs','Collect Spruce Logs (100/128)','Find the Campfire','Collect Dark Oak Logs','Collect Dark Oak Logs (230/256)','Travel to the Savanna Woodland','You claimed a Campfire Initiate Badge I!'):
  probe(s,'gui');probe('Objective: '+s,'gui');probe(s,'chat')
 jsonl(root/'PROBE_INPUT117.jsonl',sorted(probes.values(),key=lambda r:(r['kind'],r['source'])))
 write(root/'PROBE_SCOPE.json',{'inputs':len(probes),'layout_variants':sum(len(r['layouts']) for r in probes.values()),'not_an_FPS_benchmark':True,'all_live_layouts_tested':False})
 print(json.dumps({'api_endpoints':len(api_records),'outside_Dialogue':len(outside),'catalogue_items':len(items),'master':len(master),'new_sources':len(new_sources),'runtime_probe':len(probes)},ensure_ascii=False))

if __name__=='__main__':main()
