"""Independent exact targets/publication style fixtures, including separate current additions."""
import json,re
from collections import Counter
from pathlib import Path
from prepare_npc102 import ROOT,SOURCE,SPEAKERS
BASE=Path('C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-19_3')
def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def fixture(cls,resource,tsv,ref,counts,total,rows,pages,pending,retained,neutral,extraction,reused,unsafe,markers,layouts):
 s=(BASE/'tests/TestNpc100.java').read_text(encoding='utf-8').replace('TestNpc100',cls).replace('0.15.100','0.15.102').replace('npc-expansion100',resource).replace('NPC100',cls[4:])
 s=s.replace('NPC_0.15.102_REVIEWED.tsv',tsv).replace('NPC_SOURCES_0.15.102.json',ref)
 changes={'rows.size()==382':f'rows.size()=={total}','all382 new literals':f'all{total} new literals','evidence.size()==405':f'evidence.size()=={rows}','pending_variables").size()==14':f'pending_variables").size()=={pending}','complete405 row partition':f'complete{rows} row partition','54 counted labels':f'{len(counts)} counted labels','story.phraseCount()==9821':'story.phraseCount()==10525','9439 retained plus382 authored literals':'all retained and new literal resources including3 current API additions','Set.of("Finnegan","Foxy","Madame Eleanor Q. Goldsworth III")':'Set.of('+','.join(json.dumps(n) for n in markers)+')','source_pages").size()==55':f'source_pages").size()=={pages}','retained_originals").size()==6':f'retained_originals").size()=={retained}','6 old authored keys retained':f'{retained} old authored keys retained','neutral_unchanged").size()==2':f'neutral_unchanged").size()=={neutral}','two neutral sources':f'{neutral} neutral sources','extraction_pending").size()==1':f'extraction_pending").size()=={extraction}','one malformed-layout source candidate':f'{extraction} malformed-layout source candidates','unsafe_excluded").size()==0':f'unsafe_excluded").size()=={unsafe}','no Removed unique rows selected':f'{unsafe} Removed unique rows not newly registered','runtime_retained").size()==0':f'runtime_retained").size()=={reused}','no redundant old dynamic results counted':f'{reused} old dynamic results not newly counted','layouts==384':f'layouts=={layouts}','all384 published legacy strings':f'all{layouts} published legacy strings'}
 for x,y in changes.items():assert x in s,x;s=s.replace(x,y)
 s=re.sub(r'var expectedCounts=Map.ofEntries\(.*?\);',lambda m:'var expectedCounts=Map.ofEntries('+','.join('Map.entry('+json.dumps(n,ensure_ascii=False)+','+str(c)+')' for n,c in counts.items())+');',s)
 s=re.sub(r'        check\(packaged.get\("\[NPC\] Madame Eleanor Q\. Goldsworth V:.*?\n','',s)
 s=s.replace('"old flat translation retained"','"old flat translation retained: "+source+" => "+plain(dispatch(Component.literal(source)))')
 s=s.replace('        var reference=JsonParser', '''        flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());
        var reference=JsonParser''')
 s=re.sub(r'        System.out.println\("PASS: .*?\n',f'        System.out.println("PASS: {cls[4:]} "+checks+" assertions;{total} independently authored literals/{layouts} published layouts;every character/colors/actions/numbers/fragmentation/overrides/protected chat;not FPS/live completeness");\n',s)
 emit('tests/'+cls+'.java',s)
fixture('TestNpc102','npc-expansion102','NPC_0.15.102_REVIEWED.tsv','NPC_SOURCES_0.15.102.json',SPEAKERS,316,917,148,43,530,15,5,6,2,['Bartender','Calvin','Dark Auction','Darr','Finnegan','Foxy','Golden Dragon','Hub Villagers','Mino','Resistance Leader','Vanessa'],317)
fixture('TestNpcCurrent102','npc-current102','NPC_CURRENT_0.15.102_REVIEWED.tsv','NPC_CURRENT_SOURCES_0.15.102.json',{'Ghost of Armorsmith':1,'Jeff':2},3,4,3,0,1,0,0,0,0,[],3)
s=(BASE/'tests/TestNpcChoices100.java').read_text(encoding='utf-8').replace('TestNpcChoices100','TestNpcChoices102').replace('0.15.100','0.15.102').replace('NPCChoices100','NPCChoices102')
s=s.replace('groups==8&&buttons==9&&layouts==8','groups==7&&buttons==7&&layouts==7').replace('8 source groups/9 translated label slots;8 published layouts','7 source groups/7 translated label slots;7 published layouts').replace('8 exact ordered groups/9 label slots/8 valid published layouts;9 changed labels','7 exact ordered groups/7 label slots/7 valid published layouts;7 changed labels')
emit('tests/TestNpcChoices102.java',s)
for path in (ROOT/'tests').glob('*.java'):
 s=path.read_text(encoding='utf-8-sig')
 if 'phraseCount()==10206' in s or 'phraseCount()==10526' in s:path.write_text(s.replace('phraseCount()==10206','phraseCount()==10525').replace('phraseCount()==10526','phraseCount()==10525'),encoding='utf-8')
s=(ROOT/'tools/check_npc101.ps1').read_text().replace('0.15.101-dev','0.15.102-dev').replace('npc101-checks','npc102-checks').replace('NPC101','NPC102').replace('build/npc100-checks-cf94728c3189450194ab19704b7f5cc2','build/npc101-checks-0040ccf3e4234781b4e0e15b956666ec').replace("@('TestNpc101','TestNpcChoices101')","@('TestNpc102','TestNpcChoices102','TestNpcCurrent102')").replace('171','174').replace('TestNpcCorpusParser101','TestNpcCorpusParser102')
emit('tools/check_npc102.ps1',s)
s=(ROOT/'tools/dispatch_npc101.ps1').read_text().replace('npc101','npc102').replace('NPC101','NPC102').replace('0.15.101-dev','0.15.102-dev').replace('0.15.100-dev','0.15.101-dev').replace('build/npc-corpus77-246a67d89cac465f9ac05ad2ba71e1d1','build/npc102-screening-input')
emit('tools/dispatch_npc102.ps1',s)
print('Derived316 frozen +3 new current literal fixtures;7 reply groups;174 fresh test groups;10525 story keys;11252 screening rows.')
