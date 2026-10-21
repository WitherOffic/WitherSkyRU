"""Validate and publish the performance-only candidate; never touch an installed game."""
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESKTOP = Path('C:/Users/Wither/Desktop/WitherSkyRU')
DEST = DESKTOP / 'OPTIMIZATION_BATCH_2026-10-19_1'
CHECKS = ROOT / 'build/opt94-checks-90d2b9dba1da42fba339980016087ed8'
BENCH = ROOT / 'build/opt94-benchmark-5cac5a26eb9545d38c02b12a40b7e3f8'
PARTIAL = ROOT / 'build/opt94-benchmark-bedc61636ac54d84aed1ea7f775b726d'
PROBE = ROOT / 'build/opt94-probe-44e4f0586f104106b70d6b164321506d'
AUDIT = ROOT / 'build/opt94-dispatch-aa7b22e5153a41f28ba1306e9850dba1'
PREVIOUS = DESKTOP / 'NPC_TRANSLATION_BATCH_2026-10-18_6'
BASE = '502D91F03B30262A11499DA0C7DF31195455273DD43F68BA68243C18911C24DF'
FINAL = 'A784B75D65EFBCB8246C72CEE0AE7B60525EBDA9AA07B664127BC39A547BAC00'
QUEUE = 'FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def copy(source, relative):
    target = DEST / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def main():
    jar = ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar'
    baseline = DESKTOP / 'WitherSkyRU-26.1.2-fabric-0.15.93-dev.jar'
    published_jar = DESKTOP / jar.name
    assert not DEST.exists() and not published_jar.exists(), 'Never overwrite a delivery'
    assert sha(jar) == FINAL and sha(baseline) == BASE
    for name, digest in load(PREVIOUS / 'HASHES.json').items():
        assert sha(PREVIOUS / name) == digest, ('Earlier delivery changed', name)

    checks = load(CHECKS / 'CHECK_SCOPE.json')
    tests = checks['tests_passed']
    assert len(tests) == len(set(tests)) == 162
    assert checks['runtime_jar_sha256'] == FINAL and not checks['targeted_only']
    assert checks['reused_successes'] == [] and not checks['installed_pack_modified']
    assert not checks['live_fps_or_frame_times_measured']
    assert set(load(PREVIOUS / 'checks/CHECK_SCOPE.json')['tests_passed']) < set(tests)
    for name in tests:
        assert (CHECKS / (name + '-output.txt')).is_file()
    assertion_text = (CHECKS / 'ru.wither.skyru.TestOptimization94-output.txt').read_text(encoding='utf-8-sig')
    assert 'optimization94 328 assertions' in assertion_text and 'no retained style/component snapshots' in assertion_text
    native = (CHECKS / 'TestHudMixins-output.txt').read_text(encoding='utf-8-sig')
    assert native.count('PASS: actual transformed chat draw forwards original args/actions, nested restoration and exception cleanup:') == 3
    parser = (CHECKS / 'TestNpcCorpusParser94-output.txt').read_text(encoding='utf-8-sig')
    assert 'Ran 15 tests' in parser and '\nOK' in parser

    expected_changed = ['fabric.mod.json', 'ru/wither/skyru/NpcDialogueTranslator$Anchor.class',
                        'ru/wither/skyru/NpcDialogueTranslator$StyleCorrections.class',
                        'ru/wither/skyru/NpcDialogueTranslator.class', 'ru/wither/skyru/SkyRuClient.class',
                        'witherskyru.mixins.json']
    expected_added = ['ru/wither/skyru/NpcDialogueTranslator$Input.class',
                      'ru/wither/skyru/mixin/ChatClickableDrawMixin.class',
                      'ru/wither/skyru/mixin/ChatDrawMixin.class']
    with zipfile.ZipFile(baseline) as before, zipfile.ZipFile(jar) as after:
        assert after.testzip() is None
        old, new = set(before.namelist()), set(after.namelist())
        assert not old - new
        changed = sorted(name for name in old & new if before.read(name) != after.read(name))
        added = sorted(new - old)
        assert changed == expected_changed and added == expected_added, (changed, added)
        assert not any(name.startswith('net/minecraft/') for name in new), 'Test fixture must never ship'
        data = [name for name in old if name.startswith('data/') and not name.endswith('/')]
        assert data and all(before.read(name) == after.read(name) for name in data)
        assert before.read('ru/wither/skyru/RenderContext.class') == after.read('ru/wither/skyru/RenderContext.class')
        assert json.loads(after.read('fabric.mod.json'))['version'] == '0.15.94-dev'
        assert {'ChatDrawMixin', 'ChatClickableDrawMixin'} <= set(json.loads(after.read('witherskyru.mixins.json'))['client'])
    jar_diff = dict(changed=changed, added=added, removed=[], unchanged_data_resources=len(data),
                    rejected_context_experiment_absent=True, minecraft_fixture_absent=True)

    audit_summaries = [load(AUDIT / version / 'DISPATCH_SUMMARY.json') for version in ('0.15.93-dev', '0.15.94-dev')]
    for summary, digest in zip(audit_summaries, (BASE, FINAL)):
        assert summary['runtime_jar_sha256'] == digest and summary['queue_sha256'] == QUEUE
        assert (summary['tested_literal_rows'], summary['flat_changed_not_quality_proof'],
                summary['published_style_changed_not_quality_proof']) == (11247, 9237, 9165)
    dispatch = []
    for version in ('0.15.93-dev', '0.15.94-dev'):
        dispatch.append([json.loads(line) for line in (AUDIT / version / 'DISPATCH_ROWS.jsonl').read_text(encoding='utf-8-sig').splitlines()])
    assert len(dispatch[0]) == len(dispatch[1]) == 11247
    assert dispatch[0] == dispatch[1], 'Every output and row field must match, not only totals'
    equivalence = dict(rows=11247, all_row_fields_identical=True, remaining_old_style_gaps=72,
                       before_rows_sha256=sha(AUDIT / '0.15.93-dev/DISPATCH_ROWS.jsonl'),
                       after_rows_sha256=sha(AUDIT / '0.15.94-dev/DISPATCH_ROWS.jsonl'),
                       scope='Exact saved literal corpus subset; not all live branches or semantic certification')

    benchmark = load(BENCH / 'RAW.json')
    assert benchmark['runtime_jar_sha256'] == FINAL
    families = benchmark['families']
    for family, count in (('receipt', 33), ('hud', 10), ('draw', 3)):
        records = families[family]['records']
        assert len(families[family]['summary']) == count
        assert [r['version'] for r in records] == ['0.15.93-dev', '0.15.94-dev', '0.15.94-dev', '0.15.93-dev']
        for record in records:
            assert record['jar_sha256'] == (BASE if record['version'] == '0.15.93-dev' else FINAL)
            results = {k: v for k, v in record['results'].items() if k != 'scope'}
            assert len(results) == count
            for value in results.values():
                cpu_key = 'rounds_thread_cpu_ns' if family == 'hud' else 'rounds_cpu_ns'
                assert all(len(value[k]) == 4 for k in ('rounds_ns', cpu_key, 'rounds_bytes'))
                if family == 'receipt':
                    assert value['equal_useful_output']
        # Independently recompute both order comparisons from the original records.
        for summary in families[family]['summary']:
            mode = summary['mode']
            unit = 'message' if family == 'receipt' else 'operation'
            for order, pair in zip(summary['orders'], ((0, 1), (3, 2))):
                old_result, new_result = (records[i]['results'][mode] for i in pair)
                assert order['before_ns'] == old_result['ns_per_' + unit]
                assert order['after_ns'] == new_result['ns_per_' + unit]
                assert abs(order['wall_change_percent'] - 100 * (order['after_ns'] / order['before_ns'] - 1)) < 1e-9
    assert 'signer information' in (PARTIAL / 'draw/0-output.txt').read_text(encoding='utf-8-sig')

    source_files = [
        'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java',
        'src/main/java/ru/wither/skyru/SkyRuClient.java',
        'src/main/java/ru/wither/skyru/TechnicalText.java',
        'src/main/java/ru/wither/skyru/RenderContext.java',
        'src/main/java/ru/wither/skyru/mixin/ChatDrawMixin.java',
        'src/main/java/ru/wither/skyru/mixin/ChatClickableDrawMixin.java',
        'src/main/resources/witherskyru.mixins.json', 'src/main/resources/fabric.mod.json', 'build.gradle',
        'tests/TestOptimization94.java', 'tests/TestHudMixins.java', 'tests/BenchmarkOptimization94.java',
        'tests/BenchmarkProtectedChat94.java', 'tests/BenchmarkPrepared72.java', 'tests/BenchmarkContext67.java',
        'tests/fixtures94/net/minecraft/client/gui/components/ChatComponent.java',
        'tests/AuditNpcCorpus77.java', 'tests/test_npc_corpus77.py',
        'tools/check_optimization94.ps1', 'tools/benchmark_optimization94.ps1',
        'tools/dispatch_optimization94.ps1', 'tools/probe_opt94.ps1', 'tools/finalize_optimization94.py',
    ]
    assert all((ROOT / relative).is_file() for relative in source_files)
    DEST.mkdir()
    for relative in source_files:
        copy(ROOT / relative, relative)
    copy(jar, jar.name)
    for name in tests:
        for suffix in ('-output.txt', '-stderr.txt'):
            path = CHECKS / (name + suffix)
            if path.is_file() and (suffix == '-output.txt' or path.stat().st_size):
                copy(path, 'checks/' + path.name)
    for name in ('CHECK_SCOPE.json', 'TestNpcCorpusParser94-output.txt', 'compile-arguments.txt'):
        copy(CHECKS / name, 'checks/' + name)
    copy(BENCH / 'RAW.json', 'BENCHMARK_RAW.json')
    for family in ('receipt', 'hud', 'draw'):
        for path in (BENCH / family).glob('*'):
            if path.is_file() and path.suffix in ('.json', '.txt'):
                copy(path, 'benchmark/' + family + '/' + path.name)
    # Keep rejected/failed measurement attempts visibly separate from the accepted run.
    copy(PROBE / 'CONTEXT_RAW.json', 'rejected-context-experiment/CONTEXT_RAW.json')
    for family in ('receipt', 'hud', 'draw'):
        for path in (PARTIAL / family).glob('*'):
            if path.is_file() and path.suffix in ('.json', '.txt'):
                copy(path, 'initial-partial-harness-run/' + family + '/' + path.name)
    for version in ('0.15.93-dev', '0.15.94-dev'):
        for name in ('DISPATCH_SUMMARY.json', 'DISPATCH_ROWS.jsonl'):
            copy(AUDIT / version / name, 'dispatch/' + version + '/' + name)
    save(DEST / 'JAR_DIFF.json', jar_diff)
    save(DEST / 'DISPATCH_EQUIVALENCE.json', equivalence)

    report = [
        '# WitherSkyRU 0.15.94-dev — оптимизация', '',
        'Подготовлено 19 октября 2026. Сравнение с 0.15.93-dev. Эта версия меняет обработку текста, а не базу переводов.', '',
        '## Результат', '',
        '- Повторный разбор строки у закрытых обработчиков NPC заменён одним снимком текста на получение сообщения. Снимок живёт только внутри этого вызова; компоненты, цвета, команды и подсказки между сообщениями не кэшируются.',
        '- Расположения авторских цветовых привязок вычисляются при загрузке базы. Убраны потоки и лишнее построение объектов стилей при проверке каждого символа.',
        '- Три штатных пути показа чата явно передают уже подготовленные строки без повторной классификации переводчиком. Это узкая область одного вызова, не глобальное отключение чата/HUD. Получение и перевод новых сообщений сохраняются. F3 остаётся исходным.',
        '- В выбранных тестах обработки сообщений время уменьшилось на 4–31% в обоих порядках запуска; цветная реплика: около 9,05 → 6,28–6,41 мкс, временные выделения 39 280 → 31 632 байта на сообщение. Это не проценты игрового FPS.', '',
        '## Проверки', '',
        '- 162 группы Java-проверок на итоговом JAR, без переноса старых успехов; дополнительно 15 проверок выгрузки базы.',
        '- Новая проверка: 328 утверждений — единый разбор строки, время, счётчики повторов, свежие действия, изменение исходного текста, защита игроков/подписанных сообщений и отсутствие удержания объектов сообщения.',
        '- Реально преобразованы классы Minecraft 26.1.2: три новых перехвата показа чата. Прямой вызов преобразованных обработчиков проверяет исходные аргументы/действия, вложенность и очистку области после исключения. Это не запуск всей пользовательской сборки.',
        '- Все поля результатов 11 247 сохранённых реплик идентичны версии 93, включая обычный и опубликованный цветной варианты. 9 237 обычных / 9 165 цветных вариантов меняют исходный английский текст; 72 прежних цветовых пропуска остаются. Это не оценка полной готовности игры.',
        f'- Все {len(data)} прежних файлов data/ внутри JAR побайтно совпадают. Новых переводов в этой версии нет. Учёт публичной очереди прежний: 9 276 / 16 244 точных ключей (57,10%), без ключа 6 968. Это не процент всей игры.',
        '- Тестовый класс net.minecraft из измерительного стенда отсутствует в моде. Прежние классы HUD/F3/RenderContext не изменены; TechnicalText имеет только новый комментарий в исходнике.', '',
        '## Как измерялось', '',
        '12 отдельных JVM: для каждой из трёх групп порядок 93 → 94 → 94 → 93. На сценарий — прогрев и четыре измерительных раунда; исходные wall-time, CPU-time и выделения памяти сохранены в BENCHMARK_RAW.json и benchmark/. Время ниже — медиана раундов в каждом JVM, не доверительный интервал. Измерения закрытого чата выполняют одинаковую полезную работу в обеих версиях. Запись неизвестных переводов отключена только в стендах, не в настройках игры.', '',
        'Группа draw изолирует входную проверку переводчика в исходно защищённом чате через тестовый класс с точным именем вызывающего класса и отдельный загрузчик. Она НЕ измеряет шрифт, штатный сборщик текста, GPU, частоту вызовов в кадре или сам FPS. Здесь падение накладных расходов с ~1,4 мкс до ~0,01 мкс не означает ускорение всей отрисовки чата на 99%. Создание свежих событий включено одинаково в обе версии.', '',
        'Обычный HUD не показал устойчивого общего ускорения: отдельные сценарии колеблются в обе стороны. Например свежие цвета/действия без потребления вывода: +16,9% / +2,6%, но разница всего ~14 / 2 нс, память та же. Динамический счётчик с потреблением вывода: −0,8% / +3,0%, также без выигрыша памяти. Эти результаты не скрываются и не объявляются улучшением.', '',
        '## Все сценарии', '',
        'В каждой паре указан отдельный порядок сравнения: сначала 93→94, затем 94→93 (числа всё равно «версия 93 → версия 94»). Память — байты выделений на сообщение/операцию, не удержанная память. Отрицательный процент означает меньшее время.', '',
        '| Группа / сценарий | Мкс: 93 → 94, порядок 1 | Изменение | Байты: 93 → 94 | Мкс: 93 → 94, порядок 2 | Изменение | Байты: 93 → 94 |',
        '| --- | ---: | ---: | ---: | ---: | ---: | ---: |',
    ]
    for family in ('receipt', 'hud', 'draw'):
        for row in families[family]['summary']:
            cells = []
            for order in row['orders']:
                cells.extend((f"{order['before_ns']/1000:.4f} → {order['after_ns']/1000:.4f}",
                              f"{order['wall_change_percent']:+.1f}%",
                              f"{order['before_bytes']:.0f} → {order['after_bytes']:.0f}"))
            report.append('| ' + family + ' / ' + row['mode'] + ' | ' + ' | '.join(cells) + ' |')
    report += [
        '', '## Отброшенные варианты и ограничения', '',
        'Отдельная проба нового обхода стека RenderContext добавляла примерно 176 байт на вызов и не дала устойчивого выигрыша на коротком стеке. Она отменена: итоговый класс побайтно совпадает с версией 93. Сырые данные — rejected-context-experiment/.', '',
        'Первый полный стенд успешно выполнил восемь JVM (receipt/HUD), но не завершился: тестовый класс ChatComponent конфликтовал с подписанными классами Minecraft при запуске draw. Это ошибка изоляции стенда, не ошибка игрового JAR. Стенд исправлен отдельным загрузчиком; затем все 12 JVM повторены с нуля. Первая незавершённая запись и ошибка сохранены в initial-partial-harness-run/, не смешаны с итоговой статистикой.', '',
        'Нулевое влияние на FPS не доказано и не обещается. Эта работа не измеряла живые кадры, 1%/0,1% lows, GPU или взаимодействие всех модов пользовательской сборки. Само отсутствие новых ошибок в проверках не исключает живых несовместимостей. Для подтверждения плавности нужен одинаковый игровой маршрут до/после с замером времени кадров. Версия 93 сохранена для отката.', '',
        '## Файлы', '',
        f'JAR: `{jar.name}`  ', f'SHA-256: `{FINAL}`', '',
        'Мод автоматически НЕ установлен. Каталог игры, сохранения и настройки не менялись. Этот пакет содержит JAR, выбранные изменённые исходники, тесты и отчёты; это не полный исходный проект. Повторный запуск стендов требует полного проекта, Java 25 и тех же библиотек Minecraft/Fabric. HASHES.json проверяет содержимое архива; DELIVERY.json снаружи дополнительно фиксирует хеш архива.', '',
    ]
    (DEST / 'OPTIMIZATION_REPORT_RU.md').write_text('\n'.join(report), encoding='utf-8')
    (DEST / 'README_RU.md').write_text('# Оптимизация 0.15.94-dev\n\n[Отчёт и все замеры](OPTIMIZATION_REPORT_RU.md).\n\n162 группы + 15 проверок; 11 247 результатов без изменений; данные переводов прежние. Не замер игрового FPS. JAR не установлен автоматически. Тестовый класс Minecraft — только исходник стенда, не часть мода.\n', encoding='utf-8')
    hashes = {p.relative_to(DEST).as_posix(): sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()}
    save(DEST / 'HASHES.json', hashes)
    archive = DEST / 'OPTIMIZATION_0.15.94.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as bundle:
        for name in sorted(hashes):
            bundle.write(DEST / name, name)
        bundle.write(DEST / 'HASHES.json', 'HASHES.json')
    shutil.copy2(jar, published_jar)
    assert sha(published_jar) == FINAL
    delivery = dict(runtime='0.15.94-dev', jar=str(published_jar), runtime_jar_sha256=FINAL,
                    archive_sha256=sha(archive), manifest_files=len(hashes), tests=162,
                    parser_tests=15, benchmark_jvms=12, dispatch_rows_identical=11247,
                    installed_game_modified=False, live_frame_times_measured=False)
    save(DEST / 'DELIVERY.json', delivery)
    print(json.dumps(dict(destination=str(DEST), **delivery), ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
