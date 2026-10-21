"""Verify and publish the next bounded source/progress batch, not an installable release."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile
from collections import defaultdict

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'translation-source'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def rows(path):
    return [json.loads(line) for line in path.read_text(encoding='utf-8-sig').splitlines() if line]

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--checks', type=Path, required=True)
    parser.add_argument('--audit', type=Path, required=True)
    parser.add_argument('--benchmark', type=Path, required=True)
    parser.add_argument('--previous-benchmark', type=Path, action='append', default=[])
    parser.add_argument('--destination', type=Path, required=True)
    parser.add_argument('--corpus', type=Path, required=True)
    args = parser.parse_args()
    jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.78-dev.jar'
    fingerprint = sha(jar)
    checks = load(args.checks / 'CHECK_SCOPE.json')
    assert checks['runtime_jar_sha256'] == fingerprint and not checks['targeted_only']
    tests = checks['tests_passed']
    assert len(tests) == len(set(tests)) == 132 and {'TestNpc78', 'TestNpcStyles78', 'TestNpc77', 'TestNpcLookup61'} <= set(tests)
    benchmark = load(args.benchmark / 'RAW.json')
    assert len(benchmark['records']) == 4
    assert all(r['jar_sha256'] == fingerprint for r in benchmark['records'] if r['version'] == '0.15.78-dev')
    before = load(args.audit / '0.15.77-dev/DISPATCH_SUMMARY.json')
    after = load(args.audit / '0.15.78-dev/DISPATCH_SUMMARY.json')
    assert before['runtime_jar_sha256'] == '83F43BD1E85E987B8AE2F65A1AC66D0F202F5DDA913E278B5F8E0F9FAD49AE9D'
    assert after['runtime_jar_sha256'] == fingerprint and before['tested_literal_rows'] == after['tested_literal_rows'] == 11247
    assert before['queue_sha256'] == after['queue_sha256'] == 'FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    batch = load(ROOT / 'src/main/resources/data/witherskyru/npc-expansion78.json')['records']
    fixes = load(ROOT / 'src/main/resources/data/witherskyru/npc-styles78.json')['records']
    assert len(batch) == 324 and len(fixes) == 58
    selected = {r['source'] for r in batch + fixes}
    baseline = {r['source']: r for r in rows(args.audit / '0.15.77-dev/DISPATCH_ROWS.jsonl')}
    current = rows(args.audit / '0.15.78-dev/DISPATCH_ROWS.jsonl')
    assert len(baseline) == len(current) == 11247
    now = {r['source']: r for r in current}
    for row in current:
        prior = baseline[row['source']]
        if row['source'] not in selected:
            assert all(row[k] == prior[k] for k in ('flat_result', 'published_style_result')), row['source']
    for row in fixes:
        assert now[row['source']]['published_style_result'] == row['translation']
        assert baseline[row['source']]['flat_result'] == row['translation']
        assert now[row['source']]['flat_result'] == row['translation']
    remaining = [r for r in current if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']]
    assert len(remaining) == 87
    baseline_jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.77-dev.jar'
    assert sha(baseline_jar) == before['runtime_jar_sha256']
    with zipfile.ZipFile(jar) as runtime, zipfile.ZipFile(baseline_jar) as previous:
        resources = [n for n in previous.namelist() if not n.endswith('/') and n.startswith(('data/', 'assets/', 'META-INF/licenses/'))]
        assert all(previous.read(n) == runtime.read(n) for n in resources)
        changed = [n for n in previous.namelist() if n.endswith('.class') and previous.read(n) != runtime.read(n)]
        added = [n for n in runtime.namelist() if n.endswith('.class') and n not in previous.namelist()]
        assert all(n.startswith('ru/wither/skyru/NpcDialogueTranslator') for n in changed + added)
        for name in ('npc-expansion78.json', 'npc-styles78.json'):
            assert runtime.read('data/witherskyru/' + name) == (ROOT / 'src/main/resources/data/witherskyru' / name).read_bytes()
    stats = load(SOURCE / 'NPC_PROGRESS_0.15.78.json')
    assert stats['exact_resource_after'] == 1848 and stats['new_reviewed_literal_rows'] == 324
    stats.update({'validated_style_repairs': 58, 'authored_style_repairs_pending_runtime_validation': 0,
        'remaining_original_style_gap': 87, 'fresh_test_groups': 132, 'runtime_jar_sha256': fingerprint,
        'npc78_assertions': 99385, 'npc_styles78_assertions': 34585,
        'unchanged_old_resource_files': len(resources), 'runtime_changed_classes': changed,
        'runtime_added_classes': added, 'outside_selected_dispatch_text_unchanged': True,
        'dispatch_before': before, 'dispatch_after': after, 'benchmark_summary': benchmark['summary']})
    write(SOURCE / 'NPC_PROGRESS_0.15.78.json', stats)
    write(SOURCE / 'NPC_STYLE_REMAINING_0.15.78.json', {'scope': '87 remaining flat/colored dispatch candidates, not manually certified defects',
        'runtime_jar_sha256': fingerprint, 'records': remaining})
    report_path = SOURCE / 'NPC_PROGRESS_0.15.78_RU.md'
    report = report_path.read_text(encoding='utf-8').replace('58 точных планов на проверке', '58 исправлено; 87 осталось')
    report += '\n## Подтверждённые проверки\n\n'
    report += 'Все **132 группы** прошли заново на одном JAR. Новая ветка: **99 385** проверок; '
    report += 'цветные схемы: **34 585**. Проверены текущие цвета/действия, числа, времена, повторы, '
    report += 'фрагментированные компоненты, старые и новые пользовательские переопределения, защита подписанных и пользовательских сообщений.\n\n'
    report += '| Техническое срабатывание, не оценка качества | 0.15.77 | 0.15.78 |\n| --- | ---: | ---: |\n'
    for key, label in [('flat_changed_not_quality_proof', 'Обычный текст'), ('published_style_changed_not_quality_proof', 'Опубликованные цвета')]:
        report += f'| {label}, из 11 247 | {before[key]} | {after[key]} |\n'
    report += '\nИз массовой выборки исключена буквальная реплика `Doubt [x]`: '
    report += 'она прошла отдельные проверки в полной партии 324/324. За пределами новой ветки '
    report += 'и 58 исправлений результаты обычного и цветного текста дословно совпали.\n\n'
    report += 'Все прежние ресурсы и классы вне `NpcDialogueTranslator` побайтово совпадают с 0.15.77. '
    report += 'Обработчики кадров, HUD, F3 и подписанных сообщений не менялись. Списки ключей для проверки '
    report += 'пользовательских переопределений и неизменяемые русские фразы теперь готовятся при запуске. '
    report += 'Однородные реплики переносят текущее оформление целиком, без перераспределения по цветным фрагментам. '
    report += 'Многоцветные сообщения сохраняют строгую проверку исходных и опубликованных альтернативных планов. '
    report += 'Решения о переопределениях, текущие цвета и действия сообщений не кэшируются.\n\n'
    report += '## Замер нагрузки\n\nЧетыре изолированные JVM, порядок 77 → 78 → 78 → 77. '
    report += 'Это обработка входящего чата, не FPS или время кадра. Новые положительные случаи '
    report += 'выполняют дополнительный перевод и не являются равной работой. Все раунды сохранены.\n\n'
    report += '| Сценарий | Изменение времени, два порядка | Память до → после, байт/сообщение |\n| --- | ---: | ---: |\n'
    for item in benchmark['summary']:
        changes = ' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in item['orders'])
        memory = ' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in item['orders'])
        report += f'| {item["mode"]} | {changes} | {memory} |\n'
    report += '\n## Статус выпуска\n\nЭто проверенная партия исходников и отчёт, не обещание готовности всего мода. '
    report += 'В рабочей сборке остаётся прежний широкий аудит описаний предметов; автоматический '
    report += 'прогон не заменяет его смысловую вычитку. Установленная игра не менялась, JAR этим пакетом не устанавливается и не выдаётся за финальный.\n\n'
    report += f'Проверенный рабочий JAR: SHA-256 `{fingerprint}`.\n'
    report_path.write_text(report, encoding='utf-8')
    # Whole-corpus working ledger, deliberately OUTSIDE runtime resources.
    # Presence/technical output/fixture review are separate, never a guessed
    # machine-translated target or an inflated "all translated" flag.
    queue = rows(args.corpus / 'translation_queue.jsonl')
    assert len(queue) == 16244 and sha(args.corpus / 'translation_queue.jsonl') == after['queue_sha256']
    exact = defaultdict(set)
    for path in sorted((ROOT / 'src/main/resources/data/witherskyru').glob('*.json')):
        if path.name == 'npc-styles78.json':
            continue
        for r in load(path).get('records', []):
            if r.get('source') and r.get('translation'):
                exact[r['source']].add(r['translation'])
    new_sources = {r['source'] for r in batch}
    fixed_sources = {r['source'] for r in fixes}
    pending_styles = {r['source'] for r in remaining}
    previous_reviewed = {r['source'] for r in load(ROOT / 'src/main/resources/data/witherskyru/npc-expansion77.json')['records']}
    per_speaker = defaultdict(list)
    registered_count = 0
    with (SOURCE / 'NPC_WORKLIST_0.15.78.jsonl').open('w', encoding='utf-8', newline='\n') as output:
        for r in queue:
            source = r['source']
            registered = bool(exact.get(source))
            registered_count += registered
            review = ('authored_and_fixture_checked78' if source in new_sources else
                'old_target_retained_published_styles_checked78' if source in fixed_sources else
                'published_style_review_required' if source in pending_styles else
                'authored_and_fixture_checked77' if source in previous_reviewed else
                'exact_key_present_quality_not_certified' if registered else 'translation_review_required')
            runtime = now.get(source)
            item = {k: r.get(k) for k in ('source', 'tag', 'speaker', 'source_url', 'revision', 'occurrence_ids',
                'variable_definition', 'unresolved_markup', 'nonstandard_tag_layout', 'historical_heading', 'alpha_page')}
            item.update({'original_queue_status': r['status'], 'exact_registration': registered,
                'registered_targets_not_automatically_certified': sorted(exact.get(source, set())), 'review_status': review,
                'runtime_flat_snapshot_not_quality_proof': runtime['flat_result'] if runtime else None,
                'runtime_colored_snapshot_not_quality_proof': runtime['published_style_result'] if runtime else None})
            output.write(json.dumps(item, ensure_ascii=False) + '\n')
            per_speaker[r.get('speaker') or '(без указанного говорящего)'].append(item)
    assert registered_count == 1848
    table = ['# Вся очередь перевода по обозначениям говорящих — 0.15.78', '',
        '16 244 строки. Процент — только наличие точного ключа, не оценка качества и не полнота живых серверных веток. '
        'Условные имена и строки без говорящего сохранены; это не перечень самостоятельных действующих NPC. '
        'Структурные флаги переменных не сброшены даже при наличии спорного точного ключа.', '',
        '| Обозначение | Всего | Точных ключей | Доля ключей | Новые тексты / цвета78 | Цветных проблем | С переменными |',
        '| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for speaker, values in sorted(per_speaker.items()):
        registered = sum(v['exact_registration'] for v in values)
        safe = speaker.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('|', '\\|').replace('\n', ' ')
        table.append(f'| {safe} | {len(values)} | {registered} | {100*registered/len(values):.2f}% | '
            f'{sum(v["source"] in new_sources for v in values)} / {sum(v["source"] in fixed_sources for v in values)} | '
            f'{sum(v["source"] in pending_styles for v in values)} | {sum(bool(v["variable_definition"]) for v in values)} |')
    (SOURCE / 'NPC_ALL_SPEAKERS_0.15.78_RU.md').write_text('\n'.join(table) + '\n', encoding='utf-8')
    destination = args.destination.resolve()
    if destination.exists():
        raise RuntimeError('Refusing to overwrite an existing delivery')
    destination.mkdir(parents=True)
    files = ['translation-source/NPC_PROGRESS_0.15.78_RU.md', 'translation-source/NPC_PROGRESS_0.15.78.json',
        'translation-source/NPC_0.15.78_REVIEWED.tsv', 'translation-source/NPC_STYLES_0.15.78_REVIEWED.tsv',
        'translation-source/NPC_SOURCES_0.15.78.json', 'translation-source/NPC_STYLE_REVIEW_2026_10_16.json',
        'translation-source/NPC_STYLE_REMAINING_0.15.78.json',
        'translation-source/NPC_WORKLIST_0.15.78.jsonl', 'translation-source/NPC_ALL_SPEAKERS_0.15.78_RU.md',
        'src/main/resources/data/witherskyru/npc-expansion78.json', 'src/main/resources/data/witherskyru/npc-styles78.json',
        'src/main/resources/META-INF/licenses/NPC78-SOURCES.txt', 'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',
        'tests/TestNpc78.java', 'tests/TestNpcStyles78.java', 'tests/AuditNpcCorpus77.java', 'tests/BenchmarkNpc78.java',
        'tools/prepare_npc78.py', 'tools/check_npc78.ps1', 'tools/benchmark_npc78.ps1', 'tools/finalize_npc78_progress.py']
    for name in files:
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, target)
    for name in tests:
        target = destination / 'checks' / f'{name}-output.txt'
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(args.checks / target.name, target)
    shutil.copy2(args.checks / 'CHECK_SCOPE.json', destination / 'checks/CHECK_SCOPE.json')
    shutil.copy2(args.benchmark / 'RAW.json', destination / 'BENCHMARK_RAW.json')
    for index, old in enumerate(args.previous_benchmark, 1):
        historical = load(old / 'RAW.json')
        assert len(historical['records']) == 4
        target = destination / 'benchmark-history' / f'{index}-before-final-optimization.json'
        target.parent.mkdir(exist_ok=True)
        shutil.copy2(old / 'RAW.json', target)
    for version in ('0.15.77-dev', '0.15.78-dev'):
        target = destination / 'dispatch' / version
        target.mkdir(parents=True)
        shutil.copy2(args.audit / version / 'DISPATCH_SUMMARY.json', target)
    (destination / 'README_RU.md').write_text('# Вторая партия перевода публичной базы NPC\n\n'
        '[Подробная таблица и проверки](translation-source/NPC_PROGRESS_0.15.78_RU.md)\n\n'
        '[Вся очередь по говорящим](translation-source/NPC_ALL_SPEAKERS_0.15.78_RU.md)\n\n'
        '324/324 опубликованные реплики Тал Кера переведены; 58 прежних цветных вариантов исправлены. '
        'Общий счётчик точных ключей: 1 848/16 244, 11,38%. Это не процент полностью проверенной игры.\n\n'
        'Пакет содержит исходники партии, условия/ревизии, атрибуцию и результаты 132 групп проверок. '
        'Это не полный проект или финальный установочный JAR. Установленная игра не менялась.\n', encoding='utf-8')
    hashes = {str(p.relative_to(destination)).replace('\\', '/'): sha(p) for p in sorted(destination.rglob('*')) if p.is_file()}
    write(destination / 'HASHES.json', hashes)
    archive = destination / 'NPC_TRANSLATION_BATCH_02.zip'
    members = [p for p in sorted(destination.rglob('*')) if p.is_file() and p != archive]
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
        for p in members:
            bundle.write(p, str(p.relative_to(destination)).replace('\\', '/'))
    with zipfile.ZipFile(archive) as bundle:
        assert bundle.testzip() is None and len(bundle.infolist()) == len(members)
        assert all(bundle.read(str(p.relative_to(destination)).replace('\\', '/')) == p.read_bytes() for p in members)
    delivery = {'scope': 'Bounded source/progress package, not a final installable mod release',
        'new_rows': 324, 'fixed_style_sources': 58, 'remaining_style_candidates': 87,
        'fresh_test_groups': 132, 'tested_jar_sha256_not_distributed': fingerprint,
        'archive_sha256': sha(archive), 'archive_bytes': archive.stat().st_size, 'archive_entries': len(members),
        'installed_game_modified': False, 'game_fps_measured': False}
    write(destination / 'DELIVERY.json', delivery)
    print(json.dumps({'destination': str(destination), **delivery}, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
