"""Final-JAR regression/provenance audit, including unchanged renderer class payloads."""
import argparse,json,re,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,sha,rows,write

BASE='BD99E5491FACF8B6E5AA1AA730F33DF119CC6DFB7EFA272257DB49E29B3EB0BD'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_5')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
    p=argparse.ArgumentParser();p.add_argument('--checks',type=Path,required=True);p.add_argument('--dispatch',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.102-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.103-dev.jar';assert sha(before)==BASE;fingerprint=sha(after)
    previous=load(PREVIOUS/'HASHES.json')
    for rel,digest in previous.items():assert sha(PREVIOUS/rel)==digest,rel
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in set(x.namelist())&set(y.namelist()) if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class'],changed
        assert added==['META-INF/licenses/NPC103-SOURCES.txt','data/witherskyru/npc-names103.json','ru/wither/skyru/NamedNpcTemplates$Rule.class','ru/wither/skyru/NamedNpcTemplates.class'],added
        for rel in ('data/witherskyru/npc-names103.json','META-INF/licenses/NPC103-SOURCES.txt'):assert y.read(rel)==(ROOT/'src/main/resources'/rel).read_bytes()
        untouched=sum(n.endswith('.class') for n in x.namelist() if n not in changed)
    # Exact source diff, not a claim that modified receipt code is byte-identical.
    rel='src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');current=(ROOT/rel).read_text(encoding='utf-8-sig')
    changes=[('    private record Part(String text, int start, int end) {}','    record Part(String text, int start, int end) {}'),('    private record Edit(int start, int end, List<Part> parts) {}','    record Edit(int start, int end, List<Part> parts) {}\n    private static final NamedNpcTemplates NAMED_NPC_TEMPLATES = new NamedNpcTemplates();'),('        return List.of();\n    }\n\n    private List<Edit> planAuction(String body){','        return NAMED_NPC_TEMPLATES.plan(body,dictionary);\n    }\n\n    private List<Edit> planAuction(String body){')]
    for source,target in changes:assert old.count(source)==1;old=old.replace(source,target)
    assert current==old,'Unexpected receipt algorithm mutation'
    corpus=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1';assert sha(corpus/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    literal_keys={r['source'] for path in DATA.glob('*.json') for r in load(path).get('records',[]) if r.get('source') and r.get('translation')}
    queue=rows(corpus/'translation_queue.jsonl');assert len(queue)==16244 and sum(r['source'] in literal_keys for r in queue)==11092
    source=load(SOURCE/'NPC_NAMES_SOURCES_0.15.103.json');pack=load(DATA/'npc-names103.json');assert len(source['records'])==len(pack['templates'])==135
    assert len({r['speaker'] for r in source['records']})==92 and len(source['source_pages'])==93 and sum(len(r['layouts']) for r in source['records'])==136
    assert all(r['source'] not in literal_keys for r in source['records'])
    oldrows={r['source']:r for r in rows(a.dispatch/'0.15.102-dev/DISPATCH_ROWS.jsonl')};newrows={r['source']:r for r in rows(a.dispatch/'0.15.103-dev/DISPATCH_ROWS.jsonl')};assert oldrows.keys()==newrows.keys() and len(newrows)==11252
    assert all(oldrows[s][k]==r[k] for s,r in newrows.items() for k in ('flat_result','published_style_result'))
    for v,h in [('0.15.102-dev',BASE),('0.15.103-dev',fingerprint)]:assert load(a.dispatch/v/'DISPATCH_SUMMARY.json')['runtime_jar_sha256']==h
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==fingerprint and len(checks['tests_passed'])==len(set(checks['tests_passed']))==175 and 'TestNpcNames103' in checks['tests_passed'] and not checks['reused_successes']
    for test in checks['tests_passed']:
        output=(a.checks/(test+'-output.txt')).read_text(encoding='utf-8-sig')
        assert 'PASS' in output or test=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output,test
    newtest=(a.checks/'TestNpcNames103-output.txt').read_text(encoding='utf-8-sig');assert re.search(r'PASS: NpcNames103 (\d+) assertions',newtest)
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser103-output.txt').read_text(encoding='utf-8-sig')
    a.out.mkdir(exist_ok=False)
    result={'runtime_jar_sha256':fingerprint,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'unchanged_old_runtime_classes':untouched,'all_renderer_and_HUD_class_payloads_identical':True,'receipt_changes':'Existing rule priority retained; one closed fallback lookup; immutable offset plans compiled at startup; current style renderer unchanged.','fresh_test_groups':175,'parser_tests':15,'new_template_assertions':int(re.search(r'PASS: NpcNames103 (\d+) assertions',newtest)[1]),'unchanged_literal_dispatch_rows':11252,'literal_exact_keys':11092,'frozen_total':16244,'new_name_templates':135,'new_name_labels':92,'published_layouts':136,'pinned_source_pages':93,'max_rules_per_prefix':max(__import__('collections').Counter(r['prefix'] for r in pack['templates']).values()),'prior_delivery_files_verified':len(previous),'game_fps_or_frame_times_measured':False,'installed_pack_modified':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
