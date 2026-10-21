"""Publish only validated final-JAR evidence. No writes to the installed game or old deliveries."""
import argparse,json,re,html,shutil,hashlib,zipfile
from collections import defaultdict
from pathlib import Path
from prepare_npc99 import ROOT,SOURCE,DATA,CORPUS,SPEAKERS,choice_groups
from prepare_npc78 import rows,sha,write
from report_npc99 import report
BASE='76E85EF50B3308A390D5C11302198A16EAFD1C7CA2BE08F4AE46530C16718A50'
DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
PREVIOUS=DESKTOP/'NPC_TRANSLATION_BATCH_2026-10-19_1'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
 p=argparse.ArgumentParser()
 for k in ('checks','audit','dispatch','benchmark','focused','destination'):p.add_argument('--'+k,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert destination.parent==DESKTOP and not destination.exists()
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.99-dev.jar';fingerprint=sha(jar)
 assert sha(ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.98-dev.jar')==BASE
 for n,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/n)==h,('Immutable98 delivery changed',n)
 scope=load(a.checks/'CHECK_SCOPE.json');old=load(PREVIOUS/'checks/CHECK_SCOPE.json');tests=scope['tests_passed']
 assert len(tests)==len(set(tests))==167 and set(tests)-set(old['tests_passed'])=={'TestNpc99','TestNpcChoices99'} and set(old['tests_passed'])<set(tests)
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes']
 for name in tests:assert (a.checks/(name+'-output.txt')).is_file()
 parser_log=(a.checks/'TestNpcCorpusParser99-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 15 tests' in parser_log and '\nOK' in parser_log
 assertion=lambda name,label:int(re.search('PASS: '+label+r' (\d+) assertions',(a.checks/(name+'-output.txt')).read_text())[1])
 assertions=assertion('TestNpc99','NPC99');choices_assertions=assertion('TestNpcChoices99','NPCChoices99')
 proof=load(a.audit/'RESULT.json');assert proof['runtime_jar_sha256']==fingerprint and proof['baseline_jar_sha256']==BASE and proof['new_exact_outputs_flat_and_published']==397 and proof['old_output_rows_unchanged']==10850 and len(proof['bytecode'])==5
 bench=load(a.benchmark/'RAW.json');focused=load(a.focused/'RAW.json');assert bench['runtime_jar_sha256']==focused['runtime_jar_sha256']==fingerprint
 assert len(bench['families']['receipt']['summary'])==35 and sum(r['equal_useful_output'] for r in bench['families']['receipt']['summary'])==33
 for family in list(bench['families'].values())+[focused]:
  assert [r['version'] for r in family['records']]==['0.15.98-dev','0.15.99-dev','0.15.99-dev','0.15.98-dev']
  for r in family['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.98-dev' else fingerprint)
 assert len(focused['summary'])==6 and all(r['equal_useful_output'] for r in focused['summary'])
 new={r['source']:r['translation'] for r in load(DATA/'npc-expansion99.json')['records']};assert len(new)==397
 assert sum(': ✆ ' in s for s in new)==38
 authored={}
 for line in (SOURCE/'NPC_0.15.99_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if line and not line.startswith('#'):f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new
 known=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].add(r['translation'])
 q=rows(CORPUS/'translation_queue.jsonl');assert len(q)==16244 and sum(r['source'] in known for r in q)==10009 and sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 evidence=load(SOURCE/'NPC_SOURCES_0.15.99.json');assert [len(evidence[k]) for k in ('records','blocks','source_pages','pending_variables','neutral_unchanged','retained_originals','unsafe_excluded')]==[438,236,38,2,1,27,11]
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==398
 choices=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.99.json');assert len(choices['records'])==11 and sum(len(r['source_labels']) for r in choices['records'])==11 and sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choices['records'])==12
 expected_choices=[list(map(json.loads,l.split('|'))) for l in (SOURCE/'NPC_CHOICES_0.15.99_REVIEWED.tsv').read_text().splitlines() if l and not l.startswith('#')];assert expected_choices==[[r['source_labels'],r['target_labels']] for r in choices['records']]
 current={r['source']:r for r in rows(a.dispatch/'0.15.99-dev/DISPATCH_ROWS.jsonl')};assert len(current)==11247
 caveats=[];blocks={b['block_id']:b for b in evidence['blocks']}
 pamela_alpha={"[NPC] Pesthunter Pamela: Hi! I want to reforge my SkyMart Hyper Vacuum to Beady. Would you happen to have a Beady Eyes?","[NPC] Pesthunter Pamela: Hi! I want to reforge my Peony Necklace to Rooted. Would you happen to have a Burrowing Spores?","[NPC] Pesthunter Pamela: I guess I'll just have to find one myself then..."}
 assert pamela_alpha<=new.keys()
 pamela_roots=[b for b in evidence['blocks'] if b['page']=='Pesthunter Pamela' and '{{ahn|' in b['condition_context_raw'].lower()];assert len(pamela_roots)==1
 for r in evidence['records']:
  if not r['runtime_registration']:continue
  for o in r['all_occurrences']:
   b=blocks[o['block_id']];flags=[]
   if r['published_row']['speaker']=='Centauri':flags.append('test_server_and_archived_update_announcements_not_main_server_coverage')
   if '{{ahn|' in b['condition_context_raw'].lower() or r['source'] in pamela_alpha:flags.append('published_alpha_condition_not_live_certified')
   if '{{More Info Needed}}' in b['condition_context_raw']:flags.append('published_condition_needs_more_information')
   if flags:caveats.append({'source':r['source'],'block_id':b['block_id'],'occurrence_id':o['occurrence_id'],'condition_context_raw':b['condition_context_raw'],'alpha_header_evidence_block_id':pamela_roots[0]['block_id'] if r['source'] in pamela_alpha else None,'alpha_header_inheritance':'Same published Garden visitor table,rowspan=4;the later None/Denying cells inherit its alpha header.' if r['source'] in pamela_alpha else None,'flags':flags})
 assert len({r['source'] for r in caveats if r['source'].startswith('[NPC] Centauri: ')})==36
 assert len({r['source'] for r in caveats if 'published_alpha_condition_not_live_certified' in r['flags']})==3
 write(SOURCE/'NPC_CONTEXT_CAVEATS_0.15.99.json',{'scope':'Public contextual restrictions,not exclusion of all archived/test lines from exact-key ledger.','records':caveats})
 previous=rows(SOURCE/'NPC_WORKLIST_0.15.98.jsonl');assert [r['source'] for r in previous]==[r['source'] for r in q];by_speaker=defaultdict(list);pending={r['source'] for r in evidence['pending_variables']};excluded={r['source'] for r in evidence['unsafe_excluded']};flagged={r['source'] for r in caveats}
 for r in previous:
  s=r['source'];r['exact_registration']=s in known;r['registered_targets_not_automatically_certified']=sorted(known.get(s,()))
  if s in new:r['review_status']='authored_and_fixture_checked99'
  if s in pending:r['review_status']='structured_dynamic_review_pending99'
  if s in excluded:r['review_status']='removed_dialogue_not_newly_registered99'
  if s in flagged:r['published_context_caveat99']=True
  if s in current:r.update(runtime_flat_snapshot_not_quality_proof=current[s]['flat_result'],runtime_colored_snapshot_not_quality_proof=current[s]['published_style_result'])
  by_speaker[r['speaker'] or '(без говорящего)'].append(r)
 (SOURCE/'NPC_WORKLIST_0.15.99.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in previous),encoding='utf-8')
 table=['# Все говорящие — 0.15.99','','Точные ключи публичной очереди, не процент готовности всей игры. Тестовые/исторические условия, имена и теги требуют отдельного учёта.','','| Говорящий | Строк | Точных ключей | Доля | Без ключа | Новых в 99 |','| --- | ---: | ---: | ---: | ---: | ---: |']
 for name,rs in sorted(by_speaker.items()):
  count=sum(r['exact_registration'] for r in rs);table.append(f'| {html.escape(name)} | {len(rs)} | {count} | {100*count/len(rs):.2f}% | {len(rs)-count} | {sum(r["source"] in new for r in rs)} |')
 (SOURCE/'NPC_ALL_SPEAKERS_0.15.99_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
 stats=load(SOURCE/'NPC_PROGRESS_0.15.99.json');stats.update(runtime_validation='167_fresh_groups_and15_parser_tests_passed',fresh_test_groups=167,parser_tests_passed=15,new_fixture_assertions=assertions,new_choice_fixture_assertions=choices_assertions,runtime_jar_sha256=fingerprint,baseline_jar_sha256=BASE,dispatch_tested_rows=11247,dispatch_new_exact_results=397,unchanged_other_dispatch_rows=10850,remaining_previous_style_gaps=72,receipt_render_code_unchanged=True,live_fps_measured=False,source_review_notes='NPC_0.15.99_REVIEW_NOTES.md')
 write(SOURCE/'NPC_PROGRESS_0.15.99.json',stats);(SOURCE/'NPC_PROGRESS_0.15.99_RU.md').write_text(report(stats,assertions,choices_assertions,bench,focused,fingerprint),encoding='utf-8')
 destination.mkdir()
 files=list(SOURCE.glob('*0.15.99*'))+[ROOT/'build.gradle',ROOT/'src/main/resources/fabric.mod.json',ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',ROOT/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java',DATA/'npc-expansion99.json',ROOT/'src/main/resources/META-INF/licenses/NPC99-SOURCES.txt']
 files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==9439' in p.read_text(encoding='utf-8-sig')]
 files += [ROOT/'tests'/n for n in ('TestNpc99.java','TestNpcChoices99.java','BenchmarkNpc99.java','BenchmarkNpc99Focused.java','BenchmarkPrepared72.java','BenchmarkDynamicHud96.java','AuditNpcCorpus77.java','test_npc_corpus77.py')]
 files += [ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc88.py','prepare_npc92.py','prepare_npc93.py','prepare_npc99.py','review_npc92_revision.py','review_npc99_revision.py','export_npc_corpus77.py','derive_npc99_checks.py','derive_npc99_benchmark.py','check_npc99.ps1','dispatch_npc99.ps1','audit_npc99.py','benchmark_npc99.ps1','benchmark_npc99_focused.ps1','report_npc99.py','finalize_npc99.py')]
 for source in sorted(set(files)):
  target=destination/source.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
 shutil.copy2(jar,destination/jar.name);checks=destination/'checks';checks.mkdir()
 for name in tests:
  for suffix in ('-output.txt','-stderr.txt'):
   source=a.checks/(name+suffix)
   if source.exists() and source.stat().st_size:shutil.copy2(source,checks)
 for name in ('CHECK_SCOPE.json','TestNpcCorpusParser99-output.txt','compile-arguments.txt'):shutil.copy2(a.checks/name,checks)
 shutil.copytree(a.audit,destination/'bytecode')
 for label,path in [('general',a.benchmark),('focused',a.focused)]:
  target=destination/'benchmark'/label;target.mkdir(parents=True)
  for source in path.rglob('*'):
   if source.is_file() and 'isolated-' not in str(source.relative_to(path)):
    dest=target/source.relative_to(path);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
 for version in ('0.15.98-dev','0.15.99-dev'):
  target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(a.dispatch/version/'DISPATCH_SUMMARY.json',target)
 (destination/'README_RU.md').write_text('# Большой проход NPC — 0.15.99\n\n397 новых реплик / 40 обозначений говорящего, 38 телефонных реплик Абифона. 11 групп ответов / 11 подписей / 12 оформлений.\n\n10 009 / 16 244 · 61,62% точных ключей публичной очереди, не всей игры. Включены тестовые/архивные реплики Centauri; ограничения отдельно отмечены.\n\n[Отчёт и все замеры](translation-source/NPC_PROGRESS_0.15.99_RU.md) · [Контекстная вычитка](translation-source/NPC_0.15.99_REVIEW_NOTES.md) · [Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.99_RU.md).\n\n167 групп + 15 проверок извлечения. Частичный пакет исходников и доказательств, не весь проект. JAR не установлен автоматически.\n',encoding='utf-8')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_0.15.99.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for name in hashes:z.write(destination/name,name)
  z.write(destination/'HASHES.json','HASHES.json')
 with zipfile.ZipFile(archive) as z:
  assert z.testzip() is None and set(z.namelist())==set(hashes)|{'HASHES.json'}
  for name,h in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==h
 delivered=DESKTOP/jar.name
 if delivered.exists():assert sha(delivered)==fingerprint
 else:shutil.copy2(jar,delivered)
 assert sha(jar)==sha(delivered)==fingerprint
 write(destination/'DELIVERY.json',{'jar':str(delivered),'jar_sha256':fingerprint,'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(hashes),'archive_entries':len(hashes)+1,'installed_game_modified':False});print(json.dumps(load(destination/'DELIVERY.json'),indent=2));print('Progress10009/16244=61.62%;remaining6235')
if __name__=='__main__':main()
