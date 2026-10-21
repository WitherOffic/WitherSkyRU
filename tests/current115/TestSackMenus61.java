import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Observed menu fragments, independent expectations; not all live contents. */
public final class TestSackMenus61 {
    private static int checks;private static Method row,plain,styles,numeric;
    private static void check(boolean ok,String note){if(!ok)throw new AssertionError(note);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String text(Component c)throws Exception{return(String)plain.invoke(null,c);}
    private static Component input(String s,int mode,int seed)throws Exception{return(Component)row.invoke(null,s,mode,seed);}
    private static final String[][] LABELS={
        {"To Sack of Sacks","К сумке мешков"},{"Get From Sacks","Взять из мешков"},
        {"Enchanted Agronomy Sack","Мешок для зачарованного урожая"},{"Agronomy Sacks","Мешки для урожая"},
        {"Small Sack of Sacks","Малая сумка мешков"},{"Sack of Sacks III","Сумка мешков III"},
        {"Slots","Ячейки"},{"A sack which contains other sacks.","Сумка для хранения других мешков."},
        {"Sackception!","Мешки внутри мешка!"},{"Also accessible via /sacks","Также доступна через /sacks"},
        {"Unlock additional slots in your Sack of","Открывает дополнительные ячейки в сумке"},
        {"Obtain sack capacity upgrades as well","Улучшения вместимости мешков и"},
        {"as exclusive bits shop sacks.","особые мешки из магазина битов."},
        {"From Pocket Sack-in-a-Sack","От карманного мешка для мешков"},
        {"for sack total (","за всё содержимое мешка ("},
        {"Empty sack!","Мешок пуст!"},{"Stored:","В запасе:"},{"Total items:","Всего предметов:"},
        {"Click to pickup!","Нажмите, чтобы забрать!"},{"Click to put items in!","Нажмите, чтобы положить предметы!"}};
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);
        plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);
        styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var observed=new HashSet<String>();for(var r:JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SACK_MENUS_OBSERVED_0.15.61.json"))).getAsJsonObject().getAsJsonArray("records"))observed.add(r.getAsJsonObject().get("source").getAsString());
        check(observed.equals(new HashSet<>(Arrays.stream(LABELS).map(p->p[0]).toList())),"independently captured20 exact menu strings");
        for(var p:LABELS)for(int mode=0;mode<4;mode++)for(int seed=0;seed<3;seed++){
            var original=input(p[0],mode,seed);var current=original;var seq=original.getVisualOrderText();
            for(int repeat=0;repeat<5;repeat++){
                current=SkyRuClient.component(current);seq=SkyRuClient.sequence(seq,false);
                check(text(current).equals(p[1]),p[0]+" => "+text(current));
                check(AuditMiningStyles.plain(seq).equals(p[1]),"sequence label");
                check(numeric.invoke(null,List.of(original)).equals(numeric.invoke(null,List.of(current))),"original numbers");
                check(text(original).equals(p[0]),"immutable");
                var before=(List<?>)styles.invoke(null,original);var after=(List<?>)styles.invoke(null,current);check(after.stream().allMatch(s->s.equals(before.getFirst())),"fresh uniform label style/actions");
            }
            var signed=new GuiMessage(0,original,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed chat protected");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){
                var message=new GuiMessage(0,original,null,channel,null);check(SkyRuClient.npcChat(message)==message,"whole chat protected");
            }
        }
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<3;seed++)for(int[] range:List.of(new int[]{7,10},new int[]{11,13})){
            var original=new ArrayList<Component>();var target=new ArrayList<String>();
            for(int i=range[0];i<range[1];i++){original.add(input(LABELS[i][0],mode,seed));target.add(LABELS[i][1]);}
            var translated=TooltipTranslation.components(original,false);
            var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
            for(int n=0;n<5;n++){
                for(int i=0;i<target.size();i++){check(text(translated.get(i)).equals(target.get(i)),"complete paragraph");check(AuditMiningStyles.plain(seq.get(i)).equals(target.get(i)),"paragraph sequence");}
                translated=TooltipTranslation.components(translated,false);seq=TooltipTranslation.styled(seq,false);
            }
            if(range[0]==7){var before=(List<?>)styles.invoke(null,original.getLast());var after=(List<?>)styles.invoke(null,translated.getLast());int a=text(original.getLast()).indexOf("/sacks"),b=text(translated.getLast()).indexOf("/sacks");check(before.subList(a,a+6).equals(after.subList(b,b+6)),"command glyph actions");}
            var shifted=TooltipTranslation.components(original,true);check(shifted.getFirst()==original.getFirst(),"Shift original first row");
            var broken=new ArrayList<>(original);broken.set(broken.size()-1,Component.literal("Unknown menu neighbour"));
            check(TooltipTranslation.components(broken,false).getLast()==broken.getLast(),"unknown neighbour retained");
        }
        var custom=root.resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"To Sack of Sacks\tМоя сумка\n");flag("dictionary",new OfflineDictionary(custom));check(text(SkyRuClient.component(Component.literal("To Sack of Sacks"))).equals("Моя сумка"),"override priority");flag("dictionary",db);
        var roman=Component.literal("Sack of Sacks ");for(int i=0;i<3;i++)roman.append(Component.literal("I").setStyle(Style.EMPTY.withColor(0x100000+i).withInsertion("roman "+i)));
        var romanOut=SkyRuClient.component(roman);check(text(romanOut).equals("Сумка мешков III"),"Roman title");var rs=(List<?>)styles.invoke(null,roman);var rt=(List<?>)styles.invoke(null,romanOut);check(rs.subList(rs.size()-3,rs.size()).equals(rt.subList(rt.size()-3,rt.size())),"each Roman glyph/current event preserved");
        var f=RenderContext.class.getDeclaredField("CURRENT");f.setAccessible(true);var local=(ThreadLocal<RenderContext.Snapshot>)f.get(null);var previous=local.get();try{local.set(new RenderContext.Snapshot(true,false,false,false));for(var p:LABELS){var input=Component.literal(p[0]);check(SkyRuClient.component(input)==input,"input snapshot preserved");}}finally{if(previous==null)local.remove();else local.set(previous);}
        flag("enabled",false);var off=Component.literal("To Sack of Sacks");check(SkyRuClient.component(off)==off,"disabled");
        System.out.println("PASS: sack menus61 "+checks+" assertions;20 exact labels(15 newly reviewed),2 full paragraphs/4 layouts/3 fresh actions/5 repeats,commands/numbers/Shift/chat/overrides;not all live sack contents or creation clocks");
    }
}
