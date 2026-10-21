"""Reuse equal-work workbenches and bytecode checks; explicitly include3 current additions."""
import re
from prepare_npc102 import ROOT
BASE='E0EE8AA7086809A24B001F57F71902C778349215670C983BAA1382AF82606A09'
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def versions(s):return s.replace('0.15.101-dev','0.15.102-dev').replace('0.15.100-dev','0.15.101-dev').replace('1FE0127216BD7B39D4F41DEB976F70F25E6DBB1FA2B23B8B32499FDD856B7117',BASE)
s=(ROOT/'tests/BenchmarkNpc101.java').read_text().replace('BenchmarkNpc101','BenchmarkNpc102').replace('new_npc101','new_npc102').replace('new_choices101','new_choices102').replace('expansion101','expansion102').replace('New101','New102')
s=re.sub(r'inputs.put\("new_npc102_unequal_work",List.of\(.*?\)\);','inputs.put("new_npc102_unequal_work",List.of("§e[NPC] §6Mayor Aatrox§f: You slay.","§e[NPC] §6Grandma§f: Back in my day, booster cookies were §6800k§f.","§e[NPC] §aHoppity§f: §b✆ §fHe\'s a clever one, that fish. I can never catch him.","§e[NPC] §bMartin§f: Isn\'t this place fascinating?"));',s)
s=re.sub(r'inputs.put\("new_choices102_unequal_work",List.of\(.*?\)\);','inputs.put("new_choices102_unequal_work",List.of("Select an option: [I got it.]","Select an option: [Yep.]","Select an option: [Indeed.]","Select an option: [TELEPORT BACK]"));',s)
s=s.replace('modes100/101','modes101/102').replace('translate only on101','translate only on102')
emit('tests/BenchmarkNpc102.java',s)
emit('tests/BenchmarkNpc102Focused.java',(ROOT/'tests/BenchmarkNpc101Focused.java').read_text().replace('BenchmarkNpc101Focused','BenchmarkNpc102Focused'))
for suffix in ('','_focused'):
 s=versions((ROOT/f'tools/benchmark_npc101{suffix}.ps1').read_text()).replace('npc101-benchmark','npc102-benchmark').replace('npc101-focused','npc102-focused').replace('BenchmarkNpc101','BenchmarkNpc102').replace('NPC101','NPC102').replace('100,101,101,100','101,102,102,101').replace('Wrong100','Wrong101')
 emit(f'tools/benchmark_npc102{suffix}.ps1',s)
s=versions((ROOT/'tools/audit_npc101.py').read_text()).replace('from prepare_npc101','from prepare_npc102').replace('NPC_TRANSLATION_BATCH_2026-10-19_3','NPC_TRANSLATION_BATCH_2026-10-19_4').replace('npc-expansion101','npc-expansion102').replace('NPC101-SOURCES','NPC102-SOURCES').replace('len(new)==385','len(new)==319').replace('len(current)==11248','len(current)==11252').replace("'new_exact_outputs_flat_and_published':385","'new_exact_outputs_flat_and_published':319").replace("'old_output_rows_unchanged':10863","'old_output_rows_unchanged':10933")
needle="new={r['source']:r['translation'] for r in load(DATA/'npc-expansion102.json')['records']};assert len(new)==319";assert needle in s
s=s.replace(needle,"new={r['source']:r['translation'] for r in load(DATA/'npc-expansion102.json')['records']};assert len(new)==316\n extra={r['source']:r['translation'] for r in load(DATA/'npc-current102.json')['records']};assert len(extra)==3 and not new.keys()&extra.keys();new.update(extra);assert len(new)==319")
s=s.replace("added==['META-INF/licenses/NPC102-SOURCES.txt','data/witherskyru/npc-expansion102.json']","added==['META-INF/licenses/NPC102-SOURCES.txt','data/witherskyru/npc-current102.json','data/witherskyru/npc-expansion102.json']")
s=s.replace("NPC_0.15.101_PUBLIC_REVISION","NPC_0.15.102_PUBLIC_REVISION").replace("NPC_0.15.101_REVISION_DELTA","NPC_0.15.102_REVISION_DELTA").replace('accepted_for_batch101','accepted_for_batch102')
s=s.replace("assert all(r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged'] for r in delta['records'])","assert all(r['old_dialogue_templates_retained'] for r in delta['records'])")
s=s.replace(" result={"," assert sha(CORPUS/'translation_queue.jsonl')=='FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925'\n for version in ('0.15.101-dev','0.15.102-dev'):assert load(a.dispatch/version/'DISPATCH_SUMMARY.json')['queue_sha256']==sha(ROOT/'build/npc102-screening-input/translation_queue.jsonl')\n result={'frozen_ledger_rows':16244,'screening_input_rows':16248,'supplemental_current_rows':4,'new_supplemental_rows':3,")
emit('tools/audit_npc102.py',s)
print('Derived101 ->102 evidence;316 frozen +3 current new outputs;4 extra screening rows, one retained.')
