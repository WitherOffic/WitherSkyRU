"""Publish only after fresh same-JAR checks;never install or overwrite old deliveries."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc89 import ROOT,SOURCE,DATA,CORPUS,SPEAKERS,RUNTIME_RETAINED,PREVIOUS

BASE='71868BB61F6108E032859FA873F2AB5B51E95580321D4831E677E0EBDAC42112'
DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
def load(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def lines(path):return [s for s in path.read_text(encoding='utf-8-sig').splitlines() if s and not s.startswith('#')]
def save(path,text):path.write_text(text,encoding='utf-8')
def number(n):return f'{n:,}'.replace(',',' ')

def main():
 p=argparse.ArgumentParser()
 for name in ('checks','audit','benchmark','bytecode','destination'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert not destination.exists(),'Never overwrite a delivery'
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.89-dev.jar';baseline=DESKTOP/'WitherSkyRU-26.1.2-fabric-0.15.88-dev.jar'
 assert sha(baseline)==BASE;fingerprint=sha(jar);qhash=sha(CORPUS/'translation_queue.jsonl')
 assert qhash=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 scope=load(a.checks/'CHECK_SCOPE.json');tests=scope['tests_passed']
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes'] and scope['isolated_parallel_workers']==4
 assert len(tests)==len(set(tests))==153 and set(load(PREVIOUS/'checks/CHECK_SCOPE.json')['tests_passed'])<set(tests)
 assertions={}
 for name,label,marker in [('TestNpc89','NPC89','734 new authored literals/736 published layouts'),('TestNpcChoices89','NPCChoices89','9 exact ordered groups/10 label slots/9 valid published layouts')]:
  out=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert marker in out;assertions[name]=int(re.search(r'PASS: '+label+r' (\d+) assertions',out)[1])
 for name in tests:assert (a.checks/(name+'-output.txt')).is_file()
 parser=(a.checks/'TestNpcCorpusParser89-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 15 tests' in parser and '\nOK' in parser
 batch=load(DATA/'npc-expansion89.json')['records'];new={r['source']:r['translation'] for r in batch}
 assert len(new)==len(batch)==734 and Counter(r['speaker'] for r in batch)==SPEAKERS
 authored={};authoring=[]
 for speaker in SPEAKERS:
  path=SOURCE/('NPC89_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv');authoring.append(path)
  for line in lines(path):
   f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new
 evidence=load(SOURCE/'NPC_SOURCES_0.15.89.json');assert evidence['queue_sha256']==qhash
 assert (len(evidence['records']),len(evidence['source_pages']),len(evidence['blocks']))==(757,29,363)
 assert [len(evidence[k]) for k in ('pending_variables','neutral_unchanged','unsafe_excluded','retained_originals','runtime_retained')]==[13,5,0,4,1]
 assert {r['source'] for r in evidence['runtime_retained']}==RUNTIME_RETAINED
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==736
 archival=load(SOURCE/'NPC_ARCHIVAL_PROVENANCE_0.15.89.json');archivekeys={r['source'] for r in archival['records']}
 assert archival['queue_sha256']==qhash and len(archivekeys)==53 and archivekeys<=new.keys()
 revision=load(SOURCE/'NPC_0.15.89_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.89_REVISION_DELTA.json')
 assert not revision['matches_pinned_corpus'] and revision['accepted_after_exact_delta_review'] and len(revision['pages'])==29
 assert revision['differences']=={'Pesthunter Phillip':{'pinned':859295,'current':859508}} and revision['delta_review_sha256']==sha(SOURCE/'NPC_0.15.89_REVISION_DELTA.json')
 assert delta['only_expected_replacement_of_info_needed'] and delta['all_previous_dialogues_and_conditions_unchanged'] and delta['new_dialogue_count']==1 and revision['checked_client_date']=='2026-10-18'
 expected={r['title']:r['revision'] for r in evidence['source_pages']};actual={r['title']:r['revisions'][0]['revid'] for r in revision['pages']};assert actual==expected|{'Pesthunter Phillip':859508}
 extras=load(SOURCE/'NPC_EXTRAS_0.15.89.json');extraRecords=load(DATA/'npc-extras89.json')['records']
 assert extras['queue_sha256']==qhash and len(extras['records'])==len(extraRecords)==3
 assert Counter(r['kind'] for r in extras['records'])=={'raw_source_repair':2,'new_published_revision':1}
 assert all(not r['counted_as_frozen_corpus_exact_key'] for r in extras['records'])
 choices=[tuple(map(json.loads,line.split('|'))) for line in lines(SOURCE/'NPC_CHOICES_0.15.89_REVIEWED.tsv')];ce=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.89.json')
 assert len(choices)==len(ce['records'])==9 and sum(len(s) for s,t in choices)==10
 assert [(r['source_labels'],r['target_labels']) for r in ce['records']]==choices and len(ce['retained_groups'])==3
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in ce['records'])==9
 before=load(a.audit/'0.15.88-dev/DISPATCH_SUMMARY.json');after=load(a.audit/'0.15.89-dev/DISPATCH_SUMMARY.json')
 assert before['runtime_jar_sha256']==BASE and after['runtime_jar_sha256']==fingerprint and before['queue_sha256']==after['queue_sha256']==qhash
 assert before['tested_literal_rows']==after['tested_literal_rows']==11247
 assert (before['flat_changed_not_quality_proof'],before['published_style_changed_not_quality_proof'])==(6012,5940)
 assert (after['flat_changed_not_quality_proof'],after['published_style_changed_not_quality_proof'])==(6746,6674)
 old={r['source']:r for r in rows(a.audit/'0.15.88-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.audit/'0.15.89-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==current.keys()
 for s,r in current.items():
  if s in new:assert r['flat_result']==r['published_style_result']==new[s],s
  else:assert all(r[k]==old[s][k] for k in ('flat_result','published_style_result')),s
 assert len(new.keys()&current.keys())==734
 gaps=[r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
 benchmark=load(a.benchmark/'RAW.json');assert len(benchmark['summary'])==25 and [r['version'] for r in benchmark['records']]==['0.15.88-dev','0.15.89-dev','0.15.89-dev','0.15.88-dev']
 for r in benchmark['records']:
  assert r['jar_sha256']==(BASE if r['version']=='0.15.88-dev' else fingerprint) and len([k for k in r['results'] if k!='scope'])==25
  for mode,result in r['results'].items():
   if mode!='scope':assert all(len(result[k])==4 for k in ('rounds_ns','rounds_cpu_ns','rounds_bytes'))
 bytecode=load(a.bytecode/'RESULT.json');assert bytecode['runtime_jar_sha256']==fingerprint and len(bytecode['records'])==4 and all(r['executable_disassembly_identical'] for r in bytecode['records'])
 with zipfile.ZipFile(baseline) as x,zipfile.ZipFile(jar) as y:
  assert y.testzip() is None;xn,yn=set(x.namelist()),set(y.namelist());assert not xn-yn
  changed=sorted(n for n in xn&yn if x.read(n)!=y.read(n));added=sorted(yn-xn)
  assert changed==['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator$StyleCorrections.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
  assert added==['META-INF/licenses/NPC89-SOURCES.txt','data/witherskyru/npc-expansion89.json','data/witherskyru/npc-extras89.json'],added
  for n in ('npc-expansion89.json','npc-extras89.json'):assert y.read('data/witherskyru/'+n)==(DATA/n).read_bytes()
 manifest=load(PREVIOUS/'HASHES.json')
 for n in ('translation-source/translations.json','translation-source/coverage.json','src/main/resources/data/witherskyru/corrections.tsv'):assert sha(PREVIOUS/n)==manifest[n] and sha(ROOT/n)==manifest[n]
 # Outside the startup resource factory and exact choice table,source is identical.
 for n,pattern in [('NpcDialogueTranslator.java',r'public static NpcDialogueTranslator storyDialogues\(\) throws IOException \{.*?(?=    public static NpcDialogueTranslator winterDialogues)'),('ChatNotificationTranslator.java',r'(private static final java\.util\.Map<List<String>,List<String>> SAFETY_CHOICES=java\.util\.Map\.ofEntries\().*?(\n    \);)')]:
  relative='src/main/java/ru/wither/skyru/'+n;assert sha(PREVIOUS/relative)==manifest[relative]
  oldsource=(PREVIOUS/relative).read_text(encoding='utf-8-sig');newsource=(ROOT/relative).read_text(encoding='utf-8-sig')
  assert len(re.findall(pattern,oldsource,re.S))==len(re.findall(pattern,newsource,re.S))==1
  assert re.sub(pattern,'<verified startup-data region>',oldsource,flags=re.S)==re.sub(pattern,'<verified startup-data region>',newsource,flags=re.S),'No hidden hot-path source changes'
 stats=load(SOURCE/'NPC_PROGRESS_0.15.89.json');assert stats['exact_resource_after']==6784
 stats.update(runtime_validation='153_fresh_groups_passed',fresh_test_groups=153,assertions=assertions,runtime_jar_sha256=fingerprint,runtime_changed_files=changed,runtime_added_files=added,render_hud_f3_classes_byte_identical=True,old_text_resources_byte_identical=True,raw_source_repairs_separate=2,new_published_literals_separate=1,explicit_archival_rows=53,other_rows_not_live_certified=681,compatible_published_npc_layouts_checked=736,additional_choice_groups=9,additional_choice_slots=10,remaining_original_style_gap=72,outside_selected_dispatch_text_unchanged=True,dispatch_before=before,dispatch_after=after,benchmark_summary=benchmark['summary'],current_revision_check='28 unchanged;one exact reviewed addition',parser_tests_passed=15,inner_class_bytecode=bytecode)
 write(SOURCE/'NPC_PROGRESS_0.15.89.json',stats);write(SOURCE/'NPC_STYLE_REMAINING_0.15.89.json',{'scope':'72 older candidates unchanged;not certified fixed','records':gaps})
 exact=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
 worklist=rows(SOURCE/'NPC_WORKLIST_0.15.88.jsonl');queue=rows(CORPUS/'translation_queue.jsonl');assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue] and sum(r['exact_registration'] for r in worklist)==6050
 pending={r['source'] for r in evidence['pending_variables']};neutral={r['source'] for r in evidence['neutral_unchanged']};frozenRepairs={r['frozen_queue_source']:r['source'] for r in extras['records'] if r['frozen_queue_source']};bySpeaker=defaultdict(list)
 for r in worklist:
  s=r['source'];r.update(exact_registration=bool(exact.get(s)),registered_targets_not_automatically_certified=sorted(exact.get(s,set())))
  if s in new:r.update(review_status='authored_and_fixture_checked89',all_published_npc_layouts_checked89=True)
  if s in archivekeys:r['explicit_archival_provenance89']=True
  if s in pending:r.update(structural_review_pending89=True,template_coverage_certified89=False)
  if s in neutral:r['review_status']='neutral_no_translation_needed89'
  if s in RUNTIME_RETAINED:r['existing_dynamic_result_retained89']=True
  if s in frozenRepairs:r.update(known_frozen_extraction_artifact89=True,corrected_raw_source_separate89=frozenRepairs[s])
  if s in current:r.update(runtime_flat_snapshot_not_quality_proof=current[s]['flat_result'],runtime_colored_snapshot_not_quality_proof=current[s]['published_style_result'])
  bySpeaker[r['speaker'] or '(без говорящего)'].append(r)
 assert sum(r['exact_registration'] for r in worklist)==6784 and sum(r.get('review_status')=='authored_and_fixture_checked89' for r in worklist)==734
 save(SOURCE/'NPC_WORKLIST_0.15.89.jsonl',''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist))
 table=['# Все говорящие — 0.15.89','','Публичная очередь, не полный реестр игры. Точные ключи не гарантируют полный перевод каждой динамической ветки или живую доступность.','','| Говорящий | Всего строк | Точных ключей | Доля | Новых в 89 | Из них архивных | Структурных признаков |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
 for speaker,values in sorted(bySpeaker.items()):
  count=sum(r['exact_registration'] for r in values);label=html.escape(speaker).replace('|','\\|');table.append(f'| {label} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in archivekeys for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
 save(SOURCE/'NPC_ALL_SPEAKERS_0.15.89_RU.md','\n'.join(table)+'\n')
 report=['# Большая партия NPC — 0.15.89','','`█████████████░░░░░░░░░░░░░░░░░` **41,76%** точных ключей публичной базы; полоса округлена.','','| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи | 6 050 · 37,24% | 6 784 · 41,76% |','| Без подтверждённого точного ключа | 10 194 | 9 460 · 58,24% |','| Строк в неизменяемой базе | 16 244 | 16 244 |','','Это не процент полностью готовой игры. База содержит архивные, структурные и редакторские строки; закрытые серверные скрипты недоступны. Отсутствие точного ключа не исключает динамического перевода.','','| Говорящий | Новых реплик |','| --- | ---: |']
 report += [f'| {s} | {c} |' for s,c in SPEAKERS.items()]
 report += ['| Всего | 734 |','','Дополнительно: 9 групп ответов / 10 подписей, 2 исправленных исходных ключа (Молберт и Удел), 1 новая фраза Филлипа. Они не увеличивают счётчик 6 784. Из 734 строк 53 явно архивные/удалённые; для остальных 681 живое присутствие не сертифицировано. В выбранных 757 строках ещё 13 структурных кандидатов, 5 нейтральных, 4 прежних точных ключа и 1 уже работающий динамический результат.','','## Качество','','[Контекстная вычитка](NPC_0.15.89_REVIEW_NOTES.md).',f'На одном окончательном JAR прошли 153 свежие группы проверок и 15 тестов извлечения. Новые реплики и отдельные исходные ключи: {number(assertions["TestNpc89"])} проверок; кнопки: {number(assertions["TestNpcChoices89"])}. Проверены все 736 опубликованных оформлений, 9 опубликованных схем ответов, свежие цвета/стили/команды/подсказки, фрагментация, числа, время, счётчики, переопределения и защита пользовательского/подписанного чата.',f'API: {revision["checked_at"]}. 28 страниц не изменились. На странице Филлипа ровно одна новая фраза заменила заглушку InfoNeeded; весь остальной текст совпадает. Полный разбор ревизий сохранён отдельно, замороженная база не переписана.','','| Те же 11 247 строк: срабатывание, не оценка качества | 88 | 89 |','| --- | ---: | ---: |','| Обычный текст | 6 012 | 6 746 |','| Опубликованные цвета | 5 940 | 6 674 |','','Все 734 новых ключа сработали в обоих режимах. Вне них результаты неизменны. 72 старых цветных кандидата остаются открытыми.','','## Нагрузка','','Данные сливаются с прежней точной картой один раз при запуске. Новых провайдеров, регулярных выражений, кадровых проходов и сетевого перевода нет. Изменены только регистрация двух ресурсов и таблица ответов. HUD, F3 и все классы вне двух прежних переводчиков побайтово прежние. У четырёх внутренних классов исполняемый код совпал: отличаются отладочные номера строк. Все старые текстовые ресурсы побайтово сохранены.',f'Рост JAR: {number(jar.stat().st_size-baseline.stat().st_size)} байт. Полная выгрузка в мод не включена.','','Четыре отдельные JVM: 88 → 89 → 89 → 88; 25 сценариев, не менее 200 000 прогревочных вызовов и четыре раунда не менее 40 000 измеряемых вызовов каждого сценария.','','| Сценарий | Время до → после, мкс/сообщение, два порядка | Изменение | Байты/сообщение до → после |','| --- | ---: | ---: | ---: |']
 for r in benchmark['summary']:
  timings=' / '.join(f'{o["before_ns"]/1000:.2f} → {o["after_ns"]/1000:.2f}' for o in r['orders']);change=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);allocation=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in r['orders']);report.append(f'| {r["mode"]} | {timings} | {change} | {allocation} |')
 equal=[o['wall_change_percent'] for r in benchmark['summary'] if r['equal_useful_output'] for o in r['orders']]
 report+=['',f'Прежние сценарии с одинаковой полезной работой: от {min(equal):+.1f}% до {max(equal):+.1f}% в этих запусках. Разброс не доказывает ускорения или отсутствия микролагов. Три new_* сценария теперь выполняют полезный перевод вместо пропуска: это разная работа. Все раунды сохранены в BENCHMARK_RAW.json.','','Это измерения входящего чата, не FPS, 0,1% lows или время кадра в игре. Нулевая нагрузка не обещана. Версия автоматически не установлена, Minecraft не перезапускался.','','## Остаток','','Вся база ещё не переведена. Открыты 9 460 строк без подтверждённого точного ключа, структурные случаи, 72 старых цветных кандидата и прежний широкий аудит ReviewedLoreStyles. Из четырёх блоков с ошибками извлечения исправлены правильные ключи Рамеро (88), Молберта и Удела (89); Bingo остаётся открытым.','','[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.89_RU.md).','',f'SHA-256 JAR: `{fingerprint}`.']
 save(SOURCE/'NPC_PROGRESS_0.15.89_RU.md','\n'.join(report)+'\n')
 files=[p for p in SOURCE.glob('*0.15.89*') if p.is_file()]+authoring+[SOURCE/'NPC89_EXTRAS_REVIEWED.tsv',SOURCE/'translations.json',SOURCE/'coverage.json']
 files += [DATA/n for n in ('npc-expansion89.json','npc-extras89.json')]+[ROOT/'src/main/resources/META-INF/licenses/NPC89-SOURCES.txt',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
 files += [ROOT/'src/main/java/ru/wither/skyru'/n for n in ('NpcDialogueTranslator.java','ChatNotificationTranslator.java')]
 files += [ROOT/'tests'/n for n in ('TestNpc89.java','TestNpcChoices89.java','BenchmarkNpc89.java','AuditNpcCorpus77.java','test_npc_corpus77.py')]
 files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==6550' in p.read_text(encoding='utf-8-sig')]
 files += [ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc88.py','prepare_npc89.py','prepare_npc89_extras.py','review_npc89_revision.py','export_npc_corpus77.py','check_npc89.ps1','check_npc89_parallel.ps1','dispatch_npc89.ps1','benchmark_npc89.ps1','check_npc89_bytecode.ps1','finalize_npc89_progress.py')]
 destination.mkdir(parents=True)
 for path in sorted(set(files)):
  target=destination/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
 checks=destination/'checks';checks.mkdir()
 for name in tests:shutil.copy2(a.checks/(name+'-output.txt'),checks)
 for name in ('CHECK_SCOPE.json','TestNpcCorpusParser89-output.txt'):shutil.copy2(a.checks/name,checks)
 shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json');shutil.copytree(a.bytecode,destination/'bytecode')
 for v in ('0.15.88-dev','0.15.89-dev'):
  target=destination/'dispatch'/v;target.mkdir(parents=True);shutil.copy2(a.audit/v/'DISPATCH_SUMMARY.json',target)
 save(destination/'README_RU.md','# Большая партия 89\n\n734 новые реплики / 27 обозначений говорящего; 9 групп ответов / 10 подписей; 2 исправления исходных ключей и 1 новая опубликованная фраза отдельно. 53 архивные строки явно отмечены.\n\n6 784 / 16 244 точных ключей · 41,76%. Это не процент полностью готовой игры.\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.89_RU.md) · [Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.89_RU.md)\n\nЧастичный пакет исходников и отчётов, не полный проект. JAR выдаётся отдельно, автоматически не установлен.\n')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_13.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for name in sorted(hashes):z.write(destination/name,name)
  z.write(destination/'HASHES.json','HASHES.json')
 with zipfile.ZipFile(archive) as z:
  assert z.testzip() is None
  for name,expected in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==expected
  entries=len(z.namelist())
 delivered=DESKTOP/jar.name
 if delivered.exists():assert sha(delivered)==fingerprint,'Never replace a different delivered JAR'
 else:shutil.copy2(jar,delivered)
 assert sha(delivered)==fingerprint and sha(baseline)==BASE and sha(CORPUS/'translation_queue.jsonl')==qhash
 write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered),'jar_sha256':fingerprint,'installed_game_modified':False})
 print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))
if __name__=='__main__':main()
