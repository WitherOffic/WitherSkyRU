"""Mechanical isolated harness derivation;112 deliveries and historical Java stay immutable."""
import json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write
from prepare_npc113 import load,BASE

def main():
    evidence=load(SOURCE/'NPC_STRUCTURED_SOURCES_0.15.113.json');literal=evidence['literal_records'][0]
    selected=[]
    for r in evidence['records']:
        selected.append({k:r[k] for k in ('source','probe_source','layouts')})
    selected.append({'source':literal['source'],'probe_source':literal['source'],'layouts':literal['layouts']})
    write(SOURCE/'NPC_RUNTIME_CASES_0.15.113.json',{'records':selected})
    p=ROOT/'tests/AuditNpc113.java';s=p.read_text(encoding='utf-8-sig');assert s.count('NPC_PROBE_0.15.113')==1;p.write_text(s.replace('NPC_PROBE_0.15.113','NPC_RUNTIME_CASES_0.15.113'),encoding='utf-8')
    dataset=ROOT/'translation-source-regression113';dataset.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    oldroot=ROOT/'translation-source-regression112-v2';sup=load(oldroot/'EXPECTATION_SUPERSESSION105.json')
    for r in sup['changed_files']:shutil.copy2(oldroot/r['file'],dataset/r['file'])
    shutil.copy2(oldroot/'EXPECTATION_SUPERSESSION105.json',dataset/'EXPECTATION_SUPERSESSION105.json')
    copies=ROOT/'build/npc113-test-sources';copies.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        prior=ROOT/'build/expansion112-test-sources'/p.name;prior=prior if prior.exists() else p
        old=prior.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)10763\b','10764',old)
        if p.name=='TestNpc86.java':
            before='check(!packaged.containsKey(source)&&story.translate(literal,db)==literal,"placeholder never faked as literal coverage");'
            after='check(!packaged.containsKey(source)&&(source.equals('+json.dumps(literal['source'],ensure_ascii=False)+')?plain(story.translate(literal,db)).equals('+json.dumps(literal['translation'],ensure_ascii=False)+'):story.translate(literal,db)==literal),"113 fixed Duke X literal reviewed;all actual placeholders remain excluded");'
            assert new.count(before)==1;new=new.replace(before,after)
        (copies/p.name).write_text(new,encoding='utf-8')
        if new!=p.read_text(encoding='utf-8-sig'):changes.append({'file':p.name,'original_sha256':sha(p),'baseline_derived_path':prior.relative_to(ROOT).as_posix(),'baseline_derived_sha256':sha(prior),'generated_sha256':sha(copies/p.name),'new_count_replacements':n,'new_coverage_upgrade':p.name=='TestNpc86.java'})
    write(copies/'EXPECTATION_UPDATES.json',{'scope':'Retains112 isolated expectation changes;story cardinality+1 only;explicit single Duke X false-positive upgrade in86. All historical positive wording/style and every other negative preserved.','files':changes})
    write(SOURCE/'NPC_LITERAL_COVERAGE_0.15.113.json',{'scope':'Fixed proper name Duke X was incorrectly flagged by the old broad X-token extractor;one old negative explicitly upgraded in isolated86 only. No placeholder literal registered.','record':literal,'baseline_jar_sha256':BASE})
    s=(ROOT/'tools/check_expansion112.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('0.15.112-dev','0.15.113-dev'),('expansion112-checks','npc113-checks'),('expansion112-test-sources','npc113-test-sources'),('translation-source-regression112-v2','translation-source-regression113'),('TestNpcCorpusParser112','TestNpcCorpusParser113'),('EXPANSION112','NPC113'),("@('TestExpansion112'","@('TestNpc113','TestExpansion112'"),('-ne 185','-ne 186'),('/185','/186')]:s=s.replace(old,new)
    (ROOT/'tools/check_npc113.ps1').write_text(s,encoding='utf-8')
    for kind in ('dispatch','benchmark','benchmark_long'):
        oldname={'dispatch':'dispatch_expansion112.ps1','benchmark':'benchmark_expansion112.ps1','benchmark_long':'benchmark_expansion112_long.ps1'}[kind]
        name={'dispatch':'dispatch_npc113.ps1','benchmark':'benchmark_npc113.ps1','benchmark_long':'benchmark_npc113_long.ps1'}[kind]
        s=(ROOT/'tools'/oldname).read_text(encoding='utf-8-sig')
        for old,new in [('0.15.112-dev','0.15.113-dev'),('0.15.111-dev','0.15.112-dev'),('expansion112-','npc113-'),('BenchmarkExpansion112','BenchmarkNpc113'),('EXPANSION112','NPC113'),('JVMs111,112,112,111','JVMs112,113,113,112'),('fifteen equal-work','seventeen equal-work'),('three predeclared','six predeclared')]:s=s.replace(old,new)
        (ROOT/'tools'/name).write_text(s,encoding='utf-8')
    for suffix in ('','Long'):
        p=ROOT/f'tests/BenchmarkExpansion112{suffix}.java';s=p.read_text(encoding='utf-8-sig').replace('BenchmarkExpansion112','BenchmarkNpc113')
        marker='Set.of("unknown",' if not suffix else 'Set.of("existing_dynamic",'
        assert s.count(marker)==1
        added='"existing_names103_equal_work",' if not suffix else '"existing_names103_equal_work","unknown_named_speaker",'
        s=s.replace(marker,marker+added+'"expanded_prefix_misses",').replace('Focused15','Focused17').replace('Predeclared3','Predeclared6').replace('three predeclared','six predeclared')
        anchor='        var bean=(com.sun.management.ThreadMXBean)'
        extra='        var expanded=new LinkedHashSet<String>();for(var raw:JsonParser.parseString(Files.readString(Path.of("translation-source/NPC_STRUCTURED_SOURCES_0.15.113.json"))).getAsJsonObject().getAsJsonArray("records")){String source=raw.getAsJsonObject().get("source").getAsString();expanded.add(source.substring(0,source.indexOf(": ")+2)+"Future unpublished113 dialogue.");}inputs.put("expanded_prefix_misses",List.copyOf(expanded));\n'
        assert s.count(anchor)==1;s=s.replace(anchor,extra+anchor)
        (ROOT/f'tests/BenchmarkNpc113{suffix}.java').write_text(s,encoding='utf-8')
    write(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.113.json',{'scope':'Declared before performance results. Same112 fifteen short/three long equal-output controls plus old135 names short and long,unknown named speaker long,and unknown sentences for every newly expanded speaker prefix in both. Isolated receipt,not FPS. All raw retained.','baseline_sha256':BASE,'short_modes':17,'long_modes':['existing_dynamic','unknown_system_notifications','retained_cosmetic_notifications','existing_names103_equal_work','unknown_named_speaker','expanded_prefix_misses'],'order':['112','113','113','112'],'short_warmup':300000,'short_rounds':4,'short_calls_per_round_at_least':200000,'long_warmup':1000000,'long_rounds':4,'long_calls_per_round_at_least':1000000})
    print('Prepared186 groups;',len(changes),'isolated expectation files;96 runtime cases;17short+6long equal-work modes')

if __name__=='__main__':main()
