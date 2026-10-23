"""Pinned actual UI selection; colour spans retain their original order and roles.

This prepares sources, never generates translations. Captured JAR18 results and
independently authored Russian parts are required before any production import.
"""
import collections,json,re
from pathlib import Path
from build_bulk117_003 import NAMES,key,shape
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/bulk117007'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def segments(legacy):
    if not legacy.startswith('§') or re.search(r'§[^0-9a-f]',legacy):return None
    chunks=[]
    for colour,text in re.findall(r'§([0-9a-f])([^§]*)',legacy):
        if not text:continue
        if chunks and chunks[-1][0]==colour:chunks[-1][1]+=text
        else:chunks.append([colour,text])
    if not chunks:return None
    chunks[0][1]=chunks[0][1].lstrip(' ');chunks[-1][1]=chunks[-1][1].rstrip(' ')
    if any(not part for colour,part in chunks):return None
    return chunks
def main():
    pool=read(ROOT/'build/bulk117002/pool.json');existing=set();oldshapes=set()
    for name in NAMES:
        for line in (ROOT/f'src/main/resources/data/witherskyru/{name}.tsv').read_text(encoding='utf-8').splitlines():
            if '\t' in line and not line.startswith('#'):
                s=line.split('\t',1)[0];existing.add(key(s));oldshapes.add(shape(s))
    blocked=('Hypixel/','Alpha','Bingo/Events','April Fools','Resistance','Reward Bundles','Mayor Election/Events/','Raffle','Centauri','City Projects/','/2020','/2021','/2022','/2023','/2024','/2025','/2026')
    skip_pages={'Dante/UI','Cookie Clicker/UI','Candidate Aura/UI','Mino/UI','Rezzus/UI','Salesman/UI','Warren/UI'}
    previous_bad=set()
    for batch in ('003','004','005','006'):
        d=read(ROOT/f'translation-source/BULK117_{batch}_EXCLUSIONS.json')
        previous_bad.update(r['source'] for r in d.get('editorial_and_baseline',[]))
    pending=read(ROOT/'translation-source/BULK117_006_REMAINING.json')
    pending_ids={r['source_id'] for r in pending['records']}
    plain=[];multi=[];counts=collections.Counter()
    for r in pool['records']:
        s=r['source'];page=r['origins'][0]['page'];parts=segments(r['legacy'])
        if not parts or not s.isascii() or not 4<=len(s)<=170 or key(s) in existing or shape(s) in oldshapes or s in previous_bad:continue
        if any(w in page for w in blocked) or page in skip_pages or 'Skin/' in page or 'Skins/' in page:continue
        if re.search(r'[#_]|\[|\]|%%|https?://|(?:very ){2,}',s) or re.search(r'\b[IVXLCDM]+\b',s.replace('I Heart','Iheart')):continue
        if 'Level Prefix' in s or re.search(r'^(?:Blue|Red|Yellow|Green|Purple|Orange|Pink|Light Blue) (?:Arrow|Paint|Teleport Pad)$',s):continue
        if len(parts)==1:
            if r['source_id'] not in pending_ids and not(page=='Achievements/UI' and r['context_row_index']==0):continue
            if 'Contact Directory' in page or page=='Chocolate Factory/UI' or '/Profile' in page or page=='Warmer Wizard Face/UI':continue
            if s.isupper() or re.search(r'\d|^/',s):continue
            plain.append(r|{'segments':parts});continue
        if r['context_row_index']==0 or not 2<=len(parts)<=5:continue
        if not re.search(r'\b(?:you|your|Click|View|Gain|Grants|Obtain|Unlock|Reach|Requires|Increases|Reduces|Complete|Cost|per|from|when|while|with|for|to|the)\b',s,re.I):continue
        if not re.search('[A-Za-z]{3}',parts[0][1]):continue
        multi.append(r|{'segments':parts});counts[page]+=1
    order=lambda r:(r['origins'][0]['page'],r['context_id'],r['context_row_index'])
    plain.sort(key=lambda r:(r['source']!='I Heart Ice Cream',r['origins'][0]['page'],r['context_id']))
    multi.sort(key=order);seen=set();newshapes=set();selected=[]
    for r in plain+multi:
        if r['source_id'] in seen or shape(r['source']) in newshapes:continue
        seen.add(r['source_id']);newshapes.add(shape(r['source']))
        selected.append(r|{'batch_index':len(selected),'origins':[r['origins'][0]],'baseline_jar':'0.16.18-dev','kind':'wiki_ui_colour_spans' if len(r['segments'])>1 else 'wiki_ui_title'})
        if len(selected)==2300:break
    contexts={r['context_id']:pool['contexts'][r['context_id']] for r in selected};write(OUT/'review-pool.json',selected);write(OUT/'contexts.json',contexts)
    groups={}
    for r in selected:
        c=contexts[r['context_id']];g=groups.setdefault(r['context_id'],{'surface':'tooltip','lines':c['lines'],'ids':[None]*len(c['lines'])});g['ids'][r['context_row_index']]=r['source_id']
    (OUT/'probes.jsonl').write_text(''.join(json.dumps(g,ensure_ascii=False)+'\n' for g in groups.values()),encoding='utf-8')
    print('POOL',len(selected),'plain',len(plain),'multi available',len(multi),'kinds',dict(collections.Counter(r['kind'] for r in selected)))
    print('TOP',counts.most_common(12));print('RING',[(r['source'],r['origins'][0]['page'],r['legacy']) for r in pool['records'] if r['source']=='The Ring'])
if __name__=='__main__':main()
