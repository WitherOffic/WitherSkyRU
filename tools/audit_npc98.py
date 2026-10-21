"""Same-JAR provenance, output equivalence and executable/source mutation audit."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc98 import ROOT,SOURCE,DATA,CORPUS
from prepare_npc78 import rows,write,sha

BASE='97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1'
SNAPSHOT=Path('C:/Users/Wither/Desktop/WitherSkyRU/OPTIMIZATION_BATCH_2026-10-19_3')
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--dispatch',type=Path,required=True);parser.add_argument('--out',type=Path,required=True);a=parser.parse_args();a.out.mkdir(exist_ok=False)
 before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.96-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.98-dev.jar';assert sha(before)==BASE;fingerprint=sha(after)
 raw=load(DATA/'dungeon-expansion98.json')['records'];new={r['source']:r['translation'] for r in raw};assert len(new)==336
 previous={r['source']:r for r in rows(a.dispatch/'0.15.96-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.dispatch/'0.15.98-dev/DISPATCH_ROWS.jsonl')}
 assert previous.keys()==current.keys() and len(current)==11247 and new.keys()<=current.keys()
 for source,r in current.items():
  if source in new:assert r['flat_result']==r['published_style_result']==new[source],source
  else:assert all(previous[source][k]==r[k] for k in ('flat_result','published_style_result')),source
 assert sum(r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof'] for r in current.values())==72
 for version,h in [('0.15.96-dev',BASE),('0.15.98-dev',fingerprint)]:assert load(a.dispatch/version/'DISPATCH_SUMMARY.json')['runtime_jar_sha256']==h
 expected_changed=['fabric.mod.json','ru/wither/skyru/NpcDialogueTranslator$StyleCorrections.class','ru/wither/skyru/NpcDialogueTranslator.class']
 expected_added=['META-INF/licenses/NPC98-SOURCES.txt','data/witherskyru/dungeon-expansion98.json']
 with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
  assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
  changed=sorted(n for n in set(x.namelist())&set(y.namelist()) if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
  assert changed==expected_changed and added==expected_added,(changed,added)
  assert y.read('data/witherskyru/dungeon-expansion98.json')==(DATA/'dungeon-expansion98.json').read_bytes()
 java=Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin/javap.exe')
 bytecode=[]
 for classname in ('ru.wither.skyru.NpcDialogueTranslator','ru.wither.skyru.NpcDialogueTranslator$StyleCorrections'):
  ds=[]
  for label,jar in [('before',before),('after',after)]:
   text=subprocess.run([str(java),'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-c','-p','-classpath',str(jar),classname],capture_output=True,check=True,encoding='utf-8').stdout
   (a.out/(label+'-'+classname.rsplit('.',1)[1]+'.txt')).write_text(text,encoding='utf-8')
   if '$' not in classname:
    pattern=r'(?ms)^  public static ru\.wither\.skyru\.NpcDialogueTranslator dungeonDialogues\(\) throws java\.io\.IOException;\n.*?(?=^  (?:public|private|protected))'
    assert len(re.findall(pattern,text))==1;text=re.sub(pattern,'  <startup dungeon resource registration>\n',text)
    # The unchanged startup JSON constructor crosses a constant-pool width
    # boundary (ldc/ldc_w), relocating its jumps. Its source is checked below.
    # Receipt/render methods remain compared without dropping offsets/branches.
    pattern=r'(?ms)^  private ru\.wither\.skyru\.NpcDialogueTranslator\(java\.lang\.String\[\], java\.util\.Set<java\.lang\.String>, boolean, boolean\) throws java\.io\.IOException;\n.*?(?=^  (?:public|private|protected))'
    assert len(re.findall(pattern,text))==1;text=re.sub(pattern,'  <source-identical startup JSON constructor>\n',text)
   ds.append(re.sub(r'#\d+','#CP',text))
  assert ds[0]==ds[1],classname;bytecode.append({'class':classname,'receipt_render_and_other_non_startup_executable_disassembly_identical':True,'excluded_startup_factory_and_json_constructor': '$' not in classname})
 factory=r'(?s)    public static NpcDialogueTranslator dungeonDialogues\(\) throws IOException \{.*?\n    \}'
 # Delivered96 includes only its two edited Java sources. The NPC implementation
 # was unchanged since94; verify that executable identity before using94 source.
 earlier=SNAPSHOT.parent/'OPTIMIZATION_BATCH_2026-10-19_1'
 rel=Path('src/main/java/ru/wither/skyru/NpcDialogueTranslator.java');old=earlier/rel
 assert sha(old)==load(earlier/'HASHES.json')[rel.as_posix()]
 with zipfile.ZipFile(earlier/'WitherSkyRU-26.1.2-fabric-0.15.94-dev.jar') as x,zipfile.ZipFile(before) as y:assert x.read('ru/wither/skyru/NpcDialogueTranslator.class')==y.read('ru/wither/skyru/NpcDialogueTranslator.class')
 x,y=old.read_text(encoding='utf-8-sig'),(ROOT/rel).read_text(encoding='utf-8-sig');assert len(re.findall(factory,x))==len(re.findall(factory,y))==1
 assert re.sub(factory,'<registration>',x)==re.sub(factory,'<registration>',y)
 for path in (SNAPSHOT/'src/main/java').rglob('*.java'):assert sha(path)==sha(ROOT/path.relative_to(SNAPSHOT)),path
 revision=load(SOURCE/'NPC_0.15.98_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.98_REVISION_DELTA.json');assert delta['accepted_for_batch98']
 assert {k:(revision['expected'][k],v) for k,v in revision['actual'].items() if revision['expected'].get(k)!=v}=={r['page']:(r['pinned_revision'],r['current_revision']) for r in delta['records']}
 assert all(r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged'] for r in delta['records'])
 result={'runtime_jar_sha256':fingerprint,'baseline_jar_sha256':BASE,'changed_entries':changed,'added_entries':added,'new_exact_outputs_flat_and_published':336,'old_output_rows_unchanged':len(current)-336,'old_style_gaps_unchanged':72,'npc_source_unchanged_except_startup_resource_registration':True,'all_other_runtime_class_payloads_byte_identical':True,'bytecode':bytecode,'live_fps_measured':False,'installed_game_modified':False}
 write(a.out/'RESULT.json',result);print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
