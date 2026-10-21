"""Keep rejected experiments auditable; verify the source rebuild matches delivered96 payload."""
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESKTOP = Path('C:/Users/Wither/Desktop/WitherSkyRU')
DEST = DESKTOP / 'OPTIMIZATION_AUDIT_2026-10-19_1'
SNAPSHOT = ROOT / 'build/opt97-rejected-0df1d627e26346bebcca72b7f177ebba'
POST_CHECKS = ROOT / 'build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55'
FIRST_CHECKS = ROOT / 'build/opt97-checks-e0365317393d4469b1fa27e0c3824e46'
TARGETED = ROOT / 'build/opt97-targeted-463cd07156f346699798929e03012c3a'
BASE = '97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1'
REBUILT = 'F480E7F098B8FB4B3B5CE36D6BB4C2944006B9A0B7C1EBE460F4231FE197235B'
FIRST = 'FB97335C3B4B65529A4455836A7949FE50403285FF7DF99D03D70EA57710BCCC'
REVISED = '71C34F7067C6171C1DE70A54A529BBEC4EA79C8AAA19894D004FE13F45EFD816'
RUNS = [('initial', FIRST, ROOT / 'build/opt97-benchmark-599f99abc1fe490ea5a61c2addeb6074'),
        ('revised', REVISED, ROOT / 'build/opt97-benchmark-49c71aedf4844a44a854bf82898a95c5')]


def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest().upper()


def load(p):
    return json.loads(p.read_text(encoding='utf-8-sig'))


def save(p, data):
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def copy(p, relative):
    out = DEST / relative
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, out)


def main():
    assert not DEST.exists(), 'Never overwrite an audit'
    canonical = DESKTOP / 'WitherSkyRU-26.1.2-fabric-0.15.96-dev.jar'
    current = ROOT / 'build/libs' / canonical.name
    fresh = SNAPSHOT / 'restored96-fresh-build-f480e7f0.jar'
    assert sha(canonical) == sha(current) == BASE and sha(fresh) == REBUILT
    assert sha(SNAPSHOT / 'candidates/initial-fb97335c.jar') == FIRST
    assert sha(SNAPSHOT / 'candidates/revised-71c34f70.jar') == REVISED
    assert not (DESKTOP / 'WitherSkyRU-26.1.2-fabric-0.15.97-dev.jar').exists()
    assert not (ROOT / 'build/libs/WitherSkyRU-26.1.2-fabric-0.15.97-dev.jar').exists()
    with zipfile.ZipFile(canonical) as old, zipfile.ZipFile(fresh) as new:
        assert old.testzip() is None and new.testzip() is None
        assert old.namelist() == new.namelist()
        assert all(old.read(n) == new.read(n) for n in old.namelist()), 'Restored source must reproduce every class/resource byte'
        assert 'ru/wither/skyru/StyledRuns$Pool.class' not in new.namelist()
        payload = dict(all_entries_byte_identical=True, entries=len(old.namelist()),
                       old_jar_sha256=BASE, fresh_rebuild_jar_sha256=REBUILT,
                       container_sha_differs_but_payload_identical=True,
                       zip_timestamp_diff_observed=any(old.getinfo(n).date_time!=new.getinfo(n).date_time for n in old.namelist()),
                       canonical_tested_jar_restored_to_build=True)
    previous = DESKTOP / 'OPTIMIZATION_BATCH_2026-10-19_3'
    for name, digest in load(previous / 'HASHES.json').items():
        assert sha(previous / name) == digest, ('Previous delivery changed', name)
    assert sha(previous / 'OPTIMIZATION_0.15.96.zip') == load(previous / 'DELIVERY.json')['archive_sha256']
    post = load(POST_CHECKS / 'CHECK_SCOPE.json')
    assert post['runtime_jar_sha256'] == BASE and len(set(post['tests_passed'])) == len(post['tests_passed']) == 164
    assert not post['targeted_only'] and post['reused_successes'] == []
    assert 'Ran 15 tests' in (POST_CHECKS / 'TestNpcCorpusParser96-output.txt').read_text(encoding='utf-8-sig')
    first = load(FIRST_CHECKS / 'CHECK_SCOPE.json')
    assert first['runtime_jar_sha256'] == FIRST and len(first['tests_passed']) == 165 and not first['targeted_only']
    target = load(TARGETED / 'SCOPE.json')
    assert target['runtime_jar_sha256'] == REVISED and target['targeted_only']
    assert '653409 assertions' in (TARGETED / 'TestOptimization97-output.txt').read_text(encoding='utf-8-sig')
    raw_runs = []
    for label, digest, path in RUNS:
        raw = load(path / 'RAW.json')
        assert raw['runtime_jar_sha256'] == digest
        for family, count in (('receipt', 33), ('hud', 10), ('mixed', 5), ('scratch', 6)):
            item = raw['families'][family]
            assert len(item['summary']) == count
            assert [r['version'] for r in item['records']] == ['0.15.96-dev', '0.15.97-dev', '0.15.97-dev', '0.15.96-dev']
            for record in item['records']:
                assert record['jar_sha256'] == (BASE if record['version'] == '0.15.96-dev' else digest)
                rows = {k: v for k, v in record['results'].items() if k != 'scope'}
                assert len(rows) == count
                for mode, result in rows.items():
                    cpu = 'rounds_thread_cpu_ns' if family == 'hud' else 'rounds_cpu_ns'
                    assert all(len(result[k]) == 4 for k in ('rounds_ns', cpu, 'rounds_bytes'))
                    if family == 'receipt':
                        assert result['equal_useful_output']
                    if family in ('mixed', 'scratch'):
                        assert result['preflight_glyph_style_snapshot'] == item['records'][0]['results'][mode]['preflight_glyph_style_snapshot']
            unit = 'message' if family == 'receipt' else 'operation'
            for row in item['summary']:
                assert len(row['orders']) == 2
                for result, (a, b) in zip(row['orders'], ((0, 1), (3, 2))):
                    assert result['before_ns'] == item['records'][a]['results'][row['mode']]['ns_per_' + unit]
                    assert result['after_ns'] == item['records'][b]['results'][row['mode']]['ns_per_' + unit]
                    assert abs(result['wall_change_percent'] - 100 * (result['after_ns']/result['before_ns']-1)) < 1e-9
        raw_runs.append((label, path, raw))
    game_mod = Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft/mods/WitherSkyRU-26.1.2-fabric-0.15.92-dev.jar')
    assert sha(game_mod) == '2BF484EC13F14B7080AF9AE4B60A4FCDE37B65B38384CF70F66DA85EA9450304'
    with zipfile.ZipFile(game_mod) as z:
        assert json.loads(z.read('fabric.mod.json'))['version'] == '0.15.92-dev'
    DEST.mkdir()
    save(DEST / 'RESTORED96_PAYLOAD_VERIFICATION.json', payload)
    save(DEST / 'DECISION.json', dict(decision='reject_both_prototypes', released_version='0.15.96-dev', released_jar_sha256=BASE,
                                    initial_candidate_sha256=FIRST, revised_candidate_sha256=REVISED,
                                    measured_jvms=32, modes_per_candidate=54,
                                    installed_pack_version_observed='0.15.92-dev', installed_pack_modified=False,
                                    live_frame_times_measured=False, prototype_jars_not_distributed=True,
                                    initial_candidate_fresh_groups=165, revised_candidate_targeted_only=True,
                                    restored96_fresh_groups=164, parser_tests_after_restore=15))
    for label, path, raw in raw_runs:
        copy(path / 'RAW.json', 'benchmarks/' + label + '/RAW.json')
        for family in raw['families']:
            for p in (path / family).glob('*'):
                if p.is_file() and p.suffix in ('.json', '.txt'):
                    copy(p, 'benchmarks/' + label + '/' + family + '/' + p.name)
    for name in ('CHECK_SCOPE.json', 'ru.wither.skyru.TestOptimization97-output.txt', 'TestNpcCorpusParser97-output.txt'):
        copy(FIRST_CHECKS / name, 'checks-initial-prototype/' + name)
    for name in ('SCOPE.json', 'TestOptimization97-output.txt'):
        copy(TARGETED / name, 'checks-revised-prototype/' + name)
    copy(ROOT / 'build/opt97-checks-45c01bd46b0b4b54a23ed68684e43148/ru.wither.skyru.TestOptimization97-output.txt', 'initial-test-fixture-failure.txt')
    for name in ('CHECK_SCOPE.json', 'TestNpcCorpusParser96-output.txt'):
        copy(POST_CHECKS / name, 'checks-restored96/' + name)
    for name in post['tests_passed']:
        copy(POST_CHECKS / (name + '-output.txt'), 'checks-restored96/' + name + '-output.txt')
    for p in (SNAPSHOT / 'source').rglob('*'):
        if p.is_file():
            copy(p, 'rejected-source-snapshot/' + p.relative_to(SNAPSHOT / 'source').as_posix())
    for name in ('BenchmarkStyledScratch97.java', 'BenchmarkPrepared72.java', 'BenchmarkDynamicHud96.java', 'BenchmarkOptimization97.java'):
        copy(ROOT / 'tests' / name, 'harness-source/' + name)
    copy(ROOT / 'tools/report_rejected_optimization97.py', 'tools/report_rejected_optimization97.py')
    report = [
        '# Дополнительный аудит оптимизации — варианты не приняты', '',
        '19 октября 2026. Рабочая версия остаётся **0.15.96-dev**. Новый JAR 97 не выпущен. Исходники, настройки версии и рабочий артефакт возвращены к 96; её прежний опубликованный пакет не менялся.', '',
        '## Решение', '',
        '| Вариант | Идея | Что показало сравнение с 96 | Решение |',
        '| --- | --- | --- | --- |',
        '| A | Переиспользовать весь сборщик текста с очищаемым списком фрагментов | Памяти меньше, но длинный текст +24,8% / +36,9% времени; фрагментированный вывод с чтением символов +12,1% / +16,5% | Отклонён |',
        '| B | Переиспользовать только ограниченный буфер символов; состояние стилей остаётся новым | Нет надёжного общего выигрыша; фрагментированный сбор +5,9% / +13,4%, свежие действия с чтением символов +9,5% / +4,6%, динамический счётчик +1,2% / +1,4%. Отдельные режимы быстрее и/или выделяют меньше памяти | Отклонён |', '',
        'Проценты относятся к отдельным измерительным вызовам, не к игровому FPS. Короткие операции чувствительны к окружению/JIT, доверительный интервал здесь не рассчитывался. Эти записи не доказывают, что вся игра стала бы медленнее; они также не дают основания выпускать вариант как надёжное ускорение. Снижение выделений памяти само по себе не доказывает улучшение времени кадров.', '',
        '## Сохранность и проверки', '',
        '- Вариант A: 165 свежих групп проверок на своём конкретном JAR. Вариант B: отдельная свежая проверка на 653 409 утверждений, включая 36 066 ключей, Unicode, границы длины, вложенные вызовы, ошибки, освобождение данных, независимые потоки и старые обработчики. Полный прогон 165 групп для B не заявляется: после измерений вариант отклонён.',
        '- Первый запуск нового теста остановился из-за тестовой строки: смайлик был разделён между UTF-16-фрагментами, а ожидалась исходная неразделённая строка. Ошибка ожидания исправлена до успешных прогонов; диагностический вывод сохранён отдельно. Изменение игрового кода ради обхода этой проверки не делалось.',
        '- После возврата 96 заново прошли все 164 группы и 15 проверок выгрузки. Никакие переводы, возможности, цвета и команды не отключались ради результата.',
        '- Отдельно заново собранный проект даёт **побайтно те же классы и ресурсы** внутри JAR, что опубликованная 96. Хеш контейнера отличается, в том числе изменились ZIP-времена; свежая сборка сохранена в рабочем архиве, а в build/libs восстановлен точный ранее опубликованный/проверенный JAR 96. Сравнение всех файлов зафиксировано в RESTORED96_PAYLOAD_VERIFICATION.json.',
        '- Прототипы, исходники и измерения сохранены в рабочем каталоге. В этом пользовательском архиве намеренно нет устанавливаемого JAR 97.', '',
        '## Важное наблюдение об установленной сборке', '',
        'В проверенной папке PrismLauncher → **Skypixel (for Hypixel Skyblock)** лежит `WitherSkyRU-26.1.2-fabric-0.15.92-dev.jar`, её содержимое подтверждает 0.15.92-dev. Это относится именно к данной папке, не ко всем возможным экземплярам Minecraft. Файлы игры не менялись. Соответствующий запущенный процесс при проверке не обнаружен.', '',
        'Поэтому следующий полезный игровой замер должен подтверждать, что запущена именно 0.15.96-dev. Если используется другая папка сборки, сначала нужно уточнить её. Установка автоматически не выполнялась. По сравнению со скриншотом одного FPS полезнее одинаковый маршрут/настройки и запись времени кадров, после чего короткий профиль проблемного момента позволит выбрать реальный горячий участок.', '',
        '## Методика', '',
        'Два независимых набора по 16 JVM: 4 группы, каждая 96 → прототип → прототип → 96. По 54 режима на вариант, в каждом четыре измерительных раунда после прогрева. Сохранены wall-time, CPU-счётчики и выделения памяти. Для mixed/scratch контрольные снимки символов/индексов/стилей до измерения совпали. Источник/свежие события создаются одинаково; обозначенные consume-режимы читают результат. Прямые HUD-режимы используют фиксированный контекст, outside включает обход стека. Запись неизвестных строк отключена только в стендах, не в игре. Шрифт, GPU, частота вызовов, живые FPS и 1%/0,1% lows не измерялись.', '',
    ]
    for label, path, raw in raw_runs:
        report += ['## Все результаты — ' + ('вариант A' if label == 'initial' else 'вариант B'), '']
        for family in ('scratch', 'hud', 'mixed', 'receipt'):
            report += ['### ' + family, '', '| Режим | Порядок | Мкс: 96 → прототип | Изменение времени | Байты: 96 → прототип |', '| --- | ---: | ---: | ---: | ---: |']
            for row in raw['families'][family]['summary']:
                for i, r in enumerate(row['orders'], 1):
                    report.append(f"| {row['mode']} | {i} | {r['before_ns']/1000:.4f} → {r['after_ns']/1000:.4f} | {r['wall_change_percent']:+.1f}% | {r['before_bytes']:.0f} → {r['after_bytes']:.0f} |")
            report.append('')
    report += ['## Проверенная текущая сборка', '', f'0.15.96-dev SHA-256: `{BASE}`', '', 'Игра и её настройки не менялись. Отчёт не подтверждает нулевое влияние на FPS. HASHES.json описывает файлы внутри архива; DELIVERY.json рядом фиксирует сам архив.', '']
    (DEST / 'OPTIMIZATION_AUDIT_RU.md').write_text('\n'.join(report), encoding='utf-8')
    (DEST / 'README_RU.md').write_text('# Аудит оптимизации\n\n[Решение и все замеры](OPTIMIZATION_AUDIT_RU.md).\n\nОба прототипа отклонены. Рабочая версия — 0.15.96-dev. JAR 97 в архив не включён; снимок исходников — только для разбора эксперимента. Файлы установленной игры не менялись.\n', encoding='utf-8')
    hashes = {p.relative_to(DEST).as_posix(): sha(p) for p in sorted(DEST.rglob('*')) if p.is_file()}
    save(DEST / 'HASHES.json', hashes)
    archive = DEST / 'OPTIMIZATION_AUDIT_REJECTED97.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        for name in sorted(hashes):
            z.write(DEST / name, name)
        z.write(DEST / 'HASHES.json', 'HASHES.json')
    result = dict(destination=str(DEST), archive_sha256=sha(archive), manifest_files=len(hashes), decision='keep96_reject97', installed_game_modified=False, restored96_fresh_groups=164, measured_jvms=32)
    save(DEST / 'DELIVERY.json', result)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
