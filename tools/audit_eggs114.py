"""Independent114 artifact/coverage gate;finite combinations are not extra NPCs."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_eggs114 import BASE,CORPUS,load,records
from audit_npc106 import JAVA,sections
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_10')

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=Path,required=True)
    ap.add_argument('--preflight',action='store_true')
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.113-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.114-dev.jar';assert sha(before)==BASE;digest=sha(after)
    manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h,rel
    historical=[r for r in manifest if r.startswith('tests/') and r.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    path='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/path).read_text(encoding='utf-8-sig');assert (ROOT/path).read_text(encoding='utf-8-sig')==old.replace('"/data/witherskyru/system-expansion112.json"}','"/data/witherskyru/system-expansion112.json","/data/witherskyru/eggs-expansion114.json"}')
    for path in ('src/main/java/ru/wither/skyru/NamedNpcTemplates.java','src/main/java/ru/wither/skyru/ChatNotificationTranslator.java'):assert sha(ROOT/path)==manifest[path]
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class'],changed
        assert added==['META-INF/licenses/EGGS114-SOURCES.txt','data/witherskyru/eggs-expansion114.json'],added
        classes=[n for n in x.namelist() if n.endswith('.class')];identical=sum(x.read(n)==y.read(n) for n in classes);assert len(classes)==125 and identical==124
        assert y.read('data/witherskyru/eggs-expansion114.json')==(DATA/'eggs-expansion114.json').read_bytes()
    evidence=load(SOURCE/'EGG_SOURCES_0.15.114.json');selected=evidence['records'];recomputed,blocks,headers=records();assert selected==json.loads(json.dumps(recomputed,ensure_ascii=False))
    assert len(selected)==1568 and len({r['source'] for r in selected})==1568 and sum(r['kind']=='location' for r in selected)==224
    assert len(evidence['source_pages'])==2 and all(not r['published_row'][k] for r in selected for k in ('historical_heading','alpha_page','variable_definition','unresolved_markup'))
    assert load(DATA/'eggs-expansion114.json')['records']==[{k:r[k] for k in ('source','translation','style_anchors','tag','speaker','source_url')} for r in selected]
    assert {r['egg'] for r in selected if r['kind']=='envelope'}==headers['egg_names'].keys()
    glossary={}
    for line in (DATA/'corrections.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#') and '\t' in line:k,v=line.split('\t',1);glossary[k]=v
    assert all(glossary[k]==v for k,v in headers['egg_names'].items())
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.113-dev','0.15.114-dev')];assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==1568;by_source={r['source']:r for r in selected};layouts=0
    for x,y in zip(snaps[0]['records'],snaps[1]['records']):
        assert x['source']==y['source'];r=by_source[x['source']];assert x['flat']==x['source'] and y['flat']==r['translation']
        assert [v['legacy'] for v in x['layouts']]==[v['legacy'] for v in y['layouts']]==r['layouts']
        assert all(v['output']==x['source'] for v in x['layouts']) and all(v['output']==r['translation'] for v in y['layouts']);layouts+=len(y['layouts'])
    assert layouts==2912
    x=rows(a.dispatch/'0.15.113-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.114-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252 and x==y
    proof=load(a.dispatch/'RUNTIME_SCOPE.json')['records'];assert len(proof)==2
    for r in proof:
        assert r['runtime_jar_sha256']==(BASE if r['version']=='0.15.113-dev' else digest)
        assert sha(a.dispatch/r['version']/'DISPATCH_ROWS.jsonl')==r['rows_sha256'] and sha(a.dispatch/r['version']/'DISPATCH_SUMMARY.json')==r['summary_sha256']
    if a.preflight:
        print('PRELIMINARY artifact/author-target/snapshot/dispatch checks passed;NOT a completed full audit or test/performance result');return
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==187 and not checks['reused_successes']
    assert Path(checks['source_root']).resolve()==(ROOT/'translation-source-regression114').resolve()
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output
    output=(a.checks/'TestEggs114-output.txt').read_text(encoding='utf-8-sig');assert '224 independent destination targets+1344 independently recomposed' in output and '4480 primary/composition-style fixtures' in output
    assertions=int(re.search(r'EGGS114 (\d+) assertions',output)[1]);assert assertions>1000000
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser114-output.txt').read_text(encoding='utf-8-sig')
    updates=load(ROOT/'build/eggs114-test-sources/EXPECTATION_UPDATES.json')['files'];assert len(updates)==37
    for r in updates:
        p=ROOT/'tests'/r['file'];prior=ROOT/r['baseline_derived_path'];q=ROOT/'build/eggs114-test-sources'/r['file'];assert sha(p)==r['original_sha256'] and sha(prior)==r['baseline_derived_sha256'] and sha(q)==r['generated_sha256']
        expected,n=re.subn(r'(?<=phraseCount\(\)==)2341\b','3909',prior.read_text(encoding='utf-8-sig'));assert n==r['new_system_count_replacements'] and expected==q.read_text(encoding='utf-8-sig')
    supersession=ROOT/'translation-source-regression114/EXPECTATION_SUPERSESSION105.json';assert sha(supersession)==sha(PREVIOUS/'expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession)['changed_files']:assert sha(ROOT/'translation-source-regression114'/r['file'])==sha(PREVIOUS/'expectation-supersession'/r['file'])
    queue=rows(CORPUS/'translation_queue.jsonl');assert len(queue)==16244 and sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    known={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')};assert sum(r['source'] in known for r in queue)==13969
    prior=rows(SOURCE/'NPC_WORKLIST_0.15.113.jsonl');assert sum(r['exact_registration'] for r in prior)==13745
    assert sum(r['source'] in by_source for r in prior)==224 and all(not r['exact_registration'] for r in prior if r['source'] in by_source)
    draft=ROOT/'build/eggs114-editorial-draft';assert sha(draft/'WitherSkyRU-26.1.2-fabric-0.15.114-dev.jar')=='0B187DFA83A57161DFA1D974C86816276F58CC2B9E278E7005404C5350D61218' and digest!='0B187DFA83A57161DFA1D974C86816276F58CC2B9E278E7005404C5350D61218'
    draft_rows=load(draft/'EGG_SELECTED_0.15.114.json')['records'];diff=[r['location_id'] for r,s in zip(selected,draft_rows) if r['translation']!=s['translation']];assert Counter(diff)==Counter({'2842':7,'3005':7})
    a.out.mkdir(exist_ok=False);parts=[];name='ru.wither.skyru.NpcDialogueTranslator'
    for version,jar in [('113',before),('114',after)]:
        output=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout
        (a.out/(name+'-'+version+'.txt')).write_text(output,encoding='utf-8');parts.append(sections(output))
    diff=[k for k in sorted(parts[0].keys()|parts[1].keys()) if parts[0].get(k)!=parts[1].get(k)];assert diff==['public static ru.wither.skyru.NpcDialogueTranslator questObjectives() throws java.io.IOException;']
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':identical,'all_receipt_and_render_algorithms_unchanged':True,'method_differences':{name:diff},'destination_fragments':224,'finite_complete_compositions':1344,'total_new_records':1568,'six_egg_types':6,'source_pages':2,'exact_before':13745,'exact_after':13969,'frozen_total':16244,'without_exact_after':2275,'unchanged_dispatch_rows':11252,'primary_cases':1568,'composition_layouts':2912,'fixtures':4480,'fresh_test_groups':187,'parser_tests':15,'new_assertions':assertions,'historical_test_sources_unchanged':len(historical),'isolated_expectation_files':37,'new_negative_case_upgrades':0,'previous_delivery_files_verified':len(manifest),'literal_double_punctuation_compositions_not_live_certified':18,'installed_pack_modified':False,'game_fps_measured':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

from collections import Counter
if __name__=='__main__':main()
