"""Mechanical harness derivation. Historical sources and delivered111 artifacts stay immutable."""
import argparse,json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write
from prepare_expansion112 import load,BASE

def adjust_coverage(text):
    records={r['id']:r for r in load(SOURCE/'EXPANSION_SELECTED_0.15.112.json')['records']};selected=[records[k] for k in ('13387','13391')]
    old=load(SOURCE/'NPC_SOURCES_0.15.89.json')['pending_variables']
    assert all(any(x['source']==r['source'] for x in old) and not r['published_row']['variable_definition'] for r in selected)
    body='\n    private static String newlyReviewed112(String source){return switch(source){\n'+''.join('        case '+json.dumps(r['source'],ensure_ascii=False)+' -> '+json.dumps(r['translation'],ensure_ascii=False)+';\n' for r in selected)+'        default -> null;\n    };}\n'
    marker='    private static int checks;';assert text.count(marker)==1;text=text.replace(marker,marker+body)
    old='check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"placeholder never faked as literal coverage");'
    new='check(!packaged.containsKey(source)&&(newlyReviewed112(source)==null?story.translate(literal,db)==literal:plain(story.translate(literal,db)).equals(newlyReviewed112(source))),"112 two fixed pet-level literals;all other pending variables retained");'
    assert text.count(old)==1
    return text.replace(old,new)

def checks():
    s=(ROOT/'tests/TestExpansion111.java').read_text(encoding='utf-8-sig').replace('TestExpansion111','TestExpansion112').replace('EXPANSION111','EXPANSION112').replace('0.15.111.json','0.15.112.json').replace('phraseCount()==1861','phraseCount()==2341').replace('1492 retained plus369 system messages','1861 retained plus480 system messages').replace('phraseCount()==10761','phraseCount()==10763').replace('10628 retained plus133 NPC literals','10761 retained plus2 NPC literals').replace('count==1011','count==989').replace('502 messages','482 messages').replace('502 full messages;1011','482 full messages;989').replace('individually authored111','individually authored112')
    (ROOT/'tests/TestExpansion112.java').write_text(s,encoding='utf-8')
    dataset=ROOT/'translation-source-regression112';dataset.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    previous=ROOT/'translation-source-regression111-v5';sup=load(previous/'EXPECTATION_SUPERSESSION105.json')
    for r in sup['changed_files']:shutil.copy2(previous/r['file'],dataset/r['file'])
    shutil.copy2(previous/'EXPECTATION_SUPERSESSION105.json',dataset/'EXPECTATION_SUPERSESSION105.json')
    generated=ROOT/'build/expansion112-test-sources';generated.mkdir(exist_ok=False);changed=[]
    for p in (ROOT/'tests').glob('*.java'):
        prior=ROOT/'build/expansion111-test-sources-v5'/p.name;prior=prior if prior.exists() else p;old=prior.read_text(encoding='utf-8-sig')
        new,n=re.subn(r'(?<=phraseCount\(\)==)(10761|1861)\b',lambda m:'10763' if m[0]=='10761' else '2341',old)
        if p.name=='TestNpc89.java':new=adjust_coverage(new)
        (generated/p.name).write_text(new,encoding='utf-8')
        if new!=p.read_text(encoding='utf-8-sig'):changed.append({'file':p.name,'original_sha256':sha(p),'baseline_derived_path':prior.relative_to(ROOT).as_posix(),'baseline_derived_sha256':sha(prior),'generated_sha256':sha(generated/p.name),'count_replacements_from111':n,'new_coverage_upgrade':p.name=='TestNpc89.java'})
    write(generated/'EXPECTATION_UPDATES.json',{'scope':'Retains prior111 isolated count/coverage edits and105 editorial supersessions. Only current cardinalities and two fixed [Lvl1] pet literals upgraded;all original Java and positive style/translation goldens unchanged.','files':changed})
    selected={r['id']:r for r in load(SOURCE/'EXPANSION_SELECTED_0.15.112.json')['records']}
    write(SOURCE/'EXPANSION_COVERAGE_ADDITIONS_0.15.112.json',{'scope':'Two fixed [Lvl1] Tarantula requests are real complete publications,not variable definitions. Only these two old negative cases upgraded in an isolated89 test copy;all other negatives retained.','cases':[selected[k] for k in ('13387','13391')],'baseline_jar_sha256':BASE})
    s=(ROOT/'tools/check_expansion111.ps1').read_text(encoding='utf-8-sig').replace('0.15.111-dev','0.15.112-dev').replace('expansion111-checks','expansion112-checks').replace('expansion111-test-sources-v5','expansion112-test-sources').replace('translation-source-regression111-v5','translation-source-regression112').replace('TestNpcCorpusParser111','TestNpcCorpusParser112').replace('EXPANSION111','EXPANSION112').replace("@('TestExpansion111'","@('TestExpansion112','TestExpansion111'").replace('-ne 184','-ne 185').replace('/184','/185').replace('COUNT_EXPECTATION_UPDATES.json','EXPECTATION_UPDATES.json')
    (ROOT/'tools/check_expansion112.ps1').write_text(s,encoding='utf-8')
    for kind in ('dispatch','benchmark','benchmark_long'):
        old_name={'dispatch':'dispatch_expansion111.ps1','benchmark':'benchmark_expansion111.ps1','benchmark_long':'benchmark_expansion111_long.ps1'}[kind]
        text=(ROOT/'tools'/old_name).read_text(encoding='utf-8-sig').replace('0.15.111-dev','0.15.112-dev').replace('0.15.110-dev','0.15.111-dev').replace('expansion111-','expansion112-').replace('BenchmarkExpansion111','BenchmarkExpansion112').replace('EXPANSION111','EXPANSION112').replace('JVMs110,111,111,110','JVMs111,112,112,111')
        new_name={'dispatch':'dispatch_expansion112.ps1','benchmark':'benchmark_expansion112.ps1','benchmark_long':'benchmark_expansion112_long.ps1'}[kind];(ROOT/'tools'/new_name).write_text(text,encoding='utf-8')
    for suffix in ('','Long'):
        text=(ROOT/f'tests/BenchmarkExpansion111{suffix}.java').read_text(encoding='utf-8-sig').replace('BenchmarkExpansion111','BenchmarkExpansion112')
        (ROOT/f'tests/BenchmarkExpansion112{suffix}.java').write_text(text,encoding='utf-8')
    write(SOURCE/'EXPANSION_PERFORMANCE_CONTROL_0.15.112.json',{'selection':'Same15 short and3 long equal-work controls as111;declared before112 performance results. All raw retained;not game FPS.','long_modes':['existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications'],'order':['111','112','112','111'],'short_warmup':300000,'short_rounds':4,'short_calls_per_round_at_least':200000,'long_warmup':1000000,'long_rounds':4,'long_calls_per_round_at_least':1000000})
    print('185 fresh groups;989 new fixtures;',len(changed),'isolated files with prior/current cardinality edits;one also adds two explicit coverage upgrades')

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--selected',action='store_true');ap.add_argument('--checks',action='store_true');a=ap.parse_args()
    text=(ROOT/'tests/AuditExpansion111.java').read_text(encoding='utf-8-sig').replace('AuditExpansion111','AuditExpansion112').replace('EXPANSION111','EXPANSION112').replace('EXPANSION_SELECTED_0.15.111','EXPANSION_SELECTED_0.15.112' if a.selected else 'EXPANSION_PROBE_0.15.112')
    (ROOT/'tests/AuditExpansion112.java').write_text(text,encoding='utf-8')
    text=(ROOT/'tools/snapshot_expansion111.ps1').read_text(encoding='utf-8-sig').replace('0.15.110-dev','0.15.111-dev').replace('expansion111-snapshots','expansion112-snapshots').replace('AuditExpansion111','AuditExpansion112').replace('EXPANSION111','EXPANSION112')
    (ROOT/'tools/snapshot_expansion112.ps1').write_text(text,encoding='utf-8')
    if a.checks:checks()

if __name__=='__main__':main()
