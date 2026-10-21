"""Publish only the tested performance-only95 JAR; no installed-game writes."""
import argparse
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESKTOP = Path('C:/Users/Wither/Desktop/WitherSkyRU')
DEST = DESKTOP / 'OPTIMIZATION_BATCH_2026-10-19_2'
PREVIOUS = DESKTOP / 'OPTIMIZATION_BATCH_2026-10-19_1'
CHECKS = ROOT / 'build/opt95-checks-321c57ebb7bd4f0985d7000a63a9355b'
BENCH = ROOT / 'build/opt95-benchmark-7b39f68569bd4001b5b878c10f0be447'
AUDIT = ROOT / 'build/opt95-dispatch-e42b3eb1d26f45caa308bb151a1dd0bc'
BASE = 'A784B75D65EFBCB8246C72CEE0AE7B60525EBDA9AA07B664127BC39A547BAC00'
FINAL = 'E5D397D17E7F7ADF9EAD2C9BF7E607B134F2412EF89CB4D951CB78944825BCBF'


def load(p):
    return json.loads(p.read_text(encoding='utf-8-sig'))


def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest().upper()


def save(p, value):
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def copy(p, relative):
    target = DEST / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, target)


def validate_benchmark(path, family_counts, order):
    raw = load(path / 'RAW.json')
    assert raw['runtime_jar_sha256'] == FINAL
    for family, count in family_counts.items():
        item = raw['families'][family]
        assert len(item['summary']) == count
        assert [r['version'] for r in item['records']] == order
        for record in item['records']:
            assert record['jar_sha256'] == (BASE if record['version'] == '0.15.94-dev' else FINAL)
            values = {k: v for k, v in record['results'].items() if k != 'scope'}
            assert len(values) == count
            for result in values.values():
                cpu = 'rounds_thread_cpu_ns' if family == 'hud' else 'rounds_cpu_ns'
                assert all(len(result[k]) == 4 for k in ('rounds_ns', cpu, 'rounds_bytes'))
                if family in ('receipt', 'narrow'):
                    assert result['equal_useful_output']
        pairs = ((0, 1), (3, 2)) if len(order) == 4 else ((0, 1), (3, 2), (4, 5))
        unit = 'message' if family == 'receipt' else 'operation'
        for row in item['summary']:
            assert len(row['orders']) == len(pairs)
            for comparison, (old, new) in zip(row['orders'], pairs):
                assert comparison['before_ns'] == item['records'][old]['results'][row['mode']]['ns_per_' + unit]
                assert comparison['after_ns'] == item['records'][new]['results'][row['mode']]['ns_per_' + unit]
                assert abs(comparison['wall_change_percent'] - 100 * (comparison['after_ns'] / comparison['before_ns'] - 1)) < 1e-9
    return raw


def tables(raw):
    lines = []
    for family in ('hud', 'narrow', 'receipt'):
        if family not in raw['families']:
            continue
        lines += ['', '### ' + family, '', '| Сценарий | Порядок | Мкс: 94 → 95 | Изменение времени | Байты: 94 → 95 |', '| --- | ---: | ---: | ---: | ---: |']
        for row in raw['families'][family]['summary']:
            for i, comparison in enumerate(row['orders'], 1):
                lines.append(f"| {row['mode']} | {i} | {comparison['before_ns']/1000:.4f} → {comparison['after_ns']/1000:.4f} | {comparison['wall_change_percent']:+.1f}% | {comparison['before_bytes']:.0f} → {comparison['after_bytes']:.0f} |")
    return lines


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--focused', type=Path, required=True)
    parser.add_argument('--bytecode', type=Path, required=True)
    args = parser.parse_args()
    jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.95-dev.jar'
    published_jar = DESKTOP / jar.name
    assert not DEST.exists() and not published_jar.exists(), 'Never overwrite deliveries'
    assert sha(jar) == FINAL and sha(DESKTOP / 'WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar') == BASE
    for name, digest in load(PREVIOUS / 'HASHES.json').items():
        assert sha(PREVIOUS / name) == digest, ('Previous delivery changed', name)
    scope = load(CHECKS / 'CHECK_SCOPE.json')
    tests = scope['tests_passed']
    assert len(tests) == len(set(tests)) == 163 and scope['runtime_jar_sha256'] == FINAL
    assert set(load(PREVIOUS / 'checks/CHECK_SCOPE.json')['tests_passed']) < set(tests)
    assert not scope['targeted_only'] and scope['reused_successes'] == []
    assert not scope['installed_pack_modified'] and not scope['live_fps_or_frame_times_measured']
    text = (CHECKS / 'ru.wither.skyru.TestOptimization95-output.txt').read_text(encoding='utf-8-sig')
    assert 'optimization95 152494 assertions' in text and 'all 36066 exact keys plus 12600 dynamic/context rows' in text
    text = (CHECKS / 'TestNpcCorpusParser95-output.txt').read_text(encoding='utf-8-sig')
    assert 'Ran 15 tests' in text and '\nOK' in text
    native = (CHECKS / 'TestHudMixins-output.txt').read_text(encoding='utf-8-sig')
    assert native.count('PASS: actual transformed chat draw forwards original args/actions, nested restoration and exception cleanup:') == 3
    before, after = [load(AUDIT / v / 'DISPATCH_SUMMARY.json') for v in ('0.15.94-dev', '0.15.95-dev')]
    assert before['runtime_jar_sha256'] == BASE and after['runtime_jar_sha256'] == FINAL
    assert before['queue_sha256'] == after['queue_sha256'] == 'FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    old_rows, new_rows = [[json.loads(line) for line in (AUDIT / v / 'DISPATCH_ROWS.jsonl').read_text(encoding='utf-8-sig').splitlines()] for v in ('0.15.94-dev', '0.15.95-dev')]
    assert old_rows == new_rows and len(new_rows) == 11247
    assert (after['flat_changed_not_quality_proof'], after['published_style_changed_not_quality_proof']) == (9237, 9165)
    bytecode = load(args.bytecode / 'RESULT.json')
    assert bytecode['runtime_jar_sha256'] == FINAL and bytecode['baseline_jar_sha256'] == BASE
    assert bytecode['changed_entries'] == ['fabric.mod.json', 'ru/wither/skyru/OfflineDictionary$ReorderedTitle.class', 'ru/wither/skyru/OfflineDictionary$TitleEdit.class', 'ru/wither/skyru/OfflineDictionary.class', 'ru/wither/skyru/SkyRuClient.class']
    assert bytecode['source_line_only_record_classes'] == ['OfflineDictionary$TitleEdit', 'OfflineDictionary$ReorderedTitle']
    assert not bytecode['added_entries'] and not bytecode['removed_entries'] and bytecode['minecraft_fixture_absent']
    with zipfile.ZipFile(jar) as candidate, zipfile.ZipFile(DESKTOP / 'WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar') as previous:
        assert candidate.testzip() is None
        assert set(previous.namelist()) == set(candidate.namelist())
        changed = sorted(n for n in previous.namelist() if previous.read(n) != candidate.read(n))
        assert changed == bytecode['changed_entries']
        assert json.loads(candidate.read('fabric.mod.json'))['version'] == '0.15.95-dev'
    raw = validate_benchmark(BENCH, {'receipt': 33, 'hud': 10, 'narrow': 4}, ['0.15.94-dev', '0.15.95-dev', '0.15.95-dev', '0.15.94-dev'])
    focused = validate_benchmark(args.focused, {'receipt': 6, 'hud': 5}, ['0.15.94-dev', '0.15.95-dev', '0.15.95-dev', '0.15.94-dev', '0.15.94-dev', '0.15.95-dev'])
    dynamic = next(row for row in focused['families']['hud']['summary'] if row['mode'] == 'dynamic_counter_consume')['orders']
    dynamic_gain = [-row['wall_change_percent'] for row in dynamic]
    assert min(dynamic_gain) > 0
    sources = ['build.gradle', 'src/main/resources/fabric.mod.json', 'src/main/java/ru/wither/skyru/OfflineDictionary.java', 'src/main/java/ru/wither/skyru/SkyRuClient.java', 'tests/TestOptimization95.java', 'tests/BenchmarkNarrow95.java', 'tests/BenchmarkOptimization94.java', 'tests/BenchmarkPrepared72.java', 'tests/BenchmarkHudFocus95.java', 'tests/BenchmarkReceiptFocus95.java', 'tests/AuditNpcCorpus77.java', 'tests/TestHudMixins.java', 'tools/check_optimization95.ps1', 'tools/dispatch_optimization95.ps1', 'tools/benchmark_optimization95.ps1', 'tools/benchmark_optimization95_focused.ps1', 'tools/derive_optimization95_focus.py', 'tools/audit_optimization95.py', 'tools/finalize_optimization95.py']
    assert all((ROOT / name).is_file() for name in sources)
    DEST.mkdir()
    for name in sources:
        copy(ROOT / name, name)
    copy(jar, jar.name)
    for name in tests:
        for suffix in ('-output.txt', '-stderr.txt'):
            path = CHECKS / (name + suffix)
            if path.is_file() and (suffix == '-output.txt' or path.stat().st_size):
                copy(path, 'checks/' + path.name)
    for name in ('CHECK_SCOPE.json', 'compile-arguments.txt', 'TestNpcCorpusParser95-output.txt'):
        copy(CHECKS / name, 'checks/' + name)
    for label, path in (('initial', BENCH), ('focused', args.focused)):
        copy(path / 'RAW.json', 'benchmark/' + label + '/RAW.json')
        for family in load(path / 'RAW.json')['families']:
            for p in (path / family).glob('*'):
                if p.is_file() and p.suffix in ('.json', '.txt'):
                    copy(p, 'benchmark/' + label + '/' + family + '/' + p.name)
    for p in args.bytecode.iterdir():
        if p.is_file():
            copy(p, 'bytecode/' + p.name)
    for version in ('0.15.94-dev', '0.15.95-dev'):
        for name in ('DISPATCH_ROWS.jsonl', 'DISPATCH_SUMMARY.json'):
            copy(AUDIT / version / name, 'dispatch/' + version + '/' + name)
    save(DEST / 'DISPATCH_EQUIVALENCE.json', dict(rows=11247, all_row_fields_identical=True, old_style_gaps_retained=72, before_rows_sha256=sha(AUDIT / '0.15.94-dev/DISPATCH_ROWS.jsonl'), after_rows_sha256=sha(AUDIT / '0.15.95-dev/DISPATCH_ROWS.jsonl')))
    report = [
        '# WitherSkyRU 0.15.95-dev — второй проход оптимизации', '',
        '19 октября 2026. Базовая версия для сравнения — уже оптимизированная 0.15.94-dev. Новых переводов и новых кэшей в этой версии нет.', '',
        '## Изменения', '',
        '- При пустом неизменяемом наборе пользовательских переопределений больше не строятся списки потенциальных ключей для проверки этого пустого набора. Пропуск применяется только там, где прежняя проверка заведомо возвращала false. Порядок перевода, границы шаблонов и защитные правила не меняются.',
        '- При сокращении HUD с пустым набором переопределений больше не обходится исходная строка для той же заведомо отрицательной проверки. Сокращение русского результата и его текущие цвета/действия выполняются как прежде. При любом пользовательском переопределении используется прежний путь.',
        '- Набор переопределений не превращён в глобальный флаг: он остаётся свойством конкретного словаря. Новая загрузка словаря заново учитывает файл. Пользовательские функции не отключались.', '',
        '## Подтверждённый результат и границы', '',
        '- В первом полном сравнении динамический счётчик HUD выделял около 16,7 → 14,3 КБ временных объектов на вызов (примерно на 14% меньше); при потреблении итоговых символов — около 17,1 → 14,7 КБ. Это объём выделений, не удержанная память и не FPS.',
        f'- В длинном повторе динамического счётчика с потреблением символов время уменьшилось на {min(dynamic_gain):.1f}–{max(dynamic_gain):.1f}% во всех трёх парах. Выделения составляли около 16,8 → 14,4 КБ. Это узкий сценарий с постоянно новым числом, не общий прирост FPS.',
        '- Сокращение подписи без переопределений: около 1,61–1,64 → 0,53–0,56 мкс, примерно на 65–68% меньше времени. Динамическая подпись: около 1,19 → 0,49 мкс. Стенд изолирует этот API и не измеряет отрисовку шрифта/GPU или частоту вызовов в игре.',
        '- Первые замеры других путей колебались в обе стороны: например фрагментированный вывод +12,5%/+28,6%, а один порядок динамического вывода с чтением символов +12,7%, второй −7,8%. Эти данные сохранены. Для неоднозначных путей проведён отдельный длинный повтор с тремя парами сравнений; он приведён ниже целиком, без выбора только удачных запусков.',
        '- Обработка NPC, подсказки и стабильные подписи не объявляются ускоренными этим вторым проходом. Реальный FPS, 1%/0,1% lows и микролаги пользовательской сборки здесь не измерялись; нулевое влияние на FPS не доказано.', '',
        '## Проверка сохранности', '',
        '- Все 163 группы Java-проверок на одном итоговом JAR прошли заново; дополнительно 15 проверок выгрузки.',
        '- Новый тест: 152 494 утверждения. Сравнены все 36 066 ключей словаря и 12 600 дополнительных динамических/контекстных строк с доставленной версией 94: текст, диапазоны замен и политика отказа при смешанных стилях совпадают. Проверены пустые, посторонние и значимые пользовательские переопределения.',
        '- Свежие цвета, события нажатия, наведение, индексы символов, исходная идентичность, смена словаря и F3 проверены отдельно. При действительно пустом наборе переопределений исходная строка для сокращения не обходится.',
        '- Все поля 11 247 результатов сохранённого NPC-корпуса совпадают с версией 94. Прежние 72 цветовых пропуска не исправлялись. Учёт перевода прежний: 9 276 / 16 244 ключей публичной очереди (57,10%), а не всей игры.',
        f"- Все {bytecode['unchanged_data_resources']} прежних data/-ресурсов побайтно совпадают. Логика изменена только в OfflineDictionary.class и SkyRuClient.class; дополнительно изменились номер версии и номера исходных строк в двух вложенных классах-записях. Для этих записей полный verbose-разбор совпадает после исключения пути/хеша файла и номеров исходных строк. В SkyRuClient изменён только narrowHud; исполняемые инструкции остальных методов совпадают после нормализации номеров constant-pool. Это сравнение байткода, не машинного кода JIT.",
        '- Три перехвата показа чата из 94 сохранены и прошли реальное преобразование классов Minecraft/проверку очистки областей; новых перехватов в 95 нет. В моде нет тестовых классов net.minecraft.', '',
        '## Методика измерения', '',
        'Основной прогон: 12 JVM, три группы, каждая в порядке 94→95→95→94; 47 сценариев. Повтор: 12 JVM, две группы, каждая 94→95→95→94→94→95; 11 выбранных неоднозначных сценариев. В каждой JVM четыре измерительных раунда после прогрева. В таблицах числа всегда «версия 94 → версия 95», даже при обратном порядке запуска. Время — медиана раундов конкретной JVM, а не доверительный интервал.', '',
        'Динамические значения/свежие события создаются одинаково с обеих сторон. Прямые HUD-сценарии используют одинаковый фиксированный контекст; outside включает обход стека. Вывод потребляется только в явно обозначенных consume-сценариях. Учёт CPU в Windows грубый; первичные wall-time/CPU/байты сохранены. Запись неизвестных строк отключена только в стендах, не в настройках установленной игры.', '',
        '## Основной прогон — все результаты',
    ] + tables(raw) + ['', '## Длинный повтор — все результаты'] + tables(focused) + [
        '', '## Файлы и безопасная проверка в игре', '',
        f'JAR: `{jar.name}`  ', f'SHA-256: `{FINAL}`', '',
        'JAR автоматически не установлен. Игра, настройки и сохранения не менялись. Предыдущая версия 94 и её пакет сохранены. Для проверки плавности нужны одинаковые настройки, место и маршрут, несколько чередующихся прогонов с измерением времени кадров, а не одиночное число FPS. Для реального профиля использовать именно новую версию; данные от старого JAR не доказывают эффект новой.', '',
        'Пакет содержит JAR, выбранные исходники, тесты и сырые отчёты, но не полный проект/библиотеки Minecraft. Повторный запуск стендов требует полного проекта и Java 25. HASHES.json относится к содержимому архива, DELIVERY.json рядом фиксирует сам архив.', '',
    ]
    (DEST / 'OPTIMIZATION_REPORT_RU.md').write_text('\n'.join(report), encoding='utf-8')
    (DEST / 'README_RU.md').write_text('# Оптимизация 0.15.95-dev\n\n[Отчёт и все замеры](OPTIMIZATION_REPORT_RU.md).\n\n163 группы + 15 проверок; 24 измерительных JVM. Меньше временных объектов в динамическом HUD; обходы пустых переопределений исключены. Не измерение игрового FPS. Автоматической установки нет.\n', encoding='utf-8')
    hashes = {p.relative_to(DEST).as_posix(): sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()}
    save(DEST / 'HASHES.json', hashes)
    archive = DEST / 'OPTIMIZATION_0.15.95.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as bundle:
        for name in sorted(hashes):
            bundle.write(DEST / name, name)
        bundle.write(DEST / 'HASHES.json', 'HASHES.json')
    shutil.copy2(jar, published_jar)
    assert sha(published_jar) == FINAL
    delivery = dict(runtime='0.15.95-dev', jar=str(published_jar), runtime_jar_sha256=FINAL, archive_sha256=sha(archive), manifest_files=len(hashes), tests=163, parser_tests=15, measured_jvms=24, all_dispatch_rows_identical=11247, installed_game_modified=False, live_frame_times_measured=False)
    save(DEST / 'DELIVERY.json', delivery)
    print(json.dumps(dict(destination=str(DEST), **delivery), ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
