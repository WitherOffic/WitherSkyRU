"""Same-denominator old/new JAR coverage of frozen sources plus this batch.

No downloads, target generation, or inference of whole-game translation coverage.
"""
import collections, hashlib, json, shutil, sys
from pathlib import Path
import coverage117 as accounting
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'build/bulk117007'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def rows(p):
    with p.open(encoding='utf-8-sig') as f:
        for l in f:
            if l.strip():yield json.loads(l)
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def jsonl(p,values):
    with p.open('w',encoding='utf-8',newline='\n') as f:
        for v in values:f.write(json.dumps(v,ensure_ascii=False)+'\n')
def prepare():
    old=ROOT/'build/bulk117006/coverage-after';fixture=read(ROOT/'translation-source/BULK117_007_ACCEPTANCE.json')
    corpus={r['id']:r for r in rows(old/'corpus.jsonl')};original_size=len(corpus);new=0
    groups={}
    for r in fixture['records']:
        if r['id'] not in corpus:
            corpus[r['id']]={'id':r['id'],'source':r['source'],'corpora':['actual_ui_bulk117_007'],
                'origins':[{'url':r['origin']['source_url'],'context_id':r['context_id']}],'flags':[]};new+=1
        assert corpus[r['id']]['source']==r['source']
        group=groups.setdefault(r['context_id'],{'surface':'tooltip','ids':[None]*len(r['context_rows']),'lines':r['context_rows']})
        group['ids'][r['context_row_index']]=r['id']
    for name in ('coverage-before','coverage-after'):
        p=BASE/name;p.mkdir(parents=True,exist_ok=True);jsonl(p/'corpus.jsonl',corpus.values())
        shutil.copyfile(old/'probes.jsonl',p/'probes.jsonl')
        with (p/'probes.jsonl').open('a',encoding='utf-8',newline='\n') as f:
            for g in groups.values():f.write(json.dumps(g,ensure_ascii=False)+'\n')
    write(BASE/'coverage-expansion.json',{'previous_size':original_size,'expanded_size':len(corpus),'new_outside_previous_corpus':new,'added_probe_groups':len(groups)})
    print('PREPARED',original_size,'->',len(corpus),'added actual unique sources',new)
def summarize():
    for name in ('coverage-before','coverage-after'):
        accounting.OUT=BASE/name;accounting.summarize()
    before={r['id']:r for r in rows(BASE/'coverage-before/coverage.jsonl')}
    after={r['id']:r for r in rows(BASE/'coverage-after/coverage.jsonl')}
    assert before.keys()==after.keys()
    lost=[r['source'] for i,r in before.items() if r['offline_changed'] and not after[i]['offline_changed']]
    deltas=[{'id':i,'source':r['source'],'before':r['offline_outputs'],'after':after[i]['offline_outputs']} for i,r in before.items() if r['offline_outputs']!=after[i]['offline_outputs']]
    fixture=read(ROOT/'translation-source/BULK117_007_ACCEPTANCE.json')
    for r in fixture['records']:
        b,a=before[r['id']],after[r['id']]
        assert not b['russian_exact_target']
        assert a['russian_exact_target'] and a['offline_changed']
        assert r['translation'] in {s.strip() for s in a['offline_outputs']}
    assert not lost,lost
    write(BASE/'runtime-delta.json',deltas)
    report=read(BASE/'coverage-expansion.json')|{
        'before_same_expanded_corpus':read(BASE/'coverage-before/summary.json')['unique_union'],
        'after':read(BASE/'coverage-after/summary.json')['unique_union'],
        'old_changed_sources_lost':lost,'changed_source_cases':len(deltas),
        'new_exact_rows_with_prior_other_context_output':[r['source'] for r in fixture['records'] if before[r['id']]['offline_changed']],
        'observed_occurrences':sum(r['probe_occurrences'] for r in after.values()),
        'method':'Actual old0.16.18/new0.16.20 public JARs, identical expanded frozen probes; exact Russian key coverage is not whole-game/editorial coverage.'}
    write(ROOT/'translation-source/workflow/bulk117-007-coverage.json',report)
    print(json.dumps(report,ensure_ascii=False))
if __name__=='__main__':{'prepare':prepare,'summarize':summarize}[sys.argv[1]]()
