"""Predeclared longer repeat of one noisy and two near-threshold equal-work modes."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tests/BenchmarkNpc106Verify.java').read_text(encoding='utf-8-sig')
    s=s.replace('BenchmarkNpc106Verify','BenchmarkNpc106Long')
    old='Set.of("unknown","existing_style","existing_choices87","existing_choices88","existing_large_batch88","existing_dynamic","unknown_named_speaker","existing_large_batch87","existing_choices90","existing_npc102_equal_work")'
    assert s.count(old)==1
    s=s.replace(old,'Set.of("existing_choices87","existing_style","existing_dynamic")')
    s=s.replace('focused?300000:200000','focused?1000000:200000').replace('focused?200000:40000','focused?1000000:40000')
    s=s.replace('Focused10 equal-work modes;300000 warmup/four200000-call rounds','Focused3 equal-work modes;1000000 warmup/four1000000-call rounds')
    (ROOT/'tests/BenchmarkNpc106Long.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_npc106_verify.ps1').read_text(encoding='utf-8-sig')
    s=s.replace('npc106-verify-','npc106-long-').replace('BenchmarkNpc106Verify','BenchmarkNpc106Long').replace('ten equal-work modes,longer warmup/repeated rounds','three equal-work modes;1000000 warmup/four1000000-call rounds')
    (ROOT/'tools/benchmark_npc106_long.ps1').write_text(s,encoding='utf-8')

if __name__=='__main__':main()
