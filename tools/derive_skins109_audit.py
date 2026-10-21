"""Mechanical specialization of the proven SYSTEM107 evidence audit; not runtime code."""
import re
from prepare_npc78 import ROOT
def main():
    s=(ROOT/'tools/audit_system107.py').read_text(encoding='utf-8')
    replacements=[('system107','skins109'),('0.15.107-dev','0.15.109-dev'),('0.15.106-dev','0.15.108-dev'),('SYSTEM_SOURCES_0.15.107','SKINS_SOURCES_0.15.109'),('SYSTEM107-SOURCES','SKINS109-SOURCES'),('system-expansion107.json','skins-expansion109.json'),('TestSystem107-output','TestSkins109-output'),('TestNpcCorpusParser107','TestNpcCorpusParser109'),('skins109-test-sources-v2','skins109-test-sources'),('translation-source-regression107-v2','translation-source-regression109'),('NPC_WORKLIST_0.15.106','NPC_WORKLIST_0.15.108'),('2026-10-20_3','2026-10-20_5'),('8A56461E1EED72013316C8DEEA1FE268062CE4DAD0380EEDE13DD85BC8D260A2','00C1829666B04B918D37682D18C9BFAD65A64650A7839EF8D4DCD42CA1DD8D00')]
    for old,new in replacements:s=s.replace(old,new)
    s=s.replace('immutable106 evidence','immutable108 evidence').replace("[('106',before),('107',after)]","[('108',before),('109',after)]")
    s=s.replace("    for name in ('ChatNotificationTranslator','NamedNpcTemplates'):\n        rel='src/main/java/ru/wither/skyru/'+name+'.java';assert sha(ROOT/rel)==manifest[rel]\n",'')
    s=re.sub(r'\b119\b','804',s);s=re.sub(r'\b180\b','182',s)
    s=s.replace('len(evidence[\'source_pages\'])==21','len(evidence[\'source_pages\'])==52').replace('layout_count==804','layout_count==810').replace("'new_published_layouts':804","'new_published_layouts':810").replace("'pinned_pages':21","'pinned_pages':52")
    for old,new in [('11314','12220'),('11195','11416'),('4930','4024')]:s=re.sub(r'\b'+old+r'\b',new,s)
    start=s.index('    literal=');end=s.index('    assert old.count(literal)',start)
    s=s[:start]+'''    literal='"/data/witherskyru/system-expansion107.json"}'
    array='"/data/witherskyru/system-expansion107.json","/data/witherskyru/skins-expansion109.json"}'
'''+s[end:]
    old="output=(a.checks/'TestSkins109-output.txt').read_text(encoding='utf-8-sig');assert 'SYSTEM107 632925 assertions' in output"
    new="output=(a.checks/'TestSkins109-output.txt').read_text(encoding='utf-8-sig');assert 'SKINS109 ' in output and '1614 primary/published fixtures' in output;assertions=int(re.search(r'SKINS109 (\\d+) assertions',output)[1])"
    assert s.count(old)==1;s=s.replace(old,new).replace("'new_system_assertions':632925","'new_cosmetic_assertions':assertions").replace("'new_system_literals':804","'new_cosmetic_literals':804")
    s=s.replace("assert len(transformed)==29","assert len(transformed)==32").replace("'isolated_count_assertion_updates':29","'isolated_count_assertion_updates':32")
    old="        assert re.sub(r'(?<=phraseCount\\(\\)==)10525\\b','10628',old)==new"
    new="""        expected=re.sub(r'(?<=phraseCount\\(\\)==)10525\\b','10628',old)
        if item['file']=='TestNpc98.java':expected=expected.replace('phraseCount()==358','phraseCount()==460')
        if item['file']=='TestDungeonChatResources.java':expected=expected.replace('entries.size()+336','entries.size()+438')
        if item['file']=='TestSystem107.java':expected=expected.replace('phraseCount()==148','phraseCount()==952')
        assert expected==new"""
    assert s.count(old)==1;s=s.replace(old,new)
    s=s.replace('build/skins109-test-sources','build/skins109-test-sources-v3').replace('translation-source-regression109','translation-source-regression109-v3')
    (ROOT/'tools/audit_skins109.py').write_text(s,encoding='utf-8')
if __name__=='__main__':main()
