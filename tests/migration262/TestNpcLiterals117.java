import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** Independent source/target fixtures; exercises the actual server-chat queue. */
public final class TestNpcLiterals117 {
    private static int checks;
    private static void check(boolean value,String label) {
        if(!value)throw new AssertionError(label);
        checks++;
    }
    private static void flag(String name,Object value)throws Exception {
        var field=SkyRuClient.class.getDeclaredField(name);
        field.setAccessible(true);field.set(null,value);
    }
    private static GuiMessage message(Component text,GuiMessageSource source) {
        return new GuiMessage(0,text,null,source,null);
    }
    private static Component queued(Component text) {
        return SkyRuClient.npcChat(message(text,GuiMessageSource.SYSTEM_SERVER)).content();
    }
    private static String plain(Component text) {
        var out=new StringBuilder();
        StringDecomposer.iterateFormatted(text,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});
        return out.toString();
    }
    private static List<Style> styles(Component text) {
        var out=new ArrayList<Style>();
        StringDecomposer.iterateFormatted(text,Style.EMPTY,(i,s,p)->{
            for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;
        });return out;
    }
    private static int semanticToken(String text,int position,JsonArray anchors) {
        int token=0;
        for(var value:anchors) {
            token++;String from=value.getAsJsonObject().get("source").getAsString();int at=text.indexOf(from);
            if(at>=0&&position>=at&&position<at+from.length())return token;
        }
        return 0;
    }
    private static Component styled(String legacy,int seed,boolean fragmented,String time,String suffix,JsonArray anchors) {
        var parsed=Component.literal(legacy);String text=plain(parsed);var appearance=styles(parsed);
        var out=Component.literal(time).setStyle(Style.EMPTY.withColor(0x112233).withInsertion("time117"));
        for(int a=0;a<text.length();) {
            int b=a+1,token=semanticToken(text,a,anchors);
            while(b<text.length()&&appearance.get(b).equals(appearance.get(a))&&semanticToken(text,b,anchors)==token&&(!fragmented||b-a<7))b++;
            var style=appearance.get(a).withShadowColor(seed).withInsertion("current117-"+seed+"/"+token)
                .withClickEvent(new ClickEvent.RunCommand("/unchanged117-"+seed+"/"+token))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("hover117-"+seed+"/"+token)));
            out.append(Component.literal(text.substring(a,b)).setStyle(style));a=b;
        }
        return out.append(Component.literal(suffix).setStyle(Style.EMPTY.withColor(0x334455).withInsertion("counter117")));
    }
    public static void main(String[] args)throws Exception {
        Path config=Path.of(args[0]);Files.createDirectories(config);
        var dictionary=new OfflineDictionary(config);dictionary.setRecordUnknown(false);
        flag("dictionary",dictionary);flag("enabled",true);flag("onlyHypixel",false);
        flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("storyDialogues",NpcDialogueTranslator.storyDialogues());
        flag("npcDialogues",new NpcDialogueTranslator());
        flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());
        flag("questDialogues",NpcDialogueTranslator.questDialogues());
        flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());
        flag("chatNotifications",new ChatNotificationTranslator(dictionary));
        var fixtures=new ArrayList<Path>();
        for(String arg:args)if(arg.startsWith("--fixture="))fixtures.add(Path.of(arg.substring(10)));
        if(fixtures.isEmpty())try(var paths=Files.newDirectoryStream(Path.of("translation-source"),"NPC_ACCEPTANCE_0.16.*.json")) {
            for(Path path:paths)fixtures.add(path);
        }
        fixtures.sort(Comparator.comparing(Path::toString));
        var cases=new JsonArray();var sources=new HashSet<String>();
        for(Path fixture:fixtures)for(var value:JsonParser.parseString(Files.readString(fixture)).getAsJsonObject().getAsJsonArray("records")) {
            check(sources.add(value.getAsJsonObject().get("source").getAsString()),"no duplicate source across independent acceptance fixtures");
            cases.add(value);
        }
        check(!cases.isEmpty(),"nonempty independent reviewed fixtures");
        boolean baseline=Arrays.asList(args).contains("--expect-missing");
        int ordinal=0;
        for(var value:cases) {
            var row=value.getAsJsonObject();String source=row.get("source").getAsString();
            String target=row.get("translation").getAsString();String legacy=row.get("legacy").getAsString();
            check(plain(Component.literal(legacy)).equals(source),"independent published formatting matches source");
            if(baseline) {
                var input=Component.literal(source);
                check(queued(input)==input,"baseline missing exact translation: "+source);
                ordinal++;continue;
            }
            for(int seed=1;seed<=2;seed++)for(boolean fragmented:List.of(false,true))
                for(String time:List.of("","[04:05:06] "))for(String suffix:List.of(""," (x17)")) {
                Component input=styled(legacy,seed,fragmented,time,suffix,row.getAsJsonArray("style_anchors"));String before=plain(input);
                var output=queued(input);
                check(plain(output).equals(time+target+suffix),"complete authored target: "+source+" seed="+seed+" fragmented="+fragmented+" timestamp="+!time.isEmpty()+" counter="+!suffix.isEmpty()+" output="+plain(output));
                check(plain(input).equals(before),"source immutable");
                check(queued(output)==output,"translated component idempotent");
                int prefix=source.indexOf(": ")+2;var inStyles=styles(input);var outStyles=styles(output);
                check(inStyles.subList(0,time.length()+prefix).equals(outStyles.subList(0,time.length()+prefix)),"prefix styles/actions retained");
                var anchors=row.getAsJsonArray("style_anchors");var covered=new HashSet<Integer>();
                var expected=new HashMap<Integer,Style>();
                for(var raw:anchors) {
                    var anchor=raw.getAsJsonObject();String from=anchor.get("source").getAsString(),to=anchor.get("target").getAsString();
                    int x=source.indexOf(from,prefix),y=target.indexOf(to,prefix);
                    check(x>=prefix&&x==source.lastIndexOf(from)&&y>=prefix&&y==target.lastIndexOf(to),"independent unique semantic anchor");
                    for(int n=0;n<from.length();n++)covered.add(x+n);
                    for(int n=0;n<to.length();n++)expected.put(y+n,inStyles.get(time.length()+x+(from.equals(to)?n:0)));
                }
                Style body=inStyles.get(time.length()+prefix);
                for(int n=prefix;n<source.length();n++)if(!covered.contains(n)&&!Character.isWhitespace(source.charAt(n))) {
                    body=inStyles.get(time.length()+n);break;
                }
                for(int at=time.length()+prefix;at<time.length()+target.length();at++)
                    check(outStyles.get(at).equals(expected.getOrDefault(at-time.length(),body)),"every translated character has current semantic style/actions");
                if(!suffix.isEmpty())check(inStyles.subList(before.length()-suffix.length(),before.length())
                    .equals(outStyles.subList(plain(output).length()-suffix.length(),plain(output).length())),"counter preserved");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)) {
                    var protectedMessage=message(input,channel);
                    check(SkyRuClient.npcChat(protectedMessage)==protectedMessage,"protected chat channel");
                }
                var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);
                check(SkyRuClient.npcChat(signed)==signed,"signed source retained");
            }
            var input=Component.literal(source);
            flag("translateNpcChat",false);check(queued(input)==input,"NPC toggle respected");flag("translateNpcChat",true);
            flag("translateGameChat",false);check(plain(queued(input)).equals(target),"NPC translation independent of notification toggle");flag("translateGameChat",true);
            Path overrideConfig=config.resolve("override-"+ordinal);Files.createDirectories(overrideConfig);
            Files.writeString(overrideConfig.resolve("overrides.tsv"),source+"\tМой перевод\n");
            var overrideDictionary=new OfflineDictionary(overrideConfig);overrideDictionary.setRecordUnknown(false);
            flag("dictionary",overrideDictionary);check(queued(input)==input,"full-source user override wins");flag("dictionary",dictionary);
            for(var raw:row.getAsJsonArray("style_anchors")) {
                String term=raw.getAsJsonObject().get("source").getAsString();
                Path termConfig=config.resolve("term-"+ordinal+"-"+Integer.toUnsignedString(term.hashCode()));Files.createDirectories(termConfig);
                Files.writeString(termConfig.resolve("overrides.tsv"),term+"\tМой термин\n");
                var termDictionary=new OfflineDictionary(termConfig);termDictionary.setRecordUnknown(false);
                flag("dictionary",termDictionary);check(queued(input)==input,"semantic term override wins: "+term);flag("dictionary",dictionary);
            }
            var split=Component.literal(source.substring(0,source.length()-5)).setStyle(Style.EMPTY.withColor(0xffffff))
                .append(Component.literal(source.substring(source.length()-5)).setStyle(Style.EMPTY.withColor(0x123456)));
            check(queued(split)==split,"unreviewed semantic color split declined");
            var unknown=Component.literal(source+" not a published continuation");
            check(queued(unknown)==unknown,"unknown continuation declined");
            var oversized=Component.empty().append(Component.literal(source));
            for(int n=0;n<129;n++)oversized.append(Component.literal(""));
            check(queued(oversized)==oversized,"more than 128 literal nodes deliberately declined");
            ordinal++;
        }
        if(!baseline) {
            var bonzo=Component.literal("[BOSS] Bonzo: Sike!");
            check(plain(queued(bonzo)).equals("[BOSS] Bonzo: А вот и нет!"),"existing Bonzo punctuation/target retained");
            var notDialogue=Component.literal("[BOSS] Bonzo: Sike");
            check(queued(notDialogue)==notDialogue,"unsupported prose-derived Bonzo variant not imported");
        }
        System.out.println("PASS: NPC_LITERALS117 "+checks+" assertions; "+ordinal+" literals; "+
            (baseline?"baseline misses confirmed":"current colors/actions, fragmentation, timestamps/counters, overrides and chat protection; not an in-game test"));
    }
}
