"""Longer follow-up for the suspicious equal-work receipt modes; preserve initial results."""
from prepare_npc98 import ROOT
s=(ROOT/'tests/BenchmarkOptimization96.java').read_text().replace('BenchmarkOptimization96','BenchmarkNpc98Focused')
old='Set.of("unknown","existing_style","existing_choices","existing_choices84","existing_choices91","existing_choices92")'
new='Set.of("unknown","existing_style","existing_choices87","existing_choices88","existing_large_batch88","existing_dynamic")'
assert old in s;s=s.replace(old,new)
(ROOT/'tests/BenchmarkNpc98Focused.java').write_text(s,encoding='utf-8')
s=(ROOT/'tools/benchmark_dungeon98.ps1').read_text().replace('BenchmarkDungeon98','BenchmarkNpc98Focused').replace('npc98-dungeon-benchmark','npc98-focused').replace('NPC98 DUNGEON BENCHMARK','NPC98 FOCUSED BENCHMARK')
s=s.replace('"$taskRoot/isolated-$taskIndex" >','"$taskRoot/isolated-$taskIndex" focused >')
s=s.replace('three equal-work modes,one unequal-work new translation','six equal-work modes,longer warmup/repeated rounds')
(ROOT/'tools/benchmark_npc98_focused.ps1').write_text(s,encoding='utf-8')
