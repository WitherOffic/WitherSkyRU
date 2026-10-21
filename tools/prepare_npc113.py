"""Closed one-capture NPC candidates. Placeholders never inflate exact-key progress."""
import argparse,json,re
from collections import Counter
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_npc103 import TOKEN,segment
from review_game110 import runs
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASE='866E0C2277F876229F4EFA98C388CE942F8E548181EDDBDC0FEB96A91AA0095B'
NAME_IDS={124,1436,1439,1812,4767,4780,4785,5070,5342,5655,7255,7314,7347,8192,9107,9551,9553,10639,10655,11918,11924,12862,13454,14420,15893,2310}
INTEGER_IDS={1732,1733,1735,1738,2395,2403,2408,2413,2417,2422,2426,2432,2690,4557,5742,7461,7792,7794,7795,7911,9322,13069,13085,13461,13467,13696,15305,15878}

def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def probe():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    work=rows(SOURCE/'NPC_WORKLIST_0.15.112.jsonl');occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};out=[];excluded=[]
    for i,r in enumerate(work):
        if r['exact_registration'] or r['tag']!='NPC' or r['alpha_page'] or r['unresolved_markup']:continue
        s=r['source'];kind=None;marker=None
        if i in NAME_IDS:
            kind='username';markers=TOKEN.findall(s)
            marker=markers[0] if len(markers)==1 else next((v for v in ('{player}','(Player)','[PLAYER]','Player','player') if s.count(v)==1),None)
        elif '[amount] Intelligence!' in s and r['speaker']!='{{{1|}}}':kind='stat_number';marker='[amount]'
        elif i in INTEGER_IDS:
            kind='integer';marker=next(v for v in ('[quantity]','#') if s.count(v)==1)
        if kind is None:continue
        assert marker is not None and s.count(marker)==1
        canonical=s.replace(marker,'{value}');p=canonical.index(': ')+2;body=canonical[p:]
        assert not re.search(r'[<>{}\[\]#]',body.replace('{value}','')),(i,s)
        layouts=list(dict.fromkeys(occ[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        if not all(re.sub('(?i)§[0-9a-fk-or]','',v)==s for v in layouts):excluded.append({'queue_index':i,'source':s,'reason':'publication line not a single exact source;not normalized silently'});continue
        probe_value='Wither_101' if kind=='username' else '137'
        canonical_layouts=[v.replace(marker,'{value}') for v in layouts];assert all(re.sub('(?i)§[0-9a-fk-or]','',v)==canonical for v in canonical_layouts)
        out.append({'id':str(i),'source':canonical,'speaker':r['speaker'],'tag':'NPC','capture_type':kind,'marker':marker,'probe_value':probe_value,'probe_source':canonical.replace('{value}',probe_value),'layouts':[v.replace('{value}',probe_value) for v in canonical_layouts],'template_layouts':canonical_layouts,'source_url':r['source_url'],'queue_row':r,'all_occurrences':[occ[k] for k in r['occurrence_ids']]})
    write(SOURCE/'NPC_PROBE_0.15.113.json',{'scope':'Closed source patterns only;one bounded numeric value or username;no exact-key or all-live-layout claim.','records':out,'excluded':excluded})
    print('PROBE',len(out),dict(Counter(r['capture_type'] for r in out)),'excluded',len(excluded))
    for r in out:
        if r['capture_type']=='stat_number' and r['speaker']!='Alwin':continue
        print(r['id'],r['source'])
        for value in r['template_layouts']:print('STYLE',json.dumps(runs(value),ensure_ascii=False))

def prepare():
    source=load(SOURCE/'NPC_PROBE_0.15.113.json');lookup={r['id']:r for r in source['records']};stats=load(SOURCE/'NPC_STAT_COMPONENTS_0.15.113_REVIEWED.json');authored={}
    for line in (SOURCE/'NPC_STRUCTURED_0.15.113_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        uid,body,defs=line.split('|');assert uid not in authored;authored[uid]=(body,[{'source':s,'target':t} for s,t in json.loads(defs).items()])
    stat_count=Counter()
    for uid,r in lookup.items():
        if r['capture_type']!='stat_number':continue
        assert r['speaker'] in stats['speakers'];body=r['source'].split(': ',1)[1];assert uid not in authored;authored[uid]=(stats['bodies'][body],[]);stat_count[r['speaker']]+=1
    assert len(stat_count)==18 and set(stat_count.values())=={3} and len(authored)==95
    baseline=load(ROOT/'build/npc113-snapshots-b83aa758568d47feb1d17fbe5944333a/0.15.112-dev/SNAPSHOTS.json');assert baseline['runtime_jar_sha256']==BASE;old={r['source']:r for r in baseline['records']}
    previous=rows(SOURCE/'NPC_WORKLIST_0.15.112.jsonl');exact={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')}
    exact.update(r['source'] for r in previous if not r['variable_definition'] and not TOKEN.search(r['source']));entries=[];evidence=[]
    for uid,(body,definitions) in authored.items():
        r=dict(lookup[uid]);s=r['source'];p=s.index(': ')+2;t=s[:p]+body;assert body.count('{value}')==1
        assert re.findall('[0-9]+',s)==re.findall('[0-9]+',t) and set(re.findall('[A-Za-z]+',body))<={'value'},(uid,body)
        before=old[s];color_fix=uid in {'8192','11924'}
        if color_fix:
            assert before['flat']==t.replace('{value}',r['probe_value']) and all(v['output']==r['probe_source'] for v in before['layouts'])
        else:assert before['flat']==r['probe_source'] and all(v['output']==r['probe_source'] for v in before['layouts'])
        left,right=s[p:].split('{value}');toleft,toright=body.split('{value}')
        for a in definitions:
            assert (a['source'] in left and a['target'] in toleft) or (a['source'] in right and a['target'] in toright),(uid,a)
        prefix=s[:p]
        if left.startswith('✆ '):assert toleft.startswith('✆ ');prefix+='✆ ';left=left[2:];toleft=toleft[2:]
        excluded=[]
        for fixed in exact:
            lead=prefix+left
            if fixed.startswith(lead) and fixed.endswith(right):
                end=len(fixed)-len(right) if right else len(fixed);capture=fixed[len(lead):end]
                if re.fullmatch('[A-Za-z0-9_]{1,16}',capture):excluded.append(capture)
        entry={'prefix':prefix,'capture_type':r['capture_type'],'before':segment(left,toleft,definitions),'after':segment(right,toright,definitions),'override_keys':[s,r['queue_row']['source']]+[a['source'] for a in definitions],'excluded_names':sorted(set(excluded))}
        if color_fix:assert not entry['excluded_names']
        # Each emitted part has a uniform published semantic style, ignoring only boundary whitespace.
        for legacy in r['template_layouts']:
            appearance=[style for style,text in runs(legacy) for c in text]
            for side,offset in [('before',len(prefix)),('after',len(prefix)+len(left)+len('{value}'))]:
                for part in entry[side]['parts']:
                    start=offset+part['start'];end=offset+part['end'];visible=legacy_plain=re.sub('(?i)§[0-9a-fk-or]','',legacy)
                    styles={appearance[n] for n in range(start,end) if not visible[n].isspace()}
                    assert len(styles)<=1,('Mixed unreviewed semantic part',uid,side,part,styles)
        entries.append(entry);r.update(translation=t,style_anchors=definitions,archival=bool(r['queue_row']['historical_heading']),color_fix=color_fix,runtime_rule=entry);evidence.append(r)
    assert len(entries)==95 and len({r['source'] for r in evidence})==95
    retained=[{'candidate':r,'baseline':old[r['source']]} for uid,r in lookup.items() if uid not in authored];assert len(retained)==13
    literal=next(r for r in previous if r['source']=='[NPC] Dean: There is a rogue Barbarian going by the name of Duke X.')
    occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};literal_occ=[occurrences[k] for k in literal['occurrence_ids']]
    assert literal['variable_definition'] and not literal['unresolved_markup'] and all(re.sub('(?i)§[0-9a-fk-or]','',o['source_legacy_line']).strip()==literal['source'] for o in literal_occ)
    lit={'speaker':'Dean','tag':'NPC','source':literal['source'],'translation':'[NPC] Dean: Есть варвар-отступник по имени Герцог X.','style_anchors':[{'source':'Duke X','target':'Герцог X'}],'source_url':literal['source_url'],'queue_row':literal,'all_occurrences':literal_occ,'layouts':list(dict.fromkeys(o['source_legacy_line'].strip() for o in literal_occ)),'literal_review_reason':'Duke X is a fixed named character in the full four-message block;old X-token heuristic was a false positive,not a dynamic value.'}
    ids={o['block_id'] for r in evidence+[lit] for o in r['all_occurrences']};blocks=[b for file in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/file) if b['block_id'] in ids];pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    block=next(b for b in blocks if b['block_id']==literal_occ[0]['block_id']);assert 'There is a rogue Barbarian going by the name of &cDuke X&f.' in block['template_raw']
    write(DATA/'npc-structured113.json',{'schema':1,'scope':'95 closed one-capture NPC templates:93 new and2 current-color repairs;offline16-length plans,not per-frame work or arbitrary variable substitution.','templates':entries})
    write(DATA/'npc-literals113.json',{'format_version':1,'language':'ru','scope':'One explicitly reviewed X-token false positive:fixed Duke X name,not a variable.','records':[{k:lit[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')}]})
    write(SOURCE/'NPC_STRUCTURED_SOURCES_0.15.113.json',{'scope':'Bounded username/numeric contracts on pinned publications,not all live formats or an exact-key coverage increment. Raw placeholders remain evidence,not literal keys.','records':evidence,'retained_not_new':retained,'literal_records':[lit],'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':BASE})
    write(SOURCE/'NPC_STRUCTURED_SELECTED_0.15.113.json',{'records':evidence})
    info={'templates':95,'new_templates':93,'published_color_repairs':2,'numeric_templates':79,'username_templates':16,'prior_templates':135,'combined_templates':230,'published_layouts':sum(len(r['template_layouts']) for r in evidence),'speakers':len({r['speaker'] for r in evidence}),'archival_templates':sum(r['archival'] for r in evidence),'retained_old_patterns':13,'exact_new':1,'exact_before':13744,'exact_after':13745,'frozen_total':16244,'exact_remaining':2499,'pages':len(pages),'story_before':10763,'story_after':10764};write(SOURCE/'NPC_STRUCTURED_COUNTS_0.15.113.json',info)
    (ROOT/'src/main/resources/META-INF/licenses/NPC113-SOURCES.txt').write_text('NPC113 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n95 bounded authored templates,including2 color fixes,not95 exact keys. One fixed Duke X literal. Pinned public publications,not live scripts.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n',encoding='utf-8')
    print(json.dumps(info,ensure_ascii=False))

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--prepare',action='store_true');a=ap.parse_args()
    if a.prepare:prepare()
    else:probe()
