"""Compile four independently authored current-API additions, outside the frozen ledger."""
import json,re,urllib.parse
from collections import Counter
from prepare_npc102 import ROOT,SOURCE,DATA,CORPUS
from prepare_npc78 import rows,write,sha,anchors,validate
from export_npc_corpus77 import templates
delta=json.loads((SOURCE/'NPC_0.15.102_REVISION_DELTA.json').read_text(encoding='utf-8'))
records=[];blocks=[];evidence=[];pages=[];newqueue=[];retained=[]
known={}
for path in DATA.glob('*.json'):
 if path.name in {'npc-current102.json','npc-expansion102.json'}:continue
 for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
  if r.get('source') and r.get('translation'):known.setdefault(r['source'],[]).append(r)
reviewed={}
for line in (SOURCE/'NPC_CURRENT_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
 if line and not line.startswith('#'):
  f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in reviewed;reviewed[s]=(f,f'[NPC] {f[0]}: {f[2]}')
assert len(reviewed)==3
existing="[NPC] Pesthunter Phillip: Unfortunate. The  Pests aren't gonna hunt themselves!"
assert existing in known
for page in delta['full_revisions']['query']['pages']:
 title=page['title'];pair=next((r for r in delta['records'] if r['page']==title),None)
 if pair is None or not pair['added_templates']:continue
 rev=next(r for r in page['revisions'] if r['revid']==pair['current_revision']);content=rev['slots']['main']['content'];used=False
 for at,end,name,raw in templates(content):
  if name.lower()!='dialogue' or raw not in pair['added_templates']:continue
  for line in raw.splitlines():
   legacy=re.sub(r'&([0-9a-fk-or])',r'§\1',line.strip());plain=re.sub(r'§[0-9a-fk-or]','',legacy)
   if plain not in reviewed and plain!=existing:continue
   used=True;f,target=reviewed[plain] if plain in reviewed else (['Pesthunter Phillip'],None)
   defs=anchors(f[3]) if len(f)==4 else []
   if target is not None:
    assert plain not in known;validate(plain,target,defs)
    assert re.findall('[0-9]+',plain)==re.findall('[0-9]+',target) and not re.search('[A-Za-z]',f[2])
   bid=f'current-r{rev["revid"]}-o{at}';oid=bid+'-l'+str(len(evidence));url='https://hypixelskyblock.minecraft.wiki/w/'+urllib.parse.quote(title.replace(' ','_'))+'?oldid='+str(rev['revid'])
   row={'source':plain,'source_legacy_line':legacy,'speaker':f[0],'tag':'NPC','occurrence_id':oid,'block_id':bid,'page':title,'source_url':url,'revision':rev['revid'],'headings':['Dialogue'],'historical_heading':False,'alpha_page':False,'variable_definition':False,'nonstandard_tag_layout':False,'unresolved_markup':False,'source_kind':'current_public_api_addition','occurrence_ids':[oid]}
   context=content[max(0,at-240):at]
   blocks.append({'block_id':bid,'page':title,'revision':rev['revid'],'headings':['Dialogue'],'condition_context_raw':context,'template_raw':raw,'payload_raw':raw})
   if target is not None:records.append({'source_key':f'npc-current102.{len(records)}','speaker':f[0],'tag':'NPC','source':plain,'translation':target,'style_anchors':defs,'source_url':url,'evidence':'current_public_revision_not_frozen_denominator_not_live_packet'})
   else:retained.append({'source':plain,'original_resources':known[plain]})
   evidence.append({'source':plain,'published_row':row,'runtime_registration':target is not None,'all_occurrences':[row]});newqueue.append(row)
 if used:pages.append({'title':title,'revision':rev['revid'],'source_url':url,'raw_wikitext':content})
assert {r['source'] for r in records}==reviewed.keys() and len(blocks)==4 and len(pages)==3 and len(retained)==1
write(DATA/'npc-current102.json',{'format_version':1,'language':'ru','scope':'Three authored current API additions; one already translated line retained separately. Outside frozen16244-row denominator. Jeff/Phillip have alpha/update caveats.','records':records})
write(SOURCE/'NPC_CURRENT_SOURCES_0.15.102.json',{'queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'records':evidence,'blocks':blocks,'source_pages':pages,'pending_variables':[],'extraction_pending':[],'neutral_unchanged':[],'retained_originals':retained,'unsafe_excluded':[],'runtime_retained':[],'raw_archive_marker_candidates_not_live_certification':[],'outside_frozen_ledger':True})
write(SOURCE/'NPC_CURRENT_COUNTS_0.15.102.json',{'rows':3,'retained_rows':1,'by_speaker':dict(Counter(r['speaker'] for r in records)),'frozen_progress_increment':0,'current_resource_sha256':sha(DATA/'npc-current102.json')})
# Separate offline screening input; the frozen source directory stays untouched.
folder=ROOT/'build/npc102-screening-input';folder.mkdir(exist_ok=True)
(folder/'translation_queue.jsonl').write_text((CORPUS/'translation_queue.jsonl').read_text(encoding='utf-8')+''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in newqueue),encoding='utf-8')
assert len(rows(folder/'translation_queue.jsonl'))==16248
license=ROOT/'src/main/resources/META-INF/licenses/NPC102-SOURCES.txt'
base=license.read_text(encoding='utf-8').split('\nCurrent public API additions\n')[0]
license.write_text(base+'\nCurrent public API additions\n'+'\n'.join(f'{r["title"]} | revision{r["revision"]} | {r["source_url"]}' for r in pages)+'\n',encoding='utf-8')
print('Prepared3 new current literals +1 retained/3 pages; separate16248-row screening input; frozen16244-row corpus unchanged.')
