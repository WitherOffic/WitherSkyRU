"""Reconcile an authored batch against a frozen public corpus; never auto-translate it.

Generated outputs are offline evidence/report files except the finite JSON resource.
No network, game logs, credentials, render hooks or installed-instance mutations.
"""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/witherskyru'
SOURCE = ROOT / 'translation-source'
BATCH = DATA / 'npc-expansion77.json'

def number(value):
    return f'{value:,}'.replace(',', '\u202f')

def percent(value):
    return f'{value:.2%}'.replace('.', ',')

def read_jsonl(path):
    return [json.loads(line) for line in path.read_text(encoding='utf-8-sig').splitlines() if line]

def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def existing():
    result = defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path == BATCH:
            continue
        for row in json.loads(path.read_text(encoding='utf-8-sig')).get('records', []):
            if row.get('source') and row.get('translation'):
                result[row['source']].append({'translation': row['translation'], 'resource': path.name})
    return result

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--corpus', type=Path, required=True)
    parser.add_argument('--dispatch-before', type=Path)
    parser.add_argument('--dispatch-after', type=Path)
    args = parser.parse_args()
    corpus = args.corpus.resolve()
    rows = read_jsonl(corpus / 'translation_queue.jsonl')
    assert len(rows) == 16244 and len({r['source'] for r in rows}) == len(rows)
    by_source = {row['source']: row for row in rows}
    occurrences = read_jsonl(corpus / 'dialogue_occurrences.jsonl')
    by_occurrence = {row['occurrence_id']: row for row in occurrences}
    old = existing()
    authored, evidence, seen = [], [], set()
    for line in (SOURCE / 'NPC_0.15.77_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):
            continue
        fields = line.split('|')
        assert len(fields) in (3, 4), line
        speaker, body, target = fields[:3]
        prefix = f'[NPC] {speaker}: '
        source = prefix + body
        assert source not in seen and source not in old, f'Duplicate or existing source: {source}'
        seen.add(source)
        reference = by_source[source]  # Exact publication key, not generated or guessed.
        refs = [by_occurrence[i] for i in reference['occurrence_ids']]
        assert any(not r['historical_heading'] and not r['alpha_page'] for r in refs), source
        assert reference['tag'] == 'NPC' and not any(reference[k] for k in
            ('variable_definition', 'nonstandard_tag_layout', 'unresolved_markup')), source
        assert not re.search(r'<[^>]+>|\[(?:crop|rarity|number|player|pet)\]|#', source, re.I), source
        assert re.findall(r'[0-9]+', body) == re.findall(r'[0-9]+', target), source
        assert bool(body.startswith('✆ ')) == bool(target.startswith('✆ ')), source
        anchors = json.loads(fields[3]) if len(fields) == 4 else {}
        if isinstance(anchors, dict):
            anchors = [{'source': key, 'target': value} for key, value in anchors.items()]
        for anchor in anchors:
            assert body.count(anchor['source']) == target.count(anchor['target']) == 1, source
        record = {'source_key': f'npc77.{len(authored)}', 'speaker': speaker, 'tag': 'NPC',
            'source': source, 'translation': prefix + target,
            'evidence': 'frozen_public_wiki_revision_not_live_packet',
            'source_url': reference['source_url']}
        if anchors:
            record['style_anchors'] = anchors
        authored.append(record)
        evidence.append({**reference, 'all_occurrences': refs})
    assert authored
    write_json(BATCH, {'format_version': 1, 'language': 'ru',
        'scope': 'Finite reviewed NPC corpus batch, merged into existing exact story map at startup. '
            'Not every NPC, all branches, dynamic values, live color layout or FPS proof.',
        'records': authored})
    write_json(SOURCE / 'NPC_SOURCES_0.15.77.json', {
        'scope': 'Independent exact rows, legacy layout, conditions and revision provenance from locked corpus',
        'license': 'CC BY-NC-SA 3.0; attribution via source_url per row; see corpus LICENSE_SOURCES.md',
        'corpus_queue_sha256': digest(corpus / 'translation_queue.jsonl'),
        'reviewed_targets_sha256': digest(SOURCE / 'NPC_0.15.77_REVIEWED.tsv'),
        'records': evidence})
    now = dict(old)
    for record in authored:
        now[record['source']] = [{'translation': record['translation'], 'resource': BATCH.name}]
    total = len(rows)
    before = sum(row['source'] in old for row in rows)
    after = sum(row['source'] in now for row in rows)
    assert after - before == len(authored)
    categories = Counter('exact_resource_match_not_runtime_proof' if r['source'] in now else r['status'] for r in rows)
    assert sum(categories.values()) == total
    labels = {
        'exact_resource_match_not_runtime_proof': 'Точный перевод зарегистрирован в ресурсах*',
        'needs_translation_review': 'Обычные реплики без точного ключа*',
        'variable_definition': 'Определения с переменными',
        'markup_review': 'Требуют разбора разметки',
        'nonstandard_tag_review': 'Нестандартные теги',
        'unprefixed_or_choice_review': 'Ответы, продолжения и строки без тега',
    }
    stats = {'corpus_unique_rows': total, 'occurrences': len(occurrences),
        'exact_resource_before': before, 'exact_resource_after': after,
        'before_percent': before / total * 100, 'after_percent': after / total * 100,
        'without_confirmed_exact_key': total - after, 'new_reviewed_rows': len(authored),
        'new_speakers': len({r['speaker'] for r in authored}), 'partition_after': dict(categories),
        'batch_resource_sha256': digest(BATCH), 'corpus_queue_sha256': digest(corpus / 'translation_queue.jsonl'),
        'runtime_test_result': 'See separate NPC77 test output; presence is not runtime/quality/FPS certification',
        'private_server_scripts_covered': False, 'installed_game_modified': False}
    if bool(args.dispatch_before) != bool(args.dispatch_after):
        raise ValueError('Supply both dispatch audits, not one')
    audit_before = audit_after = None
    if args.dispatch_before:
        audit_before = json.loads((args.dispatch_before / 'DISPATCH_SUMMARY.json').read_text(encoding='utf-8'))
        audit_after = json.loads((args.dispatch_after / 'DISPATCH_SUMMARY.json').read_text(encoding='utf-8'))
        assert audit_before['tested_literal_rows'] == audit_after['tested_literal_rows']
        assert audit_before['queue_sha256'] == audit_after['queue_sha256'] == digest(corpus / 'translation_queue.jsonl')
        baseline = {r['source']: r for r in read_jsonl(args.dispatch_before / 'DISPATCH_ROWS.jsonl')}
        compared = read_jsonl(args.dispatch_after / 'DISPATCH_ROWS.jsonl')
        assert len(baseline) == len(compared) == audit_after['tested_literal_rows']
        # Exact ordinal keys: case-insensitive dictionaries conflate e.g. Burp / burp.
        for row in compared:
            if row['source'] not in seen:
                prior = baseline[row['source']]
                assert all(row[k] == prior[k] for k in ('flat_result', 'published_style_result')), row['source']
        gap = [r for r in compared if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']]
        write_json(SOURCE / 'NPC_STYLE_REVIEW_2026_10_16.json', {
            'scope': 'Flat changed but saved colored layout unchanged: candidates, not manually certified defects',
            'runtime_jar_sha256': audit_after['runtime_jar_sha256'], 'records': gap})
        stats['dispatch_before'] = audit_before
        stats['dispatch_after'] = audit_after
        stats['existing_plain_vs_style_gap_candidates'] = len(gap)
        stats['non_batch_dispatch_results_identical_to_76'] = True
    write_json(SOURCE / 'NPC_PROGRESS_2026_10_16.json', stats)
    report = ['# Перевод публичного корпуса NPC — 16 октября 2026', '',
        'Единица учёта: уникальный точный исходный текст, а не визуальная строка после переноса. '
        '18 049 повторных вхождений хранятся отдельно. Знаменатель — 16 244 строки опубликованной '
        'выгрузки, не все слова игры или закрытые серверные скрипты.', '',
        '| Показатель | До партии | После партии |', '| --- | ---: | ---: |',
        f'| Точные ключи перевода* | {number(before)} ({percent(before/total)}) | {number(after)} ({percent(after/total)}) |',
        f'| Без подтверждённого точного ключа* | {number(total-before)} ({percent(1-before/total)}) | {number(total-after)} ({percent(1-after/total)}) |',
        f'| Новая вычитанная партия | — | {len(authored)} строк / {stats["new_speakers"]} обозначений NPC |', '',
        '| Статус после партии | Строк | Доля всей очереди |', '| --- | ---: | ---: |']
    for key, label in labels.items():
        count = categories[key]
        report.append(f'| {label} | {number(count)} | {percent(count/total)} |')
    if audit_after:
        tested = audit_after['tested_literal_rows']
        report += ['', '## Дополнительный запуск корпуса через обработчик чата', '',
            f'Одинаковая консервативная выборка: {number(tested)} буквальных опубликованных реплик. '
            'Переменные, спорная разметка, исторические/Alpha-строки, варианты кнопок и несовпадающие '
            'цельные цветные строки сюда не входят. Новые три кнопки Морта проверены отдельно в TestNpc77.', '',
            '| Обработчик изменил текст* | 0.15.76 | Текущий черновик |', '| --- | ---: | ---: |']
        for key, label in [('flat_changed_not_quality_proof', 'Без цветного оформления'),
            ('published_style_changed_not_quality_proof', 'С опубликованным цветным оформлением')]:
            a, b = audit_before[key], audit_after[key]
            report.append(f'| {label} | {number(a)} ({percent(a/tested)}) | {number(b)} ({percent(b/tested)}) |')
        report += ['', '*Изменение текста — техническое срабатывание, не доказательство полного и качественного '
            'перевода. Проценты этой таблицы рассчитаны от отдельной выборки, не от всей базы. '
            f'Выявлено {len(gap)} прежних кандидатов: простой текст обрабатывается, а цветной вариант — нет. '
            'Они сохранены для следующей вычитки. За пределами новых 137 записей результаты обеих версий '
            'в этой выборке дословно совпали. Контрольные суммы JAR и очереди сохранены в JSON-отчёте.']
    report += ['', '*Наличие точного ключа не доказывает качество всех старых переводов и совпадение '
        'с текущими серверными пакетами. Отсутствие ключа не доказывает отсутствие перевода: часть '
        'строк могут обрабатывать уже существующие шаблоны. Эти проценты не являются процентом '
        'полностью проверенной локализации игры.', '',
        'В прежней сводке было 1 375 строк с соответствующим статусом. Текущая сверка '
        'учла ещё 12 уже существовавших ключей служебных сообщений и спорно классифицированных '
        'строк. Они не входят в новую партию и не считаются вновь переведёнными.', '',
        '## Новая партия', '', '| NPC | Добавлено строк |', '| --- | ---: |']
    for speaker, count in sorted(Counter(r['speaker'] for r in authored).items()):
        report.append(f'| {speaker} | {count} |')
    report += ['', 'Не заявляется полное покрытие веток: например, шаблоны `[crop]`, `[rarity]` и '
        'числовых условий не импортированы как буквальные реплики. В исходной выгрузке часть '
        'таких строк ещё имеет слишком общий статус и требует дополнительной классификации. '
        'Исторические/временные реплики вики также нельзя автоматически считать текущей механикой.', '',
        '## Качество и производительность', '',
        'Новые записи загружаются в существующую точную карту один раз при запуске. '
        'Новых регулярных выражений, переводчиков, перекадровых обходов и сетевых запросов не добавлено. '
        'Это архитектурное ограничение нагрузки, не обещание нулевого влияния на FPS.', '',
        'Переводы и явные соответствия цветных фрагментов: `NPC_0.15.77_REVIEWED.tsv`. '
        'Ревизии, условия и исходные цветные строки: `NPC_SOURCES_0.15.77.json`. '
        'Проверка исполнения — отдельный `TestNpc77`, а не этот счётчик. '
        'Черновая 0.15.77 содержит ранее незавершённый аудит описаний; '
        'не считать её выпущенной только на основании отчёта NPC.', '']
    speakers = defaultdict(list)
    for row in rows:
        if row.get('speaker'):
            speakers[row['speaker']].append(row)
    priorities = sorted(((speaker, len(values), sum(r['source'] not in now for r in values))
        for speaker, values in speakers.items()), key=lambda item: (-item[2], item[0]))[:12]
    report += ['## Следующие крупные ветки', '',
        'Это объём проверки по обозначению говорящего, не число гарантированно сломанных '
        'строк или полностью отсутствующих переводов. Ответы без указанного говорящего '
        'и динамические варианты требуют дополнительной привязки к ветке.', '',
        '| Обозначение говорящего | Всего строк | Без точного ключа |', '| --- | ---: | ---: |']
    for speaker, count, pending in priorities:
        report.append(f'| {speaker} | {count} | {pending} |')
    report.append('')
    (SOURCE / 'NPC_PROGRESS_2026_10_16_RU.md').write_text('\n'.join(report), encoding='utf-8')
    print(json.dumps({k: v for k, v in stats.items() if not k.startswith('dispatch_')}, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
