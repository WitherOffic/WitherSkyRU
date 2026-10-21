"""Bind batch86 evidence to a single tested JAR; publish without touching Minecraft."""
import argparse, hashlib, html, json, re, shutil, zipfile
from collections import Counter, defaultdict
from pathlib import Path
from prepare_npc78 import rows, sha, write
from prepare_npc86 import ROOT, SOURCE, DATA, CORPUS, SPEAKERS, RUNTIME_RETAINED

BASE = '077F259C843C14A2F41EC5775AA3B58A5D95B44BF52FB1488EB52CAFCADD9D6E'
CANONICAL_BASE = Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-16_08')

def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def n(value):
    return f'{value:,}'.replace(',', '\u202f')

def tsv(path, separator='\t'):
    return [line.split(separator) for line in path.read_text(encoding='utf-8-sig').splitlines() if line and not line.startswith('#')]

def main():
    parser = argparse.ArgumentParser()
    for name in ('checks', 'audit', 'benchmark', 'bytecode', 'destination', 'jar-directory'):
        parser.add_argument('--'+name, type=Path, required=True)
    a = parser.parse_args()
    destination = a.destination.resolve()
    assert not destination.exists(), 'Never overwrite an existing delivery'
    assert a.jar_directory.is_dir()
    jar = ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.86-dev.jar'
    baseline = ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.85-dev.jar'
    fingerprint = sha(jar)
    assert sha(baseline) == BASE
    scope = load(a.checks/'CHECK_SCOPE.json')
    tests = scope['tests_passed']
    assert scope['runtime_jar_sha256'] == fingerprint
    assert not scope['targeted_only'] and not scope['reused_successes']
    assert len(tests) == len(set(tests)) == 144
    assert {'TestNpc86','TestRecipeNames86','TestNpcChoices86','TestNpc85','TestEditorialQuality','TestJarDictionary014','TestHudMixins','TestFrameNoops71'} <= set(tests)
    for name in tests:
        assert (a.checks/(name+'-output.txt')).is_file()
    assertions = {}
    for name, label, marker in (
        ('TestNpc86','NPC86','699 new authored literals/699 published layouts'),
        ('TestRecipeNames86','RecipeNames86','21 exact item names;42 source-backed Alcina contexts'),
        ('TestNpcChoices86','NPCChoices86','10 exact ordered groups/12 label slots/all10 published layouts')):
        output = (a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig')
        assert marker in output
        assertions[name] = int(re.search(r'PASS: '+label+r' (\d+) assertions', output)[1])

    batch = load(DATA/'npc-expansion86.json')['records']
    new = {r['source']: r['translation'] for r in batch}
    assert len(new) == len(batch) == 699
    assert Counter(r['speaker'] for r in batch) == SPEAKERS
    authoring_paths, authored = [], {}
    for speaker in SPEAKERS:
        path = SOURCE/('NPC86_'+re.sub('[^A-Z0-9]+','_',speaker.upper())+'_REVIEWED.tsv')
        authoring_paths.append(path)
        for fields in tsv(path, '|'):
            source = f'[NPC] {fields[0]}: {fields[1]}'
            assert source not in authored
            authored[source] = f'[NPC] {fields[0]}: {fields[2]}'
    assert authored == new
    evidence = load(SOURCE/'NPC_SOURCES_0.15.86.json')
    assert len(evidence['records']) == 738 and len(evidence['source_pages']) == 13
    assert len(evidence['pending_variables']) == 33 and len(evidence['neutral_unchanged']) == 3
    assert not evidence['retained_originals'] and not evidence['unsafe_excluded']
    assert {r['source'] for r in evidence['runtime_retained']} == RUNTIME_RETAINED
    revision = load(SOURCE/'NPC_0.15.86_PUBLIC_REVISION.json')
    assert not revision['matches_pinned_corpus'] and revision['dialogue_unchanged_after_exact_manual_delta_review']
    assert len(revision['pages']) == 13
    assert revision['differences'] == {'Cult of the Fallen Star': {'pinned':843725, 'current':859622}}
    delta = load(SOURCE/'NPC_0.15.86_REVISION_DELTA.json')['records']
    assert len(delta) == 1 and delta[0]['current_revision'] == 859622
    pinned = next(r['raw_wikitext'] for r in evidence['source_pages'] if r['title']=='Cult of the Fallen Star')
    removed = '{{Stub|Need to add the countdown on [[Events#Timers]] {{TODOstate|no}}}}\n\n'
    assert pinned.startswith(removed)
    assert delta[0]['current_page']['revisions'][0]['slots']['main']['content'] == pinned[len(removed):]

    recipes = dict(tsv(SOURCE/'NPC86_RECIPE_NAMES.tsv'))
    recipe_evidence = load(SOURCE/'NPC_RECIPE_SOURCES_0.15.86.json')['records']
    assert len(recipes) == len(recipe_evidence) == 21
    assert {r['source']:r['translation'] for r in recipe_evidence} == recipes
    for row in recipe_evidence:
        assert len(row['dialogue_sources']) == 2
        for source in row['dialogue_sources']:
            assert row['translation'] in new[source]
    choices = [(json.loads(x),json.loads(y)) for x,y in tsv(SOURCE/'NPC_CHOICES_0.15.86_REVIEWED.tsv','|')]
    choice_evidence = load(SOURCE/'NPC_CHOICES_SOURCES_0.15.86.json')
    assert len(choices) == len(choice_evidence['records']) == 10
    assert sum(len(x) for x,y in choices) == 12
    assert len({label for x,y in choices for label in x}) == 11
    assert [(r['source_labels'],r['target_labels']) for r in choice_evidence['records']] == choices
    assert len(choice_evidence['retained_groups']) == 2 and len(choice_evidence['heading_only_excluded']) == 1
    assert all(r['all_occurrences'] for r in choice_evidence['records'])

    before = load(a.audit/'0.15.85-dev/DISPATCH_SUMMARY.json')
    after = load(a.audit/'0.15.86-dev/DISPATCH_SUMMARY.json')
    assert before['runtime_jar_sha256'] == BASE and after['runtime_jar_sha256'] == fingerprint
    assert before['queue_sha256'] == after['queue_sha256'] == evidence['queue_sha256'] == sha(CORPUS/'translation_queue.jsonl')
    assert before['tested_literal_rows'] == after['tested_literal_rows'] == 11247
    assert (before['flat_changed_not_quality_proof'],before['published_style_changed_not_quality_proof']) == (3949,3877)
    assert (after['flat_changed_not_quality_proof'],after['published_style_changed_not_quality_proof']) == (4648,4576)
    old = {r['source']:r for r in rows(a.audit/'0.15.85-dev/DISPATCH_ROWS.jsonl')}
    current = {r['source']:r for r in rows(a.audit/'0.15.86-dev/DISPATCH_ROWS.jsonl')}
    assert old.keys() == current.keys()
    for source,row in current.items():
        if source in new:
            assert row['flat_result'] == row['published_style_result'] == new[source], source
        else:
            assert all(row[k] == old[source][k] for k in ('flat_result','published_style_result')), source
    assert len(new.keys() & current.keys()) == 699
    for row in evidence['runtime_retained']:
        assert current[row['source']]['flat_result'] == row['before_flat']
        assert current[row['source']]['published_style_result'] == row['before_colored']
    gaps = [r for r in current.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']]
    assert len(gaps) == 72

    benchmark = load(a.benchmark/'RAW.json')
    assert [r['version'] for r in benchmark['records']] == ['0.15.85-dev','0.15.86-dev','0.15.86-dev','0.15.85-dev']
    assert len(benchmark['summary']) == 17
    for record in benchmark['records']:
        assert record['jar_sha256'] == (BASE if record['version']=='0.15.85-dev' else fingerprint)
    bytecode = load(a.bytecode/'RESULT.json')
    assert bytecode['runtime_jar_sha256'] == fingerprint and len(bytecode['records']) == 3
    assert all(r['executable_disassembly_identical'] for r in bytecode['records'])
    with zipfile.ZipFile(baseline) as x, zipfile.ZipFile(jar) as y:
        assert y.testzip() is None
        old_names,new_names = set(x.namelist()),set(y.namelist())
        assert not old_names-new_names
        changed = sorted(k for k in old_names & new_names if x.read(k)!=y.read(k))
        added = sorted(new_names-old_names)
        assert changed == ['data/witherskyru/corrections.tsv','fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class'], changed
        assert added == ['META-INF/licenses/NPC86-SOURCES.txt','data/witherskyru/npc-expansion86.json'], added
        for path in ('npc-expansion85.json','npc-expansion86.json'):
            assert y.read('data/witherskyru/'+path) == (DATA/path).read_bytes()
        correction_old = x.read('data/witherskyru/corrections.tsv').decode('utf-8-sig').splitlines()
        correction_new = y.read('data/witherskyru/corrections.tsv').decode('utf-8-sig').splitlines()
        assert correction_new[:len(correction_old)] == correction_old
        assert correction_new[len(correction_old):] == [s+'\t'+t for s,t in recipes.items()]
    manifest = load(CANONICAL_BASE/'HASHES.json')
    canonical_base = CANONICAL_BASE/'translation-source/translations.json'
    assert sha(canonical_base) == manifest['translation-source/translations.json'] == 'DDAD580AB5ED31C00B72AA3005E540BBF306F9528FC3BDDF127EB5734BBF68C6'
    old_canonical, canonical = load(canonical_base),load(SOURCE/'translations.json')
    assert canonical.keys() == old_canonical.keys()
    for key in canonical:
        if key != 'translations':
            assert canonical[key] == old_canonical[key],key
    assert {s:t for s,t in canonical['translations'].items() if s not in old_canonical['translations']} == recipes
    assert all(canonical['translations'][s] == t for s,t in old_canonical['translations'].items())
    assert load(SOURCE/'coverage.json')['runtime_dictionary_entries'] == 36065

    stats = load(SOURCE/'NPC_PROGRESS_0.15.86.json')
    assert stats['exact_resource_after'] == 4686 and stats['new_resource_sha256'] == sha(DATA/'npc-expansion86.json')
    stats.update({'runtime_validation':'144_fresh_groups_passed','fresh_test_groups':144,'assertions':assertions,'runtime_jar_sha256':fingerprint,'runtime_changed_files':changed,'runtime_added_files':added,'render_hud_f3_classes_byte_identical':True,'other_executable_code_unchanged_except_existing_maps':True,'inner_record_bytecode':bytecode,'outside_selected_dispatch_text_unchanged':True,'new_rows_in_conservative_bulk_dispatch':699,'compatible_published_npc_layouts_checked':699,'additional_choice_groups':10,'additional_choice_slots':12,'additional_unique_choice_labels':11,'remaining_original_style_gap':72,'dispatch_before':before,'dispatch_after':after,'benchmark_summary':benchmark['summary'],'current_revision_check':'12 exact revisions plus one exact editorial-only delta','parser_tests_passed':13})
    write(SOURCE/'NPC_PROGRESS_0.15.86.json',stats)
    write(SOURCE/'NPC_STYLE_REMAINING_0.15.86.json',{'scope':'72 older candidates unchanged by86, not certified fixed','records':gaps})
    exact = defaultdict(set)
    for path in sorted(DATA.glob('*.json')):
        for row in load(path).get('records',[]):
            if row.get('source') and row.get('translation'):
                exact[row['source']].add(row['translation'])
    worklist = rows(SOURCE/'NPC_WORKLIST_0.15.85.jsonl')
    queue = rows(CORPUS/'translation_queue.jsonl')
    assert len(worklist) == len(queue) == 16244
    assert [r['source'] for r in worklist] == [r['source'] for r in queue]
    pending = {r['source'] for r in evidence['pending_variables']}
    neutral = {r['source'] for r in evidence['neutral_unchanged']}
    choice_sources = {o['source'] for r in choice_evidence['records'] for o in r['all_occurrences']}
    by_speaker = defaultdict(list)
    for row in worklist:
        source = row['source']
        row['exact_registration'] = bool(exact.get(source))
        row['registered_targets_not_automatically_certified'] = sorted(exact.get(source,set()))
        if source in new:
            row['review_status'] = 'authored_and_fixture_checked86'
            row['all_published_npc_layouts_checked86'] = True
        if source in pending:
            row['structural_review_pending86'] = True
            row['template_coverage_certified86'] = False
        if source in neutral:
            row['review_status'] = 'neutral_no_language_translation_needed86'
        if source in RUNTIME_RETAINED:
            row['existing_dynamic_result_retained86'] = True
        if source in choice_sources:
            row['ordered_choice_group_checked86_separate_from_npc_keys'] = True
        if source in current:
            row['runtime_flat_snapshot_not_quality_proof'] = current[source]['flat_result']
            row['runtime_colored_snapshot_not_quality_proof'] = current[source]['published_style_result']
        by_speaker[row['speaker'] or '(без указанного говорящего)'].append(row)
    assert sum(r['exact_registration'] for r in worklist) == 4686
    assert sum(r.get('review_status')=='authored_and_fixture_checked86' for r in worklist) == 699
    (SOURCE/'NPC_WORKLIST_0.15.86.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in worklist),encoding='utf-8')
    gap_keys = {r['source'] for r in gaps}
    table = ['# Вся очередь NPC — 0.15.86','','Публичная выгрузка, не полный реестр действующих NPC. Точный ключ не сертифицирует весь перевод; динамические строки, нейтральные записи и кнопки учитываются отдельно.','','| Говорящий | Всего | Точных ключей | Доля | Новых в 86 | Старых цветных кандидатов | Структурных признаков |','| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for speaker,values in sorted(by_speaker.items()):
        count = sum(r['exact_registration'] for r in values)
        label = html.escape(speaker).replace('|','\\|')
        table.append(f'| {label} | {len(values)} | {count} | {100*count/len(values):.2f}% | {sum(r["source"] in new for r in values)} | {sum(r["source"] in gap_keys for r in values)} | {sum(bool(r["variable_definition"]) for r in values)} |')
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.86_RU.md').write_text('\n'.join(table)+'\n',encoding='utf-8')
    report = ['# Большая партия NPC — 0.15.86','','`███████░░░░░░░░░░░░░░░░░░` **28,85%** точных ключей публичной базы (полоса округлена).','','| Учёт | До | После |','| --- | ---: | ---: |','| Точные ключи переводов | 3 987 · 24,54% | 4 686 · 28,85% |','| Без подтверждённого точного ключа | 12 257 | 11 558 · 71,15% |','| Всего уникальных строк базы | 16 244 | 16 244 |','','Это не процент полностью проверенного перевода всей игры. Отсутствие точного ключа не исключает динамического перевода. База содержит исторические и редакторские строки; закрытые серверные скрипты недоступны.','','| Говорящий | Новых реплик |','| --- | ---: |']
    report += [f'| {s} | {c} |' for s,c in SPEAKERS.items()]
    report += ['| Всего | 699 |','','Дополнительно, без прибавки к счётчику NPC: 21 название блюда; 10 упорядоченных групп выбора — 12 мест для кнопок, 11 разных подписей. В выбранных 738 строках остаются 33 структурные на отдельную проверку; три нейтральные и три прежние динамические не засчитаны новыми. Таблички имён и полные описания блюд этой партией не сертифицированы.','','## Качество и актуальность','','Связанные сюжетные ветки и шесть встреч культа вычитаны в контексте. Названия блюд согласованы в репликах и предметах. Сохранены числа, направления, смысл шуток, цветные буквы и порядок команд. [Подробная вычитка](NPC_0.15.86_REVIEW_NOTES.md).','','Последняя API-проверка: '+revision['checked_client_date']+'. 12 ревизий совпали; в Cult of the Fallen Star удалена только конкретная редакторская пометка, остальной текст прежний. Архивные события Rezzus не объявлены действующими сейчас.','',f'Все 144 группы проверок заново прошли на одном окончательном JAR: {n(assertions["TestNpc86"])} проверок реплик, {n(assertions["TestRecipeNames86"])} проверок названий и {n(assertions["TestNpcChoices86"])} проверок кнопок. Все 699 новых опубликованных цветовых схем и десять схем выбора сработали. Проверены свежие цвета, курсив, действия и подсказки, числа, время, счётчики, фрагментация, переопределения, переключатели и защита пользовательского/подписанного/клиентского чата. Отдельно прошли 13 тестов парсера. Это не живой проход сервера.','','| Те же 11 247 строк: техническое срабатывание, не оценка качества | 85 | 86 |','| --- | ---: | ---: |','| Обычный текст | 3 949 | 4 648 |','| Опубликованные цвета | 3 877 | 4 576 |','','Вне 699 новых строк оба результата побуквенно прежние. 72 старых цветных кандидата остаются открытыми.','','## Нагрузка','','Новые данные включены в прежние карты при запуске. Новых кадровых проходов и сетевого перевода в игре нет. HUD/F3 и остальные исполняемые классы вне двух существующих переводчиков побайтово прежние; три внутренних record-класса отдельно совпали по исполняемому коду (сдвинулись отладочные номера строк). Из старых текстовых ресурсов изменён только словарь: дописаны ровно 21 название, прежние строки сохранены. JAR вырос на '+n(jar.stat().st_size-baseline.stat().st_size)+' байт. Полная выгруженная база в него не включена.','','| Входящий чат | Изменение времени, два порядка | Байты/сообщение до → после |','| --- | ---: | ---: |']
    labels = {'unknown':'Неизвестные реплики','existing_dynamic':'Прежние динамические','existing_exact':'Прежние точные','existing_style':'Прежние цветные','existing_tal':'Тал Кер','existing_wizard':'Волшебник','existing_tyashoi':'Алхимик Тьяшой','existing_elle':'Элль','existing_kat_elizabeth':'Кэт и Элизабет','existing_end_guide':'Одинокий искатель приключений','existing_juliette_style':'Джульетта: цвета','existing_choices':'Прежние ответы','existing_large_batch84':'Партия 84','existing_choices84':'Кнопки 84','existing_large_batch85':'Партия 85','new_large_batch86':'Новые реплики 86: дополнительная работа','new_choices86':'Новые кнопки 86: дополнительная работа'}
    for row in benchmark['summary']:
        times = ' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in row['orders'])
        memory = ' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in row['orders'])
        report.append(f'| {labels[row["mode"]]} | {times} | {memory} |')
    equal = [o['wall_change_percent'] for r in benchmark['summary'] if r['equal_useful_output'] for o in r['orders']]
    report += ['',f'Прежние сценарии в этих запусках: от {min(equal):+.1f}% до {max(equal):+.1f}%. Разброс смешанный: это не доказательство ускорения или отсутствия микролагов. Четыре отдельные JVM: 85 → 86 → 86 → 85; все раунды сохранены. Отрицательный процент означает меньшее время, положительный — большее. Новые случаи раньше пропускались, теперь выполняют полезный перевод, поэтому это не равная работа.','','| Новые случаи | Время 86, мкс/сообщение, два порядка | Дополнительное время против пропуска, мкс |','| --- | ---: | ---: |']
    for row in benchmark['summary']:
        if row['equal_useful_output']:
            continue
        absolute = ' / '.join(f'{o["after_ns"]/1000:.2f}' for o in row['orders'])
        extra = ' / '.join(f'{(o["after_ns"]-o["before_ns"])/1000:+.2f}' for o in row['orders'])
        report.append(f'| {labels[row["mode"]]} | {absolute} | {extra} |')
    report += ['','Это обработка при получении сообщения, не повторение базы каждый кадр. CPU-счётчик Windows грубый; абсолютные значения, выделения памяти и все раунды сохранены в BENCHMARK_RAW.json. Названия предметов отдельно на время отрисовки не замерялись. Ни FPS, ни 0,1% lows в этой сборке не измерены; нулевая нагрузка не обещана.','','## Что осталось','','Вся база ещё не переведена. Открыты структурные случаи, 72 прежних цветных кандидата и широкий старый смысловой аудит ReviewedLoreStyles. Эта сборка автоматически в игру не установлена, старые JAR сохранены.','','[Все говорящие и остаток](NPC_ALL_SPEAKERS_0.15.86_RU.md).','',f'SHA-256 JAR: `{fingerprint}`.']
    (SOURCE/'NPC_PROGRESS_0.15.86_RU.md').write_text('\n'.join(report)+'\n',encoding='utf-8')

    destination.mkdir(parents=True)
    files = [p for p in SOURCE.glob('*0.15.86*') if p.is_file()] + authoring_paths
    files += [SOURCE/'NPC86_RECIPE_NAMES.tsv',SOURCE/'translations.json',SOURCE/'coverage.json',DATA/'npc-expansion86.json',DATA/'corrections.tsv',ROOT/'src/main/resources/META-INF/licenses/NPC86-SOURCES.txt',ROOT/'src/main/resources/fabric.mod.json',ROOT/'build.gradle']
    files += [ROOT/'src/main/java/ru/wither/skyru'/name for name in ('NpcDialogueTranslator.java','ChatNotificationTranslator.java')]
    files += [ROOT/'tests'/name for name in ('TestNpc86.java','TestRecipeNames86.java','TestNpcChoices86.java','BenchmarkNpc86.java','AuditNpcCorpus77.java')]
    files += [ROOT/'tools'/name for name in ('prepare_npc78.py','prepare_npc86.py','check_npc86.ps1','dispatch_npc86.ps1','benchmark_npc86.ps1','check_npc86_bytecode.ps1','finalize_npc86_progress.py')]
    files += [p for p in (ROOT/'tests').glob('*.java') if 'phraseCount()==4448' in p.read_text(encoding='utf-8-sig')]
    for path in sorted(set(files)):
        target = destination/path.relative_to(ROOT)
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(path,target)
    checks = destination/'checks'
    checks.mkdir()
    for name in tests:
        shutil.copy2(a.checks/(name+'-output.txt'),checks)
    shutil.copy2(a.checks/'CHECK_SCOPE.json',checks)
    shutil.copy2(a.benchmark/'RAW.json',destination/'BENCHMARK_RAW.json')
    shutil.copytree(a.bytecode,destination/'bytecode')
    for version in ('0.15.85-dev','0.15.86-dev'):
        target = destination/'dispatch'/version
        target.mkdir(parents=True)
        shutil.copy2(a.audit/version/'DISPATCH_SUMMARY.json',target)
    (destination/'README_RU.md').write_text('# Большая партия 86\n\n699 новых реплик, 13 обозначений говорящего; 21 название блюда; 10 групп выбора/12 мест для кнопок. 144 свежие группы проверок. 4 686 / 16 244 точных ключей (28,85%).\n\n[Отчёт и ограничения](translation-source/NPC_PROGRESS_0.15.86_RU.md)\n\n[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.86_RU.md)\n\nЧастичный пакет исходников и отчётов, не полный проект. Тестовый JAR выдаётся отдельно и не устанавливается автоматически.\n',encoding='utf-8')
    hashes = {p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()}
    write(destination/'HASHES.json',hashes)
    archive = destination/'NPC_TRANSLATION_BATCH_10.zip'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
        for name in sorted(hashes):
            z.write(destination/name,name)
        z.write(destination/'HASHES.json','HASHES.json')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for name,expected in hashes.items():
            assert hashlib.sha256(z.read(name)).hexdigest().upper() == expected
        entries = len(z.namelist())
    delivered = a.jar_directory/jar.name
    if delivered.exists():
        assert sha(delivered) == fingerprint, 'Never replace a different published JAR'
    else:
        shutil.copy2(jar,delivered)
    assert sha(delivered) == fingerprint
    write(destination/'DELIVERY.json',{'archive':str(archive),'sha256':sha(archive),'bytes':archive.stat().st_size,'entries':entries,'jar':str(delivered),'jar_sha256':fingerprint,'installed_game_modified':False})
    print(json.dumps(load(destination/'DELIVERY.json'),ensure_ascii=True,indent=2))

if __name__ == '__main__':
    main()
