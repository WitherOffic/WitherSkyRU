"""Authored frozen boss dialogue records, merged into an existing startup provider."""
import argparse,json,re
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASE='4EABCC14D06DAA2BC4B18E9CEA55ACBB7C2D89FBD0BD76D7099B0576FB46AA26'
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--baseline',type=str);a=ap.parse_args()
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(CORPUS/'translation_queue.jsonl');before=rows(SOURCE/'NPC_WORKLIST_0.15.107.jsonl');known={r['source'] for r in before if r['exact_registration']}
    selected=[r for r in queue if r['tag']=='BOSS' and r['source'] not in known and r['speaker'] in ('Arachne','Dante','Bal','Livid') and not r['variable_definition']]
    assert len(selected)==102 and all(not any(r[k] for k in ('unresolved_markup','nonstandard_tag_layout','alpha_page')) for r in selected)
    occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};candidates=[];seen=set()
    for line in (SOURCE/'BOSS_0.15.108_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        index,ru=line.split('|');index=int(index);assert index not in seen;seen.add(index);r=selected[index];source=r['source'];prefix=source[:source.index(': ')+2];target=prefix+ru
        assert not re.search('[A-Za-z]',ru) and re.findall('[0-9]+',source)==re.findall('[0-9]+',target)
        layouts=list(dict.fromkeys(occurrences[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        assert all(re.sub('(?i)§[0-9a-fk-or]','',s)==source for s in layouts)
        candidates.append({'speaker':r['speaker'],'tag':'BOSS','source':source,'translation':target,'style_anchors':[],'source_url':r['source_url'],'archive_branch':r['speaker']=='Dante','published_row':r,'all_occurrences':[occurrences[k] for k in r['occurrence_ids']],'layouts':layouts})
    assert seen==set(range(102));write(SOURCE/'BOSS_CANDIDATES_0.15.108.json',{'records':candidates,'scope':'Authored targets before baseline screening;Dante explicitly archival even if queue heading is not tagged historical.'})
    if not a.baseline:print('102 authored candidates');return
    baseline=json.loads((ROOT/a.baseline).read_text(encoding='utf-8-sig'));assert baseline['runtime_jar_sha256']==BASE
    lookup={r['source']:r for r in baseline['records']};assert lookup.keys()=={r['source'] for r in candidates}
    kept=[];retained=[]
    for r in candidates:
        old=lookup[r['source']]
        if old['flat']==r['source'] and all(v['output']==r['source'] for v in old['layouts']):kept.append(r)
        else:retained.append({'candidate':r,'baseline':old})
    assert len(kept)==102 and not retained
    records=[{k:r[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')} for r in kept]
    write(DATA/'boss-expansion108.json',{'format_version':1,'language':'ru','scope':'102 exact boss keys:41 non-Dante and61 archival Dante. Pinned public strings,not live completeness. Existing provider,loaded once.','records':records})
    cases=[]
    for r in kept:
        s=r['source'];prefix=s.index(': ')+2
        for kind,legacy in [('primary','§c'+s[:prefix]+'§f'+s[prefix:])]+[(f'published{n}',v) for n,v in enumerate(r['layouts'])]:cases.append({'source':s,'translation':r['translation'],'legacy':legacy,'kind':kind,'style_anchors':[]})
    write(SOURCE/'BOSS_CASES_0.15.108.json',{'cases':cases})
    ids={o['block_id'] for r in kept for o in r['all_occurrences']};blocks=[r for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for r in rows(CORPUS/name) if r['block_id'] in ids];pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'BOSS_SOURCES_0.15.108.json',{'records':kept,'retained_not_new':retained,'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':BASE,'scope':'Dante historical branch explicitly separated;not current/live completeness.'})
    path=ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';text=path.read_text(encoding='utf-8-sig');old='"/data/witherskyru/dungeon-expansion98.json"}';new='"/data/witherskyru/dungeon-expansion98.json","/data/witherskyru/boss-expansion108.json"}';assert text.count(old)==1 or text.count(new)==1
    if old in text:path.write_text(text.replace(old,new),encoding='utf-8')
    license='BOSS108 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nAuthored Russian translations;Dante archival,other rows pinned public strings not live certification.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n'
    (ROOT/'src/main/resources/META-INF/licenses/BOSS108-SOURCES.txt').write_text(license,encoding='utf-8')
    write(SOURCE/'BOSS_PREPARATION_0.15.108.json',{'new_records':len(kept),'archival_dante':61,'other_bosses':41,'cases':len(cases),'published_layouts':len(cases)-len(kept),'pages':len(pages),'exact_before':11314,'exact_after':11416,'without_exact_after':4828,'total':16244,'provider_before':358,'provider_after':460})
    print(json.dumps({'records':len(kept),'cases':len(cases),'pages':len(pages)}))
if __name__=='__main__':main()
