"""Mechanical fixture/runner extension. Expected Russian text stays in independent reviewed TSVs."""
import json,re
from pathlib import Path
from prepare_npc93 import ROOT,SOURCE,SPEAKERS
def next_names(s):
 for a,b in [('0.15.92','0.15.93'),('Npc92','Npc93'),('NPC92','NPC93'),('Choices92','Choices93'),('Parser92','Parser93'),('npc92','npc93'),('expansion92','expansion93')]:s=s.replace(a,b)
 return s
def main():
 tests=ROOT/'tests';tools=ROOT/'tools'
 s=next_names((tests/'TestNpc92.java').read_text())
 s=s.replace('rows.size()==591','rows.size()==541').replace('all591 new literals','all541 new literals').replace('evidence.size()==671','evidence.size()==638').replace('size()==46','size()==40').replace('complete671 row partition','complete638 row partition')
 s=re.sub(r'var expectedCounts=Map.ofEntries\(.*?\);','var expectedCounts=Map.ofEntries('+','.join('Map.entry('+json.dumps(k,ensure_ascii=False)+','+str(v)+')' for k,v in SPEAKERS.items())+');',s)
 s=s.replace('37 counted labels','43 counted labels').replace('story.phraseCount()==8501','story.phraseCount()==9042').replace('7910 retained plus591 authored literals','8501 retained plus541 authored literals')
 s=s.replace('removedPages.equals(Set.of("Blacksmith","Einary"))&&reference.getAsJsonArray("source_pages").size()==39','removedPages.isEmpty()&&reference.getAsJsonArray("source_pages").size()==46')
 s=re.sub(r'        String trigger=.*?\n', '        check(packaged.get("[NPC] Weird Sailor: im goin 2 da circus").get("translation").getAsString().equals("[NPC] Weird Sailor: еду в цирк"),"slang2 means to,not numeric quantity");\n',s)
 s=s.replace('size()==28','size()==52').replace('28 old authored keys retained','52 old authored keys retained')
 s=s.replace('neutral_unchanged").size()==3','neutral_unchanged").size()==5').replace('three neutral sources','five neutral sources')
 s=s.replace('extraction_pending").size()==3','extraction_pending").size()==0').replace('three malformed-layout source candidates','no malformed-layout source candidates')
 s=s.replace('check(layouts==595,"all595 published legacy strings")','check(layouts==542,"all542 published legacy strings")')
 s=s.replace('591 new authored literals/595 published layouts;37 speaker labels;46 structured sources pending;three neutral rows;28 old keys retained;three malformed/extraction candidates pending;literal chat triggers;colors,obfuscated letters','541 new authored literals/542 published layouts;43 speaker labels;40 structured sources pending;five neutral rows;52 old keys retained;no malformed/extraction candidates;lexical2 exception;colors')
 marker='        var x=Pattern.compile("[0-9]+").matcher(plain(input));'
 assert marker in s;s=s.replace(marker,'        if(row.source.equals("[NPC] Weird Sailor: im goin 2 da circus")){check(row.target.equals("[NPC] Weird Sailor: еду в цирк"),"exact reviewed lexical 2-to expansion");return;}\n'+marker)
 (tests/'TestNpc93.java').write_text(s,encoding='utf-8')
 s=next_names((tests/'TestNpcChoices92.java').read_text()).replace('groups==14&&buttons==15&&layouts==14','groups==11&&buttons==12&&layouts==11').replace('14 source groups/15 translated label slots;14 published layouts','11 source groups/12 translated label slots;11 published layouts').replace('14 exact ordered groups/15 label slots/14 valid published layouts;15 changed labels','11 exact ordered groups/12 label slots/11 valid published layouts;12 changed labels')
 (tests/'TestNpcChoices93.java').write_text(s,encoding='utf-8')
 for name in ('check_npc92.ps1','check_npc92_parallel.ps1','dispatch_npc92.ps1','check_npc92_bytecode.ps1'):
  s=next_names((tools/name).read_text()).replace('npc91-checks-d0e906114ff0497a9dc17e2577a06569','npc92-checks-3172a7bf5466429c9a359cc4562b63a2').replace('0.15.91-dev','0.15.92-dev').replace('159','161')
  (tools/name.replace('npc92','npc93')).write_text(s,encoding='utf-8')
 # Retain every existing assertion; only the total grows when the startup map gains541 keys.
 for path in tests.glob('*.java'):
  s=path.read_text(encoding='utf-8-sig')
  if 'phraseCount()==8501' in s:path.write_text(s.replace('phraseCount()==8501','phraseCount()==9042'),encoding='utf-8')
 print('Derived2 independent batch93 fixtures;161-group fresh runners;existing test expectations extended to9042')
if __name__=='__main__':main()
