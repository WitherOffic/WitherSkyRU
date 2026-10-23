"""Materialize independently authored UI expectations; import only with --import.

The TSV is the authored target, never generated from the runtime under test.
"""
import json, re, hashlib, zipfile, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def digest(b):return hashlib.sha256(b).hexdigest()
def main():
    rows=[json.loads(l) for l in (ROOT/'translation-source/BULK117_002_SOURCE.jsonl').read_text(encoding='utf-8').splitlines()]
    contexts=read(ROOT/'translation-source/BULK117_002_CONTEXTS.json');targets={}
    for l in (ROOT/'translation-source/BULK117_002_TARGETS.tsv').read_text(encoding='utf-8').splitlines():
        if not l or l.startswith('#'):continue
        a,b=l.split('\t',1);assert int(a) not in targets;targets[int(a)]=b
    assert len(rows)==len(targets)==1000
    with zipfile.ZipFile(ROOT/'translation-source/coverage117-001/exports.zip') as z:
        existing={re.sub(r'\s+',' ',r['source'].strip()).lower() for r in map(json.loads,z.read('runtime-inventory.jsonl').decode().splitlines())}
    baseline={r['id']:r for r in map(json.loads,(ROOT/'build/bulk117002/baseline.jsonl').read_text(encoding='utf-8').splitlines())}
    expected=[];production=[];seen=set();shapes=set();numeric=re.compile(r'\d+(?:[.,]\d+)*')
    for row in rows:
        source=row['source'];target=targets[row['batch_index']];legacy=row['legacy'];ctx=contexts[row['context_id']];key=re.sub(r'\s+',' ',source).lower()
        assert key not in existing and key not in seen and source!=target;seen.add(key)
        shape=re.sub(r'\b[IVXLCDM]{1,9}\b','#ROMAN',numeric.sub('#',source)).lower();assert shape not in shapes;shapes.add(shape)
        assert numeric.findall(source)==numeric.findall(target),row['batch_index']
        assert re.findall(r'/[a-z][a-z0-9_]*',source)==re.findall(r'/[a-z][a-z0-9_]*',target)
        assert source==re.sub(r'(?i)§[0-9a-fk-or]','',legacy).strip()
        assert all(baseline[row['source_id']][k]==source for k in ('dictionary','component','tooltip'))
        assert not re.search(r'§[0-9a-fk-or]',legacy[2:]) and legacy.startswith('§')
        raw_text=legacy[2:];lead=len(raw_text)-len(raw_text.lstrip(' '));trail=len(raw_text)-len(raw_text.rstrip(' '))
        expected_legacy=legacy[:2]+' '*lead+target+' '*trail
        actual={'source':source,'translation':target,'parts':[[source,target]]}
        production.append(actual)
        expected.append(actual|{'id':row['source_id'],'batch_index':row['batch_index'],'legacy':legacy,'expected_legacy':expected_legacy,
            'context_rows':ctx['lines'],'context_row_index':row['context_row_index'],'context_id':row['context_id'],
            'origin':ctx['origin'],'kind':'wiki_ui_literal','baseline':baseline[row['source_id']]})
    b=ROOT/'../../rebuild/WitherSkyRU/build/five-pass117-20261021/repositories/NotEnoughUpdates--NotEnoughUpdates-REPO'
    with zipfile.ZipFile(next(b.glob('*.zip'))) as z:
        path=next(n for n in z.namelist() if n.endswith('/items/HAMSTER_WHEEL.json'));item=json.loads(z.read(path));assert digest(z.read(path))=='c6f12505d28a0172ea74d99f6ab5a4fda07a59e1457a5c7c1ddcc5472e884756'
    # A pre-existing dictionary target: one style fix, explicitly not a new translation.
    source='your minion by 50% for 24';target='вашего миньона на 50% на 24'
    extra={'source':source,'translation':target,'parts':[['your minion by ','вашего миньона на '],['50%','50%'],[' for 24',' на 24']],
        'batch_index':197,'legacy':item['lore'][3],'expected_legacy':'§7вашего миньона на §a50%§7 на 24',
        'context_rows':[item['displayname']]+item['lore'],'context_row_index':4,
        'expected_context_legacy':'§7на §a50%§7 в течение 24','kind':'already_covered_context','counted_as_new':False,'origin':{'source_url':'https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/blob/20a302457375ddfd7005e192486ac86781729b0a/items/HAMSTER_WHEEL.json','file_sha256':'c6f12505d28a0172ea74d99f6ab5a4fda07a59e1457a5c7c1ddcc5472e884756'}}
    fixture={'scope':'1000 actual public UI literal rows, no numeric-family padding; independent authored targets; offline acceptance required',
        'source_sha256':digest((ROOT/'translation-source/BULK117_002_SOURCE.jsonl').read_bytes()),
        'targets_sha256':digest((ROOT/'translation-source/BULK117_002_TARGETS.tsv').read_bytes()),'records':expected,'supplemental':[extra]}
    write(ROOT/'translation-source/BULK117_002_ACCEPTANCE.json',fixture)
    if '--import' in sys.argv:
        # Never overwrite accepted previous batches; extend the single existing startup map.
        resource=ROOT/'src/main/resources/data/witherskyru/bulk117-002.json';assert not resource.exists()
        write(resource,{'source_attribution':'Hypixel SkyBlock Wiki contributors, pinned revision URLs/raw SHA in BULK117_002_ACCEPTANCE.json; CC-BY-NC-SA-3.0. Hamster Wheel source: NEU-REPO MIT, notices retained.',
            'records':production})
        path=ROOT/'src/main/resources/data/witherskyru/lore.tsv';body=path.read_text(encoding='utf-8');path.write_text(body.rstrip('\n')+'\n\n# bulk117-002:1000 authored UI literals; Hamster Wheel already works in full context, no duplicate TSV row\n'+''.join(r['source']+'\t'+r['translation']+'\n' for r in production),encoding='utf-8')
    print('PASS authored1000, unique source/numeric families, protected commands, old JAR3-route misses; supplemental pre-existing full-context case1')
if __name__=='__main__':main()
