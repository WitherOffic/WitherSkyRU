"""Verify boss literals, immutable prior package, and startup-only runtime difference."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from audit_npc106 import JAVA,sections
from prepare_boss108 import BASE,CORPUS
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_4')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.107-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.108-dev.jar'
    assert sha(before)==BASE;digest=sha(after);manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h
    historical=[p for p in manifest if p.startswith('tests/') and p.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');new=(ROOT/rel).read_text(encoding='utf-8-sig')
    literal='"/data/witherskyru/dungeon-expansion98.json"}';array='"/data/witherskyru/dungeon-expansion98.json","/data/witherskyru/boss-expansion108.json"}'
    assert old.count(literal)==1 and old.replace(literal,array)==new
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class'],changed
        assert added==['META-INF/licenses/BOSS108-SOURCES.txt','data/witherskyru/boss-expansion108.json'],added
        assert y.read('data/witherskyru/boss-expansion108.json')==(DATA/'boss-expansion108.json').read_bytes()
        classes=[n for n in x.namelist() if n.endswith('.class')];assert len(classes)==125 and sum(x.read(n)==y.read(n) for n in classes)==124
    a.out.mkdir(exist_ok=False);methods=[];name='ru.wither.skyru.NpcDialogueTranslator'
    for version,jar in [('107',before),('108',after)]:
        output=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout
        (a.out/(name+'-'+version+'.txt')).write_text(output,encoding='utf-8');methods.append(sections(output))
    assert methods[0].keys()==methods[1].keys();differences=[key for key in methods[0] if methods[0][key]!=methods[1][key]]
    assert differences==['public static ru.wither.skyru.NpcDialogueTranslator dungeonDialogues() throws java.io.IOException;'],differences
    evidence=load(SOURCE/'BOSS_SOURCES_0.15.108.json');records=evidence['records'];assert len(records)==102 and not evidence['retained_not_new'] and len(evidence['source_pages'])==4
    assert all(r['published_row']['tag']=='BOSS' and not any(r['published_row'][k] for k in ('variable_definition','unresolved_markup','alpha_page','nonstandard_tag_layout')) for r in records)
    assert sum(r['archive_branch'] for r in records)==61
    targets={r['source']:r for r in records};runtime=load(DATA/'boss-expansion108.json')['records'];assert len(targets)==len(runtime)==102
    for r in runtime:
        original=targets[r['source']];assert all(r[k]==original[k] for k in ('speaker','tag','source','translation','style_anchors','source_url'))
    x=rows(a.dispatch/'0.15.107-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.108-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252;intentional=0
    for old,new in zip(x,y):
        s=old['source'];assert s==new['source']
        if s in targets:
            assert old['flat_result']==old['published_style_result']==s and new['flat_result']==new['published_style_result']==targets[s]['translation'];intentional+=1
        else:assert all(old[k]==new[k] for k in ('flat_result','published_style_result')),s
    assert intentional==102
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.107-dev','0.15.108-dev')];assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==102;layouts=0
    for old,new in zip(snaps[0]['records'],snaps[1]['records']):
        assert old['source']==new['source'];s=old['source'];r=targets[s];assert old['flat']==s and new['flat']==r['translation']
        assert [x['legacy'] for x in old['layouts']]==[x['legacy'] for x in new['layouts']]==r['layouts']
        for left,right in zip(old['layouts'],new['layouts']):assert left['output']==s and right['output']==r['translation']
        layouts+=len(new['layouts'])
    assert layouts==102
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==181 and not checks['reused_successes']
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output,name
    output=(a.checks/'TestBoss108-output.txt').read_text(encoding='utf-8-sig');assert 'BOSS108 ' in output and '204 primary/published fixtures' in output;assertions=int(re.search(r'BOSS108 (\d+) assertions',output)[1])
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser108-output.txt').read_text(encoding='utf-8-sig')
    transformed=load(ROOT/'build/boss108-test-sources/COUNT_EXPECTATION_UPDATES.json')['files'];assert len(transformed)==31
    for item in transformed:
        assert sha(ROOT/'tests'/item['file'])==item['source_sha256'] and sha(ROOT/'build/boss108-test-sources'/item['file'])==item['generated_sha256']
        old=(ROOT/'tests'/item['file']).read_text(encoding='utf-8-sig');new=(ROOT/'build/boss108-test-sources'/item['file']).read_text(encoding='utf-8-sig');expected=re.sub(r'(?<=phraseCount\(\)==)10525\b','10628',old)
        if item['file']=='TestNpc98.java':expected=expected.replace('phraseCount()==358','phraseCount()==460')
        if item['file']=='TestDungeonChatResources.java':expected=expected.replace('entries.size()+336','entries.size()+438')
        assert expected==new
    superpath=ROOT/'translation-source-regression108/EXPECTATION_SUPERSESSION105.json';assert sha(superpath)==sha(PREVIOUS/'expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for item in load(superpath)['changed_files']:assert sha(ROOT/'translation-source-regression108'/item['file'])==sha(PREVIOUS/'expectation-supersession'/item['file'])
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(CORPUS/'translation_queue.jsonl');assert len(queue)==16244;known={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')};assert sum(r['source'] in known for r in queue)==11416
    prior=rows(SOURCE/'NPC_WORKLIST_0.15.107.jsonl');assert sum(r['exact_registration'] for r in prior)==11314 and all(not r['exact_registration'] for r in prior if r['source'] in targets)
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':124,'method_differences':{'ru.wither.skyru.NpcDialogueTranslator':differences},'all_receipt_and_render_algorithms_unchanged':True,'new_boss_literals':102,'non_dante_boss_literals':41,'archival_dante_literals':61,'pinned_pages':4,'exact_before':11314,'exact_after':11416,'frozen_total':16244,'without_exact_after':4828,'screening_unchanged_rows':11150,'screening_intentional_new_targets':102,'new_primary_cases':102,'new_published_layouts':102,'fresh_test_groups':181,'parser_tests':15,'new_boss_assertions':assertions,'historical_test_sources_unchanged':len(historical),'isolated_count_assertion_updates':31,'previous_delivery_files_verified':len(manifest),'installed_pack_modified':False,'game_fps_measured':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
