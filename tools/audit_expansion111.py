"""Independent final-JAR gate:immutable110 evidence,exact additions,and startup-only edits."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from audit_npc106 import JAVA,sections
from coverage_expansion111 import adjust,cases
BASE='120A6C334CE41095E37895820439B4028F20006CEFF6AB7B6F535E350BD29B74'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_7')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.110-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.111-dev.jar';assert sha(before)==BASE;digest=sha(after)
    manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h
    historical=[rel for rel in manifest if rel.startswith('tests/') and rel.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');new=(ROOT/rel).read_text(encoding='utf-8-sig');expected=old
    for stem in ('npc-expansion106','game-expansion110'):
        added='npc-expansion111' if stem.startswith('npc') else 'system-expansion111';literal='"/data/witherskyru/'+stem+'.json"}';assert expected.count(literal)==1;expected=expected.replace(literal,'"/data/witherskyru/'+stem+'.json","/data/witherskyru/'+added+'.json"}')
    assert new==expected
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class']
        assert added==['META-INF/licenses/EXPANSION111-SOURCES.txt','data/witherskyru/npc-expansion111.json','data/witherskyru/system-expansion111.json']
        for name in ('npc-expansion111.json','system-expansion111.json'):assert y.read('data/witherskyru/'+name)==(DATA/name).read_bytes()
        classes=[n for n in x.namelist() if n.endswith('.class')];assert len(classes)==125 and sum(x.read(n)==y.read(n) for n in classes)==124
    evidence=load(SOURCE/'EXPANSION_SOURCES_0.15.111.json');records=evidence['records'];assert len(records)==502 and len(evidence['retained_not_new'])==3
    assert sum(r['tag']=='NPC' for r in records)==133 and sum(r['tag']=='SYSTEM' for r in records)==369 and sum(r['archival'] for r in records)==130
    assert all(not any(r['published_row'][k] for k in ('variable_definition','unresolved_markup','alpha_page')) for r in records)
    targets={r['source']:r for r in records};assert len(targets)==502
    runtime=load(DATA/'npc-expansion111.json')['records']+load(DATA/'system-expansion111.json')['records'];assert len(runtime)==502
    for r in runtime:assert all(r[k]==targets[r['source']][k] for k in ('speaker','tag','source','translation','style_anchors','source_url'))
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.110-dev','0.15.111-dev')];assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==502;layouts=0
    for x,y in zip(snaps[0]['records'],snaps[1]['records']):
        assert x['source']==y['source'];s=x['source'];r=targets[s];assert x['flat']==s and y['flat']==r['translation']
        assert [v['legacy'] for v in x['layouts']]==[v['legacy'] for v in y['layouts']]==r['layouts']
        assert all(v['output']==s for v in x['layouts']) and all(v['output']==r['translation'] for v in y['layouts']);layouts+=len(y['layouts'])
    assert layouts==509
    x=rows(a.dispatch/'0.15.110-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.111-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252;approved=[]
    for left,right in zip(x,y):
        s=left['source'];assert s==right['source']
        if any(left[k]!=right[k] for k in ('flat_result','published_style_result')):
            assert s in targets and left['flat_result']==left['published_style_result']==s and right['flat_result']==right['published_style_result']==targets[s]['translation'];approved.append(s)
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==184 and not checks['reused_successes']
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output
    output=(a.checks/'TestExpansion111-output.txt').read_text(encoding='utf-8-sig');assert '1011 primary/published fixtures' in output;assertions=int(re.search(r'EXPANSION111 (\d+) assertions',output)[1]);assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser111-output.txt').read_text(encoding='utf-8-sig')
    copies=ROOT/'build/expansion111-test-sources-v5';updates=load(copies/'COUNT_EXPECTATION_UPDATES.json')['files'];assert len(updates)==35
    for r in updates:
        p=ROOT/'tests'/r['file'];q=copies/r['file'];assert sha(p)==r['source_sha256'] and sha(q)==r['generated_sha256'];old=p.read_text(encoding='utf-8-sig');expected=re.sub(r'(?<=phraseCount\(\)==)(10525|10628)\b','10761',old)
        edits={'TestNpc98.java':('phraseCount()==358','phraseCount()==460'),'TestDungeonChatResources.java':('entries.size()+336','entries.size()+438'),'TestSystem107.java':('phraseCount()==148','phraseCount()==1861'),'TestSkins109.java':('phraseCount()==952','phraseCount()==1861'),'TestGame110.java':('phraseCount()==1492','phraseCount()==1861')}
        if r['file'] in edits:expected=expected.replace(*edits[r['file']])
        expected=adjust(r['file'],expected)
        assert expected==q.read_text(encoding='utf-8-sig')
    coverage=load(SOURCE/'EXPANSION_COVERAGE_SUPERSESSION_0.15.111.json');assert coverage['source']=='[NPC] [Eye]: Quit it!' and coverage['translation']==targets[coverage['source']]['translation']
    assert not targets[coverage['source']]['published_row']['variable_definition'] and targets[coverage['source']]['speaker']=='[Eye]'
    additions=load(SOURCE/'EXPANSION_COVERAGE_ADDITIONS_0.15.111.json')['cases'];assert additions==cases() and len(additions)==31
    for r in additions:assert r['translation']==targets[r['source']]['translation']
    supersession=ROOT/'translation-source-regression111-v5/EXPECTATION_SUPERSESSION105.json';assert sha(supersession)==sha(PREVIOUS/'expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession)['changed_files']:assert sha(ROOT/'translation-source-regression111-v5'/r['file'])==sha(PREVIOUS/'expectation-supersession'/r['file'])
    corpus=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1';assert sha(corpus/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925';queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244
    known={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')};assert sum(r['source'] in known for r in queue)==13262
    prior=rows(SOURCE/'NPC_WORKLIST_0.15.110.jsonl');assert sum(r['exact_registration'] for r in prior)==12760 and all(not r['exact_registration'] for r in prior if r['source'] in targets)
    a.out.mkdir(exist_ok=False);methods=[];name='ru.wither.skyru.NpcDialogueTranslator'
    for version,jar in [('110',before),('111',after)]:
        out=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout;(a.out/(name+'-'+version+'.txt')).write_text(out,encoding='utf-8');methods.append(sections(out))
    assert methods[0].keys()==methods[1].keys();diff=[k for k in methods[0] if methods[0][k]!=methods[1][k]];assert diff==['public static ru.wither.skyru.NpcDialogueTranslator storyDialogues() throws java.io.IOException;','public static ru.wither.skyru.NpcDialogueTranslator questObjectives() throws java.io.IOException;']
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':124,'method_differences':{name:diff},'all_receipt_and_render_algorithms_unchanged':True,'new_records':502,'new_npc_literals':133,'new_system_literals':369,'new_archival_literals':130,'current_npc_literals':3,'pinned_pages':len(evidence['source_pages']),'exact_before':12760,'exact_after':13262,'frozen_total':16244,'without_exact_after':2982,'existing_dispatch_rows':11252,'approved_new_literal_dispatch_changes':approved,'unchanged_dispatch_rows':11252-len(approved),'new_primary_cases':502,'new_published_layouts':509,'fresh_test_groups':184,'parser_tests':15,'new_assertions':assertions,'historical_test_sources_unchanged':len(historical),'isolated_count_assertion_updates':35,'previous_delivery_files_verified':len(manifest),'installed_pack_modified':False,'game_fps_measured':False};write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
