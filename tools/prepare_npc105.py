"""Compile eleven explicitly reviewed wording revisions; no Java/runtime algorithm changes.

Historical golden files remain immutable. An isolated, labelled regression snapshot
supersedes only the eleven authored targets and their semantic target anchors.
"""
import copy,json,re,shutil
from prepare_npc78 import ROOT,DATA,SOURCE,sha,write,anchors,validate

SNAPSHOT=ROOT/'translation-source-regression105'

def main():
    frozen=json.loads((SOURCE/'NPC_STYLE_SOURCES_0.15.104.json').read_text(encoding='utf-8'))
    originals={}
    for p in DATA.glob('*.json'):
        if p.name.startswith('npc-styles'):continue
        for r in json.loads(p.read_text(encoding='utf-8-sig')).get('records',[]):
            if r.get('source') in {x['source'] for x in frozen['gaps']}:
                originals[r['source']]=(p,r)
    revisions=[]
    for line in (SOURCE/'NPC_WORDING_0.15.105_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        i,body,primary,alternate=line.split('|');i=int(i)
        gap=frozen['gaps'][i];s=gap['source'];prefix=s[:s.index(': ')+2];target=prefix+body
        assert not re.search('[A-Za-z]',body),(s,body)
        p,old=originals[s];assert old['translation'] in (gap['flat_result'],target)
        primary,alternate=anchors(primary),anchors(alternate)
        validate(s,target,primary);validate(s,target,alternate)
        evidence=next(r for r in frozen['records'] if r['source']==s) if i not in (0,3,13,25,52) else next(r for r in frozen['pending'] if r['index']==i)
        # Derive the old plan from the immutable delivered104 resource, not this working tree.
        import zipfile
        with zipfile.ZipFile(ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.104-dev.jar') as z:
            old=next(r for r in json.loads(z.read('data/witherskyru/'+p.name))['records'] if r['source']==s)
        revisions.append({'index':i,'source':s,'old_translation':gap['flat_result'],'translation':target,
            'primary_resource':p.name,'old_primary_anchors':old.get('style_anchors',[]),
            'primary_anchors':primary,'style_anchors':alternate,'queue_row':evidence['queue_row']})
    assert len(revisions)==11 and len({r['source'] for r in revisions})==11
    by_source={r['source']:r for r in revisions}
    # Only the two primary resources and existing startup style table change.
    for name in sorted({r['primary_resource'] for r in revisions}):
        p=DATA/name;doc=json.loads(p.read_text(encoding='utf-8-sig'))
        for record in doc['records']:
            if record.get('source') not in by_source:continue
            r=by_source[record['source']];record['translation']=r['translation']
            if r['primary_anchors']:record['style_anchors']=r['primary_anchors']
            else:record.pop('style_anchors',None)
        write(p,doc)
    p=DATA/'npc-styles104.json';doc=json.loads(p.read_text(encoding='utf-8-sig'))
    old_index={r['source']:i for i,r in enumerate(doc['records'])}
    for r in revisions:
        record={'speaker':r['source'][6:r['source'].index(': ')],'source':r['source'],
            'translation':r['translation'],'style_anchors':r['style_anchors'],
            'source_url':r['queue_row']['source_url'],'evidence':'explicit_wording_revision105_primary_and_alternate_reviewed'}
        if r['source'] in old_index:doc['records'][old_index[r['source']]]=record
        else:doc['records'].append(record)
    assert len(doc['records'])==72
    doc['scope']='72 published alternate layouts. Eleven targets explicitly revised in105; old primary source spans retained. Startup table only.'
    write(p,doc)
    write(SOURCE/'NPC_WORDING_SOURCES_0.15.105.json',{'scope':'Eleven explicit wording/anchor revisions; not eleven new exact keys. Historical104 evidence is retained unchanged.','records':revisions})
    # Independent new fixtures include published layout and the retained primary
    # layout with independently constructed colors. No runtime-data-derived targets.
    cases=[];tsv=['# case index | independently reviewed anchors']
    for r in revisions:
        s=r['source'];prefix=s.index(': ')+2
        for kind in ('published','primary'):
            defs=r['style_anchors'] if kind=='published' else r['primary_anchors']
            if kind=='published':legacy=r['queue_row']['source_legacy_line'].strip()
            else:
                legacy='§e'+s[:prefix]+'§f';at=prefix
                for n,a in enumerate(sorted(defs,key=lambda a:s.index(a['source'],prefix))):
                    start=s.index(a['source'],prefix)
                    legacy+=s[at:start]+'§'+str(n+1)+a['source']+'§f';at=start+len(a['source'])
                legacy+=s[at:]
            assert re.sub('§[0-9a-fk-or]','',legacy)==s
            tsv.append(str(len(cases))+'|'+json.dumps(defs,ensure_ascii=False))
            cases.append({'source':s,'flat_result':r['translation'],'legacy':legacy,'kind':kind})
    write(SOURCE/'NPC_WORDING_CASES_0.15.105.json',{'cases':cases})
    (SOURCE/'NPC_WORDING_STYLES_0.15.105.tsv').write_text('\n'.join(tsv)+'\n',encoding='utf-8')
    # Isolated supersession: replace only a matching full target or a target anchor
    # inside a matching full source record. Never alter English source/input evidence.
    SNAPSHOT.mkdir(exist_ok=False)
    changes=[]
    def revised_anchor(a,r):
        replacements={x['source']:x['target'] for x in r['primary_anchors']+r['style_anchors']}
        if isinstance(a,dict) and a.get('source') in replacements:a['target']=replacements[a['source']]
        return a
    def visit(value,context=None):
        if isinstance(value,list):return [visit(v,context) for v in value]
        if not isinstance(value,dict):return value
        source=value.get('source');context=by_source.get(source,context) if isinstance(source,str) else context
        out={k:visit(v,context) for k,v in value.items()}
        if context:
            for key in ('translation','flat_result','target','published_style_result','runtime_colored_snapshot_not_quality_proof'):
                if out.get(key)==context['old_translation']:out[key]=context['translation']
            if isinstance(out.get('source'),str) and out['source'] in {a['source'] for a in context['primary_anchors']+context['style_anchors']}:revised_anchor(out,context)
        return out
    for p in SOURCE.iterdir():
        if not p.is_file():continue
        q=SNAPSHOT/p.name;raw=p.read_bytes();shutil.copy2(p,q)
        text=raw.decode('utf-8-sig')
        if p.suffix=='.json':
            try:doc=json.loads(text)
            except json.JSONDecodeError:continue
            new=visit(doc)
            if new!=doc:write(q,new)
        elif p.suffix=='.tsv':
            lines=[]
            for line in text.splitlines():
                f=line.split('|');s='[NPC] '+f[0]+': '+f[1] if len(f)>=3 and not line.startswith('#') else ''
                if s in by_source:
                    r=by_source[s];assert '[NPC] '+f[0]+': '+f[2] in (r['old_translation'],r['translation']);f[2]=r['translation'].split(': ',1)[1]
                    if len(f)==4:
                        defs=anchors(f[3]);defs=[revised_anchor(a,r) for a in defs]
                        # Preserve object format where older fixture parsers require it.
                        f[3]=json.dumps({a['source']:a['target'] for a in defs},ensure_ascii=False) if isinstance(json.loads(f[3]),dict) else json.dumps(defs,ensure_ascii=False)
                    line='|'.join(f)
                elif p.name=='NPC_STYLES_0.15.104_REVIEWED.tsv' and line and not line.startswith('#'):
                    i,definitions=line.split('|',1);s=frozen['gaps'][int(i)]['source']
                    if s in by_source:line=i+'|'+json.dumps(by_source[s]['style_anchors'],ensure_ascii=False)
                lines.append(line)
            updated='\n'.join(lines)+'\n'
            if updated!=text.replace('\r\n','\n'):q.write_text(updated,encoding='utf-8')
        if q.read_bytes()!=raw:changes.append({'file':p.name,'historical_sha256':sha(p),'revised_expectation_sha256':sha(q)})
    write(SNAPSHOT/'EXPECTATION_SUPERSESSION105.json',{'scope':'Historical golden data unchanged in translation-source. This isolated snapshot intentionally revises only eleven targets/semantic anchors from the authored105 manifest; not a claim of unchanged old expectations.','revisions':revisions,'changed_files':changes})
    print(json.dumps({'wording_revisions':len(revisions),'published_gaps_fixed':5,'primary_and_published_cases':len(cases),'revised_golden_files':len(changes),'snapshot':str(SNAPSHOT)},ensure_ascii=False))

if __name__=='__main__':main()
