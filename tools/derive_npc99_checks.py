"""Extend independent fixtures/runners; authored Russian stays in reviewed TSV files."""
import json,re
from prepare_npc99 import ROOT,SOURCE,SPEAKERS,selection
q,k,selected,eligible,o,b,p=selection()
progress=json.loads((SOURCE/'NPC_PROGRESS_0.15.99.json').read_text());layouts=sum(len({o[i]['source_legacy_line'].strip() for i in r['occurrence_ids']}) for r in eligible)
s=(ROOT/'tests/TestNpc93.java').read_text().replace('TestNpc93','TestNpc99').replace('0.15.93','0.15.99').replace('npc-expansion93','npc-expansion99').replace('NPC93','NPC99')
s=s.replace('rows.size()==541','rows.size()==397').replace('all541 new literals','all397 new literals').replace('evidence.size()==638','evidence.size()==438').replace('pending_variables").size()==40','pending_variables").size()==2').replace('complete638 row partition','complete438 row partition')
s=re.sub(r'var expectedCounts=Map.ofEntries\(.*?\);','var expectedCounts=Map.ofEntries('+','.join('Map.entry('+json.dumps(n)+','+str(c)+')' for n,c in SPEAKERS.items())+');',s)
s=s.replace('43 counted labels','40 counted labels').replace('story.phraseCount()==9042','story.phraseCount()==9439').replace('8501 retained plus541 authored literals','9042 retained plus397 authored literals')
s=s.replace('removedPages.isEmpty()&&reference.getAsJsonArray("source_pages").size()==46','removedPages.equals(Set.of("Rock","Worker Xavier"))&&reference.getAsJsonArray("source_pages").size()==38')
s=re.sub(r'        check\(packaged.get\("\[NPC\] Weird Sailor:.*?\n','',s)
s=re.sub(r'        if\(row.source.equals\("\[NPC\] Weird Sailor:.*?\n','',s)
s=s.replace('retained_originals").size()==52','retained_originals").size()==27').replace('52 old authored keys retained','27 old authored keys retained')
s=s.replace('neutral_unchanged").size()==5','neutral_unchanged").size()==1').replace('five neutral sources','one neutral source')
s=s.replace('reference.getAsJsonArray("unsafe_excluded").isEmpty()','reference.getAsJsonArray("unsafe_excluded").size()==11').replace('no unsafe unique literal selected','11 explicit Removed dialogue rows not selected')
s=s.replace('layouts==542',f'layouts=={layouts}').replace('all542 published legacy strings',f'all{layouts} published legacy strings')
# Independently derive the expected style of every output character from the
# reviewed semantic ranges and the input, not from the translator result.
marker='        int prefix=time.length()+prefix(row.source);'
assert marker in s
s=s.replace(marker,'''        String inputPlain=plain(input),outputPlain=plain(output);
        int sourceStart=time.length()+prefix(row.source),targetStart=time.length()+prefix(row.target);
        Style base=null;
        for(int n=sourceStart;n<inputPlain.length()-suffix.length();n++){
            if(Character.isWhitespace(inputPlain.charAt(n)))continue;
            boolean covered=false;for(var semantic:row.anchors)if(n>=time.length()+semantic.from&&n<time.length()+semantic.from+semantic.source.length()){covered=true;break;}
            if(!covered){base=a.get(n);break;}
        }
        if(base==null)base=a.get(sourceStart);
        for(int n=targetStart;n<outputPlain.length()-suffix.length();n++){
            Style expected=base;
            for(var semantic:row.anchors)if(n>=time.length()+semantic.to&&n<time.length()+semantic.to+semantic.target.length()){
                int original=time.length()+semantic.from+(semantic.source.equals(semantic.target)?n-time.length()-semantic.to:0);expected=a.get(original);break;
            }
            check(b.get(n).equals(expected),"every translated character keeps reviewed meaning/style/current actions: "+row.source);
        }
'''+marker)
s=re.sub(r'        System.out.println\("PASS: NPC99 .*?\n',f'        System.out.println("PASS: NPC99 "+checks+" assertions;397 authored literals/{layouts} published layouts;40 speaker labels;two structured and11 Removed rows pending/excluded;every character,colors,fresh actions,fragmentation,timestamps,counters,numbers,overrides,protected chat;not FPS/live completeness");\n',s)
(ROOT/'tests/TestNpc99.java').write_text(s,encoding='utf-8')
s=(ROOT/'tests/TestNpcChoices93.java').read_text().replace('TestNpcChoices93','TestNpcChoices99').replace('0.15.93','0.15.99').replace('NPCChoices93','NPCChoices99')
s=s.replace('buttons==12','buttons==11').replace('12 translated label slots','11 translated label slots').replace('12 label slots','11 label slots').replace('12 changed labels','11 changed labels')
s=s.replace('layouts==11','layouts==12').replace('11 published layouts','12 published layouts').replace('11 valid published layouts','12 valid published layouts')
(ROOT/'tests/TestNpcChoices99.java').write_text(s,encoding='utf-8')
# No previous assertion is removed; only the final merged story-map total grows.
for path in (ROOT/'tests').glob('*.java'):
 text=path.read_text(encoding='utf-8-sig')
 if 'phraseCount()==9042' in text:path.write_text(text.replace('phraseCount()==9042','phraseCount()==9439'),encoding='utf-8')
s=(ROOT/'tools/check_npc98.ps1').read_text().replace('0.15.98-dev','0.15.99-dev').replace('npc98-checks','npc99-checks').replace('NPC98','NPC99')
s=s.replace('build/opt96-checks-fc41dfc90eed4bf39a58382bb736fc55','build/npc98-checks-019a7a24546143eda7c0953cc23535de')
s=s.replace("@('TestNpc98')","@('TestNpc99','TestNpcChoices99')").replace('165','167').replace('TestNpcCorpusParser98','TestNpcCorpusParser99')
(ROOT/'tools/check_npc99.ps1').write_text(s,encoding='utf-8')
s=(ROOT/'tools/dispatch_npc98.ps1').read_text().replace('npc98','npc99').replace('NPC98','NPC99').replace('0.15.98-dev','0.15.99-dev').replace('0.15.96-dev','0.15.98-dev')
(ROOT/'tools/dispatch_npc99.ps1').write_text(s,encoding='utf-8')
print('Derived397-literal/11-choice independent fixtures;167-group runner;9439 story total;layouts',layouts)
