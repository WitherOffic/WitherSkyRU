"""Closed,offline compositions of the six pinned egg envelopes and224 authored locations."""
import argparse,json,re
from collections import Counter
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_npc113 import CORPUS,load
from review_game110 import runs
from prepare_game110 import positions
BASE='4350073540D2C94A291C9D8B9390D9BFA731C2F26B337487741AB5D10768A1BE'

def records():
    probe=load(SOURCE/'EXPANSION_PROBE_0.15.114.json')['records'];locs={r['id']:r for r in probe if '/Chocolate_Eggs/List?' in r['source_url']};assert len(locs)==224
    headers=load(SOURCE/'EGG_HEADERS_0.15.114_REVIEWED.json');authored={}
    for line in (SOURCE/'EGG_LOCATIONS_0.15.114_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        uid,value=line.split('|');assert uid not in authored;authored[uid]=json.loads(value) if value.startswith('[') else [value]
    assert authored.keys()==locs.keys()
    blocks=[b for f in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/f)];envelopes=[]
    for b in blocks:
        if b['page']!='Chocolate Eggs':continue
        legacy=b['provisional_legacy_text'].strip();plain=re.sub('(?i)§[0-9a-fk-or]','',legacy)
        match=re.fullmatch(r"HOPPITY'S HUNT You found a (Chocolate (?:Breakfast|Brunch|Lunch|Déjeuner|Dinner|Supper) Egg) \[Location message\]!",plain)
        if match:envelopes.append({'egg':match[1],'legacy':legacy,'block':b})
    assert len(envelopes)==6 and {e['egg'] for e in envelopes}==headers['egg_names'].keys()
    out=[]
    for uid,r in locs.items():
        parts=runs(r['layouts'][0]);values=authored[uid];values=list(enumerate(values)) if all(isinstance(v,str) for v in values) else values
        assert sorted(i for i,v in values)==list(range(len(parts))) and all(isinstance(v,str) and v for i,v in values),(uid,parts,values)
        target=''.join(v for i,v in values);assert re.findall('[0-9]+',r['source'])==re.findall('[0-9]+',target)
        assert set(re.findall('[A-Za-z]+',target))<={'Plhlegblast'},(uid,target)
        definitions=[{'source':parts[i][1],'target':v} for i,v in values]
        def entry(s,t,legacy,kind,egg='',parent=None,anchors=None):
            d=[dict(a) for a in (anchors if anchors is not None else definitions)]
            for a in d:
                # Explicit source/target ordinals include spaces/punctuation;never guess repeated fragments at receipt.
                for side,text in [('source',s),('target',t)]:
                    matches=[m.start() for m in re.finditer(re.escape(a[side]),text)]
                    if len(matches)>1:
                        if side=='source':expected=(s.index(r['source']) if kind!='location' else 0)+sum(len(p[1]) for p in parts[:next(i for i,v in values if v==a['target'])])
                        else:expected=t.index(target)+sum(len(v) for i,v in values[:next(n for n,(i,v) in enumerate(values) if v==a['target'])])
                        assert expected in matches,(uid,a,side,expected,matches);a[side+'_occurrence']=matches.index(expected)+1
            for side,text in [('source',s),('target',t)]:
                spans=sorted((positions(text,a,side,0)[0],positions(text,a,side,0)[0]+len(a[side])) for a in d)
                assert all(a[1]<=b[0] for a,b in zip(spans,spans[1:])),(uid,side,d)
            return {'id':kind+'-'+uid+('-'+egg if egg else ''),'location_id':uid,'source':s,'translation':t,'tag':'SYSTEM','speaker':'','style_anchors':d,'layouts':list(dict.fromkeys(legacy)),'kind':kind,'egg':egg,'source_url':r['source_url'],'published_row':r['published_row'],'all_occurrences':r['all_occurrences'],'parent_envelope':parent,'source_location_parts':[text for style,text in parts],'target_location_parts':values}
        out.append(entry(r['source'],target,r['layouts'],'location'))
        for e in envelopes:
            name=e['egg'];s=headers['header_source']+headers['prose_source']+name+' '+r['source']+'!';ru=headers['egg_names'][name]
            t=headers['header_target']+headers['prose_target']+ru+' '+target+'!'
            anchors=[{'source':headers['header_source'],'target':headers['header_target']},{'source':name,'target':ru}]+definitions
            embedded=e['legacy'].replace('[Location message]',r['layouts'][0]);parent_flat=e['legacy'].replace('[Location message]',r['source'])
            out.append(entry(s,t,[embedded,parent_flat],'envelope',name,e,anchors))
    assert len(out)==1568 and len({r['source'] for r in out})==1568
    return out,blocks,headers

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--prepare',action='store_true');a=ap.parse_args();selected,blocks,headers=records()
    if not a.prepare:
        write(SOURCE/'EGG_PROBE_0.15.114.json',{'records':selected,'scope':'Offline finite composition;probe only.224 independent destinations,not1568 NPCs. Parent/plain and embedded-location styles are explicit test layouts,not claimed live packet capture.'});print('EGG114 probe',len(selected));return
    before=load(ROOT/'build/eggs114-snapshots-ca255c45160e470a9fd78747245859d7/0.15.113-dev/SNAPSHOTS.json');assert before['runtime_jar_sha256']==BASE;old={r['source']:r for r in before['records']};assert len(old)==1568
    cases=[]
    for r in selected:
        s=r['source'];t=r['translation'];assert old[s]['flat']==s and all(v['output']==s for v in old[s]['layouts'])
        for legacy in r['layouts']:
            assert re.sub('(?i)§[0-9a-fk-or]','',legacy)==s
            appearance=[style for style,text in runs(legacy) for c in text];covered=set()
            for d in r['style_anchors']:
                start=positions(s,d,'source',0)[0];end=start+len(d['source']);covered.update(range(start,end));assert len(set(appearance[start:end]))==1,(r['id'],d,legacy)
            base={appearance[i] for i,c in enumerate(s) if i not in covered and not c.isspace()};assert len(base)<=1,(r['id'],base)
        at=0;primary='§f'
        for n,d in enumerate(sorted(r['style_anchors'],key=lambda a:positions(s,a,'source',0)[0])):
            x=positions(s,d,'source',0)[0];primary+=s[at:x]+'§'+format(1+n%14,'x')+d['source']+'§f';at=x+len(d['source'])
        primary+=s[at:]
        for kind,legacy in [('primary',primary)]+[(f'published{n}',v) for n,v in enumerate(r['layouts'])]:cases.append({k:r[k] for k in ('source','translation','style_anchors','tag')}|{'kind':kind,'legacy':legacy})
    ids={o['block_id'] for r in selected for o in r['all_occurrences']};ids.update(r['parent_envelope']['block']['block_id'] for r in selected if r['parent_envelope']);kept=[b for b in blocks if b['block_id'] in ids]
    pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {'Chocolate Eggs','Chocolate Eggs/List'}];assert len(pages)==2
    write(DATA/'eggs-expansion114.json',{'format_version':1,'language':'ru','scope':'224 authored destination fragments plus1344 finite six-envelope compositions;prepared offline,not regex or new per-frame work. Not all live packet spellings.','records':[{k:r[k] for k in ('source','translation','style_anchors','tag','speaker','source_url')} for r in selected]})
    write(SOURCE/'EGG_SELECTED_0.15.114.json',{'records':selected});write(SOURCE/'EGG_CASES_0.15.114.json',{'cases':cases});write(SOURCE/'EGG_SOURCES_0.15.114.json',{'records':selected,'blocks':kept,'source_pages':pages,'headers':headers,'baseline_jar_sha256':BASE,'scope':'224 destinations/1344 explicit envelope combinations,not NPC counts;two documented template parents. Inline fragment and parent-inherited colors both tested. Final punctuation copied verbatim,not silently normalized.'})
    (ROOT/'src/main/resources/META-INF/licenses/EGGS114-SOURCES.txt').write_text('EGGS114 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\n224 authored locations and1344 offline composed complete notifications,not1568 NPCs.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n',encoding='utf-8')
    write(SOURCE/'EGG_COUNTS_0.15.114.json',{'records':1568,'locations':224,'compositions':1344,'cases':len(cases),'pages':2,'system_before':2341,'system_after':3909,'exact_before':13745,'exact_after':13969,'total':16244,'remaining':2275,'unverified_punctuation_forms_not_claimed':sum(r['kind']=='envelope' and r['source'].endswith('!!') for r in selected)})
    print('Prepared1568 records',len(cases),'style fixtures')

if __name__=='__main__':main()
