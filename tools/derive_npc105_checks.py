"""Reuse strict glyph/action assertions with eleven independently authored revisions."""
from prepare_npc78 import ROOT

def main():
    s=(ROOT/'tests/TestNpcStyles104.java').read_text(encoding='utf-8-sig')
    s=s.replace('TestNpcStyles104','TestNpcWording105').replace('NPCStyles104','NPCWording105')
    start=s.index('        var frozen=');end=s.index('        int count=0;',start)
    s=s[:start]+'        var frozen=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_WORDING_CASES_0.15.105.json"))).getAsJsonObject().getAsJsonArray("cases");\n'+s[end:]
    s=s.replace('NPC_STYLES_0.15.104_REVIEWED.tsv','NPC_WORDING_STYLES_0.15.105.tsv')
    s=s.replace('legacy=evidence.get(source).get("source_legacy_line").getAsString().strip();check(tested.add(source),"unique reviewed style source");',
                'legacy=previous.get("legacy").getAsString();check(tested.add(source+"/"+previous.get("kind").getAsString()),"unique source/layout pair");')
    s=s.replace('check(count==67,"67 reviewed alternate layouts;five prose revisions explicitly pending");','check(count==22,"11 explicitly revised targets, each primary and published layout");')
    s=s.replace('prior flat target unchanged','explicit105 revised flat target').replace('67 independently authored published layouts,old targets intact','11 revised targets;22 published/primary fixtures')
    s=s.replace('fresh-style104','fresh-style105').replace('unchanged-style104','unchanged-style105').replace('hint104','hint105')
    (ROOT/'tests/TestNpcWording105.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/check_npc104.ps1').read_text(encoding='utf-8-sig')
    s=s.replace('0.15.104-dev','0.15.105-dev').replace('npc104-checks-','npc105-checks-').replace('NPC104','NPC105')
    s=s.replace('build/npc103-checks-d681f19ccb984d7b8488695169ed7b78','build/npc104-checks-0ac8d09677ec435c8a4e23c33f06ed07')
    s=s.replace("@('TestNpcStyles104')","@('TestNpcWording105')")
    s=s.replace('-ne 176','-ne 177').replace('/176','/177')
    s=s.replace('"$taskProject/translation-source"','"$taskProject/translation-source-regression105"')
    s=s.replace('TestNpcCorpusParser104','TestNpcCorpusParser105')
    s=s.replace('installed_pack_modified=$false;', 'expectation_supersession_manifest="$taskProject/translation-source-regression105/EXPECTATION_SUPERSESSION105.json";installed_pack_modified=$false;')
    (ROOT/'tools/check_npc105.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/dispatch_npc104.ps1').read_text(encoding='utf-8-sig')
    s=s.replace('0.15.104-dev','0.15.105-dev').replace('0.15.103-dev','0.15.104-dev').replace('npc104-dispatch-','npc105-dispatch-').replace('NPC104','NPC105')
    (ROOT/'tools/dispatch_npc105.ps1').write_text(s,encoding='utf-8')
    s=(ROOT/'tests/BenchmarkNpc104Verify.java').read_text(encoding='utf-8-sig').replace('BenchmarkNpc104Verify','BenchmarkNpc105Verify')
    start=s.index('        var repairs=');end=s.index('        var bean=',start)
    s=s[:start]+'        // Historical104 style expectations are intentionally superseded for six rows.\n        // All revised primary/published layouts are covered by TestNpcWording105.\n'+s[end:]
    (ROOT/'tests/BenchmarkNpc105Verify.java').write_text(s,encoding='utf-8')
    s=(ROOT/'tools/benchmark_npc104_verify.ps1').read_text(encoding='utf-8-sig')
    s=s.replace('0.15.104-dev','0.15.105-dev').replace('0.15.103-dev','0.15.104-dev').replace('npc104-verify-','npc105-verify-').replace('BenchmarkNpc104Verify','BenchmarkNpc105Verify').replace('NPC104','NPC105').replace('JVMs103,104,104,103','JVMs104,105,105,104')
    (ROOT/'tools/benchmark_npc105_verify.ps1').write_text(s,encoding='utf-8')
    print('Strict22-case wording fixture;177 groups with explicitly labelled eleven-target golden supersession;historical test sources untouched.')

if __name__=='__main__':main()
