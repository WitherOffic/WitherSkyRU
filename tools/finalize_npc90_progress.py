"""Validate same-JAR evidence, then publish a new immutable batch90 delivery."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc90 import ROOT,SOURCE,DATA,CORPUS,SPEAKERS,PREVIOUS,LITERAL_EXCEPTIONS

BASE='78C8A7FF013AF953AE5A07569C344208AF4739C0E759BEAF7DD1441AF24C8456'
DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def lines(p):return [s for s in p.read_text(encoding='utf-8-sig').splitlines() if s and not s.startswith('#')]
def save(p,s):p.write_text(s,encoding='utf-8')
def number(n):return f'{n:,}'.replace(',',' ')

def main():
 p=argparse.ArgumentParser()
 for name in ('checks','audit','benchmark','bytecode','destination'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert destination.parent==DESKTOP and not destination.exists(),'Never overwrite deliveries'
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.90-dev.jar';baseline=DESKTOP/'WitherSkyRU-26.1.2-fabric-0.15.89-dev.jar'
 assert sha(baseline)==BASE;fingerprint=sha(jar);qhash=sha(CORPUS/'translation_queue.jsonl')
 assert qhash=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 manifest=load(PREVIOUS/'HASHES.json')
 for name,h in manifest.items():assert sha(PREVIOUS/name)==h,('Previous package altered',name)
 scope=load(a.checks/'CHECK_SCOPE.json');tests=scope['tests_passed']
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes'] and scope['isolated_parallel_workers']==4
 assert len(tests)==len(set(tests))==155 and set(load(PREVIOUS/'checks/CHECK_SCOPE.json')['tests_passed'])<set(tests)
 assertions={}
 for name,label,marker in [('TestNpc90','NPC90','711 new authored literals/711 published layouts'),('TestNpcChoices90','NPCChoices90','30 exact ordered groups/61 label slots/31 valid published layouts')]:
  out=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert marker in out;assertions[name]=int(re.search(r'PASS: '+label+r' (\d+) assertions',out)[1])
 for name in tests:
  assert (a.checks/(name+'-output.txt')).is_file()
  stderr=(a.checks/(name+'-stderr.txt')).read_text(encoding='utf-8-sig').strip()
  if stderr:assert name=='TestHudMixins' and 'SpongePowered MIXIN' in stderr and all(s.startswith('/*') and s.endswith('*/') for s in stderr.splitlines() if s),name
 parser=(a.checks/'TestNpcCorpusParser90-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 15 tests' in parser and '\nOK' in parser
 batch=load(DATA/'npc-expansion90.json')['records'];new={r['source']:r['translation'] for r in batch}
 assert len(new)==len(batch)==711 and Counter(r['speaker'] for r in batch)==SPEAKERS
 authored={};authoring=[]
 for speaker in SPEAKERS:
  path=SOURCE/('NPC90_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv');authoring.append(path)
  for line in lines(path):
   f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new and LITERAL_EXCEPTIONS=={'[NPC] Miria: #AD!'} and new['[NPC] Miria: #AD!']=='[NPC] Miria: #РЕКЛАМА!'
 evidence=load(SOURCE/'NPC_SOURCES_0.15.90.json');assert evidence['queue_sha256']==qhash
 assert (len(evidence['records']),len(evidence['source_pages']),len(evidence['blocks']))==(740,32,331)
 assert [len(evidence[k]) for k in ('pending_variables','neutral_unchanged','unsafe_excluded','retained_originals','runtime_retained')]==[14,2,0,13,0]
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==711
 assert not any(r['page_historical_templates'] or r['removed_table_or_heading'] for r in evidence['raw_archive_marker_candidates_not_live_certification'])
 revision=load(SOURCE/'NPC_0.15.90_PUBLIC_REVISION.json')
 assert revision['matches_pinned_corpus'] and not revision['differences'] and len(revision['pages'])==32 and revision['checked_client_date']=='2026-10-18'
 assert {r['title']:r['revision'] for r in evidence['source_pages']}=={r['title']:r['revisions'][0]['revid'] for r in revision['pages']}
 reuse=load(SOURCE/'NPC_SHARED_REUSE_0.15.90.json');assert reuse['count']==len(reuse['records'])==128
 for r in reuse['records']:
  f=r['authored_tsv_row'].split('|');donor=SOURCE/r['reviewed_target_source_file'];assert sha(donor)==r['reviewed_file_sha256'] and r['automatic_translation'] is False
  assert r['source']==f'[NPC] {f[0]}: {f[1]}' and new[r['source']]==f'[NPC] {f[0]}: {f[2]}'
  assert any(x.split('|')[1:3]==f[1:3] for x in lines(donor)) and r['occurrence_ids']
 choices=[tuple(map(json.loads,line.split('|'))) for line in lines(SOURCE/'NPC_CHOICES_0.15.90_REVIEWED.tsv')];ce=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.90.json')
 assert len(choices)==len(ce['records'])==30 and sum(len(s) for s,t in choices)==61
 assert [(r['source_labels'],r['target_labels']) for r in ce['records']]==choices and len(ce['retained_groups'])==3 and len(ce['heading_only_excluded'])==4
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in ce['records'])==31
 changed_labels=[(s,t) for ss,tt in choices for s,t in zip(ss,tt) if s!=t];assert len(changed_labels)==30 and len(set(s for s,t in changed_labels))==27
 # One new exact table entry was already translated by the old dedicated NO/YES handler.
 assert (['NO','YES'],['НЕТ','ДА']) in choices
 before=load(a.audit/'0.15.89-dev/DISPATCH_SUMMARY.json');after=load(a.audit/'0.15.90-dev/DISPATCH_SUMMARY.json')
 assert before['runtime_jar_sha256']==BASE and after['runtime_jar_sha256']==fingerprint and before['queue_sha256']==after['queue_sha256']==qhash
 assert before['tested_literal_rows']==after['tested_literal_rows']==11247
 assert (before['flat_changed_not_quality_proof'],before['published_style_changed_not_quality_proof'])==(6746,6674)
 assert (after['flat_changed_not_quality_proof'],after['published_style_changed_not_quality_proof'])==(7456,7384)
 old={r['source']:r for r in rows(a.audit/'0.15.89-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.audit/'0.15.90-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==current.keys()
 for s,r in current.items():
  if s in new:assert r['flat_result']==r['published_style_result']==new[s],s
  else:assert all(r[k]==old[s][k] for k in ('flat_result','published_style_result')),s
 assert len(new.keys()&current.keys())==710 and not LITERAL_EXCEPTIONS&current.keys()
 gaps=[r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
 benchmark=load(a.benchmark/'RAW.json');assert len(benchmark['summary'])==27 and [r['version'] for r in benchmark['records']]==['0.15.89-dev','0.15.90-dev','0.15.90-dev','0.15.89-dev']
 assert sum(r['equal_useful_output'] for r in benchmark['summary'])==25
 for r in benchmark['records']:
  assert r['jar_sha256']==(BASE if r['version']=='0.15.89-dev' else fingerprint) and len([k for k in r['results'] if k!='scope'])==27
  for mode,result in r['results'].items():
   if mode!='scope':assert all(len(result[k])==4 for k in ('rounds_ns','rounds_cpu_ns','rounds_bytes'))
 bytecode=load(a.bytecode/'RESULT.json');assert bytecode['runtime_jar_sha256']==fingerprint and len(bytecode['records'])==4 and all(r['executable_disassembly_identical'] for r in bytecode['records'])
 with zipfile.ZipFile(baseline) as x,zipfile.ZipFile(jar) as y:
  assert y.testzip() is None;xn,yn=set(x.namelist()),set(y.namelist());assert not xn-yn
  changed=sorted(n for n in xn&yn if x.read(n)!=y.read(n));added=sorted(yn-xn)
  assert changed==['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
  assert added==['META-INF/licenses/NPC90-SOURCES.txt','data/witherskyru/npc-expansion90.json'],added
  assert y.read('data/witherskyru/npc-expansion90.json')==(DATA/'npc-expansion90.json').read_bytes()
  assert x.read('data/witherskyru/corrections.tsv')==y.read('data/witherskyru/corrections.tsv')==(DATA/'corrections.tsv').read_bytes()
  assert load_version(y)=='0.15.90-dev'
 for n in ('translation-source/translations.json','translation-source/coverage.json'):assert sha(ROOT/n)==manifest[n]
 # Permit exactly the startup factory, ordered table and one bounded layout constant change.
 for n,pattern in [('NpcDialogueTranslator.java',r'public static NpcDialogueTranslator storyDialogues\(\) throws IOException \{.*?(?=    public static NpcDialogueTranslator winterDialogues)'),('ChatNotificationTranslator.java',r'(private static final java\.util\.Map<List<String>,List<String>> SAFETY_CHOICES=java\.util\.Map\.ofEntries\().*?(\n    \);)')]:
  relative='src/main/java/ru/wither/skyru/'+n;oldsource=(PREVIOUS/relative).read_text(encoding='utf-8-sig');newsource=(ROOT/relative).read_text(encoding='utf-8-sig')
  assert len(re.findall(pattern,oldsource,re.S))==len(re.findall(pattern,newsource,re.S))==1
  if n=='ChatNotificationTranslator.java':
   oldline=next(x for x in oldsource.splitlines() if 'Pattern SAFETY_LAYOUT=' in x);newline=next(x for x in newsource.splitlines() if 'Pattern SAFETY_LAYOUT=' in x)
   assert oldline.replace('{1,5}','{1,11}')==newline and oldline!=newline
   newsource=newsource.replace(newline,oldline)
  assert re.sub(pattern,'<verified data region>',oldsource,flags=re.S)==re.sub(pattern,'<verified data region>',newsource,flags=re.S),'Unexpected source change'
 stats=load(SOURCE/'NPC_PROGRESS_0.15.90.json');assert stats['exact_resource_after']==7495
 stats.update(runtime_validation='155_fresh_groups_passed',fresh_test_groups=155,assertions=assertions,runtime_jar_sha256=fingerprint,runtime_changed_files=changed,runtime_added_files=added,render_hud_f3_classes_byte_identical=True,old_text_resources_byte_identical=True,compatible_published_npc_layouts_checked=711,additional_choice_table_groups=30,choice_groups_already_handled_before=1,additional_choice_slots=61,changed_choice_slots=30,distinct_changed_choice_labels=27,retained_numeric_symbol_slots=31,heading_only_choice_sources_pending=4,remaining_original_style_gap=72,outside_selected_dispatch_text_unchanged=True,generic_probe_new_rows=710,explicit_literal_exception_separately_tested=True,dispatch_before=before,dispatch_after=after,benchmark_summary=benchmark['summary'],current_revision_check='32 unchanged pinned pages;not live certification',parser_tests_passed=15,inner_class_bytecode=bytecode,shared_reviewed_body_pairs=128,layout_button_bound_before=5,layout_button_bound_after=11)
 write(SOURCE/'NPC_PROGRESS_0.15.90.json',stats);write(SOURCE/'NPC_STYLE_REMAINING_0.15.90.json',{'scope':'72 older candidates unchanged;not certified fixed','records':gaps})
 exact=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
 worklist=rows(SOURCE/'NPC_WORKLIST_0.15.89.jsonl');queue=rows(CORPUS/'translation_queue.jsonl')
 assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue] and sum(r['exact_registration'] for r in worklist)==6784
 pending={r['source'] for r in evidence['pending_variables']};neutral={r['source'] for r in evidence['neutral_unchanged']};bySpeaker=defaultdict(list)
 for r in worklist:
  s=r['source'];r.update(exact_registration=bool(exact.get(s)),registered_targets_not_automatically_certified=sorted(exact.get(s,set())))
  if s in new:r.update(review_status='authored_and_fixture_checked90',all_published_npc_layouts_checked90=True)
  if s in pending:r.update(structural_review_pending90=True,template_coverage_certified90=False)
  if s in neutral:r['review_status']='neutral_no_translation_needed90'
  if s in current:r.update(runtime_flat_snapshot_not_quality_proof=current[s]['flat_result'],runtime_colored_snapshot_not_quality_proof=current[s]['published_style_result'])
  if s in LITERAL_EXCEPTIONS:r.update(explicit_hash_literal_reviewed90=True,outside_generic_dispatch_probe90=True)
  bySpeaker[r['speaker'] or '(без говорящего)'].append(r)
 assert sum(r['exact_registration'] for r in worklist)==7495 and sum(r.get('review_status')=='authored_and_fixture_checked90' for r in worklist)==711
 save(SOURCE/'NPC_WORKLIST_0.15.90.jsonl',''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist))
 table=['# Все говорящие — 0.15.90','','Публичная очередь, не полный реестр игры. Точный ключ не гарантирует перевод всех динамических веток или живую доступность.','','| Говорящий | Строк | Точных ключей | Доля | Осталось без ключа | Новых в 90 | Структурных признаков |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
 for speaker,values in sorted(bySpeaker.items()):
  count=sum(r['exact_registration'] for r in values);label=html.escape(speaker).replace('|','\\|');table.append(f'| {label} | {len(values)} | {count} | {100*count/len(values):.2f}% | {len(values)-count} | {sum(r["source"] in new for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
 save(SOURCE/'NPC_ALL_SPEAKERS_0.15.90_RU.md','\n'.join(table)+'\n')
 report=['# Большая партия NPC — 0.15.90','','`██████████████░░░░░░░░░░░░░░░░` **46,14%** точных ключей публичной базы; полоса округлена.','','| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи | 6 784 · 41,76% | 7 495 · 46,14% |','| Без подтверждённого точного ключа | 9 460 | 8 749 · 53,86% |','| Строк в зафиксированной базе | 16 244 | 16 244 |','','Это не процент полностью готовой игры. База включает архивные, структурные и редакторские строки; закрытые серверные скрипты недоступны. Отсутствие точного ключа не исключает динамического перевода.','','| Говорящий | Новых реплик |','| --- | ---: |']
 report += [f'| {s} | {c} |' for s,c in SPEAKERS.items()]
 report += ['| Всего | 711 |','','Проверены 740 выбранных NPC-строк / 331 полный исходный блок / 32 страницы. 711 новых точных реплик, 13 прежних ключей, 14 структурных кандидатов и 2 нейтральных многоточия. 128 точных пар переиспользованы из ранее вычитанных переводов; происхождение сохранено.','','30 новых записей таблицы ответов / 61 место кнопки / 31 опубликованное оформление. Из них 30 подписей изменяются (27 разных текстов), 31 цифровая/символьная сохраняется. Одна группа НЕТ/ДА работала отдельным обработчиком раньше; её не выдаём за ранее отсутствовавший перевод. Четыре многострочных заголовка с отдельными кнопками остаются открытыми.','','## Качество','','[Контекстная вычитка](NPC_0.15.90_REVIEW_NOTES.md).',f'На одном окончательном JAR прошли 155 свежих групп и 15 тестов извлечения. Новые реплики: {number(assertions["TestNpc90"])} проверок; ответы: {number(assertions["TestNpcChoices90"])}. Проверены опубликованные цвета, отдельные радужные буквы, свежие команды/подсказки, стили, фрагментация, числа, время, счётчики, переопределения и защита пользовательского/подписанного чата.',f'API: {revision["checked_at"]}; все 32 ревизии совпали с зафиксированными. Явных разделов Historical/Removed на выбранных страницах нет. Это не подтверждает живую доступность каждой ветки.','','| Те же 11 247 строк: срабатывание, не оценка качества | 89 | 90 |','| --- | ---: | ---: |','| Обычный текст | 6 746 | 7 456 |','| Опубликованные цвета | 6 674 | 7 384 |','','В общем строгом поднаборе проверены 710 новых ключей в обоих режимах; вне них результаты неизменны. Рекламная реплика Мирии #AD! — отдельно вычитанное буквальное исключение, подтверждённое целевым тестом, а не расширением общего фильтра. 72 старых цветных кандидата не объявлены исправленными.','','У Гео публикация разбила фразу о наказании на фрагменты: переведён имеющийся точный фрагмент по полному контексту. Объединённое серверное сообщение не заявлено покрытым.','','## Нагрузка','','Ресурс сливается с прежней точной картой один раз при запуске. Новых провайдеров, сетевого перевода и покадровых обходов нет. Изменены регистрация ресурса, таблица ответов и верхний предел существующего распознавателя с 5 до 11 кнопок; обязательная проверка полного точного списка сохранена. Неизвестные наборы и 12 кнопок отвергаются. HUD, F3 и все остальные классы вне двух прежних переводчиков побайтово прежние. Исполняемый код четырёх внутренних классов совпал; различия — отладочные номера строк. Старые текстовые ресурсы сохранены.',f'Рост JAR: {number(jar.stat().st_size-baseline.stat().st_size)} байт. Полная выгрузка в мод не включена.','','Четыре отдельные JVM: 89 → 90 → 90 → 89; 27 сценариев, не менее 200 000 прогревочных вызовов и четыре раунда не менее 40 000 измеряемых вызовов на сценарий.','','| Сценарий | Время до → после, мкс/сообщение, два порядка | Изменение | Байты/сообщение до → после |','| --- | ---: | ---: | ---: |']
 for r in benchmark['summary']:
  timings=' / '.join(f'{o["before_ns"]/1000:.2f} → {o["after_ns"]/1000:.2f}' for o in r['orders']);change=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);allocation=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in r['orders']);report.append(f'| {r["mode"]} | {timings} | {change} | {allocation} |')
 equal=[o['wall_change_percent'] for r in benchmark['summary'] if r['equal_useful_output'] for o in r['orders']]
 report+=['',f'25 прежних сценариев с одинаковой полезной работой: от {min(equal):+.1f}% до {max(equal):+.1f}% в этих запусках. Разброс не доказывает ускорения или отсутствия микролагов. Два new_* сценария теперь выполняют перевод вместо пропуска: это разная работа. Все раунды сохранены в BENCHMARK_RAW.json.','','Это измерения обработки входящего чата, не FPS, 0,1% lows или времени кадра в игре. Нулевая нагрузка не обещана. Версия автоматически не установлена, Minecraft не перезапускался.','','## Остаток','','Вся база ещё не переведена. Открыты 8 749 строк без подтверждённого точного ключа, структурные случаи, 72 старых цветных кандидата и прежний широкий аудит ReviewedLoreStyles. Ошибка исходного блока Bingo также остаётся открытой. Ни Abiphone, ни весь NPC-реестр не объявляются полностью готовыми.','','[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.90_RU.md).','',f'SHA-256 JAR: `{fingerprint}`.']
 save(SOURCE/'NPC_PROGRESS_0.15.90_RU.md','\n'.join(report)+'\n')
 files=[p for p in SOURCE.glob('*0.15.90*') if p.is_file()]+authoring+[SOURCE/'translations.json',SOURCE/'coverage.json']
 files += [SOURCE/r['reviewed_target_source_file'] for r in reuse['records']]
 files += [DATA/'npc-expansion90.json',ROOT/'src/main/resources/META-INF/licenses/NPC90-SOURCES.txt',ROOT/'src/main/resources/fabric.mod.json',ROOT/'src/main/resources/data/witherskyru/corrections.tsv',ROOT/'build.gradle']
 files += [ROOT/'src/main/java/ru/wither/skyru'/n for n in ('NpcDialogueTranslator.java','ChatNotificationTranslator.java')]
 files += [ROOT/'tests'/n for n in ('TestNpc90.java','TestNpcChoices90.java','BenchmarkNpc90.java','AuditNpcCorpus77.java','test_npc_corpus77.py')]
 files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==7261' in p.read_text(encoding='utf-8-sig')]
 files += [ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc88.py','prepare_npc90.py','export_npc_corpus77.py','check_npc90.ps1','check_npc90_parallel.ps1','dispatch_npc90.ps1','benchmark_npc90.ps1','check_npc90_bytecode.ps1','finalize_npc90_progress.py')]
 destination.mkdir()
 for path in sorted(set(files)):
  target=destination/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
 checks=destination/'checks';checks.mkdir()
 for name in tests:
  shutil.copy2(a.checks/(name+'-output.txt'),checks)
  if (a.checks/(name+'-stderr.txt')).stat().st_size:shutil.copy2(a.checks/(name+'-stderr.txt'),checks)
 for name in ('CHECK_SCOPE.json','TestNpcCorpusParser90-output.txt'):shutil.copy2(a.checks/name,checks)
 shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json');shutil.copytree(a.bytecode,destination/'bytecode')
 for v in ('0.15.89-dev','0.15.90-dev'):
  target=destination/'dispatch'/v;target.mkdir(parents=True);shutil.copy2(a.audit/v/'DISPATCH_SUMMARY.json',target)
 save(destination/'README_RU.md','# Большая партия 90\n\n711 новых точных NPC-реплик / 30 обозначений говорящего. 30 записей таблицы ответов / 61 место кнопки (одна группа уже переводилась прежним обработчиком).\n\n7 495 / 16 244 точных ключей · 46,14%. Это не процент полностью готовой игры.\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.90_RU.md) · [Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.90_RU.md)\n\nЧастичный пакет исходников и отчётов, не полный проект. JAR выдаётся отдельно; автоматически не установлен.\n')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_14.zip'
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

def load_version(z):return json.loads(z.read('fabric.mod.json'))['version']
if __name__=='__main__':main()
