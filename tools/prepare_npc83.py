"""82 authored End guide literals +15 closed published-color repairs; no auto-translation."""
import argparse
from collections import defaultdict
import json,re,urllib.request
from pathlib import Path
from prepare_npc78 import anchors,rows,sha,validate,write
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
def main():
    p=argparse.ArgumentParser();p.add_argument('--corpus',type=Path,required=True);corpus=p.parse_args().corpus.resolve()
    queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244;by_source={r['source']:r for r in queue}
    selected=[r for r in queue if r['speaker']=='Lone Adventurer'];assert len(selected)==87
    pending=[r for r in selected if r['variable_definition'] or 'Select an option:' in r['source']];assert len(pending)==5
    eligible={r['source'] for r in selected if r not in pending};assert len(eligible)==82
    known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name in ('npc-expansion83.json','npc-styles83.json'):continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    assert not eligible&known.keys()
    records=[];seen=set()
    for line in (SOURCE/'NPC_0.15.83_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|');assert len(f) in (3,4);speaker,body,ru=f[:3];source=f'[NPC] {speaker}: {body}';target=f'[NPC] {speaker}: {ru}'
        assert source in eligible and source not in seen,source;seen.add(source);ref=by_source[source]
        assert not any(ref[k] for k in ('variable_definition','unresolved_markup','nonstandard_tag_layout','historical_heading','alpha_page'))
        assert re.findall('[0-9]+',body)==re.findall('[0-9]+',ru),source
        assert not re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body) and not re.search('[A-Za-z]',ru)
        definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
        records.append({'source_key':f'npc83.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':ref['source_url'],'evidence':'public_wiki_revision_not_live_packet'})
    assert seen==eligible
    write(DATA/'npc-expansion83.json',{'format_version':1,'language':'ru','scope':'82 literal Lone Adventurer rows. Four player-name templates and one editorial-looking NPC-prefixed choice excluded,not private/live completeness.','records':records})
    gaps=json.loads((SOURCE/'NPC_STYLE_REMAINING_0.15.82.json').read_text(encoding='utf-8-sig'))['records'];assert len(gaps)==87
    styles=[];style_evidence=[];fixed=set()
    for line in (SOURCE/'NPC_STYLES_0.15.83_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        index,raw=line.split('|',1);gap=gaps[int(index)];source=gap['source'];target=gap['flat_result']
        assert source not in fixed and known[source] and all(r['translation']==target for r in known[source]);fixed.add(source)
        definitions=anchors(raw);validate(source,target,definitions)
        styles.append({'source_key':f'npc83.style.{index}','speaker':gap['speaker'],'source':source,'translation':target,'style_anchors':definitions,'source_url':gap['source_url'],'evidence':'closed_alternate_layout_target_unchanged'})
        style_evidence.append({'source':source,'published_row':by_source[source],'original_resources':known[source]})
    assert len(styles)==15
    write(DATA/'npc-styles83.json',{'format_version':1,'scope':'15 reviewed alternate Juliette color plans;old targets and plans retained;loaded once.','records':styles})
    ids={i for source in seen|fixed|{r['source'] for r in pending} for i in by_source[source]['occurrence_ids']}
    occurrences={r['occurrence_id']:r for r in rows(corpus/'dialogue_occurrences.jsonl')}
    block_ids={occurrences[i]['block_id'] for i in ids}
    blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(corpus/name) if b['block_id'] in block_ids];assert {b['block_id'] for b in blocks}==block_ids
    pages=[r for r in rows(corpus/'source_pages.jsonl') if r['title'] in ('Lone Adventurer','Juliette')];expected={r['title']:r['revision'] for r in pages};assert expected=={'Lone Adventurer':823127,'Juliette':789285}
    api='https://hypixelskyblock.minecraft.wiki/api.php?action=query&titles=Lone%20Adventurer%7CJuliette&prop=revisions&rvprop=ids%7Ctimestamp&rvslots=main&format=json&formatversion=2'
    request=urllib.request.Request(api,headers={'User-Agent':'WitherSkyRU-local-translation-review/0.15.83 (read-only)'})
    with urllib.request.urlopen(request,timeout=40) as response:freshness=json.load(response)
    assert {r['title']:r['revisions'][0]['revid'] for r in freshness['query']['pages']}==expected
    write(SOURCE/'NPC_0.15.83_PUBLIC_REVISION.json',{'checked_client_date':'2026-10-16','api_url':api,'matches_pinned_corpus':True,'private_or_unpublished_dialogue_known':False,'response':freshness})
    write(SOURCE/'NPC_SOURCES_0.15.83.json',{'scope':'All87 Lone Adventurer sources plus15 old Juliette color repairs. Source choice row explicitly excluded,not guessed as live packet.','license':'Wiki contributors;CC BY-NC-SA3.0;per-row revisions','queue_sha256':sha(corpus/'translation_queue.jsonl'),'source_pages':pages,'public_revision_check':freshness,
        'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],
        'blocks':blocks,'pending_variables':pending,'retained_originals':[],
        'style_records':[dict(r,all_occurrences=[occurrences[i] for i in r['published_row']['occurrence_ids']]) for r in style_evidence]})
    before=sum(r['source'] in known for r in queue);assert before==2592
    write(SOURCE/'NPC_PROGRESS_0.15.83.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,'exact_resource_after':before+82,'after_percent':100*(before+82)/len(queue),'without_confirmed_exact_key':len(queue)-before-82,'new_reviewed_literal_rows':82,'new_by_speaker':{'Lone Adventurer':82},'published_branch_rows':87,'variable_sources_not_registered':4,'editorial_choice_source_not_registered':1,'style_repairs':15,'old_style_gap':87,'remaining_style_gap_expected':72,'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion83.json'),'style_resource_sha256':sha(DATA/'npc-styles83.json'),'installed_game_modified':False,'game_fps_measured':False})
    print('NPC83:82 new literals,15 exact style repairs;2674/16244 exact keys;5 structured exclusions')
if __name__=='__main__':main()
