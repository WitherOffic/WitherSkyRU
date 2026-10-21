"""Publish a checked finite translation/source batch; never install a game JAR."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'translation-source'
DATA = ROOT / 'src/main/resources/data/witherskyru'
BASELINE_HASH = '07D367E7D7BBDF1A168063CC1267614F04F4E9F71D7A879EC04A1289DF1DCD1C'

def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def rows(path):
    return [json.loads(s) for s in path.read_text(encoding='utf-8-sig').splitlines() if s]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def main():
    p = argparse.ArgumentParser()
    for name in ('checks', 'audit', 'benchmark', 'destination', 'corpus'):
        p.add_argument('--'+name, type=Path, required=True)
    args = p.parse_args()
    destination = args.destination.resolve()
    assert not destination.exists(), 'Refusing to overwrite an existing delivery'
    jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.79-dev.jar'
    fingerprint = sha(jar)
    checks = load(args.checks/'CHECK_SCOPE.json')
    tests = checks['tests_passed']
    assert checks['runtime_jar_sha256'] == fingerprint and not checks['targeted_only']
    assert len(tests) == len(set(tests)) == 133
    assert {'TestNpc79', 'TestNpc78', 'TestNpcStyles78', 'TestNpcLookup61', 'TestFrameNoops71', 'TestHudMixins'} <= set(tests)
    for name in tests:
        assert (args.checks/(name+'-output.txt')).is_file()
    assertions = re.search(r'PASS: NPC79 (\d+) assertions', (args.checks/'TestNpc79-output.txt').read_text(encoding='utf-8-sig'))
    assert assertions
    benchmark = load(args.benchmark/'RAW.json')
    assert [r['version'] for r in benchmark['records']] == ['0.15.78-dev', '0.15.79-dev', '0.15.79-dev', '0.15.78-dev']
    for r in benchmark['records']:
        assert r['jar_sha256'] == (fingerprint if r['version'] == '0.15.79-dev' else BASELINE_HASH)
    previous = load(args.audit/'0.15.78-dev/DISPATCH_SUMMARY.json')
    current = load(args.audit/'0.15.79-dev/DISPATCH_SUMMARY.json')
    assert previous['runtime_jar_sha256'] == BASELINE_HASH and current['runtime_jar_sha256'] == fingerprint
    assert previous['tested_literal_rows'] == current['tested_literal_rows'] == 11247
    assert previous['queue_sha256'] == current['queue_sha256'] == sha(args.corpus/'translation_queue.jsonl')
    batch = load(DATA/'npc-expansion79.json')['records']
    new = {r['source']: r['translation'] for r in batch}
    assert len(batch) == len(new) == 244
    authored = {}
    for line in (SOURCE/'NPC_0.15.79_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if line and not line.startswith('#'):
            f = line.split('|')
            authored[f'[NPC] {f[0]}: {f[1]}'] = f'[NPC] {f[0]}: {f[2]}'
    assert authored == new, 'Authoring targets must match the tested executable'
    old_dispatch = {r['source']: r for r in rows(args.audit/'0.15.78-dev/DISPATCH_ROWS.jsonl')}
    now_dispatch = {r['source']: r for r in rows(args.audit/'0.15.79-dev/DISPATCH_ROWS.jsonl')}
    assert len(old_dispatch) == len(now_dispatch) == 11247
    tested_new = 0
    for source, row in now_dispatch.items():
        if source in new:
            assert row['flat_result'] == row['published_style_result'] == new[source], source
            tested_new += 1
        else:
            assert all(row[k] == old_dispatch[source][k] for k in ('flat_result', 'published_style_result')), source
    assert tested_new == 229
    remaining = [r for r in now_dispatch.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']]
    assert len(remaining) == 87
    baseline_jar = ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.78-dev.jar'
    assert sha(baseline_jar) == BASELINE_HASH
    with zipfile.ZipFile(baseline_jar) as before, zipfile.ZipFile(jar) as after:
        old_names, new_names = set(before.namelist()), set(after.namelist())
        assert not old_names-new_names
        changed = sorted(n for n in old_names & new_names if before.read(n) != after.read(n))
        added = sorted(new_names-old_names)
        assert changed == ['fabric.mod.json', 'ru/wither/skyru/NpcDialogueTranslator.class']
        assert added == ['META-INF/licenses/NPC79-SOURCES.txt', 'data/witherskyru/npc-expansion79.json']
        assert after.read('data/witherskyru/npc-expansion79.json') == (DATA/'npc-expansion79.json').read_bytes()
    stats = load(SOURCE/'NPC_PROGRESS_0.15.79.json')
    assert stats['exact_resource_after'] == 2092 and stats['new_reviewed_literal_rows'] == 244
    assert stats['new_resource_sha256'] == sha(DATA/'npc-expansion79.json')
    stats.update({'runtime_validation': '133_fresh_groups_passed', 'fresh_test_groups': 133,
        'npc79_assertions': int(assertions[1]), 'runtime_jar_sha256': fingerprint,
        'runtime_changed_files': changed, 'runtime_added_files': added,
        'old_resource_and_render_classes_byte_identical': True,
        'outside_selected_dispatch_text_unchanged': True,
        'new_rows_in_conservative_bulk_dispatch': tested_new,
        'all244_literal_rows_fixture_checked': True, 'remaining_original_style_gap': 87,
        'dispatch_before': previous, 'dispatch_after': current, 'benchmark_summary': benchmark['summary']})
    write(SOURCE/'NPC_PROGRESS_0.15.79.json', stats)
    write(SOURCE/'NPC_STYLE_REMAINING_0.15.79.json', {'scope': '87 remaining flat/colored candidates; not all manually certified defects',
        'runtime_jar_sha256': fingerprint, 'records': remaining})
    exact = defaultdict(set)
    for path in sorted(DATA.glob('*.json')):
        if path.name == 'npc-styles78.json':
            continue
        for r in load(path).get('records', []):
            if r.get('source') and r.get('translation'):
                exact[r['source']].add(r['translation'])
    worklist = rows(SOURCE/'NPC_WORKLIST_0.15.78.jsonl')
    assert len(worklist) == 16244
    evidence = load(SOURCE/'NPC_SOURCES_0.15.79.json')
    pending = {r['source'] for r in evidence['pending_variables']}
    drafts = {}
    for line in (SOURCE/'NPC_0.15.79_VARIABLE_DRAFTS.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):
            continue
        source, target, status = line.split('|')
        assert status.startswith('PENDING_') and source not in new
        assert re.findall(r'\[[^]]+\]|<[^>]+>|#', source) == re.findall(r'\[[^]]+\]|<[^>]+>|#', target)
        assert source not in drafts
        drafts[source] = target
    assert set(drafts) == pending and len(drafts) == 7
    per_speaker = defaultdict(list)
    for r in worklist:
        source = r['source']
        r['exact_registration'] = bool(exact.get(source))
        r['registered_targets_not_automatically_certified'] = sorted(exact.get(source, set()))
        if source in new:
            r['review_status'] = 'authored_and_fixture_checked79'
            r['all244_fixture_checked79'] = True
        if source in pending:
            r['review_status'] = 'authored_variable_draft_not_runtime_registered79'
            r['supplemental_placeholder_guard79'] = True
        if source in now_dispatch:
            r['runtime_flat_snapshot_not_quality_proof'] = now_dispatch[source]['flat_result']
            r['runtime_colored_snapshot_not_quality_proof'] = now_dispatch[source]['published_style_result']
        per_speaker[r['speaker'] or '(без указанного говорящего)'].append(r)
    assert sum(r['exact_registration'] for r in worklist) == 2092
    (SOURCE/'NPC_WORKLIST_0.15.79.jsonl').write_text(''.join(json.dumps(r, ensure_ascii=False)+'\n' for r in worklist), encoding='utf-8')
    table = ['# Вся очередь NPC — 0.15.79', '',
        'Публичный корпус:16 244 уникальные строки. Точные ключи — не сертификат качества, '
        'не полный реестр действующих NPC и не закрытые серверные скрипты. Часть остальных строк может переводиться динамически.', '',
        '| Обозначение | Всего | Точных ключей | Доля | Новые тексты79 | Цветных кандидатов | Переменных по исходной разметке |',
        '| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    gap_sources = {r['source'] for r in remaining}
    for speaker, values in sorted(per_speaker.items()):
        registered = sum(r['exact_registration'] for r in values)
        safe = speaker.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('|', '\\|')
        table.append(f'| {safe} | {len(values)} | {registered} | {100*registered/len(values):.2f}% | '
            f'{sum(r["source"] in new for r in values)} | {sum(r["source"] in gap_sources for r in values)} | '
            f'{sum(bool(r["variable_definition"]) for r in values)} |')
    (SOURCE/'NPC_ALL_SPEAKERS_0.15.79_RU.md').write_text('\n'.join(table)+'\n', encoding='utf-8')
    report = ['# Перевод NPC — партия 0.15.79', '',
        '| Учёт точных ключей публичной базы | Строк | Доля |', '| --- | ---: | ---: |',
        '| Зарегистрировано после партии | 2 092 | 12,88% |',
        '| Без подтверждённого точного ключа | 14 152 | 87,12% |',
        '| Всего в корпусе | 16 244 | 100% |', '',
        'Наличие ключа не доказывает качество всех прежних переводов, отсутствие не исключает динамический перевод. '
        'Корпус не является закрытой базой живого сервера.', '',
        '## Новые ветки', '', '| Говорящий | Опубликовано | Переведено и проверено в исполнении | Переменных ещё без обработчика |',
        '| --- | ---: | ---: | ---: |', '| Wizard — Волшебник | 120 | 115 | 5 |',
        '| Wizardman — Визардмен | 131 | 129 | 2 |', '',
        '244 буквальные реплики: знакомство, Разлом, обучение пути, награды, гонка, поединок, игра «четыре в ряд», '
        'повторные разговоры и помеченные в источнике исторические ветки. Все исходные блоки/условия/ревизии сохранены. '
        'Для7 переменных строк написаны русские черновики, но они не подменены фиктивными буквальными ключами. '
        'Прежняя разметка переменных не стёрта; два неразмеченных символа# отдельно отмечены.', '',
        '## Проверки', '',
        f'Все133 группы пройдены заново на одном JAR. Новые244 строки: {int(assertions[1]):,} проверок. '
        'Сверены авторский русский текст и опубликованный источник, цвета, курсив, текущие click/hover/insertion/shadow, '
        'числа, время, счётчики, фрагментация, приоритет пользовательских переопределений, '
        'защита пользовательского и подписанного чата. Неизвестная смешанная схема остаётся исходной.', '',
        'Массовый проход: те же11 247 консервативно отобранных строк, результаты:', '',
        '| Техническое срабатывание, не оценка качества | 78 | 79 |', '| --- | ---: | ---: |',
        f'| Обычный текст | {previous["flat_changed_not_quality_proof"]} | {current["flat_changed_not_quality_proof"]} |',
        f'| Опубликованные цвета | {previous["published_style_changed_not_quality_proof"]} | {current["published_style_changed_not_quality_proof"]} |', '',
        '229 новых строк входят в массовую выборку; остальные15 (исторические/вариант ответа) проверены отдельными '
        'фикстурами в полной партии244. Вне новых ключей результаты дословно совпали. '
        '87 прежних цветных кандидатов остаются в очереди; эта партия не объявляет их исправленными.', '',
        '## Оптимизация', '',
        'Добавлен только ресурс и его загрузка в прежнюю карту при запуске. Новых regex-проходов, '
        'провайдеров, кадровых обработчиков или обработки HUD нет. Все старые ресурсы и классы, кроме '
        'загрузчика NpcDialogueTranslator, побайтово совпадают с78. F3 не менялся. '
        'Большие очереди и исходные блоки в JAR не попали.', '',
        'Четыре отдельные JVM:78→79→79→78; четыре измеряемых раунда после прогрева. '
        'Это время обработки входящего чата, не FPS или0,1% lows. Новый положительный случай выполняет дополнительный перевод.', '',
        '| Сценарий | Изменение времени в двух порядках | Байты/сообщение до→после |', '| --- | ---: | ---: |']
    for r in benchmark['summary']:
        times = ' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in r['orders'])
        memory = ' / '.join(f'{o["before_bytes"]:.0f}→{o["after_bytes"]:.0f}' for o in r['orders'])
        report.append(f'| {r["mode"]} | {times} | {memory} |')
    report += ['', '## Статус', '',
        'Установленная игра не менялась. Пакет содержит проверенную партию исходников, а не финальный установочный JAR. '
        'Остаются прежние незавершённые смысловые правки широкого аудита ReviewedLoreStyles; их нельзя '
        'объявлять завершёнными по зелёным автотестам. Последний ранее выданный релиз76 сохранён.', '',
        f'Рабочий проверенный JAR79, SHA-256: `{fingerprint}`.', '']
    (SOURCE/'NPC_PROGRESS_0.15.79_RU.md').write_text('\n'.join(report), encoding='utf-8')
    destination.mkdir(parents=True)
    files = [f'translation-source/{name}' for name in (
        'NPC_PROGRESS_0.15.79_RU.md', 'NPC_PROGRESS_0.15.79.json', 'NPC_0.15.79_REVIEWED.tsv',
        'NPC_0.15.79_VARIABLE_DRAFTS.tsv', 'NPC_SOURCES_0.15.79.json', 'NPC_STYLE_REMAINING_0.15.79.json',
        'NPC_WORKLIST_0.15.79.jsonl', 'NPC_ALL_SPEAKERS_0.15.79_RU.md')]
    files += ['src/main/resources/data/witherskyru/npc-expansion79.json',
        'src/main/resources/META-INF/licenses/NPC79-SOURCES.txt',
        'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java', 'tests/TestNpc79.java',
        'tests/BenchmarkNpc79.java', 'tests/AuditNpcCorpus77.java',
        'tools/prepare_npc79.py', 'tools/check_npc79.ps1', 'tools/dispatch_npc79.ps1',
        'tools/benchmark_npc79.ps1', 'tools/finalize_npc79_progress.py']
    files += ['tests/'+name for name in ('TestHubNpcs69.java','TestNpc70.java','TestNpc74.java','TestNpc75.java',
        'TestNpc76.java','TestNpc77.java','TestNpc78.java','TestStoryDialogues.java')]
    for name in files:
        target = destination/name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT/name, target)
    (destination/'checks').mkdir()
    for name in tests:
        shutil.copy2(args.checks/(name+'-output.txt'), destination/'checks')
    shutil.copy2(args.checks/'CHECK_SCOPE.json', destination/'checks')
    shutil.copy2(args.benchmark/'RAW.json', destination/'BENCHMARK_RAW.json')
    for version in ('0.15.78-dev', '0.15.79-dev'):
        target = destination/'dispatch'/version
        target.mkdir(parents=True)
        shutil.copy2(args.audit/version/'DISPATCH_SUMMARY.json', target)
    (destination/'README_RU.md').write_text('# Третья партия публичной базы NPC\n\n'
        '[Прогресс и проверки](translation-source/NPC_PROGRESS_0.15.79_RU.md)\n\n'
        '[Все говорящие](translation-source/NPC_ALL_SPEAKERS_0.15.79_RU.md)\n\n'
        '244 новые реплики Волшебника/Визардмена переведены и проверены;7 переменных строк — черновики, '
        'не установленные буквальные переводы. Общий счётчик2 092/16 244 (12,88%).\n\n'
        'Это партия исходников и отчётов, не полный проект и не финальный установочный JAR. '
        'Все133 группы проверки выполнены заново; игра не менялась.\n', encoding='utf-8')
    hashes = {p.relative_to(destination).as_posix():sha(p) for p in sorted(destination.rglob('*')) if p.is_file()}
    write(destination/'HASHES.json', hashes)
    archive = destination/'NPC_TRANSLATION_BATCH_03.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        for name in sorted(hashes):
            z.write(destination/name, name)
        z.write(destination/'HASHES.json', 'HASHES.json')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for name, expected in hashes.items():
            assert hashlib.sha256(z.read(name)).hexdigest().upper() == expected
        entries = len(z.namelist())
    delivery = {'archive':str(archive), 'bytes':archive.stat().st_size, 'entries':entries,
        'sha256':sha(archive), 'runtime_jar_sha256':fingerprint, 'installed_game_modified':False}
    write(destination/'DELIVERY.json', delivery)
    print(json.dumps(delivery, ensure_ascii=True, indent=2))

if __name__ == '__main__':
    main()
