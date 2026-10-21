"""Mechanical derivation of before/after harnesses; no authored translations here."""
from prepare_npc78 import ROOT

def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def main():
    s=(ROOT/'tests/BenchmarkNpc102.java').read_text(encoding='utf-8-sig').replace('BenchmarkNpc102','BenchmarkNpc103').replace('new_npc102_unequal_work','existing_npc102_equal_work').replace('new_choices102_unequal_work','existing_choices102_equal_work')
    at='        var bean=(com.sun.management.ThreadMXBean)'
    assert at in s
    addition='''        inputs.put("unknown_named_speaker",List.of("[NPC] Kat: Future unpublished dialogue.","[NPC] Detective Amog: An unsupported sentence."));
        var namedSource=JsonParser.parseString(Files.readString(Path.of("translation-source/NPC_NAMES_SOURCES_0.15.103.json"))).getAsJsonObject().getAsJsonArray("records");
        var named=new ArrayList<String>();boolean hasNamed=BenchmarkNpc103.class.getResource("/data/witherskyru/npc-names103.json")!=null;
        for(var raw:namedSource){var row=raw.getAsJsonObject();String source=row.get("source").getAsString().replace("{name}","Wither_101"),target=row.get("translation").getAsString().replace("{name}","Wither_101");named.add(source);String output=SkyRuClient.npcChat(new GuiMessage(0,Component.literal(source),null,GuiMessageSource.SYSTEM_SERVER,null)).content().getString();if(!output.equals(hasNamed?target:source))throw new AssertionError("Independent named useful output");}
        inputs.put("new_names103_unequal_work",List.copyOf(named));
'''
    s=s.replace(at,addition+at).replace('33 equal-work plus2 explicitly unequal new modes101/102','36 equal-work plus1 explicitly unequal new mode102/103').replace('Only existing33 modes have equal useful output;two new_ modes translate only on102','All36 existing modes have equal useful output;135 names translate only on103')
    emit('tests/BenchmarkNpc103.java',s)
    verify=s.replace('BenchmarkNpc103','BenchmarkNpc103Verify')
    original='Set.of("unknown","existing_style","existing_choices","existing_choices84","existing_choices91","existing_choices92")'
    assert original in verify
    verify=verify.replace(original,'Set.of("unknown","existing_style","existing_choices87","existing_choices88","existing_large_batch88","existing_dynamic","unknown_named_speaker","existing_large_batch87","existing_choices90","existing_npc102_equal_work")').replace('Focused6 equal-work modes','Focused10 equal-work modes')
    emit('tests/BenchmarkNpc103Verify.java',verify)
    for source,target in [('benchmark_npc102.ps1','benchmark_npc103.ps1'),('benchmark_npc102_focused.ps1','benchmark_npc103_focused.ps1'),('dispatch_npc102.ps1','dispatch_npc103.ps1')]:
        s=(ROOT/'tools'/source).read_text(encoding='utf-8-sig').replace('0.15.102-dev','0.15.103-dev').replace('0.15.101-dev','0.15.102-dev').replace('npc102-benchmark','npc103-benchmark').replace('npc102-focused','npc103-focused').replace('npc102-dispatch','npc103-dispatch').replace('NPC102','NPC103').replace('BenchmarkNpc102.java','BenchmarkNpc103.java').replace("'BenchmarkNpc102'","'BenchmarkNpc103'")
        s=s.replace('E0EE8AA7086809A24B001F57F71902C778349215670C983BAA1382AF82606A09','BD99E5491FACF8B6E5AA1AA730F33DF119CC6DFB7EFA272257DB49E29B3EB0BD').replace('each101,102,102,101','each102,103,103,102').replace('JVMs101,102,102,101','JVMs102,103,103,102')
        emit('tools/'+target,s)
    wrapper=(ROOT/'tools/benchmark_npc103_focused.ps1').read_text(encoding='utf-8').replace('BenchmarkNpc102Focused','BenchmarkNpc103Verify').replace('npc103-focused','npc103-verify').replace('six equal-work modes','ten equal-work modes')
    emit('tools/benchmark_npc103_verify.ps1',wrapper)
    print('Derived103 benchmark and dispatch wrappers;102 frozen baseline, same11252 literal input rows.')

if __name__=='__main__':main()
