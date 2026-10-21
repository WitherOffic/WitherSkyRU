"""Derived, source-pinned NPC recovery. Never rewrites the frozen corpus."""
import argparse,json,re
from prepare_npc78 import ROOT,SOURCE,DATA,rows,write,sha
from prepare_npc113 import CORPUS,load
from prepare_npc103 import segment
from review_game110 import runs
from prepare_game110 import positions

BASE='74AB75EB19EB4B679989A588ACE0CD4DA86417CA5E1AFD93E799BCF97F8C1789'
FIXED=[1946,6171,6355,*range(7411,7445),7965,7966,10149,14440]
NUMERIC=[7699,7713,11495,11497,11525,11535,11563,13010,14754,14869,15896]
COLOR=re.compile('(?i)§[0-9a-fk-or]')

def probe():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    work=rows(SOURCE/'NPC_WORKLIST_0.15.114.jsonl')
    occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
    blocks={b['block_id']:b for f in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/f)}
    out=[];excluded=[]
    for i in FIXED+NUMERIC:
        q=work[i]
        if q['exact_registration']:continue
        layouts=[];evidence=[]
        for oid in q['occurrence_ids']:
            o=occ[oid];b=blocks[o['block_id']];raw=o['source_legacy_line'].strip();s=q['source'];reason='exact published line'
            if i in range(7411,7445) or i==6355:
                # Only slash immediately preceding a complete colored NPC prefix is a delimiter.
                chunks=re.split(r'/\s*(?=§e\[NPC\])',raw)
                candidates=[v for v in chunks if COLOR.sub('',v)==s.removesuffix('/')]
                assert len(candidates)==1,(i,chunks,s)
                raw=candidates[0];s=COLOR.sub('',raw);reason='slash separator before next full NPC prefix,not the /coop command'
            if i==6171:
                assert b['template_raw'].endswith('?|text=}}') and raw.endswith('?|text=')
                raw=raw[:-6];s=COLOR.sub('',raw);reason='empty named text= template parameter,not visible dialogue'
            if i==1946:
                # Do not loosen the runtime padding guard to accommodate this publication typo.
                excluded.append({'id':str(i),'reason':'extra published leading space;runtime guard retained','block':b});continue
            if i==7438:
                excluded.append({'id':str(i),'reason':'malformed &! publication formatting;not silently repaired','block':b});continue
            if i==14440:
                excluded.append({'id':str(i),'reason':'nonstandard no-space prefix and obfuscated tag;not silently normalized','block':b});continue
            if i==10149:assert '<nowiki>|</nowiki>' in b['template_raw'] and s.endswith('Hi|')
            assert COLOR.sub('',raw)==s,(i,s,raw)
            evidence.append({'occurrence':o,'block':b,'transformation':reason});layouts.append(raw)
        if not layouts:continue
        if i in NUMERIC:
            marker=next(v for v in ('[#]','[year]','#') if s.count(v)==1)
            canonical=s.replace(marker,'{value}');canonical_layouts=[v.replace(marker,'{value}') for v in layouts]
            # minute[s] is a wiki plural notation,not a server-visible spelling.
            forms=[('singular','minute', '1'),('plural','minutes','137')] if '[s]' in canonical else [('number',None,'137')]
            for kind,word,value in forms:
                cs=canonical.replace('minute[s]',word) if word else canonical
                ls=[v.replace('minute[s]',word) if word else v for v in canonical_layouts]
                out.append({'id':f'{i}-{kind}','queue_index':i,'source':cs.replace('{value}',value),'template_source':cs,'probe_value':value,'template_layouts':ls,'layouts':[v.replace('{value}',value) for v in ls],'speaker':q['speaker'],'kind':'integer','archival':q['historical_heading'],'source_url':q['source_url'],'evidence':evidence,'queue_source':q['source']})
        else:
            out.append({'id':str(i),'queue_index':i,'source':s,'layouts':list(dict.fromkeys(layouts)),'speaker':q['speaker'],'kind':'literal','archival':q['historical_heading'],'source_url':q['source_url'],'evidence':evidence,'queue_source':q['source']})
    write(SOURCE/'NPC_RECOVERY_PROBE_0.15.115.json',{'records':out,'excluded':excluded,'scope':'Derived review view. Frozen corpus stays byte-identical. Archival sources explicitly labelled;not all current server scripts.'})
    print('Candidates',len(out),'excluded',len(excluded))

def prepare():
    candidate=load(SOURCE/'NPC_RECOVERY_PROBE_0.15.115.json');baseline=load(ROOT/'build/npc115-snapshots-1995c683e6cc4bf683088f6e62e0f74d/0.15.114-dev/SNAPSHOTS.json')
    assert baseline['runtime_jar_sha256']==BASE
    old={r['source']:r for r in baseline['records']};authored=load(SOURCE/'NPC_RECOVERY_0.15.115_REVIEWED.json');literal=[];templates=[];selected=[];retained=[];cases=[]
    # Chat notices precede exact dialogue lookup. Never shadow an older fixed translation.
    old_exact={r['source'] for p in DATA.glob('*.json') if p.name not in ('npc-recovery115.json','npc-structured115.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')}
    for r in candidate['records']:
        uid=r['id'];s=r['source'];before=old[s]
        if uid not in authored:
            assert uid=='6171' and before['flat']!=s and all(v['output']==before['flat'] for v in before['layouts']);retained.append({'candidate':r,'baseline':before});continue
        body,defs=authored[uid];definitions=[{'source':a,'target':b} for a,b in defs.items()];prefix=f"[NPC] {r['speaker']}: "
        assert before['flat']==s and all(v['output']==s for v in before['layouts'])
        assert set(re.findall('[A-Za-z]+',body))<={'value','SkyBlock','Shift','coop'},(uid,body)
        fullsource=r.get('template_source',s);target=prefix+body
        assert re.findall('[0-9]+',fullsource)==re.findall('[0-9]+',target),(uid,body)
        r=dict(r,source=fullsource,translation=target,style_anchors=definitions,capture_type='integer',color_fix=False)
        if r['kind']=='integer':
            assert body.count('{value}')==1
            left,right=fullsource[len(prefix):].split('{value}');toleft,toright=body.split('{value}')
            for a in definitions:assert (a['source'] in left and a['target'] in toleft) or (a['source'] in right and a['target'] in toright),(uid,a)
            excluded=[];lead=prefix+left
            for fixed in old_exact:
                if fixed.startswith(lead) and fixed.endswith(right):
                    end=len(fixed)-len(right) if right else len(fixed);value=fixed[len(lead):end]
                    if len(value)<=16 and re.fullmatch(r'(?:0|[1-9][0-9]*|[1-9][0-9]{0,2}(?:,[0-9]{3})+)',value):excluded.append(value)
            entry={'prefix':prefix,'capture_type':'integer','before':segment(left,toleft,definitions),'after':segment(right,toright,definitions),'override_keys':[fullsource,r['queue_source']]+list(defs),'excluded_names':sorted(set(excluded))}
            for legacy in r['template_layouts']:
                appearance=[style for style,text in runs(legacy) for c in text];visible=COLOR.sub('',legacy)
                for side,offset in [('before',len(prefix)),('after',len(prefix)+len(left)+len('{value}'))]:
                    for p in entry[side]['parts']:
                        styles={appearance[n] for n in range(offset+p['start'],offset+p['end']) if not visible[n].isspace()};assert len(styles)<=1,(uid,side,p,styles)
            r['runtime_rule']=entry;templates.append(entry)
        else:
            for a in definitions:
                positions(s,a,'source',len(prefix));positions(target,a,'target',len(prefix))
            entry={k:r[k] for k in ('source','translation','speaker','source_url','style_anchors')};entry['tag']='NPC';literal.append(entry)
            # Primary layout uses independent colors for semantic anchors;publication is tested separately.
            primary='§f'+s[:len(prefix)];at=len(prefix)
            for n,a in enumerate(sorted(definitions,key=lambda a:positions(s,a,'source',len(prefix))[0])):
                x=positions(s,a,'source',len(prefix))[0];primary+=s[at:x]+'§'+format(1+n%14,'x')+a['source']+'§f';at=x+len(a['source'])
            primary+=s[at:]
            for kind,legacy in [('primary',primary)]+[(f'published{n}',v) for n,v in enumerate(r['layouts'])]:cases.append({k:entry[k] for k in ('source','translation','style_anchors','tag')}|{'kind':kind,'legacy':legacy})
        selected.append(r)
    assert {r['id'] for r in selected}==authored.keys() and (len(literal),len(templates),len(cases))==(35,13,70)
    write(DATA/'npc-recovery115.json',{'format_version':1,'language':'ru','scope':'35 fixed source-backed messages,including31 archival Hub tutorial lines. Source delimiter repairs are offline only.','records':literal})
    write(DATA/'npc-structured115.json',{'schema':1,'scope':'13 closed integer shapes,11 original wiki patterns,two explicit minute/minutes branches;no placeholder exact keys.','templates':templates})
    pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {e['block']['page'] for r in selected+ [v['candidate'] for v in retained] for e in r['evidence']}]
    write(SOURCE/'NPC_RECOVERY_SOURCES_0.15.115.json',{'records':selected,'retained_not_new':retained,'excluded':candidate['excluded'],'source_pages':pages,'baseline_jar_sha256':BASE,'scope':'Pinned public descriptions,including archival sections;not current-server completeness. Raw originals and derived spellings both retained.'})
    write(SOURCE/'NPC_RECOVERY_CASES_0.15.115.json',{'cases':cases})
    exact={r['source'] for r in literal};oldwork=rows(SOURCE/'NPC_WORKLIST_0.15.114.jsonl');count=sum(r['source'] in exact for r in oldwork)
    info={'literal_records':len(literal),'numeric_templates':len(templates),'combined_templates':243,'original_numeric_queue_patterns':11,'literal_fixtures':len(cases),'new_exact_queue_keys':count,'new_normalized_keys_outside_frozen_queue':len(literal)-count,'exact_before':13969,'exact_after':13969+count,'total_frozen':16244,'remaining':2275-count,'story_before':10764,'story_after':10799,'system_after':3909,'retained_old_records':len(retained),'pages':len(pages),'archival_records':sum(r['archival'] for r in selected)}
    write(SOURCE/'NPC_RECOVERY_COUNTS_0.15.115.json',info)
    license_text='NPC115 attribution: CC BY-NC-SA3.0\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n35 literals and13 bounded number shapes. Historical sources explicitly retained. No all-live-server claim.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n'
    (ROOT/'src/main/resources/META-INF/licenses/NPC115-SOURCES.txt').write_text(license_text,encoding='utf-8')
    print(json.dumps(info,ensure_ascii=False))

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--prepare',action='store_true');args=ap.parse_args()
    prepare() if args.prepare else probe()
