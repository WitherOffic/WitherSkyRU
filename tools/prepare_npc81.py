"""Compile authored Elle translations; retain old targets, variables and editorial gaps separately."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import re
from prepare_npc78 import validate

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'translation-source'
DATA=ROOT/'src/main/resources/data/witherskyru'

def rows(path):
    return [json.loads(s) for s in path.read_text(encoding='utf-8-sig').splitlines() if s]

def write(path,value):
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--corpus',type=Path,required=True)
    args=p.parse_args();corpus=args.corpus.resolve()
    queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244
    selected=[r for r in queue if r['speaker']=='Elle'];assert len(selected)==235
    pending=[r for r in selected if r['variable_definition'] or r['unresolved_markup'] or '<sup>' in r['source']]
    neutral=[r for r in selected if r['source']=='[NPC] Elle: ...']
    assert len(pending)==4 and len(neutral)==1
    by_source={r['source']:r for r in selected}
    known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name=='npc-expansion81.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    retained=[r for r in selected if r['source'] in known];assert len(retained)==9
    eligible={r['source'] for r in selected if r not in pending+neutral+retained};assert len(eligible)==221
    records=[];seen=set()
    for line in (SOURCE/'NPC_0.15.81_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|');assert len(f) in (3,4),line
        speaker,body,ru=f[:3];source=f'[NPC] {speaker}: {body}';target=f'[NPC] {speaker}: {ru}'
        assert source in eligible and source not in known and source not in seen,source
        seen.add(source);ref=by_source[source]
        assert not any(ref[k] for k in ('variable_definition','unresolved_markup','nonstandard_tag_layout'))
        assert re.findall('[0-9]+',body)==re.findall('[0-9]+',ru),source
        assert not re.search(r'<[^>]+>|\{[^}]+\}|#',body)
        assert not re.search('[A-Za-z]',ru),target
        assert body.startswith('✆ ')==ru.startswith('✆ '),'Phone identity prefix must be copied'
        definitions=[]
        if len(f)==4:
            raw=json.loads(f[3]);definitions=[{'source':k,'target':v} for k,v in raw.items()] if isinstance(raw,dict) else raw
        validate(source,target,definitions)
        records.append({'source_key':f'npc81.{len(records)}','speaker':speaker,'tag':'NPC',
            'source':source,'translation':target,'style_anchors':definitions,'source_url':ref['source_url'],
            'evidence':'frozen_public_wiki_revision_not_live_packet'})
    assert seen==eligible
    write(DATA/'npc-expansion81.json',{'format_version':1,'language':'ru',
        'scope':'221 new Elle literal rows from235 published rows,all branches including Kuudra. '
        'Nine old targets retained,three player-name templates and one editorial-sic source pending,one neutral. '
        'Not proof of private/live completeness.','records':records})
    occurrences={r['occurrence_id']:r for r in rows(corpus/'dialogue_occurrences.jsonl')}
    block_ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(corpus/name) if b['block_id'] in block_ids]
    assert {b['block_id'] for b in blocks}==block_ids
    page=next(r for r in rows(corpus/'source_pages.jsonl') if r['title']=='Elle')
    assert page['revision']==830430
    freshness=json.loads((SOURCE/'NPC_0.15.81_PUBLIC_REVISION.json').read_text(encoding='utf-8-sig'))
    assert freshness['revision']==page['revision'] and freshness['pageid']==page['pageid']
    write(SOURCE/'NPC_SOURCES_0.15.81.json',{'scope':'Complete235 frozen Elle source rows;221 new literals,9 prior '
        'targets retained;four structured pending sources;one neutral. All blocks/conditions/occurrences retained.',
        'license':'Wiki contributors;CC BY-NC-SA3.0;per-row revisions','queue_sha256':sha(corpus/'translation_queue.jsonl'),
        'public_revision_check':freshness,'source_page':page,
        'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,
            'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],
        'blocks':blocks,'pending_variables':pending,'neutral_unchanged':neutral,
        'retained_originals':[{'source':r['source'],'original_resources':known[r['source']]} for r in retained]})
    before=sum(r['source'] in known for r in queue);assert before==2306
    after=before+len(records);assert after==2527
    write(SOURCE/'NPC_PROGRESS_0.15.81.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,
        'exact_resource_after':after,'after_percent':100*after/len(queue),'without_confirmed_exact_key':len(queue)-after,
        'new_reviewed_literal_rows':221,'new_by_speaker':{'Elle':221},'published_branch_rows':235,
        'retained_old_keys':9,'pending_variable_or_editorial_rows':4,'neutral_unchanged_rows':1,
        'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion81.json'),
        'installed_game_modified':False,'game_fps_measured':False})
    print(f'NPC81 prepared:221 new,9 retained,4 pending,1 neutral;total{after}/16244')

if __name__=='__main__':main()
