import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Independent reviewed text, legacy observations and numerical/name/action guards. */
public final class TestNpc70 {
    private static int checks;
    private static final Pattern NUM=Pattern.compile("[0-9]+(?:,[0-9]{3})*");
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,value);}
    private static String text(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var r=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)r.add(s);return true;});return r;}
    private static Style fresh(int seed,int span){return Style.EMPTY.withColor((seed*0x10307+span*0x50701)&0xffffff).withInsertion("npc70 "+seed+"/"+span).withClickEvent(new ClickEvent.SuggestCommand("/original70-"+seed+"-"+span)).withHoverEvent(new HoverEvent.ShowText(Component.literal("hover70 "+seed+"/"+span)));}
    private static GuiMessage msg(Component c,GuiMessageSource ch){return new GuiMessage(0,c,null,ch,null);}
    private static Component queued(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
    private static void digits(Component before,Component after){var a=NUM.matcher(text(before));var b=NUM.matcher(text(after));var x=styles(before);var y=styles(after);while(a.find()){check(b.find()&&a.group().equals(b.group()),"value copied");check(x.subList(a.start(),a.end()).equals(y.subList(b.start(),b.end())),"individual numeric glyph actions");}check(!b.find(),"no invented value");}
    private static Component decorate(Component raw,int seed,int mode){String s=text(raw);var ss=styles(raw);var out=Component.empty();for(int i=0;i<s.length();){int end=i+1;while(end<s.length()&&end<i+(mode==0?8:2)&&ss.get(end).equals(ss.get(i)))end++;out.append(Component.literal(s.substring(i,end)).setStyle(ss.get(i).withInsertion("fresh70 "+seed).withClickEvent(new ClickEvent.RunCommand("/unchanged70-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("current70 "+seed)))));i=end;}return out;}
    private static void guards(Component source,Component output,String target)throws Exception{
        check(text(output).equals(target),"complete target: "+text(source)+" => "+text(output));digits(source,output);check(queued(output)==output,"idempotence");for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(source,ch);check(SkyRuClient.npcChat(m)==m,"player/client source unchanged");}var signed=new GuiMessage(0,source,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed source unchanged");flag("translateNpcChat",false);check(queued(source)==source,"NPC toggle");flag("translateNpcChat",true);flag("translateGameChat",false);check(text(queued(source)).equals(target),"NPC independent of notices");flag("translateGameChat",true);
    }
    private static String expectedDynamic(String s,OfflineDictionary db){
        String head="[NPC] Kat: ";if(s.startsWith(head)){
            if(s.startsWith(head+"Come back in "))return head+"Возвращайся через "+units(s.substring((head+"Come back in ").length(),s.length()-" to pick it up!".length()))+" чтобы забрать питомца!";
            if(s.startsWith(head+"You can pick it up in "))return head+"Питомца можно забрать через "+units(s.substring((head+"You can pick it up in ").length(),s.length()-1))+".";
        }
        head="[NPC] Jacob: ";if(!s.startsWith(head))return null;String body=s.substring(head.length());
        var m=Pattern.compile("PERSONAL BEST! You've surpassed your previous record of ([0-9,]+) items collected in the (.+) Contest!").matcher(body);
        if(m.matches())return head+"ЛИЧНЫЙ РЕКОРД! Ты побил прежний рекорд: "+m.group(1)+" "+noun(m.group(1))+" на соревновании по культуре «"+db.translateForSearch(m.group(2))+"»!";
        m=Pattern.compile("You placed in the (BRONZE|SILVER|GOLD|PLATINUM|DIAMOND) bracket in the (.+) Contest!").matcher(body);
        if(m.matches())return head+"Твоя категория — «"+Map.of("BRONZE","БРОНЗА","SILVER","СЕРЕБРО","GOLD","ЗОЛОТО","PLATINUM","ПЛАТИНА","DIAMOND","АЛМАЗ").get(m.group(1))+"» на соревновании по культуре «"+db.translateForSearch(m.group(2))+"»!";
        m=Pattern.compile("You didn't earn a medal, but you may claim a participation reward in the (.+) Contest!").matcher(body);if(m.matches())return head+"Медаль не получена, но награда за участие доступна на соревновании по культуре «"+db.translateForSearch(m.group(1))+"»!";
        m=Pattern.compile("You collected ([0-9,]+) items! PERSONAL BEST!").matcher(body);if(m.matches())return head+"Ты собрал "+m.group(1)+" "+noun(m.group(1))+"! ЛИЧНЫЙ РЕКОРД!";
        return null;
    }
    private static String units(String s){return s.replaceAll("\\bdays?\\b","дн").replaceAll("\\bhours?\\b","ч").replaceAll("\\bminutes?\\b","мин").replaceAll("\\bseconds?\\b","с");}
    private static String noun(String s){int n=Integer.parseInt(s.replace(",",""));return n%100>=11&&n%100<=14?"предметов":n%10==1?"предмет":n%10>=2&&n%10<=4?"предмета":"предметов";}
    private static void semantic(Component before,Component after,String from,String to){int a=text(before).indexOf(from),b=text(after).indexOf(to);check(a>=0&&b>=0,"semantic term exists");var x=styles(before);var y=styles(after);for(int n=b;n<b+to.length();n++)check(y.get(n).equals(x.get(a)),"semantic current style/action "+to);}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var source=Path.of(args[1]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        var targets=new HashMap<String,String>();for(String line:Files.readAllLines(source.resolve("NPC_0.15.70_REVIEWED.tsv"))){if(line.isEmpty()||line.startsWith("#"))continue;var f=line.split("\\|",-1);String prefix="[NPC] "+f[0]+": ";check(targets.put(prefix+f[1],prefix+f[2])==null,"unique reviewed source");}
        check(targets.size()==22&&NpcDialogueTranslator.storyDialogues().phraseCount()==10799,"22 retained additions,581 base plus239 NPC74 and100 separately checked NPC75 additions plus137 NPC77 additions plus324 NPC78 additions");
        var evidence=JsonParser.parseString(Files.readString(source.resolve("NPC_OBSERVED_0.15.70.json"))).getAsJsonObject();int exact=0,dynamic=0;var seen=new HashSet<String>();
        for(var raw:evidence.getAsJsonArray("records")){var row=raw.getAsJsonObject();String original=row.get("source").getAsString(),expected=targets.get(original);boolean literal=expected!=null;if(expected==null)expected=expectedDynamic(original,db);if(expected==null)continue;String sample=row.get("sample").getAsString();var observed=Component.literal(sample);String prefix=text(observed).substring(0,text(observed).indexOf("[NPC]"));
            check(seen.add(original),"independent observation once");for(int seed=1;seed<=3;seed++)for(int mode=0;mode<2;mode++)for(String suffix:List.of(""," (x17)"," (×2)")){
                var input=decorate(observed.copy().append(Component.literal(suffix)),seed,mode);var out=queued(input);guards(input,out,prefix+expected+suffix);check(text(input).equals(text(observed)+suffix),"immutable observed input");for(var s:styles(out)){check(s.getInsertion().equals("fresh70 "+seed),"fresh insertion");check(s.getClickEvent().equals(new ClickEvent.RunCommand("/unchanged70-"+seed)),"fresh click");}
                if(original.contains("PERSONAL BEST"))semantic(input,out,"PERSONAL BEST","ЛИЧНЫЙ РЕКОРД");
            }
            if(literal)exact++;else dynamic++;
        }
        check(exact==22&&dynamic>=17,"22 exact and at least17 independent numeric examples;got "+exact+"/"+dynamic);
        // Crops/ranks are closed identities, not arbitrary player/item names.
        for(String crop:List.of("Wheat","Carrot","Potato","Nether Wart","Sugar Cane","Mushroom","Cocoa Beans","Cactus","Pumpkin","Melon"))for(String rank:List.of("BRONZE","SILVER","GOLD","PLATINUM","DIAMOND")){
            String s="[NPC] Jacob: You placed in the "+rank+" bracket in the "+crop+" Contest!";var c=Component.literal(s).setStyle(fresh(10,0));var out=queued(c);guards(c,out,expectedDynamic(s,db));
        }
        for(String n:List.of("1","2","5","11","21","1,280","187,857"))for(String crop:List.of("Carrot","Sugar Cane")){
            String s="[NPC] Jacob: PERSONAL BEST! You've surpassed your previous record of "+n+" items collected in the "+crop+" Contest!";var c=Component.literal(s).setStyle(fresh(11,0));guards(c,queued(c),expectedDynamic(s,db));
        }
        String[] times={"0 seconds","1 second","5 minutes","1 day","2 days","20 hours 57 minutes 25 seconds","1 day 0 hours 1 minute 2 seconds","9999 days 23 hours 59 minutes 59 seconds"};
        for(String time:times)for(boolean pickup:List.of(true,false)){String s="[NPC] Kat: "+(pickup?"You can pick it up in "+time+".":"Come back in "+time+" to pick it up!");var c=Component.literal(s).setStyle(fresh(12,0));guards(c,queued(c),expectedDynamic(s,db));}
        for(String nick:List.of("power","excellent","wise","choice","A","Z_1","ABC_123456789012"))for(String head:List.of("","["+nick+" head]","\uFFFC"))for(boolean power:List.of(true,false))for(int seed=1;seed<=3;seed++)for(String suffix:List.of(""," (x17)"," (×2)")){
            String start="[05:23:11] [NPC] Maxwell: ",a=power?start:start+"A wise choice, ",b=power?", that's an excellent power!":"!",expected=(power?start:start+"Мудрый выбор, ")+head+nick+(power?", отличная сила!":"!")+suffix;
            var c=Component.literal(a).setStyle(fresh(seed,0));String name=head+nick;for(int i=0;i<name.length();i++)c.append(Component.literal(name.substring(i,i+1)).setStyle(fresh(seed,20+i)));c.append(Component.literal(b+suffix).setStyle(fresh(seed,0)));var out=queued(c);guards(c,out,expected);int from=a.length(),to=expected.indexOf(name);check(styles(c).subList(from,from+name.length()).equals(styles(out).subList(to,to+name.length())),"each nickname/head glyph copied,including word collisions");
        }
        for(String bad:List.of("[NPC] Kat: You can pick it up in 24 hours.","[NPC] Kat: Come back in 1 second 2 days to pick it up!","[NPC] Kat: Come back in 2 weeks to pick it up!","[NPC] Kat: Come back in -1 day to pick it up!","[NPC] Maxwell: Name with spaces, that's an excellent power!","[NPC] Maxwell: A wise choice, [Other head]Name!","[NPC] Maxwell: ABC_12345678901234, that's an excellent power!","[NPC] Jacob: You placed in the BLUE bracket in the Carrot Contest!","[NPC] Jacob: You placed in the SILVER bracket in the Unknown Crop Contest!")){var c=Component.literal(bad);check(queued(c)==c,"malformed/unknown left unchanged: "+bad);}
        // Full and semantic user edits take precedence over every new dynamic family.
        String[][] custom={{"[NPC] Maxwell: power, that's an excellent power!","power"},{"[NPC] Kat: Come back in 2 days to pick it up!","days"},{"[NPC] Jacob: You placed in the SILVER bracket in the Carrot Contest!","Carrot"}};
        for(var p:custom)for(String key:List.of(p[0],p[1])){var path=root.resolve("own-"+checks);Files.createDirectories(path);Files.writeString(path.resolve("overrides.tsv"),key+"\tМой текст\n");var own=new OfflineDictionary(path);flag("dictionary",own);flag("chatNotifications",new ChatNotificationTranslator(own));var c=Component.literal(p[0]);check(queued(c)==c,"custom full/semantic override");}flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));
        System.out.println("PASS: NPC70 "+checks+" assertions;22 observed exact lines;all39 independent observed gap samples;Maxwell2 named forms incl collision names/heads/per-glyph actions;Kat2 ordered duration sentences;Jacob3 result forms plus collected color boundary;10 crops/5 ranks;not all NPCs/live FPS");
    }
}
