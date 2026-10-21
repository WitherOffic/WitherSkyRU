import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Two captured full cards, not all attributes or all sack UI. */
public final class TestSackUpgrades56{
    private static int checks;private static Method row,plain,joined,numeric;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static Component row(String s,int mode,int seed)throws Exception{return(Component)row.invoke(null,s,mode,seed);}
    private static String text(List<Component> rows)throws Exception{return(String)joined.invoke(null,rows);}
    private static final List<String> POCKET=List.of("Карманный мешок для мешков","Объедините этот предмет на наковальне","с любым мешком. Прирост вместимости:","+200% от исходной вместимости.","","С одним мешком можно объединить","не более трёх таких предметов!","","Карманный мешок, который можно","вшить в другой мешок!","","SPECIAL");
    private static final List<String> SHARD=List.of("Осколок «Мешкопрят»","Вместимость мешков I","Мешки вмещают на +1% больше предметов.","","Этот осколок можно извлечь","из охотничьего ящика.","","Нажмите ПКМ, чтобы отправить в охотничий ящик!","Shift + ПКМ, чтобы переместить всё!","","❣ Требуется уровень навыка «Охота» 15.","EPIC ЛЕСНОЙ ОСКОЛОК (ID E16)");
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);joined=TestSacks55.class.getDeclaredMethod("joined",List.class);joined.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);int cards=0;
        var capture=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SACK_PRIMARY_0.15.55.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:capture){var r=raw.getAsJsonObject();String id=r.get("id").getAsString();if(!Set.of("POCKET_SACK_IN_A_SACK","ATTRIBUTE_SHARD_SACK_SIZE;1").contains(id))continue;cards++;var source=new ArrayList<String>();source.add(r.get("title_legacy").getAsString());for(var e:r.getAsJsonArray("lore_legacy"))source.add(e.getAsString());String target=String.join("\n",id.startsWith("POCKET")?POCKET:SHARD);
            for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
                var original=new ArrayList<Component>();for(String s:source)original.add(row(s,mode,seed));var result=TooltipTranslation.components(original,false);var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                for(int repeat=0;repeat<5;repeat++){check(text(result).equals(target),id+" mode"+mode+" => "+text(result));check(String.join("\n",seq.stream().map(AuditMiningStyles::plain).toList()).equals(target),"sequence");check(numeric.invoke(null,original).equals(numeric.invoke(null,result)),"per-glyph numbers/ID actions");result=TooltipTranslation.components(result,false);seq=TooltipTranslation.styled(seq,false);}
                check(text(original).equals(String.join("\n",source.stream().map(TestSackUpgrades56::clean).toList())),"immutable source");
                var shift=TooltipTranslation.components(original,true);check(shift.getFirst()==original.getFirst(),"Shift original title");check(text(shift.subList(1,shift.size())).equals(target.substring(target.indexOf('\n')+1)),"Shift body");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,original.getFirst(),null,channel,null);check(SkyRuClient.npcChat(msg)==msg,"whole ordinary chat");}
                var signed=new GuiMessage(0,original.getFirst(),new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");
            }
        }
        check(cards==2,"both full captured cards");
        var original=List.of(row("Your sacks can hold §a+1% §7more items.",0,1));var dir=root.resolve("custom");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),"Your sacks can hold \tМой текст\n");flag("dictionary",new OfflineDictionary(dir));check(text(TooltipTranslation.components(original,false)).equals(text(original.stream().map(SkyRuClient::component).toList())),"override group");flag("dictionary",db);
        for(String head:List.of("Combine this item in an anvil with any","This item can be combined with a sack","It's a pocket sack, to be stitched","You can Syphon this shard from")){var broken=List.<Component>of(Component.literal(head),Component.literal("UNKNOWN neighbour"));check(text(TooltipTranslation.components(broken,false)).equals(text(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbour");}
        flag("enabled",false);check(TooltipTranslation.components(original,false)==original,"disabled");
        System.out.println("PASS: sack upgrades56 "+checks+" assertions;two full captured cards/pocket3 paragraphs/shard bonus+syphon,4 styles/5 fresh actions/5 repeats,original percentages/IDs/Shift/chat/overrides;not all attributes/live windows");
    }
}
