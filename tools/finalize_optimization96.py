"""Publish the exact164-group-tested96 candidate; no installed-game writes."""
import argparse
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESKTOP = Path('C:/Users/Wither/Desktop/WitherSkyRU')
DEST = DESKTOP / 'OPTIMIZATION_BATCH_2026-10-19_3'
PREVIOUS = DESKTOP / 'OPTIMIZATION_BATCH_2026-10-19_2'
CHECKS = ROOT / 'build/opt96-checks-316b4448c15c41e687fe88161dcda883'
AUDIT = ROOT / 'build/opt96-dispatch-1deb2f743f3d47a9bb619b4ebeb4eeca'
BYTECODE = ROOT / 'build/opt96-bytecode-5f0482d48d7d426d969cce31a640c000'
BASE = 'E5D397D17E7F7ADF9EAD2C9BF7E607B134F2412EF89CB4D951CB78944825BCBF'
FINAL = '97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1'


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


def validate_benchmark(path, counts=None, order=None):
    raw = load(path / 'RAW.json')
    assert raw['runtime_jar_sha256'] == FINAL
    if counts is None:
        counts = (('receipt', 33), ('hud', 10), ('mixed', 5))
    if order is None:
        order = ['0.15.95-dev', '0.15.96-dev', '0.15.96-dev', '0.15.95-dev']
    pairs = ((0, 1), (3, 2)) if len(order) == 4 else ((0, 1), (3, 2), (4, 5))
    for family, count in counts:
        item = raw['families'][family]
        assert len(item['summary']) == count
        assert [r['version'] for r in item['records']] == order
        for record in item['records']:
            assert record['jar_sha256'] == (BASE if record['version'] == '0.15.95-dev' else FINAL)
            values = {k: v for k, v in record['results'].items() if k != 'scope'}
            assert len(values) == count
            for mode, result in values.items():
                cpu = 'rounds_thread_cpu_ns' if family == 'hud' else 'rounds_cpu_ns'
                assert all(len(result[k]) == 4 for k in ('rounds_ns', cpu, 'rounds_bytes'))
                if family == 'receipt':
                    assert result['equal_useful_output']
                if family == 'mixed':
                    assert result['preflight_glyph_style_snapshot'] == item['records'][0]['results'][mode]['preflight_glyph_style_snapshot']
        unit = 'message' if family == 'receipt' else 'operation'
        for row in item['summary']:
            assert len(row['orders']) == len(pairs)
            for comparison, (old, new) in zip(row['orders'], pairs):
                assert comparison['before_ns'] == item['records'][old]['results'][row['mode']]['ns_per_' + unit]
                assert comparison['after_ns'] == item['records'][new]['results'][row['mode']]['ns_per_' + unit]
                assert comparison['before_bytes'] == item['records'][old]['results'][row['mode']]['bytes_per_' + unit]
                assert comparison['after_bytes'] == item['records'][new]['results'][row['mode']]['bytes_per_' + unit]
                assert abs(comparison['wall_change_percent'] - 100 * (comparison['after_ns'] / comparison['before_ns'] - 1)) < 1e-9
    return raw


def table(raw):
    lines = []
    for family in ('mixed', 'hud', 'receipt'):
        if family not in raw['families']:
            continue
        lines += ['', '### ' + family, '', '| Сценарий | Порядок | Мкс: 95 → 96 | Изменение времени | Байты: 95 → 96 |', '| --- | ---: | ---: | ---: | ---: |']
        for row in raw['families'][family]['summary']:
            for i, comparison in enumerate(row['orders'], 1):
                lines.append(f"| {row['mode']} | {i} | {comparison['before_ns']/1000:.4f} → {comparison['after_ns']/1000:.4f} | {comparison['wall_change_percent']:+.1f}% | {comparison['before_bytes']:.0f} → {comparison['after_bytes']:.0f} |")
    return lines


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--benchmark', type=Path, required=True)
    parser.add_argument('--focused', type=Path, required=True)
    args = parser.parse_args()
    jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.96-dev.jar'
    baseline = DESKTOP / 'WitherSkyRU-26.1.2-fabric-0.15.95-dev.jar'
    published_jar = DESKTOP / jar.name
    assert not DEST.exists() and not published_jar.exists(), 'Never overwrite a delivery'
    assert sha(jar) == FINAL and sha(baseline) == BASE
    for name, digest in load(PREVIOUS / 'HASHES.json').items():
        assert sha(PREVIOUS / name) == digest, ('Previous delivery changed', name)
    scope = load(CHECKS / 'CHECK_SCOPE.json')
    tests = scope['tests_passed']
    assert len(tests) == len(set(tests)) == 164 and scope['runtime_jar_sha256'] == FINAL
    assert set(load(PREVIOUS / 'checks/CHECK_SCOPE.json')['tests_passed']) < set(tests)
    assert not scope['targeted_only'] and scope['reused_successes'] == []
    assert not scope['installed_pack_modified'] and not scope['live_fps_or_frame_times_measured']
    text = (CHECKS / 'ru.wither.skyru.TestOptimization96-output.txt').read_text(encoding='utf-8-sig')
    assert 'optimization96 491027 assertions' in text and '61369 parser rows including all 36066 exact keys' in text and '7200 fresh styled sequences' in text
    text = (CHECKS / 'TestNpcCorpusParser96-output.txt').read_text(encoding='utf-8-sig')
    assert 'Ran 15 tests' in text and '\nOK' in text
    native = (CHECKS / 'TestHudMixins-output.txt').read_text(encoding='utf-8-sig')
    assert native.count('PASS: actual transformed chat draw forwards original args/actions, nested restoration and exception cleanup:') == 3
    before, after = [load(AUDIT / v / 'DISPATCH_SUMMARY.json') for v in ('0.15.95-dev', '0.15.96-dev')]
    assert before['runtime_jar_sha256'] == BASE and after['runtime_jar_sha256'] == FINAL
    assert before['queue_sha256'] == after['queue_sha256'] == 'FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    old_rows, new_rows = [[json.loads(line) for line in (AUDIT / v / 'DISPATCH_ROWS.jsonl').read_text(encoding='utf-8-sig').splitlines()] for v in ('0.15.95-dev', '0.15.96-dev')]
    assert old_rows == new_rows and len(new_rows) == 11247
    assert (after['flat_changed_not_quality_proof'], after['published_style_changed_not_quality_proof']) == (9237, 9165)
    bytecode = load(BYTECODE / 'RESULT.json')
    expected_changes = ['fabric.mod.json', 'ru/wither/skyru/InterfaceDisplay.class', 'ru/wither/skyru/OfflineDictionary.class']
    assert bytecode['runtime_jar_sha256'] == FINAL and bytecode['baseline_jar_sha256'] == BASE and bytecode['changed_entries'] == expected_changes
    assert not bytecode['added_entries'] and not bytecode['removed_entries'] and bytecode['minecraft_fixture_absent']
    assert bytecode['skyru_client_and_all_mixin_classes_byte_identical']
    with zipfile.ZipFile(jar) as candidate, zipfile.ZipFile(baseline) as previous:
        assert candidate.testzip() is None and set(previous.namelist()) == set(candidate.namelist())
        assert sorted(n for n in previous.namelist() if previous.read(n) != candidate.read(n)) == expected_changes
        assert json.loads(candidate.read('fabric.mod.json'))['version'] == '0.15.96-dev'
    raw = validate_benchmark(args.benchmark)
    focused = validate_benchmark(args.focused, counts=(('hud', 5), ('receipt', 6)), order=['0.15.95-dev', '0.15.96-dev', '0.15.96-dev', '0.15.95-dev', '0.15.95-dev', '0.15.96-dev'])
    highlights = []
    for family, mode, title in [('hud', 'dynamic_counter_consume', 'Динамический счётчик Gems с чтением итоговых символов'), ('mixed', 'numeric_uniform_consume', 'Чередование девяти числовых подписей'), ('mixed', 'fresh_labels_actions_consume', 'Те же подписи со свежими цветами и действиями')]:
        measurements = focused if family == 'hud' else raw
        orders = next(r for r in measurements['families'][family]['summary'] if r['mode'] == mode)['orders']
        gains = [-r['wall_change_percent'] for r in orders]
        memory = [100 * (1-r['after_bytes']/r['before_bytes']) for r in orders]
        assert min(gains) > 0 and min(memory) > 0, ('Hot-path improvement not repeated', mode)
        highlights.append(f'- {title}: время меньше на {min(gains):.1f}–{max(gains):.1f}%, временные выделения меньше на {min(memory):.1f}–{max(memory):.1f}% в {len(orders)} парах сравнения. Это результат отдельного API, не прирост FPS.')
    sources = ['build.gradle', 'src/main/resources/fabric.mod.json', 'src/main/java/ru/wither/skyru/InterfaceDisplay.java', 'src/main/java/ru/wither/skyru/OfflineDictionary.java', 'tests/TestOptimization96.java', 'tests/BenchmarkDynamicHud96.java', 'tests/BenchmarkOptimization94.java', 'tests/BenchmarkOptimization96.java', 'tests/BenchmarkPrepared72.java', 'tests/BenchmarkHudFocus95.java', 'tests/BenchmarkReceiptFocus96.java', 'tests/AuditNpcCorpus77.java', 'tests/TestHudMixins.java', 'tools/check_optimization96.ps1', 'tools/dispatch_optimization96.ps1', 'tools/benchmark_optimization96.ps1', 'tools/benchmark_optimization96_focused.ps1', 'tools/derive_optimization96_benchmark.py', 'tools/audit_optimization96.py', 'tools/finalize_optimization96.py']
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
    for name in ('CHECK_SCOPE.json', 'compile-arguments.txt', 'TestNpcCorpusParser96-output.txt'):
        copy(CHECKS / name, 'checks/' + name)
    for label, root, measurements in (('initial', args.benchmark, raw), ('focused', args.focused, focused)):
        copy(root / 'RAW.json', 'benchmark/' + label + '/RAW.json')
        for family in measurements['families']:
            for p in (root / family).glob('*'):
                if p.is_file() and p.suffix in ('.json', '.txt'):
                    copy(p, 'benchmark/' + label + '/' + family + '/' + p.name)
    for p in BYTECODE.iterdir():
        if p.is_file():
            copy(p, 'bytecode/' + p.name)
    for version in ('0.15.95-dev', '0.15.96-dev'):
        for name in ('DISPATCH_ROWS.jsonl', 'DISPATCH_SUMMARY.json'):
            copy(AUDIT / version / name, 'dispatch/' + version + '/' + name)
    save(DEST / 'DISPATCH_EQUIVALENCE.json', dict(rows=11247, all_row_fields_identical=True, old_style_gaps_retained=72, before_rows_sha256=sha(AUDIT / '0.15.95-dev/DISPATCH_ROWS.jsonl'), after_rows_sha256=sha(AUDIT / '0.15.96-dev/DISPATCH_ROWS.jsonl')))
    report = [
        '# WitherSkyRU 0.15.96-dev — оптимизация разбора HUD', '',
        '19 октября 2026. Сравнение с уже оптимизированной 0.15.95-dev. База переводов и набор функций не менялись.', '',
        '## Что изменилось', '',
        '- Числовые подписи Sawdust, Cleanup, Gems, North Stars, Coins/h, Farming XP/h, Farming Level, Yaw и Pitch выбираются до проверки неподходящих форм предметов/коллекций. Быстрая проверка только отвергает заведомо чужие префиксы; каждое принятие по-прежнему подтверждает прежний полный regex с прежними границами длины, пробелов и символов числа.',
        '- Для этой формы не строятся многочисленные заведомо неподходящие Matcher. План содержит те же диапазоны замены подписи; числовой фрагмент, его текущие цвета и действия не меняются. Старый вариант бонуса Sawdust имеет ту же подпись и диапазон; пересечение проверено.',
        '- При проверке уже созданного плана InterfaceDisplay используется этот локальный план, без повторного вызова парсера/создания такого же списка. Обычный текст по-прежнему сверяется с действующим словарём. Новых кэшей или удержания объектов сообщения между кадрами нет.', '',
        '## Подтверждённые узкие измерения', '',
    ] + highlights + [
        '', 'Время и выделения памяти измерены на отдельных вызовах обработки строк. Они не равны времени кадра, FPS, 1%/0,1% lows или нагрузке GPU. Ни полная отрисовка шрифта, ни частота вызовов в игре не измерялись. Нулевое влияние на FPS не доказано.', '',
        '## Проверка сохранности', '',
        '- Все 164 группы Java-проверок на одном итоговом JAR прошли заново; дополнительно 15 проверок выгрузки базы.',
        '- Новый тест: 491 027 утверждений на 61 369 строках парсера, включая все 36 066 точных ключей словаря, границы длины/пробелов/знаков числа, неподходящий регистр, legacy-цвета, Unicode, случайные и повреждённые строки. Сравнение с фактически доставленным JAR 95: полная допустимость формы, списки ключей, диапазоны замен и обычный текст совпали.',
        '- 7 200 свежих последовательностей с разными цветами/командами/подсказками и разделением строки сравниваются посимвольно, включая индексы и исходную идентичность. Пустые, посторонние, частичные и полные пользовательские замены сохраняют прежний приоритет. Проверены также отсутствующая и неизменённая подписи.',
        '- Все поля 11 247 сохранённых результатов NPC-корпуса совпали с 95. Прежние 72 цветовых пропуска не исправлялись; доля точных ключей очереди прежняя: 9 276 / 16 244 (57,10%), а не процент всей игры.',
        f"- Все {bytecode['unchanged_data_resources']} data/-ресурсов JAR побайтно совпадают. Менялись только InterfaceDisplay.class, OfflineDictionary.class и номер версии. SkyRuClient, все перехваты отрисовки, F3, защитные области чата и кэши побайтно прежние. В OfflineDictionary менялся только вызов проверки локального плана в findDisplayEdits; это подтверждено разбором исполняемого байткода после нормализации номеров constant-pool, не машинного кода JIT.",
        '- Новых перехватов и тестовых net.minecraft-классов в моде нет. Существующие три перехвата чата прошли реальное преобразование классов Minecraft и проверку очистки областей после исключения.', '',
        '## Методика и все результаты', '',
        'Основной прогон: 12 отдельных JVM, три группы, каждая в порядке 95 → 96 → 96 → 95, всего 48 сценариев. Длинный повтор: ещё 12 JVM, две группы, каждая 95 → 96 → 96 → 95 → 95 → 96, всего 11 выбранных сценариев. Для каждого режима четыре измерительных раунда после прогрева. В таблицах числа всегда «версия 95 → версия 96»; порядок 2 запускался наоборот. Время — медиана раундов конкретной JVM, не доверительный интервал. Все результаты, включая ухудшения и колебания вне целевых путей, приведены ниже без отбора.', '',
        'В первом прогоне одна партия чата87 показала аномальные −51% в первом порядке и −1,4% в обратном. Такой разброс не принят за доказательство массового ускорения чата; эта партия включена в длинный повтор. Статичный HUD и фрагментированные строки также дали разнонаправленные/положительные изменения времени (например +24,2% статичной подписи в одном порядке, +5,9%/+7,3% фрагментированного вывода). Их повторные результаты сохранены полностью. Вне целевых динамических форм ускорение не обещается.', '',
        'В длинном повторе статичные подписи показали +0,6…+10,1% (примерно +1…+12 нс), фрагментированный вывод +0,1…+4,7% (примерно +0,2…+15 нс), без роста выделений памяти. Это не объявляется ускорением. Непосредственный байткод прежнего пути готового кэшированного вывода не менялся, но это не гарантирует одинаковое машинное исполнение/JIT и не отменяет измеренные колебания. Партия чата87 в повторе дала лишь −0,9…−2,2%, а не −51%.', '',
        'Прямой HUD использует одинаковый фиксированный контекст. Outside включает обход стека. Исходные меняющиеся числа/свежие действия создаются одинаково; consume-режимы читают итоговые символы. Дополнительные mixed-режимы чередуют девять подписей, включают шесть/семь начальных пробелов, разные цвета подписи/значения и посторонние переопределения. Их контрольные снимки текста, индексов и стилей до измерения совпадают во всех четырёх JVM. CPU-счётчики Windows грубые; исходные wall-time/CPU/байты сохранены. Запись неизвестных строк отключена только в стендах, не в настройках игры.',
    ] + table(raw) + ['', '## Длинный повтор — все результаты'] + table(focused) + [
        '', '## Файлы', '', f'JAR: `{jar.name}`  ', f'SHA-256: `{FINAL}`', '',
        'JAR автоматически не установлен. Игра, сохранения и настройки не менялись. Предыдущая версия 95 и её архив сохранены для сравнения/отката. Для проверки плавности нужны одинаковые место, настройки и маршрут, несколько чередующихся прогонов и запись времени кадров; одиночное число FPS не подтверждает отсутствие микролагов.', '',
        'Пакет содержит JAR, выбранные исходники, тесты и сырые отчёты, а не полный проект с библиотеками. Для повторения стендов нужны полный проект и Java 25. HASHES.json фиксирует файлы внутри архива; DELIVERY.json рядом — сам архив.', '',
    ]
    (DEST / 'OPTIMIZATION_REPORT_RU.md').write_text('\n'.join(report), encoding='utf-8')
    (DEST / 'README_RU.md').write_text('# Оптимизация 0.15.96-dev\n\n[Отчёт и все измерения](OPTIMIZATION_REPORT_RU.md).\n\n164 группы + 15 проверок; 61 369 строк парсера без расхождений. Быстрый выбор прежних числовых форм HUD и повторное использование локального плана. Не измерение игрового FPS. Автоматической установки нет.\n', encoding='utf-8')
    hashes = {p.relative_to(DEST).as_posix(): sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()}
    save(DEST / 'HASHES.json', hashes)
    archive = DEST / 'OPTIMIZATION_0.15.96.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as bundle:
        for name in sorted(hashes):
            bundle.write(DEST / name, name)
        bundle.write(DEST / 'HASHES.json', 'HASHES.json')
    shutil.copy2(jar, published_jar)
    assert sha(published_jar) == FINAL
    delivery = dict(runtime='0.15.96-dev', jar=str(published_jar), runtime_jar_sha256=FINAL, archive_sha256=sha(archive), manifest_files=len(hashes), fresh_java_groups=164, parser_tests=15, measured_jvms=24, benchmark_modes=48, focused_modes=11, all_dispatch_rows_identical=11247, installed_game_modified=False, live_frame_times_measured=False)
    save(DEST / 'DELIVERY.json', delivery)
    print(json.dumps(dict(destination=str(DEST), **delivery), ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
