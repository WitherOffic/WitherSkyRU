import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Frozen primary-source Dungeon samples; never contacts a server or launches Minecraft. */
public class TestDungeonChatResources {
    private static int checks;
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var out=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static List<String> numbers(String s){return Pattern.compile("[0-9]+").matcher(s).results().map(x->x.group()).toList();}
    private static void retained(Component a,Component b,String from,String to,boolean exact){
        int x=text(a).indexOf(from),y=text(b).indexOf(to);check(x>=0&&y>=0,"missing semantic anchor: "+from);
        var old=styles(a);var now=styles(b);
        for(int i=0;i<to.length();i++)check(now.get(y+i).equals(old.get(x+(exact?i:0))),"semantic colour/action: "+from);
    }
    public static void main(String[] args)throws Exception{
        Path proof=Path.of(args[1]);var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        flag("npcDialogues",new NpcDialogueTranslator());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());
        flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        check(new NpcDialogueTranslator().phraseCount()==113,"old NPC resource unchanged");
        Style action=Style.EMPTY.withInsertion("unchanged").withClickEvent(new ClickEvent.RunCommand("/unchanged"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("unchanged hover")));
        int total=0;
        for(boolean notification:List.of(false,true)){
            var translator=notification?NpcDialogueTranslator.dungeonNotifications():NpcDialogueTranslator.dungeonDialogues();
            String resource=notification?"DUNGEON_NOTIFICATIONS_RU.json":"DUNGEON_DIALOGUES_RU.json";
            String sampleFile=notification?"DUNGEON_NOTIFICATION_PRIMARY_SAMPLES.json":"DUNGEON_DIALOGUE_PRIMARY_SAMPLES.json";
            var entries=JsonParser.parseString(Files.readString(proof.resolve(resource))).getAsJsonObject().getAsJsonArray("records");
            var samples=JsonParser.parseString(Files.readString(proof.resolve(sampleFile))).getAsJsonArray();
            check(entries.size()==(notification?28:22)&&translator.phraseCount()==(notification?entries.size():entries.size()+336),"closed resource count");
            Map<String,JsonObject> records=new HashMap<>();
            for(var raw:entries){var row=raw.getAsJsonObject();check(records.put(row.get("source").getAsString(),row)==null,"duplicate");}
            Set<String> seen=new HashSet<>();
            for(var raw:samples){
                var sample=raw.getAsJsonObject();String source=sample.get("source").getAsString(),legacy=sample.get("sample").getAsString();
                var row=records.get(source);check(row!=null&&seen.add(source),"independent source sample");
                check(legacy.replaceAll("(?i)§[0-9a-fk-or]","").equals(source),"primary visible source matches");
                check(row.get("source_file").equals(sample.get("source_file")),"immutable primary source provenance");
                String target=row.get("translation").getAsString();
                String prefix=notification?"":"["+row.get("tag").getAsString()+"] "+row.get("speaker").getAsString()+": ";
                check(target.startsWith(prefix),"original speaker/tag");
                check(numbers(source).equals(numbers(target)),"amounts preserved");
                for(String timestamp:List.of("","[01:02:03] ")){
                    Component original=Component.literal(timestamp+legacy).setStyle(action);Component output=translator.translate(original,db);
                    check(text(output).equals(timestamp+target),"legacy replay: "+source+" -> "+text(output));
                    check(original.getString().equals(timestamp+legacy),"source immutable");
                    for(int repeat=0;repeat<5;repeat++){
                        check(translator.translate(output,db)==output,"idempotence identity");
                        if(!prefix.isEmpty())retained(original,output,prefix,prefix,true);
                        if(!timestamp.isEmpty())retained(original,output,timestamp,timestamp,true);
                        if(row.has("style_anchors"))for(var anchor:row.getAsJsonArray("style_anchors")){
                            var a=anchor.getAsJsonObject();String from=a.get("source").getAsString(),to=a.get("target").getAsString();
                            retained(original,output,from,to,from.equals(to));
                        }
                        for(Style s:styles(output)){
                            check(Objects.equals(s.getClickEvent(),action.getClickEvent()),"original command");
                            check(Objects.equals(s.getHoverEvent(),action.getHoverEvent()),"original hover");
                            check(Objects.equals(s.getInsertion(),action.getInsertion()),"original insertion");
                        }
                    }
                    flag("translateNpcChat",!notification);flag("translateGameChat",notification);
                    var message=new GuiMessage(123,original,null,GuiMessageSource.SYSTEM_SERVER,null);
                    var translated=SkyRuClient.npcChat(message);
                    check(text(translated.content()).equals(timestamp+target),"isolated toggle routing: "+source);
                    check(translated.addedTime()==123&&translated.source()==message.source()&&translated.tag()==message.tag(),"queue metadata");
                    for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){
                        var excluded=new GuiMessage(123,original,null,channel,null);
                        check(SkyRuClient.npcChat(excluded)==excluded,"whole player/client message unchanged");
                    }
                    var signed=new GuiMessage(123,original,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);
                    check(SkyRuClient.npcChat(signed)==signed,"signed unchanged");
                    flag("translateNpcChat",notification);flag("translateGameChat",!notification);
                    check(SkyRuClient.npcChat(message)==message,"other toggle cannot translate");
                    flag("translateNpcChat",true);flag("translateGameChat",true);flag("enabled",false);
                    check(SkyRuClient.npcChat(message)==message,"disabled unchanged");flag("enabled",true);
                }
                var plain=Component.literal(source);check(translator.translate(plain,db).getString().equals(target),"plain reply");
                var unknown=Component.literal("[MVP+] Wither_101: "+source);
                check(translator.translate(unknown,db)==unknown,"player-shaped quotation");
                total++;
            }
            check(seen.size()==entries.size(),"all exact messages replayed");
            Component unknown=Component.literal("[BOSS] Unknown: invented source XY-987");
            check(translator.translate(unknown,db)==unknown,"unknown not guessed");
        }
        for(String raw:List.of("pickaxe","banana","Wither_101","[MVP+] Wither_101: pickaxe","/msg Wither_101 pickaxe")){
            Component c=Component.literal(raw);for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_SERVER)){
                var m=new GuiMessage(0,c,null,channel,null);check(SkyRuClient.npcChat(m)==m,"no generic chat word/name replacement");
            }
        }
        String source="[NPC] Mort: You should find it useful if you get lost.";
        var split=Component.empty().append(Component.literal("[NPC] Mort: You should ").setStyle(action))
            .append(Component.literal("find it useful if you get lost.").setStyle(action.withClickEvent(new ClickEvent.RunCommand("/different"))));
        check(NpcDialogueTranslator.dungeonDialogues().translate(split,db)==split,"ambiguous prose actions fail closed");
        var keyed=Component.translatable(source);check(NpcDialogueTranslator.dungeonDialogues().translate(keyed,db)==keyed,"nonliteral not guessed");
        Path custom=Path.of(args[0]).resolve("custom-dungeon");Files.createDirectories(custom);
        Files.writeString(custom.resolve("overrides.tsv"),source+"\tМой текст\nGuided Sheep\tМой термин\n");
        var override=new OfflineDictionary(custom);
        var c=Component.literal(source);check(NpcDialogueTranslator.dungeonDialogues().translate(c,override)==c,"full override");
        c=Component.literal("Guided Sheep is now available!");check(NpcDialogueTranslator.dungeonNotifications().translate(c,override)==c,"semantic override");
        flag("dictionary",override);
        var m=new GuiMessage(0,c,null,GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(m)==m,"queue respects semantic override");
        flag("dictionary",db);
        check(total==50,"50 exact source-backed messages");
        System.out.println("PASS: Dungeon closed chat "+checks+" assertions; Mort 4 / Oruo 9 / bosses 9 / notices 28; all frozen legacy styles, names/actions/timestamps, five repeats, separate toggles; not live-tested");
    }
}
