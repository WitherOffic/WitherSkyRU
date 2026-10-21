"""Authored exact notification tables;finite meal/skin labels expanded offline only."""
import argparse,json,re
from prepare_npc78 import ROOT,DATA,SOURCE,sha,rows,write,anchors

CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'

def validate(source,target,definitions):
    for side,text in [('source',source),('target',target)]:
        spans=[]
        for a in definitions:
            term=a[side];prefix=2 if text.startswith('✆ ') else 0;hits=[m.start() for m in re.finditer(re.escape(term),text) if m.start()>=prefix];ordinal=a.get(side+'_occurrence',0)
            assert hits and (len(hits)==1 or ordinal and ordinal<=len(hits)),(source,a,side,hits)
            pos=hits[ordinal-1] if ordinal else hits[0];spans.append((pos,pos+len(term)))
        spans.sort();assert all(a[1]<=b[0] for a,b in zip(spans,spans[1:])),(source,definitions)

def authored():
    result=[]
    for line in (SOURCE/'SYSTEM_0.15.107_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|');assert len(f) in (2,3);result.append((f[0],f[1],anchors(f[2]) if len(f)==3 else [],'individually_authored'))
    for line in (SOURCE/'SYSTEM_FOODS_0.15.107_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        food,ru=line.split('|');source=f'EW! You ate a {food}. Was it worth it?';target=f'ФУ! Вы попробовали {ru}. Оно того стоило?'
        result.append((source,target,anchors(json.dumps({'EW!':'ФУ!',food:ru},ensure_ascii=False)),'finite_authored_food_message'))
    for line in (SOURCE/'SYSTEM_SKINS_0.15.107_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        page,header,ru_header,label,ru_label=line.split('|');source=f'{header} You swapped to the {label} skin!';target=f'{ru_header} Вы сменили облик на «{ru_label}»!'
        result.append((source,target,anchors(json.dumps({header:ru_header,label:ru_label},ensure_ascii=False)),page))
    return result

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--baseline');a=ap.parse_args()
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue={r['source']:r for r in rows(CORPUS/'translation_queue.jsonl')};occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
    known={r['source'] for r in rows(SOURCE/'NPC_WORKLIST_0.15.106.jsonl') if r['exact_registration']};candidates=[];seen=set()
    for source,target,definitions,kind in authored():
        assert source in queue and source not in known and source not in seen,source;seen.add(source)
        r=queue[source];assert r['tag'] is None and not any(r[k] for k in ('variable_definition','unresolved_markup','historical_heading','alpha_page'))
        assert not any(t in source for t in ('{{','$[','|/','<player>','[player]'))
        if kind.endswith(' Skin'):assert r['page']==kind
        validate(source,target,definitions);assert re.findall('[0-9]+',source)==re.findall('[0-9]+',target)
        assert set(re.findall('[A-Za-z]+',target))<={'SkyBlock'},source
        layouts=list(dict.fromkeys(occurrences[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        assert all(re.sub('(?i)§[0-9a-fk-or]','',s).strip()==source for s in layouts),source
        candidates.append({'speaker':'','tag':'SYSTEM','source':source,'translation':target,'style_anchors':definitions,'source_url':r['source_url'],'authored_kind':kind,'published_row':r,'all_occurrences':[occurrences[k] for k in r['occurrence_ids']],'layouts':layouts})
    write(SOURCE/'SYSTEM_CANDIDATES_0.15.107.json',{'records':candidates,'scope':'Candidate authored targets,not yet runtime registration or new-translation counts.'})
    if not a.baseline:
        print(json.dumps({'candidates':len(candidates),'layouts':sum(len(r['layouts']) for r in candidates)},ensure_ascii=False));return
    baseline=json.loads((ROOT/a.baseline).read_text(encoding='utf-8-sig'));assert baseline['runtime_jar_sha256']=='8A56461E1EED72013316C8DEEA1FE268062CE4DAD0380EEDE13DD85BC8D260A2'
    by_source={r['source']:r for r in baseline['records']};assert by_source.keys()==seen
    kept=[];retained=[]
    for r in candidates:
        old=by_source[r['source']]
        if old['flat']==r['source'] and all(x['output']==r['source'] for x in old['layouts']):kept.append(r)
        else:retained.append({'candidate':r,'baseline':old,'reason':'Earlier handler already changed at least one output;do not add a shadowed/redundant exact key.'})
    assert kept
    records=[{k:r[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')} for r in kept]
    write(DATA/'system-expansion107.json',{'format_version':1,'language':'ru','scope':f'{len(records)} exact current-publication system notifications. Offline finite labels;not a runtime regex,translation API or new receipt pass.','records':records})
    cases=[]
    for r in kept:
        source=r['source'];at=0;primary='§f'
        for n,d in enumerate(sorted(r['style_anchors'],key=lambda x:source.index(x['source']))):
            start=source.index(d['source']);primary+=source[at:start]+'§'+str(n+1)+d['source']+'§f';at=start+len(d['source'])
        primary+=source[at:]
        for kind,legacy in [('primary',primary)]+[(f'published{n}',s) for n,s in enumerate(r['layouts'])]:cases.append({'source':source,'translation':r['translation'],'legacy':legacy,'kind':kind,'style_anchors':r['style_anchors']})
    write(SOURCE/'SYSTEM_CASES_0.15.107.json',{'cases':cases})
    ids={o['block_id'] for r in candidates for o in r['all_occurrences']}
    blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids]
    pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'SYSTEM_SOURCES_0.15.107.json',{'records':kept,'retained_not_new':retained,'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':baseline['runtime_jar_sha256'],'scope':'Pinned publication strings,not live packet completeness. Existing changed outputs retained,not counted as new translations.'})
    path=ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';text=path.read_text(encoding='utf-8-sig')
    old='new NpcDialogueTranslator("/data/witherskyru/quest-objectives.json",java.util.Set.of("SYSTEM"),true,true)'
    new='new NpcDialogueTranslator(new String[]{"/data/witherskyru/quest-objectives.json","/data/witherskyru/system-expansion107.json"},java.util.Set.of("SYSTEM"),true,true)'
    assert text.count(old)==1 or text.count(new)==1
    if old in text:path.write_text(text.replace(old,new),encoding='utf-8')
    license='SYSTEM107 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nAuthored Russian notifications and finite meal/skin labels;English strings retained from pinned public pages.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n'
    (ROOT/'src/main/resources/META-INF/licenses/SYSTEM107-SOURCES.txt').write_text(license,encoding='utf-8')
    write(SOURCE/'SYSTEM_PREPARATION_0.15.107.json',{'before_exact':11195,'after_exact':11195+len(records),'total':16244,'without_exact':5049-len(records),'new_records':len(records),'candidates':len(candidates),'retained_not_new':len(retained),'cases':len(cases),'published_layouts':len(cases)-len(records),'pages':len(pages),'quest_provider_before':29,'quest_provider_after':29+len(records)})
    print(json.dumps({'new':len(records),'retained':len(retained),'cases':len(cases),'pages':len(pages)},ensure_ascii=False))

if __name__=='__main__':main()
