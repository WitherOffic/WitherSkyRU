"""Compile authored Wizard dialogue; never machine-translate or infer live placeholders."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re
from prepare_npc78 import validate

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/witherskyru'
SOURCE = ROOT / 'translation-source'

def rows(path):
    return [json.loads(s) for s in path.read_text(encoding='utf-8-sig').splitlines() if s]

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--corpus', type=Path, required=True)
    args = parser.parse_args()
    corpus = args.corpus.resolve()
    queue = rows(corpus / 'translation_queue.jsonl')
    assert len(queue) == 16244
    by_source = {r['source']: r for r in queue}
    selected = [r for r in queue if r['speaker'] in ('Wizard', 'Wizardman')]
    # The source queue does not flag '#' placeholders; do not mistake them for literals.
    pending = [r for r in selected if r['variable_definition'] or '#' in r['source']]
    eligible = {r['source'] for r in selected if r not in pending}
    assert len(selected) == 251 and len(pending) == 7 and len(eligible) == 244
    known = defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name == 'npc-expansion79.json':
            continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records', []):
            if r.get('source') and r.get('translation'):
                known[r['source']].append(r)
    records, seen = [], set()
    for line in (SOURCE / 'NPC_0.15.79_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):
            continue
        f = line.split('|')
        assert len(f) in (3, 4), line
        speaker, body, ru = f[:3]
        source, target = f'[NPC] {speaker}: {body}', f'[NPC] {speaker}: {ru}'
        assert source in eligible and source not in seen and source not in known, source
        seen.add(source)
        ref = by_source[source]
        assert not any(ref[k] for k in ('variable_definition', 'unresolved_markup', 'nonstandard_tag_layout'))
        assert re.findall('[0-9]+', body) == re.findall('[0-9]+', ru), source
        assert not re.search(r'<[^>]+>|\{[^}]+\}|#', body), source
        assert not re.search('[A-Za-z]', ru.replace('SkyBlock', '').replace(' I.', ' .')), target
        definitions = []
        if len(f) == 4:
            raw = json.loads(f[3])
            definitions = [{'source': k, 'target': v} for k, v in raw.items()] if isinstance(raw, dict) else raw
        validate(source, target, definitions)
        record = {'source_key': f'npc79.{len(records)}', 'speaker': speaker, 'tag': 'NPC',
            'source': source, 'translation': target, 'style_anchors': definitions,
            'source_url': ref['source_url'], 'evidence': 'frozen_public_wiki_revision_not_live_packet'}
        records.append(record)
    assert seen == eligible
    write(DATA / 'npc-expansion79.json', {'format_version': 1, 'language': 'ru',
        'scope': 'All244 literal Wizard/Wizardman rows in frozen public corpus; seven placeholder rows '
                 'explicitly pending. Includes labeled historical material. Not a live server completeness claim.',
        'records': records})
    occurrences = {r['occurrence_id']: r for r in rows(corpus / 'dialogue_occurrences.jsonl')}
    block_ids = {occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks = [b for name in ('dialogue_blocks.jsonl', 'supplemental_dialogue_blocks.jsonl')
              for b in rows(corpus / name) if b['block_id'] in block_ids]
    assert {b['block_id'] for b in blocks} == block_ids
    write(SOURCE / 'NPC_SOURCES_0.15.79.json', {'scope': 'Complete frozen251 Wizard/Wizardman source rows; '
        '244 authored literal translations, seven placeholder rows retained as pending, never guessed.',
        'license': 'Wiki contributors; CC BY-NC-SA 3.0; per-row revision attribution',
        'queue_sha256': sha(corpus / 'translation_queue.jsonl'),
        'records': [{'source': r['source'], 'published_row': r,
            'runtime_registration': r['source'] in seen,
            'all_occurrences': [occurrences[i] for i in r['occurrence_ids']]} for r in selected],
        'blocks': blocks, 'pending_variables': pending})
    before = sum(r['source'] in known for r in queue)
    after = before + len(records)
    assert before == 1848 and after == 2092
    stats = {'corpus_unique_rows': len(queue), 'exact_resource_before': before, 'exact_resource_after': after,
        'after_percent': 100*after/len(queue), 'without_confirmed_exact_key': len(queue)-after,
        'new_reviewed_literal_rows': len(records), 'new_by_speaker': dict(Counter(r['speaker'] for r in records)),
        'published_branch_rows': len(selected), 'pending_variable_rows': len(pending),
        'runtime_validation': 'pending', 'new_resource_sha256': sha(DATA / 'npc-expansion79.json'),
        'installed_game_modified': False, 'game_fps_measured': False}
    write(SOURCE / 'NPC_PROGRESS_0.15.79.json', stats)
    print(json.dumps(stats, ensure_ascii=True, indent=2))

if __name__ == '__main__':
    main()
