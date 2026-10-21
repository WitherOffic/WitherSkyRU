"""Import closed authored strings; materialize only observed literal families."""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT=Path(__file__).resolve().parents[1]
SOURCE=PROJECT/'translation-source'
DATA=PROJECT/'src/main/resources/data/witherskyru'
MARKER='# Batch 0.13 closed captions; tools/prepare_batch_0_13.py'

def rows(path):
    return [s.split('\t') for s in path.read_text(encoding='utf-8-sig').splitlines() if s.strip() and not s.startswith('#')]

def main():
    path=DATA/'continuation.tsv'
    original=path.read_text(encoding='utf-8')
    if MARKER in original:
        original,section=original.split(MARKER,1)
        previous=json.loads((SOURCE/'BATCH_0.13_DERIVATION.json').read_text(encoding='utf-8'))
        assert dict(s.split('\t') for s in section.splitlines() if s.strip())=={r['source']:r['translation'] for r in previous['generated_entries']}
    corpus={json.loads(s)['source']:json.loads(s) for s in (SOURCE/'corpus.jsonl').read_text(encoding='utf-8').splitlines()}
    baseline=json.loads((SOURCE/'TRANSLATIONS_0.12.0_HISTORY.json').read_text(encoding='utf-8'))['translations']
    dictionary={}
    for filename in ('phrases.tsv','vanilla-names.tsv','lexicon.tsv','curated.tsv','lore.tsv','continuation.tsv','enchantments.tsv','names.tsv','pet-lore.tsv'):
        content=[s.split('\t') for s in original.splitlines() if s.strip() and not s.startswith('#')] if filename=='continuation.tsv' else rows(DATA/filename)
        dictionary.update({r[0]:r[1] for r in content})
    added,reasons={},{}
    def put(src,dst,reason):
        if src not in corpus or baseline.get(src,src)!=src or dictionary.get(src,src)!=src:return
        if src not in added and src!=dst:
            added[src],reasons[src]=dst,reason
    authored={}
    for table in sorted(SOURCE.glob('BATCH_0.13_*.tsv')):
        if table.name.endswith(('NUMBERED.tsv','REVIEWED.tsv')):continue
        for src,dst in rows(table):
            assert src not in authored,(table.name,src)
            if re.search(r'\{N\d+\}',src):continue
            authored[src]=dst
    for src,dst in authored.items():put(src,dst,'authored exact source')
    def known(src):
        value=authored.get(src,dictionary.get(src,baseline.get(src)))
        return value if value and value!=src else None
    reforges={r[0]:r[1] for r in rows(DATA/'reforges.tsv')}
    for src,record in corpus.items():
        if 'reforgeName' in record['roles'] and src in reforges:
            put(src,reforges[src],'observed reforge display name; established morphology')
        m=re.fullmatch(r'Applies the (.+) reforge( when)?',src)
        if m and m[1] in reforges:
            put(src,'Накладывает перековку «'+reforges[m[1]]+'»'+(' при' if m[2] else ''),'observed reforge lore; established morphology')
        m=re.fullmatch(r'(.+) Bonus',src)
        if m and m[1] in reforges:
            put(src,'Бонус перековки «'+reforges[m[1]]+'»','observed reforge bonus heading')
        # Full literal gem clauses only; no loose global replacement of names.
        gems={'Amber':'янтаря','Amethyst':'аметиста','Aquamarine':'аквамарина','Citrine':'цитрина','Jade':'нефрита','Jasper':'яшмы','Onyx':'оникса','Opal':'опала','Peridot':'перидота','Ruby':'рубина','Sapphire':'сапфира','Topaz':'топаза'}
        for en,ru in gems.items():
            closed={f'A near perfect version of {en}.':f'Почти идеальная разновидность {ru}.',
                    f'A perfectly refined {en}.':f'Идеально обработанный образец {ru}.',
                    f'A slightly better version of {en},':f'Слегка улучшенная разновидность {ru},',
                    f'A slightly better version of {en}, but':f'Слегка улучшенная разновидность {ru}, но',
                    f'A type of {en} that has clearly':f'Разновидность {ru}, которую явно',
                    f'A type of {en} that has':f'Разновидность {ru}, которую',
                    f'A type of {en} that has clearly been':f'Разновидность {ru}, которую явно',
                    f'{en}, but it could still use some':f'{ru}, но ещё требуется немного',
                    f'{en}, but it could still use':f'{ru}, но ещё требуется'}
            if src in closed:put(src,closed[src],'observed literal gemstone lore')
        for prefix,target in [('Enigma: ','Энигма: '),('Stats ➜ ','Характеристики ➜ '),('Reforge Bonus: ','Бонус перековки: '),('Piece Bonus: ','Бонус части: ')]:
            if src.startswith(prefix) and known(src[len(prefix):]):
                put(src,target+known(src[len(prefix):]),'observed exact closed heading')
        m=re.fullmatch(r'(.+) \(Master Mode\)',src)
        if m and known(m[1]):put(src,known(m[1])+' (мастер-режим)','observed boss display name')
        if src.startswith('Ability: '):
            if src.rstrip() in authored:
                put(src,authored[src.rstrip()]+src[len(src.rstrip()):],'observed ability heading; literal whitespace variant')
            body=src[len('Ability: '):].rstrip()
            action=''
            for en,ru in [('HOLD RIGHT CLICK','УДЕРЖИВАЙТЕ ПКМ'),('RIGHT CLICK','ПКМ'),('LEFT CLICK','ЛКМ'),('ON SHOOT','ПРИ ВЫСТРЕЛЕ'),('ON HIT','ПРИ ПОПАДАНИИ'),('PASSIVE','ПАССИВНО'),('SNEAK','ПРИ ПРИСЕДАНИИ')]:
                if body.endswith('  '+en):body,action=body[:-(len(en)+2)],'  '+ru;break
            if known(body):put(src,'Способность: '+known(body)+action,'observed ability heading using authored label')
    numeric=rows(SOURCE/'BATCH_0.13_NUMBERED.tsv')+[r for r in rows(SOURCE/'BATCH_0.13_CONTEXT_TAILS.tsv') if re.search(r'\{N\d+\}',r[0])]
    for src,dst in numeric:
        tokens=re.findall(r'\{N\d+\}',src)
        assert tokens and len(tokens)==len(set(tokens)) and set(tokens)==set(re.findall(r'\{N\d+\}',dst))
        offset,pieces=0,[]
        for m in re.finditer(r'\{N\d+\}',src):
            pieces.extend([re.escape(src[offset:m.start()]),r'(\d+(?:[,.]\d+)*)']);offset=m.end()
        pieces.append(re.escape(src[offset:]));regex=re.compile(''.join(pieces))
        for observed in corpus:
            m=regex.fullmatch(observed)
            if m:
                target=dst
                for token,value in zip(tokens,m.groups()):target=target.replace(token,value)
                put(observed,target,'literal numeric lore observed in snapshot')
    for src,dst in rows(SOURCE/'BATCH_0.13_REVIEWED.tsv'):
        assert src in corpus and baseline.get(src,src)!=src and src not in dictionary and src not in added
        added[src],reasons[src]=dst,'reviewed existing partial source; not counted as new'
    path.write_text(original.rstrip('\n')+'\n'+MARKER+'\n'+''.join(k+'\t'+v+'\n' for k,v in sorted(added.items())),encoding='utf-8')
    report={'revision':'0.13.0-dev','imported_rows':len(added),'categories':dict(Counter(reasons.values())),
            'generated_entries':[{'source':k,'translation':v,'reason':reasons[k]} for k,v in sorted(added.items())],
            'runtime_verified':False,'npc_new_dialogues':0,'full_server_coverage':False}
    (SOURCE/'BATCH_0.13_DERIVATION.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'imported':len(added),'categories':report['categories']},ensure_ascii=False))

if __name__=='__main__':main()
