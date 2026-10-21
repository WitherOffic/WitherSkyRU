"""Independent112 artifact/coverage gate. No rewriting historical positive oracles."""
import argparse,json,re,subprocess,zipfile
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_expansion112 import BASE,load,CORPUS,candidates
from derive_expansion112 import adjust_coverage
from audit_npc106 import JAVA,sections
PREVIOUS=__import__('pathlib').Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_8')

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=__import__('pathlib').Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.111-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.112-dev.jar';assert sha(before)==BASE;digest=sha(after)
    old_manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in old_manifest.items():assert sha(PREVIOUS/rel)==h
    historical=[rel for rel in old_manifest if rel.startswith('tests/') and rel.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==old_manifest[rel],rel
    for rel in old_manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==old_manifest[rel],rel
    rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');expected=old
    for stem in ('npc','system'):
        marker='"/data/witherskyru/'+stem+'-expansion111.json"}';assert expected.count(marker)==1;expected=expected.replace(marker,'"/data/witherskyru/'+stem+'-expansion111.json","/data/witherskyru/'+stem+'-expansion112.json"}')
    assert (ROOT/rel).read_text(encoding='utf-8-sig')==expected
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class']
        assert added==['META-INF/licenses/EXPANSION112-SOURCES.txt','data/witherskyru/npc-expansion112.json','data/witherskyru/system-expansion112.json']
        for name in ('npc-expansion112.json','system-expansion112.json'):assert y.read('data/witherskyru/'+name)==(DATA/name).read_bytes()
        classes=[n for n in x.namelist() if n.endswith('.class')];assert len(classes)==125 and sum(x.read(n)==y.read(n) for n in classes)==124
    evidence=load(SOURCE/'EXPANSION_SOURCES_0.15.112.json');records=evidence['records'];assert len(records)==482 and not evidence['retained_not_new']
    assert sum(r['tag']=='NPC' for r in records)==2 and sum(r['tag']=='SYSTEM' for r in records)==480 and sum(r['cosmetic'] for r in records)==295
    assert sum(r['archival'] for r in records)==5 and all(not any(r['published_row'][k] for k in ('variable_definition','unresolved_markup','alpha_page')) for r in records)
    authored,retained=candidates();assert not retained and {r['source']:r['translation'] for r in authored}=={r['source']:r['translation'] for r in records}
    targets={r['source']:r for r in records};assert len(targets)==482
    runtime=load(DATA/'npc-expansion112.json')['records']+load(DATA/'system-expansion112.json')['records'];assert len(runtime)==482
    for r in runtime:assert all(r[k]==targets[r['source']][k] for k in ('speaker','tag','source','translation','style_anchors','source_url'))
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.111-dev','0.15.112-dev')];assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==482;layouts=0
    for x,y in zip(snaps[0]['records'],snaps[1]['records']):
        s=x['source'];assert s==y['source'];r=targets[s];assert x['flat']==s and y['flat']==r['translation']
        assert [v['legacy'] for v in x['layouts']]==[v['legacy'] for v in y['layouts']]==r['layouts']
        assert all(v['output']==s for v in x['layouts']) and all(v['output']==r['translation'] for v in y['layouts']);layouts+=len(y['layouts'])
    assert layouts==507
    x=rows(a.dispatch/'0.15.111-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.112-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252 and x==y
    proof=load(a.dispatch/'RUNTIME_SCOPE.json')['records'];assert len(proof)==2
    for r in proof:
        assert r['runtime_jar_sha256']==(BASE if r['version']=='0.15.111-dev' else digest)
        assert sha(a.dispatch/r['version']/'DISPATCH_ROWS.jsonl')==r['rows_sha256'] and sha(a.dispatch/r['version']/'DISPATCH_SUMMARY.json')==r['summary_sha256']
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==185 and not checks['reused_successes']
    assert __import__('pathlib').Path(checks['source_root']).resolve()==(ROOT/'translation-source-regression112-v2').resolve()
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output
    output=(a.checks/'TestExpansion112-output.txt').read_text(encoding='utf-8-sig');assert '989 primary/published fixtures' in output;assertions=int(re.search(r'EXPANSION112 (\d+) assertions',output)[1]);assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser112-output.txt').read_text(encoding='utf-8-sig')
    copies=ROOT/'build/expansion112-test-sources';updates=load(copies/'EXPECTATION_UPDATES.json')['files'];assert len(updates)==36
    for r in updates:
        p=ROOT/'tests'/r['file'];prior=ROOT/r['baseline_derived_path'];q=copies/r['file'];assert sha(p)==r['original_sha256'] and sha(prior)==r['baseline_derived_sha256'] and sha(q)==r['generated_sha256']
        expected,n=re.subn(r'(?<=phraseCount\(\)==)(10761|1861)\b',lambda m:'10763' if m[0]=='10761' else '2341',prior.read_text(encoding='utf-8-sig'));assert n==r['count_replacements_from111']
        if r['file']=='TestNpc89.java':expected=adjust_coverage(expected)
        assert expected==q.read_text(encoding='utf-8-sig')
    additions=load(SOURCE/'EXPANSION_COVERAGE_ADDITIONS_0.15.112.json')['cases'];assert len(additions)==2 and {r['id'] for r in additions}=={'13387','13391'}
    for r in additions:assert r==targets[r['source']] and '[Lvl 1] Tarantula' in r['source'] and not r['archival']
    for name in ('EXPANSION_COVERAGE_ADDITIONS_0.15.111.json','EXPANSION_COVERAGE_SUPERSESSION_0.15.111.json'):assert sha(SOURCE/name)==old_manifest['translation-source/'+name]
    supersession=ROOT/'translation-source-regression112-v2/EXPECTATION_SUPERSESSION105.json';assert sha(supersession)==sha(PREVIOUS/'expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession)['changed_files']:assert sha(ROOT/'translation-source-regression112-v2'/r['file'])==sha(PREVIOUS/'expectation-supersession'/r['file'])
    editorial=load(SOURCE/'EXPANSION_EDITORIAL_SUPERSESSION_0.15.112.json');assert {r['id'] for r in editorial['changes']}=={'2414','2423','3366'}
    for r in editorial['changes']:assert r['after']==targets[r['source']]['translation'] and r['after_anchors']==targets[r['source']]['style_anchors']
    assert digest!=editorial['preliminary_jar_sha256'] and str(a.checks).replace('\\','/')!=editorial['preliminary_checks']
    assert sha(ROOT/editorial['retained_preliminary_sources']/'WitherSkyRU-26.1.2-fabric-0.15.112-dev.jar')==editorial['preliminary_jar_sha256']
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925';queue=rows(CORPUS/'translation_queue.jsonl');assert len(queue)==16244
    known={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')};assert sum(r['source'] in known for r in queue)==13744
    prior=rows(SOURCE/'NPC_WORKLIST_0.15.111.jsonl');assert sum(r['exact_registration'] for r in prior)==13262 and all(not r['exact_registration'] for r in prior if r['source'] in targets)
    assert checks['runtime_jar_sha256']!=editorial['preliminary_jar_sha256']
    a.out.mkdir(exist_ok=False);methods=[];name='ru.wither.skyru.NpcDialogueTranslator'
    for version,jar in [('111',before),('112',after)]:
        out=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout;(a.out/(name+'-'+version+'.txt')).write_text(out,encoding='utf-8');methods.append(sections(out))
    assert methods[0].keys()==methods[1].keys();diff=[k for k in methods[0] if methods[0][k]!=methods[1][k]];assert diff==['public static ru.wither.skyru.NpcDialogueTranslator storyDialogues() throws java.io.IOException;','public static ru.wither.skyru.NpcDialogueTranslator questObjectives() throws java.io.IOException;']
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':124,'method_differences':{name:diff},'all_receipt_and_render_algorithms_unchanged':True,'new_records':482,'new_npc_literals':2,'new_system_literals':480,'new_cosmetic_literals':295,'new_archival_literals':5,'pinned_pages':len(evidence['source_pages']),'exact_before':13262,'exact_after':13744,'frozen_total':16244,'without_exact_after':2500,'unchanged_dispatch_rows':11252,'new_primary_cases':482,'new_published_layouts':507,'fresh_test_groups':185,'parser_tests':15,'new_assertions':assertions,'historical_test_sources_unchanged':len(historical),'isolated_expectation_files':36,'explicit_fixed_pet_coverage_upgrades':2,'previous_delivery_files_verified':len(old_manifest),'installed_pack_modified':False,'game_fps_measured':False};write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
