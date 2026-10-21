"""Read-only full-message/style review against the pinned candidate probe."""
import json,re
from prepare_npc78 import ROOT,SOURCE

def authored():
    out=[]
    for line in (SOURCE/'GAME_0.15.110_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        index,target=line.split('|');out.append((int(index),target))
    assert len(out)==len({i for i,t in out})
    return out

def runs(value):
    style=('default','');out=[];at=0
    for m in re.finditer('(?i)§([0-9a-fk-or])',value):
        if m.start()>at:
            part=value[at:m.start()]
            if out and out[-1][0]==style:out[-1]=(style,out[-1][1]+part)
            else:out.append((style,part))
        c=m[1].lower()
        if c in '0123456789abcdef':style=(c,'')
        elif c=='r':style=('default','')
        else:style=(style[0],''.join(sorted(set(style[1]+c))))
        at=m.end()
    if at<len(value):
        part=value[at:]
        if out and out[-1][0]==style:out[-1]=(style,out[-1][1]+part)
        else:out.append((style,part))
    return out

if __name__=='__main__':
    r=json.loads((SOURCE/'GAME_CANDIDATES_0.15.110.json').read_text(encoding='utf-8'))['records']
    baseline=json.loads((ROOT/'build/game110-snapshots-d2025d44a02445f08fb60087a919ce62/0.15.109-dev/SNAPSHOTS.json').read_text(encoding='utf-8-sig'))
    b={s['source']:s for s in baseline['records']}
    for i,t in authored():
        s=r[i]['source'];prior=b[s]
        if prior['flat']!=s or any(v['output']!=s for v in prior['layouts']):print('ALREADY CHANGED',i,s)
        if re.findall('[0-9]+',s)!=re.findall('[0-9]+',t):print('NUMBER MISMATCH',i,s,'=>',t)
        for legacy in r[i]['layouts']:
            v=runs(legacy);visible=[(a,x) for a,x in v if x.strip()]
            if len({a for a,x in visible})>1:print(i,json.dumps(v,ensure_ascii=False),'TARGET',t)
    print('Authored',len(authored()))
