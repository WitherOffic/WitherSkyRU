"""Frozen before/after inputs; newly translated styles are explicitly unequal useful work."""
from prepare_npc78 import ROOT

def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def main():
    s=(ROOT/'tests/BenchmarkNpc103.java').read_text(encoding='utf-8-sig').replace('BenchmarkNpc103','BenchmarkNpc104').replace('new_names103_unequal_work','existing_names103_equal_work')
    needle='        var bean=(com.sun.management.ThreadMXBean)'
    addition='''        var repairs=JsonParser.parseString(Files.readString(Path.of("translation-source/NPC_STYLE_SOURCES_0.15.104.json"))).getAsJsonObject().getAsJsonArray("records");
        var fixedStyles=new ArrayList<String>();boolean hasStyles=BenchmarkNpc104.class.getResource("/data/witherskyru/npc-styles104.json")!=null;
        for(var raw:repairs){var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();for(var legacy:row.getAsJsonArray("layouts")){fixedStyles.add(legacy.getAsString());var input=Component.literal(legacy.getAsString());String output=SkyRuClient.npcChat(new GuiMessage(0,input,null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString();if(!output.equals(hasStyles?target:source)&&!(hasStyles?target:source).equals(strip(output)))throw new AssertionError("Independent repaired-layout output");}}
        inputs.put("new_styles104_unequal_work",List.copyOf(fixedStyles));
'''
    # The wrapper's legacy components can retain § codes on103; compare visible text, not raw encodings.
    addition=addition.replace('!(hasStyles?target:source).equals(strip(output))','!(hasStyles?target:source).equals(output.replaceAll("(?i)§[0-9a-fk-or]",""))')
    assert needle in s;s=s.replace(needle,addition+needle)
    needle='result.addProperty("equal_useful_output",!entry.getKey().startsWith("new_"));'
    assert needle in s
    s=s.replace(needle,needle+'var useful=new JsonArray();for(var row:rows){String visible=SkyRuClient.npcChat(new GuiMessage(0,row,null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString().replaceAll("(?i)§[0-9a-fk-or]","");useful.add(visible);}result.add("useful_visible_outputs",useful);')
    s=s.replace('36 equal-work plus1 explicitly unequal new mode102/103','37 equal-work plus1 explicitly unequal new style mode103/104').replace('All36 existing modes have equal useful output;135 names translate only on103','All37 existing modes must have equal useful output,checked by recorded output lists;67 style repairs translate only on104')
    emit('tests/BenchmarkNpc104.java',s)
    verify=s.replace('BenchmarkNpc104','BenchmarkNpc104Verify')
    start='Set.of("unknown","existing_style","existing_choices","existing_choices84","existing_choices91","existing_choices92")'
    assert start in verify
    verify=verify.replace(start,'Set.of("unknown","existing_style","existing_choices87","existing_choices88","existing_large_batch88","existing_dynamic","unknown_named_speaker","existing_large_batch87","existing_choices90","existing_npc102_equal_work")').replace('Focused6 equal-work modes','Focused10 equal-work modes')
    emit('tests/BenchmarkNpc104Verify.java',verify)
    for source,target in [('benchmark_npc103.ps1','benchmark_npc104.ps1'),('benchmark_npc103_verify.ps1','benchmark_npc104_verify.ps1')]:
        s=(ROOT/'tools'/source).read_text(encoding='utf-8-sig').replace('0.15.103-dev','0.15.104-dev').replace('0.15.102-dev','0.15.103-dev').replace('npc103-benchmark','npc104-benchmark').replace('npc103-verify','npc104-verify').replace('BenchmarkNpc103','BenchmarkNpc104').replace('NPC103','NPC104')
        s=s.replace('BD99E5491FACF8B6E5AA1AA730F33DF119CC6DFB7EFA272257DB49E29B3EB0BD','01E04AD01F2CF57B09CD5AF4A28093BC6130D48D4D79B2D5748C45D59ECFF028').replace('each102,103,103,102','each103,104,104,103').replace('JVMs102,103,103,102','JVMs103,104,104,103')
        emit('tools/'+target,s)
    print('38 receipt modes:37 equal-output,one explicit new-style mode;HUD10,mixed5;focused10.')

if __name__=='__main__':main()
