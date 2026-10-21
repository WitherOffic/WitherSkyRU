"""Publish only one fully validated batch87 JAR. No game installation."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc87 import ROOT,SOURCE,DATA,CORPUS,SPEAKERS
BASE='ED74E94BA8A7CA0173562F099035823C0263046924940789058AA5467E5B6A67'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-17_10')
def load(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def n(value):return f'{value:,}'.replace(',','\u202f')
def lines(path):return [line for line in path.read_text(encoding='utf-8-sig').splitlines() if line and not line.startswith('#')]
def main():
 p=argparse.ArgumentParser()
 for name in ('checks','audit','benchmark','bytecode','destination','jar-directory'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert not destination.exists(),'Never overwrite a delivery';assert a.jar_directory.is_dir()
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.87-dev.jar';baseline=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.86-dev.jar';fingerprint=sha(jar);assert sha(baseline)==BASE
 scope=load(a.checks/'CHECK_SCOPE.json');tests=scope['tests_passed']
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes']
 assert len(tests)==len(set(tests))==146
 assert {'TestNpc87','TestNpcChoices87','TestNpc86','TestRecipeNames86','TestNpcChoices86','TestEditorialQuality','TestHudMixins','TestFrameNoops71','TestJarDictionary014'}<=set(tests)
 for name in tests:assert (a.checks/(name+'-output.txt')).is_file()
 assertions={}
 for name,label,marker in [('TestNpc87','NPC87','673 new authored literals/688 published layouts'),('TestNpcChoices87','NPCChoices87','63 exact ordered groups/67 label slots/all63 published layouts')]:
  output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert marker in output
  assertions[name]=int(re.search(r'PASS: '+label+r' (\d+) assertions',output)[1])
 batch=load(DATA/'npc-expansion87.json')['records'];new={r['source']:r['translation'] for r in batch}
 assert len(new)==len(batch)==673 and Counter(r['speaker'] for r in batch)==SPEAKERS
 authoring=[];authored={}
 for speaker in SPEAKERS:
  path=SOURCE/('NPC87_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv');authoring.append(path)
  for line in lines(path):
   f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new
 evidence=load(SOURCE/'NPC_SOURCES_0.15.87.json')
 assert len(evidence['records'])==707 and len(evidence['source_pages'])==14
 assert len(evidence['pending_variables'])==28 and len(evidence['neutral_unchanged'])==1 and len(evidence['unsafe_excluded'])==5
 assert not evidence['retained_originals'] and not evidence['runtime_retained']
 assert all(r['historical_heading'] for r in evidence['unsafe_excluded'])
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==688
 revision=load(SOURCE/'NPC_0.15.87_PUBLIC_REVISION.json');assert revision['matches_pinned_corpus'] and not revision['differences'] and len(revision['pages'])==14
 assert {r['title']:r['revisions'][0]['revid'] for r in revision['pages']}=={r['title']:r['revision'] for r in evidence['source_pages']}
 choices=[tuple(map(json.loads,line.split('|'))) for line in lines(SOURCE/'NPC_CHOICES_0.15.87_REVIEWED.tsv')]
 choice_evidence=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.87.json')
 assert len(choices)==len(choice_evidence['records'])==63 and sum(len(s) for s,t in choices)==67
 assert [(r['source_labels'],r['target_labels']) for r in choice_evidence['records']]==choices
 assert len({label for s,t in choices for label in s})==65
 assert [(s,t) for ss,tt in choices for s,t in zip(ss,tt) if s==t]==[('...','...')]
 assert len({s for ss,tt in choices for s,t in zip(ss,tt) if s!=t})==64
 assert len(choice_evidence['retained_groups'])==1 and choice_evidence['structural_excluded'] and choice_evidence['historical_excluded']
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choice_evidence['records'])==63
 before=load(a.audit/'0.15.86-dev/DISPATCH_SUMMARY.json');after=load(a.audit/'0.15.87-dev/DISPATCH_SUMMARY.json')
 assert before['runtime_jar_sha256']==BASE and after['runtime_jar_sha256']==fingerprint
 assert before['queue_sha256']==after['queue_sha256']==evidence['queue_sha256']==sha(CORPUS/'translation_queue.jsonl')
 assert before['tested_literal_rows']==after['tested_literal_rows']==11247
 assert (before['flat_changed_not_quality_proof'],before['published_style_changed_not_quality_proof'])==(4648,4576)
 assert (after['flat_changed_not_quality_proof'],after['published_style_changed_not_quality_proof'])==(5321,5249)
 old={r['source']:r for r in rows(a.audit/'0.15.86-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.audit/'0.15.87-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==current.keys()
 for source,row in current.items():
  if source in new:assert row['flat_result']==row['published_style_result']==new[source],source
  else:assert all(row[k]==old[source][k] for k in ('flat_result','published_style_result')),source
 assert len(new.keys()&current.keys())==673
 gaps=[r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
 benchmark=load(a.benchmark/'RAW.json');assert len(benchmark['summary'])==19
 assert [r['version'] for r in benchmark['records']]==['0.15.86-dev','0.15.87-dev','0.15.87-dev','0.15.86-dev']
 for record in benchmark['records']:
  assert record['jar_sha256']==(BASE if record['version']=='0.15.86-dev' else fingerprint)
  assert len([k for k in record['results'] if k!='scope'])==19
  for mode,result in record['results'].items():
   if mode!='scope':assert all(len(result[k])==4 for k in ('rounds_ns','rounds_cpu_ns','rounds_bytes'))
 bytecode=load(a.bytecode/'RESULT.json');assert bytecode['runtime_jar_sha256']==fingerprint and len(bytecode['records'])==3 and all(r['executable_disassembly_identical'] for r in bytecode['records'])
 with zipfile.ZipFile(baseline) as x,zipfile.ZipFile(jar) as y:
  assert y.testzip() is None;xn,yn=set(x.namelist()),set(y.namelist());assert not xn-yn
  changed=sorted(k for k in xn&yn if x.read(k)!=y.read(k));added=sorted(yn-xn)
  assert changed==['META-INF/licenses/NPC86-SOURCES.txt','fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
  assert added==['META-INF/licenses/NPC87-SOURCES.txt','data/witherskyru/npc-expansion87.json'],added
  assert y.read('META-INF/licenses/NPC86-SOURCES.txt')==x.read('META-INF/licenses/NPC86-SOURCES.txt').replace(b'702 Russian',b'699 Russian')
  assert y.read('data/witherskyru/npc-expansion87.json')==(DATA/'npc-expansion87.json').read_bytes()
  for name in xn:
   if name.startswith('data/'):assert x.read(name)==y.read(name),name
 previous_manifest=load(PREVIOUS/'HASHES.json')
 for name in ('translation-source/translations.json','translation-source/coverage.json','src/main/resources/data/witherskyru/corrections.tsv'):
  assert sha(ROOT/name)==previous_manifest[name]
 stats=load(SOURCE/'NPC_PROGRESS_0.15.87.json');assert stats['exact_resource_after']==5359 and stats['new_resource_sha256']==sha(DATA/'npc-expansion87.json')
 stats.update({'runtime_validation':'146_fresh_groups_passed','fresh_test_groups':146,'assertions':assertions,'runtime_jar_sha256':fingerprint,'runtime_changed_files':changed,'runtime_added_files':added,'render_hud_f3_classes_byte_identical':True,'old_text_resources_byte_identical':True,'inner_record_bytecode':bytecode,'outside_selected_dispatch_text_unchanged':True,'compatible_published_npc_layouts_checked':688,'additional_choice_groups':63,'additional_choice_slots':67,'distinct_translated_choice_labels':64,'neutral_choice_labels_retained':1,'remaining_original_style_gap':72,'dispatch_before':before,'dispatch_after':after,'benchmark_summary':benchmark['summary'],'current_revision_check':'all14 full-page revisions match','parser_tests_passed':13})
 write(SOURCE/'NPC_PROGRESS_0.15.87.json',stats);write(SOURCE/'NPC_STYLE_REMAINING_0.15.87.json',{'scope':'72 older candidates unchanged by87,not certified fixed','records':gaps})
 exact=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
 worklist=rows(SOURCE/'NPC_WORKLIST_0.15.86.jsonl');queue=rows(CORPUS/'translation_queue.jsonl');assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue]
 pending={r['source'] for r in evidence['pending_variables']};neutral={r['source'] for r in evidence['neutral_unchanged']};excluded={r['source'] for r in evidence['unsafe_excluded']};choice_sources={o['source'] for r in choice_evidence['records'] for o in r['all_occurrences']};by_speaker=defaultdict(list)
 for r in worklist:
  source=r['source'];r['exact_registration']=bool(exact.get(source));r['registered_targets_not_automatically_certified']=sorted(exact.get(source,set()))
  if source in new:r['review_status']='authored_and_fixture_checked87';r['all_published_npc_layouts_checked87']=True
  if source in pending:r['structural_review_pending87']=True;r['template_coverage_certified87']=False
  if source in neutral:r['review_status']='neutral_no_language_translation_needed87'
  if source in excluded:r['review_status']='historical_source_excluded87'
  if source in choice_sources:r['ordered_choice_group_checked87_separate_from_npc_keys']=True
  if source in current:r['runtime_flat_snapshot_not_quality_proof']=current[source]['flat_result'];r['runtime_colored_snapshot_not_quality_proof']=current[source]['published_style_result']
  by_speaker[r['speaker'] or '(без указанного говорящего)'].append(r)
 assert sum(r['exact_registration'] for r in worklist)==5359 and sum(r.get('review_status')=='authored_and_fixture_checked87' for r in worklist)==673
 (SOURCE/'NPC_WORKLIST_0.15.87.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist),encoding='utf-8')
 gap_keys={r['source'] for r in gaps};table=['# Вся очередь NPC — 0.15.87','','Публичная выгрузка, не полный реестр игры. Точные ключи не сертифицируют все динамические ветки, качество всего перевода или действующий статус NPC. Кнопки учитываются отдельно.','','| Говорящий | Всего | Точных ключей | Доля | Новых в 87 | Старых цветных кандидатов | Структурных признаков |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
 for speaker,values in sorted(by_speaker.items()):
  count=sum(r['exact_registration'] for r in values);label=html.escape(speaker).replace('|','\\|')
  table.append(f'| {label} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in gap_keys for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
 (SOURCE/'NPC_ALL_SPEAKERS_0.15.87_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
 report=['# Большая партия NPC — 0.15.87','','`██████████░░░░░░░░░░░░░░░░░░░░` **32,99%** точных ключей публичной базы (полоса округлена).','','| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи переводов | 4 686 · 28,85% | 5 359 · 32,99% |','| Без подтверждённого точного ключа | 11 558 | 10 885 · 67,01% |','| Всего уникальных строк базы | 16 244 | 16 244 |','','Это не процент полностью проверенного перевода всей игры. Отсутствие точного ключа не исключает динамического перевода. База содержит архивные, структурные и редакторские строки, а закрытые серверные скрипты недоступны.','','| Говорящий | Новых реплик |','| --- | ---: |']
 report += [f'| {s} | {c} |' for s,c in SPEAKERS.items()]
 report += ['| Всего | 673 |','','Дополнительно: 63 группы выбора, 67 мест для кнопок, 64 разные переведённые подписи. В одном месте «...» сохранено, переводится заголовок. Кнопки не увеличивают счётчик точных NPC-ключей. В выбранных 707 строках отдельно остаются 28 структурных, одна нейтральная и пять явно удалённых.','','## Качество и актуальность','','[Подробная контекстная вычитка](NPC_0.15.87_REVIEW_NOTES.md). Прочитаны временные линии Амога, единая цепочка охоты и слияния, история Звёздорождённых, Временная камера, тренировки Уго и побочные задания. Сохранены направления, количества, команды и точный серверный ввод Help Wizardman! с русским пояснением.','','Последняя API-проверка: '+revision['checked_client_date']+'. Все 14 ревизий исходных страниц совпали с сохранёнными полными текстами. Это не живое прохождение сервера.','',f'Все 146 групп проверок заново прошли на одном окончательном JAR. Новая партия: {n(assertions["TestNpc87"])} проверок реплик и {n(assertions["TestNpcChoices87"])} проверок выбора. Проверены все 688 опубликованных строк оформления и 63 схемы выбора, свежие цвета/действия/подсказки, числа, фрагментация, счётчики, время, переопределения и защита подписанного/пользовательского/клиентского чата. Отдельно прошли 13 тестов парсера.','','| Те же 11 247 строк: техническое срабатывание, не оценка качества | 86 | 87 |','| --- | ---: | ---: |','| Обычный текст | 4 648 | 5 321 |','| Опубликованные цвета | 4 576 | 5 249 |','','Все 673 новые строки сработали в обоих режимах. Вне них результаты побуквенно прежние. 72 старых цветных кандидата остаются открытыми.','','## Нагрузка','','Данные загружаются в прежние точные карты при запуске. Новых провайдеров, регулярных выражений, сетевого перевода и кадровых проходов нет. HUD/F3 и классы вне двух прежних переводчиков побайтово прежние; три внутренних record-класса дополнительно совпали по исполняемому коду, изменились отладочные номера строк. Старые текстовые таблицы не изменены. В атрибуции прежней партии исправлена только цифра 702 → 699. Полная выгруженная база в мод не включена; размер JAR вырос на '+n(jar.stat().st_size-baseline.stat().st_size)+' байт.','','Четыре отдельные JVM в порядке 86 → 87 → 87 → 86. В каждом из 19 сценариев — не менее 200 000 прогревочных вызовов, затем четыре раунда не менее чем по 40 000 измеряемых вызовов; все результаты сохранены.','','| Входящий чат | Изменение времени, два порядка | Байты/сообщение до → после |','| --- | ---: | ---: |']
 labels={'unknown':'Неизвестные реплики','existing_dynamic':'Прежние динамические реплики','existing_exact':'Прежние точные реплики','existing_style':'Прежние цветные реплики','existing_tal':'Тал Кер','existing_wizard':'Волшебник','existing_tyashoi':'Алхимик Тьяшой','existing_elle':'Элль','existing_kat_elizabeth':'Кэт и Элизабет','existing_end_guide':'Одинокий искатель приключений','existing_juliette_style':'Джульетта: цвета','existing_choices':'Прежние ответы','existing_large_batch84':'Реплики партии 84','existing_choices84':'Ответы партии 84','existing_large_batch85':'Реплики партии 85','existing_large_batch86':'Реплики партии 86','existing_choices86':'Ответы партии 86','new_large_batch87':'Новые реплики 87: дополнительный перевод','new_choices87':'Новые ответы 87: дополнительный перевод'}
 for row in benchmark['summary']:
  times=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in row['orders']);memory=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in row['orders'])
  report.append(f'| {labels[row["mode"]]} | {times} | {memory} |')
 equal=[o['wall_change_percent'] for r in benchmark['summary'] if r['equal_useful_output'] for o in r['orders']]
 report+=['',f'Прежние сценарии: от {min(equal):+.1f}% до {max(equal):+.1f}% в этих запусках. Разброс не доказывает ускорения или отсутствия микролагов. Отрицательный процент — меньшее время, положительный — большее. new_large_batch87 и new_choices87 выполняют новый полезный перевод вместо прежнего пропуска, это не равная работа.','','| Новые случаи | Время 87, мкс/сообщение, два порядка | Изменение против прежнего пропуска, мкс |','| --- | ---: | ---: |']
 for row in benchmark['summary']:
  if not row['equal_useful_output']:
   absolute=' / '.join(f'{o["after_ns"]/1000:.2f}' for o in row['orders']);extra=' / '.join(f'{(o["after_ns"]-o["before_ns"])/1000:+.2f}' for o in row['orders']);report.append(f'| {labels[row["mode"]]} | {absolute} | {extra} |')
 report+=['','Это обработка входящего сообщения, не повторение базы каждый кадр. Микрозамеры и грубый CPU-счётчик Windows нельзя пересчитывать в FPS. Все раунды и выделения памяти сохранены в BENCHMARK_RAW.json. Ни FPS, ни редкие просадки/0,1% lows в этой сборке не измерены. Нулевая нагрузка не обещана.','','## Незавершённое','','Вся база ещё не переведена. Структурные случаи, 72 старых цветных кандидата и широкий прежний аудит ReviewedLoreStyles остаются открытыми. Эта версия в игре не проверялась и автоматически не установлена. Старые JAR и пакеты сохранены.','','[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.87_RU.md).','',f'SHA-256 JAR: `{fingerprint}`.']
 (SOURCE/'NPC_PROGRESS_0.15.87_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
 destination.mkdir(parents=True)
 files=[p for p in SOURCE.glob('*0.15.87*') if p.is_file()]+authoring+[DATA/'npc-expansion87.json',ROOT/'src/main/resources/META-INF/licenses/NPC87-SOURCES.txt',ROOT/'src/main/resources/META-INF/licenses/NPC86-SOURCES.txt',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
 files += [ROOT/'src/main/java/ru/wither/skyru'/name for name in ('NpcDialogueTranslator.java','ChatNotificationTranslator.java')]
 files += [ROOT/'tests'/name for name in ('TestNpc87.java','TestNpcChoices87.java','BenchmarkNpc87.java','AuditNpcCorpus77.java')]
 files += [ROOT/'tools'/name for name in ('prepare_npc78.py','prepare_npc87.py','check_npc87.ps1','dispatch_npc87.ps1','benchmark_npc87.ps1','check_npc87_bytecode.ps1','finalize_npc87_progress.py')]
 files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==5121' in p.read_text(encoding='utf-8-sig')]
 for path in sorted(set(files)):
  target=destination/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
 checks=destination/'checks';checks.mkdir()
 for name in tests:shutil.copy2(a.checks/(name+'-output.txt'),checks)
 shutil.copy2(a.checks/'CHECK_SCOPE.json',checks);shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json');shutil.copytree(a.bytecode,destination/'bytecode')
 for version in ('0.15.86-dev','0.15.87-dev'):
  target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(a.audit/version/'DISPATCH_SUMMARY.json',target)
 (destination/'README_RU.md').write_text('# Большая партия 87\n\n673 новые реплики,13 обозначений говорящего;63 группы выбора/67 мест для кнопок.146 свежих групп проверок.5 359 /16 244 точных ключей (32,99%).\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.87_RU.md)\n\n[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.87_RU.md)\n\nЧастичный пакет исходников и отчётов,не полный проект. JAR выдаётся отдельно и не устанавливается автоматически.\n',encoding='utf-8')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_11.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for name in sorted(hashes):z.write(destination/name,name)
  z.write(destination/'HASHES.json','HASHES.json')
 with zipfile.ZipFile(archive) as z:
  assert z.testzip() is None
  for name,expected in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==expected
  entries=len(z.namelist())
 delivered=a.jar_directory/jar.name
 if delivered.exists():assert sha(delivered)==fingerprint,'Never replace a different published JAR'
 else:shutil.copy2(jar,delivered)
 assert sha(delivered)==fingerprint
 write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered),'jar_sha256':fingerprint,'installed_game_modified':False})
 print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))
if __name__=='__main__':main()
