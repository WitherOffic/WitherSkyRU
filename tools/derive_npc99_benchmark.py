"""Equal-work prior scenarios plus explicitly unequal newly translated receipt scenarios."""
from prepare_npc99 import ROOT
BASE='76E85EF50B3308A390D5C11302198A16EAFD1C7CA2BE08F4AE46530C16718A50'
s=(ROOT/'tests/BenchmarkOptimization96.java').read_text().replace('BenchmarkOptimization96','BenchmarkNpc99')
marker='        var bean=(com.sun.management.ThreadMXBean)'
assert s.count(marker)==1
addition='''        inputs.put("new_npc99_unequal_work",List.of("§e[NPC] §2Helia§f: §fHeyhey! Welcome to §6Torrhus Canyon§f!","§e[NPC] Tony§f: §b✆ §f§fI guess you want my §9Abiphone Contact§f, huh?","§e[NPC] §8§lCentauri§f: Pro tip: use §7/centauri","§e[NPC] §cCrag§f: §b✆§f Bring me a §eBezal Shard§7, and I'll give you my contact."));
        inputs.put("new_choices99_unequal_work",List.of("Select an option: [Wait... are you a talking pickaxe?]","Select an option: [Deface them?!]","Select an option: [What kind of game?]","Select an option: [What Elite?]"));
        boolean expansion99=BenchmarkNpc99.class.getResource("/data/witherskyru/npc-expansion99.json")!=null;
        for(String mode:List.of("new_npc99_unequal_work","new_choices99_unequal_work"))for(String source:inputs.get(mode)){
            var output=SkyRuClient.npcChat(new GuiMessage(0,Component.literal(source),null,GuiMessageSource.SYSTEM_SERVER,null)).content();
            if((!output.getString().equals(source))!=expansion99)throw new AssertionError("New99 useful-output expectation: "+source);
        }
'''
s=s.replace(marker,addition+marker)
s=s.replace('33 equal-work modes95/96','33 equal-work plus2 explicitly unequal new modes98/99').replace('All33 modes have equal useful output on95 and96;no translation rows added.','Only existing33 modes have equal useful output;two new_ modes translate only on99.')
(ROOT/'tests/BenchmarkNpc99.java').write_text(s,encoding='utf-8')
s=(ROOT/'tools/benchmark_npc98.ps1').read_text().replace('0.15.98-dev','0.15.99-dev').replace('0.15.96-dev','0.15.98-dev').replace('npc98-benchmark','npc99-benchmark').replace('NPC98 BENCHMARK','NPC99 BENCHMARK').replace('96,98,98,96','98,99,99,98').replace('Wrong96 baseline','Wrong98 baseline')
s=s.replace('97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1',BASE).replace("'receipt'{'BenchmarkOptimization96.java'}","'receipt'{'BenchmarkNpc99.java'}").replace("'receipt'{'BenchmarkOptimization96'}","'receipt'{'BenchmarkNpc99'}")
s=s.replace('@{mode=$taskMode;orders=@($taskOrders)}','@{mode=$taskMode;equal_useful_output=($taskMode -notlike "new_*");orders=@($taskOrders)}')
(ROOT/'tools/benchmark_npc99.ps1').write_text(s,encoding='utf-8')
s=(ROOT/'tests/BenchmarkNpc98Focused.java').read_text().replace('BenchmarkNpc98Focused','BenchmarkNpc99Focused')
(ROOT/'tests/BenchmarkNpc99Focused.java').write_text(s,encoding='utf-8')
s=(ROOT/'tools/benchmark_npc98_focused.ps1').read_text().replace('0.15.98-dev','0.15.99-dev').replace('0.15.96-dev','0.15.98-dev').replace('BenchmarkNpc98Focused','BenchmarkNpc99Focused').replace('npc98-focused','npc99-focused').replace('NPC98 FOCUSED','NPC99 FOCUSED').replace('96,98,98,96','98,99,99,98')
(ROOT/'tools/benchmark_npc99_focused.ps1').write_text(s,encoding='utf-8')
print('Derived35-mode receipt/unchanged HUD benchmarks;33 equal-work and2 unequal new modes;separate6-mode follow-up')
