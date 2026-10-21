"""Fresh isolated115 tests. Historical sources and positive golden files stay immutable."""
import re,shutil
from prepare_npc78 import ROOT,SOURCE,write,sha
from prepare_npc113 import load

NANCY_OLD='for(var raw:reference.getAsJsonArray("extraction_pending")){String s=raw.getAsJsonObject().get("source").getAsString();var c=Component.literal(s);check(!packaged.containsKey(s)&&story.translate(c,db)==c,"malformed/extraction candidate not registered");}'
NANCY_NEW='for(var raw:reference.getAsJsonArray("extraction_pending")){String s=raw.getAsJsonObject().get("source").getAsString();var c=Component.literal(s);if(s.equals("[NPC] Mayor Nancy: Hi|")){check(!packaged.containsKey(s)&&plain(story.translate(c,db)).equals("[NPC] Mayor Nancy: Привет|"),"115 source-backed nowiki pipe recovered");}else if(s.equals("[NPC] Generow: Click to claim your reward!")){check(!packaged.containsKey(s)&&plain(story.translate(c,db)).equals("[NPC] Generow: Нажми, чтобы забрать награду!"),"115 complete message recovered before next full speaker separator");}else check(!packaged.containsKey(s)&&story.translate(c,db)==c,"malformed/extraction candidate not registered: "+s);}'

def nancy_positive_upgrade(text):
    assert text.count(NANCY_OLD)==1,'Only the exact historical negative loop may be superseded'
    text=text.replace(NANCY_OLD,NANCY_NEW)
    marker='        default -> null;\n    };}'
    assert text.count(marker)==1
    # Complete second Andrew sentence is now independently source-backed. The
    # first sentence with a trailing extraction slash remains a strict negative.
    addition='        case "[NPC] Andrew: If you\'d like to discuss SkyBlock with other players then check out the SkyBlock section of the forums!" -> "[NPC] Andrew: Хочешь обсудить SkyBlock с другими игроками? Загляни в раздел SkyBlock на форумах!";\n'
    return text.replace(marker,addition+marker)

def isolated_positive_upgrade(filename,text):
    if filename=='TestNpc102.java':return nancy_positive_upgrade(text)
    if filename=='TestNpc92.java':
        assert text.count(NANCY_OLD)==1
        replacement='for(var raw:reference.getAsJsonArray("extraction_pending")){String s=raw.getAsJsonObject().get("source").getAsString();var c=Component.literal(s);if(s.equals("[NPC] Generow: Click to claim your reward!")){check(!packaged.containsKey(s)&&plain(story.translate(c,db)).equals("[NPC] Generow: Нажми, чтобы забрать награду!"),"115 complete source-backed Generow message");}else check(!packaged.containsKey(s)&&story.translate(c,db)==c,"malformed/extraction candidate not registered: "+s);}'
        return text.replace(NANCY_OLD,replacement)
    if filename=='TestNpc85.java':
        marker='        default -> null;\n    };}';assert text.count(marker)==1
        additions='        case "[NPC] Jerry: Your SkyBlock island is part of a much larger universe... [1/2]" -> "[NPC] Jerry: Твой остров SkyBlock — часть гораздо большей вселенной... [1/2]";\n        case "[NPC] Jerry: Wait, how did I get here? [2/2]" -> "[NPC] Jerry: Постой, как я сюда попал? [2/2]";\n'
        return text.replace(marker,additions+marker)
    return text

def repair_existing_derived():
    # Historical tests and golden data are never edited. Five distinct literal
    # sources across six negative fixtures now require exact authored positives.
    copies=ROOT/'build/npc115-test-sources';manifest=load(copies/'EXPECTATION_UPDATES.json')
    for filename in ('TestNpc102.java','TestNpc92.java','TestNpc85.java'):
        p=copies/filename;record=next(r for r in manifest['files'] if r['file']==p.name);assert sha(ROOT/'tests'/p.name)==record['original_sha256']
        baseline=ROOT/record['baseline_derived_path'];assert sha(baseline)==record['baseline_derived_sha256']
        old=baseline.read_text(encoding='utf-8-sig');old,n=re.subn(r'(?<=phraseCount\(\)==)10764\b','10799',old);assert n==record['new_story_count_replacements']
        p.write_text(isolated_positive_upgrade(filename,old),encoding='utf-8')
        record['generated_sha256']=sha(p);record['explicit_positive_upgrade']={'TestNpc102.java':['Nancy Hi|','Generow complete claim','Andrew complete second sentence'],'TestNpc92.java':['Generow complete claim'],'TestNpc85.java':['Jerry complete part1/2','Jerry complete part2/2']}[filename]
    manifest['scope']='Preserves114 isolated expectations and105 supersession. Story cardinality10764→10799. Five exact source-backed positives across six former negative fixtures in three isolated test copies;all other negatives unchanged.'
    write(copies/'EXPECTATION_UPDATES.json',manifest)

def main():
    s=(ROOT/'tests/TestEggs114.java').read_text(encoding='utf-8-sig')
    for old,new in [('TestEggs114','TestNpcLiterals115'),('EGGS114','NPC_LITERALS115'),('EGG_CASES_0.15.114','NPC_RECOVERY_CASES_0.15.115'),('phraseCount()==10764','phraseCount()==10799'),('count==4480','count==70'),('4480 primary/composition-style fixtures','70 primary/published fixtures'),('1568 records,all primary and explicit publication-composition layouts','35 recovered complete NPC messages,primary and publication layouts')]:s=s.replace(old,new)
    s=s.replace('        TestEggTargets114.verify(sourceRoot);','        TestNpcNumbers115.verifyAuthored(sourceRoot);')
    (ROOT/'tests/TestNpcLiterals115.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tests/TestNpc113.java').read_text(encoding='utf-8-sig');s=s[:s.index('        for(var raw:data.getAsJsonArray("retained_not_new"))')]+'''        check(tested==13&&layouts==13&&fixes==0,"13 reviewed integer shapes");
        verifyAuthored(root);
        System.out.println("PASS: NPC_NUMBERS115 "+TestNpcNames103.checks+" assertions;13 bounded number templates;current glyph styles/actions,bounds,overrides,channels;not FPS");
    }
    static void verifyAuthored(Path root)throws Exception{
        var author=JsonParser.parseString(Files.readString(root.resolve("NPC_RECOVERY_0.15.115_REVIEWED.json"))).getAsJsonObject();
        var evidence=JsonParser.parseString(Files.readString(root.resolve("NPC_RECOVERY_SOURCES_0.15.115.json"))).getAsJsonObject();
        var seen=new HashSet<String>();
        for(var raw:evidence.getAsJsonArray("records")){
            var r=raw.getAsJsonObject();String id=r.get("id").getAsString();check(seen.add(id),"unique independently authored ID");var a=author.getAsJsonArray(id);
            check(r.get("translation").getAsString().equals("[NPC] "+r.get("speaker").getAsString()+": "+a.get(0).getAsString()),"full target independently recovered from author file");
            var defs=a.get(1).getAsJsonObject();check(defs.size()==r.getAsJsonArray("style_anchors").size(),"all authored highlights retained");
            for(var d:r.getAsJsonArray("style_anchors")){var p=d.getAsJsonObject();check(defs.get(p.get("source").getAsString()).getAsString().equals(p.get("target").getAsString()),"independent authored semantic target");}
        }
        check(seen.size()==48&&seen.size()==author.size(),"35 literals+13 bounded shapes,not48 frozen exact keys");
    }
}
'''
    s=s.replace('TestNpc113','TestNpcNumbers115').replace('NPC_STRUCTURED_SOURCES_0.15.113','NPC_RECOVERY_SOURCES_0.15.115')
    s=s.replace('            var r=raw.getAsJsonObject();var row=row(r);','            var r=raw.getAsJsonObject();if(!r.get("kind").getAsString().equals("integer"))continue;var row=row(r);')
    s=s.replace('        check(tested==95&&layouts==95&&fixes==2,"95 patterns,not95 exact-key additions;2 existing-text color fixes");','')
    (ROOT/'tests/TestNpcNumbers115.java').write_text(s,encoding='utf-8')
    dataset=ROOT/'translation-source-regression115';dataset.mkdir(exist_ok=False)
    for p in (ROOT/'translation-source-regression114').iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    for p in SOURCE.glob('*0.15.115*'):shutil.copy2(p,dataset/p.name)
    copies=ROOT/'build/npc115-test-sources';copies.mkdir(exist_ok=False);changed=[]
    for p in (ROOT/'tests').glob('*.java'):
        prior=ROOT/'build/eggs114-test-sources'/p.name;prior=prior if prior.exists() else p
        old=prior.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)10764\b','10799',old)
        new=isolated_positive_upgrade(p.name,new)
        (copies/p.name).write_text(new,encoding='utf-8')
        if new!=p.read_text(encoding='utf-8-sig'):
            record={'file':p.name,'original_sha256':sha(p),'baseline_derived_path':prior.relative_to(ROOT).as_posix(),'baseline_derived_sha256':sha(prior),'generated_sha256':sha(copies/p.name),'new_story_count_replacements':n}
            if p.name in ('TestNpc102.java','TestNpc92.java','TestNpc85.java'):record['explicit_positive_upgrade']='Explicit source-backed positives only,see deterministic isolated_positive_upgrade().'
            changed.append(record)
    write(copies/'EXPECTATION_UPDATES.json',{'scope':'Preserves114 isolated expectations and105 supersession. Story cardinality10764→10799. Five exact source-backed positives across six fixtures;other negatives unchanged.','files':changed})
    s=(ROOT/'tools/check_eggs114.ps1').read_text(encoding='utf-8-sig')
    for old,new in [('0.15.114-dev','0.15.115-dev'),('eggs114-checks','npc115-checks'),('eggs114-test-sources','npc115-test-sources'),('translation-source-regression114','translation-source-regression115'),('TestNpcCorpusParser114','TestNpcCorpusParser115'),('EGGS114','NPC115'),("@('TestEggs114'","@('TestNpcLiterals115','TestNpcNumbers115','TestEggs114'"),('-ne 187','-ne 189'),('/187','/189')]:s=s.replace(old,new)
    (ROOT/'tools/check_npc115.ps1').write_text(s,encoding='utf-8')
    for src,dst in [('dispatch_eggs114.ps1','dispatch_npc115.ps1'),('benchmark_eggs114.ps1','benchmark_npc115.ps1'),('benchmark_eggs114_long.ps1','benchmark_npc115_long.ps1')]:
        s=(ROOT/'tools'/src).read_text(encoding='utf-8-sig')
        for old,new in [('0.15.114-dev','CANDIDATE'),('0.15.113-dev','0.15.114-dev'),('CANDIDATE','0.15.115-dev'),('eggs114-','npc115-'),('BenchmarkEggs114','BenchmarkNpc115'),('EGGS114','NPC115'),('JVMs113,114,114,113','JVMs114,115,115,114')]:s=s.replace(old,new)
        (ROOT/'tools'/dst).write_text(s,encoding='utf-8')
    for suffix in ('','Long'):
        s=(ROOT/f'tests/BenchmarkEggs114{suffix}.java').read_text(encoding='utf-8-sig').replace('BenchmarkEggs114','BenchmarkNpc115')
        (ROOT/f'tests/BenchmarkNpc115{suffix}.java').write_text(s,encoding='utf-8')
    write(SOURCE/'NPC_PERFORMANCE_CONTROL_0.15.115.json',{'scope':'Same17+6 equal-output modes as114,declared before results. No new runtime algorithms. Raw retained;not game FPS.','short_modes':17,'long_modes':load(SOURCE/'EGG_PERFORMANCE_CONTROL_0.15.114.json')['long_modes'],'order':['114','115','115','114']})
    print('189 fresh groups;',len(changed),'isolated copies;70 literal fixtures+13 numeric templates')

if __name__=='__main__':main()
