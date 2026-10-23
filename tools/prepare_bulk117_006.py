"""Continue the pinned UI pool, including genuine fixed menu captions.

No translations are generated; baseline/editorial acceptance are separate.
"""
import json,re,collections,sys
from pathlib import Path
from build_bulk117_003 import NAMES,key,shape
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/bulk117006'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    pool=read(ROOT/'build/bulk117002/pool.json');existing=set();shapes=set()
    for name in NAMES:
        for l in (ROOT/f'src/main/resources/data/witherskyru/{name}.tsv').read_text(encoding='utf-8').splitlines():
            if '\t' in l and not l.startswith('#'):
                s=l.split('\t')[0];existing.add(key(s));shapes.add(shape(s))
    known_bad=set()
    for batch in ('003','004','005'):
        x=read(ROOT/f'translation-source/BULK117_{batch}_EXCLUSIONS.json')
        known_bad.update(r['source'] for r in x.get('editorial_and_baseline',[]))
    known_bad|={'Spawn inside the dangerous Void','Also accessible via','Melon Collection.'}
    old_changed={r['source'] for l in (ROOT/'build/bulk117005/coverage-after/coverage.jsonl').open(encoding='utf-8') if (r:=json.loads(l))['offline_changed']}
    blocked=('Hypixel/','Alpha','Achievements/','Bingo/Events','April Fools','Resistance','Reward Bundles','Mayor Election/Events/','Raffle','Centauri','City Projects/','/2020','/2021','/2022','/2023','/2024','/2025','/2026')
    bodies=[];titles=[]
    for r in pool['records']:
        s=r['source'];legacy=r['legacy'];page=r['origins'][0]['page']
        if len(s)<9 or ' ' not in s or not s.isascii() or not legacy.startswith('§') or '§' in legacy[2:]:continue
        if key(s) in existing or shape(s) in shapes or s in known_bad or s in old_changed:continue
        if (any(t in page for t in blocked) and page!='Achievements/UI') or page in ('Dante/UI','Cookie Clicker/UI','Candidate Aura/UI','Mino/UI','Rezzus/UI','Salesman/UI'):continue
        if re.fullmatch(r'Hex #[A-Fa-f0-9]{6}',s) or re.search(r'(?<!\w)#(?!\w)|\[(?:season|#date|n-th)\]',s):continue
        if re.search(r'(?:very ){2,}',s):continue
        if 'Skin/' in page or 'Skins/' in page or page=='Bazaar/UI/Oddities/Normal Enchantments':continue
        if re.match(r"^(?:Milestone|Agatha's Milestone) [IVXLCDM]+$",s):continue
        if r['context_row_index']==0:
            # Only fixed captions, never exemplars of editable/player/pet fields or IDs.
            if any(p in page for p in ('Island NPC/','Profile/','Player/')):continue
            if re.search(r'\[|\]|_|\d|^/|https?:|%|^Hex\b',s) or s.isupper():continue
            titles.append(r)
        else:bodies.append(r)
    order=lambda r:(r['origins'][0]['page'],r['context_id'],r['context_row_index'])
    bodies.sort(key=order);titles.sort(key=order)
    by_id={r['source_id']:r for r in bodies};back=read(ROOT/'translation-source/BULK117_005_REMAINING.json')
    ordered=[by_id[r['source_id']] for r in back['records'] if r['source_id'] in by_id]+bodies+titles
    selected=[];seen=set();newshapes=set()
    for r in ordered:
        if r['source_id'] in seen or shape(r['source']) in newshapes:continue
        seen.add(r['source_id']);newshapes.add(shape(r['source']))
        selected.append(r|{'batch_index':len(selected),'origins':[r['origins'][0]],'baseline_jar':'0.16.17-dev','kind':'wiki_ui_title' if r['context_row_index']==0 else 'wiki_ui_literal'})
        if len(selected)==2400:break
    contexts={r['context_id']:pool['contexts'][r['context_id']] for r in selected}
    write(OUT/'review-pool.json',selected);write(OUT/'contexts.json',contexts)
    groups={}
    for r in selected:
        g=groups.setdefault(r['context_id'],{'surface':'tooltip','lines':contexts[r['context_id']]['lines'],'ids':[None]*len(contexts[r['context_id']]['lines'])})
        g['ids'][r['context_row_index']]=r['source_id']
    with (OUT/'probes.jsonl').open('w',encoding='utf-8') as f:
        for g in groups.values():f.write(json.dumps(g,ensure_ascii=False)+'\n')
    print('CANDIDATES',len(selected),'available descriptions',len(bodies),'available fixed captions',len(titles),'kinds',dict(collections.Counter(r['kind'] for r in selected)))
    print('QUEUE_UI_MATCHES',[(r['source'],r['origins'][0]['page'],r['context_id'],r['context_row_index']) for r in pool['records'] if r['source']=='The Real Zoo Shady'])
    assert len(selected)>=1000,'Save partial inputs; need additional genuine sources, never numeric padding'
if __name__=='__main__':main()
