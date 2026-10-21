"""Isolated114 harnesses,only SYSTEM cardinality changes;no old positives/negatives weakened."""
import json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write
from prepare_npc113 import load

def main():
    s=(ROOT/'tests/TestExpansion112.java').read_text(encoding='utf-8-sig')
    for old,new in [('TestExpansion112','TestEggs114'),('EXPANSION112','EGGS114'),('EXPANSION_CASES_0.15.112','EGG_CASES_0.15.114'),('phraseCount()==2341','phraseCount()==3909'),('phraseCount()==10763','phraseCount()==10764'),('count==989','count==4480'),('482 messages,each primary and all publication layouts','1568 records,all primary and explicit publication-composition layouts'),('482 full messages;989 primary/published fixtures','224 locations+1344 finite envelopes;4480 primary/composition-style fixtures'),('individually authored112','authored114 locations/headers'),('1861 retained plus480 system messages','2341 retained plus1568 finite records')]:s=s.replace(old,new)
    s=s.replace('List.of("","[04:05:06] ","   ","[04:05:06]   ")','List.of("","[04:05:06] ")')
    marker='        var frozen=JsonParser.parseString('
    extra='        TestEggTargets114.verify(sourceRoot);\n'
    assert s.count(marker)==1;s=s.replace(marker,extra+marker)
    (ROOT/'tests/TestEggs114.java').write_text(s,encoding='utf-8')
    dataset=ROOT/'translation-source-regression114';dataset.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    oldroot=ROOT/'translation-source-regression113';sup=load(oldroot/'EXPECTATION_SUPERSESSION105.json')
    for r in sup['changed_files']:shutil.copy2(oldroot/r['file'],dataset/r['file'])
    shutil.copy2(oldroot/'EXPECTATION_SUPERSESSION105.json',dataset/'EXPECTATION_SUPERSESSION105.json')
    copies=ROOT/'build/eggs114-test-sources';copies.mkdir(exist_ok=False);changed=[]
    for p in (ROOT/'tests').glob('*.java'):
        prior=ROOT/'build/npc113-test-sources'/p.name;prior=prior if prior.exists() else p
        old=prior.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)2341\b','3909',old)
        (copies/p.name).write_text(new,encoding='utf-8')
        if new!=p.read_text(encoding='utf-8-sig'):changed.append({'file':p.name,'original_sha256':sha(p),'baseline_derived_path':prior.relative_to(ROOT).as_posix(),'baseline_derived_sha256':sha(prior),'generated_sha256':sha(copies/p.name),'new_system_count_replacements':n})
    write(copies/'EXPECTATION_UPDATES.json',{'scope':'Preserves all113 isolated expectation changes and105 supersession. Only SYSTEM cardinality2341→3909 changed;no new negative upgrades or altered old text/style targets.','files':changed})
    s=(ROOT/'tools/check_npc113.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('0.15.113-dev','0.15.114-dev'),('npc113-checks','eggs114-checks'),('npc113-test-sources','eggs114-test-sources'),('translation-source-regression113','translation-source-regression114'),('TestNpcCorpusParser113','TestNpcCorpusParser114'),('NPC113','EGGS114'),("@('TestNpc113'","@('TestEggs114','TestNpc113'"),('-ne 186','-ne 187'),('/186','/187')]:s=s.replace(old,new)
    (ROOT/'tools/check_eggs114.ps1').write_text(s,encoding='utf-8')
    for name in ('dispatch','benchmark','benchmark_long'):
        oldname={'dispatch':'dispatch_npc113.ps1','benchmark':'benchmark_npc113.ps1','benchmark_long':'benchmark_npc113_long.ps1'}[name]
        newname={'dispatch':'dispatch_eggs114.ps1','benchmark':'benchmark_eggs114.ps1','benchmark_long':'benchmark_eggs114_long.ps1'}[name]
        s=(ROOT/'tools'/oldname).read_text(encoding='utf-8-sig')
        for old,new in [('0.15.113-dev','0.15.114-dev'),('0.15.112-dev','0.15.113-dev'),('npc113-','eggs114-'),('BenchmarkNpc113','BenchmarkEggs114'),('NPC113','EGGS114'),('JVMs112,113,113,112','JVMs113,114,114,113')]:s=s.replace(old,new)
        (ROOT/'tools'/newname).write_text(s,encoding='utf-8')
    for suffix in ('','Long'):
        s=(ROOT/f'tests/BenchmarkNpc113{suffix}.java').read_text(encoding='utf-8-sig').replace('BenchmarkNpc113','BenchmarkEggs114')
        (ROOT/f'tests/BenchmarkEggs114{suffix}.java').write_text(s,encoding='utf-8')
    write(SOURCE/'EGG_PERFORMANCE_CONTROL_0.15.114.json',{'scope':'Same17+6 equal-output modes as113,declared before114 results. No new runtime algorithms;all raw retained,not game FPS.','short_modes':17,'long_modes':load(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.113.json')['long_modes'],'order':['113','114','114','113'],'short_warmup':300000,'short_rounds':4,'short_calls_per_round_at_least':200000,'long_warmup':1000000,'long_rounds':4,'long_calls_per_round_at_least':1000000})
    print('187 fresh groups;',len(changed),'isolated files;4480 new fixtures;17+6 equal-work modes')

if __name__=='__main__':main()
