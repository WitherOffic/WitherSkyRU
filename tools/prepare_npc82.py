"""Compile 65 authored literals; preserve complete source branches and placeholder exclusions."""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import urllib.request
from prepare_npc78 import anchors, rows, sha, validate, write

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'translation-source'
DATA=ROOT/'src/main/resources/data/witherskyru'
PAGES={'Kat':842631,'Kat (Rift)':828115,'Elizabeth':830426,'Elle of the Nether':815694}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--corpus',type=Path,required=True)
    corpus=parser.parse_args().corpus.resolve()
    queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244
    selected=[r for r in queue if r['speaker'] in {'Kat','Elizabeth','Elle of the Nether'}]
    assert len(selected)==91 and {r['page'] for r in selected}==set(PAGES)
    by_source={r['source']:r for r in selected}
    pending=[r for r in selected if r['variable_definition']]
    assert len(pending)==10
    known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name=='npc-expansion82.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    retained=[r for r in selected if r['source'] in known];assert len(retained)==16
    eligible={r['source'] for r in selected if r not in pending+retained};assert len(eligible)==65
    api='https://hypixelskyblock.minecraft.wiki/api.php?action=query&titles=Kat%7CKat%20(Rift)%7CElizabeth%7CElle%20of%20the%20Nether&prop=revisions&rvprop=ids%7Ctimestamp&rvslots=main&format=json&formatversion=2'
    request=urllib.request.Request(api,headers={'User-Agent':'WitherSkyRU-local-translation-review/0.15.82 (read-only)'})
    with urllib.request.urlopen(request,timeout=40) as response: freshness=json.load(response)
    assert {r['title']:r['revisions'][0]['revid'] for r in freshness['query']['pages']}==PAGES
    write(SOURCE/'NPC_0.15.82_PUBLIC_REVISION.json',{'checked_client_date':'2026-10-16','api_url':api,
        'matches_pinned_corpus':True,'private_or_unpublished_dialogue_known':False,'response':freshness})
    records=[];seen=set()
    for line in (SOURCE/'NPC_0.15.82_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|');assert len(f) in (3,4),line
        speaker,body,ru=f[:3];source=f'[NPC] {speaker}: {body}';target=f'[NPC] {speaker}: {ru}'
        assert source in eligible and source not in known and source not in seen,source
        seen.add(source);ref=by_source[source]
        assert not any(ref[k] for k in ('variable_definition','unresolved_markup','nonstandard_tag_layout'))
        assert re.findall('[0-9]+',body)==re.findall('[0-9]+',ru),source
        assert not re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body)
        assert not re.search('[A-Za-z]',ru.replace('SkyBlock','')),target
        assert body.startswith('✆ ')==ru.startswith('✆ ')
        definitions=anchors(f[3]) if len(f)==4 else []
        validate(source,target,definitions)
        records.append({'source_key':f'npc82.{len(records)}','speaker':speaker,'tag':'NPC',
            'source':source,'translation':target,'style_anchors':definitions,'source_url':ref['source_url'],
            'evidence':'historical_public_wiki_revision' if ref['historical_heading'] else 'public_wiki_revision_not_live_packet'})
    assert seen==eligible
    assert Counter(r['speaker'] for r in records)=={'Elizabeth':17,'Kat':39,'Elle of the Nether':9}
    write(DATA/'npc-expansion82.json',{'format_version':1,'language':'ru',
        'scope':'65 literal rows:Elizabeth17,Kat39(Hub and Rift),historical Elle of the Nether9. '
        'Sixteen old keys retained;ten variable sources excluded,not private/live completeness.','records':records})
    occurrences={r['occurrence_id']:r for r in rows(corpus/'dialogue_occurrences.jsonl')}
    block_ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(corpus/name) if b['block_id'] in block_ids]
    assert {b['block_id'] for b in blocks}==block_ids
    pages=[r for r in rows(corpus/'source_pages.jsonl') if r['title'] in PAGES]
    assert {r['title']:r['revision'] for r in pages}==PAGES
    write(SOURCE/'NPC_SOURCES_0.15.82.json',{'scope':'All91 selected public rows,including16 prior targets and10 placeholder sources. '
        'Old Elle branch explicitly historical. Identity names retained;no nameplate coverage claim.',
        'license':'Wiki contributors;CC BY-NC-SA3.0;per-row revisions',
        'queue_sha256':sha(corpus/'translation_queue.jsonl'),'source_pages':pages,'public_revision_check':freshness,
        'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,
            'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],
        'blocks':blocks,'pending_variables':pending,
        'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained]})
    before=sum(r['source'] in known for r in queue);assert before==2527
    write(SOURCE/'NPC_PROGRESS_0.15.82.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,
        'exact_resource_after':before+len(records),'after_percent':100*(before+len(records))/len(queue),
        'without_confirmed_exact_key':len(queue)-before-len(records),'new_reviewed_literal_rows':len(records),
        'new_by_speaker':dict(Counter(r['speaker'] for r in records)),'published_branch_rows':len(selected),
        'retained_old_keys':len(retained),'variable_sources_not_registered':len(pending),
        'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion82.json'),
        'installed_game_modified':False,'game_fps_measured':False})
    print('NPC82 prepared:65 new,16 retained,10 variable sources excluded;2592/16244 exact keys')

if __name__=='__main__':main()
