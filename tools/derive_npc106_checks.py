"""Explicitly extend strict source-backed fixtures and only their total-count contract."""
import json,re,shutil
from prepare_npc78 import ROOT,SOURCE,sha,write

def main():
    stats=json.loads((SOURCE/'NPC_PREPARATION_0.15.106.json').read_text(encoding='utf-8'))
    s=(ROOT/'tests/TestNpcWording105.java').read_text(encoding='utf-8-sig')
    s=s.replace('TestNpcWording105','TestNpc106').replace('NPCWording105','NPC106').replace('NPC_WORDING_CASES_0.15.105.json','NPC_CASES_0.15.106.json').replace('NPC_WORDING_STYLES_0.15.105.tsv','NPC_CASE_STYLES_0.15.106.tsv')
    s=s.replace('check(count==22,"11 explicitly revised targets, each primary and published layout");',f'check(count=={stats["cases"]},"103 archival literals,each primary and published layout");')
    s=s.replace('var story=NpcDialogueTranslator.storyDialogues();','var story=NpcDialogueTranslator.storyDialogues();check(story.phraseCount()==10628,"10525 retained plus103 authored archival keys");')
    s=s.replace('11 revised targets;22 published/primary fixtures','103 archival targets;206 primary/published fixtures').replace('explicit105 revised flat target','independently authored106 flat target')
    (ROOT/'tests/TestNpc106.java').write_text(s,encoding='utf-8')
    ref=json.loads((SOURCE/'NPC_CHOICES_SOURCES_0.15.106.json').read_text(encoding='utf-8'))
    layouts=sum(len({o['source_legacy_line'].strip() for o in r['all_occurrences']}) for r in ref['records'])
    s=(ROOT/'tests/TestNpcChoices102.java').read_text(encoding='utf-8-sig').replace('TestNpcChoices102','TestNpcChoices106').replace('NPCChoices102','NPCChoices106').replace('0.15.102','0.15.106')
    s=s.replace('groups==7&&buttons==7&&layouts==7&&malformed==0',f'groups==56&&buttons==75&&layouts=={layouts}&&malformed==0')
    s=s.replace('7 source groups/7 translated label slots;7 published layouts',f'56 source groups/75 translated label slots;{layouts} published layouts')
    s=s.replace('7 exact ordered groups/7 label slots/7 valid published layouts;7 changed labels',f'56 exact ordered groups/75 label slots/{layouts} valid published layouts;75 changed labels')
    (ROOT/'tests/TestNpcChoices106.java').write_text(s,encoding='utf-8')
    snapshot=ROOT/'translation-source-regression106';snapshot.mkdir(exist_ok=False)
    for p in SOURCE.iterdir():
        if p.is_file():shutil.copy2(p,snapshot/p.name)
    supersession=json.loads((ROOT/'translation-source-regression105/EXPECTATION_SUPERSESSION105.json').read_text(encoding='utf-8'))
    for item in supersession['changed_files']:
        assert sha(SOURCE/item['file'])==item['historical_sha256']
        shutil.copy2(ROOT/'translation-source-regression105'/item['file'],snapshot/item['file'])
    shutil.copy2(ROOT/'translation-source-regression105/EXPECTATION_SUPERSESSION105.json',snapshot/'EXPECTATION_SUPERSESSION105.json')
    # The original Java expectations stay intact. Compile isolated mechanical
    # copies with only phraseCount()==10525 changed to10628,not prose assertions.
    generated=ROOT/'build/npc106-test-sources';generated.mkdir(exist_ok=False);changes=[]
    for p in (ROOT/'tests').glob('*.java'):
        source=p.read_text(encoding='utf-8-sig');target,n=re.subn(r'(?<=phraseCount\(\)==)10525\b','10628',source)
        assert target.count('10525')==source.count('10525')-n
        (generated/p.name).write_text(target,encoding='utf-8')
        if n:changes.append({'file':p.name,'replacements':n,'source_sha256':sha(p),'generated_sha256':sha(generated/p.name)})
    write(generated/'COUNT_EXPECTATION_UPDATES.json',{'scope':'Only the explicit story-provider cardinality assertion changes10525->10628. Historical authored strings/styles are not replaced. Eleven wording supersessions from105 remain separately labelled.','files':changes})
    s=(ROOT/'tools/check_npc105.ps1').read_text(encoding='utf-8-sig')
    s=s.replace('0.15.105-dev','0.15.106-dev').replace('npc105-checks-','npc106-checks-').replace('NPC105','NPC106').replace('TestNpcCorpusParser105','TestNpcCorpusParser106')
    s=s.replace('build/npc104-checks-0ac8d09677ec435c8a4e23c33f06ed07','build/npc105-checks-0a8d81b407df4fc4b4ccaa0b00ac2052')
    s=s.replace("@('TestNpcWording105')","@('TestNpc106','TestNpcChoices106')").replace('-ne 177','-ne 179').replace('/177','/179')
    s=s.replace('"$taskProject/tests" -File','"$taskProject/build/npc106-test-sources" -File').replace('translation-source-regression105','translation-source-regression106')
    s=s.replace('installed_pack_modified=$false;','count_assertion_updates="$taskProject/build/npc106-test-sources/COUNT_EXPECTATION_UPDATES.json";installed_pack_modified=$false;')
    (ROOT/'tools/check_npc106.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/dispatch_npc105.ps1').read_text(encoding='utf-8-sig').replace('0.15.105-dev','0.15.106-dev').replace('0.15.104-dev','0.15.105-dev').replace('npc105-dispatch-','npc106-dispatch-').replace('NPC105','NPC106')
    (ROOT/'tools/dispatch_npc106.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tests/BenchmarkNpc105Verify.java').read_text(encoding='utf-8-sig').replace('BenchmarkNpc105Verify','BenchmarkNpc106Verify')
    (ROOT/'tests/BenchmarkNpc106Verify.java').write_text(s,encoding='utf-8')
    # The benchmark is a separately compiled harness,not a test group.
    s=(ROOT/'tools/benchmark_npc105_verify.ps1').read_text(encoding='utf-8-sig').replace('0.15.105-dev','0.15.106-dev').replace('0.15.104-dev','0.15.105-dev').replace('npc105-verify-','npc106-verify-').replace('BenchmarkNpc105Verify','BenchmarkNpc106Verify').replace('NPC105','NPC106').replace('JVMs104,105,105,104','JVMs105,106,106,105')
    (ROOT/'tools/benchmark_npc106_verify.ps1').write_text(s,encoding='utf-8')
    print(json.dumps({'new_groups':2,'all_groups':179,'choice_layouts':layouts,'isolated_count_assertion_files':len(changes),'count_assertions':sum(x['replacements'] for x in changes)},ensure_ascii=False))

if __name__=='__main__':main()
