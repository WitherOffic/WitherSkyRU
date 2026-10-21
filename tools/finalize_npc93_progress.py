"""Publish only after same-JAR fresh tests, corpus dispatch, bytecode and receipt measurements."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc93 import ROOT,SOURCE,DATA,CORPUS,SPEAKERS,PREVIOUS
from report_npc93 import render_report
BASE='2BF484EC13F14B7080AF9AE4B60A4FCDE37B65B38384CF70F66DA85EA9450304'
DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def lines(p):return [l for l in p.read_text(encoding='utf-8-sig').splitlines() if l and not l.startswith('#')]
def save(p,s):p.write_text(s,encoding='utf-8')
def main():
 parser=argparse.ArgumentParser()
 for k in ('checks','audit','benchmark','focused','bytecode','destination'):parser.add_argument('--'+k,type=Path,required=True)
 a=parser.parse_args();destination=a.destination.resolve();assert destination.parent==DESKTOP and not destination.exists(),'Never overwrite delivery'
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.93-dev.jar';baseline=DESKTOP/'WitherSkyRU-26.1.2-fabric-0.15.92-dev.jar';assert sha(baseline)==BASE;fingerprint=sha(jar)
 qhash=sha(CORPUS/'translation_queue.jsonl');assert qhash=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 manifest=load(PREVIOUS/'HASHES.json')
 for n,h in manifest.items():assert sha(PREVIOUS/n)==h,('Immutable previous delivery changed',n)
 scope=load(a.checks/'CHECK_SCOPE.json');tests=scope['tests_passed'];assert len(tests)==len(set(tests))==161 and set(load(PREVIOUS/'checks/CHECK_SCOPE.json')['tests_passed'])<set(tests)
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes'] and scope['isolated_parallel_workers']==4
 assertions={}
 for name,label,marker in [('TestNpc93','NPC93','541 new authored literals/542 published layouts'),('TestNpcChoices93','NPCChoices93','11 exact ordered groups/12 label slots/11 valid published layouts')]:
  text=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert marker in text;assertions[name]=int(re.search('PASS: '+label+r' (\d+) assertions',text)[1])
 for name in tests:
  assert (a.checks/(name+'-output.txt')).is_file();stderr=(a.checks/(name+'-stderr.txt')).read_text(encoding='utf-8-sig').strip()
  if stderr:assert name=='TestHudMixins' and 'SpongePowered MIXIN' in stderr and all(l.startswith('/*') and l.endswith('*/') for l in stderr.splitlines() if l)
 text=(a.checks/'TestNpcCorpusParser93-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 15 tests' in text and '\nOK' in text
 batch=load(DATA/'npc-expansion93.json')['records'];new={r['source']:r['translation'] for r in batch};assert len(batch)==len(new)==541 and Counter(r['speaker'] for r in batch)==SPEAKERS
 authoring=[];authored={}
 for speaker in SPEAKERS:
  p=SOURCE/('NPC93_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv');authoring.append(p)
  for line in lines(p):
   f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new
 evidence=load(SOURCE/'NPC_SOURCES_0.15.93.json');assert evidence['queue_sha256']==qhash and (len(evidence['records']),len(evidence['blocks']),len(evidence['source_pages']))==(638,320,46)
 assert [len(evidence[k]) for k in ('pending_variables','neutral_unchanged','unsafe_excluded','retained_originals','runtime_retained','extraction_pending')]==[40,5,0,52,0,0]
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==542
 assert any(r['source']=='[NPC] Hina: Congratulations Player.' for r in evidence['pending_variables']) and any(r['source']=='[NPC] Kuudra Archeologist: ∫ 2x - 2x^5 + 7x^3 dx' for r in evidence['neutral_unchanged'])
 blocks={b['block_id']:b for b in evidence['blocks']};context_flags=[]
 for r in evidence['records']:
  if not r['runtime_registration']:continue
  for o in r['all_occurrences']:
   b=blocks[o['block_id']];c=b['condition_context_raw'];flags=[]
   if r['published_row']['speaker']=='Clerk Seraphine' and (any(x in c for x in ('April 2021','The Uprising','0.9-0.20.3','0.8.1-0.9','0.1-0.8.1'))):flags.append('explicit historical version/event condition;not live-certified')
   if '{{ahn|' in c.lower():flags.append('published alpha/experimental note;not live-certified')
   if flags:context_flags.append({'source':r['source'],'occurrence_id':o['occurrence_id'],'block_id':b['block_id'],'condition_context_raw':c,'flags':flags})
 assert len({r['source'] for r in context_flags if 'Clerk Seraphine' in r['source']})==13
 revision=load(SOURCE/'NPC_0.15.93_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.93_REVISION_DELTA.json')
 assert not revision['matches_pinned_corpus'] and revision['accepted_after_exact_delta_review'] and len(revision['pages'])==46
 assert revision['delta_review_sha256']==sha(SOURCE/'NPC_0.15.93_REVISION_DELTA.json') and delta['accepted_for_batch93'] and delta['queue_sha256']==qhash
 changes={'Mage Guard':859587,'Barbarian Guard':859588,'Spaceman':859732};expected={r['title']:r['revision'] for r in evidence['source_pages']};assert expected|changes=={r['title']:r['revisions'][0]['revid'] for r in revision['pages']}
 assert revision['differences']=={r['page']:{'pinned':r['pinned_revision'],'current':r['current_revision']} for r in delta['records']} and all(r['npc_utterances_and_conditions_unchanged'] and r['entire_dialogue_section_including_conditions_unchanged'] for r in delta['records'])
 reuse=load(SOURCE/'NPC_SHARED_REUSE_0.15.93.json');assert len(reuse['records'])==60
 for r in reuse['records']:
  donor=SOURCE/r['donor_file'];assert sha(donor)==r['donor_sha256'] and r['donor_authored_row'] in lines(donor) and not r['automatic_translation'];d=r['donor_authored_row'].split('|');f=r['new_authored_row'].split('|');assert d[1:]==f[1:] and new[r['source']]==f'[NPC] {f[0]}: {f[2]}'
 choices=[tuple(map(json.loads,l.split('|'))) for l in lines(SOURCE/'NPC_CHOICES_0.15.93_REVIEWED.tsv')];ce=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.93.json')
 assert len(choices)==len(ce['records'])==11 and sum(len(x) for x,y in choices)==12 and [(r['source_labels'],r['target_labels']) for r in ce['records']]==choices
 assert not ce['retained_groups'] and len(ce['heading_only_excluded'])==6 and sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in ce['records'])==11
 before=load(a.audit/'0.15.92-dev/DISPATCH_SUMMARY.json');after=load(a.audit/'0.15.93-dev/DISPATCH_SUMMARY.json');assert before['runtime_jar_sha256']==BASE and after['runtime_jar_sha256']==fingerprint and before['queue_sha256']==after['queue_sha256']==qhash
 assert before['tested_literal_rows']==after['tested_literal_rows']==11247 and (before['flat_changed_not_quality_proof'],before['published_style_changed_not_quality_proof'])==(8696,8624) and (after['flat_changed_not_quality_proof'],after['published_style_changed_not_quality_proof'])==(9237,9165)
 old={r['source']:r for r in rows(a.audit/'0.15.92-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.audit/'0.15.93-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==current.keys() and len(new.keys()&current.keys())==541
 for s,r in current.items():
  if s in new:assert r['flat_result']==r['published_style_result']==new[s],s
  else:assert all(r[k]==old[s][k] for k in ('flat_result','published_style_result')),s
 gaps=[r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
 benchmark=load(a.benchmark/'RAW.json');assert len(benchmark['summary'])==33 and sum(r['equal_useful_output'] for r in benchmark['summary'])==31 and [r['version'] for r in benchmark['records']]==['0.15.92-dev','0.15.93-dev','0.15.93-dev','0.15.92-dev']
 for r in benchmark['records']:
  assert r['jar_sha256']==(BASE if r['version']=='0.15.92-dev' else fingerprint) and len([k for k in r['results'] if k!='scope'])==33
  for mode,v in r['results'].items():
   if mode!='scope':assert all(len(v[k])==4 for k in ('rounds_ns','rounds_cpu_ns','rounds_bytes'))
 focused=load(a.focused/'RAW.json');assert len(focused['summary'])==6 and all(r['equal_useful_output'] for r in focused['summary']) and [r['version'] for r in focused['records']]==['0.15.92-dev','0.15.93-dev','0.15.93-dev','0.15.92-dev']
 for r in focused['records']:
  assert r['jar_sha256']==(BASE if r['version']=='0.15.92-dev' else fingerprint) and len([k for k in r['results'] if k!='scope'])==6
  for mode,v in r['results'].items():
   if mode!='scope':assert all(len(v[k])==4 for k in ('rounds_ns','rounds_cpu_ns','rounds_bytes'))
 bytecode=load(a.bytecode/'RESULT.json');assert bytecode['runtime_jar_sha256']==fingerprint and len(bytecode['records'])==4 and all(r['executable_disassembly_identical'] for r in bytecode['records'])
 with zipfile.ZipFile(baseline) as x,zipfile.ZipFile(jar) as y:
  assert y.testzip() is None and not set(x.namelist())-set(y.namelist());changed=sorted(n for n in set(x.namelist())&set(y.namelist()) if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
  assert changed==['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
  assert added==['META-INF/licenses/NPC93-SOURCES.txt','data/witherskyru/npc-expansion93.json'] and json.loads(y.read('fabric.mod.json'))['version']=='0.15.93-dev'
  assert y.read('data/witherskyru/npc-expansion93.json')==(DATA/'npc-expansion93.json').read_bytes()
 for n,pattern in [('NpcDialogueTranslator.java',r'public static NpcDialogueTranslator storyDialogues\(\) throws IOException \{.*?(?=    public static NpcDialogueTranslator winterDialogues)'),('ChatNotificationTranslator.java',r'(private static final java\.util\.Map<List<String>,List<String>> SAFETY_CHOICES=java\.util\.Map\.ofEntries\().*?(\n    \);)')]:
  rel='src/main/java/ru/wither/skyru/'+n;oldsource=(PREVIOUS/rel).read_text(encoding='utf-8-sig');newsource=(ROOT/rel).read_text(encoding='utf-8-sig');assert len(re.findall(pattern,oldsource,re.S))==len(re.findall(pattern,newsource,re.S))==1 and re.sub(pattern,'<data>',oldsource,flags=re.S)==re.sub(pattern,'<data>',newsource,flags=re.S)
 for rel in ('translation-source/translations.json','translation-source/coverage.json','src/main/resources/data/witherskyru/corrections.tsv'):assert sha(ROOT/rel)==manifest[rel]
 stats=load(SOURCE/'NPC_PROGRESS_0.15.93.json');stats.update(runtime_validation='161_fresh_groups_passed',fresh_test_groups=161,parser_tests_passed=15,assertions=assertions,runtime_jar_sha256=fingerprint,runtime_changed_files=changed,runtime_added_files=added,render_hud_f3_classes_byte_identical=True,matching_logic_unchanged=True,published_npc_layouts_checked=542,additional_choice_groups=11,additional_choice_slots=12,remaining_original_style_gap=72,outside_selected_dispatch_text_unchanged=True,dispatch_before=before,dispatch_after=after,benchmark_summary=benchmark['summary'],focused_benchmark_summary=focused['summary'],current_revision_check='43 unchanged/3 exact reviewed changes outside Dialogue;not live certification',inner_class_bytecode=bytecode,context_flag_records=len(context_flags));write(SOURCE/'NPC_PROGRESS_0.15.93.json',stats)
 write(SOURCE/'NPC_STYLE_REMAINING_0.15.93.json',{'scope':'72 older unchanged candidates,not certified fixed','records':gaps});write(SOURCE/'NPC_ARCHIVAL_PROVENANCE_0.15.93.json',{'queue_sha256':qhash,'scope':'Explicit event/version and alpha conditions preserved;not live certification.13 Clerk historical keys.','records':context_flags})
 exact=defaultdict(set)
 for p in DATA.glob('*.json'):
  for r in load(p).get('records',[]):
   if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
 worklist=rows(SOURCE/'NPC_WORKLIST_0.15.92.jsonl');queue=rows(CORPUS/'translation_queue.jsonl');assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue] and sum(r['exact_registration'] for r in worklist)==8735
 pending={r['source'] for r in evidence['pending_variables']};neutral={r['source'] for r in evidence['neutral_unchanged']};flags={r['source'] for r in context_flags};bySpeaker=defaultdict(list)
 for r in worklist:
  s=r['source'];r.update(exact_registration=bool(exact.get(s)),registered_targets_not_automatically_certified=sorted(exact.get(s,set())))
  if s in new:r.update(review_status='authored_and_fixture_checked93',all_published_npc_layouts_checked93=True)
  if s in pending:r.update(structural_review_pending93=True,template_coverage_certified93=False)
  if s in neutral:r['review_status']='neutral_no_translation_needed93'
  if s in flags:r['published_context_caveat93']=True
  if s in current:r.update(runtime_flat_snapshot_not_quality_proof=current[s]['flat_result'],runtime_colored_snapshot_not_quality_proof=current[s]['published_style_result'])
  bySpeaker[r['speaker'] or '(без говорящего)'].append(r)
 assert sum(r['exact_registration'] for r in worklist)==9276 and sum(r.get('review_status')=='authored_and_fixture_checked93' for r in worklist)==541
 save(SOURCE/'NPC_WORKLIST_0.15.93.jsonl',''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist))
 table=['# Все говорящие — 0.15.93','','Публичная очередь, не полный реестр игры. Точный ключ не гарантирует живую доступность или все динамические ветки.','','| Говорящий | Строк | Точных ключей | Доля | Без ключа | Новых в 93 |','| --- | ---: | ---: | ---: | ---: | ---: |']
 for name,rs in sorted(bySpeaker.items()):
  count=sum(r['exact_registration'] for r in rs);table.append(f'| {html.escape(name)} | {len(rs)} | {count} | {100*count/len(rs):.2f}% | {len(rs)-count} | {sum(r["source"] in new for r in rs)} |')
 save(SOURCE/'NPC_ALL_SPEAKERS_0.15.93_RU.md','\n'.join(table)+'\n');render_report(SOURCE,SPEAKERS,assertions,revision,benchmark,jar,baseline,fingerprint,focused)
 files=[p for p in SOURCE.glob('*0.15.93*') if p.is_file()]+authoring+[SOURCE/'translations.json',SOURCE/'coverage.json',SOURCE/'NPC92_OFFEA_REVIEWED.tsv',DATA/'npc-expansion93.json',ROOT/'src/main/resources/META-INF/licenses/NPC93-SOURCES.txt',ROOT/'src/main/resources/fabric.mod.json',DATA/'corrections.tsv',ROOT/'build.gradle']
 files += [ROOT/'src/main/java/ru/wither/skyru'/n for n in ('NpcDialogueTranslator.java','ChatNotificationTranslator.java')]
 files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==9042' in p.read_text(encoding='utf-8-sig') or p.name in ('TestNpc93.java','TestNpcChoices93.java','BenchmarkNpc93.java','AuditNpcCorpus77.java')]+[ROOT/'tests/test_npc_corpus77.py']
 files += [ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc88.py','prepare_npc92.py','prepare_npc93.py','review_npc92_revision.py','review_npc93_revision.py','reuse_npc93.py','report_npc93.py','derive_npc93_checks.py','derive_npc93_benchmark.py','export_npc_corpus77.py','check_npc93.ps1','check_npc93_parallel.ps1','dispatch_npc93.ps1','benchmark_npc93.ps1','benchmark_npc93_focused.ps1','check_npc93_bytecode.ps1','finalize_npc93_progress.py')]
 destination.mkdir()
 for p in sorted(set(files)):
  target=destination/p.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
 checks=destination/'checks';checks.mkdir()
 for name in tests:
  shutil.copy2(a.checks/(name+'-output.txt'),checks)
  if (a.checks/(name+'-stderr.txt')).stat().st_size:shutil.copy2(a.checks/(name+'-stderr.txt'),checks)
 for name in ('CHECK_SCOPE.json','TestNpcCorpusParser93-output.txt'):shutil.copy2(a.checks/name,checks)
 shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json');shutil.copy2(a.focused/'RAW.json',destination/'BENCHMARK_FOCUSED_RAW.json');shutil.copytree(a.bytecode,destination/'bytecode')
 for label,path in (('initial',a.benchmark),('focused',a.focused)):
  target=destination/'benchmark_harness'/label;target.mkdir(parents=True);shutil.copy2(path/'BenchmarkNpc93.class',target)
 for v in ('0.15.92-dev','0.15.93-dev'):
  target=destination/'dispatch'/v;target.mkdir(parents=True);shutil.copy2(a.audit/v/'DISPATCH_SUMMARY.json',target)
 save(destination/'README_RU.md','# Большая партия 93\n\n541 новый NPC-ключ / 43 обозначения говорящего. 11 групп ответов / 12 подписей.\n\n9 276 / 16 244 · 57,10% точных ключей публичной базы, не всей игры. 13 новых ключей Серафины исторические; отдельные условия Spaceman экспериментальные.\n\n[Отчёт](translation-source/NPC_PROGRESS_0.15.93_RU.md) · [Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.93_RU.md)\n\nЧастичный пакет исходников и отчётов, не полный проект. JAR отдельно, автоматически не установлен.\n')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_17.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for name in sorted(hashes):z.write(destination/name,name)
  z.write(destination/'HASHES.json','HASHES.json')
 with zipfile.ZipFile(archive) as z:
  assert z.testzip() is None
  for name,h in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==h
  entries=len(z.namelist())
 delivered=DESKTOP/jar.name
 if delivered.exists():assert sha(delivered)==fingerprint,'Never replace different delivered JAR'
 else:shutil.copy2(jar,delivered)
 assert sha(delivered)==fingerprint and sha(baseline)==BASE and sha(CORPUS/'translation_queue.jsonl')==qhash
 write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered),'jar_sha256':fingerprint,'installed_game_modified':False});print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))
if __name__=='__main__':main()
