"""Mechanical audit/fixture harnesses for gameplay notifications; no runtime logic."""
import json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write
def snapshots():
    s=(ROOT/'tests/AuditSystem107.java').read_text(encoding='utf-8-sig').replace('AuditSystem107','AuditGame110').replace('SYSTEM_CANDIDATES_0.15.107','GAME_SELECTED_0.15.110').replace('SYSTEM107','GAME110');(ROOT/'tests/AuditGame110.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/snapshot_system107.ps1').read_text(encoding='utf-8-sig').replace("@('0.15.106-dev')","@('0.15.109-dev')").replace('system107-snapshots','game110-snapshots').replace('AuditSystem107','AuditGame110').replace('SYSTEM107','GAME110');(ROOT/'tools/snapshot_game110.ps1').write_text(s,encoding='utf-8')
def main():
    snapshots();stats=json.loads((SOURCE/'GAME_PREPARATION_0.15.110.json').read_text(encoding='utf-8'));assert stats['cases']==1081
    s=(ROOT/'tests/TestSystem107.java').read_text(encoding='utf-8-sig').replace('TestSystem107','TestGame110').replace('SYSTEM107','GAME110').replace('SYSTEM_CASES_0.15.107','GAME_CASES_0.15.110').replace('story.phraseCount()==148','story.phraseCount()==1492').replace('29 retained goals plus119 exact system notifications','952 retained plus540 exact gameplay messages').replace('count==238','count==1081').replace('119 exact notifications,each primary and published layout','540 messages,each primary and all publication layouts').replace('119 exact system notifications;238 primary/published fixtures','540 gameplay messages;1081 primary/published fixtures').replace('independently authored107','individually authored110')
    (ROOT/'tests/TestGame110.java').write_text(s,encoding='utf-8')
    dataset=ROOT/'translation-source-regression110-v2';dataset.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,dataset/p.name)
    prior=ROOT/'translation-source-regression109-v3';sup=json.loads((prior/'EXPECTATION_SUPERSESSION105.json').read_text(encoding='utf-8'))
    for r in sup['changed_files']:shutil.copy2(prior/r['file'],dataset/r['file'])
    shutil.copy2(prior/'EXPECTATION_SUPERSESSION105.json',dataset/'EXPECTATION_SUPERSESSION105.json')
    generated=ROOT/'build/game110-test-sources-v2';generated.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        old=p.read_text(encoding='utf-8-sig');new,n=re.subn(r'(?<=phraseCount\(\)==)10525\b','10628',old)
        if p.name=='TestNpc98.java':new,n2=re.subn(r'phraseCount\(\)==358','phraseCount()==460',new);assert n2==1;n+=n2
        if p.name=='TestDungeonChatResources.java':new,n2=re.subn(r'entries.size\(\)\+336','entries.size()+438',new);assert n2==1;n+=n2
        if p.name=='TestSystem107.java':new,n2=re.subn(r'phraseCount\(\)==148','phraseCount()==1492',new);assert n2==1;n+=n2
        if p.name=='TestSkins109.java':new,n2=re.subn(r'phraseCount\(\)==952','phraseCount()==1492',new);assert n2==1;n+=n2
        (generated/p.name).write_text(new,encoding='utf-8')
        if n:changes.append({'file':p.name,'replacements':n,'source_sha256':sha(p),'generated_sha256':sha(generated/p.name)})
    assert len(changes)==33;write(generated/'COUNT_EXPECTATION_UPDATES.json',{'scope':'33 isolated count-only copies;no source/target/style golden changes. SYSTEM provider1492 keys.','files':changes})
    s=(ROOT/'tools/check_skins109.ps1').read_text(encoding='utf-8-sig').replace('0.15.109-dev','0.15.110-dev').replace('skins109-checks-','game110-checks-').replace('SKINS109','GAME110').replace('TestNpcCorpusParser109','TestNpcCorpusParser110').replace('build/skins109-test-sources-v3','build/game110-test-sources-v2').replace('translation-source-regression109-v3','translation-source-regression110-v2').replace("@('TestSkins109','TestBoss108','TestSystem107')","@('TestGame110','TestSkins109','TestBoss108','TestSystem107')").replace('-ne 182','-ne 183').replace('/182','/183');(ROOT/'tools/check_game110.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/dispatch_skins109.ps1').read_text(encoding='utf-8-sig').replace('0.15.109-dev','0.15.110-dev').replace('0.15.108-dev','0.15.109-dev').replace('skins109-dispatch-','game110-dispatch-').replace('SKINS109','GAME110');(ROOT/'tools/dispatch_game110.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tests/BenchmarkSkins109.java').read_text(encoding='utf-8-sig').replace('BenchmarkSkins109','BenchmarkGame110');(ROOT/'tests/BenchmarkGame110.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_skins109.ps1').read_text(encoding='utf-8-sig').replace('0.15.109-dev','0.15.110-dev').replace('0.15.108-dev','0.15.109-dev').replace('skins109-verify-','game110-verify-').replace('BenchmarkSkins109','BenchmarkGame110').replace('SKINS109','GAME110').replace('JVMs108,109,109,108','JVMs109,110,110,109');(ROOT/'tools/benchmark_game110.ps1').write_text(s,encoding='utf-8')
    print('1081 fixtures;183 fresh groups;33 count-only copies;15 equal-output performance modes')
if __name__=='__main__':main()
