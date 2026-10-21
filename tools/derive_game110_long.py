"""Predeclare a longer control before any110 benchmark results are inspected."""
from prepare_npc78 import ROOT,SOURCE,write

def main():
    s=(ROOT/'tests/BenchmarkSkins109Long.java').read_text(encoding='utf-8-sig').replace('BenchmarkSkins109Long','BenchmarkGame110Long');(ROOT/'tests/BenchmarkGame110Long.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_skins109_long.ps1').read_text(encoding='utf-8-sig').replace('0.15.109-dev','0.15.110-dev').replace('0.15.108-dev','0.15.109-dev').replace('skins109-long-','game110-long-').replace('BenchmarkSkins109Long','BenchmarkGame110Long').replace('SKINS109','GAME110').replace('JVMs108,109,109,108','JVMs109,110,110,109');(ROOT/'tools/benchmark_game110_long.ps1').write_text(s,encoding='utf-8')
    write(SOURCE/'GAME_PERFORMANCE_CONTROL_0.15.110.json',{'selection':'Predeclared before inspecting any110 performance results. Same three long-control modes as109,not a selected best run.','modes':['existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications'],'warmup_calls_at_least':1000000,'measured_rounds':4,'calls_per_round_at_least':1000000,'order':['109','110','110','109'],'retain_original_raw':True,'scope':'Receipt only,not render/FPS/frametime/0.1% lows.'})
if __name__=='__main__':main()
