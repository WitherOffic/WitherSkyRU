"""Extend equal-work92 receipt benchmark with43 source-backed NPC samples and11 new choices."""
import json
from prepare_npc93 import ROOT,SOURCE,SPEAKERS,selection
from derive_npc93_checks import next_names
def main():
 text=next_names((ROOT/'tests/BenchmarkNpc92.java').read_text()).replace('expansion93','expansion92')
 text=text.replace('new_large_batch92','existing_large_batch92').replace('new_choices92','existing_choices92')
 q,k,s,e,o,b=selection();samples=[next(r['source_legacy_line'] for r in e if r['speaker']==name) for name in SPEAKERS]
 choices=[json.loads(l.split('|')[0]) for l in (SOURCE/'NPC_CHOICES_0.15.93_REVIEWED.tsv').read_text().splitlines() if l and not l.startswith('#')]
 value=lambda arr:'List.of('+','.join(json.dumps(x,ensure_ascii=False) for x in arr)+')'
 addition='        inputs.put("new_large_batch93",'+value(samples)+');\n        inputs.put("new_choices93",'+value(['Select an option: '+' '.join('['+x+']' for x in labels) for labels in choices])+');\n'
 addition+='        boolean expansion93=BenchmarkNpc93.class.getResource("/data/witherskyru/npc-expansion93.json")!=null;\n        for(String mode:List.of("new_large_batch93","new_choices93"))for(String source:inputs.get(mode)){var output=SkyRuClient.npcChat(new GuiMessage(0,Component.literal(source),null,GuiMessageSource.SYSTEM_SERVER,null)).content();if((!output.getString().equals(source))!=expansion93)throw new AssertionError("New93 useful-output expectation: "+source);}\n'
 marker='        var bean=(com.sun.management.ThreadMXBean)';assert marker in text;text=text.replace(marker,addition+marker)
 # Resource92 remains the old-feature availability check in both JARs.
 text=text.replace('boolean expansion92=BenchmarkNpc93.class.getResource("/data/witherskyru/npc-expansion93.json")','boolean expansion92=BenchmarkNpc93.class.getResource("/data/witherskyru/npc-expansion92.json")')
 text=text.replace('31 modes','33 modes').replace('New92 literals/choices','New93 literals/choices').replace('All29 prior91 modes','All31 prior92 modes')
 marker='        for(var entry:inputs.entrySet())'
 text=text.replace(marker,'        boolean focused=args.length>2; if(focused)inputs.keySet().retainAll(Set.of("unknown","existing_style","existing_choices","existing_choices84","existing_choices91","existing_choices92"));\n'+marker)
 text=text.replace('(200000+rows.size()-1)','((focused?300000:200000)+rows.size()-1)').replace('(40000+rows.size()-1)','((focused?200000:40000)+rows.size()-1)')
 text=text.replace('out.addProperty("scope","Chat receipt','out.addProperty("scope",focused?"Focused6 equal-work modes;300000 warmup/four200000-call rounds;not FPS":"Chat receipt')
 (ROOT/'tests/BenchmarkNpc93.java').write_text(text,encoding='utf-8')
 script=next_names((ROOT/'tools/benchmark_npc92.ps1').read_text()).replace('0.15.91-dev','0.15.92-dev').replace('DECE73F17BEE06AE1D4318F3F85AC8908DBEEEEAFA108E53FC509C9FD856899D','2BF484EC13F14B7080AF9AE4B60A4FCDE37B65B38384CF70F66DA85EA9450304').replace('Wrong91 baseline','Wrong92 baseline').replace('in91,92,92,91 order','in92,93,93,92 order')
 script=script.replace("'new_large_batch92','new_choices92'","'existing_large_batch92','existing_choices92','new_large_batch93','new_choices93'")
 (ROOT/'tools/benchmark_npc93.ps1').write_text(script,encoding='utf-8')
 focused=script.replace('npc93-benchmark-','npc93-focused-').replace('BenchmarkNpc93 $taskFile "$taskRoot/isolated-$taskIndex"','BenchmarkNpc93 $taskFile "$taskRoot/isolated-$taskIndex" focus')
 begin=focused.index('$taskSummary=foreach(');end=focused.index('){',begin)
 focused=focused[:begin]+"$taskSummary=foreach($taskMode in @('unknown','existing_style','existing_choices','existing_choices84','existing_choices91','existing_choices92'))"+focused[end+1:]
 focused=focused.replace("scope='Four isolated JVMs in92,93,93,92 order;all rounds retained;chat receipt only,not game FPS or frame timing'","scope='Focused repeat after variable first run;6 equal-work modes;4 JVMs92,93,93,92;300000 warmup and4 rounds200000 calls;not game FPS'")
 (ROOT/'tools/benchmark_npc93_focused.ps1').write_text(focused,encoding='utf-8')
 print('Derived33 receipt modes;31 equal-work92 modes and2 new useful-output modes;no live FPS claim')
if __name__=='__main__':main()
