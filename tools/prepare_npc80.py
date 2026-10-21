"""Compile authored Tyashoi text, with explicit timer/neutral exclusions and puzzle preservation."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import re
from prepare_npc78 import validate

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT/'translation-source'
DATA = ROOT/'src/main/resources/data/witherskyru'
BOOK = 'Shadows indicate riddles, illuminating unseen stars'

def rows(path):
    return [json.loads(s) for s in path.read_text(encoding='utf-8-sig').splitlines() if s]

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--corpus',type=Path,required=True)
    args=p.parse_args()
    corpus=args.corpus.resolve()
    queue=rows(corpus/'translation_queue.jsonl')
    assert len(queue)==16244
    selected=[r for r in queue if r['speaker']=='Tyashoi Alchemist']
    pending=[r for r in selected if r['variable_definition'] or '#' in r['source']]
    neutral=[r for r in selected if r['source'] in ('[NPC] Tyashoi Alchemist:', '[NPC] Tyashoi Alchemist: ...')]
    by_source={r['source']:r for r in selected}
    eligible={r['source'] for r in selected if r not in pending+neutral}
    assert len(selected)==217 and len(pending)==1 and len(neutral)==2 and len(eligible)==214
    known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name=='npc-expansion80.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    records=[]
    seen=set()
    for line in (SOURCE/'NPC_0.15.80_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|')
        assert len(f) in (3,4),line
        speaker,body,ru=f[:3]
        source,target=f'[NPC] {speaker}: {body}',f'[NPC] {speaker}: {ru}'
        assert source in eligible and source not in known and source not in seen,source
        seen.add(source)
        ref=by_source[source]
        assert not any(ref[k] for k in ('variable_definition','unresolved_markup','nonstandard_tag_layout'))
        assert re.findall('[0-9]+',body)==re.findall('[0-9]+',ru),source
        assert not re.search(r'<[^>]+>|\{[^}]+\}|#',body),source
        # Deliberate English exceptions: original riddle title and copy-only quantity x2/x10.
        latin_screen=ru.replace(BOOK,'')
        latin_screen=re.sub(r'\bx(?=\d)','',latin_screen)
        assert not re.search('[A-Za-z]',latin_screen),target
        if BOOK in body:assert ru.count(BOOK)==1,'Do not erase the original riddle initials/word lengths'
        definitions=[]
        if len(f)==4:
            raw=json.loads(f[3])
            definitions=[{'source':k,'target':v} for k,v in raw.items()] if isinstance(raw,dict) else raw
        validate(source,target,definitions)
        records.append({'source_key':f'npc80.{len(records)}','speaker':speaker,'tag':'NPC',
            'source':source,'translation':target,'style_anchors':definitions,
            'source_url':ref['source_url'],'evidence':'frozen_public_wiki_revision_not_live_packet'})
    assert seen==eligible
    write(DATA/'npc-expansion80.json',{'format_version':1,'language':'ru',
        'scope':'All214 translatable literal Tyashoi rows in frozen217 public rows;two neutral and one variable '
        'timer excluded. Original book-riddle title retained alongside Russian. Not live completeness.', 'records':records})
    occurrences={r['occurrence_id']:r for r in rows(corpus/'dialogue_occurrences.jsonl')}
    block_ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(corpus/name) if b['block_id'] in block_ids]
    assert {b['block_id'] for b in blocks}==block_ids
    page=next(r for r in rows(corpus/'source_pages.jsonl') if r['title']=='Tyashoi Alchemist')
    assert '10 hours of playtime' in page['raw_wikitext'],'Do not infer missing time units without the published requirement'
    write(SOURCE/'NPC_SOURCES_0.15.80.json',{'scope':'All217 frozen Tyashoi rows with full blocks and historical conditions; '
        '214 authored literals,one variable timer pending,two language-neutral lines unchanged.',
        'license':'Wiki contributors;CC BY-NC-SA3.0;per-row revisions', 'queue_sha256':sha(corpus/'translation_queue.jsonl'),
        'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,
            'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],
        'blocks':blocks,'pending_variables':pending,'neutral_unchanged':neutral,
        'requirement_unit_evidence':{'source_url':page['source_url'],'revision':page['revision'],
            'published_requirement':'Combat level10 and10 hours of playtime', 'full_page':page}})
    before=sum(r['source'] in known for r in queue)
    assert before==2092
    after=before+len(records)
    write(SOURCE/'NPC_PROGRESS_0.15.80.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,
        'exact_resource_after':after,'after_percent':100*after/len(queue),'without_confirmed_exact_key':len(queue)-after,
        'new_reviewed_literal_rows':214,'new_by_speaker':{'Tyashoi Alchemist':214},'published_branch_rows':217,
        'pending_variable_rows':1,'neutral_unchanged_rows':2,'runtime_validation':'pending',
        'new_resource_sha256':sha(DATA/'npc-expansion80.json'),'installed_game_modified':False,'game_fps_measured':False})
    print(f'NPC80 prepared:214 literals/217 published;1 timer pending;2 neutral;total{after}/16244')

if __name__=='__main__':main()
