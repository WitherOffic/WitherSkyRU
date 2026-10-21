"""Receipt probe,identical input on both versions;no changes to historical tests."""
import argparse,json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write
from coverage_expansion111 import adjust,journal
def snapshots(selected=False):
    s=(ROOT/'tests/AuditGame110.java').read_text(encoding='utf-8-sig').replace('AuditGame110','AuditExpansion111').replace('GAME_SELECTED_0.15.110','EXPANSION_PROBE_0.15.111').replace('GAME110','EXPANSION111');(ROOT/'tests/AuditExpansion111.java').write_text(s,encoding='utf-8')
    if selected:(ROOT/'tests/AuditExpansion111.java').write_text(s.replace('EXPANSION_PROBE_0.15.111','EXPANSION_SELECTED_0.15.111'),encoding='utf-8')
    s=(ROOT/'tools/snapshot_game110.ps1').read_text(encoding='utf-8-sig').replace('0.15.109-dev','0.15.110-dev').replace('game110-snapshots','expansion111-snapshots').replace('AuditGame110','AuditExpansion111').replace('GAME110','EXPANSION111');(ROOT/'tools/snapshot_expansion111.ps1').write_text(s,encoding='utf-8')
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--probe-only',action='store_true');ap.add_argument('--test-only',action='store_true');a=ap.parse_args()
    if a.probe_only:snapshots();return
    snapshots(True)
    s=(ROOT/'tests/TestGame110.java').read_text(encoding='utf-8-sig').replace('TestGame110','TestExpansion111').replace('GAME110','EXPANSION111').replace('GAME_CASES_0.15.110','EXPANSION_CASES_0.15.111').replace('story.phraseCount()==1492','story.phraseCount()==1861').replace('952 retained plus540 exact gameplay messages','1492 retained plus369 system messages').replace('count==1081','count==1011').replace('540 messages,each primary and all publication layouts','502 messages,each primary and all publication layouts').replace('540 gameplay messages;1081 primary/published fixtures','502 full messages;1011 primary/published fixtures').replace('individually authored110','individually authored111')
    marker='    private static int checks;';assert s.count(marker)==1
    s=s.replace(marker,marker+'\n    private static int prefix(String value){int p=value.startsWith("[NPC] ")?value.indexOf(": ")+2:0;return value.startsWith("✆ ",p)?p+2:p;}')
    s=s.replace('int prefix=0;','int prefix=prefix(text);').replace('int prefix=source.startsWith("✆ ")?2:0;','int prefix=prefix(source);')
    # Existing NPC providers deliberately do not accept arbitrary leading spaces.
    # Exercise those as negative cases rather than broaden the production parser.
    marker='var output=queued(input);check(plain(output).equals(time+target+suffix)';assert s.count(marker)==1
    s=s.replace(marker,'var output=queued(input);if(source.startsWith("[NPC] ")&&!List.of("","[04:05:06] ").contains(time)){check(output==input,"unsupported NPC padding intentionally declined");check(plain(input).equals(time+source+suffix),"negative source immutable");continue;}check(plain(output).equals(time+target+suffix)')
    marker='flag("storyDialogues",NpcDialogueTranslator.storyDialogues());';assert s.count(marker)==1
    s=s.replace(marker,'check(NpcDialogueTranslator.storyDialogues().phraseCount()==10761,"10628 retained plus133 NPC literals");'+marker)
    s=s.replace('check(queued(input)==input,"game notification switch")','check(source.startsWith("[NPC] ")?plain(queued(input)).equals(target):queued(input)==input,"tag-scoped game switch")').replace('check(plain(queued(input)).equals(target),"system notification independent of NPC switch")','check(source.startsWith("[NPC] ")?queued(input)==input:plain(queued(input)).equals(target),"tag-scoped NPC switch")')
    marker='check(queued(input)==input,"alternate semantic override wins: "+term)';assert s.count(marker)==1
    s=s.replace(marker,'check(term.startsWith("#")?plain(queued(input)).equals(target):queued(input)==input,"TSV comment keys are not overrides;recordable semantic overrides win: "+term)')
    (ROOT/'tests/TestExpansion111.java').write_text(s,encoding='utf-8')
    if a.test_only:print('Prepared new mixed test only;not a final full-suite result');return
    journal();dataset=ROOT/'translation-source-regression111-v5';dataset.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    prior=ROOT/'translation-source-regression110-v2';sup=json.loads((prior/'EXPECTATION_SUPERSESSION105.json').read_text(encoding='utf-8'))
    for r in sup['changed_files']:shutil.copy2(prior/r['file'],dataset/r['file'])
    shutil.copy2(prior/'EXPECTATION_SUPERSESSION105.json',dataset/'EXPECTATION_SUPERSESSION105.json')
    generated=ROOT/'build/expansion111-test-sources-v5';generated.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        old=p.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)(10525|10628)\b','10761',old)
        patterns={'TestNpc98.java':('phraseCount()==358','phraseCount()==460'),'TestDungeonChatResources.java':('entries.size()+336','entries.size()+438'),'TestSystem107.java':('phraseCount()==148','phraseCount()==1861'),'TestSkins109.java':('phraseCount()==952','phraseCount()==1861'),'TestGame110.java':('phraseCount()==1492','phraseCount()==1861')}
        if p.name in patterns:
            a,b=patterns[p.name];assert new.count(a)==1;new=new.replace(a,b);n+=1
        new=adjust(p.name,new)
        (generated/p.name).write_text(new,encoding='utf-8')
        if n:changes.append({'file':p.name,'replacements':n,'source_sha256':sha(p),'generated_sha256':sha(generated/p.name)})
    assert len(changes)==35;write(generated/'COUNT_EXPECTATION_UPDATES.json',{'scope':'35 count updates;story10761,SYSTEM1861. Six copies additionally contain31 explicit negative-coverage upgrades documented in EXPANSION_COVERAGE_ADDITIONS_0.15.111.json. Historical positive translation/style goldens are not rewritten.','files':changes})
    s=(ROOT/'tools/check_game110.ps1').read_text(encoding='utf-8-sig').replace('0.15.110-dev','0.15.111-dev').replace('game110-checks-','expansion111-checks-').replace('GAME110','EXPANSION111').replace('TestNpcCorpusParser110','TestNpcCorpusParser111').replace('build/game110-test-sources-v2','build/expansion111-test-sources').replace('translation-source-regression110-v2','translation-source-regression111').replace("@('TestGame110','TestSkins109','TestBoss108','TestSystem107')","@('TestExpansion111','TestGame110','TestSkins109','TestBoss108','TestSystem107')").replace('-ne 183','-ne 184').replace('/183','/184');(ROOT/'tools/check_expansion111.ps1').write_text(s,encoding='utf-8')
    s=s.replace('build/expansion111-test-sources','build/expansion111-test-sources-v5').replace('translation-source-regression111','translation-source-regression111-v5');(ROOT/'tools/check_expansion111.ps1').write_text(s,encoding='utf-8')
    for kind in ('dispatch','benchmark'):
        s=(ROOT/f'tools/{kind}_game110.ps1').read_text(encoding='utf-8-sig').replace('0.15.110-dev','0.15.111-dev').replace('0.15.109-dev','0.15.110-dev').replace('game110-','expansion111-').replace('BenchmarkGame110','BenchmarkExpansion111').replace('GAME110','EXPANSION111').replace('JVMs109,110,110,109','JVMs110,111,111,110');(ROOT/f'tools/{kind}_expansion111.ps1').write_text(s,encoding='utf-8')
    for kind in ('','Long'):
        s=(ROOT/f'tests/BenchmarkGame110{kind}.java').read_text(encoding='utf-8-sig').replace('BenchmarkGame110','BenchmarkExpansion111');(ROOT/f'tests/BenchmarkExpansion111{kind}.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_game110_long.ps1').read_text(encoding='utf-8-sig').replace('0.15.110-dev','0.15.111-dev').replace('0.15.109-dev','0.15.110-dev').replace('game110-long-','expansion111-long-').replace('BenchmarkGame110Long','BenchmarkExpansion111Long').replace('GAME110','EXPANSION111').replace('JVMs109,110,110,109','JVMs110,111,111,110');(ROOT/'tools/benchmark_expansion111_long.ps1').write_text(s,encoding='utf-8')
    write(SOURCE/'EXPANSION_PERFORMANCE_CONTROL_0.15.111.json',{'selection':'Same15 equal-output controls and same3 long controls as110,declared before111 results.','long_modes':['existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications'],'order':['110','111','111','110'],'long_warmup_at_least':1000000,'long_rounds':4,'long_calls_per_round_at_least':1000000,'scope':'Receipt only,not FPS/frametimes/0.1% lows.','retain_all_raw':True})
    print('184 fresh groups;1011 new fixtures;35 count updates plus31 explicit literal/archive coverage upgrades in6 isolated copies')
if __name__=='__main__':main()
