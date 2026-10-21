"""103 pinned archival Goon literals and56 current ordered reply groups.
Closed startup tables only; no new per-frame/per-message algorithms.
"""
import json,re,shutil
from collections import defaultdict
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write,anchors,validate

CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'

def main():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(CORPUS/'translation_queue.jsonl');by_source={r['source']:r for r in queue}
    before=rows(SOURCE/'NPC_WORKLIST_0.15.105.jsonl');known={r['source'] for r in before if r['exact_registration']}
    selected=[r for r in queue if r['speaker']=='Goon' and r['source'] not in known]
    assert len(selected)==103 and all(r['historical_heading'] and not r['alpha_page'] and not r['variable_definition'] and not r['unresolved_markup'] for r in selected)
    records=[];cases=[];case_tsv=['# Authored source/layout cases | semantic anchors'];seen=set()
    occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
    for line in (SOURCE/'NPC_GOON_0.15.106_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|');i=int(f[0]);assert i not in seen;seen.add(i);r=selected[i];s=r['source'];target='[NPC] Goon: '+f[1];definitions=anchors(f[2]) if len(f)==3 else []
        validate(s,target,definitions);assert re.findall('[0-9]+',s)==re.findall('[0-9]+',target),s
        allowed={'[YOUTUBE] im_a_squid_kid ','[MVP+] Confused_Goon ','[MVP++] SnowflakeKiwi '}
        residual=f[1]
        for a in definitions:
            if a['source']==a['target'] and a['target'] in allowed:residual=residual.replace(a['target'],'')
        assert not re.search('[A-Za-z]',residual),(s,residual)
        record={'speaker':'Goon','tag':'NPC','source':s,'translation':target,'style_anchors':definitions,'source_url':r['source_url'],'evidence':'pinned_historical_heading_not_current_or_live_certification'}
        records.append(record)
        layouts=list(dict.fromkeys(occurrences[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        prefix=s.index(': ')+2;legacy='§e'+s[:prefix]+'§f';at=prefix
        for n,a in enumerate(sorted(definitions,key=lambda a:s.index(a['source'],prefix))):
            start=s.index(a['source'],prefix);legacy+=s[at:start]+'§'+str(n+1)+a['source']+'§f';at=start+len(a['source'])
        legacy+=s[at:]
        for kind,value in [('primary',legacy)]+[(f'published{n}',v) for n,v in enumerate(layouts)]:
            assert re.sub('(?i)§[0-9a-fk-or]','',value)==s
            case_tsv.append(str(len(cases))+'|'+json.dumps(definitions,ensure_ascii=False))
            cases.append({'source':s,'flat_result':target,'legacy':value,'kind':kind})
    assert seen==set(range(103))
    write(DATA/'npc-expansion106.json',{'format_version':1,'scope':'103 archival Goon exact literals,including published colored ranks. Not current/live NPC completeness. Merged once at startup.','records':records})
    write(SOURCE/'NPC_CASES_0.15.106.json',{'cases':cases})
    (SOURCE/'NPC_CASE_STYLES_0.15.106.tsv').write_text('\n'.join(case_tsv)+'\n',encoding='utf-8')
    code=ROOT/'src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';text=code.read_text(encoding='utf-8-sig')
    marker='"/data/witherskyru/npc-current102.json"}'
    assert text.count(marker)==1
    new_npc_text=text.replace(marker,'"/data/witherskyru/npc-current102.json","/data/witherskyru/npc-expansion106.json"}')
    grouped=defaultdict(list)
    for o in occurrences.values():
        if o['source'].startswith('Select an option:'):grouped[tuple(re.findall(r'\[([^][]+)\]',o['source']))].append(o)
    path=ROOT/'src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';text=path.read_text(encoding='utf-8-sig')
    known_choices={tuple(json.loads('['+a+']')) for a,b in re.findall(r'Map\.entry\(List\.of\((.*?)\),List\.of\((.*?)\)\)',text)}
    assert len(known_choices)==289
    choices=[];choice_entries=[];seen=set()
    for line in (SOURCE/'NPC_CHOICES_0.15.106_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        source,target=map(json.loads,line.split('|'));key=tuple(source)
        assert key in grouped and key not in known_choices and key not in seen and len(source)==len(target) and 1<=len(source)<=11;seen.add(key)
        assert all(len(s)<=180 and not re.search('[A-Za-z]',t) for s,t in zip(source,target))
        eligible=[o for o in grouped[key] if not o['historical_heading'] and not o['alpha_page'] and not o['variable_definition'] and not o['unresolved_markup']]
        assert eligible and all(re.fullmatch(r'Select an option: *(?:\[[^\[\]\r\n]{1,180}\] *){1,11}',o['source']) for o in eligible),source
        choices.append({'source_labels':source,'target_labels':target,'all_occurrences':eligible,'excluded_noncurrent_occurrences':[o for o in grouped[key] if o not in eligible]})
        def strings(parts):return ','.join(json.dumps(p,ensure_ascii=False) for p in parts)
        choice_entries.append('        java.util.Map.entry(List.of('+strings(source)+'),List.of('+strings(target)+'))')
    assert len(choices)==56
    marker='        java.util.Map.entry(List.of("TELEPORT BACK"),List.of("ТЕЛЕПОРТИРОВАТЬСЯ НАЗАД"))'
    assert text.count(marker)==1
    path.write_text(text.replace(marker,marker+',\n'+',\n'.join(choice_entries)),encoding='utf-8')
    code.write_text(new_npc_text,encoding='utf-8')
    ids={occurrences[k]['block_id'] for r in selected for k in r['occurrence_ids']}|{o['block_id'] for r in choices for o in r['all_occurrences']}
    blocks=[r for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for r in rows(CORPUS/name) if r['block_id'] in ids]
    pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'NPC_SOURCES_0.15.106.json',{'scope':'Separate103 historical literals and56 nonhistorical reply groups;not current/live completeness.','queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'records':[{'runtime_record':r,'published_row':by_source[r['source']],'all_occurrences':[occurrences[k] for k in by_source[r['source']]['occurrence_ids']]} for r in records],'blocks':blocks,'source_pages':pages})
    write(SOURCE/'NPC_CHOICES_SOURCES_0.15.106.json',{'records':choices,'blocks':blocks,'scope':'56 closed ordered groups;current publication occurrences only. English inputs,click commands and actions retained;not all live dialog variants.'})
    license='NPC106 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n103 archival Goon literals and56 current reply groups;authored Russian translations.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n'
    (ROOT/'src/main/resources/META-INF/licenses/NPC106-SOURCES.txt').write_text(license,encoding='utf-8')
    write(SOURCE/'NPC_PREPARATION_0.15.106.json',{'exact_before':11092,'exact_after':11195,'total':16244,'without_exact_after':5049,'new_archival_literals':103,'new_current_choice_groups':56,'new_label_slots':sum(len(r['source_labels']) for r in choices),'cases':len(cases),'published_layouts':len(cases)-103,'pages':len(pages),'game_fps_measured':False})
    print(json.dumps({'records':103,'choices':56,'labels':sum(len(r['source_labels']) for r in choices),'cases':len(cases),'pages':len(pages)},ensure_ascii=False))

if __name__=='__main__':main()
