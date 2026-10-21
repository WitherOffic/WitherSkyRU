"""Compare the final artifact with103: only a startup style-table registration may change."""
import argparse,json,re,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,sha,rows,write,anchors,validate
from prepare_npc104 import CORPUS,PENDING

BASE='01E04AD01F2CF57B09CD5AF4A28093BC6130D48D4D79B2D5748C45D59ECFF028'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_6')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    parser=argparse.ArgumentParser()
    for name in ('checks','dispatch','out'):parser.add_argument('--'+name,type=Path,required=True)
    a=parser.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.103-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.104-dev.jar'
    assert sha(before)==BASE;fingerprint=sha(after);manifest=load(PREVIOUS/'HASHES.json')
    for rel,digest in manifest.items():assert sha(PREVIOUS/rel)==digest,rel
    with zipfile.ZipFile(before) as old,zipfile.ZipFile(after) as new:
        assert new.testzip() is None and not set(old.namelist())-set(new.namelist())
        changed=sorted(n for n in set(old.namelist())&set(new.namelist()) if old.read(n)!=new.read(n));added=sorted(set(new.namelist())-set(old.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator$StyleCorrections.class'],changed
        assert added==['META-INF/licenses/NPC104-SOURCES.txt','data/witherskyru/npc-styles104.json'],added
        unchanged=sum(n.endswith('.class') and n not in changed for n in old.namelist())
        assert new.read('data/witherskyru/npc-styles104.json')==(DATA/'npc-styles104.json').read_bytes()
    rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java'
    x=(PREVIOUS/rel).read_text(encoding='utf-8-sig');y=(ROOT/rel).read_text(encoding='utf-8-sig')
    source='"/data/witherskyru/npc-styles83.json"}';target='"/data/witherskyru/npc-styles83.json","/data/witherskyru/npc-styles104.json"}'
    assert x.count(source)==1 and x.replace(source,target)==y,'Unexpected source mutation'
    for rel in ('src/main/java/ru/wither/skyru/ChatNotificationTranslator.java','src/main/java/ru/wither/skyru/NamedNpcTemplates.java'):assert sha(ROOT/rel)==manifest[rel]
    # Previous test expectations are not rewritten to make the new version pass.
    previous_tests=[rel for rel in manifest if rel.startswith('tests/') and rel.endswith('.java')]
    for rel in previous_tests:assert sha(ROOT/rel)==manifest[rel],rel
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    known={r['source'] for path in DATA.glob('*.json') for r in load(path).get('records',[]) if r.get('source') and r.get('translation')}
    assert sum(r['source'] in known for r in rows(CORPUS/'translation_queue.jsonl'))==11092
    evidence=load(SOURCE/'NPC_STYLE_SOURCES_0.15.104.json');pack=load(DATA/'npc-styles104.json')['records'];assert len(pack)==len(evidence['records'])==67 and len(evidence['pending'])==5 and len(evidence['source_pages'])==14
    authored={int(line.split('|',1)[0]):anchors(line.split('|',1)[1]) for line in (SOURCE/'NPC_STYLES_0.15.104_REVIEWED.tsv').read_text(encoding='utf-8-sig').splitlines() if line and not line.startswith('#')}
    for r,p in zip(evidence['records'],pack):assert p['style_anchors']==authored[r['index']]==r['style_anchors'];validate(p['source'],p['translation'],p['style_anchors'])
    old={r['source']:r for r in rows(a.dispatch/'0.15.103-dev/DISPATCH_ROWS.jsonl')};new={r['source']:r for r in rows(a.dispatch/'0.15.104-dev/DISPATCH_ROWS.jsonl')};assert old.keys()==new.keys() and len(new)==11252
    fixed={r['source']:r['translation'] for r in pack}
    for s,r in new.items():
        assert r['flat_result']==old[s]['flat_result'],s
        if s in fixed:assert old[s]['published_style_result']==s and r['published_style_result']==fixed[s]==r['flat_result'],s
        else:assert old[s]['published_style_result']==r['published_style_result'],s
    old_gaps=[r for r in old.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];new_gaps=[r for r in new.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']]
    assert len(old_gaps)==72 and len(new_gaps)==5 and {r['source'] for r in new_gaps}=={r['snapshot']['source'] for r in evidence['pending']}
    for v,h in [('0.15.103-dev',BASE),('0.15.104-dev',fingerprint)]:assert load(a.dispatch/v/'DISPATCH_SUMMARY.json')['runtime_jar_sha256']==h
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==fingerprint and len(checks['tests_passed'])==len(set(checks['tests_passed']))==176 and 'TestNpcStyles104' in checks['tests_passed'] and not checks['reused_successes']
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output,name
    output=(a.checks/'TestNpcStyles104-output.txt').read_text(encoding='utf-8-sig');count=int(re.search(r'PASS: NPCStyles104 (\d+) assertions',output)[1]);assert count>50000
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser104-output.txt').read_text(encoding='utf-8-sig')
    a.out.mkdir(exist_ok=False)
    result={'runtime_jar_sha256':fingerprint,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'unchanged_runtime_classes':unchanged,'all_message_and_render_method_class_payloads_unchanged':True,'only_runtime_class_change':'StyleCorrections resource loader, startup only','primary_resources_and_targets_unchanged':True,'previous_test_sources_unchanged':len(previous_tests),'previous_delivery_files_verified':len(manifest),'exact_keys':11092,'frozen_total':16244,'style_gap_before':72,'style_repairs':67,'style_gap_remaining':5,'source_pages':14,'fresh_test_groups':176,'parser_tests':15,'new_style_assertions':count,'all_11252_flat_outputs_unchanged':True,'other_11185_styled_outputs_unchanged':True,'installed_pack_modified':False,'game_fps_or_frame_times_measured':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
