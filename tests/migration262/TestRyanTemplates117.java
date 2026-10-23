import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

public final class TestRyanTemplates117 {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);if(name.equals("dictionary"))flag("chatNotifications",new ChatNotificationTranslator((OfflineDictionary)value));}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component queue(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static String plain(Component c){var out=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static Style style(String role,int color,int seed){return Style.EMPTY.withColor(color).withShadowColor(seed).withInsertion(role+seed).withClickEvent(new ClickEvent.RunCommand("/unchanged117-"+role+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal(role+seed)));}
    private static Component make(JsonArray segments,int seed,boolean fragments,boolean envelope,boolean head){
        var out=Component.literal(envelope?"[04:05:06] ":"").setStyle(style("time",0x112233,seed));
        for(var value:segments){var row=value.getAsJsonObject();String text=row.get("text").getAsString(),role=row.get("role").getAsString();var appearance=style(role,row.get("color").getAsInt(),seed);
            if(head&&role.equals("name")){var object=new ObjectContents(new AtlasSprite(Identifier.parse("minecraft:gui"),Identifier.parse("minecraft:icon")),Optional.of(Component.literal("["+text+" head]")));out.append(MutableComponent.create(object).setStyle(style("head",0x334455,seed)));}
            for(int at=0;at<text.length();){int end=fragments?Math.min(at+7,text.length()):text.length();out.append(Component.literal(text.substring(at,end)).setStyle(appearance));at=end;}
        }
        return out.append(Component.literal(envelope?" (x17)":"").setStyle(style("counter",0x223344,seed)));
    }
    public static void main(String[] args)throws Exception{
        Path config=Path.of(args[0]);var dictionary=new OfflineDictionary(config);dictionary.setRecordUnknown(false);
        flag("dictionary",dictionary);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(dictionary));
        var cases=JsonParser.parseString(Files.readString(Path.of("translation-source/RYAN_ACCEPTANCE_0.16.3.json"))).getAsJsonObject().getAsJsonArray("cases");
        boolean baseline=Arrays.asList(args).contains("--expect-missing"),newOnly=Arrays.asList(args).contains("--new-only");var overridden=new HashSet<String>();int count=0;
        for(var value:cases){var row=value.getAsJsonObject();String kind=row.get("kind").getAsString();
            if(baseline&&newOnly&&kind.equals("greeting"))continue;
            if(baseline){var input=make(row.getAsJsonArray("source_segments"),1,false,false,false);var output=queue(input);check(plain(output).equals(plain(input)),"baseline output: "+plain(output)+" source="+plain(input));count++;continue;}
            for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(boolean envelope:List.of(false,true))for(boolean head:kind.equals("greeting")?List.of(false,true):List.of(false)){
                var input=make(row.getAsJsonArray("source_segments"),seed,fragments,envelope,head);var expected=make(row.getAsJsonArray("target_segments"),seed,fragments,envelope,head);String before=plain(input);var output=queue(input);
                check(plain(output).equals(plain(expected)),kind+" complete target: "+plain(output)+" expected "+plain(expected));
                check(styles(output).equals(styles(expected)),kind+" each current color/action/name/number preserved");
                check(plain(input).equals(before),"source immutable");check(queue(output)==output,"idempotent");
                if(head)check(output.getSiblings().stream().anyMatch(c->c.getContents() instanceof ObjectContents),"opaque head retained");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"protected channel");}
                var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed source unchanged");
            }
            if(overridden.add(kind)){
                var input=make(row.getAsJsonArray("source_segments"),3,false,false,false);String source=plain(input);Path custom=config.resolve("override-"+kind);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),source+"\tМой текст\n");var replacement=new OfflineDictionary(custom);replacement.setRecordUnknown(false);flag("dictionary",replacement);check(queue(input)==input,"full override wins "+kind);flag("dictionary",dictionary);
                if(row.has("override_term")){Path term=config.resolve("term-"+kind);Files.createDirectories(term);Files.writeString(term.resolve("overrides.tsv"),row.get("override_term").getAsString()+"\tМой термин\n");var replacementTerm=new OfflineDictionary(term);replacementTerm.setRecordUnknown(false);flag("dictionary",replacementTerm);check(queue(input)==input,"term override wins "+kind);flag("dictionary",dictionary);}
                if(kind.equals("greeting")){
                    Path legacy=config.resolve("legacy-key");Files.createDirectories(legacy);
                    Files.writeString(legacy.resolve("overrides.tsv"),"[NPC] Ryan: A Campfire Initiate Badge I, for our latest initiate, {name}!\tМой текст\n");
                    var replacementLegacy=new OfflineDictionary(legacy);replacementLegacy.setRecordUnknown(false);flag("dictionary",replacementLegacy);
                    check(queue(input)==input,"legacy {name} key wins for plain greeting");
                    var headed=make(row.getAsJsonArray("source_segments"),3,false,false,true);check(queue(headed)==headed,"legacy {name} key wins for opaque head");flag("dictionary",dictionary);
                }
                flag("translateNpcChat",false);check(queue(input)==input,"NPC toggle");flag("translateNpcChat",true);
                flag("translateGameChat",false);check(plain(queue(input)).equals(plain(make(row.getAsJsonArray("target_segments"),3,false,false,false))),"independent NPC toggle");flag("translateGameChat",true);
            }
            count++;
        }
        if(!baseline){for(String text:List.of("[NPC] Ryan: A Campfire Initiate Badge I, for our latest initiate, [Other head]Player_1!","[NPC] Ryan: A Campfire Initiate Badge I, for our latest initiate, Name With Spaces!","[NPC] Ryan: I need you to hold your Campfire God Badge X so I can upgrade it!","[NPC] Ryan: Try the Trial of Fire Trial XXXI for a greater challenge!","[NPC] Ryan: Try the Trial of Fire Trial IIII for a greater challenge!","[NPC] Ryan: Next step is the Trial of Fire I! (01 damage)","[NPC] Ryan: Next step is the Trial of Fire I! (1,00 damage)","[NPC] Ryan: Next step is the Trial of Fire I! (0 damage)","[NPC] Ryan: Next step is the Trial of Fire I! (1,000,000,000 damage)")){var c=Component.literal(text);check(queue(c)==c,"unknown/malformed Ryan declined: "+text);}}
        if(!baseline)for(String amount:List.of("999999999","999,999,999")){
            var c=Component.literal("[NPC] Ryan: Next step is the Trial of Fire XXX! ("+amount+" damage)");
            check(plain(queue(c)).equals("[NPC] Ryan: Следующее испытание огня — XXX! ("+amount+" урона)"),"numeric upper bound preserved");
            check(c.getString().endsWith("("+amount+" damage)"),"upper-bound source unchanged");
        }
        System.out.println("PASS: RYAN117 "+checks+" assertions; "+count+" concrete cases of five closed patterns; "+(baseline?"baseline missing":"styles/actions, opaque heads, bounds, overrides and protected channels; not a game test"));
    }
}
