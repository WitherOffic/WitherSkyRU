"""Mechanical extension of independent publication/target tests. No generated translations."""
import json,re
from prepare_npc98 import ROOT,SOURCE,selection
from prepare_npc78 import rows

q,k,selected,eligible,o,b,p=selection()
progress=json.loads((SOURCE/'NPC_PROGRESS_0.15.98.json').read_text())
s=(ROOT/'tests/TestNpc93.java').read_text()
s=s.replace('TestNpc93','TestNpc98').replace('0.15.93','0.15.98').replace('npc-expansion93','dungeon-expansion98').replace('NPC93','NPC98')
s=s.replace('NpcDialogueTranslator.storyDialogues()','NpcDialogueTranslator.dungeonDialogues()').replace('flag("storyDialogues",story)','flag("dungeonDialogues",story)')
s=s.replace('String prefix="[NPC] "+f[0]','String prefix="[BOSS] "+f[0]')
s=s.replace('rows.size()==541','rows.size()==336').replace('all541 new literals','all336 new literals').replace('evidence.size()==638','evidence.size()==365').replace('pending_variables").size()==40','pending_variables").size()==13').replace('complete638 row partition','complete365 row partition')
s=re.sub(r'var expectedCounts=Map.ofEntries\(.*?\);','var expectedCounts=Map.ofEntries('+','.join('Map.entry('+json.dumps(name)+','+str(count)+')' for name,count in progress['new_by_speaker'].items())+');',s)
s=s.replace('43 counted labels','11 counted labels').replace('story.phraseCount()==9042','story.phraseCount()==358').replace('8501 retained plus541 authored literals','22 retained dungeon keys plus336 authored boss literals')
s=s.replace('source_pages").size()==46','source_pages").size()==11')
s=s.replace('removedPages.isEmpty()','removedPages.equals(Set.of("Goldor","Maxor","Storm"))')
marker='        int prefix=time.length()+prefix(row.source);'
assert marker in s
s=s.replace(marker,'''        if(row.anchors.isEmpty()){
            var bodyStyle=a.get(time.length()+prefix(row.source));
            for(int n=time.length()+prefix(row.target);n<plain(output).length()-suffix.length();n++)check(b.get(n).equals(bodyStyle),"every translated body character keeps current publication style/actions: "+row.source);
        }
'''+marker)
s=re.sub(r'        check\(packaged.get\("\[NPC\] Weird Sailor:.*?\n','',s)
s=re.sub(r'        if\(row.source.equals\("\[NPC\] Weird Sailor:.*?\n','',s)
s=s.replace('retained_originals").size()==52','retained_originals").size()==8').replace('52 old authored keys retained','8 old authored keys retained')
s=s.replace('neutral_unchanged").size()==5','neutral_unchanged").size()==1').replace('five neutral sources','one neutral source')
s=s.replace('extraction_pending").size()==0','extraction_pending").size()==7')
# Dungeon provider deliberately retains its original no-counter contract.
s=s.replace('List.of(""," (x17)"," (×2)")','List.of("")')
s=s.replace('"[03:04:05] "," (x23)"','"[03:04:05] ",""')
layouts=sum(len({o[i]['source_legacy_line'].strip() for i in r['occurrence_ids']}) for r in eligible)
s=s.replace('layouts==542',f'layouts=={layouts}').replace('all542 published legacy strings',f'all{layouts} published legacy strings')
s=re.sub(r'        System.out.println\("PASS: NPC98 .*?\n',f'        System.out.println("PASS: NPC98 "+checks+" assertions;336 boss literals/{layouts} published layouts;11 labels;13 structural and7 extraction candidates pending;colors,fresh actions,fragmentation,timestamps,numbers,overrides,channels;not FPS/live completeness");\n',s)
marker='        check(layouts=='
at=s.index(marker)
s=s[:at]+'''        var boss=rows.getFirst();
        for(String extra:List.of(" (x17)"," (×2)")){var c=Component.literal(boss.source+extra);check(story.translate(c,db)==c,"original dungeon no-counter contract unchanged");}
        var original=Component.literal(boss.source.substring(0,prefix(boss.source))).append(Component.literal(boss.source.substring(prefix(boss.source),prefix(boss.source)+3)).setStyle(style(1,1))).append(Component.literal(boss.source.substring(prefix(boss.source)+3)).setStyle(style(1,2)));
        check(story.translate(original,db)==original,"unreviewed intra-word styles declined");
''' +s[at:]
(ROOT/'tests/TestNpc98.java').write_text(s,encoding='utf-8')
path=ROOT/'tests/TestDungeonChatResources.java';s=path.read_text();s=s.replace('translator.phraseCount()==entries.size()','translator.phraseCount()==(notification?entries.size():entries.size()+336)');path.write_text(s,encoding='utf-8')
# Extend the verified164-group runner, not globbed/rejected prototypes.
s=(ROOT/'tools/check_optimization96.ps1').read_text().replace('0.15.96-dev','0.15.98-dev').replace('opt96-checks','npc98-checks').replace('OPT96','NPC98')
s=s.replace('build/opt95-checks-321c57ebb7bd4f0985d7000a63a9355b','build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55')
s=s.replace("@('ru.wither.skyru.TestOptimization96')","@('TestNpc98')").replace('164','165').replace('TestNpcCorpusParser96','TestNpcCorpusParser98')
(ROOT/'tools/check_npc98.ps1').write_text(s,encoding='utf-8')
s=(ROOT/'tools/dispatch_npc93.ps1').read_text().replace('npc93','npc98').replace('NPC93','NPC98').replace('0.15.92-dev','0.15.96-dev').replace('0.15.93-dev','0.15.98-dev')
(ROOT/'tools/dispatch_npc98.ps1').write_text(s,encoding='utf-8')
s=(ROOT/'tools/benchmark_optimization96.ps1').read_text().replace('0.15.95-dev','0.15.96-dev').replace('0.15.96-dev.jar\"\n','0.15.98-dev.jar\"\n')
# Explicit version substitutions keep the before/after distinct.
s=(ROOT/'tools/benchmark_optimization96.ps1').read_text().replace('0.15.96-dev','0.15.98-dev').replace('0.15.95-dev','0.15.96-dev').replace('opt96-benchmark','npc98-benchmark').replace('OPT96 BENCHMARK','NPC98 BENCHMARK').replace('95,96,96,95','96,98,98,96').replace('Wrong95 baseline','Wrong96 baseline')
s=s.replace('E5D397D17E7F7ADF9EAD2C9BF7E607B134F2412EF89CB4D951CB78944825BCBF','97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1')
(ROOT/'tools/benchmark_npc98.ps1').write_text(s,encoding='utf-8')
print('Derived independent336-literal fixture,165-group runner,96->98 dispatch/benchmarks;layouts',layouts)
