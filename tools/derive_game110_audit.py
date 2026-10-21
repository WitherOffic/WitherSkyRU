"""Specialize the prior independent gate; retain immutable historical oracles."""
import re
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tools/audit_skins109.py').read_text(encoding='utf-8')
    for old,new in [('audit_skins109','audit_game110'),('0.15.109-dev','0.15.110-dev'),('0.15.108-dev','0.15.109-dev'),('SKINS_SOURCES_0.15.109','GAME_SOURCES_0.15.110'),('NPC_WORKLIST_0.15.108','NPC_WORKLIST_0.15.109'),('skins-expansion109.json','game-expansion110.json'),('SKINS109-SOURCES','GAME110-SOURCES'),('SKINS109','GAME110'),('TestSkins109-output','TestGame110-output'),('TestNpcCorpusParser109','TestNpcCorpusParser110'),('build/skins109-test-sources-v3','build/game110-test-sources'),('translation-source-regression109-v3','translation-source-regression110'),('2026-10-20_5','2026-10-20_6'),('00C1829666B04B918D37682D18C9BFAD65A64650A7839EF8D4DCD42CA1DD8D00','5263691ECFAF4CD57D60CCD69DE8C9DD05AE5D2167875DC17BC1AD77FF41CFE3')]:s=s.replace(old,new)
    s=s.replace('immutable108 evidence','immutable109 evidence').replace("[('108',before),('109',after)]","[('109',before),('110',after)]")
    start=s.index('    literal=');end=s.index('    assert old.count(literal)',start)
    s=s[:start]+'''    literal='"/data/witherskyru/skins-expansion109.json"}'
    array='"/data/witherskyru/skins-expansion109.json","/data/witherskyru/game-expansion110.json"}'
'''+s[end:]
    # Replace numbers by complete tokens, avoiding historical registry counts.
    for old,new in [('804','540'),('810','541'),('1614','1081'),('182','183'),('11416','12220'),('12220','12760'),('4024','3484'),('52','254'),('32','33')]:
        # simultaneous replacement below avoids transitive exact-before/after changes
        s=re.sub(r'\b'+old+r'\b','__NUMBER_'+old+'__',s)
    for old,new in [('804','540'),('810','541'),('1614','1081'),('182','183'),('11416','12220'),('12220','12760'),('4024','3484'),('52','254'),('32','33')]:s=s.replace('__NUMBER_'+old+'__',new)
    s=s.replace("expected=expected.replace('phraseCount()==148','phraseCount()==952')","expected=expected.replace('phraseCount()==148','phraseCount()==1492')")
    marker="        assert expected==new";assert s.count(marker)==1
    s=s.replace(marker,"        if item['file']=='TestSkins109.java':expected=expected.replace('phraseCount()==952','phraseCount()==1492')\n"+marker)
    s=s.replace('new_cosmetic_literals','new_gameplay_literals').replace('new_cosmetic_assertions','new_gameplay_assertions')
    s=s.replace('build/game110-test-sources','build/game110-test-sources-v2').replace('translation-source-regression110','translation-source-regression110-v2')
    (ROOT/'tools/audit_game110.py').write_text(s,encoding='utf-8')
if __name__=='__main__':main()
