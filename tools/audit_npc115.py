"""Artifact and evidence gate. Separate recovered literals,templates,and frozen exact keys."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_npc115 import BASE,CORPUS,load
from audit_npc106 import JAVA,sections,without_line_tables
from derive_npc115_checks import isolated_positive_upgrade

PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_11')
OLDER=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_10')

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','snapshots','dispatch','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.114-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.115-dev.jar'
    assert sha(before)==BASE;digest=sha(after);manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h,rel
    historical=[rel for rel in manifest if rel.startswith('tests/') and rel.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    p='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/p).read_text(encoding='utf-8-sig')
    assert (ROOT/p).read_text(encoding='utf-8-sig')==old.replace('"/data/witherskyru/npc-literals113.json"}','"/data/witherskyru/npc-literals113.json","/data/witherskyru/npc-recovery115.json"}')
    p='src/main/java/ru/wither/skyru/NamedNpcTemplates.java';old=(OLDER/p).read_text(encoding='utf-8-sig')
    assert (ROOT/p).read_text(encoding='utf-8-sig')==old.replace('"/data/witherskyru/npc-structured113.json"}','"/data/witherskyru/npc-structured113.json","/data/witherskyru/npc-structured115.json"}')
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        expected={'fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator.class','ru/wither/skyru/NamedNpcTemplates.class'}
        debug_only=[n for n in changed if n not in expected]
        for n in debug_only:assert n.endswith('.class') and without_line_tables(x.read(n))==without_line_tables(y.read(n)),n
        assert expected.issubset(changed)
        assert added==['META-INF/licenses/NPC115-SOURCES.txt','data/witherskyru/npc-recovery115.json','data/witherskyru/npc-structured115.json'],added
        classes=[n for n in x.namelist() if n.endswith('.class')];identical=sum(x.read(n)==y.read(n) for n in classes)
        for n in added:assert y.read(n)==(ROOT/'src/main/resources'/n).read_bytes()
    a.out.mkdir(exist_ok=False);differences={}
    for name,expected_method in [('ru.wither.skyru.NpcDialogueTranslator','public static ru.wither.skyru.NpcDialogueTranslator storyDialogues() throws java.io.IOException;'),('ru.wither.skyru.NamedNpcTemplates','ru.wither.skyru.NamedNpcTemplates();')]:
        parts=[]
        for version,jar in [('114',before),('115',after)]:
            text=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout
            (a.out/(name+'-'+version+'.txt')).write_text(text,encoding='utf-8');parts.append(sections(text))
        diff=[k for k in sorted(parts[0].keys()|parts[1].keys()) if parts[0].get(k)!=parts[1].get(k)];assert diff==[expected_method],(name,diff);differences[name]=diff
    candidate=load(SOURCE/'NPC_RECOVERY_PROBE_0.15.115.json');evidence=load(SOURCE/'NPC_RECOVERY_SOURCES_0.15.115.json');author=load(SOURCE/'NPC_RECOVERY_0.15.115_REVIEWED.json');selected={r['id']:r for r in evidence['records']}
    assert len(selected)==len(author)==48 and sum(r['kind']=='literal' for r in selected.values())==35
    for uid,r in selected.items():
        body,defs=author[uid];assert r['translation']==f"[NPC] {r['speaker']}: "+body and r['style_anchors']==[{'source':x,'target':y} for x,y in defs.items()]
    literal=[{k:r[k] for k in ('source','translation','speaker','source_url','style_anchors')}|{'tag':'NPC'} for r in selected.values() if r['kind']=='literal']
    assert literal==load(DATA/'npc-recovery115.json')['records']
    assert [r['runtime_rule'] for r in selected.values() if r['kind']=='integer']==load(DATA/'npc-structured115.json')['templates']
    exclusions={r['id']:r['runtime_rule']['excluded_names'] for r in selected.values() if r.get('runtime_rule',{}).get('excluded_names')};assert exclusions=={'13010-number':['5','6','7']},exclusions
    assert sum(len(load(DATA/f)['templates']) for f in ('npc-names103.json','npc-structured113.json','npc-structured115.json'))==243
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.114-dev','0.15.115-dev')];assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==len(candidate['records'])==49
    for r,x,y in zip(candidate['records'],snaps[0]['records'],snaps[1]['records']):
        assert r['source']==x['source']==y['source']
        if r['id'] in selected:
            target=selected[r['id']]['translation'].replace('{value}',r.get('probe_value',''))
            assert x['flat']==x['source'] and all(v['output']==x['source'] for v in x['layouts'])
            assert y['flat']==target and all(v['output']==target for v in y['layouts']),(r['id'],y,target)
        else:assert x==y and r['id']=='6171'
        assert [v['legacy'] for v in x['layouts']]==[v['legacy'] for v in y['layouts']]==r['layouts']
    x=rows(a.dispatch/'0.15.114-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.115-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252
    changes=[]
    for old,new in zip(x,y):
        if old!=new:changes.append({'before':old,'after':new})
    # One former English literal is intentionally covered now;all old translations remain exact.
    assert len(changes)==1,('Unexpected previous output changes;review explicitly',changes[:4])
    previous,current=changes[0]['before'],changes[0]['after'];assert previous['source']==current['source']=='[NPC] Mayor Nancy: Hi|'
    assert previous['flat_result']==previous['published_style_result']==previous['source']
    assert current['flat_result']==current['published_style_result']=='[NPC] Mayor Nancy: Привет|'
    for k in ('speaker','source_url','occurrence_id'):assert previous[k]==current[k]
    proof=load(a.dispatch/'RUNTIME_SCOPE.json')['records'];assert len(proof)==2
    for r in proof:
        assert r['runtime_jar_sha256']==(BASE if r['version']=='0.15.114-dev' else digest)
        for filename,key in [('DISPATCH_ROWS.jsonl','rows_sha256'),('DISPATCH_SUMMARY.json','summary_sha256')]:assert sha(a.dispatch/r['version']/filename)==r[key]
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==189 and not checks['reused_successes']
    for name in checks['tests_passed']:
        text=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in text or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in text
    new_assertions={}
    for name,marker in [('TestNpcLiterals115','NPC_LITERALS115'),('TestNpcNumbers115','NPC_NUMBERS115')]:new_assertions[name]=int(re.search(marker+r' (\d+) assertions',(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig'))[1])
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser115-output.txt').read_text(encoding='utf-8-sig')
    updates=load(ROOT/'build/npc115-test-sources/EXPECTATION_UPDATES.json')['files'];assert len(updates)==39
    for r in updates:
        assert sha(ROOT/'tests'/r['file'])==r['original_sha256'] and sha(ROOT/r['baseline_derived_path'])==r['baseline_derived_sha256'] and sha(ROOT/'build/npc115-test-sources'/r['file'])==r['generated_sha256']
        expected,n=re.subn(r'(?<=phraseCount\(\)==)10764\b','10799',(ROOT/r['baseline_derived_path']).read_text(encoding='utf-8-sig'))
        if r['file'] in ('TestNpc102.java','TestNpc92.java','TestNpc85.java'):expected=isolated_positive_upgrade(r['file'],expected);assert r['explicit_positive_upgrade']
        else:assert not r.get('explicit_positive_upgrade')
        assert expected==(ROOT/'build/npc115-test-sources'/r['file']).read_text(encoding='utf-8-sig') and n==r['new_story_count_replacements']
    assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    queue=rows(CORPUS/'translation_queue.jsonl');known={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')};assert sum(r['source'] in known for r in queue)==13984
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'debug_only_entries':debug_only,'added_entries':added,'byte_identical_runtime_classes':identical,'runtime_classes':len(classes),'method_differences':differences,'receipt_and_render_algorithms_unchanged':True,'fixed_literals':35,'new_integer_shapes':13,'bounded_total':243,'exact_before':13969,'exact_after':13984,'frozen_total':16244,'remaining':2260,'historical_tests_preserved':len(historical),'previous_delivery_files_verified':len(manifest),'fresh_test_groups':189,'parser_tests':15,'new_assertions':new_assertions,'unchanged_dispatch_rows':11251,'explicit_dispatch_coverage_upgrades':changes,'new_negative_upgrades':0,'isolated_expectation_files':39,'installed_pack_modified':False,'game_fps_measured':False}
    result['new_negative_upgrades']=6
    result['explicit_negative_to_positive_sources']=['[NPC] Mayor Nancy: Hi|','[NPC] Generow: Click to claim your reward!',"[NPC] Andrew: If you'd like to discuss SkyBlock with other players then check out the SkyBlock section of the forums!",'[NPC] Jerry: Your SkyBlock island is part of a much larger universe... [1/2]','[NPC] Jerry: Wait, how did I get here? [2/2]']
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
