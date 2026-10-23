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
def main():
    targets={}
    for l in (DATA/'BULK117_003_TARGETS.tsv').read_text(encoding='utf-8').splitlines():
        if not l or l.startswith('#'):continue
        a,b=l.split('\t',1);assert int(a) not in targets;targets[int(a)]=b
    assert len(targets)==1000
    pool=read(ROOT/'build/bulk117003/review-pool.json');selected=[r for r in pool if r['batch_index'] in targets]
    assert len(selected)==1000
    contexts_all=read(ROOT/'build/bulk117003/contexts.json')
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
    for p in ('baseline.jsonl','extra/baseline.jsonl'):
        baseline.update({r['id']:r for r in map(json.loads,(ROOT/'build/bulk117003'/p).read_text(encoding='utf-8').splitlines())})
    fixture=[];production=[];seen=set();shapes=set()
    for r in selected:
        source=r['source'];target=targets[r['batch_index']];legacy=r['legacy'];context=contexts[r['context_id']]
        assert key(source) not in existing|seen,source;seen.add(key(source))
        assert shape(source) not in existing_shapes|shapes,source;shapes.add(shape(source))
        assert re.findall(r'\d+(?:[.,]\d+)*',source)==re.findall(r'\d+(?:[.,]\d+)*',target),r['batch_index']
        # LVL is an English label, not a Roman level. True level tokens stay literal.
        assert re.findall(r'\b(?!LVL\b)[IVXLCDM]{1,9}\b',source)==re.findall(r'\b(?!LVL\b)[IVXLCDM]{1,9}\b',target),r['batch_index']
        assert re.findall(r'/[a-z][a-z0-9_]*',source)==re.findall(r'/[a-z][a-z0-9_]*',target)
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
    api=DATA/'coverage117-001/snapshots/api-resources-skyblock-items.json'
    assert sha(api.read_bytes())=='adb76eae35cbce2f18f01b947f9d914967b36e53895adf615e39efbd9d2c4fe8'
    item=read(api)['items'][5285];assert item['id']=='SHINIEST'
    colors={'gray':'7','aqua':'b','dark_gray':'8','italic':'o'}
    description=re.sub(r'%%([a-z_]+)%%',lambda m:'§'+colors[m[1]],item['description']).splitlines()
    extra={'source':"Purchased from Agatha's Shop!",'translation':'Куплено в магазине Агаты!',
        'parts':[['Purchased from ','Куплено в '],["Agatha's Shop",'магазине Агаты'],['!','!']],
        'legacy':description[0],'expected_legacy':'§7Куплено в §bмагазине Агаты§7!',
        'expected_context_legacy':'§7Куплено в §bмагазине Агаты§7!',
        'context_rows':['§6Shiniest']+description,'context_row_index':1,
        'kind':'existing_key_style_fix','counted_as_new':False,
        'origin':{'source_url':'https://api.hypixel.net/v2/resources/skyblock/items','json_path':'items[5285].description[0]',
            'item_id':'SHINIEST','file_sha256':sha(api.read_bytes())},'baseline':baseline['agatha518']}
    assert extra['baseline']['dictionary']==extra['translation']
    assert all(extra['baseline'][k]==extra['source'] for k in ('component','tooltip'))
    sources=DATA/'BULK117_003_SOURCE.jsonl'
    sources.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in selected),encoding='utf-8')
    write(DATA/'BULK117_003_CONTEXTS.json',contexts)
    write(DATA/'BULK117_003_EXCLUSIONS.json',{'numeric_family_variants':read(ROOT/'build/bulk117003/numeric-exclusions.json'),
        'editorial':[{'batch_index':468,'reason':'Russian editorial output is only unchanged SkyBlock brand; do not count as Russian translation'},
            {'batch_index':558,'reason':'Void Sepulture needs adjoining title fragment; defer coherent semantic plan'},
            {'batch_index':711,'reason':'Unescaped command slash in wiki UI slot; cannot certify actual source row'},
            {'batch_index':913,'reason':'Feather Collection item describes Melon Collection; inconsistent public UI source'}]})
    write(DATA/'BULK117_003_ACCEPTANCE.json',{'scope':'1000 independently authored actual UI literals; one existing-key style repair separately',
        'source_sha256':sha(sources.read_bytes()),'targets_sha256':sha((DATA/'BULK117_003_TARGETS.tsv').read_bytes()),
        'raw_pages_verified':len(pages),'unique_slots':len(contexts),'records':fixture,'supplemental':[extra]})
    if '--import' in sys.argv:
        resource=ROOT/'src/main/resources/data/witherskyru/bulk117-003.json';assert not resource.exists()
        write(resource,{'source_attribution':'Hypixel SkyBlock Wiki contributors, CC-BY-NC-SA-3.0; pinned revisions/raw hashes in BULK117_003_ACCEPTANCE.json. Agatha source: public Hypixel API items snapshot; existing target retained. Notices preserved.',
            'records':production+[{k:extra[k] for k in ('source','translation','parts')}]})
        p=ROOT/'src/main/resources/data/witherskyru/lore.tsv';body=p.read_text(encoding='utf-8')
        p.write_text(body.rstrip('\n')+'\n\n# bulk117-003:1000 authored UI literals; Agatha existing-key style repair has no duplicate TSV key\n'+''.join(r['source']+'\t'+r['translation']+'\n' for r in production),encoding='utf-8')
    print(f'PASS authored1000; raw pages{len(pages)}, slots{len(contexts)}; old JAR3-route misses1000; existing-key style miss1')
if __name__=='__main__':main()
