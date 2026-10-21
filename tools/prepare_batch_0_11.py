"""Closed exact captions and numbered lore; no game, Java, network or word guessing."""
from collections import Counter
import json
from pathlib import Path
import re

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / 'translation-source'
DATA = PROJECT / 'src/main/resources/data/witherskyru'
MARKER = '# Batch 0.11 authored captions; tools/prepare_batch_0_11.py'

def rows(path):
    return [line.split('\t') for line in path.read_text(encoding='utf-8-sig').splitlines()
            if line.strip() and not line.startswith('#')]

def main():
    path = DATA / 'continuation.tsv'
    original = path.read_text(encoding='utf-8')
    if MARKER in original:
        original, section = original.split(MARKER, 1)
        report = json.loads((SOURCE / 'BATCH_0.11_DERIVATION.json').read_text(encoding='utf-8'))
        if dict(line.split('\t') for line in section.splitlines() if line.strip()) != {r['source']: r['translation'] for r in report['generated_entries']}:
            raise ValueError('Own block edited independently; reconcile first')
    corpus = {json.loads(line)['source'] for line in (SOURCE / 'corpus.jsonl').read_text(encoding='utf-8').splitlines()}
    dictionary = {}
    for filename in ('phrases.tsv','vanilla-names.tsv','lexicon.tsv','curated.tsv','lore.tsv','continuation.tsv','enchantments.tsv','names.tsv','pet-lore.tsv'):
        content = [line.split('\t') for line in original.splitlines() if line.strip() and not line.startswith('#')] if filename == 'continuation.tsv' else rows(DATA / filename)
        dictionary.update({r[0]:r[1] for r in content})
    added, reasons = {}, {}
    def put(src, dst, reason):
        if src not in dictionary and src not in added and src != dst:
            added[src], reasons[src] = dst, reason
    for filename in ('BATCH_0.11_ATTRIBUTES.tsv','BATCH_0.11_AUTHORED.tsv'):
        for src,dst in rows(SOURCE / filename):
            put(src,dst,'authored exact caption or lore')
    # Runtime's existing level/name wrapping generates only observed corpus rows.
    # Numbered patterns are anchored literal phrases, not arbitrary prose swaps.
    for src,dst in rows(SOURCE / 'BATCH_0.11_NUMBERED.tsv'):
        tokens = re.findall(r'\{N\d+\}', src)
        assert set(tokens) == set(re.findall(r'\{N\d+\}',dst)) and len(tokens) == len(set(tokens))
        pieces,offset = [],0
        for match in re.finditer(r'\{N\d+\}',src):
            pieces.extend([re.escape(src[offset:match.start()]),r'(\d+(?:[,.]\d+)*)'])
            offset=match.end()
        pieces.append(re.escape(src[offset:]))
        regex=re.compile(''.join(pieces))
        for observed in corpus:
            match=regex.fullmatch(observed)
            if match:
                target=dst
                for token,value in zip(tokens,match.groups()): target=target.replace(token,value)
                put(observed,target,'authored numbered lore observed in snapshot')
    # Explicit colour + explicitly authored grammatical noun forms; item captions only.
    items=json.loads((SOURCE / 'item-context.json').read_text(encoding='utf-8'))
    colours={r[0]:r[1:] for r in rows(SOURCE / 'BATCH_0.11_COLOURS.tsv')}
    nouns={r[0]:r[1:] for r in rows(SOURCE / 'BATCH_0.11_COLOUR_OBJECTS.tsv')}
    for item in items:
        name=item['name']
        for colour,forms in colours.items():
            prefix=colour+' '
            if name.startswith(prefix) and name[len(prefix):] in nouns:
                noun,gender=nouns[name[len(prefix):]]
                put(name,forms[{'m':0,'f':1,'n':2,'p':3}[gender]]+' '+noun,'observed coloured object caption')
    path.write_text(original.rstrip('\n')+'\n'+MARKER+'\n'+''.join(k+'\t'+v+'\n' for k,v in sorted(added.items())),encoding='utf-8')
    report={'revision':'0.11.0-dev','imported_rows':len(added),'categories':dict(Counter(reasons.values())),
            'generated_entries':[{'source':k,'translation':v,'reason':reasons[k]} for k,v in sorted(added.items())],
            'numeric_variants':'Only actual observed strings; no synthetic inflation','npc_new_dialogues':0,'runtime_verified':False}
    (SOURCE / 'BATCH_0.11_DERIVATION.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'imported':len(added),'categories':report['categories']},ensure_ascii=False))

if __name__ == '__main__': main()
