"""Publish a bounded NPC translation progress package, not a finished mod release."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--checks', type=Path, required=True)
    parser.add_argument('--benchmark', type=Path, required=True)
    parser.add_argument('--audit', type=Path, required=True)
    parser.add_argument('--destination', type=Path, required=True)
    args = parser.parse_args()
    checks = json.loads((args.checks / 'CHECK_SCOPE.json').read_text(encoding='utf-8-sig'))
    jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.77-dev.jar'
    fingerprint = sha(jar)
    assert checks['runtime_jar_sha256'] == fingerprint and not checks['targeted_only']
    names = checks['tests_passed']
    assert len(names) == len(set(names)) == 130
    assert {'TestNpc77', 'TestLoreBoundary77', 'TestEnchantmentExpansion', 'TestReviewedLoreIndex'} <= set(names)
    benchmark = json.loads((args.benchmark / 'RAW.json').read_text(encoding='utf-8-sig'))
    assert len(benchmark['records']) == 4
    assert all(r['jar_sha256'] == fingerprint for r in benchmark['records'] if r['version'] == '0.15.77-dev')
    audit = json.loads((args.audit / '0.15.77-dev/DISPATCH_SUMMARY.json').read_text(encoding='utf-8'))
    assert audit['runtime_jar_sha256'] == fingerprint
    progress = json.loads((ROOT / 'translation-source/NPC_PROGRESS_2026_10_16.json').read_text(encoding='utf-8'))
    assert progress['new_reviewed_rows'] == 137 and progress['exact_resource_after'] == 1524
    assert progress['dispatch_after']['runtime_jar_sha256'] == fingerprint
    resource = ROOT / 'src/main/resources/data/witherskyru/npc-expansion77.json'
    assert progress['batch_resource_sha256'] == sha(resource)
    with zipfile.ZipFile(jar) as runtime:
        assert runtime.read('data/witherskyru/npc-expansion77.json') == resource.read_bytes()
        assert runtime.read('META-INF/licenses/NPC77-SOURCES.txt') == (ROOT / 'src/main/resources/META-INF/licenses/NPC77-SOURCES.txt').read_bytes()
        baseline_jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.76-dev.jar'
        assert sha(baseline_jar) == '9C1E7A9F3AADF17EC8522792776661B933DC3200D0CA6DF93832A955A2871957'
        with zipfile.ZipFile(baseline_jar) as baseline:
            old_resources = [name for name in baseline.namelist() if not name.endswith('/')
                and name.startswith(('data/', 'assets/', 'META-INF/licenses/'))]
            assert all(baseline.read(name) == runtime.read(name) for name in old_resources)
            changed_classes = [name for name in baseline.namelist() if name.endswith('.class')
                and baseline.read(name) != runtime.read(name)]
            assert all(name == 'ru/wither/skyru/NpcDialogueTranslator.class'
                or name.startswith('ru/wither/skyru/ReviewedLoreStyles') for name in changed_classes)
    destination = args.destination.resolve()
    if destination.exists():
        raise RuntimeError('Refusing to overwrite a previously delivered progress package')
    destination.mkdir(parents=True)
    files = ['translation-source/NPC_PROGRESS_2026_10_16_RU.md', 'translation-source/NPC_PROGRESS_2026_10_16.json',
        'translation-source/NPC_0.15.77_REVIEWED.tsv', 'translation-source/NPC_SOURCES_0.15.77.json',
        'translation-source/NPC_STYLE_REVIEW_2026_10_16.json',
        'src/main/resources/data/witherskyru/npc-expansion77.json',
        'src/main/resources/META-INF/licenses/NPC77-SOURCES.txt',
        'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',
        'tests/TestNpc77.java', 'tests/TestLoreBoundary77.java', 'tests/TestReviewedLoreIndex.java',
        'tests/AuditNpcCorpus77.java', 'tests/BenchmarkNpc77.java',
        'tools/prepare_npc77.py', 'tools/check_npc77.ps1', 'tools/benchmark_npc77.ps1',
        'tools/finalize_npc77_progress.py']
    for name in files:
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, target)
    for name in names:
        target = destination / 'checks' / f'{name}-output.txt'
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(args.checks / target.name, target)
    shutil.copy2(args.checks / 'CHECK_SCOPE.json', destination / 'checks/CHECK_SCOPE.json')
    shutil.copy2(args.benchmark / 'RAW.json', destination / 'BENCHMARK_RAW.json')
    for version in ('0.15.76-dev', '0.15.77-dev'):
        target = destination / 'dispatch' / version
        target.mkdir(parents=True)
        shutil.copy2(args.audit / version / 'DISPATCH_SUMMARY.json', target)
    lines = ['# Первая партия перевода публичной базы NPC', '',
        '[Подробная таблица прогресса](translation-source/NPC_PROGRESS_2026_10_16_RU.md)', '',
        'В исходники мода добавлено 137 вычитанных точных реплик для 25 обозначений NPC. '
        'Это начало переноса базы, не перевод всех 16 244 строк и не подтверждение всех живых серверных веток.', '',
        'Все 130 групп автоматических проверок прошли заново на одном JAR; новая партия NPC — '
        '66 808 проверок. Дополнительно исправлена граница короткого описания Syphon: '
        'неизвестное продолжение больше не допускает частичный захват абзаца. '
        'Отдельно проверены 123 карточки зачарований и сравнение индексированного поиска с последовательным.', '',
        'Глобальный автоматический проход — 11 247 буквальных реплик, простые и опубликованные '
        'цветные варианты. За пределами новой партии результаты до/после дословно совпали. '
        '145 прежних расхождений между обычным и цветным текстом сохранены отдельно, не объявлены исправленными.', '',
        '## Нагрузка обработчика чата', '',
        'Четыре отдельные JVM в порядке 76 → 77 → 77 → 76. Все исходные замеры сохранены. '
        'В новых положительных случаях выполняется дополнительный перевод; это не одинаковая работа. '
        'Эта проверка не измеряет FPS, графику, плавность кадров или 0,1% lows.', '',
        '| Сценарий | Изменение времени, два порядка | Выделение памяти до → после, байт/сообщение |',
        '| --- | ---: | ---: |']
    for item in benchmark['summary']:
        orders = item['orders']
        changes = ' / '.join(f'{o["wall_change_percent"]:+.1f}%' for o in orders)
        allocations = ' / '.join(f'{o["before_bytes"]:.0f} → {o["after_bytes"]:.0f}' for o in orders)
        lines.append(f'| {item["mode"]} | {changes} | {allocations} |')
    lines += ['', 'Новые записи добавлены в существующую карту при запуске. Новых кадровых '
        'обработчиков, динамических регулярных выражений или фоновых сетевых запросов нет. '
        'Нулевое влияние на FPS не обещается; игровые замеры этой версии не проводились.', '',
        '## Статус сборки', '',
        'Этот пакет содержит таблицы, исходники партии, происхождение строк и результаты проверок. '
        'Установленная игра не менялась. JAR сюда намеренно не включён: в рабочей 0.15.77 также '
        'сохраняется прежний незавершённый аудит описаний предметов, который требует отдельной '
        'смысловой вычитки. Прохождение тестов не делает его автоматически полностью готовым релизом. '
        'Последний ранее выданный релиз — 0.15.76-dev.', '',
        f'Проверенный рабочий JAR: SHA-256 `{fingerprint}`.', '',
        'Оригинал: участники Hypixel SkyBlock Wiki; точные ревизии указаны у каждой строки. '
        'Русские адаптации — CC BY-NC-SA 3.0; файл атрибуции приложен. '
        'Это не выгрузка закрытых скриптов Hypixel.', '']
    (destination / 'README_RU.md').write_text('\n'.join(lines), encoding='utf-8')
    hashes = {str(path.relative_to(destination)).replace('\\', '/'): sha(path)
        for path in sorted(destination.rglob('*')) if path.is_file()}
    (destination / 'HASHES.json').write_text(json.dumps(hashes, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    archive = destination / 'NPC_TRANSLATION_BATCH_01.zip'
    members = [path for path in sorted(destination.rglob('*')) if path.is_file() and path != archive]
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
        for path in members:
            bundle.write(path, str(path.relative_to(destination)).replace('\\', '/'))
    with zipfile.ZipFile(archive) as bundle:
        assert bundle.testzip() is None and len(bundle.infolist()) == len(members)
        for path in members:
            assert bundle.read(str(path.relative_to(destination)).replace('\\', '/')) == path.read_bytes()
    delivery = {'scope': 'Progress/evidence/source batch, NOT installable or final whole-mod release',
        'jar_sha256_tested_not_distributed': fingerprint, 'archive_sha256': sha(archive),
        'archive_bytes': archive.stat().st_size, 'archive_entries': len(members),
        'fresh_test_groups': len(names), 'new_rows': 137, 'new_speaker_identities': 25,
        'prior_resource_files_byte_identical_to_76': len(old_resources),
        'changed_runtime_classes_vs_76': changed_classes,
        'installed_game_modified': False, 'game_fps_measured': False}
    (destination / 'DELIVERY.json').write_text(json.dumps(delivery, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'destination': str(destination), **delivery}, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
