"""Mechanical new batch harnesses; preserve old oracles with explicit count-only copies."""
import argparse,json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write
def snapshots():
    s=(ROOT/'tests/AuditSystem107.java').read_text(encoding='utf-8-sig').replace('AuditSystem107','AuditSkins109').replace('SYSTEM_CANDIDATES_0.15.107','SKINS_CANDIDATES_0.15.109').replace('SYSTEM107','SKINS109');(ROOT/'tests/AuditSkins109.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/snapshot_system107.ps1').read_text(encoding='utf-8-sig').replace("@('0.15.106-dev')","@('0.15.108-dev')").replace('system107-snapshots','skins109-snapshots').replace('AuditSystem107','AuditSkins109').replace('SYSTEM107','SKINS109');(ROOT/'tools/snapshot_skins109.ps1').write_text(s,encoding='utf-8')
def main(revision):
    snapshots();stats=json.loads((SOURCE/'SKINS_PREPARATION_0.15.109.json').read_text(encoding='utf-8'));assert stats['cases']==1614
    s=(ROOT/'tests/TestSystem107.java').read_text(encoding='utf-8-sig').replace('TestSystem107','TestSkins109').replace('SYSTEM107','SKINS109').replace('SYSTEM_CASES_0.15.107','SKINS_CASES_0.15.109').replace('story.phraseCount()==148','story.phraseCount()==952').replace('29 retained goals plus119 exact system notifications','148 retained plus804 exact cosmetic notifications').replace('count==238','count==1614').replace('119 exact notifications,each primary and published layout','804 notifications,each primary and all publication layouts').replace('119 exact system notifications;238 primary/published fixtures','804 cosmetic notifications;1614 primary/published fixtures').replace('independently authored107','authored finite109')
    (ROOT/'tests/TestSkins109.java').write_text(s,encoding='utf-8')
    dataset='translation-source-regression109'+('' if revision==1 else '-v'+str(revision))
    sources='build/skins109-test-sources'+('' if revision==1 else '-v'+str(revision))
    snapshot=ROOT/dataset;snapshot.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,snapshot/p.name)
    prior=ROOT/'translation-source-regression108';sup=json.loads((prior/'EXPECTATION_SUPERSESSION105.json').read_text(encoding='utf-8'))
    for r in sup['changed_files']:shutil.copy2(prior/r['file'],snapshot/r['file'])
    shutil.copy2(prior/'EXPECTATION_SUPERSESSION105.json',snapshot/'EXPECTATION_SUPERSESSION105.json')
    generated=ROOT/sources;generated.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        old=p.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)10525\b','10628',old)
        if p.name=='TestNpc98.java':new,n2=re.subn(r'phraseCount\(\)==358','phraseCount()==460',new);assert n2==1;n+=n2
        if p.name=='TestDungeonChatResources.java':new,n2=re.subn(r'entries.size\(\)\+336','entries.size()+438',new);assert n2==1;n+=n2
        if p.name=='TestSystem107.java':new,n2=re.subn(r'phraseCount\(\)==148','phraseCount()==952',new);assert n2==1;n+=n2
        (generated/p.name).write_text(new,encoding='utf-8')
        if n:changes.append({'file':p.name,'replacements':n,'source_sha256':sha(p),'generated_sha256':sha(generated/p.name)})
    assert len(changes)==32;write(generated/'COUNT_EXPECTATION_UPDATES.json',{'scope':'31 earlier count-only copies plus TestSystem107 provider148->952. No source/target/style golden changes.','files':changes})
    s=(ROOT/'tools/check_boss108.ps1').read_text(encoding='utf-8-sig').replace('0.15.108-dev','0.15.109-dev').replace('boss108-checks-','skins109-checks-').replace('BOSS108','SKINS109').replace('TestNpcCorpusParser108','TestNpcCorpusParser109').replace('build/boss108-test-sources',sources).replace('translation-source-regression108',dataset).replace("@('TestBoss108','TestSystem107')","@('TestSkins109','TestBoss108','TestSystem107')").replace('-ne 181','-ne 182').replace('/181','/182');(ROOT/'tools/check_skins109.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/dispatch_boss108.ps1').read_text(encoding='utf-8-sig').replace('0.15.108-dev','0.15.109-dev').replace('0.15.107-dev','0.15.108-dev').replace('boss108-dispatch-','skins109-dispatch-').replace('BOSS108','SKINS109');(ROOT/'tools/dispatch_skins109.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tests/BenchmarkBoss108.java').read_text(encoding='utf-8-sig').replace('BenchmarkBoss108','BenchmarkSkins109')
    marker='        var bean=(com.sun.management.ThreadMXBean)';assert s.count(marker)==1
    addition='''        var retainedCosmetics=new ArrayList<String>();try(var r=new java.io.InputStreamReader(BenchmarkSkins109.class.getResourceAsStream("/data/witherskyru/system-expansion107.json"),java.nio.charset.StandardCharsets.UTF_8)){var list=JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("records");for(int i=list.size()-3;i<list.size();i++)retainedCosmetics.add(list.get(i).getAsJsonObject().get("source").getAsString());}inputs.put("retained_cosmetic_notifications",retainedCosmetics);
'''
    s=s.replace(marker,addition+marker).replace('"retained_boss_dialogues","unknown_boss_dialogues")','"retained_boss_dialogues","unknown_boss_dialogues","retained_cosmetic_notifications")').replace('Focused14','Focused15');(ROOT/'tests/BenchmarkSkins109.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_boss108.ps1').read_text(encoding='utf-8-sig').replace('0.15.108-dev','0.15.109-dev').replace('0.15.107-dev','0.15.108-dev').replace('boss108-verify-','skins109-verify-').replace('BenchmarkBoss108','BenchmarkSkins109').replace('BOSS108','SKINS109').replace('JVMs107,108,108,107','JVMs108,109,109,108').replace('fourteen equal-work','fifteen equal-work');(ROOT/'tools/benchmark_skins109.ps1').write_text(s,encoding='utf-8')
    print('1614 cases;182 groups;32 count-only copies;15 equal-output performance modes')
if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--snapshots-only',action='store_true');ap.add_argument('--revision',type=int,default=1);a=ap.parse_args()
    if a.snapshots_only:snapshots()
    else:main(a.revision)
