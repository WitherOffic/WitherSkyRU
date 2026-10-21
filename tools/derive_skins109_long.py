"""Predeclare repeat after initial forward pair: one noisy dynamic plus two SYSTEM modes."""
import re
from prepare_npc78 import ROOT,SOURCE,write
def main():
    modes=['existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications']
    s=(ROOT/'tests/BenchmarkSkins109.java').read_text(encoding='utf-8-sig').replace('BenchmarkSkins109','BenchmarkSkins109Long')
    replacement='if(focused)inputs.keySet().retainAll(Set.of('+','.join('"'+m+'"' for m in modes)+'));'
    s,n=re.subn(r'if\(focused\)inputs.keySet\(\).retainAll\(Set.of\([^;]+\)\);',replacement,s);assert n==1
    s=s.replace('focused?300000','focused?1000000').replace('focused?200000','focused?1000000').replace('Focused15 equal-work modes;300000 warmup/four200000-call rounds','Predeclared3 equal-work modes;1000000 warmup/four1000000-call rounds')
    (ROOT/'tests/BenchmarkSkins109Long.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_skins109.ps1').read_text(encoding='utf-8-sig').replace('BenchmarkSkins109','BenchmarkSkins109Long').replace('skins109-verify-','skins109-long-').replace('fifteen equal-work modes,longer warmup/repeated rounds','three predeclared equal-work modes,1000000 warmup/four1000000-call rounds')
    (ROOT/'tools/benchmark_skins109_long.ps1').write_text(s,encoding='utf-8')
    write(SOURCE/'SKINS_PERFORMANCE_REPEAT_0.15.109.json',{'trigger':'Forward pair existing_dynamic+12.86%;other forward modes<=1.91%. Modes selected before inspecting reverse-pair results,not a selected best run.','modes':modes,'warmup_calls_at_least':1000000,'measured_rounds':4,'calls_per_round_at_least':1000000,'order':['108','109','109','108'],'retain_original_raw':True,'scope':'Receipt only,not render/FPS/frametime/0.1% lows.'})
if __name__=='__main__':main()
