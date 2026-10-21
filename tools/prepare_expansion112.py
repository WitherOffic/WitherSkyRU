"""Pinned-corpus112 screening. Probe is not runtime coverage or a draft translation."""
import argparse,json,re
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from review_expansion111 import prefix,runs
from prepare_game110 import positions
CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
BASE='EE1D97D3CAF2A1BA7C821AEDD187D4C83D3309F6453D0C57245C99A2C5493563'
SNAP=ROOT/'build/expansion112-snapshots-3bb29a277d934369a43fae926d5b0301/0.15.111-dev/SNAPSHOTS.json'

def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def candidates():
    probe_rows=load(SOURCE/'EXPANSION_PROBE_0.15.112.json')['records'];by_id={r['id']:r for r in probe_rows};c=load(SOURCE/'EXPANSION_SKIN_COMPONENTS_0.15.112_REVIEWED.json');authored={};skin_ids=set()
    for line in (SOURCE/'EXPANSION_0.15.112_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        uid,t=line.split('|');assert uid not in authored;r=by_id[uid];p=prefix(r['source']);authored[uid]=(r['source'][:p]+t if r['tag']=='NPC' else t,[])
    direct={'Baby_Spinosaurus_Skin':'spino','Campfire_Power_Orb_Skin':'campfires','Corrupt_Wither_Goggles_Helmet_Skin':'goggles','Cute_Jellyfish_Skin':'jellies','Hungry_Flying_Fish_Skin':'axolotls','Ice_Cream_Cone_Power_Orb_Skin':'icecream'}
    for r in probe_rows:
        uid=r['id'];s=r['source'];page=r['source_url'].split('/w/')[-1].split('?')[0];match=re.fullmatch(r'(.+!) You swapped to the (.+) skin!',s)
        if match and page in set(direct)|{'Catgirl_Black_Cat_Skin','Harvester_Helmet_Skin'}:
            h,v=match.groups();rh=c['headers'][h]
            if page in direct:label=c[direct[page]][v]
            elif page=='Catgirl_Black_Cat_Skin':
                coat,name=v.rsplit(' ',1);mode,color=c['cat_coats'][coat];label=color+' '+c['cat_names'][name] if mode=='pre' else c['cat_names'][name]+' '+color
            else:
                noun=next(n for n in c['crops'] if v==n or v.endswith(' '+n));ru,gender=c['crops'][noun]
                color=v[:-len(noun)-1] if v!=noun else '';label=ru if not color else c['crop_colors'][color]['mfn'.index(gender)]+' '+ru[0].lower()+ru[1:]
            assert uid not in authored;authored[uid]=(f'{rh} Вы сменили облик на «{label}»!',[{'source':h,'target':rh},{'source':v,'target':label}]);skin_ids.add(uid)
        if page=='Bacte' and re.fullmatch(r'⚠ Get back in the arena or you will DIE!+ ⚠',s):
            assert uid not in authored;authored[uid]=('⚠ Вернитесь на арену, иначе вы УМРЁТЕ'+'!'*(s.count('!'))+' ⚠',[])
    assert len(skin_ids)==295
    before=load(SNAP);assert before['runtime_jar_sha256']==BASE;old={r['source']:r for r in before['records']};kept=[];retained=[]
    for uid,(t,anchors) in authored.items():
        r=dict(by_id[uid]);s=r['source'];assert re.findall('[0-9]+',s)==re.findall('[0-9]+',t),(uid,s,t)
        r.update(translation=t,style_anchors=anchors,archival=bool(r['published_row']['historical_heading']),cosmetic=uid in skin_ids)
        if old[s]['flat']==s and all(v['output']==s for v in old[s]['layouts']):kept.append(r)
        else:retained.append({'candidate':r,'baseline':old[s]})
    return kept,retained

def style_review():
    kept,retained=candidates()
    for r in kept:
        p=prefix(r['source']);defs=r['style_anchors']
        for value in r['layouts']:
            offset=0;parts=[]
            for style,text in runs(value):
                end=offset+len(text)
                if end>p:parts.append((style,text[max(0,p-offset):]))
                offset=end
            if not r['cosmetic'] and len({a for a,b in parts if b.strip()})>1:print(r['id'],json.dumps(parts,ensure_ascii=False),'RU',r['translation'][p:])
    print('New',len(kept),'NPC',sum(r['tag']=='NPC' for r in kept),'cosmetics',sum(r['cosmetic'] for r in kept),'retained',len(retained))

def prepare():
    kept,retained=candidates();assert len(kept)==482 and not retained
    styles=load(SOURCE/'EXPANSION_STYLES_0.15.112_REVIEWED.json');extra={}
    def definitions(value):return [{'source':s,'target':t} for s,t in value.items()] if isinstance(value,dict) else value
    for uid,v in styles['rows'].items():extra[uid]=definitions(v)
    for group in styles['groups']:
        for uid in group['ids']:assert str(uid) not in extra;extra[str(uid)]=definitions(group['pairs'])
    assert set(extra)<=set(r['id'] for r in kept);cases=[]
    for r in kept:
        uid=r['id'];s=r['source'];t=r['translation'];p=prefix(s);defs=list(r['style_anchors'])+list(extra.get(uid,[]))
        if 1053<=int(uid)<=1064:defs=[{'source':s[2:-2],'target':t[2:-2]}]
        assert set(re.findall('[A-Za-z_]+',t[p:]))<={'SkyBlock','Hypixel','Discord','Pit','x'},(uid,t)
        for d in defs:
            if any(len(positions(text,d,side,p))!=1 for side,text in [('source',s),('target',t)]):d['whole_word']=True
            assert all(len(positions(text,d,side,p))==1 for side,text in [('source',s),('target',t)]),(uid,d)
        for side,text in [('source',s),('target',t)]:
            spans=sorted((positions(text,d,side,p)[0],positions(text,d,side,p)[0]+len(d[side])) for d in defs)
            assert all(a[1]<=b[0] for a,b in zip(spans,spans[1:])),(uid,side,defs)
        covered={n for d in defs for n in range(positions(s,d,'source',p)[0],positions(s,d,'source',p)[0]+len(d['source']))}
        for legacy in r['layouts']:
            appearance=[style for style,text in runs(legacy) for c in text];assert len(appearance)==len(s)
            base={appearance[n] for n,c in enumerate(s) if n>=p and n not in covered and not c.isspace()};assert len(base)<=1,('Unreviewed style boundary',uid,base,legacy)
            for d in defs:
                start=positions(s,d,'source',p)[0];assert d['source']==d['target'] or len(set(appearance[start:start+len(d['source'])]))==1,('Mixed semantic fragment',uid,d)
        r['style_anchors']=defs
        at=p;primary='§e'+s[:p]+'§f' if p else '§f'
        for n,d in enumerate(sorted(defs,key=lambda d:positions(s,d,'source',p)[0])):
            start=positions(s,d,'source',p)[0];primary+=s[at:start]+'§'+format(1+n%14,'x')+d['source']+'§f';at=start+len(d['source'])
        primary+=s[at:]
        for kind,v in [('primary',primary)]+[(f'published{n}',v) for n,v in enumerate(r['layouts'])]:cases.append({'source':s,'translation':t,'legacy':v,'kind':kind,'style_anchors':defs,'tag':r['tag']})
    assert len({r['source'] for r in kept})==482
    for tag,resource in [('NPC','npc-expansion112.json'),('SYSTEM','system-expansion112.json')]:
        write(DATA/resource,{'format_version':1,'language':'ru','scope':'482 authored exact pinned messages;finite cosmetic components expanded offline. Existing startup merge,not new render/regex/network work.','records':[{k:r[k] for k in ('speaker','tag','source','translation','style_anchors','source_url')} for r in kept if r['tag']==tag]})
    ids={o['block_id'] for r in kept for o in r['all_occurrences']};blocks=[b for name in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for b in rows(CORPUS/name) if b['block_id'] in ids];pages=[p for p in rows(CORPUS/'source_pages.jsonl') if p['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'EXPANSION_SELECTED_0.15.112.json',{'records':kept});write(SOURCE/'EXPANSION_CASES_0.15.112.json',{'cases':cases});write(SOURCE/'EXPANSION_SOURCES_0.15.112.json',{'records':kept,'retained_not_new':retained,'source_pages':pages,'blocks':blocks,'baseline_jar_sha256':BASE,'scope':'Full pinned publications,not closed/live scripts. Only2 actual NPC literals;cosmetic notifications and SYSTEM text not counted as NPCs.'})
    stats={'new_records':482,'npc_records':2,'system_records':480,'cosmetic_records':295,'other_records':187,'archival_records':sum(r['archival'] for r in kept),'cases':len(cases),'published_layouts':len(cases)-482,'pages':len(pages),'exact_before':13262,'exact_after':13744,'total':16244,'without_exact_after':2500,'story_before':10761,'story_after':10763,'system_before':1861,'system_after':2341};write(SOURCE/'EXPANSION_PREPARATION_0.15.112.json',stats)
    license='EXPANSION112 public-source attribution (CC BY-NC-SA3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nFull pinned public messages;295 concrete cosmetic notifications expanded offline,not NPC counts or live-script certification.\n\n'+'\n'.join(f"{p['title']} | revision{p['revision']} | {p['source_url']}" for p in pages)+'\n';(ROOT/'src/main/resources/META-INF/licenses/EXPANSION112-SOURCES.txt').write_text(license,encoding='utf-8')
    print(json.dumps(stats))

def probe():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    work=rows(SOURCE/'NPC_WORKLIST_0.15.111.jsonl');occ={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')};out=[]
    for index,r in enumerate(work):
        s=r['source'];tag=r['tag'] or 'SYSTEM'
        if r['exact_registration'] or any(r[k] for k in ('variable_definition','unresolved_markup','alpha_page')) or tag not in ('NPC','SYSTEM','BOSS'):continue
        p=s.find(': ')+2 if tag in ('NPC','BOSS') else 0
        if tag in ('NPC','BOSS') and (p<2 or not s.startswith('['+tag+'] '+r['speaker']+': ')):continue
        body=s[p:];guarded=body.replace('[OPEN MENU]','').replace('[Vargul]','').replace('[Lvl 1]','')
        if not re.search('[A-Za-z]{2}',body) or re.search(r'[#$<>{}\[\]|]',guarded) or body.endswith('/'):continue
        layouts=list(dict.fromkeys(occ[k]['source_legacy_line'].strip() for k in r['occurrence_ids']))
        if not layouts or not all(re.sub('(?i)§[0-9a-fk-or]','',v)==s for v in layouts):continue
        out.append({'id':str(index),'source':s,'speaker':r['speaker'] if tag in ('NPC','BOSS') else '', 'tag':tag,'source_url':r['source_url'],'published_row':r,'all_occurrences':[occ[k] for k in r['occurrence_ids']],'layouts':layouts})
    write(SOURCE/'EXPANSION_PROBE_0.15.112.json',{'scope':'Literal source screening only;full-context/manual review and real111 receipt probe required. Not runtime additions or completeness certification.','records':out})
    (SOURCE/'EXPANSION_PROBE_0.15.112.txt').write_text('\n'.join(f"{r['id']}|{r['tag']}|{'ARCHIVE' if r['published_row']['historical_heading'] else ''}|{r['source_url'].split('/w/')[-1]}|{r['source']}" for r in out)+'\n',encoding='utf-8')
    print('Probed',len(out),'NPC',sum(r['tag']=='NPC' for r in out),'BOSS',sum(r['tag']=='BOSS' for r in out))

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--styles',action='store_true');ap.add_argument('--prepare',action='store_true');a=ap.parse_args()
    if a.styles:style_review()
    elif a.prepare:prepare()
    else:probe()
