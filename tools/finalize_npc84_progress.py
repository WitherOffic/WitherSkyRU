"""Publish one verified large batch and dev JAR;never install or touch a game instance."""
import argparse,hashlib,html,json,re,shutil,zipfile
from collections import defaultdict
from pathlib import Path
from prepare_npc78 import rows,sha,write
from prepare_npc84 import FILES,SPEAKERS
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
BASE='8AF47CF26C6AA61AC2651A0547CE262D950E269B2D572DCC381AB59CF323BE95'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def n(v):return f'{v:,}'.replace(',','\u202f')
def main():
    parser=argparse.ArgumentParser()
    for name in ('checks','audit','benchmark','bytecode','corpus','destination','jar-directory'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();destination=args.destination.resolve();jar_directory=args.jar_directory.resolve()
    assert not destination.exists(),'Refusing to overwrite an existing delivery';assert jar_directory.is_dir()
    jar=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.84-dev.jar';fingerprint=sha(jar)
    baseline=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.83-dev.jar';assert sha(baseline)==BASE
    scope=load(args.checks/'CHECK_SCOPE.json');tests=scope['tests_passed']
    assert scope['runtime_jar_sha256']==fingerprint and not scope['targeted_only'] and len(tests)==len(set(tests))==140
    assert {'TestNpc84','TestNpcChoices84','TestNpc83','TestNpcStyles83','TestNpcLookup61','TestEditorialQuality','TestStoryActions','TestFrameNoops71','TestHudMixins'}<=set(tests)
    for name in tests:assert (args.checks/(name+'-output.txt')).is_file()
    text=(args.checks/'TestNpc84-output.txt').read_text(encoding='utf-8-sig');assert '624 new authored literals/625 published layouts' in text
    assertions=int(re.search(r'PASS: NPC84 (\d+) assertions',text)[1])
    text=(args.checks/'TestNpcChoices84-output.txt').read_text(encoding='utf-8-sig');assert '11 exact ordered groups/12 labels/all12 published layouts' in text
    choice_assertions=int(re.search(r'PASS: NPCChoices84 (\d+) assertions',text)[1])
    bytecode=load(args.bytecode/'RESULT.json');assert bytecode['runtime_jar_sha256']==fingerprint and len(bytecode['records'])==3 and all(r['executable_disassembly_identical'] for r in bytecode['records'])
    benchmark=load(args.benchmark/'RAW.json');assert [r['version'] for r in benchmark['records']]==['0.15.83-dev','0.15.84-dev','0.15.84-dev','0.15.83-dev']
    for r in benchmark['records']:assert r['jar_sha256']==(fingerprint if r['version']=='0.15.84-dev' else BASE)
    batch=load(DATA/'npc-expansion84.json')['records'];new={r['source']:r['translation'] for r in batch};assert len(new)==len(batch)==624
    authored={}
    for filename in FILES:
        for line in (SOURCE/filename).read_text(encoding='utf-8-sig').splitlines():
            if line and not line.startswith('#'):
                f=line.split('|');source=f'[NPC] {f[0]}: {f[1]}';assert source not in authored;authored[source]=f'[NPC] {f[0]}: {f[2]}'
    assert new==authored
    evidence=load(SOURCE/'NPC_SOURCES_0.15.84.json');assert len(evidence['records'])==649 and len(evidence['pending_variables'])==5 and len(evidence['neutral_unchanged'])==20 and len(evidence['manual_parser_false_positive_cleared'])==1
    drafts={}
    for line in (SOURCE/'NPC_0.15.84_VARIABLE_DRAFTS.tsv').read_text(encoding='utf-8-sig').splitlines():
        if line and not line.startswith('#'):
            source,target,status=line.split('|');assert status=='UNCONFIRMED_TEMPLATE_COVERAGE' and source not in new and source not in drafts
            assert re.findall(r'\[[^]]+\]|<[^>]+>',source)==re.findall(r'\[[^]]+\]|<[^>]+>',target);drafts[source]=target
    assert set(drafts)=={r['source'] for r in evidence['pending_variables']}
    choices=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.84.json')['records'];assert len(choices)==11 and sum(len(r['source_labels']) for r in choices)==12
    authored_choices=[]
    for line in (SOURCE/'NPC_CHOICES_0.15.84_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if line and not line.startswith('#'):authored_choices.append(tuple(map(json.loads,line.split('|'))))
    assert authored_choices==[(r['source_labels'],r['target_labels']) for r in choices]
    choice_sources={o['source'] for r in choices for o in r['all_occurrences']};assert len(choice_sources)==11
    previous=load(args.audit/'0.15.83-dev/DISPATCH_SUMMARY.json');current=load(args.audit/'0.15.84-dev/DISPATCH_SUMMARY.json')
    assert previous['runtime_jar_sha256']==BASE and current['runtime_jar_sha256']==fingerprint
    assert previous['tested_literal_rows']==current['tested_literal_rows']==11247
    assert previous['queue_sha256']==current['queue_sha256']==sha(args.corpus/'translation_queue.jsonl')==evidence['queue_sha256']
    old_dispatch={r['source']:r for r in rows(args.audit/'0.15.83-dev/DISPATCH_ROWS.jsonl')};now_dispatch={r['source']:r for r in rows(args.audit/'0.15.84-dev/DISPATCH_ROWS.jsonl')};assert old_dispatch.keys()==now_dispatch.keys()
    tested=0
    for source,row in now_dispatch.items():
        if source in new:assert row['flat_result']==row['published_style_result']==new[source],source;tested+=1
        else:assert all(row[k]==old_dispatch[source][k] for k in ('flat_result','published_style_result')),source
    assert tested==623,'One source-parser model-range false-positive remains excluded by the deliberately unchanged bulk guard;all624 fixture-checked'
    remaining=[r for r in now_dispatch.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert len(remaining)==72
    expected_changed=['data/witherskyru/corrections.tsv','fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class']
    with zipfile.ZipFile(baseline) as before,zipfile.ZipFile(jar) as after:
        assert after.testzip() is None
        old_names,new_names=set(before.namelist()),set(after.namelist());assert not old_names-new_names
        changed=sorted(k for k in old_names&new_names if before.read(k)!=after.read(k));added=sorted(new_names-old_names)
        assert changed==expected_changed,changed
        assert added==['META-INF/licenses/NPC84-SOURCES.txt','data/witherskyru/npc-expansion84.json'],added
        assert after.read('data/witherskyru/npc-expansion84.json')==(DATA/'npc-expansion84.json').read_bytes()
        old_rows=before.read('data/witherskyru/corrections.tsv').decode('utf-8-sig').splitlines();new_rows=after.read('data/witherskyru/corrections.tsv').decode('utf-8-sig').splitlines()
        assert new_rows==old_rows+['Webbed Fossil\tОкаменевшая перепонка'],'Exactly one intended item correction'
    assert load(SOURCE/'translations.json')['translations']['Webbed Fossil']=='Окаменевшая перепонка'
    stats=load(SOURCE/'NPC_PROGRESS_0.15.84.json');assert stats['exact_resource_after']==3298 and stats['new_resource_sha256']==sha(DATA/'npc-expansion84.json')
    stats.update({'runtime_validation':'140_fresh_groups_passed','fresh_test_groups':140,'npc84_assertions':assertions,'choice_assertions':choice_assertions,'choice_groups':11,'choice_labels':12,'choice_published_layouts':12,
        'runtime_jar_sha256':fingerprint,'runtime_changed_files':changed,'runtime_added_files':added,'render_hud_f3_classes_byte_identical':True,'three_record_executable_bytecodes_identical':True,
        'outside_selected_dispatch_text_unchanged':True,'new_rows_in_conservative_bulk_dispatch':tested,'compatible_published_npc_layouts_checked':625,'remaining_original_style_gap':72,
        'dispatch_before':previous,'dispatch_after':current,'benchmark_summary':benchmark['summary']})
    write(SOURCE/'NPC_PROGRESS_0.15.84.json',stats);write(SOURCE/'NPC_STYLE_REMAINING_0.15.84.json',{'scope':'72 older candidates unchanged by84,not certified fixed','records':remaining})
    exact=defaultdict(set)
    for path in sorted(DATA.glob('*.json')):
        for r in load(path).get('records',[]):
            if r.get('source') and r.get('translation'):exact[r['source']].add(r['translation'])
    worklist=rows(SOURCE/'NPC_WORKLIST_0.15.83.jsonl');queue=rows(args.corpus/'translation_queue.jsonl');assert len(worklist)==len(queue)==16244 and [r['source'] for r in worklist]==[r['source'] for r in queue]
    per_speaker=defaultdict(list);neutral={r['source'] for r in evidence['neutral_unchanged']}
    for r in worklist:
        source=r['source'];r['exact_registration']=bool(exact.get(source));r['registered_targets_not_automatically_certified']=sorted(exact.get(source,set()))
        if source in new:r['review_status']='authored_and_fixture_checked84';r['all_published_npc_layouts_checked84']=True
        if source in drafts:r['structural_draft84']=True;r['template_coverage_certified84']=False
        if source in neutral:r['review_status']='neutral_no_language_translation_needed84'
        if source==evidence['manual_parser_false_positive_cleared'][0]['source']:r['source_parser_false_positive_manually_cleared84']=True
        if source in choice_sources:r['exact_ordered_choice_group_checked84']=True
        if source in now_dispatch:r['runtime_flat_snapshot_not_quality_proof']=now_dispatch[source]['flat_result'];r['runtime_colored_snapshot_not_quality_proof']=now_dispatch[source]['published_style_result']
        per_speaker[r['speaker'] or '(без указанного говорящего)'].append(r)
    assert sum(r['exact_registration'] for r in worklist)==3298
    (SOURCE/'NPC_WORKLIST_0.15.84.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist),encoding='utf-8')
    gaps={r['source'] for r in remaining};table=['# Вся очередь NPC — 0.15.84','','Публичный корпус, не полный реестр действующих NPC. Точные ключи не сертифицируют качество всей игры; динамические варианты и кнопки учитываются отдельно.','',
        '| Обозначение | Всего | Точных ключей | Доля | Новые тексты84 | Цветных кандидатов | Переменных по исходной разметке |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for speaker,values in sorted(per_speaker.items()):
        count=sum(r['exact_registration'] for r in values);safe=html.escape(speaker).replace('|','\\|')
        table.append(f'| {safe} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in gaps for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.84_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report=['# Большая партия NPC — 0.15.84','','`█████░░░░░░░░░░░░░░░░░░░░` 20,30% точных ключей публичного корпуса (полоса округлена).','',
        '| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи переводов | 2 674 · 16,46% | 3 298 · 20,30% |','| Без подтверждённого точного ключа | 13 570 | 12 946 · 79,70% |','| Всего в зафиксированной базе | 16 244 | 16 244 |','',
        'Это не процент полностью проверенного перевода всей игры. Отсутствие точного ключа не исключает динамический перевод. В базе есть исторические, нейтральные и редакторские строки; закрытые серверные скрипты недоступны.','',
        '| Обозначение говорящего | Новых реплик |','| --- | ---: |']
    for speaker,count in SPEAKERS.items():report.append(f'| {speaker} | {count} |')
    report+=['| Всего | 624 |','','Royal Resident и No Name — два этапа одного персонажа. Общий охват: 649 источников на семи страницах. 624 переведены; 20 числовых отсчётов и многоточие не требуют перевода; пять шаблонов с подстановками остаются черновиками. Ни нейтральные строки, ни черновики не засчитаны как новые переводы. Один диапазон моделей абифонов вручную проверен как буквальная строка: признак переменной у парсера оказался ложным.','',
        'Дополнительно переведены 12 подписей в 11 точных наборах ответа; проверены 12 опубликованных цветных вариантов. Они учтены отдельно от 624 реплик NPC и не увеличивают счётчик точных JSON-ключей. Команды нажатия сохранены, не выполнялись и не переписывались.','',
        'Исправлено название Webbed Fossil: «Окаменевшая перепонка» вместо «Окаменелость в паутине», по анатомическому контексту пингвина. Название согласовано с репликами доктора Стоуна. Это интерпретация названия, не новая игровая механика. [Источник контекста](https://hypixelskyblock.minecraft.wiki/w/Webbed_Fossil?oldid=849841).','',
        '## Проверки','',
        f'Все 140 групп прошли заново на окончательном JAR. Новые реплики: {n(assertions)} проверок; кнопки: {n(choice_assertions)}. Проверены 625 опубликованных цветовых схем реплик и 12 схем кнопок, свежие цвета, курсив, действия и всплывающие подсказки, числа, время, счётчики, фрагментация, пользовательские переопределения, переключатели и защита подписанного, пользовательского и клиентского чата. Название предмета проверено через обработчик подсказки и режим Shift. Это автоматические проверки, не испытание на закрытом сервере.','',
        '| Массовая техническая сверка тех же 11 247 строк | 83 | 84 |','| --- | ---: | ---: |','| Срабатывание обычного текста | 2 637 | 3 260 |','| Срабатывание опубликованных цветов | 2 565 | 3 188 |','',
        'Все 623 новых источника, допущенные прежним консервативным фильтром, сработали. Один диапазон моделей исключён прежним флагом парсера и отдельно проверен среди 625 полных цветовых схем. Вне 624 новых источников NPC оба массовых результата дословно совпали. Прежние 72 цветных кандидата остаются, не объявлены исправленными.','',
        'API 16 октября подтвердил совпадение последних ревизий семи страниц с выгрузкой. Сохранены полные страницы, исходные блоки с условиями и все вхождения. 13 тестов парсера прошли отдельно.','',
        '## Нагрузка','',
        'Новых кадровых проходов, сетевых вызовов в игре или изменений HUD/F3 нет. 624 строки загружаются в прежнюю карту точного поиска; кнопки добавлены в прежнюю статическую таблицу. Все классы отрисовки/HUD/F3 побайтово прежние. Три служебных record-класса отличаются отладочными номерами строк; их исполняемый код совпал. Единственное изменение старого текстового ресурса — одна поправка названия предмета. Полная выгруженная база и отчёты не попали в JAR.','',
        '| Обработка входящего чата | Изменение времени, два порядка | Байты/сообщение до → после |','| --- | ---: | ---: |']
    mode_labels={'unknown':'Неизвестные реплики','existing_dynamic':'Прежние динамические реплики','existing_exact':'Прежние точные реплики','existing_style':'Прежние цветные реплики','existing_tal':'Тал Кер','existing_wizard':'Волшебник','existing_tyashoi':'Алхимик Тьяшой','existing_elle':'Элль','existing_kat_elizabeth':'Кэт и Элизабет','existing_end_guide':'Одинокий искатель приключений','existing_juliette_style':'Цветные реплики Джульетты','existing_choices':'Прежние варианты ответа','new_large_batch':'Новые реплики — дополнительный перевод','new_choices':'Новые ответы — дополнительный перевод'}
    for r in benchmark['summary']:
        times=' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders']);memory=' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in r['orders']);report.append(f'| {mode_labels[r["mode"]]} | {times} | {memory} |')
    report+=['','Четыре отдельные JVM: 83 → 84 → 84 → 83; все прогретые раунды сохранены. Отрицательный процент означает меньшее время, положительный — большее. Новые положительные случаи делают дополнительный перевод, поэтому полезная работа не равна. Это не FPS и не 0,1% lows. Нулевое влияние на CPU, память или плавность не обещано; в игре эта сборка не измерена.','',
        'В этих запусках прежние сценарии дали от −9,2% до +2,6% времени обработки; это смешанные изменения, а не доказанное ускорение всех случаев. Неизвестные реплики: около +1,0%, или +0,04 мкс на сообщение. Новые реплики теперь действительно переводятся и стоят дополнительно около 0,20–0,60 мкс на сообщение против прежнего пропуска; выделение памяти также выросло. Новые кнопки сработали быстрее прежнего пути отказа, но это другой полезный результат. Эти цифры не позволяют вычислить FPS в игре.','',
        '## Оставшиеся ограничения','',
        'Полный перевод базы не завершён. Пять новых структурных черновиков, 72 старых цветных кандидата и прежний широкий смысловой аудит ReviewedLoreStyles (9sф, Wither Skeleton, Magma Cube, Dedication, Golem) остаются открытыми. Имена над моделями и все связанные меню этой партией не сертифицируются. JAR тестовый, автоматически не установлен; старые сборки сохранены.','',f'SHA-256 JAR: `{fingerprint}`.','',
        '[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.84_RU.md). Подробные смысловые решения — NPC_0.15.84_REVIEW_NOTES.md.']
    (SOURCE/'NPC_PROGRESS_0.15.84_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    destination.mkdir(parents=True)
    files=[p for p in SOURCE.glob('*0.15.84*') if p.is_file()]+[SOURCE/name for name in FILES]+[SOURCE/'NPC84_ITEM_CORRECTIONS.json',SOURCE/'translations.json']
    files += [DATA/'npc-expansion84.json',DATA/'corrections.tsv',ROOT/'src/main/resources/META-INF/licenses/NPC84-SOURCES.txt',ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',ROOT/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
    files += [ROOT/'tests'/name for name in ('TestNpc84.java','TestNpcChoices84.java','TestStoryActions.java','BenchmarkNpc84.java','AuditNpcCorpus77.java','TestHubNpcs69.java','TestNpc70.java','TestNpc74.java','TestNpc75.java','TestNpc76.java','TestNpc77.java','TestNpc78.java','TestNpc79.java','TestNpc80.java','TestNpc81.java','TestNpc82.java','TestNpc83.java','TestStoryDialogues.java')]
    files += [ROOT/'tools'/name for name in ('prepare_npc78.py','prepare_npc84.py','check_npc84.ps1','dispatch_npc84.ps1','benchmark_npc84.ps1','check_npc84_bytecode.ps1','finalize_npc84_progress.py')]
    for p in files:target=destination/p.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
    (destination/'checks').mkdir()
    for name in tests:shutil.copy2(args.checks/(name+'-output.txt'),destination/'checks')
    shutil.copy2(args.checks/'CHECK_SCOPE.json',destination/'checks');shutil.copy2(args.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json');shutil.copy2(args.bytecode/'RESULT.json',destination/'RECORD_BYTECODE_CHECK.json')
    for version in ('0.15.83-dev','0.15.84-dev'):
        target=destination/'dispatch'/version;target.mkdir(parents=True);shutil.copy2(args.audit/version/'DISPATCH_SUMMARY.json',target)
    (destination/'README_RU.md').write_text('# Большая партия84\n\n624 новые реплики,12 подписей кнопок,140 свежих групп проверок. 3 298/16 244 точных ключей (20,30%).\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.84_RU.md)\n\n[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.84_RU.md)\n\nЭто частичный пакет исходников/отчётов,не полный проект. Тестовый JAR выдаётся отдельно,не устанавливается автоматически.\n',encoding='utf-8')
    hashes={p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()};write(destination/'HASHES.json',hashes);archive=destination/'NPC_TRANSLATION_BATCH_08.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for name in sorted(hashes):z.write(destination/name,name)
        z.write(destination/'HASHES.json','HASHES.json')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for name,expected in hashes.items():assert hashlib.sha256(z.read(name)).hexdigest().upper()==expected
        entries=len(z.namelist())
    delivered_jar=jar_directory/jar.name
    if delivered_jar.exists():assert sha(delivered_jar)==fingerprint,'Different existing JAR;not replacing'
    else:shutil.copy2(jar,delivered_jar)
    assert sha(delivered_jar)==fingerprint
    write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered_jar),'jar_sha256':fingerprint,'installed_game_modified':False})
    print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))
if __name__=='__main__':main()
