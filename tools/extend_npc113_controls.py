"""Pre-run extension for misses at expanded prefixes;no performance result yet exists."""
from prepare_npc78 import ROOT,SOURCE,write
from prepare_npc113 import load

def main():
    for suffix in ('','Long'):
        p=ROOT/f'tests/BenchmarkNpc113{suffix}.java';s=p.read_text(encoding='utf-8-sig')
        marker='Set.of("unknown",' if not suffix else 'Set.of("existing_dynamic",';assert s.count(marker)==1
        s=s.replace(marker,marker+'"expanded_prefix_misses",').replace('Focused16','Focused17').replace('Predeclared5','Predeclared6')
        anchor='        var bean=(com.sun.management.ThreadMXBean)'
        extra='        var expanded=new LinkedHashSet<String>();for(var raw:JsonParser.parseString(Files.readString(Path.of("translation-source/NPC_STRUCTURED_SOURCES_0.15.113.json"))).getAsJsonObject().getAsJsonArray("records")){String source=raw.getAsJsonObject().get("source").getAsString();expanded.add(source.substring(0,source.indexOf(": ")+2)+"Future unpublished113 dialogue.");}inputs.put("expanded_prefix_misses",List.copyOf(expanded));\n'
        assert s.count(anchor)==1;s=s.replace(anchor,extra+anchor);p.write_text(s,encoding='utf-8')
    control=load(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.113.json');assert control['short_modes']==16 and len(control['long_modes'])==5
    control.update(short_modes=17,scope='Declared before any113 performance result. Original112 fifteen short/three long controls plus old135 names short/long,unknown named speaker long,and unknown sentences for all44 newly expanded NPC prefixes short/long. All raw retained;isolated receipt,not FPS.')
    control['long_modes'].append('expanded_prefix_misses');write(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.113.json',control)
    for name in ('benchmark_npc113.ps1','benchmark_npc113_long.ps1'):
        p=ROOT/'tools'/name;s=p.read_text(encoding='utf-8-sig').replace('sixteen equal-work','seventeen equal-work').replace('five predeclared','six predeclared');p.write_text(s,encoding='utf-8')
    p=ROOT/'tools/finalize_npc113.py';s=p.read_text(encoding='utf-8')
    for old,new in [('performance(a.benchmark,digest,16)','performance(a.benchmark,digest,17)'),('performance(a.long,digest,5)','performance(a.long,digest,6)'),('процесса: 16 равных','процесса: 17 равных'),('контроль пяти режимов','контроль шести режимов')]:assert s.count(old)==1;s=s.replace(old,new)
    p.write_text(s,encoding='utf-8');print('Controls extended to17+6 before measurements;production JAR unchanged')

if __name__=='__main__':main()
