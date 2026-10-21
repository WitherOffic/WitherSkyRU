"""Long repeated HUD samples for the noisy fresh-colors/actions mode; runtime unchanged."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tests/BenchmarkPrepared72.java').read_text(encoding='utf-8-sig').replace('BenchmarkPrepared72','BenchmarkHud104Verify')
    old='List.of("stable_hud","stable_outside_fresh_context","dynamic_counter","fresh_colors_actions","fragmented16","over_limit_uncached","stable_hud_consume","dynamic_counter_consume","fresh_colors_actions_consume","fragmented16_consume")'
    assert old in s;s=s.replace(old,'List.of("fresh_colors_actions","fresh_colors_actions_consume")')
    assert 'i<20000;i++' in s and 'int batches=base.equals("dynamic_counter")?20000:4000;' in s
    s=s.replace('i<20000;i++','i<200000;i++').replace('int batches=base.equals("dynamic_counter")?20000:4000;','int batches=200000;')
    start=s.index('        out.addProperty("scope",');end=s.index('\n',start)
    s=s[:start]+'        out.addProperty("scope","Two same-work fresh-color/action HUD modes;2 million warmup operations and four2-million-operation rounds/mode. Source construction/consumption identical on103/104. Not game FPS or full-frame timing.");'+s[end:]
    (ROOT/'tests/BenchmarkHud104Verify.java').write_text(s,encoding='utf-8')
    print('Long HUD harness:2 modes,2million warmup/four2million-operation rounds each.')

if __name__=='__main__':main()
