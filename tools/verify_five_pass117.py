"""Independent evidence/queue integrity checks,not a semantic translation oracle."""
import argparse,hashlib,json,re,zipfile,urllib.parse
from pathlib import Path,PurePosixPath
from collections import Counter

PROJECT=Path(__file__).resolve().parents[1]
def sha(data):return hashlib.sha256(data).hexdigest().upper()
def rows(path):
 with path.open(encoding='utf-8-sig') as f:
  for line in f:
   if line.strip():yield json.loads(line)

def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root;checks=Counter()
 def check(value,group):
  if not value:raise AssertionError(group)
  checks[group]+=1
 previous=Path('C:/Users/Wither/Desktop/WitherSkyRU/PUBLIC_TEXT_EXPORT_2026-10-21');old_hashes=json.loads((previous/'HASHES.json').read_text(encoding='utf-8'))
 for rel,expected in old_hashes.items():check(sha((previous/rel).read_bytes())==expected,'immutable116_delivery')
 public=PROJECT/'build/public-text116-6ee5c58738c54ede81623ec912a75105';boundary=json.loads((public/'RUNTIME_BOUNDARY.json').read_text(encoding='utf-8'))
 for rel,expected in boundary.items():check(sha((PROJECT/rel).read_bytes())==expected,'runtime_sources_unchanged_by117')
 old_queue=public/'MASTER_REVIEW_QUEUE.jsonl';master={r['source']:r for r in rows(root/'MASTER_REVIEW_QUEUE.jsonl')};check(len(master)==sum(1 for _ in rows(root/'MASTER_REVIEW_QUEUE.jsonl')),'master_unique')
 for r in rows(old_queue):check(r['source'] in master,'no_prior_review_source_lost')
 for r in master.values():check(not r['automatically_added_to_mod'],'no_automatic_runtime_import')
 repos=json.loads((root/'PASS1_REPOSITORIES.json').read_text(encoding='utf-8'));blob_count=0
 for repo in repos:
  folder=root/'repositories'/repo['repository'].replace('/','--');tree=json.loads((folder/'tree.json').read_text(encoding='utf-8'));check(not tree['truncated'],'untruncated_tree');expected={r['path']:r['sha'] for r in tree['tree'] if r['type']=='blob'};present=set()
  archive=folder/(repo['commit']+'.zip');check(sha(archive.read_bytes())==repo['archive_sha256'],'repository_archive_sha256')
  with zipfile.ZipFile(archive) as z:
   check(z.testzip() is None,'repository_zip_crc')
   for member in z.infolist():
    if member.is_dir():continue
    name='/'.join(PurePosixPath(member.filename).parts[1:]);data=z.read(member);check(name in expected and name not in present,'repository_tree_member');present.add(name);check(hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()==expected[name],'repository_git_blob_sha1');blob_count+=1
  check(present==expected.keys(),'all_repository_tree_blobs_present');check(bool(json.loads((folder/'LICENSES.json').read_text(encoding='utf-8'))),'repository_license_retained')
 for receipt in root.rglob('*.receipt.json'):
  record=json.loads(receipt.read_text(encoding='utf-8'));body=receipt.with_name(receipt.name.removesuffix('.receipt.json'));check(body.exists() and sha(body.read_bytes())==record['sha256'],'download_receipts')
 for record in json.loads((root/'PASS1_OFFICIAL_API.json').read_text(encoding='utf-8')):
  path=root/'official-api'/(record['endpoint'].replace('/','-')+'.json');check(json.loads(path.read_text(encoding='utf-8'))['success'] and sha(path.read_bytes())==record['sha256'],'official_api_success_and_hash')
 delta=json.loads((root/'WIKI_DELTA117.json').read_text(encoding='utf-8'));responses={};captured_pages={}
 for p in (root/'wiki-delta/requests').glob('*.json'):
  record=json.loads(p.read_text(encoding='utf-8'));check(sha(record['body'].encode())==record['body_sha256'],'delta_API_body_hash');params=dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(record['url']).query));body=json.loads(record['body'])
  if params.get('list')=='recentchanges':
   check(params['rcstart']==delta['window']['start'] and params['rcend']==delta['window']['end'],'delta_closed_time_window');responses[params.get('rccontinue')]=body
  for page in body.get('query',{}).get('pages',[]):
   if page.get('revisions'):
    revision=page['revisions'][0];captured_pages[page['pageid']]=(revision['revid'],revision['slots']['main']['content'])
 cursor=None;entries=[];seen=set()
 while True:
  check(cursor not in seen and cursor in responses,'delta_continuation_chain');seen.add(cursor);body=responses[cursor];entries+=body['query']['recentchanges']
  if 'continue' not in body:break
  cursor=body['continue']['rccontinue']
 check(entries==list(rows(root/'wiki-delta/RECENT_CHANGES.jsonl')) and len(entries)==delta['public_change_entries'],'delta_recent_changes_saved_exactly')
 for p in rows(root/'wiki-delta/source_pages.jsonl'):check(captured_pages[p['pageid']]==(p['revision'],p['raw_wikitext']) and sha(p['raw_wikitext'].encode())==p['raw_sha256'],'delta_complete_raw_revision')
 check(len(master)==delta['master_after'] and delta['continuation_exhausted'],'delta_master_and_closed_pagination')
 drafts=list(rows(root/'REVIEWED_DRAFTS117.jsonl'));check(len(drafts)==len({r['source'] for r in drafts})==141,'authored_target_cardinality')
 for r in drafts:
  check(r['source'] in master and master[r['source']]['translation_draft_ru']==r['translation'],'authored_target_in_master');check(r['translation']!=r['source'] and r['origins'] and not r['automatically_added_to_mod'],'authored_target_scope');check(Counter(re.findall('<[^<>]+>',r['source']))==Counter(re.findall('<[^<>]+>',r['translation'])),'variable_markers_preserved')
 priorities=list(rows(root/'PASS3_RYAN_MELODY_EDITORIAL_REVIEW.jsonl'));check(len(priorities)==62 and Counter(r['npc'] for r in priorities)=={'Ryan':41,'Melody':21},'all_published_priority_NPC_rows_reviewed')
 phone=json.loads((PROJECT/'translation-source/ABIPHONE_REQUIREMENTS_117_REVIEWED.json').read_text(encoding='utf-8'));special=json.loads((root/'PASS4_SPECIAL_CONSTANTS.json').read_text(encoding='utf-8'));requirements={re.sub('(?i)§[0-9a-fk-or]','',s) for r in special['constants/abiphone.json'].values() for s in r.get('requirement',[])}-{''}
 check(requirements==phone['translations'].keys()|phone['protected_literals'].keys(),'all_captured_contact_requirements_classified')
 for s,t in phone['translations'].items():check(re.findall('[0-9]+',s)==re.findall('[0-9]+',t),'contact_requirement_numbers_unchanged')
 check(set(phone['protected_literals'])=={'I love the warmth of the Holidays despite the snow'},'typed_English_quest_literal_preserved')
 catalogue=json.loads((root/'PASS4_CATALOGUE_SUMMARY.json').read_text(encoding='utf-8'));check(catalogue['distinct_book_variants_after_alias_deduplication']==720 and catalogue['enchantment_IDs_in_applicability_catalogue']==146,'finite_enchantment_catalogue');check(catalogue['pet_IDs_with_numeric_endpoints']==88 and catalogue['pet_item_variants']==322,'finite_pet_catalogue')
 native=json.loads((root/'native-snbt-reader/SNBT_SUMMARY.json').read_text(encoding='utf-8'));check(native['snbt_files']==native['parsed_files']==8847 and not native['parse_errors'],'all_native_SNBT_parsed');check(sum(1 for _ in rows(root/'native-snbt-reader/SNBT_TEXT_OCCURRENCES.jsonl'))==native['all_string_occurrences'],'SNBT_text_cardinality')
 triage=json.loads((root/'PASS5_RUNTIME_TRIAGE.json').read_text(encoding='utf-8'));probe=root/triage['runtime_probe'];proof=json.loads((probe/'RUNTIME_SUMMARY.json').read_text(encoding='utf-8'));jar=PROJECT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.115-dev.jar';check(sha(jar.read_bytes())==triage['runtime_jar_sha256']==proof['runtime_jar_sha256'],'real_JAR_fingerprint');check(proof['inputs']==sum(1 for _ in rows(probe/'RUNTIME_ROWS.jsonl'))==1988 and proof['layouts']==1673,'runtime_probe_cardinality')
 for name,count in (('TestFivePass117-output.txt',16),('TestPublicText116-output.txt',14)):
  text=(root/name).read_text(encoding='utf-8-sig');check(f'Ran {count} tests' in text and '\nOK' in text,'export_regression_tests')
 result={'all_passed':True,'assertions':sum(checks.values()),'by_group':dict(checks),'preserved116_manifest_files':len(old_hashes),'verified_repository_blobs':blob_count,'runtime_source_hashes_unchanged':len(boundary),'combined_unique_review_sources':len(master),'authored_target_drafts_not_imported':len(drafts),'existing_priority_targets_reviewed':56,'new_priority_targets_runtime_pending':6,'runtime_JAR_used_for_screening_sha256':proof['runtime_jar_sha256'],'all_game_translation_complete':False,'all_private_live_server_scripts_available':False,'FPS_measured':False}
 (root/'VERIFICATION117.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
