"""113 evidence gate:bounded patterns separate from exact keys,legacy positives untouched."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write
from prepare_npc113 import BASE,CORPUS,load
from prepare_npc103 import segment
from audit_npc106 import JAVA,sections,without_line_tables
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_9')

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.112-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.113-dev.jar';assert sha(before)==BASE;digest=sha(after)
    manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h,rel
    historical=[r for r in manifest if r.startswith('tests/') and r.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    path='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/path).read_text(encoding='utf-8-sig');assert (ROOT/path).read_text(encoding='utf-8-sig')==old.replace('"/data/witherskyru/npc-expansion112.json"}','"/data/witherskyru/npc-expansion112.json","/data/witherskyru/npc-literals113.json"}')
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        assert changed==['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NamedNpcTemplates$Rule.class','ru/wither/skyru/NamedNpcTemplates.class','ru/wither/skyru/NpcDialogueTranslator.class'],changed
        assert added==['META-INF/licenses/NPC113-SOURCES.txt','data/witherskyru/npc-literals113.json','data/witherskyru/npc-structured113.json'],added
        classes=[n for n in x.namelist() if n.endswith('.class')];identical=sum(x.read(n)==y.read(n) for n in classes);assert len(classes)==125 and identical==120
        debug_only='ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class'
        assert without_line_tables(x.read(debug_only))==without_line_tables(y.read(debug_only))
        assert all(x.read(n)==y.read(n) for n in classes if any(word in n for word in ('mixin/','Hud','Render','SkyRuClient.class','OfflineDictionary.class')))
        for name in ('npc-literals113.json','npc-structured113.json'):assert y.read('data/witherskyru/'+name)==(DATA/name).read_bytes()
    evidence=load(SOURCE/'NPC_STRUCTURED_SOURCES_0.15.113.json');records=evidence['records'];assert len(records)==95 and len(evidence['retained_not_new'])==13
    assert sum(r['archival'] for r in records)==4 and len({r['speaker'] for r in records})==44 and len(evidence['source_pages'])==45
    assert sum(r['capture_type']!='username' for r in records)==79 and sum(r['color_fix'] for r in records)==2
    assert load(DATA/'npc-structured113.json')['templates']==[r['runtime_rule'] for r in records]
    authored={}
    for line in (SOURCE/'NPC_STRUCTURED_0.15.113_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#'):
            uid,body,defs=line.split('|');authored[uid]=(body,[{'source':k,'target':v} for k,v in json.loads(defs).items()])
    stats=load(SOURCE/'NPC_STAT_COMPONENTS_0.15.113_REVIEWED.json');stat_count=0
    for r in records:
        s=r['source'];prefix=s.index(': ')+2
        if r['capture_type']=='stat_number':
            assert r['speaker'] in stats['speakers'];expected=stats['bodies'][s[prefix:]];anchors=[];stat_count+=1
        else:expected,anchors=authored[r['id']]
        assert r['translation']==s[:prefix]+expected and anchors==r['style_anchors']
        left,right=s[len(r['runtime_rule']['prefix']):].split('{value}');tl,tr=expected.removeprefix('✆ ').split('{value}')
        assert r['runtime_rule']['before']==segment(left,tl,anchors) and r['runtime_rule']['after']==segment(right,tr,anchors)
    assert stat_count==54
    literal=evidence['literal_records'];assert len(literal)==1 and literal[0]['source']=='[NPC] Dean: There is a rogue Barbarian going by the name of Duke X.'
    assert load(DATA/'npc-literals113.json')['records']==[{k:literal[0][k] for k in ('speaker','tag','source','translation','style_anchors','source_url')}]
    snaps=[load(a.snapshots/v/'SNAPSHOTS.json') for v in ('0.15.112-dev','0.15.113-dev')];assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['records'])==len(snaps[1]['records'])==96;by_source={r['source']:r for r in records+literal};layouts=0
    for x,y in zip(snaps[0]['records'],snaps[1]['records']):
        assert x['source']==y['source'];r=by_source[x['source']];target=r['translation'].replace('{value}',r.get('probe_value',''));assert y['flat']==target
        assert x['flat']==(target if r.get('color_fix') else y['probe_source'])
        assert [v['legacy'] for v in x['layouts']]==[v['legacy'] for v in y['layouts']]==r['layouts']
        assert all(v['output']==y['probe_source'] for v in x['layouts']) and all(v['output']==target for v in y['layouts']);layouts+=len(y['layouts'])
    x=rows(a.dispatch/'0.15.112-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.113-dev/DISPATCH_ROWS.jsonl');assert len(x)==len(y)==11252 and x==y
    proof=load(a.dispatch/'RUNTIME_SCOPE.json')['records'];assert len(proof)==2
    for r in proof:
        assert r['runtime_jar_sha256']==(BASE if r['version']=='0.15.112-dev' else digest)
        assert sha(a.dispatch/r['version']/'DISPATCH_ROWS.jsonl')==r['rows_sha256'] and sha(a.dispatch/r['version']/'DISPATCH_SUMMARY.json')==r['summary_sha256']
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==186 and not checks['reused_successes']
    assert Path(checks['source_root']).resolve()==(ROOT/'translation-source-regression113').resolve()
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output
    assertions=int(re.search(r'NPC113 (\d+) assertions',(a.checks/'TestNpc113-output.txt').read_text(encoding='utf-8-sig'))[1]);assert assertions==918356
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser113-output.txt').read_text(encoding='utf-8-sig')
    updates=load(ROOT/'build/npc113-test-sources/EXPECTATION_UPDATES.json')['files'];assert len(updates)==37
    for r in updates:
        p=ROOT/'tests'/r['file'];prior=ROOT/r['baseline_derived_path'];q=ROOT/'build/npc113-test-sources'/r['file']
        assert sha(p)==r['original_sha256'] and sha(prior)==r['baseline_derived_sha256'] and sha(q)==r['generated_sha256']
        expected,n=re.subn(r'(?<=phraseCount\(\)==)10763\b','10764',prior.read_text(encoding='utf-8-sig'));assert n==r['new_count_replacements']
        if r['file']=='TestNpc86.java':
            before_check='check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"placeholder never faked as literal coverage");'
            after_check='check(!packaged.containsKey(source)&&(source.equals('+json.dumps(literal[0]['source'],ensure_ascii=False)+')?plain(story.translate(literal,db)).equals('+json.dumps(literal[0]['translation'],ensure_ascii=False)+'):story.translate(literal,db)==literal),"113 fixed Duke X literal reviewed;all actual placeholders remain excluded");'
            assert expected.count(before_check)==1;expected=expected.replace(before_check,after_check)
        assert expected==q.read_text(encoding='utf-8-sig')
    supersession=ROOT/'translation-source-regression113/EXPECTATION_SUPERSESSION105.json';assert sha(supersession)==sha(PREVIOUS/'expectation-supersession/EXPECTATION_SUPERSESSION105.json')
    for r in load(supersession)['changed_files']:assert sha(ROOT/'translation-source-regression113'/r['file'])==sha(PREVIOUS/'expectation-supersession'/r['file'])
    queue=rows(CORPUS/'translation_queue.jsonl');assert len(queue)==16244 and sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    known={r['source'] for p in DATA.glob('*.json') for r in load(p).get('records',[]) if r.get('source') and r.get('translation')};assert sum(r['source'] in known for r in queue)==13745
    a.out.mkdir(exist_ok=False);differences={}
    allowed={'ru.wither.skyru.NpcDialogueTranslator':{'public static ru.wither.skyru.NpcDialogueTranslator storyDialogues() throws java.io.IOException;'},'ru.wither.skyru.ChatNotificationTranslator':{'private java.util.List<ru.wither.skyru.ChatNotificationTranslator$Edit> planNpcRequest(java.lang.String);'},'ru.wither.skyru.NamedNpcTemplates':{'ru.wither.skyru.NamedNpcTemplates();','java.util.List<ru.wither.skyru.ChatNotificationTranslator$Edit> plan(java.lang.String, ru.wither.skyru.OfflineDictionary);','private static boolean number(java.lang.String, int, int, int);'}}
    for name,approved in allowed.items():
        parts=[]
        for version,jar in [('112',before),('113',after)]:
            output=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout
            (a.out/(name+'-'+version+'.txt')).write_text(output,encoding='utf-8');parts.append(sections(output))
        diff=[k for k in sorted(parts[0].keys()|parts[1].keys()) if parts[0].get(k)!=parts[1].get(k)];assert set(diff)==approved,(name,diff);differences[name]=diff
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':identical,'render_hud_and_client_dispatch_byte_identical':True,'method_differences':differences,'new_patterns':93,'color_repairs':2,'pattern_resource_count':95,'numeric_patterns':79,'username_patterns':16,'archival_patterns':4,'speaker_labels':44,'pinned_pages':45,'new_exact_keys':1,'exact_before':13744,'exact_after':13745,'frozen_total':16244,'without_exact_after':2499,'unchanged_dispatch_rows':11252,'primary_cases':96,'published_layouts':layouts,'fresh_test_groups':186,'parser_tests':15,'new_assertions':assertions,'historical_test_sources_unchanged':len(historical),'isolated_expectation_files':37,'explicit_Duke_X_coverage_upgrade':True,'previous_delivery_files_verified':len(manifest),'installed_pack_modified':False,'game_fps_measured':False}
    result['line_table_only_entries']=[debug_only]
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
