"""Same-work benchmark families and immutable100-vs101 output/bytecode audit."""
import re
from prepare_npc101 import ROOT
BASE='1FE0127216BD7B39D4F41DEB976F70F25E6DBB1FA2B23B8B32499FDD856B7117'
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def versions(s):return s.replace('0.15.100-dev','0.15.101-dev').replace('0.15.99-dev','0.15.100-dev').replace('E3441D47BA2C3E9D6ADC745B7BBA3A1F602DC7E4E61719370FAA0BADEEEEAB6A',BASE)
s=(ROOT/'tests/BenchmarkNpc100.java').read_text().replace('BenchmarkNpc100','BenchmarkNpc101').replace('new_npc100','new_npc101').replace('new_choices100','new_choices101').replace('expansion100','expansion101').replace('New100','New101')
s=re.sub(r'inputs.put\("new_npc101_unequal_work",List.of\(.*?\)\);','inputs.put("new_npc101_unequal_work",List.of("§e[NPC] §dAgnes§f: Welcome to the §5Year of the Witch§f!","§e[NPC] §eHunter Jasper§f: §fThat little ball right here is actually a §9Scrappy§f.","§e[NPC] §5Maddox§f: §b✆ §fAbi...phone?","§e[NPC] §6Sunny§f: §fMeow"));',s)
s=re.sub(r'inputs.put\("new_choices101_unequal_work",List.of\(.*?\)\);','inputs.put("new_choices101_unequal_work",List.of("Select an option: [Okay, I\\\'m listening...]","Select an option: [I\\\'m here to cause trouble, of course] [I have a letter of recommendation]","Select an option: [I\\\'m sorry but who are you?]","Select an option: [I just want the Timecharm and I\\\'m out.]"));',s)
s=s.replace('modes99/100','modes100/101').replace('translate only on100','translate only on101')
emit('tests/BenchmarkNpc101.java',s)
emit('tests/BenchmarkNpc101Focused.java',(ROOT/'tests/BenchmarkNpc100Focused.java').read_text().replace('BenchmarkNpc100Focused','BenchmarkNpc101Focused'))
for suffix in ('','_focused'):
 s=versions((ROOT/f'tools/benchmark_npc100{suffix}.ps1').read_text()).replace('npc100-benchmark','npc101-benchmark').replace('npc100-focused','npc101-focused').replace('BenchmarkNpc100','BenchmarkNpc101').replace('NPC100','NPC101').replace('99,100,100,99','100,101,101,100').replace('Wrong99','Wrong100')
 emit(f'tools/benchmark_npc101{suffix}.ps1',s)
s=versions((ROOT/'tools/audit_npc100.py').read_text()).replace('from prepare_npc100','from prepare_npc101').replace('NPC_TRANSLATION_BATCH_2026-10-19_2','NPC_TRANSLATION_BATCH_2026-10-19_3').replace('npc-expansion100','npc-expansion101').replace('NPC100-SOURCES','NPC101-SOURCES').replace('len(new)==382','len(new)==385').replace('len(current)==11247','len(current)==11248').replace("'new_exact_outputs_flat_and_published':382","'new_exact_outputs_flat_and_published':385").replace("'old_output_rows_unchanged':10865","'old_output_rows_unchanged':10863")
needle=" rev=load(SOURCE/'NPC_0.15.100_PUBLIC_REVISION.json');assert rev['matches_pinned_corpus'] and rev['expected']==rev['actual']"
assert needle in s
s=s.replace(needle," rev=load(SOURCE/'NPC_0.15.101_PUBLIC_REVISION.json');delta=load(SOURCE/'NPC_0.15.101_REVISION_DELTA.json');assert delta['accepted_for_batch101']\n assert {k:(rev['expected'][k],v) for k,v in rev['actual'].items() if rev['expected'].get(k)!=v}=={r['page']:(r['pinned_revision'],r['current_revision']) for r in delta['records']}\n assert all(r['entire_dialogue_section_including_conditions_unchanged'] and r['all_dialogue_templates_unchanged'] for r in delta['records'])")
emit('tools/audit_npc101.py',s)
print('Derived100 ->101 evidence. Added one specifically reviewed literal to OFFLINE audit only.')
