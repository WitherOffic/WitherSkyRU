"""Extend independent TSV/publication fixtures without weakening previous assertions."""
import json,re
from prepare_npc101 import ROOT,SPEAKERS,PREVIOUS
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
s=(PREVIOUS/'tests/TestNpc100.java').read_text(encoding='utf-8').replace('TestNpc100','TestNpc101').replace('0.15.100','0.15.101').replace('npc-expansion100','npc-expansion101').replace('NPC100','NPC101')
for x,y in [('rows.size()==382','rows.size()==385'),('all382 new literals','all385 new literals'),('evidence.size()==405','evidence.size()==544'),('pending_variables").size()==14','pending_variables").size()==16'),('complete405 row partition','complete544 row partition'),('54 counted labels','79 counted labels'),('story.phraseCount()==9821','story.phraseCount()==10206'),('9439 retained plus382 authored literals','9821 retained plus385 authored literals'),('Set.of("Finnegan","Foxy","Madame Eleanor Q. Goldsworth III")','Set.of("Lumina")'),('source_pages").size()==55','source_pages").size()==83'),('retained_originals").size()==6','retained_originals").size()==136'),('6 old authored keys retained','136 old authored keys retained'),('neutral_unchanged").size()==2','neutral_unchanged").size()==6'),('two neutral sources','six neutral sources'),('extraction_pending").size()==1','extraction_pending").size()==0'),('one malformed-layout source candidate','no malformed-layout source candidates'),('layouts==384','layouts==385'),('all384 published legacy strings','all385 published legacy strings'),('runtime_retained").size()==0','runtime_retained").size()==1'),('no redundant old dynamic results counted','one old dynamic result retained,not newly counted')]:
 assert x in s,x
 s=s.replace(x,y)
s=re.sub(r'var expectedCounts=Map.ofEntries\(.*?\);','var expectedCounts=Map.ofEntries('+','.join('Map.entry('+json.dumps(n)+','+str(c)+')' for n,c in SPEAKERS.items())+');',s)
s=re.sub(r'        check\(packaged.get\("\[NPC\] Madame Eleanor Q\. Goldsworth V:.*?\n','',s)
marker='        int layouts=0;';assert marker in s
s=s.replace(marker,'''        check(packaged.get("[NPC] Gavin: Don't tell mum this, but I just use /scg to open it.").get("translation").getAsString().contains("/scg"),"literal command preserved,never sent");
        String puzzle="[NPC] Puzzler: ▲▼triangle▼▶▲food◀▲";
        check(packaged.get(puzzle).get("translation").getAsString().equals("[NPC] Puzzler: ▲▼треугольник▼▶▲еда◀▲"),"puzzle glyph sequence preserved exactly");
        check(evidence.get("[NPC] Fragilis: Y'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!").getAsJsonObject("published_row").get("variable_definition").getAsBoolean(),"reviewed classifier false positive retained as provenance,not hidden queue rewrite");
'''+marker)
s=re.sub(r'        System.out.println\("PASS: NPC101 .*?\n','        System.out.println("PASS: NPC101 "+checks+" assertions;385 authored literals/385 published layouts;79 speaker labels;16 variable,6 neutral,1 previously dynamic row separate;every character/colors/actions/numbers/puzzle glyphs/commands/overrides/protected chat;not FPS/live completeness");\n',s)
emit('tests/TestNpc101.java',s)
s=(ROOT/'tests/TestNpcChoices100.java').read_text(encoding='utf-8').replace('TestNpcChoices100','TestNpcChoices101').replace('0.15.100','0.15.101').replace('NPCChoices100','NPCChoices101')
s=s.replace('groups==8&&buttons==9&&layouts==8','groups==6&&buttons==7&&layouts==6').replace('8 source groups/9 translated label slots;8 published layouts','6 source groups/7 translated label slots;6 published layouts').replace('8 exact ordered groups/9 label slots/8 valid published layouts;9 changed labels','6 exact ordered groups/7 label slots/6 valid published layouts;7 changed labels')
s=s.replace('        check(groups==6', '''        check(ref.getAsJsonArray("excluded_groups_not_runtime").size()==1,"one exact malformed published choice separate from accepted groups");
        var malformedChoice=ref.getAsJsonArray("excluded_groups_not_runtime").get(0).getAsJsonObject();
        check(malformedChoice.get("source").getAsString().equals("Select an option: [Yeah]."),"exact external-punctuation publication typo");
        var malformedInput=Component.literal(malformedChoice.get("source_legacy_line").getAsString().strip());
        check(dispatch(malformedInput)==malformedInput,"unverified external punctuation never broadens gameplay parsing");
        check(groups==6''')
emit('tests/TestNpcChoices101.java',s)
for path in (ROOT/'tests').glob('*.java'):
 s=path.read_text(encoding='utf-8-sig')
 if 'phraseCount()==9821' in s:path.write_text(s.replace('phraseCount()==9821','phraseCount()==10206'),encoding='utf-8')
s=(ROOT/'tools/check_npc100.ps1').read_text().replace('0.15.100-dev','0.15.101-dev').replace('npc100-checks','npc101-checks').replace('NPC100','NPC101').replace('build/npc99-checks-502ca76eeabe4c9aa8449399ab6fa8a8','build/npc100-checks-cf94728c3189450194ab19704b7f5cc2').replace("@('TestNpc100','TestNpcChoices100')","@('TestNpc101','TestNpcChoices101')").replace('169','171').replace('TestNpcCorpusParser100','TestNpcCorpusParser101')
emit('tools/check_npc101.ps1',s)
# A source-specific correction in OFFLINE screening only. No gameplay grammar
# changes: the pinned template is a complete constant sentence, not a variable.
s=(ROOT/'tests/AuditNpcCorpus77.java').read_text().replace('AuditNpcCorpus77','AuditNpcCorpus101')
needle='row.get("variable_definition").getAsBoolean()||';assert s.count(needle)==1
s=s.replace(needle,'(row.get("variable_definition").getAsBoolean()&&!source.equals("[NPC] Fragilis: Y\'know now that we are getting deeper and deeper a lot of these blocks are getting harder to mine!"))||')
emit('tests/AuditNpcCorpus101.java',s)
s=(ROOT/'tools/dispatch_npc100.ps1').read_text().replace('npc100','npc101').replace('NPC100','NPC101').replace('0.15.100-dev','0.15.101-dev').replace('0.15.99-dev','0.15.100-dev').replace('AuditNpcCorpus77','AuditNpcCorpus101')
emit('tools/dispatch_npc101.ps1',s)
print('Derived 385 literals/6 choice groups;171 fresh groups;10206 story keys;11248 offline audit rows.')
