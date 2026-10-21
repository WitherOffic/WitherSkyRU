"""Compile a large independently authored eight-speaker batch,without runtime network or auto-translation."""
import argparse
from collections import Counter,defaultdict
import json,re,urllib.parse,urllib.request
from pathlib import Path
from prepare_npc78 import anchors,rows,sha,validate,write
ROOT=Path(__file__).resolve().parents[1];SOURCE=ROOT/'translation-source';DATA=ROOT/'src/main/resources/data/witherskyru'
SPEAKERS={'Dr. Stone':107,'Royal Resident':133,'Freddy':78,'No Name':62,'Kaus':62,'Rollim':57,'Tel Kar':61,'Udel':64}
FILES=['NPC84_KAUS_ROLLIM_REVIEWED.tsv','NPC84_TEL_KAR_REVIEWED.tsv','NPC84_FREDDY_REVIEWED.tsv','NPC84_ROYAL_RESIDENT_REVIEWED.tsv','NPC84_NO_NAME_REVIEWED.tsv','NPC84_UDEL_REVIEWED.tsv','NPC84_DR_STONE_REVIEWED.tsv']
# Public parser marks a model range as a variable; manual review confirms fixed retail model names.
FALSE_VARIABLE='[NPC] Udel: I have a wide range of Abiphones, from the Abiphone X+ to the Abiphone XIII Pro Giga!'
LATIN={'SkyBlock','x','y','z','X','XIII','XIV','Pro','Giga','Flip','FLIP'}
def structured(r):
    body=r['source'].split(': ',1)[1]
    return r['source']!=FALSE_VARIABLE and (r['variable_definition'] or bool(re.search(r'<[^>]+>|\{[^}]+\}|\[[^]]+\]|#',body)))
def neutral(r):return not re.search('[A-Za-z]',r['source'].split(': ',1)[1])
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--corpus',type=Path,required=True);parser.add_argument('--validate-only',action='store_true');args=parser.parse_args();corpus=args.corpus.resolve()
    queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244
    selected=[r for r in queue if r['speaker'] in SPEAKERS];assert len(selected)==649
    pending=[r for r in selected if structured(r)];unchanged=[r for r in selected if neutral(r)]
    assert len(pending)==5 and len(unchanged)==20
    by_source={r['source']:r for r in selected};eligible={r['source'] for r in selected if r not in pending+unchanged}
    assert len(eligible)==624 and Counter(by_source[s]['speaker'] for s in eligible)==SPEAKERS
    known=defaultdict(list)
    for path in sorted(DATA.glob('*.json')):
        if path.name=='npc-expansion84.json':continue
        for r in json.loads(path.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') and r.get('translation'):known[r['source']].append(r)
    assert not eligible&known.keys()
    records=[];seen=set();authored=[]
    for filename in FILES:
        path=SOURCE/filename
        if not path.exists():assert args.validate_only,filename;continue
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if not line or line.startswith('#'):continue
            f=line.split('|');assert len(f) in (3,4),line
            speaker,body,ru=f[:3];source=f'[NPC] {speaker}: {body}';target=f'[NPC] {speaker}: {ru}'
            assert source in eligible and source not in seen,(filename,source);seen.add(source);ref=by_source[source]
            assert not any(ref[k] for k in ('unresolved_markup','nonstandard_tag_layout','historical_heading','alpha_page'))
            assert re.findall('[0-9]+',body)==re.findall('[0-9]+',ru),source
            for token in re.findall('[A-Za-z]+',ru):assert token in LATIN and token in re.findall('[A-Za-z]+',body),(source,token)
            definitions=anchors(f[3]) if len(f)==4 else [];validate(source,target,definitions)
            records.append({'source_key':f'npc84.{len(records)}','speaker':speaker,'tag':'NPC','source':source,'translation':target,'style_anchors':definitions,'source_url':ref['source_url'],'evidence':'public_wiki_revision_not_live_packet'})
            authored.append(line)
    if args.validate_only:
        print('Authored validated:',len(records),'of624;',dict(Counter(r['speaker'] for r in records)));print('Remaining:',len(eligible-seen));return
    assert seen==eligible
    (SOURCE/'NPC_0.15.84_REVIEWED.tsv').write_text('# Combined authored eight-speaker batch;see individual NPC84_* files for editing.\n'+'\n'.join(authored)+'\n',encoding='utf-8')
    write(DATA/'npc-expansion84.json',{'format_version':1,'language':'ru','scope':'624 authored literal rows from649 sources/eight speaker labels. Five structured templates excluded,20 neutral rows unchanged. One fixed model range manually cleared as a parser false-positive. Not private/live completeness.','records':records})
    occurrences={r['occurrence_id']:r for r in rows(corpus/'dialogue_occurrences.jsonl')};block_ids={occurrences[i]['block_id'] for r in selected for i in r['occurrence_ids']}
    blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(corpus/name) if b['block_id'] in block_ids];assert {b['block_id'] for b in blocks}==block_ids
    choice_occurrences=[o for o in occurrences.values() if o['block_id'] in block_ids and o['source'].startswith('Select an option:')]
    observed_choices=defaultdict(list)
    for o in choice_occurrences:observed_choices[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
    choices=[];choice_seen=set()
    for line in (SOURCE/'NPC_CHOICES_0.15.84_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        a,b=map(json.loads,line.split('|'));key=tuple(a)
        assert key in observed_choices and key not in choice_seen and len(a)==len(b) and 1<=len(a)<=5
        assert re.findall('[0-9]+',' '.join(a))==re.findall('[0-9]+',' '.join(b));choice_seen.add(key)
        choices.append({'source_labels':a,'target_labels':b,'all_occurrences':observed_choices[key]})
    assert choice_seen==set(observed_choices) and len(choices)==11
    write(SOURCE/'NPC_CHOICES_SOURCES_0.15.84.json',{'scope':'All11 exact ordered choice groups in the selected source blocks;12 authored visible labels. No arbitrary groups/commands guessed.','queue_sha256':sha(corpus/'translation_queue.jsonl'),'records':choices,'blocks':blocks})
    titles={r['page'] for r in selected};pages=[r for r in rows(corpus/'source_pages.jsonl') if r['title'] in titles];expected={r['title']:r['revision'] for r in pages};assert len(expected)==7
    api='https://hypixelskyblock.minecraft.wiki/api.php?'+urllib.parse.urlencode({'action':'query','titles':'|'.join(sorted(titles)),'prop':'revisions','rvprop':'ids|timestamp','rvslots':'main','format':'json','formatversion':'2'})
    request=urllib.request.Request(api,headers={'User-Agent':'WitherSkyRU-local-translation-review/0.15.84 (read-only)'})
    with urllib.request.urlopen(request,timeout=45) as response:freshness=json.load(response)
    assert {r['title']:r['revisions'][0]['revid'] for r in freshness['query']['pages']}==expected,'Public pages changed;review the delta before claiming current coverage'
    write(SOURCE/'NPC_0.15.84_PUBLIC_REVISION.json',{'checked_client_date':'2026-10-16','api_url':api,'matches_pinned_corpus':True,'private_or_unpublished_dialogue_known':False,'response':freshness})
    write(SOURCE/'NPC_SOURCES_0.15.84.json',{'scope':'All649 rows,all occurrences/blocks/conditions,full seven source pages;20 neutral and5 structured exclusions separate.','license':'Wiki contributors;CC BY-NC-SA3.0;per-row revisions','queue_sha256':sha(corpus/'translation_queue.jsonl'),'source_pages':pages,'public_revision_check':freshness,
        'records':[{'source':r['source'],'published_row':r,'runtime_registration':r['source'] in seen,'all_occurrences':[occurrences[i] for i in r['occurrence_ids']]} for r in selected],
        'blocks':blocks,'pending_variables':pending,'neutral_unchanged':unchanged,'retained_originals':[],
        'manual_parser_false_positive_cleared':[by_source[FALSE_VARIABLE]]})
    before=sum(r['source'] in known for r in queue);assert before==2674
    write(SOURCE/'NPC_PROGRESS_0.15.84.json',{'corpus_unique_rows':len(queue),'exact_resource_before':before,'exact_resource_after':before+624,'after_percent':100*(before+624)/len(queue),'without_confirmed_exact_key':len(queue)-before-624,'new_reviewed_literal_rows':624,'new_by_speaker':SPEAKERS,'published_branch_rows':649,'structured_sources_not_registered':5,'neutral_unchanged_rows':20,'manual_parser_false_positive_cleared':1,'runtime_validation':'pending','new_resource_sha256':sha(DATA/'npc-expansion84.json'),'installed_game_modified':False,'game_fps_measured':False})
    print('NPC84 prepared:624 new,5 structured,20 neutral;3298/16244 exact keys;seven revisions current')
if __name__=='__main__':main()
