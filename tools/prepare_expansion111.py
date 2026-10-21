"""Pinned-source screening before authoring111;no runtime writes during probing."""
import argparse,json,re
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from review_expansion111 import authored,prefix,runs
from prepare_game110 import positions
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASE='120A6C334CE41095E37895820439B4028F20006CEFF6AB7B6F535E350BD29B74'

def probe():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    work=rows(SOURCE/'NPC_WORKLIST_0.15.110.jsonl');known={r['source'] for r in work if r['exact_registration']};occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};out=[]
    missing=[r for r in work if not r['exact_registration'] and r['tag']=='NPC']
    for index,r in enumerate(missing):
        source=r['source'];prefix='[NPC] '+r['speaker']+': '
        if not source.startswith(prefix) or r['variable_definition'] or r['unresolved_markup'] or r['alpha_page']:continue
        body=source[len(prefix):];guarded=re.sub(r'Slot #([23])',r'Slot \1',body)
        if not re.search('[A-Za-z]{2,}',body) or any(c in guarded for c in '[]#{}<>|') or body.endswith('/'):continue
        layouts=list(dict.fromkeys(occ[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        if not all(re.sub('(?i)§[0-9a-fk-or]','',v)==source for v in layouts):continue
        out.append({'id':'N'+str(index),'source':source,'speaker':r['speaker'],'tag':'NPC','source_url':r['source_url'],'published_row':r,'all_occurrences':[occ[k] for k in r['occurrence_ids']],'layouts':layouts})
    for r in json.loads((SOURCE/'GAME_CANDIDATES_0.15.110.json').read_text(encoding='utf-8'))['records']:
        if r['source'] in known:continue
        out.append(dict(r,id='S'+str(r['probe_index']),tag='SYSTEM',speaker=''))
    write(SOURCE/'EXPANSION_PROBE_0.15.111.json',{'scope':'Probe only;archival NPC headings explicitly retained for separate counts. No draft registered.','records':out});print('Probe',len(out),'NPC',sum(r['tag']=='NPC' for r in out))
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--probe',action='store_true');a=ap.parse_args()
    if a.probe:probe();return
    records,retained=authored();extra={}
    for line in (SOURCE/'EXPANSION_STYLES_0.15.111_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            uid,value=line.split('|');assert uid not in extra;v=json.loads(value);extra[uid]=[{'source':s,'target':t} for s,t in v.items()] if isinstance(v,dict) else v
    headers={659,700,708,711,714,718,719,727,746,747,750,754,755,787,809,812,838,841,846,886,902,942,943,953,1040,1055,1144}|set(range(669,674))|set(range(1042,1051))|set(range(1109,1114))|set(range(1148,1154));header_ids={'S'+str(i) for i in headers};cases=[]
    for r in records:
        uid=r['id'];s=r['source'];t=r['translation'];p=prefix(s);defs=list(extra.get(uid,[]))
        assert set(re.findall('[A-Za-z_]+',t[p:]))<={'SkyBlock','Shift','secretentrancetothemountaintop','XII'},(uid,t)
        if uid in header_ids:defs.append({'source':re.match(r'.+?[!?]',s)[0],'target':re.match(r'.+?[!?]',t)[0]})
        if uid.startswith('S') and 1060<=int(uid[1:])<=1074:defs.append({'source':re.fullmatch(r'You bought (.+)!',s)[1],'target':re.fullmatch(r'Вы купили (.+)!',t)[1]})
        for d in defs:
            if any(len(positions(text,d,side,p))!=1 for side,text in [('source',s),('target',t)]):d['whole_word']=True
            assert all(len(positions(text,d,side,p))==1 for side,text in [('source',s),('target',t)]),(uid,d)
        for side,text in [('source',s),('target',t)]:
            spans=sorted((positions(text,d,side,p)[0],positions(text,d,side,p)[0]+len(d[side])) for d in defs);assert all(a[1]<=b[0] for a,b in zip(spans,spans[1:])),(uid,side,defs)
        covered={n for d in defs for n in range(positions(s,d,'source',p)[0],positions(s,d,'source',p)[0]+len(d['source']))}
        for legacy in r['layouts']:
            appearance=[style for style,text in runs(legacy) for c in text];assert len(appearance)==len(s)
            base={appearance[n] for n,c in enumerate(s) if n>=p and n not in covered and not c.isspace()};assert len(base)<=1,('Unreviewed style boundary',uid,base,legacy)
            for d in defs:
                start=positions(s,d,'source',p)[0];assert d['source']==d['target'] or len(set(appearance[start:start+len(d['source'])]))==1,('Mixed semantic fragment',uid,d)
        r['style_anchors']=defs;r['archival']=bool(r['published_row']['historical_heading']);r['authored_scope']='literal_full_pinned_message_not_variable_template_or_live_completeness'
        at=p;primary=('§e'+s[:p]+'§f') if p else '§f'
        for n,d in enumerate(sorted(defs,key=lambda d:positions(s,d,'source',p)[0])):
            start=positions(s,d,'source',p)[0];primary+=s[at:start]+'§'+format(1+n%14,'x')+d['source']+'§f';at=start+len(d['source'])
        primary+=s[at:]
        for kind,value in [('primary',primary)]+[(f'published{n}',v) for n,v in enumerate(r['layouts'])]:cases.append({'source':s,'translation':t,'legacy':value,'kind':kind,'style_anchors':defs,'tag':r['tag']})
    assert set(extra)<=set(r['id'] for r in records) and header_ids<=set(r['id'] for r in records)
    assert sum(r['tag']=='NPC' for r in records)==133 and sum(r['tag']=='SYSTEM' for r in records)==369
    for tag,resource in [('NPC','npc-expansion111.json'),('SYSTEM','system-expansion111.json')]:write(DATA/resource,{'format_version':1,'language':'ru','scope':'Authored pinned literals;archival headings explicitly tracked in source evidence. Existing provider startup merge only. No new render,regex or network algorithms.','records':[{k:r[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')} for r in records if r['tag']==tag]})
    ids={o['block_id'] for r in records for o in r['all_occurrences']};blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids];pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'EXPANSION_SELECTED_0.15.111.json',{'records':records});write(SOURCE/'EXPANSION_CASES_0.15.111.json',{'cases':cases});write(SOURCE/'EXPANSION_SOURCES_0.15.111.json',{'records':records,'retained_not_new':retained,'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':BASE,'scope':'Full frozen publication strings;historical NPC headings are not current NPC additions or live script certification.'})
    stats={'new_records':502,'npc_records':133,'system_records':369,'archival_records':sum(r['archival'] for r in records),'current_npc_records':sum(r['tag']=='NPC' and not r['archival'] for r in records),'cases':len(cases),'published_layouts':len(cases)-502,'pages':len(pages),'exact_before':12760,'exact_after':13262,'total':16244,'without_exact_after':2982,'story_before':10628,'story_after':10761,'system_before':1492,'system_after':1861};write(SOURCE/'EXPANSION_PREPARATION_0.15.111.json',stats)
    license='EXPANSION111 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nAuthored translations of pinned full publications. Archival NPC headings retained and counted separately,not claimed as current/live script coverage.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n';(ROOT/'src/main/resources/META-INF/licenses/EXPANSION111-SOURCES.txt').write_text(license,encoding='utf-8');print(json.dumps(stats))
if __name__=='__main__':main()
