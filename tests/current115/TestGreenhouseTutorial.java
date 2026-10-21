import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Independent screenshot text, all nine authored records, synthetic style/boundary tests. */
public class TestGreenhouseTutorial {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff).withInsertion("body");
    private static final Style TERM=BODY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("term")));
    private static final Style NUMBER=BODY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/number"));
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static List<Style> styles(Component source){var result=new ArrayList<Style>();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)result.add(s);return true;});return result;}
    private static void span(Component source,String word,Style wanted){int start=word.chars().allMatch(Character::isDigit)?Math.max(0,source.getString().indexOf(": ")+2):0;int at=source.getString().indexOf(word,start);check(at>=0,"missing span "+word);var list=styles(source);for(int i=at;i<at+word.length();i++)check(list.get(i).equals(wanted),"changed style/event "+word);}
    private static GuiMessage message(Component c,GuiMessageSource source){return new GuiMessage(17,c,null,source,null);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);var garden=NpcDialogueTranslator.gardenDialogues();
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",garden);flag("chatNotifications",new ChatNotificationTranslator(db));
        var resource=TestGreenhouseTutorial.class.getResourceAsStream("/data/witherskyru/garden-dialogues.json");
        var rows=JsonParser.parseReader(new java.io.InputStreamReader(resource,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records");
        check(rows.size()==43&&garden.phraseCount()==43,"43 additional observed Garden dialogue records");
        check(garden.translate(Component.literal("[NPC] Sam: Just aim the can at the crop you want to water and hold right-click!")).getString().equals("[NPC] Sam: Наведите лейку на растение, которое хотите полить, и удерживайте ПКМ!"),"independent watering instruction");
        check(garden.translate(Component.literal("[NPC] Sam: Planted crops in your Greenhouse apply buffs and debuffs to nearby crops!")).getString().equals("[NPC] Sam: Культуры в вашей теплице накладывают на соседние растения положительные и отрицательные эффекты!"),"both positive and negative effects, neighbours retained");
        for(var raw:rows){
            var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();
            var anchors=new ArrayList<String[]>();if(row.has("style_anchors"))for(var anchor:row.getAsJsonArray("style_anchors"))anchors.add(new String[]{anchor.getAsJsonObject().get("source").getAsString(),anchor.getAsJsonObject().get("target").getAsString()});
            anchors.sort(Comparator.comparingInt(a->source.indexOf(a[0])));
            String speaker=row.get("speaker").getAsString();
            var input=Component.empty().append(Component.literal("[04:32:25] [NPC] ").setStyle(NUMBER)).append(Component.literal(speaker+": ").setStyle(TERM));int at=("[NPC] "+speaker+": ").length();
            var wanted=new LinkedHashMap<String,Style>();
            for(String[] anchor:anchors){int next=source.indexOf(anchor[0],at);input.append(Component.literal(source.substring(at,next)).setStyle(BODY));Style style=anchor[0].equals("debuffs")?BODY.withColor(0xff5555):anchor[0].equals("advanced")?BODY.withColor(0x55ffff):TERM;input.append(Component.literal(anchor[0]).setStyle(style));wanted.put(anchor[1],style);at=next+anchor[0].length();}
            input.append(Component.literal(source.substring(at)).setStyle(BODY));
            var translated=SkyRuClient.npcChat(message(input,GuiMessageSource.SYSTEM_SERVER)).content();
            check(translated.getString().equals("[04:32:25] "+target),"styled Sam: "+translated.getString());
            span(translated,"[04:32:25] [NPC] ",NUMBER);span(translated,speaker+": ",TERM);for(var e:wanted.entrySet())span(translated,e.getKey(),e.getValue());
            for(int n=0;n<5;n++)check(garden.translate(translated,db)==translated,"idempotent Sam");
            var counterInput=input.copy().append(Component.literal(" (x16)").setStyle(NUMBER));
            var counterResult=garden.translate(counterInput,db);check(counterResult.getString().equals("[04:32:25] "+target+" (x16)"),"Garden deduplication counter");span(counterResult," (x16)",NUMBER);
            check(input.getString().equals("[04:32:25] "+source),"original Sam immutable");
            var player=message(input,GuiMessageSource.PLAYER);check(SkyRuClient.npcChat(player)==player,"player cannot spoof NPC");
            var client=message(input,GuiMessageSource.SYSTEM_CLIENT);check(SkyRuClient.npcChat(client)==client,"unknown client source protected");
            var custom=Path.of(args[0]).resolve(row.get("source_key").getAsString());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),source+"\tМоя реплика\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);
            check(garden.translate(input,own)==input,"full NPC override");
            for(String[] anchor:anchors){Files.writeString(custom.resolve("overrides.tsv"),anchor[0]+"\tМой термин\n");own=new OfflineDictionary(custom);own.setRecordUnknown(false);check(garden.translate(input,own)==input,"semantic NPC override");}
            flag("translateNpcChat",false);var system=message(input,GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(system)==system,"NPC toggle disabled");flag("translateNpcChat",true);
            flag("translateGameChat",false);check(SkyRuClient.npcChat(system).content().getString().equals("[04:32:25] "+target),"NPC independent of game notices");flag("translateGameChat",true);
        }
        // Raw read-only log samples: source-channel assumption is explicit, not a live test.
        var frozen=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("GARDEN_0.15.34_OBSERVED_SAMPLES.json"))).getAsJsonObject().getAsJsonArray("samples");
        int observed=0;
        for(var raw:frozen){var row=raw.getAsJsonObject();var sample=Component.literal(row.get("sample").getAsString());var system=message(sample,GuiMessageSource.SYSTEM_SERVER);
            check(SkyRuClient.npcChat(system)!=system,"untranslated observed Garden sample: "+row.get("source").getAsString());observed++;
        }
        check(observed>=50,"full observed Garden sample set");
        var head=Component.literal("[NPC] Pesthunter Phillip: Howdy, [Wither_101 head]Wither_101!");
        check(SkyRuClient.npcChat(message(head,GuiMessageSource.SYSTEM_SERVER)).content().getString().equals("[NPC] Pesthunter Phillip: Здравствуйте, [Wither_101 head]Wither_101!"),"literal head label and nickname kept");
        var hyd=Component.literal("Your HydroCan™ 1000 is out of water! (x16)").setStyle(NUMBER);var hydResult=SkyRuClient.npcChat(message(hyd,GuiMessageSource.SYSTEM_SERVER)).content();
        check(hydResult.getString().equals("В лейке HydroCan™ 1000 закончилась вода! (x16)"),"closed HydroCan depletion");span(hydResult,"1000",NUMBER);span(hydResult,"(x16)",NUMBER);
        var objectives=Map.ofEntries(Map.entry("NEW OBJECTIVE","НОВАЯ ЦЕЛЬ"),Map.entry("Plant a carrot","Посадите морковь"),Map.entry("Water the carrot","Полейте морковь"),Map.entry("Wait for the carrot to grow","Дождитесь, пока морковь вырастет"),Map.entry("Harvest the carrot","Соберите морковь"),
            Map.entry("Wait for the Greenhouse to complete building","Дождитесь завершения строительства теплицы"),Map.entry("Talk to Sam at the Greenhouse","Поговорите с Sam в теплице"),Map.entry("Plant 2 carrots next to each other and water them in your Greenhouse","Посадите 2 моркови рядом друг с другом в теплице и полейте их"),
            Map.entry("Analyze growing crops in the Greenhouse with the Plant Diagnostics Tool","Изучите растущие культуры в теплице с помощью анализатора растений"),Map.entry("Spread a mutation in the Greenhouse","Получите мутацию в теплице"),Map.entry("Harvest the mutation in the Greenhouse","Соберите мутировавшую культуру в теплице"),Map.entry("Talk to the Carpenter in the Greenhouse","Поговорите с Плотником в теплице"));
        for(var e:objectives.entrySet()){
            var input=Component.literal("[04:32:27]   "+e.getKey()).setStyle(TERM);var system=message(input,GuiMessageSource.SYSTEM_SERVER);
            check(SkyRuClient.npcChat(system).content().getString().equals("[04:32:27]   "+e.getValue()),"objective with padding/timestamp");
            var player=message(input,GuiMessageSource.PLAYER);check(SkyRuClient.npcChat(player)==player,"player objective text protected");
            flag("translateGameChat",false);check(SkyRuClient.npcChat(system)==system,"game toggle independent");flag("translateGameChat",true);
        }
        var quantity=Component.empty().append(Component.literal("Plant ").setStyle(BODY)).append(Component.literal("2").setStyle(NUMBER)).append(Component.literal(" carrots next to each other and water them in your Greenhouse").setStyle(BODY));
        var quantityResult=SkyRuClient.npcChat(message(quantity,GuiMessageSource.SYSTEM_SERVER)).content();
        check(quantityResult.getString().equals(objectives.get(quantity.getString())),"planting quantity instruction");span(quantityResult,"2",NUMBER);
        var diagnostics=new LinkedHashMap<String,String>();
        diagnostics.put("Age: 23s","Возраст: 23 с");diagnostics.put("Stage: 1/8","Стадия: 1/8");diagnostics.put("Next Stage: 3h 9m 16s","Следующая стадия через: 3 ч 9 мин 16 с");
        diagnostics.put("Times Mutated: 0","Мутаций: 0");diagnostics.put("Mutates Remaining: 12","Ещё нужно мутаций: 12");diagnostics.put("Decay attempt in: 2d 23h 59m 36s","Проверка на увядание через: 2 дн 23 ч 59 мин 36 с");
        diagnostics.put("Decays at: Thu 15.10.2026 04:43:11","Время увядания: Thu 15.10.2026 04:43:11");
        for(var e:diagnostics.entrySet()){
            var input=Component.empty();int colon=e.getKey().indexOf(':');input.append(Component.literal(e.getKey().substring(0,colon+1)).setStyle(BODY));
            for(char ch:e.getKey().substring(colon+1).toCharArray())input.append(Component.literal(String.valueOf(ch)).setStyle(Character.isDigit(ch)?NUMBER:TERM));
            var result=SkyRuClient.component(input);check(result.getString().equals(e.getValue()),"diagnostic: "+result.getString());
            check(db.translate(e.getKey()).equals(e.getValue()),"plain diagnostic");
            var sequence=TooltipTranslation.styled(List.of(input.getVisualOrderText()),false).getFirst();var visible=new StringBuilder();sequence.accept((i,s,cp)->{visible.appendCodePoint(cp);return true;});check(visible.toString().equals(e.getValue()),"diagnostic sequence");
            var list=styles(result);for(int i=0;i<result.getString().length();i++)if(Character.isDigit(result.getString().charAt(i)))check(list.get(i).equals(NUMBER),"per-digit event/colour unchanged");
            for(int n=0;n<5;n++)check(SkyRuClient.component(result).getString().equals(e.getValue()),"diagnostic idempotent");
            var custom=Path.of(args[0]).resolve("diagnostic-"+colon+"-"+e.getKey().length());Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),e.getKey().substring(0,colon+1)+"\tМоя подпись\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);check(!SkyRuClient.component(input).getString().equals(e.getValue()),"diagnostic label override");flag("dictionary",db);
        }
        for(String text:List.of("pickaxe","arbitrary English text","Wither_101","[MVP+] Wither_101: Plant a carrot","[NPC] Sam: Unknown future dialogue, never guess!")){
            var raw=message(Component.literal(text),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(raw)==raw,"ordinary/unobserved text untouched: "+text);
        }
        System.out.println("PASS: Greenhouse/Garden "+checks+" assertions; 43 additional exact records (32 Sam), "+observed+" frozen legacy samples, objectives/diagnostics/deduplication, original nicknames/styles/timestamps/toggles/overrides; not all NPCs or live-tested");
    }
}
