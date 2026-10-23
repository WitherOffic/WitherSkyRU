"""Import one authored batch, with pinned raw UI provenance and old-JAR evidence.

Targets are authored before importing production data; no runtime-generated targets.
"""
import hashlib, json, re, sys
from pathlib import Path
from prepare_bulk117_002 import slot, WIKI, split_parameters

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'translation-source'
NAMES = ('phrases','vanilla-names','lexicon-names','curated','lore','continuation','enchantments','names','pet-lore','catalogue','corrections','quality-fixes','proofread','quest-interface')
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(b): return hashlib.sha256(b).hexdigest()
def write(p,v): p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def key(s): return re.sub(r'\s+',' ',s.strip()).casefold()
def shape(s):
    s=re.sub(r'(?i)\d+(?:[.,]\d+)*\s*(?:[kmbt](?=\b))','#',s)
    return re.sub(r'\b[IVXLCDM]{1,9}\b','#ROMAN',re.sub(r'\d+(?:[.,]\d+)*','#',s)).casefold()
def roman_levels(text):
    result=[]
    for token in re.finditer(r'\b(?!LVL\b)[IVXLCDM]{1,9}\b',text):
        # English first-person contractions/verbs are not level I.
        if token[0]=='I' and re.match(r"(?:['’]|\s+(?:can|will)\b)",text[token.end():]):continue
        result.append(token[0])
    return result
def main():
    targets={}
    for l in (DATA/'BULK117_004_TARGETS.tsv').read_text(encoding='utf-8').splitlines():
        if not l or l.startswith('#'):continue
        a,b=l.split('\t',1);assert int(a) not in targets;targets[int(a)]=b
    assert len(targets)==1000
    assert not set(targets)&{r['batch_index'] for r in read(ROOT/'build/bulk117004/editorial-exclusions.json')}
    pool=read(ROOT/'build/bulk117004/review-pool.json');selected=[r for r in pool if r['batch_index'] in targets]
    assert len(selected)==1000
    contexts_all=read(ROOT/'build/bulk117004/contexts.json')
    contexts={r['context_id']:contexts_all[r['context_id']] for r in selected}
    # Revalidate the cache against pinned raw pages and the actual UI slot fields.
    pages={c['origin']['page'] for c in contexts.values()};verified=set()
    for l in WIKI.open(encoding='utf-8'):
        page=json.loads(l)
        if page['title'] not in pages:continue
        raw=page['raw_wikitext'];assert sha(raw.encode())==page['raw_sha256'].lower()
        for c in contexts.values():
            o=c['origin']
            if o['page']!=page['title']:continue
            assert o['revision']==page['revision'] and o['raw_sha256']==page['raw_sha256']
            block=raw[o['template_start']:o['template_end']]
            assert sha(block.encode())==o['template_raw_sha256']
            active=re.sub(r'<!--[\s\S]*?-->',lambda m:' '*(m.end()-m.start()),block)
            args=split_parameters(active[2:-2]);values=[]
            for n,arg in enumerate(args[2:],2):
                arg=arg.strip();k=str(n)
                if '=' in arg:
                    k,arg=arg.split('=',1);k=k.strip()
                if k==o['slot']:values.append(arg)
            assert o['slot_raw'] in values and slot(o['slot_raw'])==c['lines'],o
        verified.add(page['title'])
        if verified==pages:break
    assert verified==pages
    existing=set();existing_shapes=set()
    for name in NAMES:
        for l in (ROOT/f'src/main/resources/data/witherskyru/{name}.tsv').read_text(encoding='utf-8').splitlines():
            if '\t' in l and not l.startswith('#'):
                source=l.split('\t',1)[0];existing.add(key(source));existing_shapes.add(shape(source))
    baseline={}
    for p in ('baseline.jsonl',):
        baseline.update({r['id']:r for r in map(json.loads,(ROOT/'build/bulk117004'/p).read_text(encoding='utf-8').splitlines())})
    fixture=[];production=[];seen=set();shapes=set()
    for r in selected:
        source=r['source'];target=targets[r['batch_index']];legacy=r['legacy'];context=contexts[r['context_id']]
        assert key(source) not in existing|seen,source;seen.add(key(source))
        assert shape(source) not in existing_shapes|shapes,source;shapes.add(shape(source))
        assert re.findall(r'\d+(?:[.,]\d+)*',source)==re.findall(r'\d+(?:[.,]\d+)*',target),r['batch_index']
        # LVL is an English label, not a Roman level. True level tokens stay literal.
        assert roman_levels(source)==roman_levels(target),r['batch_index']
        assert re.findall(r'(?<![A-Za-z0-9_])/[a-z][a-z0-9_]*',source)==re.findall(r'(?<![A-Za-z0-9_])/[a-z][a-z0-9_]*',target)
        assert re.search('[А-Яа-яЁё]',target) and source!=target
        assert all(baseline[r['source_id']][k]==source for k in ('dictionary','component','tooltip')),source
        assert legacy.startswith('§') and '§' not in legacy[2:]
        assert re.sub('§.','',legacy).strip()==source
        assert context['lines'][r['context_row_index']]==legacy
        text=legacy[2:];lead=len(text)-len(text.lstrip(' '));trail=len(text)-len(text.rstrip(' '))
        plan={'source':source,'translation':target,'parts':[[source,target]]};production.append(plan)
        fixture.append(plan|{'id':r['source_id'],'batch_index':r['batch_index'],'legacy':legacy,
            'expected_legacy':legacy[:2]+' '*lead+target+' '*trail,'context_rows':context['lines'],
            'context_row_index':r['context_row_index'],'context_id':r['context_id'],'origin':context['origin'],
            'kind':'wiki_ui_literal','baseline':baseline[r['source_id']]})
    sources=DATA/'BULK117_004_SOURCE.jsonl'
    sources.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in selected),encoding='utf-8')
    write(DATA/'BULK117_004_CONTEXTS.json',contexts)
    write(DATA/'BULK117_004_EXCLUSIONS.json',{'editorial_and_baseline':read(ROOT/'build/bulk117004/editorial-exclusions.json'),'already_changed_in_old_corpus':read(ROOT/'build/bulk117004/selection-exclusions.json')})
    write(DATA/'BULK117_004_ACCEPTANCE.json',{'scope':'1000 independently authored actual UI literals; no supplemental new key',
        'source_sha256':sha(sources.read_bytes()),'targets_sha256':sha((DATA/'BULK117_004_TARGETS.tsv').read_bytes()),
        'raw_pages_verified':len(pages),'unique_slots':len(contexts),'records':fixture,'supplemental':[]})
    if '--import' in sys.argv:
        resource=ROOT/'src/main/resources/data/witherskyru/bulk117-004.json';assert not resource.exists()
        write(resource,{'source_attribution':'Hypixel SkyBlock Wiki contributors, CC-BY-NC-SA-3.0; pinned revisions/raw hashes in BULK117_004_ACCEPTANCE.json. Notices preserved; no wiki numeric placeholders or numeric/color-family padding.',
            'records':production})
        p=ROOT/'src/main/resources/data/witherskyru/lore.tsv';body=p.read_text(encoding='utf-8')
        p.write_text(body.rstrip('\n')+'\n\n# bulk117-004:1000 authored UI literals; baseline misses in all three display routes\n'+''.join(r['source']+'\t'+r['translation']+'\n' for r in production),encoding='utf-8')
    print(f'PASS authored1000; raw pages{len(pages)}, slots{len(contexts)}; old JAR3-route misses1000')
if __name__=='__main__':main()
