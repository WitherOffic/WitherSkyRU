import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Authored targets and publication layouts, independently of packaged offset plans. */
public final class TestNpcNames103 {
    static int checks;
    static void check(boolean v,String why){if(!v)throw new AssertionError(why);checks++;}
    static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    static String plain(Component c){var s=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,t,p)->{s.appendCodePoint(p);return true;});return s.toString();}
    static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,t,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(t);return true;});return out;}
    static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    static Component dispatch(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    static Style style(int seed,int part){return Style.EMPTY.withColor((seed*0x5291+part*0x2739)&0xffffff).withBold(seed%2==0).withItalic(seed%3==0).withShadowColor(seed+part).withInsertion("fresh103 "+seed+"/"+part).withClickEvent(new ClickEvent.SuggestCommand("/unchanged103 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint103 "+seed+"/"+part)));}
    static void append(MutableComponent c,String text,Style s,boolean fragments){if(fragments){for(int i=0;i<text.length();i++)c.append(Component.literal(text.substring(i,i+1)).setStyle(s));}else c.append(Component.literal(text).setStyle(s));}
    static final String TOKEN="{name}";
    static final java.util.regex.Pattern NAME=java.util.regex.Pattern.compile("<(?:player|Username|username|playername|Player|player name|Player Name)>|\\[(?:player|Player|player name|Player Name|username|Player.s Name)\\]");
    record Anchor(String from,String to){}
    record Row(String source,String target,List<Anchor> anchors,List<String> layouts){}
    static int prefix(String s){int n=s.indexOf(": ")+2;return s.startsWith("✆ ",n)?n+2:n;}
    static String real(String s,String name){return s.replace(TOKEN,name);}
    static Component fixture(Row r,String name,int seed,boolean fragments,String time,String suffix){
        String s=real(r.source,name);int prefix=prefix(s);var c=Component.empty();append(c,time+s.substring(0,prefix),style(seed,0),fragments);
        var marks=new TreeMap<Integer,Map.Entry<String,Style>>();int n=1;
        for(var a:r.anchors){int at=s.indexOf(a.from,prefix);check(at>=prefix,"authored anchor");marks.put(at,Map.entry(a.from,style(seed,n++)));}
        int nameAt=r.source.indexOf(TOKEN);for(int i=0;i<name.length();i++)marks.put(nameAt+i,Map.entry(name.substring(i,i+1),style(seed,40+i)));
        int cursor=prefix;for(var entry:marks.entrySet()){append(c,s.substring(cursor,entry.getKey()),style(seed,20),fragments);append(c,entry.getValue().getKey(),entry.getValue().getValue(),fragments);cursor=entry.getKey()+entry.getValue().getKey().length();}
        append(c,s.substring(cursor),style(seed,20),fragments);append(c,suffix,style(seed,99),fragments);return c;
    }
    static Component publication(String legacy,String name,int seed,boolean fragments,String time,String suffix){
        String s=NAME.matcher(legacy).replaceAll(name);var raw=Component.literal(s);String text=plain(raw);var old=styles(raw);var c=Component.empty();append(c,time,style(seed,0),fragments);
        for(int at=0;at<text.length();){int end=at+1;while(end<text.length()&&old.get(end).equals(old.get(at)))end++;
            var current=old.get(at).withInsertion("publication103 "+seed).withClickEvent(new ClickEvent.RunCommand("/unchanged-publication103 "+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hint "+seed)));
            append(c,text.substring(at,end),current,fragments);at=end;}
        append(c,suffix,style(seed,99),fragments);return c;
    }
    static void result(Row r,String name,Component c,String time,String suffix){
        var output=dispatch(c);String expected=time+real(r.target,name)+suffix;String old=plain(c);check(plain(output).equals(expected),"translated: "+r.source+" => "+plain(output));
        check(old.equals(time+real(r.source,name)+suffix),"source immutable");check(dispatch(output)==output,"idempotence");var a=styles(c);var b=styles(output);
        int from=time.length()+r.source.indexOf(TOKEN),to=time.length()+r.target.indexOf(TOKEN);
        check(a.subList(from,from+name.length()).equals(b.subList(to,to+name.length())),"name every character/style/action retained");
        int identity=time.length()+prefix(r.source);check(a.subList(0,identity).equals(b.subList(0,identity)),"timestamp/identity/phone unchanged");
        if(!suffix.isEmpty())check(a.subList(old.length()-suffix.length(),old.length()).equals(b.subList(expected.length()-suffix.length(),expected.length())),"counter current styles/actions");
        String s=real(r.source,name),t=real(r.target,name);
        int sourceName=r.source.indexOf(TOKEN),targetName=r.target.indexOf(TOKEN);
        for(int side=0;side<2;side++){
            int ss=side==0?prefix(s):sourceName+name.length(),se=side==0?sourceName:s.length();
            int ts=side==0?prefix(t):targetName+name.length(),te=side==0?targetName:t.length();
            var terms=new ArrayList<Anchor>();for(var anchor:r.anchors){int x=s.indexOf(anchor.from,prefix(s));if(x>=ss&&x<se)terms.add(anchor);}
            terms.sort(Comparator.comparingInt(anchor->s.indexOf(anchor.from,prefix(s))));int x=ss,y=ts;
            for(int k=0;k<=terms.size();k++){
                var anchor=k<terms.size()?terms.get(k):null;int endSource=anchor==null?se:s.indexOf(anchor.from,prefix(s)),endTarget=anchor==null?te:t.indexOf(anchor.to,prefix(t));
                Style base=null;for(int n=x;n<endSource;n++)if(!Character.isWhitespace(s.charAt(n))){base=a.get(time.length()+n);break;}
                if(base==null&&x<endSource)base=a.get(time.length()+x);
                for(int n=y;n<endTarget;n++)check(base!=null&&b.get(time.length()+n).equals(base),"every gap glyph current style/action "+r.source+" @"+n);
                if(anchor!=null){for(int n=0;n<anchor.to.length();n++)check(b.get(time.length()+endTarget+n).equals(a.get(time.length()+endSource)),"semantic glyph current style/action");x=endSource+anchor.from.length();y=endTarget+anchor.to.length();}
            }
        }
    }
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);var root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);
        flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var evidence=JsonParser.parseString(Files.readString(root.resolve("NPC_NAMES_SOURCES_0.15.103.json"))).getAsJsonObject();var references=new HashMap<String,JsonObject>();
        for(var raw:evidence.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(references.put(r.get("source").getAsString(),r)==null,"unique evidence");}
        var rows=new ArrayList<Row>();
        for(String line:Files.readAllLines(root.resolve("NPC_NAMES_0.15.103_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] f=line.split("\\|",-1);String source="[NPC] "+f[0]+": "+f[1],target="[NPC] "+f[0]+": "+f[2];
            var ref=references.get(source);check(ref!=null&&ref.get("translation").getAsString().equals(target),"authored target equals pinned reference");var anchors=new ArrayList<Anchor>();
            for(var raw:ref.getAsJsonArray("style_anchors")){var a=raw.getAsJsonObject();anchors.add(new Anchor(a.get("source").getAsString(),a.get("target").getAsString()));}
            var layouts=new ArrayList<String>();for(var raw:ref.getAsJsonArray("layouts")){String legacy=raw.getAsString();check(NAME.matcher(plain(Component.literal(legacy))).replaceAll(TOKEN).equals(source),"publication source");layouts.add(legacy);}
            rows.add(new Row(source,target,List.copyOf(anchors),List.copyOf(layouts)));
        }
        check(rows.size()==135,"135 authored templates; not135 new exact-key translations");
        if(args.length>2&&args[2].equals("baseline")){
            var out=new JsonArray();for(var row:rows){var o=new JsonObject();o.addProperty("source",row.source);o.addProperty("flat",plain(dispatch(Component.literal(real(row.source,"Wither_101")))));var published=new JsonArray();for(var layout:row.layouts)published.add(plain(dispatch(publication(layout,"Wither_101",1,false,"",""))));o.add("published",published);out.add(o);}
            Files.writeString(config.resolveSibling("baseline-results.json"),out.toString());System.out.println("BASELINE135 rows");return;
        }
        int layouts=0;
        for(var row:rows){
            for(String name:List.of("a","_","Wither_101","Player123456789_"))for(int seed:List.of(3,6))for(boolean fragments:List.of(false,true))for(String suffix:List.of(""," (x1)"," (×999999)")){
                String time=fragments?"[12:34:56] ":"";result(row,name,fixture(row,name,seed,fragments,time,suffix),time,suffix);
            }
            for(var layout:row.layouts){layouts++;for(String name:List.of("a","Wither_101","Player123456789_"))for(boolean fragments:List.of(false,true))result(row,name,publication(layout,name,8,fragments,"[12:34:56] "," (x7)"),"[12:34:56] "," (x7)");}
            var input=fixture(row,"Wither_101",7,false,"","");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=message(input,channel);check(SkyRuClient.npcChat(m)==m,"player/client protected");}
            var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protected");
            flag("translateNpcChat",false);check(dispatch(input)==input,"NPC toggle");flag("translateNpcChat",true);
            for(String bad:List.of("","Player12345678901_","name with spaces","имя","<player>","name\nInjected","name!","[Name head]Name")){
                var c=Component.literal(real(row.source,bad));
                if(row.source.startsWith("[NPC] Ryan: ")&&bad.equals("[Name head]Name"))
                    check(plain(dispatch(c)).equals(real(row.target,bad)),"reviewed Ryan matching-head extension; EXPECTATION_UPDATES_0.16.3.json");
                else check(dispatch(c)==c,"unsupported name stays original: "+row.source+"/"+bad);
            }
            for(String altered:List.of(real(row.source,"Wither_101")+" added",real(row.source,"Wither_101").replace("[NPC] ","[BOSS] "),"prefix "+real(row.source,"Wither_101"))){var c=Component.literal(altered);check(dispatch(c)==c,"anchored closed template");}
        }
        check(layouts==136,"all136 frozen publication layouts");
        var sample=rows.getFirst();String source=real(sample.source,"Wither_101");var own=config.resolve("full-override");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\tМой перевод\n");
        var custom=new ChatNotificationTranslator(new OfflineDictionary(own));var c=Component.literal(source);check(custom.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"live full override");
        own=config.resolve("semantic-override");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),"Heart of the Forest\tМой термин\n");custom=new ChatNotificationTranslator(new OfflineDictionary(own));check(custom.translateNpcRequest(c,GuiMessageSource.SYSTEM_SERVER,false)==c,"semantic override");
        c=Component.literal("[NPC] Unpublished103: Hello Wither_101!");check(dispatch(c)==c,"unknown NPC not guessed");
        c=Component.literal("[NPC] Kat: He").append(Component.literal("y Wither_101!").setStyle(style(7,2)));check(dispatch(c)==c,"unreviewed mixed semantic style fails closed");
        // Same immutable offset plan for different current components, no cache of mutable message data.
        var clazz=Class.forName("ru.wither.skyru.NamedNpcTemplates");var constructor=clazz.getDeclaredConstructor();constructor.setAccessible(true);var model=constructor.newInstance();var method=clazz.getDeclaredMethod("plan",String.class,OfflineDictionary.class);method.setAccessible(true);
        var first=method.invoke(model,real(sample.source,"Wither_101"),db);var second=method.invoke(model,real(sample.source,"Other_2020"),db);check(first==second,"same-length precompiled offsets reused without live text/styles");
        System.out.println("PASS: NpcNames103 "+checks+" assertions;135 templates/92 NPC labels/136 published layouts;fresh colors/actions/names/counters/toggles/protected messages;not FPS or all-game completeness");
    }
}
