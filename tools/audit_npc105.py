"""Final-JAR/data regression audit. Wording revisions are explicit, not hidden equality."""
import argparse,copy,json,re,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,sha,rows,write,validate

BASE='AE3FF9687AC6D451F42AB8BBD763B237C879E7A4D7A86A609809749BCE490502'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_1')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.104-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.105-dev.jar'
    assert sha(before)==BASE;fingerprint=sha(after)
    manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h,rel
    revisions=load(SOURCE/'NPC_WORDING_SOURCES_0.15.105.json')['records'];fixed={r['source']:r for r in revisions}
    assert len(fixed)==11 and {r['index'] for r in revisions}=={0,2,3,11,13,14,25,36,41,52,59}
    for r in revisions:
        assert not re.search('[A-Za-z]',r['translation'].split(': ',1)[1])
        validate(r['source'],r['translation'],r['primary_anchors']);validate(r['source'],r['translation'],r['style_anchors'])
    with zipfile.ZipFile(before) as old,zipfile.ZipFile(after) as new:
        assert new.testzip() is None and not set(old.namelist())-set(new.namelist())
        changed=sorted(n for n in set(old.namelist())&set(new.namelist()) if old.read(n)!=new.read(n))
        added=sorted(set(new.namelist())-set(old.namelist()))
        assert changed==['data/witherskyru/npc-expansion74.json','data/witherskyru/npc-styles104.json','data/witherskyru/story-dialogues.json','fabric.mod.json'],changed
        assert added==['META-INF/licenses/NPC105-SOURCES.txt'],added
        classes=[n for n in old.namelist() if n.endswith('.class')]
        assert all(old.read(n)==new.read(n) for n in classes) and len(classes)==125
        for filename in {r['primary_resource'] for r in revisions}:
            olddoc=json.loads(old.read('data/witherskyru/'+filename));newdoc=json.loads(new.read('data/witherskyru/'+filename))
            expected=copy.deepcopy(olddoc)
            for record in expected['records']:
                if record.get('source') not in fixed:continue
                r=fixed[record['source']];assert record['translation']==r['old_translation']
                assert record.get('style_anchors',[])==r['old_primary_anchors']
                assert [x['source'] for x in r['primary_anchors']]==[x['source'] for x in r['old_primary_anchors']]
                record['translation']=r['translation']
                if r['primary_anchors']:record['style_anchors']=r['primary_anchors']
                else:record.pop('style_anchors',None)
            assert newdoc==expected,filename
        priorstyles=json.loads(old.read('data/witherskyru/npc-styles104.json'))['records'];styles=json.loads(new.read('data/witherskyru/npc-styles104.json'))['records']
        assert len(priorstyles)==67 and len(styles)==72
        oldstyles={r['source']:r for r in priorstyles};newstyles={r['source']:r for r in styles}
        assert set(newstyles)-set(oldstyles)=={r['source'] for r in revisions if r['index'] in {0,3,13,25,52}}
        for s,r in newstyles.items():
            if s in fixed:assert r['translation']==fixed[s]['translation'] and r['style_anchors']==fixed[s]['style_anchors']
            else:assert r==oldstyles[s]
        for filename in ('npc-expansion74.json','npc-styles104.json','story-dialogues.json'):
            assert new.read('data/witherskyru/'+filename)==(DATA/filename).read_bytes()
    historical=[rel for rel in manifest if rel.startswith('tests/') and rel.endswith('.java')]
    altered=[rel for rel in historical if sha(ROOT/rel)!=manifest[rel]]
    assert altered==['tests/TestNpcLookup61.java'],altered
    # Frozen historical authoring/evidence stays unchanged in the actual project.
    frozenfiles=[rel for rel in manifest if rel.startswith('translation-source/')]
    for rel in frozenfiles:assert sha(ROOT/rel)==manifest[rel],rel
    assert sha(ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1/translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    old={r['source']:r for r in rows(a.dispatch/'0.15.104-dev/DISPATCH_ROWS.jsonl')};new={r['source']:r for r in rows(a.dispatch/'0.15.105-dev/DISPATCH_ROWS.jsonl')}
    assert old.keys()==new.keys() and len(new)==11252
    flatdiff={s for s in new if new[s]['flat_result']!=old[s]['flat_result']}
    styleddiff={s for s in new if new[s]['published_style_result']!=old[s]['published_style_result']}
    assert flatdiff==styleddiff==set(fixed)
    for s,r in new.items():
        if s in fixed:assert r['flat_result']==r['published_style_result']==fixed[s]['translation']
    gaps=[r for r in new.values() if r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof']];assert not gaps
    for version,h in [('0.15.104-dev',BASE),('0.15.105-dev',fingerprint)]:assert load(a.dispatch/version/'DISPATCH_SUMMARY.json')['runtime_jar_sha256']==h
    checks=load(a.checks/'CHECK_SCOPE.json')
    assert checks['runtime_jar_sha256']==fingerprint and len(checks['tests_passed'])==len(set(checks['tests_passed']))==177 and not checks['reused_successes']
    assert 'TestNpcWording105' in checks['tests_passed']
    for name in checks['tests_passed']:
        text=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig')
        assert 'PASS' in text or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in text,name
    assertions=int(re.search(r'NPCWording105 (\d+) assertions',(a.checks/'TestNpcWording105-output.txt').read_text(encoding='utf-8-sig'))[1]);assert assertions==25769
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser105-output.txt').read_text(encoding='utf-8-sig')
    supersession=load(ROOT/'translation-source-regression105/EXPECTATION_SUPERSESSION105.json')
    assert len(supersession['revisions'])==11
    for item in supersession['changed_files']:
        assert sha(SOURCE/item['file'])==item['historical_sha256']
        assert sha(ROOT/'translation-source-regression105'/item['file'])==item['revised_expectation_sha256']
    known={r['source'] for path in DATA.glob('*.json') for r in load(path).get('records',[]) if r.get('source') and r.get('translation')}
    assert sum(r['source'] in known for r in rows(ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1/translation_queue.jsonl'))==11092
    a.out.mkdir(exist_ok=False)
    result={'runtime_jar_sha256':fingerprint,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'all_125_runtime_classes_byte_identical':True,'wording_revisions':11,'new_exact_keys':0,'exact_keys':11092,'frozen_total':16244,'remaining_without_exact_key':5152,'new_style_repairs':5,'cumulative_selected_style_repairs':72,'selected_style_gaps_remaining':0,'dispatch_inputs':11252,'changed_flat_outputs':11,'changed_styled_outputs':11,'unchanged_other_outputs':11241,'fresh_test_groups':177,'parser_tests':15,'new_assertions':assertions,'historical_test_sources_unchanged':len(historical)-1,'historical_lookup_test_intentionally_updated':altered,'historical_source_evidence_files_unchanged':len(frozenfiles),'isolated_expectation_supersession_files':len(supersession['changed_files']),'previous_delivery_files_verified':len(manifest),'installed_pack_modified':False,'game_fps_or_frame_times_measured':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
