"""Mechanical clones of the accepted harnesses: only names, mode selection and sampling length differ."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / 'tests/BenchmarkPrepared72.java').read_text(encoding='utf-8-sig')
source = source.replace('BenchmarkPrepared72', 'BenchmarkHudFocus95')
source = source.replace('i<20000', 'i<50000').replace('base.equals("dynamic_counter")?20000:4000', '20000')
old = '"stable_hud","stable_outside_fresh_context","dynamic_counter","fresh_colors_actions","fragmented16","over_limit_uncached","stable_hud_consume","dynamic_counter_consume","fresh_colors_actions_consume","fragmented16_consume"'
assert source.count(old) == 1
source = source.replace(old, '"stable_hud_consume","stable_outside_fresh_context","dynamic_counter_consume","fresh_colors_actions_consume","fragmented16_consume"')
start = source.index('        out.addProperty("scope",')
end = source.index('\n', start)
source = source[:start] + '        out.addProperty("scope","Focused5 equal-work HUD modes;50000 warm batches,4x20000 batches of10 calls each;fixed caller for direct HUD modes,outside includes caller inspection;CPU counters coarse;not game FPS or native rendering.");' + source[end:]
(root / 'tests/BenchmarkHudFocus95.java').write_text(source, encoding='utf-8')
source = (root / 'tests/BenchmarkOptimization94.java').read_text(encoding='utf-8-sig').replace('BenchmarkOptimization94', 'BenchmarkReceiptFocus95')
old = 'Set.of("unknown","existing_style","existing_choices","existing_choices84","existing_choices91","existing_choices92")'
assert source.count(old) == 1
source = source.replace(old, 'Set.of("unknown","existing_style","existing_extras89","existing_choices84","existing_choices93","existing_large_batch90")')
(root / 'tests/BenchmarkReceiptFocus95.java').write_text(source, encoding='utf-8')
