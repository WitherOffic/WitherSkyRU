"""Compile authored single-username plans offline, with pinned publication evidence.

No placeholder is registered as an exact key. No translation service is used.
"""
import json,re
from collections import Counter,defaultdict
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write,anchors

CORPUS=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1'
TOKEN=re.compile(r'<(?:player|Username|username|playername|Player|player name|Player Name)>|\[(?:player|Player|player name|Player Name|username|Player.s Name)\]')
def canonical(s):return TOKEN.sub('{name}',s)

def segment(source,target,definitions):
    terms=[]
    for a in definitions:
        x,y=a['source'],a['target']
        if x not in source:continue
        assert source.count(x)==target.count(y)==1,(source,target,a)
        terms.append((source.index(x),target.index(y),x,y))
    terms.sort();parts=[];s=t=0
    for a,b,x,y in terms+[(len(source),len(target),'','')]:
        assert a>=s and b>=t,(source,target,definitions)
        if a>s:parts.append({'start':s,'end':a,'text':target[t:b]})
        else:assert b==t,('Cannot insert text without an explicit style source',source,target,t,b)
        if x:parts.append({'start':a,'end':a+len(x),'text':y})
        s=a+len(x);t=b+len(y)
    assert ''.join(p['text'] for p in parts)==target
    return {'source':source,'target':target,'parts':parts}

def main():
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(CORPUS/'translation_queue.jsonl');lookup=defaultdict(list)
    for r in queue:
        if r['tag']=='NPC' and not any(r[k] for k in ('historical_heading','alpha_page','unresolved_markup','nonstandard_tag_layout')) and len(TOKEN.findall(r['source']))==1:
            lookup[canonical(r['source'])].append(r)
    occurrences={r['occurrence_id']:r for r in rows(CORPUS/'dialogue_occurrences.jsonl')}
    exact={r['source'] for p in DATA.glob('*.json') for r in json.loads(p.read_text(encoding='utf-8-sig')).get('records',[]) if r.get('source') and r.get('translation')}
    exact.update(r['source'] for r in queue if not r['variable_definition'] and not TOKEN.search(r['source']))
    templates=[];evidence=[];seen=set()
    for line in (SOURCE/'NPC_NAMES_0.15.103_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines():
        if not line or line.startswith('#'):continue
        f=line.split('|');assert len(f) in (3,4)
        speaker,english,russian=f[:3];prefix=f'[NPC] {speaker}: '
        source=prefix+english;target=prefix+russian
        assert source in lookup and source not in seen,source
        assert english.count('{name}')==russian.count('{name}')==1 and english!=russian
        assert not re.search(r'<[^>]+>|\[[^]]+\]|#',english)
        assert re.findall(r'[0-9]+',english)==re.findall(r'[0-9]+',russian)
        assert set(re.findall('[A-Za-z]+',russian))<= {'name','SkyBlock','I'},source
        seen.add(source);definitions=anchors(f[3]) if len(f)==4 else []
        left,right=english.split('{name}');toleft,toright=russian.split('{name}')
        for a in definitions:
            assert (a['source'] in left and a['target'] in toleft) or (a['source'] in right and a['target'] in toright),(source,a)
        if left.startswith('✆ '):
            assert toleft.startswith('✆ ');prefix+='✆ ';left=left[2:];toleft=toleft[2:]
        excluded=[]
        for fixed in exact:
            lead=prefix+left
            if fixed.startswith(lead) and fixed.endswith(right):
                end=len(fixed)-len(right) if right else len(fixed);capture=fixed[len(lead):end]
                if re.fullmatch('[A-Za-z0-9_]{1,16}',capture):excluded.append(capture)
        entry={'prefix':prefix,'before':segment(left,toleft,definitions),'after':segment(right,toright,definitions),'override_keys':[source]+[a['source'] for a in definitions],'excluded_names':sorted(set(excluded))}
        templates.append(entry)
        evidence.append({'source':source,'translation':target,'speaker':speaker,'style_anchors':definitions,'queue_rows':lookup[source],
            'layouts':list(dict.fromkeys(occurrences[i]['source_legacy_line'] for r in lookup[source] for i in r['occurrence_ids']))})
    write(DATA/'npc-names103.json',{'schema':1,'capture':'one ASCII Minecraft username, 1..16 characters; no heads or arbitrary variables','templates':templates})
    ids={occurrences[i]['block_id'] for e in evidence for r in e['queue_rows'] for i in r['occurrence_ids']}
    blocks=[r for f in ('dialogue_blocks.jsonl','supplemental_dialogue_blocks.jsonl') for r in rows(CORPUS/f) if r['block_id'] in ids]
    pages=[r for r in rows(CORPUS/'source_pages.jsonl') if r['title'] in {b['page'] for b in blocks}]
    write(SOURCE/'NPC_NAMES_SOURCES_0.15.103.json',{'scope':'Pinned public templates, not a live server capture or all NPC coverage. Placeholders not counted as exact translations.','queue_sha256':sha(CORPUS/'translation_queue.jsonl'),'records':evidence,'blocks':blocks,'source_pages':pages})
    license_text='NPC103 public-source attribution (CC BY-NC-SA 3.0)\nhttps://creativecommons.org/licenses/by-nc-sa/3.0/\n\nRussian translations and closed username plans adapted from the pinned dialogue sources below.\nNo claim of current live server completeness.\n\n'
    license_text+='\n'.join(f"{p['title']} | revision {p['revision']} | https://hypixelskyblock.minecraft.wiki/w/{p['title'].replace(' ','_')}?oldid={p['revision']}" for p in pages)+'\n'
    (ROOT/'src/main/resources/META-INF/licenses/NPC103-SOURCES.txt').write_text(license_text,encoding='utf-8')
    print(json.dumps({'templates':len(templates),'speakers':len({e['speaker'] for e in evidence}),'queue_rows':sum(len(e['queue_rows']) for e in evidence),'layouts':sum(len(e['layouts']) for e in evidence),'pages':len(pages),'max_rules_per_prefix':max(Counter(t['prefix'] for t in templates).values())}))

if __name__=='__main__':main()
