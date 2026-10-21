"""Read-only source/target and style review;old changed outputs never counted as new."""
import json,re
from prepare_npc78 import ROOT,SOURCE
from review_game110 import runs
def prefix(source):
    p=source.index(': ')+2 if source.startswith('[NPC] ') else 0
    return p+2 if source.startswith('✆ ',p) else p
def authored():
    probe={r['id']:r for r in json.loads((SOURCE/'EXPANSION_PROBE_0.15.111.json').read_text(encoding='utf-8'))['records']}
    baseline=json.loads((ROOT/'build/expansion111-snapshots-6c3264d7e33d4a83a681cc8ffb7db812/0.15.110-dev/SNAPSHOTS.json').read_text(encoding='utf-8-sig'));old={r['source']:r for r in baseline['records']};out=[];retained=[];seen=set()
    for line in (SOURCE/'EXPANSION_0.15.111_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        uid,body=line.split('|');assert uid not in seen;seen.add(uid);r=probe[uid];s=r['source'];p=prefix(s);t=s[:p]+body if r['tag']=='NPC' else body
        assert re.findall('[0-9]+',s)==re.findall('[0-9]+',t),(uid,s,t)
        r=dict(r,translation=t)
        if old[s]['flat']==s and all(v['output']==s for v in old[s]['layouts']):out.append(r)
        else:retained.append(dict(candidate=r,baseline=old[s]))
    assert len(out)==502 and len(retained)==3
    return out,retained
if __name__=='__main__':
    out,retained=authored()
    for r in out:
        p=prefix(r['source'])
        for v in r['layouts']:
            body=v;full=runs(v);offset=0;parts=[]
            for style,text in full:
                end=offset+len(text)
                if end>p:parts.append((style,text[max(0,p-offset):]))
                offset=end
            if len({a for a,b in parts if b.strip()})>1:print(r['id'],json.dumps(parts,ensure_ascii=False),'RU',r['translation'][p:])
    print('New',len(out),'NPC',sum(r['tag']=='NPC' for r in out),'retained',len(retained))
