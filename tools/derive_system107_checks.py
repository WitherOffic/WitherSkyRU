"""Strict SYSTEM glyph/action fixtures;retain all earlier wording/count supersessions."""
import json,re,shutil
from prepare_npc78 import ROOT,SOURCE,write,sha

def main():
    stats=json.loads((SOURCE/'SYSTEM_PREPARATION_0.15.107.json').read_text(encoding='utf-8'))
    s=(ROOT/'tests/TestNpc106.java').read_text(encoding='utf-8-sig')
    s=s.replace('TestNpc106','TestSystem107').replace('NPC106','SYSTEM107')
    s=s.replace('var story=NpcDialogueTranslator.storyDialogues();check(story.phraseCount()==10628,"10525 retained plus103 authored archival keys");',f'var story=NpcDialogueTranslator.questObjectives();check(story.phraseCount()=={stats["quest_provider_after"]},"29 retained goals plus119 exact system notifications");')
    s=s.replace('flag("translateGameChat",false);flag("storyDialogues",story);','flag("translateGameChat",true);flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("questObjectives",story);')
    s=s.replace('NPC_CASES_0.15.106.json','SYSTEM_CASES_0.15.107.json')
    old='for(String line:Files.readAllLines(sourceRoot.resolve("NPC_CASE_STYLES_0.15.106.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;String[] fields=line.split("\\\\|",2);var previous=frozen.get(Integer.parseInt(fields[0])).getAsJsonObject();'
    assert s.count(old)==1
    s=s.replace(old,'for(var rawCase:frozen){var previous=rawCase.getAsJsonObject();')
    s=s.replace('previous.get("flat_result")','previous.get("translation")').replace('var definitions=anchors(fields[1]);','var definitions=previous.getAsJsonArray("style_anchors");')
    s=s.replace('int prefix=text.indexOf(": ")+2;if(text.startsWith("✆ ",prefix))prefix+=2;','int prefix=0;')
    s=s.replace('int prefix=source.indexOf(": ")+2;if(source.startsWith("✆ ",prefix))prefix+=2;','int prefix=source.startsWith("✆ ")?2:0;')
    s=s.replace('List.of("","[04:05:06] ")','List.of("","[04:05:06] ","   ","[04:05:06]   ")')
    s=s.replace('if(source.startsWith("[NPC] Tia the Fairy: ")&&!suffix.isEmpty()){check(output==input,"base NPC provider intentionally retains unsupported repeat suffix");continue;}','')
    old='var input=modern(legacy,6,false,"","");flag("translateNpcChat",false);check(queued(input)==input,"NPC switch");flag("translateNpcChat",true);'
    new='var own=config.resolve("full-"+count);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\\tМой текст\\n");flag("dictionary",new OfflineDictionary(own));var input=modern(legacy,6,false,"","");check(queued(input)==input,"full source override wins");flag("dictionary",db);flag("translateGameChat",false);check(queued(input)==input,"game notification switch");flag("translateGameChat",true);flag("translateNpcChat",false);check(plain(queued(input)).equals(target),"system notification independent of NPC switch");flag("translateNpcChat",true);'
    assert s.count(old)==1;s=s.replace(old,new)
    s=s.replace('check(count==206,"103 archival literals,each primary and published layout");',f'check(count=={stats["cases"]},"119 exact notifications,each primary and published layout");')
    start=s.index('        var input=Component.literal("[NPC] Baker: My ")')
    end=s.index('        System.out.println(',start)
    s=s[:start]+'''        var input=Component.literal("Your ").setStyle(Style.EMPTY.withColor(0x123456)).append(Component.literal("armadillo is full of energy!").setStyle(Style.EMPTY.withColor(0x654321)));check(story.translate(input,db)==input,"unreviewed semantic split not guessed");
        for(String value:List.of("[NPC] Unknown: Your armadillo is full of energy!","[VIP] Player: Your armadillo is full of energy!","Future system notification not yet published.","[04:05] Your armadillo is full of energy!","Your armadillo is full of energy! (y17)")){var original=Component.literal(value);check(queued(original)==original,"unknown source/prefix/counter not guessed");}
        var translatable=Component.translatable("unknown.system107","Your armadillo is full of energy!");check(story.translate(translatable,db)==translatable,"non-literal tree protected");
'''+s[end:]
    s=s.replace('103 archival targets;206 primary/published fixtures','119 exact system notifications;238 primary/published fixtures').replace('independently authored106 flat target','independently authored107 flat target')
    (ROOT/'tests/TestSystem107.java').write_text(s,encoding='utf-8')
    snapshot=ROOT/'translation-source-regression107-v2';snapshot.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,snapshot/p.name)
    super105=json.loads((ROOT/'translation-source-regression106/EXPECTATION_SUPERSESSION105.json').read_text(encoding='utf-8'))
    for r in super105['changed_files']:
        assert sha(SOURCE/r['file'])==r['historical_sha256'];shutil.copy2(ROOT/'translation-source-regression106'/r['file'],snapshot/r['file'])
    shutil.copy2(ROOT/'translation-source-regression106/EXPECTATION_SUPERSESSION105.json',snapshot/'EXPECTATION_SUPERSESSION105.json')
    generated=ROOT/'build/system107-test-sources-v2';generated.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        old=p.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)10525\b','10628',old)
        (generated/p.name).write_text(new,encoding='utf-8')
        if n:changes.append({'file':p.name,'replacements':n,'source_sha256':sha(p),'generated_sha256':sha(generated/p.name)})
    assert len(changes)==29
    write(generated/'COUNT_EXPECTATION_UPDATES.json',{'scope':'Same29 historical story cardinality assertions as106:10525->10628 only. No additional wording or style expectation changes.','files':changes})
    s=(ROOT/'tools/check_npc106.ps1').read_text(encoding='utf-8-sig')
    s=s.replace('0.15.106-dev','0.15.107-dev').replace('npc106-checks-','system107-checks-').replace('NPC106','SYSTEM107').replace('TestNpcCorpusParser106','TestNpcCorpusParser107')
    s=s.replace('build/npc105-checks-0a8d81b407df4fc4b4ccaa0b00ac2052','build/npc106-checks-18089a61d6f44d749d0337c2b08ef206')
    s=s.replace("@('TestNpc106','TestNpcChoices106')","@('TestSystem107')").replace('-ne 179','-ne 180').replace('/179','/180')
    s=s.replace('build/npc106-test-sources','build/system107-test-sources-v2').replace('translation-source-regression106','translation-source-regression107-v2')
    (ROOT/'tools/check_system107.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/dispatch_npc106.ps1').read_text(encoding='utf-8-sig').replace('0.15.106-dev','0.15.107-dev').replace('0.15.105-dev','0.15.106-dev').replace('npc106-dispatch-','system107-dispatch-').replace('NPC106','SYSTEM107')
    (ROOT/'tools/dispatch_system107.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tests/BenchmarkNpc106Verify.java').read_text(encoding='utf-8-sig').replace('BenchmarkNpc106Verify','BenchmarkSystem107')
    marker='        var bean=(com.sun.management.ThreadMXBean)'
    assert s.count(marker)==1
    addition='''        var retainedGoals=new ArrayList<String>();try(var r=new java.io.InputStreamReader(BenchmarkSystem107.class.getResourceAsStream("/data/witherskyru/quest-objectives.json"),java.nio.charset.StandardCharsets.UTF_8)){var goals=JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("records");for(int i=0;i<3;i++)retainedGoals.add(goals.get(i).getAsJsonObject().get("source").getAsString());}
        inputs.put("retained_system_goals",retainedGoals);inputs.put("unknown_system_notifications",List.of("Future system notification not yet published.","✆ Unpublished future Abiphone message."));
'''
    s=s.replace(marker,addition+marker)
    needle='"existing_choices90","existing_npc102_equal_work")';assert s.count(needle)==1
    s=s.replace(needle,'"existing_choices90","existing_npc102_equal_work","retained_system_goals","unknown_system_notifications")').replace('Focused10 equal-work modes','Focused12 equal-work modes')
    (ROOT/'tests/BenchmarkSystem107.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_npc106_verify.ps1').read_text(encoding='utf-8-sig').replace('0.15.106-dev','0.15.107-dev').replace('0.15.105-dev','0.15.106-dev').replace('npc106-verify-','system107-verify-').replace('BenchmarkNpc106Verify','BenchmarkSystem107').replace('NPC106','SYSTEM107').replace('JVMs105,106,106,105','JVMs106,107,107,106').replace('ten equal-work modes','twelve equal-work modes')
    (ROOT/'tools/benchmark_system107.ps1').write_text(s,encoding='utf-8')
    print('238 source/style cases;180 groups;all prior fixture targets retained;12 equal-output benchmark modes.')

if __name__=='__main__':main()
