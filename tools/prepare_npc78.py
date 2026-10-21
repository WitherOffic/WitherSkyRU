"""Compile authored Tal Ker targets and closed alternate color plans. No auto-translation."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/witherskyru'
SOURCE = ROOT / 'translation-source'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def rows(path):
    return [json.loads(line) for line in path.read_text(encoding='utf-8-sig').splitlines() if line]

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def anchors(value):
    raw = json.loads(value)
    return [{'source': k, 'target': v} for k, v in raw.items()] if isinstance(raw, dict) else raw

def validate(source, target, definitions):
    prefix = source.index(': ') + 2
    for field, text in [('source', source), ('target', target)]:
        spans = []
        for a in definitions:
            term = a[field]
            assert term
            boundary = r'(?<![\w])' if a.get('whole_word') else ''
            tail = r'(?![\w])' if a.get('whole_word') else ''
            found = [m.start() + prefix for m in re.finditer(boundary + re.escape(term) + tail, text[prefix:])]
            ordinal = a.get(field + '_occurrence')
            assert found and (ordinal and 0 < ordinal <= len(found) or not ordinal and len(found) == 1), (source, a, found)
            position = found[ordinal-1] if ordinal else found[0]
            spans.append((position, position + len(term)))
        spans.sort()
        assert all(a[1] <= b[0] for a, b in zip(spans, spans[1:])), (source, definitions)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--corpus', type=Path, required=True)
    args = parser.parse_args()
    corpus = args.corpus.resolve()
    queue = rows(corpus / 'translation_queue.jsonl')
    assert len(queue) == 16244
    by_source = {r['source']: r for r in queue}
    all_occurrences = rows(corpus / 'dialogue_occurrences.jsonl')
    occurrences = {r['occurrence_id']: r for r in all_occurrences}
    known = defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name in ('npc-expansion78.json', 'npc-styles78.json'):
            continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records', []):
            if r.get('source') and r.get('translation'):
                known[r['source']].append(r)
    authored = []
    seen = set()
    for line in (SOURCE / 'NPC_0.15.78_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):
            continue
        parts = line.split('|')
        assert len(parts) in (3, 4)
        speaker, body, ru = parts[:3]
        source = f'[NPC] {speaker}: {body}'
        target = f'[NPC] {speaker}: {ru}'
        assert source not in known and source not in seen
        seen.add(source)
        reference = by_source[source]
        assert reference['speaker'] == 'Tal Ker' and reference['tag'] == 'NPC'
        assert not any(reference[k] for k in ('variable_definition', 'nonstandard_tag_layout', 'unresolved_markup'))
        assert re.findall('[0-9]+', body) == re.findall('[0-9]+', ru), source
        # [x] is the literal published meme, not a player/number placeholder.
        assert not re.search(r'<[^>]+>|\{[^}]+\}|\[(?!x\])[^]]+\]|#', body), source
        definitions = anchors(parts[3]) if len(parts) == 4 else []
        validate(source, target, definitions)
        record = {'source_key': f'npc78.{len(authored)}', 'speaker': speaker, 'tag': 'NPC',
            'source': source, 'translation': target, 'source_url': reference['source_url'],
            'evidence': 'frozen_public_wiki_revision_not_live_packet'}
        if definitions:
            record['style_anchors'] = definitions
        authored.append(record)
    tal_sources = {r['source'] for r in queue if r.get('speaker') == 'Tal Ker'}
    assert seen == tal_sources and len(authored) == 324, 'Complete frozen324 branch, not a selection'
    write(DATA / 'npc-expansion78.json', {'format_version': 1, 'language': 'ru',
        'scope': 'All324 exact tagged Tal Ker rows in frozen public corpus: Garden,First Time,Second Time. '
            'Not proof of private/live branches or a complete game translation.', 'records': authored})
    gap_path = SOURCE / 'NPC_STYLE_REVIEW_2026_10_16.json'
    assert sha(gap_path) == '323695199484B608AFC468AB0088A3C9DC5B4A21AF9358A2F14E2EBB7C5AA143'
    gaps = json.loads(gap_path.read_text(encoding='utf-8'))['records']
    style_records, style_evidence, fixed = [], [], set()
    for line in (SOURCE / 'NPC_STYLES_0.15.78_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):
            continue
        index, specification = line.split('|', 1)
        index = int(index)
        gap = gaps[index]
        source, target = gap['source'], gap['flat_result']
        assert source not in fixed
        fixed.add(source)
        assert known[source] and all(r['translation'] == target for r in known[source]), source
        definitions = anchors(specification)
        validate(source, target, definitions)
        style_records.append({'source_key': f'npc78.style.{index}', 'speaker': gap['speaker'],
            'source': source, 'translation': target, 'style_anchors': definitions,
            'source_url': gap['source_url'], 'evidence': 'closed_alternate_layout_target_unchanged'})
        style_evidence.append({'gap_index': index, 'source': source, 'target': target,
            'original_resources': known[source], 'published_row': by_source[source]})
    assert len(style_records) == 58
    write(DATA / 'npc-styles78.json', {'format_version': 1,
        'scope': '58 authored alternate semantic plans, loaded once; original text/plans/override terms retained.',
        'records': style_records})
    selected = seen | fixed
    block_ids = {occurrences[i]['block_id'] for source in selected for i in by_source[source]['occurrence_ids']}
    blocks = [b for name in ('dialogue_blocks.jsonl', 'supplemental_dialogue_blocks.jsonl')
        for b in rows(corpus / name) if b['block_id'] in block_ids]
    assert {b['block_id'] for b in blocks} == block_ids
    evidence = [{'source': source, 'published_row': by_source[source],
        'all_occurrences': [occurrences[i] for i in by_source[source]['occurrence_ids']]}
        for source in sorted(selected)]
    write(SOURCE / 'NPC_SOURCES_0.15.78.json', {'scope': 'Frozen complete Tal Ker branch plus58 closed style repairs; '
        'complete original blocks/conditions/revisions retained, not private server scripts',
        'license': 'Wiki contributors; CC BY-NC-SA 3.0; per-row revision attribution',
        'queue_sha256': sha(corpus / 'translation_queue.jsonl'), 'records': evidence,
        'blocks': blocks, 'style_repair_originals': style_evidence})
    before = sum(r['source'] in known for r in queue)
    after = before + len(authored)
    pending = len(queue) - after
    coverage = Counter('exact_resource_key_not_quality_proof' if r['source'] in known or r['source'] in seen
        else r['status'] for r in queue)
    sections = Counter(' / '.join(by_source[s]['headings']) for s in seen)
    stats = {'corpus_unique_rows': len(queue), 'exact_resource_before': before, 'exact_resource_after': after,
        'after_percent': 100 * after / len(queue), 'without_confirmed_exact_key': pending,
        'new_reviewed_literal_rows': 324, 'tal_ker_frozen_rows_covered': 324,
        'tal_ker_frozen_rows_remaining': 0, 'tal_ker_sections': dict(sections),
        'authored_style_repairs_pending_runtime_validation': 58, 'old_style_gap': 145,
        'partition_after': dict(coverage), 'installed_game_modified': False,
        'all_private_live_scripts_known': False, 'game_fps_measured': False,
        'new_resource_sha256': sha(DATA / 'npc-expansion78.json'),
        'style_resource_sha256': sha(DATA / 'npc-styles78.json')}
    write(SOURCE / 'NPC_PROGRESS_0.15.78.json', stats)
    n = lambda value: f'{value:,}'.replace(',', '\u202f')
    pct = lambda value: f'{100*value/len(queue):.2f}%'.replace('.', ',')
    report = ['# Перевод корпуса NPC — партия 0.15.78', '',
        'Учёт уникальных строк замороженной публичной выгрузки, не визуальных переносов и не всей закрытой базы игры.', '',
        '| Показатель | До партии | После партии |', '| --- | ---: | ---: |',
        f'| Точные записи перевода* | {n(before)} ({pct(before)}) | {n(after)} ({pct(after)}) |',
        f'| Без подтверждённого точного ключа* | {n(len(queue)-before)} | {n(pending)} ({pct(pending)}) |',
        '| Новая вычитанная партия | — | 324 реплики Тал Кера |',
        '| Цветные варианты прежних реплик | 145 кандидатов | 58 точных планов на проверке |', '',
        '*Наличие записи не доказывает качество всех старых переводов; отсутствие ключа не исключает работу '
        'динамического переводчика. Тал Кер покрыт целиком только в рамках опубликованных 324 точных реплик. '
        'Скрытые или неизвестные серверные ветки не объявляются переведёнными.', '',
        '## Полнота ветки Тал Кера', '', '| Раздел источника | Уникальных реплик |', '| --- | ---: |']
    for section, count in sorted(sections.items()):
        report.append(f'| {section} | {count} |')
    report += ['', 'Все 324 исходных ключа сопоставлены с авторскими русскими целями. Сохранены последовательности '
        'встреч, условия и полные исходные блоки. Буквальное `Doubt [x]` не объявлено переменной: это опубликованная реплика.', '',
        '## Цвета и нагрузка', '',
        'Старые тексты, исходные планы оформления и их пользовательские переопределения сохранены. '
        'Для 58 подтверждённых источником схем добавлены отдельные точные альтернативы. '
        'Они используются только если прежняя схема не подошла; текущие цвета и действия собираются один раз '
        'на обработку сообщения и не кэшируются между сообщениями.', '',
        'Нового обработчика кадров, сети или динамического шаблона для каждой реплики нет. '
        'Массовая проверка, общий прогон и замеры нагрузки сохраняются отдельно; запись в таблице сама по себе '
        'не является доказательством успешного исполнения или FPS. Установленная игра не менялась.', '']
    (SOURCE / 'NPC_PROGRESS_0.15.78_RU.md').write_text('\n'.join(report), encoding='utf-8')
    print(json.dumps(stats, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
