"""Mechanical harness derivation; immutable historical targets and explicit cardinality copies."""
import argparse,json,re,shutil
from prepare_npc78 import ROOT,SOURCE,write,sha
def main():
    s=(ROOT/'tests/AuditSystem107.java').read_text(encoding='utf-8-sig').replace('AuditSystem107','AuditBoss108').replace('SYSTEM_CANDIDATES_0.15.107','BOSS_CANDIDATES_0.15.108').replace('SYSTEM107','BOSS108')
    (ROOT/'tests/AuditBoss108.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/snapshot_system107.ps1').read_text(encoding='utf-8-sig').replace("@('0.15.106-dev')","@('0.15.107-dev')").replace('system107-snapshots','boss108-snapshots').replace('AuditSystem107','AuditBoss108').replace('SYSTEM107','BOSS108')
    (ROOT/'tools/snapshot_boss108.ps1').write_text(s,encoding='utf-8')
    if not (SOURCE/'BOSS_PREPARATION_0.15.108.json').exists():return
    stats=json.loads((SOURCE/'BOSS_PREPARATION_0.15.108.json').read_text(encoding='utf-8'));assert stats['cases']==204
    s=(ROOT/'tests/TestNpc106.java').read_text(encoding='utf-8-sig').replace('TestNpc106','TestBoss108').replace('NPC106','BOSS108')
    s=s.replace('var story=NpcDialogueTranslator.storyDialogues();check(story.phraseCount()==10628,"10525 retained plus103 authored archival keys");','var story=NpcDialogueTranslator.dungeonDialogues();check(story.phraseCount()==460,"358 retained plus102 exact boss keys");')
    s=s.replace('flag("storyDialogues",story);','flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("dungeonDialogues",story);')
    s=s.replace('NPC_CASES_0.15.106.json','BOSS_CASES_0.15.108.json')
    start=s.index('        for(String line:Files.readAllLines(');end=s.index('            for(int seed=',start)
    s=s[:start]+'''        for(var value:frozen){var previous=value.getAsJsonObject();String source=previous.get("source").getAsString(),target=previous.get("translation").getAsString(),legacy=previous.get("legacy").getAsString();check(tested.add(source+"/"+previous.get("kind").getAsString()),"unique source/layout pair");check(plain(Component.literal(legacy)).equals(source),"complete published input");var definitions=previous.getAsJsonArray("style_anchors");
'''+s[end:]
    start='if(source.startsWith("[NPC] Tia the Fairy: ")&&!suffix.isEmpty())';s=s.replace(start,'if(!suffix.isEmpty())').replace('base NPC provider intentionally retains unsupported repeat suffix','existing dungeon provider intentionally retains unsupported repeat suffix')
    old='var input=modern(legacy,6,false,"","");flag("translateNpcChat",false);'
    new='var own=config.resolve("full-"+count);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\\tМой текст\\n");flag("dictionary",new OfflineDictionary(own));var input=modern(legacy,6,false,"","");check(queued(input)==input,"full source override preserved");flag("dictionary",db);flag("translateNpcChat",false);'
    assert s.count(old)==1;s=s.replace(old,new)
    s=s.replace('count==206','count==204').replace('103 archival literals,each primary and published layout','102 boss literals,each primary and published layout').replace('103 archival targets;206 primary/published fixtures','41 non-Dante+61 archival Dante targets;204 primary/published fixtures').replace('independently authored106','independently authored108')
    start=s.index('        var input=Component.literal("[NPC] Baker: My ")');end=s.index('        System.out.println(',start)
    s=s[:start]+'''        var input=Component.literal("[BOSS] Bal: You ").append(Component.literal("cannot best me!").setStyle(Style.EMPTY.withColor(0x123456)));check(story.translate(input,db)==input,"unreviewed semantic split not guessed");
        for(String unknown:List.of("[BOSS] Bal: An unpublished future sentence.","[NPC] Bal: You cannot best me!","[BOSS] Unknown: You cannot best me!")){var original=Component.literal(unknown);check(queued(original)==original,"unknown speaker/tag/body protected");}
'''+s[end:]
    (ROOT/'tests/TestBoss108.java').write_text(s,encoding='utf-8')
    snapshot=ROOT/'translation-source-regression108';snapshot.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,snapshot/p.name)
    prior=ROOT/'translation-source-regression107-v2';sup=json.loads((prior/'EXPECTATION_SUPERSESSION105.json').read_text(encoding='utf-8'))
    for r in sup['changed_files']:shutil.copy2(prior/r['file'],snapshot/r['file'])
    shutil.copy2(prior/'EXPECTATION_SUPERSESSION105.json',snapshot/'EXPECTATION_SUPERSESSION105.json')
    generated=ROOT/'build/boss108-test-sources';generated.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        old=p.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)10525\b','10628',old)
        if p.name=='TestNpc98.java':new,n2=re.subn(r'phraseCount\(\)==358','phraseCount()==460',new);assert n2==1;n+=n2
        if p.name=='TestDungeonChatResources.java':new,n2=re.subn(r'entries.size\(\)\+336','entries.size()+438',new);assert n2==1;n+=n2
        (generated/p.name).write_text(new,encoding='utf-8')
        if n:changes.append({'file':p.name,'replacements':n,'source_sha256':sha(p),'generated_sha256':sha(generated/p.name)})
    assert len(changes)==31;write(generated/'COUNT_EXPECTATION_UPDATES.json',{'scope':'29 prior story counts10525->10628;two existing dungeon count assertions358->460 and22+336->22+438. No text/style golden changes.','files':changes})
    s=(ROOT/'tools/check_system107.ps1').read_text(encoding='utf-8-sig').replace('0.15.107-dev','0.15.108-dev').replace('system107-checks-','boss108-checks-').replace('SYSTEM107','BOSS108').replace('TestNpcCorpusParser107','TestNpcCorpusParser108').replace('build/system107-test-sources-v2','build/boss108-test-sources').replace('translation-source-regression107-v2','translation-source-regression108')
    s=s.replace("@('TestSystem107')","@('TestBoss108','TestSystem107')").replace('-ne 180','-ne 181').replace('/180','/181')
    (ROOT/'tools/check_boss108.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/dispatch_system107.ps1').read_text(encoding='utf-8-sig').replace('0.15.107-dev','0.15.108-dev').replace('0.15.106-dev','0.15.107-dev').replace('system107-dispatch-','boss108-dispatch-').replace('SYSTEM107','BOSS108')
    (ROOT/'tools/dispatch_boss108.ps1').write_text(s,encoding='utf-8')
    benchmark()
    print('204 strict cases;181 groups;31 isolated cardinality copies;14 equal-output modes')
def benchmark():
    s=(ROOT/'tests/BenchmarkSystem107.java').read_text(encoding='utf-8-sig').replace('BenchmarkSystem107','BenchmarkBoss108')
    marker='        var bean=(com.sun.management.ThreadMXBean)'
    addition='''        var retainedBosses=new ArrayList<String>();try(var r=new java.io.InputStreamReader(BenchmarkBoss108.class.getResourceAsStream("/data/witherskyru/dungeon-expansion98.json"),java.nio.charset.StandardCharsets.UTF_8)){var list=JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("records");for(int i=0;i<3;i++)retainedBosses.add(list.get(i).getAsJsonObject().get("source").getAsString());}
        inputs.put("retained_boss_dialogues",retainedBosses);inputs.put("unknown_boss_dialogues",List.of("[BOSS] Arachne: Future boss dialogue not yet published.","[BOSS] Unknown: An unpublished future boss sentence."));
'''
    assert s.count(marker)==1;s=s.replace(marker,addition+marker)
    s=s.replace('"retained_system_goals","unknown_system_notifications")','"retained_system_goals","unknown_system_notifications","retained_boss_dialogues","unknown_boss_dialogues")').replace('Focused12','Focused14')
    (ROOT/'tests/BenchmarkBoss108.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_system107.ps1').read_text(encoding='utf-8-sig').replace('0.15.107-dev','0.15.108-dev').replace('0.15.106-dev','0.15.107-dev').replace('system107-verify-','boss108-verify-').replace('BenchmarkSystem107','BenchmarkBoss108').replace('SYSTEM107','BOSS108').replace('JVMs106,107,107,106','JVMs107,108,108,107').replace('twelve equal-work','fourteen equal-work')
    (ROOT/'tools/benchmark_boss108.ps1').write_text(s,encoding='utf-8')
if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--benchmark-only',action='store_true');args=ap.parse_args()
    if args.benchmark_only:benchmark()
    else:main()
