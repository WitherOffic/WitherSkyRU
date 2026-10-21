"""Evidence audit: separate archives/replies/exact keys;prove hot methods unchanged."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc78 import ROOT,DATA,SOURCE,rows,sha,write

BASE='9D19995EED91114A500A98AD540A41D1765B12BAC665340D6E1C68F20F9C627B'
PREVIOUS=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-20_2')
JAVA=Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))

def instruction_semantics(part):
    # Appending startup constants can widen ldc to ldc_w and move byte offsets
    # without changing an operation. Preserve branch/exception topology using
    # instruction ordinals;retain resolved constant/member comments verbatim.
    lines=part.splitlines();ops=[]
    for line in lines:
        match=re.match(r'^\s+(\d+):\s+([a-z][a-z0-9_]*)(.*)$',line)
        if match:ops.append(int(match[1]))
    ordinals={pc:n for n,pc in enumerate(ops)}
    if ops:ordinals[ops[-1]+1]=len(ops) # terminal return/athrow endpoint
    def destination(value):
        value=int(value);assert value in ordinals,('Unknown branch/exception endpoint',value)
        return '@'+str(ordinals[value])
    out=[]
    for line in lines:
        match=re.match(r'^\s+(\d+):\s+([a-z][a-z0-9_]*)(.*)$',line)
        if match:
            opcode=match[2];args=match[3].strip()
            if opcode.startswith('if') or opcode in ('goto','goto_w','jsr','jsr_w'):
                args=re.sub(r'^-?\d+',lambda m:destination(m[0]),args)
            opcode={'ldc_w':'ldc','goto_w':'goto','jsr_w':'jsr'}.get(opcode,opcode)
            line='@'+str(ordinals[int(match[1])])+': '+opcode+' '+args
        elif re.match(r'^\s+(?:-?\d+|default):\s+-?\d+\s*$',line):
            line=re.sub(r'(:\s+)(-?\d+)\s*$',lambda m:m[1]+destination(m[2]),line).strip()
        elif re.match(r'^\s+\d+\s+\d+\s+\d+\s+\S',line):
            match=re.match(r'^\s+(\d+)\s+(\d+)\s+(\d+)\s+(.*)',line)
            line=' '.join(destination(match[i]) for i in (1,2,3))+' '+match[4]
        line=re.sub(r'#\d+','#',line).strip()
        if '//' in line:
            instruction,comment=line.split('//',1)
            line=re.sub(r'\s+',' ',instruction).strip()+' // '+comment.lstrip()
        out.append(line)
    return '\n'.join(out).strip()

def sections(text):
    return {p.splitlines()[0].strip():instruction_semantics(p) for p in re.split(r'(?m)^  (?=\S)',text)[1:]}

def without_line_tables(raw):
    """Compare every other classfile byte,not just javap's instruction listing."""
    result=bytearray(raw);u2=lambda n:int.from_bytes(raw[n:n+2],'big');u4=lambda n:int.from_bytes(raw[n:n+4],'big')
    at=10;index=1;names={}
    while index<u2(8):
        tag=raw[at];at+=1
        if tag==1:
            size=u2(at);at+=2;names[index]=raw[at:at+size].decode('utf-8');at+=size
        elif tag in (3,4,9,10,11,12,17,18):at+=4
        elif tag in (5,6):at+=8;index+=1
        elif tag in (7,8,16,19,20):at+=2
        elif tag==15:at+=3
        else:raise AssertionError(('Unknown classfile tag',tag))
        index+=1
    def attributes(pos):
        count=u2(pos);pos+=2
        for _ in range(count):
            name=names[u2(pos)];size=u4(pos+2);start=pos+6;end=start+size
            if name=='LineNumberTable':result[start:end]=bytes(size)
            elif name=='Code':
                nested=start+8+u4(start+4);nested+=2+8*u2(nested)
                assert attributes(nested)==end
            pos=end
        return pos
    at+=6;at+=2+2*u2(at)
    for _ in range(2):
        count=u2(at);at+=2
        for _ in range(count):at=attributes(at+6)
    at=attributes(at);assert at==len(raw)
    return bytes(result)

def main():
    ap=argparse.ArgumentParser()
    for name in ('checks','dispatch','snapshots','out'):ap.add_argument('--'+name,type=Path,required=True)
    a=ap.parse_args();before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.105-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.106-dev.jar'
    assert sha(before)==BASE;digest=sha(after);manifest=load(PREVIOUS/'HASHES.json')
    for rel,h in manifest.items():assert sha(PREVIOUS/rel)==h
    historical=[p for p in manifest if p.startswith('tests/') and p.endswith('.java')]
    for rel in historical:assert sha(ROOT/rel)==manifest[rel],rel
    for rel in manifest:
        if rel.startswith('translation-source/'):assert sha(ROOT/rel)==manifest[rel],rel
    # Only closed startup registrations are edited in the two production sources.
    rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');new=(ROOT/rel).read_text(encoding='utf-8-sig')
    assert old.replace('"/data/witherskyru/npc-current102.json"}','"/data/witherskyru/npc-current102.json","/data/witherskyru/npc-expansion106.json"}')==new
    rel='src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';old=(PREVIOUS/rel).read_text(encoding='utf-8-sig');new=(ROOT/rel).read_text(encoding='utf-8-sig')
    entries=[]
    for line in (SOURCE/'NPC_CHOICES_0.15.106_REVIEWED.tsv').read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):continue
        left,right=map(json.loads,line.split('|'));fmt=lambda xs:','.join(json.dumps(s,ensure_ascii=False) for s in xs)
        entries.append('        java.util.Map.entry(List.of('+fmt(left)+'),List.of('+fmt(right)+'))')
    marker='        java.util.Map.entry(List.of("TELEPORT BACK"),List.of("ТЕЛЕПОРТИРОВАТЬСЯ НАЗАД"))'
    assert old.replace(marker,marker+',\n'+',\n'.join(entries))==new
    assert len(re.findall(r'Map\.entry\(List\.of\(',old))==289 and len(re.findall(r'Map\.entry\(List\.of\(',new))==345
    with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
        assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
        changed=sorted(n for n in x.namelist() if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
        expected=['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class']
        assert changed==expected,changed
        assert added==['META-INF/licenses/NPC106-SOURCES.txt','data/witherskyru/npc-expansion106.json'],added
        assert y.read('data/witherskyru/npc-expansion106.json')==(DATA/'npc-expansion106.json').read_bytes()
        classes=[n for n in x.namelist() if n.endswith('.class')];assert len(classes)==125
        for name in changed:
            if '$' in name:assert without_line_tables(x.read(name))==without_line_tables(y.read(name)),name
    a.out.mkdir(exist_ok=False);method_differences={}
    for path in [p for p in changed if p.endswith('.class')]:
        name=path[:-6].replace('/','.');outputs=[]
        for version,jar in [('105',before),('106',after)]:
            output=subprocess.run([str(JAVA/'javap.exe'),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-J-Dstderr.encoding=UTF-8','-p','-c','-classpath',str(jar),name],capture_output=True,text=True,encoding='utf-8',check=True,timeout=45).stdout
            (a.out/(name+'-'+version+'.txt')).write_text(output,encoding='utf-8');outputs.append(sections(output))
        assert outputs[0].keys()==outputs[1].keys()
        differences=[key for key in outputs[0] if outputs[0][key]!=outputs[1][key]]
        expected=[]
        if name=='ru.wither.skyru.ChatNotificationTranslator':expected=['static {};']
        if name=='ru.wither.skyru.NpcDialogueTranslator':expected=['public static ru.wither.skyru.NpcDialogueTranslator storyDialogues() throws java.io.IOException;']
        assert differences==expected,(name,differences)
        method_differences[name]=differences
    # Full existing literal screening must be unchanged;new archives are separately tested.
    x=rows(a.dispatch/'0.15.105-dev/DISPATCH_ROWS.jsonl');y=rows(a.dispatch/'0.15.106-dev/DISPATCH_ROWS.jsonl')
    assert len(x)==len(y)==11252
    for old,new in zip(x,y):assert old['source']==new['source'] and all(old[k]==new[k] for k in ('flat_result','published_style_result')),old['source']
    evidence=load(SOURCE/'NPC_SOURCES_0.15.106.json')['records'];fixed={r['runtime_record']['source']:r['runtime_record']['translation'] for r in evidence};assert len(fixed)==103
    assert all(r['published_row']['historical_heading'] and not r['published_row']['variable_definition'] for r in evidence)
    snaps=[load(a.snapshots/v/'NEW_ROWS.json') for v in ('0.15.105-dev','0.15.106-dev')]
    assert snaps[0]['runtime_jar_sha256']==BASE and snaps[1]['runtime_jar_sha256']==digest
    assert len(snaps[0]['npc'])==len(snaps[1]['npc'])==206 and len(snaps[0]['choices'])==len(snaps[1]['choices'])==58
    for old,new in zip(snaps[0]['npc'],snaps[1]['npc']):
        assert old['source']==new['source'] and old['kind']==new['kind']
        assert old['flat']==old['styled']==old['source'] and new['flat']==new['styled']==fixed[new['source']]
    choices=load(SOURCE/'NPC_CHOICES_SOURCES_0.15.106.json')['records'];assert len(choices)==56 and sum(len(r['source_labels']) for r in choices)==75
    lookup={o['source']:r for r in choices for o in r['all_occurrences']}
    for old,new in zip(snaps[0]['choices'],snaps[1]['choices']):
        assert old['source']==new['source'] and old['legacy']==new['legacy']
        r=lookup[new['source']];target=new['source'].replace('Select an option:','Выберите ответ:')
        for source,ru in zip(r['source_labels'],r['target_labels']):target=target.replace('['+source+']','['+ru+']')
        assert old['flat']==old['styled']==old['source'] and new['flat']==new['styled']==target
    checks=load(a.checks/'CHECK_SCOPE.json');assert checks['runtime_jar_sha256']==digest and len(checks['tests_passed'])==len(set(checks['tests_passed']))==179 and not checks['reused_successes']
    for name in checks['tests_passed']:
        output=(a.checks/(name+'-output.txt')).read_text(encoding='utf-8-sig');assert 'PASS' in output or name=='TestNpcObserved' and 'NPC SNAPSHOT: 76 rendered / 0 unsupported' in output,name
    assert 'NPC106 217703 assertions' in (a.checks/'TestNpc106-output.txt').read_text(encoding='utf-8-sig')
    assert 'NPCChoices106 81691 assertions' in (a.checks/'TestNpcChoices106-output.txt').read_text(encoding='utf-8-sig')
    assert 'Ran 15 tests' in (a.checks/'TestNpcCorpusParser106-output.txt').read_text(encoding='utf-8-sig')
    transformed=load(ROOT/'build/npc106-test-sources/COUNT_EXPECTATION_UPDATES.json')['files'];assert len(transformed)==29
    for item in transformed:
        assert sha(ROOT/'tests'/item['file'])==item['source_sha256'] and sha(ROOT/'build/npc106-test-sources'/item['file'])==item['generated_sha256']
        old=(ROOT/'tests'/item['file']).read_text(encoding='utf-8-sig');new=(ROOT/'build/npc106-test-sources'/item['file']).read_text(encoding='utf-8-sig')
        assert re.sub(r'(?<=phraseCount\(\)==)10525\b','10628',old)==new
    corpus=ROOT/'build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1';assert sha(corpus/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'
    known={r['source'] for path in DATA.glob('*.json') for r in load(path).get('records',[]) if r.get('source') and r.get('translation')}
    assert sum(r['source'] in known for r in rows(corpus/'translation_queue.jsonl'))==11195
    result={'runtime_jar_sha256':digest,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'byte_identical_runtime_classes':120,'method_differences':method_differences,'all_receipt_and_render_algorithms_unchanged':True,'nested_debug_line_tables_changed_only':3,'new_archival_literals':103,'new_current_choice_groups':56,'new_label_slots':75,'exact_before':11092,'exact_after':11195,'frozen_total':16244,'without_exact_after':5049,'existing11252_flat_and_styled_outputs_unchanged':True,'new_npc_cases':206,'new_choice_layouts':58,'fresh_test_groups':179,'parser_tests':15,'new_npc_assertions':217703,'new_choice_assertions':81691,'historical_test_sources_unchanged':len(historical),'isolated_count_assertion_updates':29,'previous_delivery_files_verified':len(manifest),'installed_pack_modified':False,'game_fps_measured':False}
    write(a.out/'AUDIT.json',result);print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
