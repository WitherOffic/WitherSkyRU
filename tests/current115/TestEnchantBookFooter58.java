import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Display-only closed book captions; independent expected text, fresh glyph actions. */
public final class TestEnchantBookFooter58{
    private static int checks;private static Method row,plain,numeric,styles,edits;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String text(Component c)throws Exception{return(String)plain.invoke(null,c);}
    private static final String[][] SOURCES={{"Enchantment Table","Стол зачарований"},{"Experiments","Эксперименты"},{"Bazaar","Базар"},{"Catacombs","Катакомбы"},{"Dungeon","Подземелье"}};
    private static final String[][] NAMES={{"Chance","Шанс"},{"Execute","Казнь"},{"Prosecute","Приговор"},{"Titan Killer","Убийца титанов"},{"Thunderbolt","Разряд молнии"}};
    private static void test(String original,String target,String anchor,String translated)throws Exception{
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<3;seed++){
            var source=(Component)row.invoke(null,original,mode,seed);var result=source;var seq=source.getVisualOrderText();
            for(int repeat=0;repeat<5;repeat++){
                result=SkyRuClient.component(result);seq=SkyRuClient.sequence(seq,false);
                check(text(result).equals(target),"component caption "+original+" => "+text(result));
                check(AuditMiningStyles.plain(seq).equals(target),"sequence caption");
                check(numeric.invoke(null,List.of(source)).equals(numeric.invoke(null,List.of(result))),"source number glyphs/actions");
            }
            var a=(List<?>)styles.invoke(null,source);var b=(List<?>)styles.invoke(null,result);
            check(a.get(text(source).indexOf(anchor)).equals(b.get(target.indexOf(translated))),"semantic style/action");
            int dash=text(source).indexOf('-');if(dash>=0&&target.indexOf('-')>=0)check(a.get(dash).equals(b.get(target.indexOf('-'))),"bullet/range punctuation copied");
            check(text(source).equals(original.replaceAll("(?i)§[0-9a-fk-or]","")),"immutable source");
            for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,source,null,ch,null);check(SkyRuClient.npcChat(msg)==msg,"whole ordinary chat");}
            var signed=new GuiMessage(0,source,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed original");
        }
    }
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        edits=Class.forName("ru.wither.skyru.EnchantBookFooter").getDeclaredMethod("edits",String.class);edits.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(var p:SOURCES)for(String prefix:List.of("- "," - "))for(String range:List.of(""," (I-V)"," (III-V)"," (IV-V)"," (VI)"," (VII)"))test(prefix+"§b"+p[0]+"§7"+range,prefix+p[1]+range,p[0],p[1]);
        for(var p:SOURCES)for(String range:List.of("I-V","III-V","VI","VII"))for(String comma:List.of("",","))test("§a"+range+"§7: §b"+p[0]+"§7"+comma,range+": "+p[1]+comma,p[0],p[1]);
        for(var p:NAMES)for(int i=0;i<(p[0].equals("Chance")?5:p[0].equals("Execute")||p[0].equals("Prosecute")?6:7);i++){String rank=List.of("I","II","III","IV","V","VI","VII").get(i);test("§a"+p[0]+" §e"+rank,p[1]+" "+rank,p[0],p[1]);}
        for(var p:new String[][]{{"Sword","Меч"},{"Longsword","Длинный меч"},{"Gauntlet","Рукавица"},{"Bow","Лук"},{"Fishing Weapon","Рыболовное оружие"},{"Execute","Казнь"},{"Prosecute","Приговор"},{"Giant Killer","Убийца великанов"},{"Thunderlord","Повелитель грома"}})for(String prefix:List.of("- "," - "))test(prefix+"§b"+p[0],prefix+p[1],p[0],p[1]);
        for(String n:List.of("11","14","20","28","36"))for(String prefix:List.of("- "," - "))test(prefix+"§bEnchanting Level §a"+n,prefix+"Зачарование: уровень "+n,"Enchanting Level","Зачарование: уровень");
        for(String s:List.of("- Unknown source (I-V)","- Bazaar (IIII)","- Bazaar (V-I)","- Bazaar (VIII)","- Bazaar (I-)","VI: Dungeon UNKNOWN","- Enchanting Level 0","- Enchanting Level 01","- Enchanting Level 11 UNKNOWN","Chance VI","Execute VII","Prosecute VII","Thunderbolt VIII","- Sword UNKNOWN"))check(((List<?>)edits.invoke(null,s)).isEmpty(),"closed plan rejects invalid/unreviewed "+s);
        for(var p:new String[][]{{"Chance IV","Шанс IV","IV"},{"- Experiments (III-VI)","- Эксперименты (III-VI)","III-VI"},{"VI: Dungeon,","VI: Подземелье,","VI"}}){
            var original=Component.empty();int start=p[0].indexOf(p[2]);var rankStyles=new ArrayList<Style>();
            for(int i=0;i<p[0].length();i++){var st=Style.EMPTY.withInsertion("frame rank").withClickEvent(new ClickEvent.RunCommand("/rank-original"));if(i>=start&&i<start+p[2].length()){st=st.withColor(0x110000+i*0x101).withInsertion("rank glyph "+i);rankStyles.add(st);}original.append(Component.literal(p[0].substring(i,i+1)).setStyle(st));}
            var actual=SkyRuClient.component(original);check(text(actual).equals(p[1]),"independent Roman gradient");int next=p[1].indexOf(p[2]);var ss=(List<?>)styles.invoke(null,actual);check(ss.subList(next,next+p[2].length()).equals(rankStyles),"every Roman rank/dash glyph copied");
            var sequence=SkyRuClient.sequence(original.getVisualOrderText(),false);var out=new ArrayList<Style>();sequence.accept((i,s,c)->{for(int j=0;j<Character.charCount(c);j++)out.add(s);return true;});check(AuditMiningStyles.plain(sequence).equals(p[1])&&out.subList(next,next+p[2].length()).equals(rankStyles),"Roman sequence glyphs copied");
        }
        for(String key:List.of("Chance IV","Chance","- Experiments (VI)","Experiments","- Enchanting Level 14","Enchanting Level","Enchanting","Sword")){
            var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));
            String source=key.startsWith("Chance")?"Chance IV":key.contains("Experiments")?"- Experiments (VI)":key.equals("Sword")?"- Sword":"- Enchanting Level 14";
            String target=source.equals("Chance IV")?"Шанс IV":source.contains("Experiments")?"- Эксперименты (VI)":source.equals("- Sword")?"- Меч":"- Зачарование: уровень 14";
            check(!text(SkyRuClient.component((Component)row.invoke(null,source,2,7))).equals(target),"custom caption veto "+key);
        }
        flag("dictionary",db);flag("enabled",false);var source=Component.literal("- Experiments (VI)");check(SkyRuClient.component(source)==source,"disabled identity");
        System.out.println("PASS: book footer58 "+checks+" assertions;closed sources/roman ranges/31 titles/applied/conflicts/requirements,4 layouts/3 fresh actions/5 repeats,invalid shapes/custom veto/player/signed;display only,not acquisition rule validation");
    }
}
