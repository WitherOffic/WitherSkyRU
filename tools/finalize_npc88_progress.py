"""Publish batch88 only after fresh, same-hash checks. Never install or overwrite a delivery."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import Counter,defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc88 import ROOT,SOURCE,DATA,CORPUS,SPEAKERS,RUNTIME_RETAINED

BASE='A7F6BC590258E6832AA0CEB7B5B52B8985505B0C06E6BBEACA3F34C7A75AC309'
DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
PREVIOUS=DESKTOP/'NPC_TRANSLATION_BATCH_2026-10-17_11'
CANONICAL=DESKTOP/'NPC_TRANSLATION_BATCH_2026-10-17_10'
def load(path):return json.loads(path.read_text(encoding='utf-8-sig'))
def lines(path):return [s for s in path.read_text(encoding='utf-8-sig').splitlines() if s and not s.startswith('#')]
def number(value):return f'{value:,}'.replace(',',' ')
def save_text(path,text):path.write_text(text,encoding='utf-8')

def main():
 p=argparse.ArgumentParser()
 for name in ('checks','audit','benchmark','bytecode','destination','jar-directory'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert not destination.exists(),'Never overwrite a delivery'
 assert a.jar_directory.is_dir()
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.88-dev.jar'
 baseline=DESKTOP/'WitherSkyRU-26.1.2-fabric-0.15.87-dev.jar';assert sha(baseline)==BASE
 fingerprint=sha(jar);queue_hash=sha(CORPUS/'translation_queue.jsonl')
 assert queue_hash=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
 scope=load(a.checks/'CHECK_SCOPE.json');tests=scope['tests_passed']
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes']
 assert scope.get('isolated_parallel_workers')==4
 assert len(tests)==len(set(tests))==151
 assert {'TestNpc88','TestNpcChoices88','TestNpcItems88','TestNpcRawRepair88','TestCropLab88','TestNpc87','TestNpcChoices87','TestEditorialQuality','TestHudMixins','TestFrameNoops71','TestJarDictionary014'}<=set(tests)
 assertions={}
 for name,label,marker in [('TestNpc88','NPC88','691 new authored literals/693 published layouts'),('TestNpcChoices88','NPCChoices88','48 exact ordered groups/54 label slots/46 valid published layouts/two malformed publications rejected'),('TestNpcItems88','NPCItems88','one exact item name'),('TestNpcRawRepair88','NPCRawRepair88','one exact raw-source repair'),('TestCropLab88','CropLab88','two observed variable sentences;all published occurrences')]:
  output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert marker in output,(name,output[-1000:])
  assertions[name]=int(re.search(r'PASS: '+label+r' (\d+) assertions',output)[1])
 for name in tests:assert (a.checks/(name+'-output.txt')).is_file()
 parser_output=(a.checks/'TestNpcCorpusParser88-output.txt').read_text(encoding='utf-8-sig');assert 'Ran 15 tests' in parser_output and '\nOK' in parser_output
 batch=load(DATA/'npc-expansion88.json')['records'];new={r['source']:r['translation'] for r in batch}
 assert len(new)==len(batch)==691 and Counter(r['speaker'] for r in batch)==SPEAKERS
 authoring=[];authored={}
 for speaker in SPEAKERS:
  path=SOURCE/('NPC88_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv');authoring.append(path)
  for line in lines(path):
   f=line.split('|');s=f'[NPC] {f[0]}: {f[1]}';assert s not in authored;authored[s]=f'[NPC] {f[0]}: {f[2]}'
 assert authored==new
 evidence=load(SOURCE/'NPC_SOURCES_0.15.88.json');assert evidence['queue_sha256']==queue_hash
 assert len(evidence['records'])==723 and len(evidence['source_pages'])==19 and len(evidence['blocks'])==305
 assert len(evidence['pending_variables'])==28 and len(evidence['neutral_unchanged'])==1 and len(evidence['unsafe_excluded'])==1
 excluded_row=evidence['unsafe_excluded'][0]
 assert excluded_row['historical_heading'] and 'Removed' in excluded_row['headings'] and not excluded_row['alpha_page'] and not evidence['retained_originals']
 assert {r['source'] for r in evidence['runtime_retained']}==RUNTIME_RETAINED
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in evidence['records'] if r['runtime_registration'])==693
 revision=load(SOURCE/'NPC_0.15.88_PUBLIC_REVISION.json')
 assert revision['matches_pinned_corpus'] and not revision['differences'] and len(revision['pages'])==19
 assert revision['checked_client_date']=='2026-10-18'
 assert {r['title']:r['revisions'][0]['revid'] for r in revision['pages']}=={r['title']:r['revision'] for r in evidence['source_pages']}
 choices=[tuple(map(json.loads,line.split('|'))) for line in lines(SOURCE/'NPC_CHOICES_0.15.88_REVIEWED.tsv')]
 choice_evidence=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.88.json')
 assert len(choices)==len(choice_evidence['records'])==48 and sum(len(s) for s,t in choices)==54
 assert [(r['source_labels'],r['target_labels']) for r in choice_evidence['records']]==choices
 assert len({s for ss,tt in choices for s,t in zip(ss,tt) if s!=t})==54
 assert all(s!=t for ss,tt in choices for s,t in zip(ss,tt))
 assert len(choice_evidence['retained_groups'])==3 and len(choice_evidence['heading_only_excluded'])==1 and not choice_evidence['historical_excluded']
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in choice_evidence['records'])==48
 repair=load(SOURCE/'NPC_RAW_REPAIRS_0.15.88.json');collisions=load(SOURCE/'NPC_EXTRACTION_COLLISIONS_0.15.88.json')
 raw_batch=load(DATA/'npc-raw-repairs88.json')['records'];assert len(repair['records'])==len(raw_batch)==1
 raw=repair['records'][0];assert raw['source']==raw_batch[0]['source'] and raw['translation']==raw_batch[0]['translation']
 assert raw['raw_line'] in raw['block']['payload_raw'] and not raw['counted_as_frozen_corpus_exact_key']
 assert raw['source'] not in {r['source'] for r in rows(CORPUS/'translation_queue.jsonl')}
 assert raw['frozen_queue_source'] in new and new[raw['frozen_queue_source']]==raw['translation']
 assert collisions['changed_blocks']==len(collisions['records'])==4 and repair['queue_sha256']==collisions['queue_sha256']==queue_hash
 crop=load(SOURCE/'NPC_CROP_LAB_DYNAMIC_0.15.88.json');assert crop['matches_pinned_pages'] and crop['queue_sha256']==queue_hash
 assert len(crop['records'])==len(crop['revision_pages'])==len(crop['screenshots'])==2
 assert all(not r['counted_as_new_exact_key'] and '<player>' in r['source_template'] for r in crop['records'])
 for line,record in zip(lines(SOURCE/'NPC88_CROP_LAB_DYNAMIC_REVIEWED.tsv'),crop['records']):
  speaker,source,target=line.split('|');assert record['source_template']==f'[NPC] {speaker}: {source}' and record['target_template']==f'[NPC] {speaker}: {target}'
 valid_choices=[o for r in choice_evidence['records'] for o in r['all_occurrences'] if o['source'].count('Select an option:')==1]
 malformed_choices=[o for r in choice_evidence['records'] for o in r['all_occurrences'] if o['source'].count('Select an option:')==2]
 assert len(valid_choices)==51 and len(malformed_choices)==2
 assert sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences'] if o['source'].count('Select an option:')==1}) for r in choice_evidence['records'])==46
 assert {r['block_id'] for r in malformed_choices}=={'p100454-r789601-o2823','p100454-r789601-o3334'}
 names=dict(line.split('\t') for line in lines(SOURCE/'NPC88_ITEM_NAMES.tsv'))
 assert names=={'mrahcemiT esrevrorriM':'Зеркальный талисман времени'}
 previous_manifest=load(CANONICAL/'HASHES.json')
 for name in ('translation-source/translations.json','translation-source/coverage.json','src/main/resources/data/witherskyru/corrections.tsv'):assert sha(CANONICAL/name)==previous_manifest[name]
 old_canonical=load(CANONICAL/'translation-source/translations.json');current_canonical=load(SOURCE/'translations.json')
 assert {k:v for k,v in old_canonical.items() if k!='translations'}=={k:v for k,v in current_canonical.items() if k!='translations'}
 assert current_canonical['translations']==old_canonical['translations']|names
 assert not names.keys()&old_canonical['translations'].keys()
 old_corrections=(CANONICAL/'src/main/resources/data/witherskyru/corrections.tsv').read_bytes()
 corrections=(DATA/'corrections.tsv').read_bytes();assert corrections.startswith(old_corrections)
 assert corrections[len(old_corrections):].decode('utf-8').strip()=='mrahcemiT esrevrorriM\tЗеркальный талисман времени'
 old_coverage=load(CANONICAL/'translation-source/coverage.json');coverage=load(SOURCE/'coverage.json')
 allowed={'runtime_dictionary_entries','runtime_reference_version','snapshot_coverage_note'}
 assert {k:v for k,v in old_coverage.items() if k not in allowed}=={k:v for k,v in coverage.items() if k not in allowed}
 assert old_coverage['runtime_dictionary_entries']==36065 and coverage['runtime_dictionary_entries']==36066 and coverage['runtime_reference_version']=='0.15.88-dev'
 before=load(a.audit/'0.15.87-dev/DISPATCH_SUMMARY.json');after=load(a.audit/'0.15.88-dev/DISPATCH_SUMMARY.json')
 assert before['runtime_jar_sha256']==BASE and after['runtime_jar_sha256']==fingerprint
 assert before['queue_sha256']==after['queue_sha256']==queue_hash and before['tested_literal_rows']==after['tested_literal_rows']==11247
 assert (before['flat_changed_not_quality_proof'],before['published_style_changed_not_quality_proof'])==(5321,5249)
 assert (after['flat_changed_not_quality_proof'],after['published_style_changed_not_quality_proof'])==(6012,5940)
 old={r['source']:r for r in rows(a.audit/'0.15.87-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.audit/'0.15.88-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==current.keys()
 for source,row in current.items():
  if source in new:assert row['flat_result']==row['published_style_result']==new[source],source
  else:assert all(row[k]==old[source][k] for k in ('flat_result','published_style_result')),source
 assert len(new.keys()&current.keys())==691 and all(s in current and s not in new for s in RUNTIME_RETAINED)
 gaps=[r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(gaps)==72
 benchmark=load(a.benchmark/'RAW.json');assert len(benchmark['summary'])==22
 assert [r['version'] for r in benchmark['records']]==['0.15.87-dev','0.15.88-dev','0.15.88-dev','0.15.87-dev']
 for record in benchmark['records']:
  assert record['jar_sha256']==(BASE if record['version']=='0.15.87-dev' else fingerprint)
  assert len([k for k in record['results'] if k!='scope'])==22
  for mode,result in record['results'].items():
   if mode!='scope':assert all(len(result[k])==4 for k in ('rounds_ns','rounds_cpu_ns','rounds_bytes'))
 bytecode=load(a.bytecode/'RESULT.json');assert bytecode['runtime_jar_sha256']==fingerprint and len(bytecode['records'])==3 and all(r['executable_disassembly_identical'] for r in bytecode['records'])
 with zipfile.ZipFile(baseline) as x,zipfile.ZipFile(jar) as y:
  assert y.testzip() is None;xn,yn=set(x.namelist()),set(y.namelist());assert not xn-yn
  changed=sorted(k for k in xn&yn if x.read(k)!=y.read(k));added=sorted(yn-xn)
  assert changed==['data/witherskyru/corrections.tsv','fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
  assert added==['META-INF/licenses/NPC88-SOURCES.txt','data/witherskyru/npc-expansion88.json','data/witherskyru/npc-raw-repairs88.json'],added
  for name in ('npc-expansion88.json','npc-raw-repairs88.json','corrections.tsv'):assert y.read('data/witherskyru/'+name)==(DATA/name).read_bytes()
 stats=load(SOURCE/'NPC_PROGRESS_0.15.88.json');assert stats['exact_resource_after']==6050 and stats['new_resource_sha256']==sha(DATA/'npc-expansion88.json')
 stats.update({'additional_variable_crop_lab_forms':2,'crop_lab_variable_sentences_not_exact_keys':True,'valid_published_choice_layouts':46,'malformed_published_choice_layouts_declined':2,'isolated_parallel_test_workers':4,'crop_lab_assertions':assertions['TestCropLab88']})
 stats.update({'runtime_validation':'151_fresh_groups_passed','fresh_test_groups':151,'assertions':assertions,'runtime_jar_sha256':fingerprint,'runtime_changed_files':changed,'runtime_added_files':added,'render_hud_f3_classes_byte_identical':True,'old_text_resources_unchanged_except_one_item_correction':True,'raw_source_repairs_separately_counted':1,'new_exact_item_names':1,'inner_record_bytecode':bytecode,'outside_selected_dispatch_text_unchanged':True,'compatible_published_npc_layouts_checked':693,'additional_choice_groups':48,'additional_choice_slots':54,'distinct_translated_choice_labels':54,'remaining_original_style_gap':72,'dispatch_before':before,'dispatch_after':after,'benchmark_summary':benchmark['summary'],'current_revision_check':'all19 full-page revisions match','parser_tests_passed':15,'offline_extraction_collision_blocks':4,'other_collision_blocks_not_certified':3})
 write(SOURCE/'NPC_PROGRESS_0.15.88.json',stats);write(SOURCE/'NPC_STYLE_REMAINING_0.15.88.json',{'scope':'72 older candidates unchanged by88,not certified fixed','records':gaps})
 exact=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
 worklist=rows(SOURCE/'NPC_WORKLIST_0.15.87.jsonl');queue=rows(CORPUS/'translation_queue.jsonl')
 assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue]
 assert sum(r['exact_registration'] for r in worklist)==5359
 pending={r['source'] for r in evidence['pending_variables']};neutral={r['source'] for r in evidence['neutral_unchanged']};excluded={r['source'] for r in evidence['unsafe_excluded']};choice_sources={o['source'] for r in choice_evidence['records'] for o in r['all_occurrences']};by_speaker=defaultdict(list)
 crop_sources={r['source_template'] for r in crop['records']}
 for r in worklist:
  source=r['source'];r['exact_registration']=bool(exact.get(source));r['registered_targets_not_automatically_certified']=sorted(exact.get(source,set()))
  if source in new:r.update(review_status='authored_and_fixture_checked88',all_published_npc_layouts_checked88=True)
  if source in pending:r.update(structural_review_pending88=True,template_coverage_certified88=False)
  if source in neutral:r['review_status']='neutral_no_language_translation_needed88'
  if source in excluded:r['review_status']='removed_source_excluded88'
  if source in RUNTIME_RETAINED:r['existing_dynamic_results_retained88']=True
  if source in crop_sources:r.update(review_status='dynamic_crop_lab_shape_fixture_checked88',dynamic_source_shape_fixture_checked88=True,not_a_new_exact_key88=True)
  if source==raw['frozen_queue_source']:r['known_frozen_extraction_artifact88']=True;r['corrected_raw_source_separate88']=raw['source']
  if source in choice_sources:r['ordered_choice_group_checked88_separate_from_npc_keys']=True
  if source in current:r['runtime_flat_snapshot_not_quality_proof']=current[source]['flat_result'];r['runtime_colored_snapshot_not_quality_proof']=current[source]['published_style_result']
  by_speaker[r['speaker'] or '(без указанного говорящего)'].append(r)
 assert sum(r['exact_registration'] for r in worklist)==6050 and sum(r.get('review_status')=='authored_and_fixture_checked88' for r in worklist)==691
 assert sum(r.get('dynamic_source_shape_fixture_checked88',False) for r in worklist)==2
 save_text(SOURCE/'NPC_WORKLIST_0.15.88.jsonl',''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist))
 gap_keys={r['source'] for r in gaps};table=['# Вся очередь NPC — 0.15.88','','Публичная выгрузка, не полный реестр игры. Точные ключи не сертифицируют все динамические ветки, качество всего перевода или действующий статус NPC. Кнопки и исправленный исходный ключ Рамеро учитываются отдельно.','','| Говорящий | Всего | Точных ключей | Доля | Новых в 88 | Старых цветных кандидатов | Структурных признаков |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
 for speaker,values in sorted(by_speaker.items()):
  count=sum(r['exact_registration'] for r in values);label=html.escape(speaker).replace('|','\\|')
  table.append(f'| {label} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in gap_keys for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
 save_text(SOURCE/'NPC_ALL_SPEAKERS_0.15.88_RU.md','\n'.join(table)+'\n')
 report=['# Большая партия NPC — 0.15.88','','`███████████░░░░░░░░░░░░░░░░░░░` **37,24%** точных ключей публичной базы (полоса округлена).','','| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи переводов | 5 359 · 32,99% | 6 050 · 37,24% |','| Без подтверждённого точного ключа | 10 885 | 10 194 · 62,76% |','| Уникальных строк базы | 16 244 | 16 244 |','','Это не процент полностью проверенного перевода всей игры. Отсутствие точного ключа не исключает динамического перевода. База содержит архивные, структурные и редакторские строки; закрытые серверные скрипты недоступны. Сохранённая база не переписана. Один её ключ Рамеро содержит найденное искажение извлечения; правильная исходная строка проверена отдельно и не увеличивает знаменатель или счётчик 6 050.','','| Говорящий | Новых реплик |','| --- | ---: |']
 report += [f'| {s} | {c} |' for s,c in SPEAKERS.items()]
 report += ['| Всего | 691 |','','Дополнительно: 48 упорядоченных групп выбора, 54 места/разные переведённые подписи кнопок, одно название предмета и один правильный исходный ключ Рамеро. Они не увеличивают счётчик точных NPC-ключей. В выбранных 723 строках остаются 28 структурных, одна нейтральная, одна удалённая реплика; две реплики Сириуса уже переводились динамически и не добавлены повторно. У Убика отдельно оставлен опубликованный заголовок выбора без самих кнопок — их порядок не выдуман.','','## Качество и актуальность','','[Контекстная вычитка](NPC_0.15.88_REVIEW_NOTES.md). Проверены сюжетные линии Элизы и Убика, Рамеро и Джолиет, задания фракций, испытания костра, художник, аукцион и события. Архивный Secret Bingo не объявлен действующим событием.','',f'API-проверка: {revision["checked_at"]}. Все 19 ревизий полных исходных страниц совпали с сохранёнными. Это проверка публикаций, не живое прохождение сервера.','',f'На одном окончательном JAR заново пройдена 151 группа проверок. Реплики: {number(assertions["TestNpc88"])} проверок; ответы: {number(assertions["TestNpcChoices88"])}; предмет: {number(assertions["TestNpcItems88"])}; правильный исходный ключ: {number(assertions["TestNpcRawRepair88"])}. Все 693 опубликованных оформления реплик, 46 чистых опубликованных схем выбора и 48 нормальных групп выбора проверены со свежими цветами, действиями, подсказками, фрагментацией, числами, временем, счётчиками, переопределениями и защитой пользовательского/подписанного чата. Два ошибочных опубликованных варианта с повторным заголовком выбора намеренно отклонены. Две переменные реплики Бет и Джейка: {number(assertions["TestCropLab88"])} проверок; имена и значки головы сохраняются, точный счётчик базы не увеличивается. Обе исходные страницы дополнительно сверены с API. Отдельно прошли 15 тестов извлечения.','','| Те же 11 247 строк: техническое срабатывание, не оценка качества | 87 | 88 |','| --- | ---: | ---: |','| Обычный текст | 5 321 | 6 012 |','| Опубликованные цвета | 5 249 | 5 940 |','','Все 691 новая строка сработала в обоих режимах. Вне них результаты прежние. 72 старых цветных кандидата остаются открытыми.','','## Нагрузка','','Новые данные объединяются в прежние точные карты при запуске. Нет новых провайдеров, кадровых проходов или сетевого перевода. Для двух новых сообщений Бет и Джейка добавлены два заранее скомпилированных узких шаблона: проверяются только при получении сообщения после проверки префикса говорящего. HUD/F3 и классы вне двух прежних переводчиков побайтово прежние. У трёх внутренних record-классов исполняемый код совпал; сдвинулись отладочные номера строк. Единственная правка старых текстовых таблиц — новое название предмета. Исправление извлечения работает только при подготовке базы, не в игре. Полная выгрузка в мод не включена.',f'Размер JAR вырос на {number(jar.stat().st_size-baseline.stat().st_size)} байт.','','Четыре отдельные JVM в порядке 87 → 88 → 88 → 87. В каждом из 22 сценариев — не менее 200 000 прогревочных вызовов, затем четыре раунда не менее чем по 40 000 измеряемых вызовов.','','| Входящий чат | Изменение времени, два порядка | Байты/сообщение до → после |','| --- | ---: | ---: |']
 labels={'unknown':'Неизвестные реплики','existing_dynamic':'Прежние переменные реплики','existing_exact':'Прежние точные реплики','existing_style':'Прежние цветные реплики','existing_tal':'Тал Кер','existing_wizard':'Волшебник','existing_tyashoi':'Алхимик Тьяшой','existing_elle':'Элль','existing_kat_elizabeth':'Кэт и Элизабет','existing_end_guide':'Одинокий искатель приключений','existing_juliette_style':'Джульетта: цвета','existing_choices':'Прежние ответы','existing_large_batch84':'Реплики партии 84','existing_choices84':'Ответы партии 84','existing_large_batch85':'Реплики партии 85','existing_large_batch86':'Реплики партии 86','existing_choices86':'Ответы партии 86','existing_large_batch87':'Реплики партии 87','existing_choices87':'Ответы партии 87','new_large_batch88':'Новые реплики 88: дополнительный перевод','new_choices88':'Новые ответы 88: дополнительный перевод','new_crop_lab88':'Бет и Джейк: переменное имя'}
 for row in benchmark['summary']:
  times=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in row['orders']);memory=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in row['orders'])
  report.append(f'| {labels[row["mode"]]} | {times} | {memory} |')
 equal=[o['wall_change_percent'] for r in benchmark['summary'] if r['equal_useful_output'] for o in r['orders']]
 report+=['',f'Прежние сценарии: от {min(equal):+.1f}% до {max(equal):+.1f}% в этих запусках. Разброс не доказывает ускорения или отсутствия микролагов. new_large_batch88, new_choices88 и new_crop_lab88 выполняют дополнительный полезный перевод вместо прежнего пропуска, это не равная работа.','','| Новые случаи | Время 88, мкс/сообщение | Дополнительное время против пропуска, мкс |','| --- | ---: | ---: |']
 for row in benchmark['summary']:
  if not row['equal_useful_output']:
   absolute=' / '.join(f'{o["after_ns"]/1000:.2f}' for o in row['orders']);extra=' / '.join(f'{(o["after_ns"]-o["before_ns"])/1000:+.2f}' for o in row['orders']);report.append(f'| {labels[row["mode"]]} | {absolute} | {extra} |')
 report+=['','Это обработка входящего сообщения, а не базы каждый кадр. Микрозамеры и грубый CPU-счётчик Windows нельзя пересчитывать в FPS. Все раунды сохранены в BENCHMARK_RAW.json. FPS, редкие просадки и 0,1% lows этой версии в игре не измерены; нулевая нагрузка не обещана.','','## Незавершённое','','Вся база ещё не переведена. Структурные строки, 72 старых цветных кандидата и прежний широкий аудит ReviewedLoreStyles остаются открытыми. Из четырёх блоков с коллизиями извлечения отдельно исправлен ключ Рамеро; Bingo, Molbert и Udel пока не сертифицированы этим пакетом. Версия автоматически не установлена, игра не перезапускалась. Старые JAR и пакеты сохранены.','','[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.88_RU.md).','',f'SHA-256 JAR: `{fingerprint}`.']
 save_text(SOURCE/'NPC_PROGRESS_0.15.88_RU.md','\n'.join(report)+'\n')
 destination.mkdir(parents=True)
 files=[p for p in SOURCE.glob('*0.15.88*') if p.is_file()]+authoring+[SOURCE/'NPC88_ITEM_NAMES.tsv',SOURCE/'NPC88_RAW_SOURCE_REPAIRS.tsv',SOURCE/'NPC88_CROP_LAB_DYNAMIC_REVIEWED.tsv',SOURCE/'translations.json',SOURCE/'coverage.json']
 files += [DATA/name for name in ('npc-expansion88.json','npc-raw-repairs88.json','corrections.tsv')]
 files += [ROOT/'src/main/resources/META-INF/licenses/NPC88-SOURCES.txt',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
 files += [ROOT/'src/main/java/ru/wither/skyru'/name for name in ('NpcDialogueTranslator.java','ChatNotificationTranslator.java')]
 files += [ROOT/'tests'/name for name in ('TestNpc88.java','TestNpcChoices88.java','TestNpcItems88.java','TestNpcRawRepair88.java','TestCropLab88.java','BenchmarkNpc88.java','AuditNpcCorpus77.java','test_npc_corpus77.py')]
 files += [ROOT/'tools'/name for name in ('prepare_npc78.py','prepare_npc88.py','prepare_npc88_repairs.py','prepare_crop_lab88.py','export_npc_corpus77.py','check_npc88.ps1','check_npc88_parallel.ps1','dispatch_npc88.ps1','benchmark_npc88.ps1','check_npc88_bytecode.ps1','finalize_npc88_progress.py')]
 files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==5813' in p.read_text(encoding='utf-8-sig')]
 for path in sorted(set(files)):
  target=destination/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
 checks=destination/'checks';checks.mkdir()
 for name in tests:shutil.copy2(a.checks/(name+'-output.txt'),checks)
 shutil.copy2(a.checks/'CHECK_SCOPE.json',checks);shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json');shutil.copytree(a.bytecode,destination/'bytecode')
 shutil.copy2(a.checks/'TestNpcCorpusParser88-output.txt',checks)
 for version in ('0.15.87-dev','0.15.88-dev'):
  target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(a.audit/version/'DISPATCH_SUMMARY.json',target)
 save_text(destination/'README_RU.md','# Большая партия 88\n\n691 новая реплика, 16 обозначений говорящего; 48 групп выбора/54 кнопки; одно название предмета; один исправленный исходный ключ и две переменные реплики по новым скриншотам отдельно. 151 свежая группа проверок. 6 050 / 16 244 точных ключей (37,24%).\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.88_RU.md)\n\n[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.88_RU.md)\n\nЧастичный пакет исходников и отчётов, не полный проект. JAR выдаётся отдельно и не устанавливается автоматически.\n')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_12.zip'
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
 assert sha(delivered)==fingerprint and sha(baseline)==BASE and sha(CORPUS/'translation_queue.jsonl')==queue_hash
 write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered),'jar_sha256':fingerprint,'installed_game_modified':False})
 print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))
if __name__=='__main__':main()
