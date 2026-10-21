"""Publish checked translation batch without touching the installed game or older deliveries."""
import argparse,json,re,html,shutil,zipfile,hashlib
from collections import defaultdict
from pathlib import Path
from prepare_npc98 import ROOT,SOURCE,DATA,CORPUS
from prepare_npc78 import rows,sha,write

DESKTOP=Path('C:/Users/Wither/Desktop/WitherSkyRU')
BASE='97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
 p=argparse.ArgumentParser()
 for k in ('checks','audit','dispatch','benchmark','dungeon','focused','destination'):p.add_argument('--'+k,type=Path,required=True)
 a=p.parse_args();destination=a.destination.resolve();assert destination.parent==DESKTOP and not destination.exists()
 jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.98-dev.jar';fingerprint=sha(jar)
 assert sha(ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.96-dev.jar')==BASE
 scope=load(a.checks/'CHECK_SCOPE.json');old=load(ROOT/'build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55/CHECK_SCOPE.json')
 assert len(scope['tests_passed'])==len(set(scope['tests_passed']))==165 and set(scope['tests_passed'])-set(old['tests_passed'])=={'TestNpc98'}
 assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and not scope['reused_successes']
 for name in scope['tests_passed']:assert (a.checks/(name+'-output.txt')).is_file()
 assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser98-output.txt').read_text(encoding='utf-8-sig')
 assertions=int(re.search(r'PASS: NPC98 (\d+) assertions',(a.checks/'TestNpc98-output.txt').read_text())[1])
 proof=load(a.audit/'RESULT.json');assert proof['runtime_jar_sha256']==fingerprint and proof['baseline_jar_sha256']==BASE and proof['new_exact_outputs_flat_and_published']==336 and proof['old_output_rows_unchanged']==10911
 bench=load(a.benchmark/'RAW.json');dungeon=load(a.dungeon/'RAW.json');assert bench['runtime_jar_sha256']==dungeon['runtime_jar_sha256']==fingerprint
 for family in bench['families'].values():
  assert [r['version'] for r in family['records']]==['0.15.96-dev','0.15.98-dev','0.15.98-dev','0.15.96-dev']
  for r in family['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.96-dev' else fingerprint)
 assert [r['version'] for r in dungeon['records']]==['0.15.96-dev','0.15.98-dev','0.15.98-dev','0.15.96-dev']
 for r in dungeon['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.96-dev' else fingerprint)
 focused=load(a.focused/'RAW.json');assert focused['runtime_jar_sha256']==fingerprint and len(focused['summary'])==6 and all(r['equal_useful_output'] for r in focused['summary'])
 assert [r['version'] for r in focused['records']]==['0.15.96-dev','0.15.98-dev','0.15.98-dev','0.15.96-dev']
 for r in focused['records']:assert r['jar_sha256']==(BASE if r['version']=='0.15.96-dev' else fingerprint)
 batch=load(DATA/'dungeon-expansion98.json')['records'];new={r['source']:r['translation'] for r in batch};assert len(new)==336
 known=defaultdict(set)
 for path in DATA.glob('*.json'):
  for r in load(path).get('records',[]):
   if r.get('source') and r.get('translation'):known[r['source']].add(r['translation'])
 q=rows(CORPUS/'translation_queue.jsonl');assert len(q)==16244 and sum(r['source'] in known for r in q)==9612
 current={r['source']:r for r in rows(a.dispatch/'0.15.98-dev/DISPATCH_ROWS.jsonl')};evidence=load(SOURCE/'NPC_SOURCES_0.15.98.json')
 assert [len(evidence[k]) for k in ('records','blocks','source_pages','pending_variables','extraction_pending','neutral_unchanged','retained_originals')]==[365,181,11,13,7,1,8]
 previous=rows(SOURCE/'NPC_WORKLIST_0.15.93.jsonl');assert [r['source'] for r in previous]==[r['source'] for r in q]
 by_speaker=defaultdict(list);pending={r['source'] for r in evidence['pending_variables']};extraction={r['source'] for r in evidence['extraction_pending']}
 for r in previous:
  s=r['source'];r['exact_registration']=s in known;r['registered_targets_not_automatically_certified']=sorted(known.get(s,()))
  if s in new:r['review_status']='authored_and_fixture_checked98'
  if s in pending:r['review_status']='structured_dynamic_review_pending98'
  if s in extraction:r['review_status']='editorial_placeholder_or_extraction_pending98'
  if s in current:r.update(runtime_flat_snapshot_not_quality_proof=current[s]['flat_result'],runtime_colored_snapshot_not_quality_proof=current[s]['published_style_result'])
  by_speaker[r['speaker'] or '(без говорящего)'].append(r)
 (SOURCE/'NPC_WORKLIST_0.15.98.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in previous),encoding='utf-8')
 table=['# Все говорящие — 0.15.98','','Точные ключи публичной очереди, не процент готовности всей игры. Имена/теги могут оставаться исходными.','','| Говорящий | Строк | Точных ключей | Доля | Без ключа | Новых в 98 |','| --- | ---: | ---: | ---: | ---: | ---: |']
 for name,rs in sorted(by_speaker.items()):
  n=sum(r['exact_registration'] for r in rs);table.append(f'| {html.escape(name)} | {len(rs)} | {n} | {100*n/len(rs):.2f}% | {len(rs)-n} | {sum(r["source"] in new for r in rs)} |')
 (SOURCE/'NPC_ALL_SPEAKERS_0.15.98_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
 stats=load(SOURCE/'NPC_PROGRESS_0.15.98.json');stats.update(runtime_validation='165_fresh_groups_and15_parser_tests_passed',fresh_test_groups=165,parser_tests_passed=15,new_fixture_assertions=assertions,runtime_jar_sha256=fingerprint,baseline_jar_sha256=BASE,dispatch_tested_rows=11247,dispatch_new_exact_results=336,unchanged_other_dispatch_rows=10911,remaining_previous_style_gaps=72,receipt_render_code_unchanged=True,live_fps_measured=False,source_review_notes='NPC_0.15.98_REVIEW_NOTES.md')
 write(SOURCE/'NPC_PROGRESS_0.15.98.json',stats)
 report=['# Перевод Катакомб — 0.15.98','','`██████████████████░░░░░░░░░░░░` **59,17%** точных ключей публичной очереди. Полоса округлена.','','| Учёт | До | После |','| --- | ---: | ---: |','| Точных ключей | 9 276 · 57,10% | 9 612 · 59,17% |','| Без подтверждённого точного ключа | 6 968 | 6 632 · 40,83% |','| Всего строк | 16 244 | 16 244 |','','Это не процент полностью готовой игры. Есть структурные, нейтральные, исторические и редакторские строки; закрытые серверные скрипты не выгружены. Динамический перевод может работать и без точного ключа.','','336 новых реплик / 11 боссов: Bonzo, Goldor, Maxor, Necron, Sadan, Scarf, Storm, The Professor, The Watcher, The Wither King, Thorn. Прежние восемь ключей выбранной группы не переписаны. Имена в префиксах и теги сохранены; это не исправление всех надписей над сущностями.','','[Контекстная вычитка и ограничения](NPC_0.15.98_REVIEW_NOTES.md) · [Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.98_RU.md).','','## Проверки','',f'На одном окончательном JAR: 165 свежих групп + 15 тестов извлечения. Новый набор: {assertions:,} проверок. Все 336 новых ключей дают ожидаемый русский результат в обычном и опубликованном цветном оформлении. Остальные 10 911 проверяемых строк сохранили результат; 72 прежних цветных пробела не объявлены исправленными.','','Очередь и знаменатель не изменены. 11 страниц сверены с текущим публичным API: 9 совпали; у Bonzo и The Watcher изменились только служебные категории вне секции Dialogue. Полные диалоги и условия совпадают.','','В выбранных 365 строках также остаются 13 структурных и 7 редакторских/оборванных кандидатов; одна нейтральная строка не засчитана переводом.','','## Производительность','','Основа — 0.15.96. Отклонённые эксперименты 0.15.97 не включены. Новые данные объединяются с существующим словарём подземелий один раз при запуске. Новый проход по сообщениям или кадрам не добавлен. Методы приёма и отрисовки подтверждены неизменными; все остальные классы, кроме служебных метаданных вложенного класса, побайтно совпадают с 96.','','Ниже — изолированные замеры на том же JAR, не FPS, время кадра или 0,1% lows. Каждый блок: четыре отдельные JVM в порядке 96 → 98 → 98 → 96; показаны оба порядка, не только удобный результат. Увеличенный словарь требует дополнительной памяти; нулевая нагрузка не обещается.']
 for name,family in bench['families'].items():
  report+=['',f'### {name}','','| Сценарий | Время до → после, мкс, два порядка | Изменение | Байты до → после |','| --- | ---: | ---: | ---: |']
  for r in family['summary']:
   times=' / '.join(f'{o["before_ns"]/1000:.3f} → {o["after_ns"]/1000:.3f}' for o in r['orders']);changes=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);alloc=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in r['orders']);report.append(f'| {r["mode"]} | {times} | {changes} | {alloc} |')
 report+=['','### Прежние и новые реплики боссов','','| Сценарий | Одинаковая полезная работа | До → после, мкс | Изменение |','| --- | --- | ---: | ---: |']
 for r in dungeon['summary']:
  times=' / '.join(f'{o["before_ns"]/1000:.3f} → {o["after_ns"]/1000:.3f}' for o in r['orders']);changes=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);report.append(f'| {r["mode"]} | {"Да" if r["equal_useful_output"] else "Нет: появился перевод"} | {times} | {changes} |')
 report+=['','### Длинный повтор подозрительных прежних сценариев','','Первичный разброс сохранён выше. Два сценария ответов existing_choices87 и existing_choices88 в первом опыте были медленнее в обоих порядках. Повтор: ещё четыре JVM, шесть одинаковых по полезной работе сценариев, 300 000 прогревочных и четыре раунда не менее 200 000 измеряемых вызовов. Это не замена первого результата и не измерение игры.','','| Сценарий | До → после, мкс | Изменение | Байты до → после |','| --- | ---: | ---: | ---: |']
 for r in focused['summary']:
  times=' / '.join(f'{o["before_ns"]/1000:.3f} → {o["after_ns"]/1000:.3f}' for o in r['orders']);changes=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);alloc=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in r['orders']);report.append(f'| {r["mode"]} | {times} | {changes} | {alloc} |')
 positive=[o['after_ns']-o['before_ns'] for r in focused['summary'] for o in r['orders'] if o['after_ns']>o['before_ns']]
 report+=['',f'Максимальное положительное отклонение длинного повтора: {max(positive,default=0)/1000:.3f} мкс на сообщение. Любое положительное отклонение не скрыто и не объявляется нулевой нагрузкой.']
 report+=['','В сценарии new_boss_color_unequal_work версия 96 оставляет английский, а 98 переводит. Его проценты нельзя интерпретировать как ускорение/замедление одинаковой работы. Исходные раунды всех измерений сохранены. В игре плавность этой версии ещё не проверена.','','Автоматической установки, отключения других модов и перезапуска игры не было. По уточнению пользователя сейчас используется 0.15.96 в той же папке Skypixel; старая 0.15.92 отключена.','','## Остаток','','6 632 строки без подтверждённого точного ключа. Продолжается перевод всей базы, включая переменные, многострочные меню, Абифон и другие NPC. Ни эта партия, ни ключевой процент не означают завершения всех персонажей.','',f'SHA-256 JAR: `{fingerprint}`.']
 report_path=SOURCE/'NPC_PROGRESS_0.15.98_RU.md';report_path.write_text('\n'.join(report)+'\n',encoding='utf-8')
 destination.mkdir()
 files=[p for p in SOURCE.glob('*0.15.98*') if p.is_file()]+[ROOT/'build.gradle',ROOT/'src/main/resources/fabric.mod.json',ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',DATA/'dungeon-expansion98.json',ROOT/'src/main/resources/META-INF/licenses/NPC98-SOURCES.txt']
 files += [ROOT/'tests'/n for n in ('TestNpc98.java','TestDungeonChatResources.java','BenchmarkDungeon98.java','BenchmarkNpc98Focused.java','BenchmarkOptimization96.java','BenchmarkPrepared72.java','BenchmarkDynamicHud96.java','AuditNpcCorpus77.java','test_npc_corpus77.py')]
 files += [ROOT/'tools'/n for n in ('prepare_npc78.py','prepare_npc88.py','prepare_npc92.py','prepare_npc93.py','prepare_npc98.py','review_npc92_revision.py','review_npc98_revision.py','export_npc_corpus77.py','derive_npc98_checks.py','derive_npc98_focused.py','check_npc98.ps1','dispatch_npc98.ps1','audit_npc98.py','benchmark_npc98.ps1','benchmark_dungeon98.ps1','benchmark_npc98_focused.ps1','finalize_npc98.py')]
 for source in files:
  target=destination/source.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
 shutil.copy2(jar,destination/jar.name)
 checks=destination/'checks';checks.mkdir()
 for name in scope['tests_passed']:
  for suffix in ('-output.txt','-stderr.txt'):
   source=a.checks/(name+suffix)
   if source.exists() and source.stat().st_size:shutil.copy2(source,checks)
 for name in ('CHECK_SCOPE.json','TestNpcCorpusParser98-output.txt','compile-arguments.txt'):shutil.copy2(a.checks/name,checks)
 shutil.copytree(a.audit,destination/'bytecode')
 for label,path in [('general',a.benchmark),('dungeon',a.dungeon),('focused',a.focused)]:
  target=destination/'benchmark'/label;target.mkdir(parents=True)
  for source in path.rglob('*'):
   if source.is_file() and 'isolated-' not in str(source.relative_to(path)):
    dest=target/source.relative_to(path);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,dest)
 for version in ('0.15.96-dev','0.15.98-dev'):
  target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(a.dispatch/version/'DISPATCH_SUMMARY.json',target)
 (destination/'README_RU.md').write_text('# Перевод Катакомб — 0.15.98\n\n336 новых реплик / 11 боссов. 9 612 / 16 244 · 59,17% точных ключей публичной очереди, не всей игры.\n\n[Отчёт и измерения](translation-source/NPC_PROGRESS_0.15.98_RU.md) · [Контекстная вычитка](translation-source/NPC_0.15.98_REVIEW_NOTES.md) · [Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.98_RU.md).\n\n165 групп + 15 проверок извлечения. Пакет содержит JAR и частичные исходники/доказательства, не весь проект. Автоматической установки нет.\n',encoding='utf-8')
 hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes)
 archive=destination/'NPC_TRANSLATION_0.15.98.zip'
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
 print(json.dumps(load(destination/'DELIVERY.json'),indent=2));print('Progress9612/16244=59.17%;remaining6632')
if __name__=='__main__':main()
