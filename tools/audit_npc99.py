"""Same-final-JAR output equivalence, provenance and executable hot-path mutation audit."""
import argparse,json,re,subprocess,zipfile
from pathlib import Path
from prepare_npc99 import ROOT,SOURCE,DATA,CORPUS
from prepare_npc78 import rows,write,sha
BASE='76E85EF50B3308A390D5C11302198A16EAFD1C7CA2BE08F4AE46530C16718A50'
OLD=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_1')
CHAT_OLD=OLD.parent/'NPC_TRANSLATION_BATCH_2026-10-18_6'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def without_choices(s):
 marker='private static final java.util.Map<List<String>,List<String>> SAFETY_CHOICES=java.util.Map.ofEntries('
 assert s.count(marker)==1;start=s.index(marker);end=s.index('\n    );',start)+len('\n    );');return s[:start]+'<immutable authored label table>'+s[end:]
def main():
 p=argparse.ArgumentParser();p.add_argument('--dispatch',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args();a.out.mkdir(exist_ok=False)
 before=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.98-dev.jar';after=ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.99-dev.jar';assert sha(before)==BASE;fingerprint=sha(after)
 new={r['source']:r['translation'] for r in load(DATA/'npc-expansion99.json')['records']};assert len(new)==397
 old={r['source']:r for r in rows(a.dispatch/'0.15.98-dev/DISPATCH_ROWS.jsonl')};current={r['source']:r for r in rows(a.dispatch/'0.15.99-dev/DISPATCH_ROWS.jsonl')}
 assert old.keys()==current.keys() and len(current)==11247 and new.keys()<=current.keys()
 for s,r in current.items():
  if s in new:assert r['flat_result']==r['published_style_result']==new[s],s
  else:assert all(old[s][k]==r[k] for k in ('flat_result','published_style_result')),s
 assert sum(r['flat_changed_not_quality_proof'] and not r['published_style_changed_not_quality_proof'] for r in current.values())==72
 for version,h in [('0.15.98-dev',BASE),('0.15.99-dev',fingerprint)]:assert load(a.dispatch/version/'DISPATCH_SUMMARY.json')['runtime_jar_sha256']==h
 changed_expected=['fabric.mod.json','ru/wither/skyru/ChatNotificationTranslator$Edit.class','ru/wither/skyru/ChatNotificationTranslator$Part.class','ru/wither/skyru/ChatNotificationTranslator$QuizLine66.class','ru/wither/skyru/ChatNotificationTranslator.class','ru/wither/skyru/NpcDialogueTranslator.class']
 with zipfile.ZipFile(before) as x,zipfile.ZipFile(after) as y:
  assert y.testzip() is None and not set(x.namelist())-set(y.namelist())
  changed=sorted(n for n in set(x.namelist())&set(y.namelist()) if x.read(n)!=y.read(n));added=sorted(set(y.namelist())-set(x.namelist()))
  assert changed==changed_expected and added==['META-INF/licenses/NPC99-SOURCES.txt','data/witherskyru/npc-expansion99.json'],(changed,added)
  assert y.read('data/witherskyru/npc-expansion99.json')==(DATA/'npc-expansion99.json').read_bytes()
 rel='src/main/java/ru/wither/skyru/NpcDialogueTranslator.java';assert sha(OLD/rel)==load(OLD/'HASHES.json')[rel]
 pattern=r'(?s)    public static NpcDialogueTranslator storyDialogues\(\) throws IOException \{.*?\n    \}'
 x,y=(OLD/rel).read_text(encoding='utf-8-sig'),(ROOT/rel).read_text(encoding='utf-8-sig');assert len(re.findall(pattern,x))==len(re.findall(pattern,y))==1 and re.sub(pattern,'<startup story registration>',x)==re.sub(pattern,'<startup story registration>',y)
 rel='src/main/java/ru/wither/skyru/ChatNotificationTranslator.java';assert sha(CHAT_OLD/rel)==load(CHAT_OLD/'HASHES.json')[rel]
 with zipfile.ZipFile(ROOT/'build/libs/WitherSkyRU-26.1.2-fabric-0.15.93-dev.jar') as x,zipfile.ZipFile(before) as y:assert x.read('ru/wither/skyru/ChatNotificationTranslator.class')==y.read('ru/wither/skyru/ChatNotificationTranslator.class')
 assert without_choices((CHAT_OLD/rel).read_text(encoding='utf-8-sig'))==without_choices((ROOT/rel).read_text(encoding='utf-8-sig'))
 bytecode=[];java='C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin/javap.exe'
 for cls in ('NpcDialogueTranslator','ChatNotificationTranslator','ChatNotificationTranslator$Edit','ChatNotificationTranslator$Part','ChatNotificationTranslator$QuizLine66'):
  ds=[]
  for label,jar in [('before',before),('after',after)]:
   s=subprocess.run([java,'-J-Dfile.encoding=UTF-8','-J-Dstdout.encoding=UTF-8','-c','-p','-classpath',str(jar),'ru.wither.skyru.'+cls],capture_output=True,check=True,encoding='utf-8').stdout
   (a.out/(label+'-'+cls+'.txt')).write_text(s,encoding='utf-8')
   if cls=='NpcDialogueTranslator':
    patterns=[r'(?ms)^  public static ru\.wither\.skyru\.NpcDialogueTranslator storyDialogues\(\) throws java\.io\.IOException;\n.*?(?=^  (?:public|private|protected))',r'(?ms)^  private ru\.wither\.skyru\.NpcDialogueTranslator\(java\.lang\.String\[\], java\.util\.Set<java\.lang\.String>, boolean, boolean\) throws java\.io\.IOException;\n.*?(?=^  (?:public|private|protected))']
    for pattern in patterns:assert len(re.findall(pattern,s))==1;s=re.sub(pattern,'  <source-checked startup code>\n',s)
   if cls=='ChatNotificationTranslator':assert s.count('  static {};')==1;s=s[:s.index('  static {};')]+'<source-checked startup tables>\n}'
   normalized=re.sub(r'#\d+','#CP',s)
   # javap right-pads comments differently when #99 becomes #101. Keep all
   # instruction offsets/branches and comment contents; normalize that padding.
   ds.append(re.sub(r'[ \t]+//',' //',normalized))
  assert ds[0]==ds[1],cls;bytecode.append({'class':cls,'all_non_startup_executable_instructions_equal_after_constant_pool_reference_normalization':True})
 rev=load(SOURCE/'NPC_0.15.99_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.99_REVISION_DELTA.json');assert delta['accepted_for_batch99']
 assert {k:(rev['expected'][k],v) for k,v in rev['actual'].items() if rev['expected'].get(k)!=v}=={r['page']:(r['pinned_revision'],r['current_revision']) for r in delta['records']}
 assert all(r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged'] for r in delta['records'])
 result={'runtime_jar_sha256':fingerprint,'baseline_jar_sha256':BASE,'new_exact_outputs_flat_and_published':397,'old_output_rows_unchanged':10850,'old_style_gaps_unchanged':72,'changed_entries':changed,'added_entries':added,'source_changes_only_startup_registration_and_authored_choice_table':True,'all_other_runtime_class_payloads_identical':True,'bytecode':bytecode,'live_fps_measured':False,'installed_game_modified':False}
 write(a.out/'RESULT.json',result);print(json.dumps(result,indent=2))
if __name__=='__main__':main()
