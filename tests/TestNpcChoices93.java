import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Source-backed exact button groups;fresh styles/actions,never submit a command. */
public final class TestNpcChoices93 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){var s=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,t,p)->{s.appendCodePoint(p);return true;});return s.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,t,p)->{for(int n=0;n<Character.charCount(p);n++)a.add(t);return true;});return a;}
    private static GuiMessage msg(Component c,GuiMessageSource k){return new GuiMessage(0,c,null,k,null);}
    private static Component dispatch(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static Style appearance(int seed,int part){return Style.EMPTY.withColor((seed*0x4381+part*0x1279)&0xffffff).withBold(part%2==0).withItalic(seed%2==0).withShadowColor(seed).withInsertion("choice90 "+seed+"/"+part).withClickEvent(new ClickEvent.RunCommand("/never-execute90 "+seed+"/"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("fresh choice90 "+seed+"/"+part)));}
    private static void add(MutableComponent c,String text,Style s,boolean fragments){if(fragments)for(int n=0;n<text.length();n++)c.append(Component.literal(text.substring(n,n+1)).setStyle(s));else c.append(Component.literal(text).setStyle(s));}
    private static List<String> labels(JsonArray a){var out=new ArrayList<String>();for(var r:a)out.add(r.getAsString());return out;}
    private static void result(Component input,String expected,List<String> from,List<String> to,String time,String suffix){
        var output=dispatch(input);check(plain(output).equals(expected),"exact visible labels: "+plain(input)+" => "+plain(output));check(dispatch(output)==output,"idempotent object");var a=styles(input);var b=styles(output);
        int src=time.length()+"Select an option:".length(),dst=time.length()+"Выберите ответ:".length();
        check(a.subList(0,time.length()).equals(b.subList(0,time.length())),"time current style/actions");
        for(int n=time.length();n<dst;n++)check(b.get(n).equals(a.get(time.length())),"heading current style/actions");
        for(int i=0;i<from.size();i++){
            int x=plain(input).indexOf(from.get(i),src),y=plain(output).indexOf(to.get(i),dst);check(x>=src&&y>=dst,"ordered labels present");
            check(a.subList(src,x).equals(b.subList(dst,y)),"brackets/separators current style/actions retained");
            for(int n=y;n<y+to.get(i).length();n++)check(b.get(n).equals(a.get(x)),"button current command/hover/style retained");
            src=x+from.get(i).length();dst=y+to.get(i).length();
        }
        check(a.subList(src,a.size()).equals(b.subList(dst,b.size())),"closing brackets/counter current style/actions");
        var x=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(input));var y=java.util.regex.Pattern.compile("[0-9]+").matcher(plain(output));while(x.find())check(y.find()&&x.group().equals(y.group()),"numeric labels unchanged");check(!y.find(),"no added number");
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var sourceRoot=Path.of(args[1]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("chatNotifications",new ChatNotificationTranslator(db));
        var ref=JsonParser.parseString(Files.readString(sourceRoot.resolve("NPC_CHOICES_SOURCES_0.15.93.json"))).getAsJsonObject();var evidence=new HashMap<List<String>,JsonObject>();for(var raw:ref.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(evidence.put(labels(r.getAsJsonArray("source_labels")),r)==null,"unique source group");}
        int groups=0,buttons=0,layouts=0,malformed=0;
        for(String line:Files.readAllLines(sourceRoot.resolve("NPC_CHOICES_0.15.93_REVIEWED.tsv"))){
            if(line.isEmpty()||line.startsWith("#"))continue;String[] fields=line.split("\\|",-1);var from=labels(JsonParser.parseString(fields[0]).getAsJsonArray());var to=labels(JsonParser.parseString(fields[1]).getAsJsonArray());
            check(evidence.containsKey(from)&&from.size()==to.size(),"authored labels source-backed");groups++;buttons+=from.size();
            var unique=new HashSet<String>();for(var raw:evidence.get(from).getAsJsonArray("all_occurrences")){
                var o=raw.getAsJsonObject();String source=o.get("source").getAsString(),legacy=o.get("source_legacy_line").getAsString().strip();check(plain(Component.literal(legacy)).equals(source),"published exact layout");
                var malformedSources=Map.of("Select an option: Select an option: [Not really...]","p100454-r790601-o2823","Select an option: Select an option: [I honestly don't know...]","p100454-r790601-o3334");
                if(malformedSources.containsKey(source)){
                    check(o.get("block_id").getAsString().equals(malformedSources.get(source)),"exact known publication typo,not a general normalization");
                    boolean backed=false;for(var block:ref.getAsJsonArray("blocks")){var b=block.getAsJsonObject();if(b.get("block_id").equals(o.get("block_id")))backed=b.get("payload_raw").getAsString().contains("&eSelect an option: &eSelect an option: &a[&a"+from.getFirst()+"&a]");}
                    check(backed,"duplicate present in raw publication,not extractor invention");var original=Component.literal(legacy);check(dispatch(original)==original,"malformed publication not accepted as a new runtime grammar");malformed++;continue;
                }
                String expected=source.replace("Select an option:","Выберите ответ:");for(int n=0;n<from.size();n++)expected=expected.replace("["+from.get(n)+"]","["+to.get(n)+"]");
                if(unique.add(legacy)){result(Component.literal(legacy),expected,from,to,"","");layouts++;}
            }
            for(int seed=1;seed<=3;seed++)for(boolean fragmented:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)")){
                var input=Component.empty();add(input,time+"Select an option:",appearance(seed,0),fragmented);String expected=time+"Выберите ответ:";
                for(int n=0;n<from.size();n++){add(input," [",appearance(seed,20+n),fragmented);add(input,from.get(n),appearance(seed,n+1),fragmented);add(input,"]",appearance(seed,30+n),fragmented);expected+=" ["+to.get(n)+"]";}
                add(input,suffix,appearance(seed,40),fragmented);String original=plain(input);result(input,expected+suffix,from,to,time,suffix);check(plain(input).equals(original),"source immutable");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(input,channel);check(SkyRuClient.npcChat(m)==m,"user/client protected");}
                var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protected");
                flag("translateGameChat",false);check(dispatch(input)==input,"notification toggle respected");flag("translateGameChat",true);flag("translateNpcChat",false);check(plain(dispatch(input)).equals(expected+suffix),"notification toggle independent of NPC toggle");flag("translateNpcChat",true);
            }
            String base="Select an option: "+String.join(" ",from.stream().map(s->"["+s+"]").toList());
            for(String key:List.of(base,from.getFirst())){var own=root.resolve("override-"+groups+"-"+checks);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),key+"\tМоя кнопка\n");var input=Component.literal(base);check(new ChatNotificationTranslator(new OfflineDictionary(own)).translate(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"full/button override wins");}
        }
        check(groups==11&&buttons==12&&layouts==11&&malformed==0,"11 source groups/12 translated label slots;11 published layouts");
        for(String s:List.of("Select an option: [YES] [MAYBE]","Select an option: [Why?] [UNKNOWN]","Select an option: [Why?] UNKNOWN","[NPC] Unknown: Select an option: [Why?]","[VIP] Player: Select an option: [Why?]","Select an option: [❈] [✧] [⸕] [☘] [✎] [UNKNOWN]","Select an option: [1] [2] [3] [4] [5] [6] [7] [8] [9] [10] [11]","Select an option: [5,000] [26,000] [28,000] [30,000] [32,000] [34,000] [36,000] [38,000] [40,000] [50,000] [55,000] [60,000]")){var c=Component.literal(s);check(dispatch(c)==c,"unknown 6/11 and out-of-bound12 groups/protected prefix not guessed");}
        System.out.println("PASS: NPCChoices93 "+checks+" assertions;11 exact ordered groups/12 label slots/11 valid published layouts;12 changed labels;fresh commands/styles/hover,fragmentation,numbers,times,counters,overrides,toggles,signed/player/client guards;no commands executed/live certification");
    }
}
