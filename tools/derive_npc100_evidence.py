"""Re-use unchanged workbench families, changing only explicit batch metadata and samples."""
import re
from prepare_npc100 import ROOT
BASE='E3441D47BA2C3E9D6ADC745B7BBA3A1F602DC7E4E61719370FAA0BADEEEEAB6A'
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def version(s):return s.replace('0.15.99-dev','0.15.100-dev').replace('0.15.98-dev','0.15.99-dev').replace('76E85EF50B3308A390D5C11302198A16EAFD1C7CA2BE08F4AE46530C16718A50',BASE)
s=(ROOT/'tests/BenchmarkNpc99.java').read_text().replace('BenchmarkNpc99','BenchmarkNpc100').replace('new_npc99','new_npc100').replace('new_choices99','new_choices100').replace('expansion99','expansion100').replace('New99','New100')
s=re.sub(r'inputs.put\("new_npc100_unequal_work",List.of\(.*?\)\);','inputs.put("new_npc100_unequal_work",List.of("§e[NPC] §bAlan§f: Greetings, adventurer!","§e[NPC] Alatar§f: §eTime§f flows differently in this §ddimension§f.","§e[NPC] Walter§f: §b✆ §fCall me anytime!","§e[NPC] §6Scardius§f: It actually seemed to be quite effective!"));',s)
s=re.sub(r'inputs.put\("new_choices100_unequal_work",List.of\(.*?\)\);','inputs.put("new_choices100_unequal_work",List.of("Select an option: [No]","Select an option: [Trade] [No thanks]","Select an option: [I lost my Golden Belt.]","Select an option: [Buy tip]"));',s)
s=s.replace('modes98/99','modes99/100').replace('translate only on99','translate only on100')
emit('tests/BenchmarkNpc100.java',s)
emit('tests/BenchmarkNpc100Focused.java',(ROOT/'tests/BenchmarkNpc99Focused.java').read_text().replace('BenchmarkNpc99Focused','BenchmarkNpc100Focused'))
for suffix in ('','_focused'):
 s=version((ROOT/f'tools/benchmark_npc99{suffix}.ps1').read_text()).replace('npc99-benchmark','npc100-benchmark').replace('npc99-focused','npc100-focused').replace('BenchmarkNpc99','BenchmarkNpc100').replace('NPC99','NPC100').replace('98,99,99,98','99,100,100,99').replace('Wrong98','Wrong99')
 emit(f'tools/benchmark_npc100{suffix}.ps1',s)
s=version((ROOT/'tools/audit_npc99.py').read_text()).replace('from prepare_npc99','from prepare_npc100').replace('NPC_TRANSLATION_BATCH_2026-10-19_1','NPC_TRANSLATION_BATCH_2026-10-19_2').replace("CHAT_OLD=OLD.parent/'NPC_TRANSLATION_BATCH_2026-10-18_6'","CHAT_OLD=OLD").replace('npc-expansion99','npc-expansion100').replace('NPC99-SOURCES','NPC100-SOURCES').replace('len(new)==397','len(new)==382').replace("'new_exact_outputs_flat_and_published':397","'new_exact_outputs_flat_and_published':382").replace("'old_output_rows_unchanged':10850","'old_output_rows_unchanged':10865")
# Both complete source snapshots are now in the same immutable baseline delivery.
s=re.sub(r" with zipfile.ZipFile\(ROOT/'build/libs/WitherSkyRU-26\.1\.2-fabric-0\.15\.93-dev.jar'\).*?\n",'',s)
start=s.index(" rev=load(SOURCE/'NPC_0.15.99_PUBLIC_REVISION.json')")
end=s.index(' result=',start)
s=s[:start]+" rev=load(SOURCE/'NPC_0.15.100_PUBLIC_REVISION.json');assert rev['matches_pinned_corpus'] and rev['expected']==rev['actual']\n"+s[end:]
emit('tools/audit_npc100.py',s)
print('Derived same-work workbenches and source/class/bytecode/output audit for99 ->100.')
