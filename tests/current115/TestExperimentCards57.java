import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Two complete pinned primary item cards, not menu reconstruction. */
public final class TestExperimentCards57{
    private static int checks;private static Method row,plain,joined,numeric,styles;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static Component row(String s,int mode,int seed)throws Exception{return(Component)row.invoke(null,s,mode,seed);}
    private static String text(List<Component> rows)throws Exception{return(String)joined.invoke(null,rows);}
    private static final List<String> TABLE=List.of("Стол экспериментов","Разместите этот предмет на своём острове!","","Проводите безумные опыты зачарования,","получая дополнительный опыт зачарования и предметы!","","RARE");
    private static final List<String> SERUM=List.of("Метафизическая сыворотка","Расходуемый предмет","","Сыворотка сокращает длину","цепочки, необходимой для Суперпар","в дополнительных опытах на 1!","","Можно выпить не более 3 доз","сыворотки, пока её вкус","не станет слишком абстрактным.","","Нажмите ПКМ, чтобы использовать!","","EPIC");
    private static void anchor(List<Component> from,int a,String word,List<Component> to,int b,String next)throws Exception{String s=(String)plain.invoke(null,from.get(a)),t=(String)plain.invoke(null,to.get(b));check(s.contains(word)&&t.contains(next),"anchor "+word);check(((List<?>)styles.invoke(null,from.get(a))).get(s.indexOf(word)).equals(((List<?>)styles.invoke(null,to.get(b))).get(t.indexOf(next))),"semantic color/action "+word);}
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);joined=TestSacks55.class.getDeclaredMethod("joined",List.class);joined.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);int cards=0;
        var capture=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("EXPERIMENT_PRIMARY_0.15.57.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:capture){var r=raw.getAsJsonObject();if(!r.has("id"))continue;String id=r.get("id").getAsString();cards++;var source=new ArrayList<String>();source.add(r.get("title_legacy").getAsString());for(var e:r.getAsJsonArray("lore_legacy"))source.add(e.getAsString());String target=String.join("\n",id.equals("EXPERIMENTATION_TABLE")?TABLE:SERUM);
            for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
                var original=new ArrayList<Component>();for(String s:source)original.add(row(s,mode,seed));var result=TooltipTranslation.components(original,false);var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                for(int repeat=0;repeat<5;repeat++){check(text(result).equals(target),id+" mode"+mode+" => "+text(result));check(String.join("\n",seq.stream().map(AuditMiningStyles::plain).toList()).equals(target),"sequence");check(numeric.invoke(null,original).equals(numeric.invoke(null,result)),"original per-glyph values/actions");
                    if(id.equals("EXPERIMENTATION_TABLE")){anchor(original,4,"Enchanting Exp",result,4,"опыт зачарования");anchor(original,4,"items",result,4,"предметы");}else{anchor(original,4,"Superpairs",result,4,"Суперпар");anchor(original,9,"too abstract",result,9,"слишком абстрактным");}
                    result=TooltipTranslation.components(result,false);seq=TooltipTranslation.styled(seq,false);
                }
                var shift=TooltipTranslation.components(original,true);check(shift.getFirst()==original.getFirst(),"Shift title");check(text(shift.subList(1,shift.size())).equals(target.substring(target.indexOf('\n')+1)),"Shift body");
                for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,original.getFirst(),null,channel,null);check(SkyRuClient.npcChat(msg)==msg,"ordinary chat");}
                var signed=new GuiMessage(0,original.getFirst(),new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");
            }
        }
        check(cards==2,"two pinned full cards");var original=List.of(row("Conduct wild enchanting experiments to",2,9),row("gain bonus §bEnchanting Exp §7and §eitems§7!",2,9));
        var dir=root.resolve("override");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),"Enchanting Exp\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));check(text(TooltipTranslation.components(original,false)).equals(text(original.stream().map(SkyRuClient::component).toList())),"override group declines");flag("dictionary",db);
        var broken=List.<Component>of(original.getFirst(),Component.literal("gain bonus Enchanting Exp and items! UNKNOWN"));check(text(TooltipTranslation.components(broken,false)).equals(text(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbour");flag("enabled",false);check(TooltipTranslation.components(original,false)==original,"disabled");
        System.out.println("PASS: experiment cards57 "+checks+" assertions;two full primary cards/4 styles/5 fresh actions/5 repeats/semantic colors+italic/values/Shift/chat/overrides;not full live table GUI");
    }
}
