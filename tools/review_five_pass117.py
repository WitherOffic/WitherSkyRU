"""Source-bound editorial drafts and explicit public-documentation gaps."""
import argparse,json,re
from pathlib import Path
from collections import Counter,defaultdict
from audit_five_pass117 import ROOT,PUBLIC,COLOR,rows,jsonl,write,digest
from enrich_five_pass117 import MELODY_UI
from export_public_text116 import provisional_text

def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root
 author=json.loads((ROOT/'translation-source/FIVE_PASS_117_REVIEWED.json').read_text(encoding='utf-8'));phone=json.loads((ROOT/'translation-source/ABIPHONE_REQUIREMENTS_117_REVIEWED.json').read_text(encoding='utf-8'));known=json.loads((root/'EXACT_WORKTREE_REFERENCE.json').read_text(encoding='utf-8'))
 missing={r['source']:r for r in rows(PUBLIC/'NPC_DIALOGUE_REVIEW_QUEUE.jsonl')};priority={r['source']:r for r in rows(PUBLIC/'REPORTED_NPC_PRIORITY_QUEUE.jsonl')};observed=json.loads((PUBLIC/'OBSERVED_MISSING.json').read_text(encoding='utf-8'));pages={};gaps=[]
 for p in rows(PUBLIC/'source_pages.jsonl'):
  text=p['raw_wikitext'];alpha=bool(re.search(r'\{\{\s*(?:AHN|Alpha)\s*[|}]',text,re.I))
  if p['title'] in {origin['page'] for r in missing.values() for origin in r['origins']}|{'Melody/UI','Ryan','Melody'}:pages[p['title']]=p
  section='';offset=0
  for number,line in enumerate(text.splitlines(keepends=True),1):
   heading=re.match(r'^\s*(={2,6})\s*(.*?)\s*\1\s*$',line)
   if heading:section=heading[2]
   if re.search(r'\{\{\s*(?:InfoNeeded|Info Needed|DialogueNeeded|Dialogue Needed)\b',line,re.I) and re.search(r'dialog|conversation|abiphone',section,re.I):
    gaps.append({'page':p['title'],'source_url':p['source_url'],'revision':p['revision'],'section':section,'line':number,'raw_line':line.rstrip('\r\n'),'alpha_marker':alpha,'status':'public_source_itself_requests_missing_information_not_recoverable_by_repeating_same_API'})
   offset+=len(line)
 jsonl(root/'PASS3_PUBLIC_DOCUMENTATION_GAPS.jsonl',gaps)
 drafts=[]
 for r in author['records']:
  s=r['source'];r=dict(r)
  if s in missing or s in priority:
   refs=missing[s]['origins'] if s in missing else [priority[s]];r['origins']=refs
   page=next((pages.get(ref.get('page',ref.get('npc'))) for ref in refs if pages.get(ref.get('page',ref.get('npc')))),None)
   r['source_page_alpha_marker']=bool(page and re.search(r'\{\{\s*(?:AHN|Alpha)\s*[|}]',page['raw_wikitext'],re.I))
   r['source_page_raw_sha256']=page['raw_sha256'] if page else None
  else:
   evidence=[o for o in observed['observations'] if o.get('source')==s];assert evidence,(s,'No observed or published source');r['origins']=evidence
  assert Counter(re.findall(r'<[^<>]+>',s))==Counter(re.findall(r'<[^<>]+>',r['translation'])),s
  r['status']='editorially_authored_runtime_import_and_style_acceptance_pending';r['automatically_added_to_mod']=False;drafts.append(r)
 ui=next(p for p in rows(PUBLIC/'source_pages.jsonl') if p['title']=='Melody/UI');plain_ui=COLOR.sub('',provisional_text(ui['raw_wikitext'])).replace('\\,',',')
 for source,target in MELODY_UI:
  assert source in plain_ui,(source,'UI text must be present in saved full source')
  drafts.append({'source':source,'translation':target,'kind':'Melody_UI','origins':[{'source_url':ui['source_url'],'revision':ui['revision'],'raw_sha256':ui['raw_sha256']}],'status':'editorially_authored_runtime_import_and_style_acceptance_pending','automatically_added_to_mod':False})
 special=json.loads((root/'PASS4_SPECIAL_CONSTANTS.json').read_text(encoding='utf-8'));contacts=special['constants/abiphone.json'];requirements=defaultdict(list)
 for name,record in contacts.items():
  for value in record.get('requirement',[]):
   source=COLOR.sub('',value)
   if source:requirements[source].append({'contact':name,'legacy':value,'source_file':'constants/abiphone.json'})
 assert set(requirements)==set(phone['translations'])|phone['protected_literals'].keys()
 for source,target in phone['translations'].items():
  assert re.findall(r'[0-9]+',source)==re.findall(r'[0-9]+',target),(source,target)
  drafts.append({'source':source,'translation':target,'kind':'Abiphone_contact_guide_requirement_not_all_server_dialogues','origins':requirements[source],'status':'editorially_authored_runtime_import_and_style_acceptance_pending','automatically_added_to_mod':False})
 protected=[{'source':s,'reason':why,'origins':requirements[s],'status':'intentional_exact_phrase_to_type_English_preserved_not_untranslated','text_was_not_sent':True} for s,why in phone['protected_literals'].items()]
 jsonl(root/'REVIEWED_DRAFTS117.jsonl',drafts);jsonl(root/'PROTECTED_QUEST_LITERALS117.jsonl',protected)
 # Explicit editorial review of every currently published Ryan/Melody message.
 new={r['source']:r for r in drafts};review=[]
 for source,r in sorted(priority.items()):
  targets=known.get(source,[])
  if targets:
   assert len({t['translation'] for t in targets})==1
   target=targets[-1]['translation'];status='existing_target_semantically_reviewed_live_styles_and_reachability_separate'
  else:
   assert source in new;target=new[source]['translation'];status='new_authored_target_runtime_shape_pending'
  review.append({'source':source,'translation_ru':target,'npc':r['npc'],'source_url':r['source_url'],'status':status,'not_an_all_live_branch_guarantee':True})
 jsonl(root/'PASS3_RYAN_MELODY_EDITORIAL_REVIEW.jsonl',review)
 master={r['source']:r for r in rows(root/'MASTER_REVIEW_QUEUE.jsonl')};before=len(master)
 for r in drafts:
  s=r['source']
  if s not in master:master[s]={'source':s,'translation_ru':'','origins':[],'kinds':[],'automatically_added_to_mod':False,'status':'review_required'}
  entry=master[s];entry['translation_draft_ru']=r['translation'];entry['draft_status']=r['status'];entry['origins'].append({'editorial_draft_file':'REVIEWED_DRAFTS117.jsonl','kind':r['kind']})
  if 'manually_authored_draft_not_runtime_imported' not in entry['kinds']:entry['kinds'].append('manually_authored_draft_not_runtime_imported')
 jsonl(root/'MASTER_REVIEW_QUEUE.jsonl',(master[s] for s in sorted(master)))
 summary={'authored_unique_targets':len({r['source'] for r in drafts}),'authored_records':len(drafts),'by_kind':dict(Counter(r['kind'] for r in drafts)),'published_Ryan_Melody_messages_editorially_reviewed':len(review),'existing_targets_reviewed':sum(r['status'].startswith('existing') for r in review),'new_priority_targets_pending_runtime':sum(r['status'].startswith('new') for r in review),'Abiphone_contacts_in_independent_catalogue':len(contacts),'Abiphone_unique_nonempty_requirement_lines':len(requirements),'Abiphone_requirement_targets_authored':len(phone['translations']),'protected_English_quest_input_literals':len(protected),'public_Dialogue_information_needed_markers':len(gaps),'public_pages_with_dialogue_gaps':len({r['page'] for r in gaps}),'master_before_editorial':before,'master_after_editorial':len(master),'all_drafts_imported_into_mod':False,'all_live_dialogue_known':False}
 write(root/'EDITORIAL_REVIEW117.json',summary);print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':main()
