"""Publish final-JAR checked evidence into a new delivery. Never modify the installed game."""
import argparse,json,re,html,shutil,hashlib,zipfile
from collections import defaultdict
from pathlib import Path
from prepare_npc102 import ROOT,SOURCE,DATA,CORPUS,PREVIOUS
from prepare_npc78 import rows,sha,write
from report_npc102 import report
BASE='E0EE8AA7086809A24B001F57F71902C778349215670C983BAA1382AF82606A09'
DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
 p=argparse.ArgumentParser()
 for k in ('checks','audit','dispatch','benchmark','focused','destination'):p.add_argument('--'+k,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert destination.parent==DESKTOP and not destination.exists()
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.102-dev.jar';fingerprint=sha(jar)
 assert sha(ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.101-dev.jar')==BASE
 for name,h in load(PREVIOUS/'HASHES.json').items():assert sha(PREVIOUS/name)==h,('Previous delivery changed',name)
 scope=load(a.checks/'CHECK_SCOPE.json');prior=load(PREVIOUS/'checks/CHECK_SCOPE.json');tests=scope['tests_passed']
 assert len(tests)==len(set(tests))==174 and set(tests)-set(prior['tests_passed'])=={'TestNpc102','TestNpcChoices102','TestNpcCurrent102'} and set(prior['tests_passed'])<set(tests)
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes']
 for name in tests:assert (a.checks/(name+'-output.txt')).is_file()
 parser=(a.checks/'TestNpcCorpusParser102-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 15 tests' in parser and '\nOK' in parser
 assertion=lambda name,label:int(re.search('PASS: '+label+r' (\d+) assertions',(a.checks/(name+'-output.txt')).read_text())[1])
 assertions=assertion('TestNpc102','Npc102');buttons=assertion('TestNpcChoices102','NPCChoices102')
 proof=load(a.audit/'RESULT.json');assert proof['runtime_jar_sha256']==fingerprint and proof['baseline_jar_sha256']==BASE and proof['new_exact_outputs_flat_and_published']==319 and proof['old_output_rows_unchanged']==10933 and len(proof['bytecode'])==5
 bench=load(a.benchmark/'RAW.json');focused=load(a.focused/'RAW.json');assert bench['runtime_jar_sha256']==focused['runtime_jar_sha256']==fingerprint
 assert len(bench['families']['receipt']['summary'])==35 and sum(r['equal_useful_output'] for r in bench['families']['receipt']['summary'])==33
 assert len(bench['families']['hud']['summary'])==10 and len(bench['families']['mixed']['summary'])==5
 assert len(focused['summary'])==6 and all(r['equal_useful_output'] for r in focused['summary'])
 for family in list(bench['families'].values())+[focused]:
  assert [r['version'] for r in family['records']]==['0.15.101-dev','0.15.102-dev','0.15.102-dev','0.15.101-dev']
  for r in family['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.101-dev' else fingerprint)
 new={r['source']:r['translation'] for r in load(DATA/'npc-expansion102.json')['records']};assert len(new)==316 and sum(': ✆ ' in s for s in new)==4
 extra={r['source']:r['translation'] for r in load(DATA/'npc-current102.json')['records']};assert len(extra)==3 and not new.keys()&extra.keys()
 cur=load(SOURCE/'NPC_CURRENT_SOURCES_0.15.102.json');assert len(cur['records'])==4 and len(cur['retained_originals'])==1
 current_assertions=assertion('TestNpcCurrent102','NpcCurrent102')
 authored={}
 for line in (SOURCE/'NPC_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
  if line and not line.startswith('#'):f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new
 current_authored={}
 for line in (SOURCE/'NPC_CURRENT_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
  if line and not line.startswith('#'):f=line.split('|');current_authored[f'[NPC] {f[0]}: {f[1]}']=f'[NPC] {f[0]}: {f[2]}'
 assert current_authored==extra
 known=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].add(r['translation'])
 q=rows(CORPUS/'translation_queue.jsonl');assert len(q)==16244 and sum(r['source'] in known for r in q)==11092 and sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 evidence=load(SOURCE/'NPC_SOURCES_0.15.102.json');assert [len(evidence[k]) for k in ('records','blocks','source_pages','pending_variables','extraction_pending','neutral_unchanged','retained_originals','unsafe_excluded')]==[917,471,148,43,5,15,530,2]
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==317
 choices=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.102.json');assert len(choices['records'])==7 and sum(len(r['source_labels']) for r in choices['records'])==7 and sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choices['records'])==7
 expected=[list(map(json.loads,l.split('|'))) for l in (SOURCE/'NPC_CHOICES_0.15.102_REVIEWED.tsv').read_text(encoding='utf-8').splitlines() if l and not l.startswith('#')];assert expected==[[r['source_labels'],r['target_labels']] for r in choices['records']]
 revision=load(SOURCE/'NPC_0.15.102_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.102_REVISION_DELTA.json');assert delta['accepted_for_batch102'] and len(revision['actual'])==148
 assert {k:(revision['expected'][k],v) for k,v in revision['actual'].items() if revision['expected'].get(k)!=v}=={r['page']:(r['pinned_revision'],r['current_revision']) for r in delta['records']}
 assert all(r['old_dialogue_templates_retained'] for r in delta['records'])
 current={r['source']:r for r in rows(a.dispatch/'0.15.102-dev/DISPATCH_ROWS.jsonl')};assert len(current)==11252
 previous=rows(SOURCE/'NPC_WORKLIST_0.15.101.jsonl');assert [r['source'] for r in previous]==[r['source'] for r in q]
 pending={r['source'] for r in evidence['pending_variables']};malformed={r['source'] for r in evidence['extraction_pending']};speakers=defaultdict(list)
 for r in previous:
  s=r['source'];r['exact_registration']=s in known;r['registered_targets_not_automatically_certified']=sorted(known.get(s,()))
  if s in new:r['review_status']='authored_and_fixture_checked102'
  if s=="[NPC] Fragilis: Y'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!":r['reviewed_classifier_exception101']='constant sentence confirmed against full pinned template; frozen variable_definition flag not rewritten'
  if s in pending:r['review_status']='structured_dynamic_review_pending102'
  if s in malformed:r['review_status']='malformed_publication_pending102'
  if s in {"[NPC] Ghost of Goon: I'm looking for something.",'[NPC] Ghost of Goon: Something with...reviving capabilities...','[NPC] Ghost of Goon: Thank you...thank you...may he return...'}:r['published_context_caveat101']='inherited_alpha_garden_header_Dante_Goon_rowspan3_not_live_certification'
  if s in current:r.update(runtime_flat_snapshot_not_quality_proof=current[s]['flat_result'],runtime_colored_snapshot_not_quality_proof=current[s]['published_style_result'])
  speakers[r['speaker'] or '(без говорящего)'].append(r)
 (SOURCE/'NPC_WORKLIST_0.15.102.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in previous),encoding='utf-8')
 table=['# Все говорящие — 0.15.102','','Точные ключи публичной очереди, не готовность всей игры. Имена, теги, переменные, исторические и тестовые условия учитываются отдельно.','','| Говорящий | Строк | Точных ключей | Доля | Без ключа | Новых в 102 |','| --- | ---: | ---: | ---: | ---: | ---: |']
 for name,rs in sorted(speakers.items()):
  count=sum(r['exact_registration'] for r in rs);table.append(f'| {html.escape(name)} | {len(rs)} | {count} | {100*count/len(rs):.2f}% | {len(rs)-count} | {sum(r["source"] in new for r in rs)} |')
 (SOURCE/'NPC_ALL_SPEAKERS_0.15.102_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
 stats=load(SOURCE/'NPC_PROGRESS_0.15.102.json');stats.update(new_current_literal_rows=3,retained_current_rows=1,total_runtime_new_literals=319,current_fixture_assertions=current_assertions,runtime_validation='174_fresh_groups_and15_parser_tests_passed',fresh_test_groups=174,parser_tests_passed=15,new_fixture_assertions=assertions,new_choice_fixture_assertions=buttons,runtime_jar_sha256=fingerprint,baseline_jar_sha256=BASE,dispatch_tested_rows=11252,dispatch_new_exact_results=319,unchanged_other_dispatch_rows=10933,remaining_previous_style_gaps=72,extraction_pending_rows=5,receipt_render_code_unchanged=True,live_fps_measured=False,runtime_retained_rows=6,malformed_choice_rows=0,source_review_notes='NPC_0.15.102_REVIEW_NOTES.md')
 write(SOURCE/'NPC_PROGRESS_0.15.102.json',stats);(SOURCE/'NPC_PROGRESS_0.15.102_RU.md').write_text(report(stats,bench,focused),encoding='utf-8')
 destination.mkdir()
 files=list(SOURCE.glob('*0.15.102*'))+[ROOT/'build.gradle',ROOT/'src/main/resources/fabric.mod.json',ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',ROOT/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java',DATA/'npc-expansion102.json',DATA/'npc-current102.json',ROOT/'src/main/resources/META-INF/licenses/NPC102-SOURCES.txt']
 files+=list((ROOT/'tests').glob('*.java'))+[ROOT/'tests/test_npc_corpus77.py']
 files+=list((ROOT/'tools').glob('*npc102*'))+[ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc88.py','prepare_npc92.py','prepare_npc93.py','prepare_npc99.py','prepare_npc100.py','prepare_npc101.py','review_npc92_revision.py','export_npc_corpus77.py')]
 for source in sorted(set(files)):
  target=destination/source.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
 shutil.copy2(jar,destination/jar.name);checks=destination/'checks';checks.mkdir()
 for name in tests:
  for suffix in ('-output.txt','-stderr.txt'):
   source=a.checks/(name+suffix)
   if source.exists() and source.stat().st_size:shutil.copy2(source,checks)
 for name in ('CHECK_SCOPE.json','TestNpcCorpusParser102-output.txt','compile-arguments.txt'):shutil.copy2(a.checks/name,checks)
 shutil.copytree(a.audit,destination/'bytecode')
 for label,path in [('general',a.benchmark),('focused',a.focused)]:
  for source in path.rglob('*'):
   if source.is_file() and 'isolated-' not in str(source.relative_to(path)):
    target=destination/'benchmark'/label/source.relative_to(path);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
 for version in ('0.15.101-dev','0.15.102-dev'):
  target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(a.dispatch/version/'DISPATCH_SUMMARY.json',target)
 (destination/'README_RU.md').write_text('# Перевод NPC — 0.15.102\n\n319 новых переводов: 316 ключей очереди и 3 свежие реплики сверх неё. 7 групп ответов / 7 подписей.\n\n11 092 / 16 244 · 68,28% точных ключей публичной очереди, не всей игры. Остаток 5 152.\n\n[Отчёт и все замеры](translation-source/NPC_PROGRESS_0.15.102_RU.md) · [Контекстная вычитка](translation-source/NPC_0.15.102_REVIEW_NOTES.md) · [Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.102_RU.md).\n\n174 группы + 15 проверок извлечения. Частичный пакет исходников и доказательств, не весь проект. JAR автоматически не установлен. Нулевое влияние на FPS не заявляется.\n',encoding='utf-8')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_0.15.102.zip'
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
 write(destination/'DELIVERY.json',{'jar':str(delivered),'jar_sha256':fingerprint,'archive':str(archive),'archive_sha256':sha(archive),'manifest_files':len(hashes),'archive_entries':len(hashes)+1,'installed_game_modified':False})
 print(json.dumps(load(destination/'DELIVERY.json'),indent=2));print('Progress11092/16244=68.28%;remaining5152;3 current additions separate')
if __name__=='__main__':main()
