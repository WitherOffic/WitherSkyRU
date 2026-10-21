"""Verify exact notifications, immutable106 evidence, and startup-only bytecode edit."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from audit_npc106 import JAVA,sections

BASE='8A56461E1EED72013316C8DEEA1FE268062CE4DAD0380EEDE13DD85BC8D260A2'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_3')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.106-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.107-dev.jar'
    assert sha(before)==BASE;digest=sha(after);manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h
    historical=[p for p in manifest if p.startswith('tests/') and p.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');new=(ROOT/rel).read_text(encoding='utf-8-sig')
    literal='new NpcDialogueTranslator("/data/witherskyru/quest-objectives.json",java.util.Set.of("SYSTEM"),true,true)'
    array='new NpcDialogueTranslator(new String[]{"/data/witherskyru/quest-objectives.json","/data/witherskyru/system-expansion107.json"},java.util.Set.of("SYSTEM"),true,true)'
    assert old.count(literal)==1 and old.replace(literal,array)==new
    for name in ('ChatNotificationTranslator','NamedNpcTemplates'):
        rel='src/main/java/ru/wither/skyru/'+name+'.java';assert sha(ROOT/rel)==manifest[rel]
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class'],changed
        assert added==['META-INF/licenses/SYSTEM107-SOURCES.txt','data/witherskyru/system-expansion107.json'],added
        assert y.read('data/witherskyru/system-expansion107.json')==(DATA/'system-expansion107.json').read_bytes()
        classes=[n for n in x.namelist() if n.endswith('.class')];assert len(classes)==125
        assert sum(x.read(n)==y.read(n) for n in classes)==124
    a.out.mkdir(exist_ok=False);methods=[];name='ru.wither.skyru.NpcDialogueTranslator'
    for version,jar in [('106',before),('107',after)]:
        output=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout
        (a.out/(name+'-'+version+'.txt')).write_text(output,encoding='utf-8');methods.append(sections(output))
    assert methods[0].keys()==methods[1].keys()
    differences=[key for key in methods[0] if methods[0][key]!=methods[1][key]]
    assert differences==['public static ru.wither.skyru.NpcDialogueTranslator questObjectives() throws java.io.IOException;'],differences
    x=rows(a.dispatch/'0.15.106-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.107-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252
    for old,new in zip(x,y):assert old['source']==new['source'] and all(old[k]==new[k] for k in ('flat_result','published_style_result')),old['source']
    evidence=load(SOURCE/'SYSTEM_SOURCES_0.15.107.json');records=evidence['records'];assert len(records)==119 and not evidence['retained_not_new'] and len(evidence['source_pages'])==21
    assert all(r['published_row']['tag'] is None and not any(r['published_row'][k] for k in ('historical_heading','variable_definition','unresolved_markup','alpha_page')) for r in records)
    targets={r['source']:r for r in records};assert len(targets)==119
    runtime=load(DATA/'system-expansion107.json')['records'];assert len(runtime)==119
    for r in runtime:
        original=targets[r['source']];assert all(r[k]==original[k] for k in ('speaker','tag','source','translation','style_anchors','source_url'))
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.106-dev','0.15.107-dev')]
    assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==119
    layout_count=0
    for old,new in zip(snaps[0]['records'],snaps[1]['records']):
        assert old['source']==new['source'];source=old['source'];r=targets[source]
        assert old['flat']==source and new['flat']==r['translation']
        assert [x['legacy'] for x in old['layouts']]==[x['legacy'] for x in new['layouts']]==r['layouts']
        for left,right in zip(old['layouts'],new['layouts']):assert left['output']==source and right['output']==r['translation']
        layout_count+=len(new['layouts'])
    assert layout_count==119
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==180 and not checks['reused_successes']
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output,name
    output=(a.checks/'TestSystem107-output.txt').read_text(encoding='utf-8-sig');assert 'SYSTEM107 632925 assertions' in output
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser107-output.txt').read_text(encoding='utf-8-sig')
    transformed=load(ROOT/'build/system107-test-sources-v2/COUNT_EXPECTATION_UPDATES.json')['files'];assert len(transformed)==29
    for item in transformed:
        assert sha(ROOT/'tests'/item['file'])==item['source_sha256'] and sha(ROOT/'build/system107-test-sources-v2'/item['file'])==item['generated_sha256']
        old=(ROOT/'tests'/item['file']).read_text(encoding='utf-8-sig');new=(ROOT/'build/system107-test-sources-v2'/item['file']).read_text(encoding='utf-8-sig')
        assert re.sub(r'(?<=phraseCount\(\)==)10525\b','10628',old)==new
    supersession=ROOT/'translation-source-regression107-v2/EXPECTATION_SUPERSESSION105.json';previous=PREVIOUS/'expectation-supersession/EXPECTATION_SUPERSESSION105.json'
    assert sha(supersession)==sha(previous)
    for item in load(supersession)['changed_files']:
        assert sha(ROOT/'translation-source-regression107-v2'/item['file'])==sha(PREVIOUS/'expectation-supersession'/item['file'])
    corpus=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1';assert sha(corpus/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244
    known={r['source'] for path in DATA.glob('*.json') for r in load(path).get('records',[]) if r.get('source') and r.get('translation')}
    assert sum(r['source'] in known for r in queue)==11314
    prior=rows(SOURCE/'NPC_WORKLIST_0.15.106.jsonl');assert sum(r['exact_registration'] for r in prior)==11195
    assert all(not r['exact_registration'] for r in prior if r['source'] in targets)
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':124,'method_differences':{'ru.wither.skyru.NpcDialogueTranslator':differences},'all_receipt_and_render_algorithms_unchanged':True,'new_system_literals':119,'pinned_pages':21,'exact_before':11195,'exact_after':11314,'frozen_total':16244,'without_exact_after':4930,'existing11252_flat_and_styled_outputs_unchanged':True,'new_primary_cases':119,'new_published_layouts':119,'fresh_test_groups':180,'parser_tests':15,'new_system_assertions':632925,'historical_test_sources_unchanged':len(historical),'isolated_count_assertion_updates':29,'previous_delivery_files_verified':len(manifest),'installed_pack_modified':False,'game_fps_measured':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
