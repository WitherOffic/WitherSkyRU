"""Mechanical fixture extension, retaining prior assertions and independent TSV targets."""
import json,re
from prepare_npc100 import ROOT,SOURCE,SPEAKERS

def emit(rel,s): (ROOT/rel).write_text(s,encoding='utf-8')
s=(ROOT/'tests/TestNpc99.java').read_text(encoding='utf-8').replace('TestNpc99','TestNpc100').replace('0.15.99','0.15.100').replace('npc-expansion99','npc-expansion100').replace('NPC99','NPC100')
for x,y in [('rows.size()==397','rows.size()==382'),('all397 new literals','all382 new literals'),('evidence.size()==438','evidence.size()==405'),('pending_variables").size()==2','pending_variables").size()==14'),('complete438 row partition','complete405 row partition'),('40 counted labels','54 counted labels'),('story.phraseCount()==9439','story.phraseCount()==9821'),('9042 retained plus397 authored literals','9439 retained plus382 authored literals'),('Set.of("Rock","Worker Xavier")','Set.of("Finnegan","Foxy","Madame Eleanor Q. Goldsworth III")'),('source_pages").size()==38','source_pages").size()==55'),('retained_originals").size()==27','retained_originals").size()==6'),('27 old authored keys retained','6 old authored keys retained'),('neutral_unchanged").size()==1','neutral_unchanged").size()==2'),('one neutral source','two neutral sources'),('unsafe_excluded").size()==11','unsafe_excluded").size()==0'),('11 explicit Removed dialogue rows not selected','no Removed unique rows selected'),('extraction_pending").size()==0','extraction_pending").size()==1'),('no malformed-layout source candidates','one malformed-layout source candidate'),('layouts==398','layouts==384'),('all398 published legacy strings','all384 published legacy strings')]:
 assert x in s,x
 s=s.replace(x,y)
s=re.sub(r'var expectedCounts=Map.ofEntries\(.*?\);','var expectedCounts=Map.ofEntries('+','.join('Map.entry('+json.dumps(n)+','+str(c)+')' for n,c in SPEAKERS.items())+');',s)
s=re.sub(r'        System.out.println\("PASS: NPC100 .*?\n','        System.out.println("PASS: NPC100 "+checks+" assertions;382 authored literals/384 publication layouts;54 speaker labels;14 variable,1 extraction,2 neutral rows separately accounted;every character/colors/actions/fragmentation/timestamps/counters/numbers/overrides/protected chat;not FPS/live completeness");\n',s)
# The quest explicitly requires this English player input. We translate the
# instruction but never the required phrase nor any outgoing command.
needle='        int layouts=0;'
assert needle in s
s=s.replace(needle,'''        check(packaged.get("[NPC] Madame Eleanor Q. Goldsworth V: Then say \\\"Eleanor is the best!\\\"").get("translation").getAsString().equals("[NPC] Madame Eleanor Q. Goldsworth V: Затем скажи «Eleanor is the best!»"),"required server input preserved verbatim");
'''+needle)
emit('tests/TestNpc100.java',s)
s=(ROOT/'tests/TestNpcChoices99.java').read_text(encoding='utf-8').replace('TestNpcChoices99','TestNpcChoices100').replace('0.15.99','0.15.100').replace('NPCChoices99','NPCChoices100')
s=s.replace('groups==11&&buttons==11&&layouts==12','groups==8&&buttons==9&&layouts==8').replace('11 source groups/11 translated label slots;12 published layouts','8 source groups/9 translated label slots;8 published layouts').replace('11 exact ordered groups/11 label slots/12 valid published layouts;11 changed labels','8 exact ordered groups/9 label slots/8 valid published layouts;9 changed labels')
emit('tests/TestNpcChoices100.java',s)
for path in (ROOT/'tests').glob('*.java'):
 s=path.read_text(encoding='utf-8-sig')
 if 'phraseCount()==9439' in s:path.write_text(s.replace('phraseCount()==9439','phraseCount()==9821'),encoding='utf-8')
s=(ROOT/'tools/check_npc99.ps1').read_text().replace('0.15.99-dev','0.15.100-dev').replace('npc99-checks','npc100-checks').replace('NPC99','NPC100').replace('build/npc98-checks-019a7a24546143eda7c0953cc23535de','build/npc99-checks-502ca76eeabe4c9aa8449399ab6fa8a8').replace("@('TestNpc99','TestNpcChoices99')","@('TestNpc100','TestNpcChoices100')").replace('167','169').replace('TestNpcCorpusParser99','TestNpcCorpusParser100')
emit('tools/check_npc100.ps1',s)
s=(ROOT/'tools/dispatch_npc99.ps1').read_text().replace('npc99','npc100').replace('NPC99','NPC100').replace('0.15.99-dev','0.15.100-dev').replace('0.15.98-dev','0.15.99-dev')
emit('tools/dispatch_npc100.ps1',s)
print('Derived 382-literal/8-choice fixtures;169 fresh groups;9821 story keys.')
