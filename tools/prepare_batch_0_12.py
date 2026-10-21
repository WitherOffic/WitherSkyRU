"""Materialize authored, closed captions and literal numbered lore. No game/network."""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT=Path(__file__).resolve().parents[1]
SOURCE=PROJECT/'translation-source'
DATA=PROJECT/'src/main/resources/data/witherskyru'
MARKER='# Batch 0.12 closed captions; tools/prepare_batch_0_12.py'

def rows(path):
    return [s.split('\t') for s in path.read_text(encoding='utf-8-sig').splitlines() if s.strip() and not s.startswith('#')]

def main():
    path=DATA/'continuation.tsv'
    original=path.read_text(encoding='utf-8')
    if MARKER in original:
        original,section=original.split(MARKER,1)
        previous=json.loads((SOURCE/'BATCH_0.12_DERIVATION.json').read_text(encoding='utf-8'))
        if dict(s.split('\t') for s in section.splitlines() if s.strip())!={r['source']:r['translation'] for r in previous['generated_entries']}:
            raise ValueError('Own block was edited independently; reconcile first')
    corpus={json.loads(s)['source'] for s in (SOURCE/'corpus.jsonl').read_text(encoding='utf-8').splitlines()}
    items=json.loads((SOURCE/'item-context.json').read_text(encoding='utf-8'))
    names={r['name'] for r in items}
    dictionary={}
    for filename in ('phrases.tsv','vanilla-names.tsv','lexicon.tsv','curated.tsv','lore.tsv','continuation.tsv','enchantments.tsv','names.tsv','pet-lore.tsv'):
        content=[s.split('\t') for s in original.splitlines() if s.strip() and not s.startswith('#')] if filename=='continuation.tsv' else rows(DATA/filename)
        dictionary.update({r[0]:r[1] for r in content})
    added,reasons,retained={},{},[]
    def put(src,dst,reason,support=False):
        if src in dictionary and dictionary[src]!=src:
            if dictionary[src]!=dst:retained.append({'source':src,'retained':dictionary[src],'candidate':dst})
            return
        if src not in added and src!=dst and (src in corpus or support):
            added[src],reasons[src]=dst,reason
    for src,dst in rows(SOURCE/'BATCH_0.12_AUTHORED.tsv'):
        put(src,dst,'authored exact caption or lore',True)
    def known(src):
        return added.get(src,dictionary.get(src))
    locations={r[0]:r[1:] for r in rows(SOURCE/'BATCH_0.12_LOCATIONS.tsv')}
    for src,(nom,direction) in locations.items():
        put(src,nom,'authored location caption',True)
    for name in names:
        m=re.fullmatch(r'(Portal to |Travel Scroll to )(.+)',name)
        if m:
            destination=m[2]
            if destination.startswith('the '):destination=destination[4:]
            if destination in locations:
                put(name,('Портал ' if m[1]=='Portal to ' else 'Свиток перемещения ')+locations[destination][1],'observed portal/scroll destination')
    reforges={r[0]:r[1] for r in rows(DATA/'reforges.tsv')}
    for source in corpus:
        if source.startswith('Stats ➜ '):
            label=known(source[len('Stats ➜ '):])
            if label:put(source,'Характеристики ➜ '+label,'observed stat interface heading')
        if source.startswith('Quiver '):
            name=source[len('Quiver '):]
            if name in names and known(name):put(source,'Колчан: '+known(name),'observed quiver caption')
        if ' - ' in source and not re.search(r'[\\{}\[\]$^]',source):
            left,right=source.split(' - ',1)
            if left in locations and right in locations:
                put(source,locations[left][0]+' — '+locations[right][0],'observed fast-travel interface heading')
        for prefix,suffix,target in [('',' Warps','Перемещения: '),('',' Collection','Коллекция: '),('',' RNG Meter','Счётчик случайной добычи: ')]:
            if source.endswith(suffix):
                base=source[:-len(suffix)]
                label=known(base)
                if label and not re.search(r'[\\{}\[\]$^]',base):put(source,target+label,'observed closed menu title')
        m=re.fullmatch(r'Applies the (.+) reforge( when)?',source)
        if m and m[1] in reforges:
            put(source,'Накладывает перековку «'+reforges[m[1]]+'»'+(' при' if m[2] else ''),'observed reforge instruction using established name')
    for src,dst in rows(SOURCE/'BATCH_0.12_NUMBERED.tsv'):
        tokens=re.findall(r'\{N\d+\}',src)
        assert set(tokens)==set(re.findall(r'\{N\d+\}',dst)) and len(tokens)==len(set(tokens))
        offset,pieces=0,[]
        for m in re.finditer(r'\{N\d+\}',src):
            pieces.extend([re.escape(src[offset:m.start()]),r'(\d+(?:[,.]\d+)*)'])
            offset=m.end()
        pieces.append(re.escape(src[offset:]))
        regex=re.compile(''.join(pieces))
        for observed in corpus:
            m=regex.fullmatch(observed)
            if m:
                target=dst
                for token,value in zip(tokens,m.groups()):target=target.replace(token,value)
                put(observed,target,'authored numeric lore observed in snapshot')
    path.write_text(original.rstrip('\n')+'\n'+MARKER+'\n'+''.join(k+'\t'+v+'\n' for k,v in sorted(added.items())),encoding='utf-8')
    report={'revision':'0.12.0-dev','imported_rows':len(added),'categories':dict(Counter(reasons.values())),
            'retained_previous_candidates':retained,'generated_entries':[{'source':k,'translation':v,'reason':reasons[k]} for k,v in sorted(added.items())],
            'runtime_verified':False,'npc_new_dialogues':0,'full_server_coverage':False}
    (SOURCE/'BATCH_0.12_DERIVATION.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'imported':len(added),'categories':report['categories']},ensure_ascii=False))

if __name__=='__main__':main()
