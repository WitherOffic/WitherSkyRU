"""Materialize authored whole-source translations, retaining protected originals."""
import json,re
from pathlib import Path
from collections import Counter
P=Path(__file__).resolve().parents[1];S=P/'translation-source';D=P/'src/main/resources/data/witherskyru'
MARKER='# Batch 0.14 exact archived sources; tools/prepare_batch_0_14.py'
def rows(p):return [l.split('\t') for l in p.read_text(encoding='utf-8-sig').splitlines() if l.strip() and not l.startswith('#')]
def main():
    target=D/'continuation.tsv';base=target.read_text(encoding='utf-8')
    if MARKER in base:
        base,part=base.split(MARKER,1)
        prev=json.loads((S/'BATCH_0.14_DERIVATION.json').read_text(encoding='utf-8'))
        assert dict(l.split('\t') for l in part.splitlines() if l.strip())=={r['source']:r['translation'] for r in prev['generated_entries']}
    corpus={json.loads(l)['source']:json.loads(l) for l in (S/'corpus.jsonl').read_text(encoding='utf-8').splitlines()}
    previous=json.loads((S/'TRANSLATIONS_0.13.0_HISTORY.json').read_text(encoding='utf-8'))['translations']
    dictionary={}
    for name in ['phrases.tsv','vanilla-names.tsv','lexicon.tsv','curated.tsv','lore.tsv','continuation.tsv','enchantments.tsv','names.tsv','pet-lore.tsv']:
        raw=[l.split('\t') for l in base.splitlines() if l.strip() and not l.startswith('#')] if name=='continuation.tsv' else rows(D/name)
        dictionary.update({r[0]:r[1] for r in raw})
    added={};reason={};auth={};numeric=[]
    for table in sorted(S.glob('BATCH_0.14_*.tsv')):
        for src,dst in rows(table):
            assert src not in auth,(table.name,src)
            auth[src]=dst
            if re.search(r'\{N\d+\}',src):numeric.append((src,dst))
    def put(src,dst,why):
        if src not in corpus or dictionary.get(src,src)!=src or previous.get(src,src)!=src:return
        if src!=dst and src not in added:added[src]=dst;reason[src]=why
    for src,dst in auth.items():
        if not re.search(r'\{N\d+\}',src):put(src,dst,'authored exact archived source')
    reforges=dict(rows(S/'BATCH_0.14_REFORGES.tsv'))
    titles=dict(rows(S/'BATCH_0.14_ROLES.tsv'))
    for src in corpus:
        m=re.fullmatch(r'Applies the (.+) reforge( when)?',src)
        if m and m[1] in reforges:put(src,'Накладывает перековку «'+reforges[m[1]]+'»'+(' при' if m[2] else ''),'closed reforge lore')
        m=re.fullmatch(r'(.+) Bonus',src)
        if m and m[1] in reforges:put(src,'Бонус перековки «'+reforges[m[1]]+'»','closed reforge bonus')
        for a,b in [('Enigma: ','Энигма: '),('Pickaxe Ability: ','Способность кирки: '),('Axe Ability: ','Способность топора: ')]:
            if src.startswith(a) and src[len(a):] in auth:put(src,b+auth[src[len(a):]],'exact observed heading using authored label')
        if 'name' in corpus[src]['roles']:
            for title,ru in sorted(titles.items(),key=lambda r:-len(r[0])):
                prefix=title+' '
                if src.startswith(prefix) and re.fullmatch(r'[A-Z][A-Za-z]+',src[len(prefix):]):
                    put(src,ru+' '+src[len(prefix):],'closed NPC title; proper name retained')
                    break
        m=re.fullmatch(r'between (#[0-9A-F]{6}) and (#[0-9A-F]{6})!',src)
        if m:put(src,'между '+m[1]+' и '+m[2]+'!','observed dye description; color codes unchanged')
        m=re.fullmatch(r'to (#[0-9A-F]{6})!',src)
        if m:put(src,'на '+m[1]+'!','observed dye description; color codes unchanged')
        m=re.fullmatch(r'(.+) to use (that colour|that gemstone|that shark|that season|the day version|the night version|the Barbarian faction version|the Mage faction version)',src)
        if m:
            choice={'that colour':'этот цвет','that gemstone':'этот самоцвет','that shark':'эту акулу','that season':'этот сезон','the day version':'дневную версию','the night version':'ночную версию','the Barbarian faction version':'версию фракции варваров','the Mage faction version':'версию фракции магов'}[m[2]]
            put(src,m[1]+' — чтобы использовать '+choice,'configuration explanation; literal selection key retained')
    for src,dst in numeric:
        tokens=re.findall(r'\{N\d+\}',src);assert len(tokens)==len(set(tokens)) and set(tokens)==set(re.findall(r'\{N\d+\}',dst))
        regex=re.escape(src)
        for t in tokens:regex=regex.replace(re.escape(t),r'(\d+(?:[,.]\d+)*)')
        pat=re.compile(regex)
        for actual in corpus:
            match=pat.fullmatch(actual)
            if match:
                translated=dst
                for t,v in zip(tokens,match.groups()):translated=translated.replace(t,v)
                put(actual,translated,'authored numeric definition; observed literal only')
    target.write_text(base.rstrip('\n')+'\n'+MARKER+'\n'+''.join(k+'\t'+v+'\n' for k,v in sorted(added.items())),encoding='utf-8')
    report={'revision':'0.14.0-dev','imported_rows':len(added),'categories':dict(Counter(reason.values())),
            'generated_entries':[{'source':k,'translation':v,'reason':reason[k]} for k,v in sorted(added.items())],
            'new_npc_chat_replicas':0,'runtime_verified':False,'full_server_coverage':False}
    (S/'BATCH_0.14_DERIVATION.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'imported':len(added),'categories':report['categories']},ensure_ascii=False))
if __name__=='__main__':main()
