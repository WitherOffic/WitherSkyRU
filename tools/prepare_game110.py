"""Probe receipt reachability before authoring a large gameplay-notification batch."""
import argparse,json,re
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from review_game110 import authored,runs
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASE='5263691ECFAF4CD57D60CCD69DE8C9DD05AE5D2167875DC17BC1AD77FF41CFE3'
def probe():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    work=rows(SOURCE/'NPC_WORKLIST_0.15.109.jsonl');occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};records=[]
    for r in work:
        s=r['source'];page=r['source_url'].split('/w/')[-1].split('?')[0]
        if r['exact_registration'] or r['tag'] is not None or any(r[k] for k in ('variable_definition','unresolved_markup','historical_heading','alpha_page')):continue
        if page.endswith('_Skin') or any(p in page for p in ('Chocolate_Eggs/List','Minions','Garden_Visitors','Alpha_Hypixel_Network')):continue
        if len(s)<16 or not re.search('[A-Za-z]',s):continue
        guarded=s.replace('[OPEN MENU]','').replace('[Vargul]','')
        if re.search(r'[#$<>{}\[\]]',guarded) or '|/' in s:continue
        layouts=list(dict.fromkeys(occurrences[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        if not all(re.sub('(?i)§[0-9a-fk-or]','',v)==s for v in layouts):continue
        records.append({'probe_index':len(records),'source':s,'source_url':r['source_url'],'published_row':r,'all_occurrences':[occurrences[k] for k in r['occurrence_ids']],'layouts':layouts})
    write(SOURCE/'GAME_CANDIDATES_0.15.110.json',{'records':records,'scope':'Probe only;not runtime additions,quality certification or new-translation counts. Fragments still require manual rejection.'});print('Probe candidates',len(records))
def positions(text,a,side,prefix):
    term=re.escape(a[side]);whole=a.get('whole_word',False)
    pattern=(r'(?<![\w])' if whole else '')+term+(r'(?![\w])' if whole else '')
    values=[m.start() for m in re.finditer(pattern,text) if m.start()>=prefix]
    occurrence=a.get(side+'_occurrence',0)
    return values[occurrence-1:occurrence] if occurrence else values

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--probe',action='store_true');a=ap.parse_args()
    if a.probe:probe();return
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    probe_rows=json.loads((SOURCE/'GAME_CANDIDATES_0.15.110.json').read_text(encoding='utf-8'))['records']
    prior=json.loads((ROOT/'build/game110-snapshots-d2025d44a02445f08fb60087a919ce62/0.15.109-dev/SNAPSHOTS.json').read_text(encoding='utf-8-sig'));assert prior['runtime_jar_sha256']==BASE
    old={r['source']:r for r in prior['records']};extra={}
    for line in (SOURCE/'GAME_STYLES_0.15.110_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            i,value=line.split('|');assert int(i) not in extra;extra[int(i)]=json.loads(value)
    header_ids=set(range(72,77))|set(range(78,82))|{125,127,248,249,250,251,252,253,254,272,319,384,391,416,417,438,454,457,458,468,469,472,494,496,497,544,558,566,606,609,624,628,629,630,632}|set(range(131,136))|set(range(374,381))|set(range(418,423))|set(range(525,530))
    result=[];cases=[];reviews=[]
    for i,target in authored():
        raw=probe_rows[i];assert raw['probe_index']==i;source=raw['source'];assert old[source]['flat']==source and all(x['output']==source for x in old[source]['layouts'])
        assert re.findall('[0-9]+',source)==re.findall('[0-9]+',target),(i,source,target)
        assert set(re.findall('[A-Za-z_]+',target))<={'SkyBlock','BlockDash','PLAYER_TRANSFER_COOLDOWN','farmingtoolkit','legacytrades','craft','Watchdog'},(i,target)
        mapping=dict(extra.get(i,{}))
        if i in header_ids:
            left=re.match(r'.+?[!?]',source)[0];right=re.match(r'.+?[!?]',target)[0];assert left not in mapping;mapping[left]=right
        if source.startswith('✆ ') and ' has been added to your Abiphone' in source:
            left=source[2:].split(' has been added')[0];right=re.match(r'✆ (.+?) добавлен[а]? в контакты',target)[1];assert left not in mapping;mapping[left]=right
        if source.startswith('CROWD: '):mapping['CROWD']='ТОЛПА'
        if source.startswith('[Vargul]: '):mapping.update({'[':'[','Vargul':'Варгул',']':']',': ':': '})
        definitions=[{'source':s,'target':t} for s,t in mapping.items()];prefix=2 if source.startswith('✆ ') else 0
        for definition in definitions:
            if source.startswith('[Vargul]: ') and definition['source']==': ':definition.update({'source_occurrence':1,'target_occurrence':1})
            if any(len(positions(text,definition,side,prefix))!=1 for side,text in [('source',source),('target',target)]):definition['whole_word']=True
            assert all(len(positions(text,definition,side,prefix))==1 for side,text in [('source',source),('target',target)]),(i,definition)
        for side,text in [('source',source),('target',target)]:
            spans=sorted((positions(text,d,side,prefix)[0],positions(text,d,side,prefix)[0]+len(d[side])) for d in definitions);assert all(a[1]<=b[0] for a,b in zip(spans,spans[1:])),(i,side)
        # Offline proof: every non-base visible source run is explicitly anchored.
        # No colors are guessed or generated by this analysis.
        covered={p for d in definitions for p in range(positions(source,d,'source',prefix)[0],positions(source,d,'source',prefix)[0]+len(d['source']))}
        for legacy in raw['layouts']:
            appearance=[style for style,text in runs(legacy) for c in text];assert len(appearance)==len(source)
            base={appearance[n] for n,c in enumerate(source) if n>=prefix and n not in covered and not c.isspace()};assert len(base)<=1,('Unreviewed style boundary',i,base,legacy)
            for d in definitions:
                start=positions(source,d,'source',prefix)[0];styles=appearance[start:start+len(d['source'])]
                assert d['source']==d['target'] or len(set(styles))==1,('Mixed semantic anchor',i,d)
        record={'speaker':'','tag':'SYSTEM','source':source,'translation':target,'style_anchors':definitions,'source_url':raw['source_url'],'probe_index':i,'published_row':raw['published_row'],'all_occurrences':raw['all_occurrences'],'layouts':raw['layouts']};result.append(record)
        # Independent synthetic layout exercises each authored anchor, not only publication colors.
        body=source[prefix:];at=prefix;primary='§b✆ §f' if prefix else '§f'
        for n,d in enumerate(sorted(definitions,key=lambda d:positions(source,d,'source',prefix)[0])):
            start=positions(source,d,'source',prefix)[0];primary+=source[at:start]+'§'+format(1+n%14,'x')+d['source']+'§f';at=start+len(d['source'])
        primary+=source[at:]
        for kind,legacy in [('primary',primary)]+[(f'published{n}',v) for n,v in enumerate(raw['layouts'])]:cases.append({'source':source,'translation':target,'legacy':legacy,'kind':kind,'style_anchors':definitions})
        reviews.append({'probe_index':i,'source':source,'target':target,'style_anchors':definitions})
    selected={i for i,t in authored()};assert set(extra)<=selected and header_ids<=selected
    assert len(result)==540 and len({r['source'] for r in result})==540
    ids={o['block_id'] for r in result for o in r['all_occurrences']};blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids];pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'GAME_SELECTED_0.15.110.json',{'records':result,'scope':'540 full pinned public gameplay messages. SYSTEM routing includes crowd and Vargul;not540 NPCs,not live server-script completeness.'})
    write(DATA/'game-expansion110.json',{'format_version':1,'language':'ru','scope':'540 exact authored gameplay messages. Expanded semantic headers offline;existing SYSTEM map only,not new render/regex/network work.','records':[{k:r[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')} for r in result]})
    write(SOURCE/'GAME_CASES_0.15.110.json',{'cases':cases});write(SOURCE/'GAME_SOURCES_0.15.110.json',{'records':result,'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':BASE,'retained_not_new':[],'probe_evidence':'game110-snapshots-d2025d44a02445f08fb60087a919ce62','scope':'Pinned published complete messages,not private/live packets. Fragments/variable placeholders/existing-handled outputs deliberately excluded.'})
    write(SOURCE/'GAME_EDITORIAL_0.15.110.json',{'scope':'Full-message source/target review before compilation;not runtime translation. Existing creature/item terminology checked;phone puns adapted without losing reply/button intent.','records':reviews})
    license='GAME110 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n540 authored Russian gameplay messages;English source strings retained from the pinned public revisions.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n';(ROOT/'src/main/resources/META-INF/licenses/GAME110-SOURCES.txt').write_text(license,encoding='utf-8')
    stats={'new_records':len(result),'cases':len(cases),'published_layouts':len(cases)-len(result),'pages':len(pages),'exact_before':12220,'exact_after':12760,'total':16244,'without_exact_after':3484,'provider_before':952,'provider_after':1492,'new_npc_keys':0};write(SOURCE/'GAME_PREPARATION_0.15.110.json',stats);print(json.dumps(stats))
if __name__=='__main__':main()
