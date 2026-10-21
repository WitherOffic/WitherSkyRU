import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Independent authored targets and current styles,not packaged offset plans. */
public final class TestNpcNumbers115 {
    static void check(boolean value,String reason){TestNpcNames103.check(value,reason);}
    static void flag(String key,Object value)throws Exception{TestNpcNames103.flag(key,value);}
    static void dictionary(OfflineDictionary value)throws Exception{flag("dictionary",value);flag("chatNotifications",new ChatNotificationTranslator(value));}
    static String plain(Component value){return TestNpcNames103.plain(value);}
    static Component dispatch(Component value){return TestNpcNames103.dispatch(value);}
    static List<Style> styles(Component value){return TestNpcNames103.styles(value);}
    static TestNpcNames103.Row row(JsonObject r){
        var anchors=new ArrayList<TestNpcNames103.Anchor>();
        for(var raw:r.getAsJsonArray("style_anchors")){var a=raw.getAsJsonObject();anchors.add(new TestNpcNames103.Anchor(a.get("source").getAsString(),a.get("target").getAsString()));}
        return new TestNpcNames103.Row(r.get("source").getAsString().replace("{value}","{name}"),r.get("translation").getAsString().replace("{value}","{name}"),List.copyOf(anchors),List.of());
    }
    static Component publication(String legacy,String value,int seed,boolean fragmented,String time,String suffix){
        var raw=Component.literal(legacy.replace("{value}",value));String text=plain(raw);var old=styles(raw);var out=Component.empty();TestNpcNames103.append(out,time,TestNpcNames103.style(seed,0),fragmented);
        for(int at=0;at<text.length();){int end=at+1;while(end<text.length()&&old.get(end).equals(old.get(at)))end++;
            var style=old.get(at).withInsertion("current113 "+seed).withClickEvent(new ClickEvent.SuggestCommand("/unchanged113 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint113 "+seed)));
            TestNpcNames103.append(out,text.substring(at,end),style,fragmented);at=end;
        }
        TestNpcNames103.append(out,suffix,TestNpcNames103.style(seed,99),fragmented);return out;
    }
    static List<String> values(String kind){
        if(kind.equals("username"))return List.of("a","_","Wither_101","A123456789012345");
        if(kind.equals("integer"))return List.of("0","1","137","1000","1,000","999,999,999,999");
        return List.of("0","1","137","1000","1,000","999,999,999,999","-1","+12","0.25","1,234.50","-12,345.6");
    }
    static List<String> invalid(String kind){
        if(kind.equals("username"))return List.of("","Player12345678901_","name with spaces","имя","<player>","[Player head]Name","a!","a\nInjected");
        var bad=new ArrayList<>(List.of("","00","01","1,23","1234,567","1,,000","1e3","NaN","∞","12 345","1_000","#","[amount]","{value}","١٢","12345678901234567","1\nInjected"));
        if(kind.equals("integer"))bad.addAll(List.of("-1","+1","1.5","1,000.00"));else bad.addAll(List.of("--1","+","-",".5","1.","1.234","1,000.000"));return bad;
    }
    static void literal(JsonObject r,Path config,OfflineDictionary db)throws Exception{
        String source=r.get("source").getAsString(),target=r.get("translation").getAsString(),from="Duke X",to="Герцог X";int prefix=source.indexOf(": ")+2,x=source.indexOf(from,prefix),y=target.indexOf(to,prefix);
        for(int seed:List.of(3,6))for(boolean fragments:List.of(false,true))for(String time:List.of("","[12:34:56] "))for(String suffix:List.of(""," (x17)")){
            var head=TestNpcNames103.style(seed,0);var base=TestNpcNames103.style(seed,20);var name=TestNpcNames103.style(seed,15);var tail=TestNpcNames103.style(seed,99);var input=Component.empty();
            TestNpcNames103.append(input,time+source.substring(0,prefix),head,fragments);TestNpcNames103.append(input,source.substring(prefix,x),base,fragments);TestNpcNames103.append(input,from,name,fragments);TestNpcNames103.append(input,source.substring(x+from.length()),base,fragments);TestNpcNames103.append(input,suffix,tail,fragments);
            var output=dispatch(input);check(plain(output).equals(time+target+suffix),"reviewed literal complete target");check(plain(input).equals(time+source+suffix),"literal input immutable");check(dispatch(output)==output,"literal idempotence");var styles=styles(output);
            for(int at=0;at<time.length()+target.length()+suffix.length();at++){
                var expected=at<time.length()+prefix?head:at>=time.length()+target.length()?tail:at>=time.length()+y&&at<time.length()+y+to.length()?name:base;
                check(styles.get(at).equals(expected),"every literal glyph current style/actions "+at);
            }
        }
        var own=config.resolve("literal-override");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),"Duke X\tМой термин\n");dictionary(new OfflineDictionary(own));var input=Component.literal(source);check(dispatch(input)==input,"literal semantic override wins");dictionary(db);
        for(String unsupported:List.of(source.replace("Duke X","Duke Y"),source.replace("[NPC]","[BOSS]"))){var c=Component.literal(unsupported);check(dispatch(c)==c,"only actual fixed proper name registered");}
    }
    public static void main(String[] args)throws Exception{
        Path config=Path.of(args[0]),root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);
        flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var data=JsonParser.parseString(Files.readString(root.resolve("NPC_RECOVERY_SOURCES_0.15.115.json"))).getAsJsonObject();int tested=0,layouts=0,fixes=0;
        var clazz=Class.forName("ru.wither.skyru.NamedNpcTemplates");var constructor=clazz.getDeclaredConstructor();constructor.setAccessible(true);var model=constructor.newInstance();var plan=clazz.getDeclaredMethod("plan",String.class,OfflineDictionary.class);plan.setAccessible(true);
        for(var raw:data.getAsJsonArray("records")){
            var r=raw.getAsJsonObject();if(!r.get("kind").getAsString().equals("integer"))continue;var row=row(r);String kind=r.get("capture_type").getAsString();boolean fix=r.get("color_fix").getAsBoolean();if(fix)fixes++;
            String template=r.get("source").getAsString(),target=r.get("translation").getAsString();
            for(String value:values(kind))for(int seed:List.of(3,6))for(boolean fragmented:List.of(false,true))for(String time:List.of("","[12:34:56] "))for(String suffix:List.of(""," (x1)"," (×999999)")){
                var input=TestNpcNames103.fixture(row,value,seed,fragmented,time,suffix);TestNpcNames103.result(row,value,input,time,suffix);
            }
            for(var layout:r.getAsJsonArray("template_layouts")){layouts++;for(String value:values(kind))for(boolean fragmented:List.of(false,true)){
                String legacy=layout.getAsString();check(plain(Component.literal(legacy)).equals(template),"pinned canonical source");TestNpcNames103.result(row,value,publication(legacy,value,8,fragmented,"[12:34:56] "," (x7)"),"[12:34:56] "," (x7)");
            }}
            String value=kind.equals("username")?"Wither_101":"137",source=template.replace("{value}",value);var input=Component.literal(source);
            check(plain(dispatch(input)).equals(target.replace("{value}",value)),"authored full target,not just Cyrillic screening");
            for(String bad:invalid(kind)){
                String full=template.replace("{value}",bad);check(((List<?>)plan.invoke(model,full,db)).isEmpty(),"bounded capture validator rejects "+kind+"/"+bad);
                var c=Component.literal(full);check(dispatch(c)==c,"invalid captured shape not guessed");
            }
            for(String altered:List.of(source+" added",source.replace("[NPC] ","[BOSS] "),"prefix "+source)){var c=Component.literal(altered);check(dispatch(c)==c,"anchored closed source");}
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=TestNpcNames103.message(input,channel);check(SkyRuClient.npcChat(m)==m,"player/client channels protected");}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed input protected");
            flag("translateNpcChat",false);check(dispatch(input)==input,"NPC switch");flag("translateNpcChat",true);
            String second=kind.equals("username")?"Other_2020":"284";check(plan.invoke(model,source,db)==plan.invoke(model,template.replace("{value}",second),db),"same-length immutable offset plan reused;no live-value/style cache");
            var own=config.resolve("override-full-"+tested);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\tМой текст\n");dictionary(new OfflineDictionary(own));check(dispatch(input)==input,"full live source override wins: "+source);dictionary(db);
            for(var anchor:r.getAsJsonArray("style_anchors")){
                String term=anchor.getAsJsonObject().get("source").getAsString();own=config.resolve("override-term-"+tested+"-"+term.hashCode());Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),term+"\tМой термин\n");dictionary(new OfflineDictionary(own));check(dispatch(input)==input,"semantic override wins: "+source+" / "+term);dictionary(db);
            }
            for(var excluded:r.getAsJsonObject("runtime_rule").getAsJsonArray("excluded_names"))check(((List<?>)plan.invoke(model,template.replace("{value}",excluded.getAsString()),db)).isEmpty(),"previous fixed-word exclusions retained");
            tested++;
        }

        check(tested==13&&layouts==13&&fixes==0,"13 reviewed integer shapes");
        verifyAuthored(root);
        System.out.println("PASS: NPC_NUMBERS115 "+TestNpcNames103.checks+" assertions;13 bounded number templates;current glyph styles/actions,bounds,overrides,channels;not FPS");
    }
    static void verifyAuthored(Path root)throws Exception{
        var author=JsonParser.parseString(Files.readString(root.resolve("NPC_RECOVERY_0.15.115_REVIEWED.json"))).getAsJsonObject();
        var evidence=JsonParser.parseString(Files.readString(root.resolve("NPC_RECOVERY_SOURCES_0.15.115.json"))).getAsJsonObject();
        var seen=new HashSet<String>();
        for(var raw:evidence.getAsJsonArray("records")){
            var r=raw.getAsJsonObject();String id=r.get("id").getAsString();check(seen.add(id),"unique independently authored ID");var a=author.getAsJsonArray(id);
            check(r.get("translation").getAsString().equals("[NPC] "+r.get("speaker").getAsString()+": "+a.get(0).getAsString()),"full target independently recovered from author file");
            var defs=a.get(1).getAsJsonObject();check(defs.size()==r.getAsJsonArray("style_anchors").size(),"all authored highlights retained");
            for(var d:r.getAsJsonArray("style_anchors")){var p=d.getAsJsonObject();check(defs.get(p.get("source").getAsString()).getAsString().equals(p.get("target").getAsString()),"independent authored semantic target");}
        }
        check(seen.size()==48&&seen.size()==author.size(),"35 literals+13 bounded shapes,not48 frozen exact keys");
    }
}
