import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** All25 current and25 frozen complete cards, independent human expectations. */
public final class TestEnchantmentBatch59{
    private static int checks;private static Method row,plain,joined,numeric,styles;
    private static final Set<String> FAMILIES=Set.of("THUNDERLORD","CLEAVE","IMPALING","VENOMOUS");
    private static final Pattern NUMBER=Pattern.compile("[0-9]+(?:[.,][0-9]+)?");
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static Component row(String s,int mode,int seed)throws Exception{return(Component)row.invoke(null,s,mode,seed);}
    private static String plain(Component c)throws Exception{return(String)plain.invoke(null,c);}
    private static String joined(List<Component> c)throws Exception{return(String)joined.invoke(null,c);}
    private static List<String> numbers(String s){var out=new ArrayList<String>();var m=NUMBER.matcher(clean(s));while(m.find())out.add(m.group());return out;}
    private static String expected(String family,List<String> effect){var n=numbers(String.join("\n",effect));return switch(family){
        case "THUNDERLORD"->"После каждых "+n.get(0)+" попаданий по монстру\nмолния наносит\n"+n.get(1)+"% урона этого удара.";
        case "CLEAVE"->"Наносит "+n.get(0)+"% от вашего урона\nдругим существам в радиусе\n"+n.get(1)+" блока от цели.";
        case "IMPALING"->"Увеличивает урон по ⚓\nводным мобам на "+n.get(0)+"%.";
        default->"Замедляет передвижение цели на\n"+n.get(0)+"% и наносит +"+n.get(1)+"% вашего урона\nв секунду за попадание. Общий предел —\n"+n.get(2)+" попаданий. Длительность: "+n.get(3)+" с.";};}
    private static String footer(String raw){String s=clean(raw);for(var p:new String[][]{{"Combinable in Anvil","Можно объединить на наковальне"},{"Cleave","Рассечение"},{"Impaling","Пронзатель"},{"Venomous","Ядовитость"},{"Fateful Stinger","Судьбоносное жало"},{"Gunpowder Collection","Коллекция: Порох"},{"Pufferfish Collection","Коллекция: Иглобрюх"},{"Fishing Rod","Удочка"},{"Dungeons","Подземелья"},{"Chance","Шанс"},{"Execute","Казнь"},{"Prosecute","Приговор"},{"Titan Killer","Убийца титанов"},{"Giant Killer","Убийца великанов"},{"Thunderbolt","Разряд молнии"},{"Thunderlord","Повелитель грома"},{"Sources:","Источники:"},{"Source:","Источник:"},{"Applied To:","Применяется к:"},{"Requirements:","Требования:"},{"Conflicts:","Несовместимо с:"},{"Enchantment Table","Стол зачарований"},{"Experiments","Эксперименты"},{"Bazaar","Базар"},{"Catacombs","Катакомбы"},{"Diamond Collection","Коллекция: Алмаз"},{"Fishing Weapon","Рыболовное оружие"},{"Longsword","Длинный меч"},{"Sword","Меч"},{"Gauntlet","Рукавица"},{"Bow","Лук"},{"Dungeon","Подземелье"}})s=s.replace(p[0],p[1]);return s.replaceAll("Enchanting Level ([0-9]+)","Зачарование: уровень $1");}
    private static void anchor(List<Component> original,int a,String old,List<Component> result,int b,String next)throws Exception{String s=plain(original.get(a)),t=plain(result.get(b));check(s.contains(old)&&t.contains(next),"missing anchor "+old);var ss=(List<?>)styles.invoke(null,original.get(a));var tt=(List<?>)styles.invoke(null,result.get(b));check(ss.get(s.indexOf(old)).equals(tt.get(t.indexOf(next))),"semantic action/color "+old);}
    private static void test(String id,List<String> effect,List<String> full,Path root,OfflineDictionary db)throws Exception{
        String family=id.split(";")[0],target=expected(family,effect);check(numbers(String.join("\n",effect)).equals(numbers(target)),"actual source values "+id);
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            var original=new ArrayList<Component>();for(String s:effect)original.add(row(s,mode,seed));var result=TooltipTranslation.components(original,false);var sequence=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
            for(int repeat=0;repeat<5;repeat++){check(joined(result).equals(target),id+" mode"+mode+" => "+joined(result));check(String.join("\n",sequence.stream().map(AuditMiningStyles::plain).toList()).equals(target),"sequence");check(numeric.invoke(null,original).equals(numeric.invoke(null,result)),"every numeric glyph style/action");if(family.equals("THUNDERLORD"))anchor(original,1,"lightning",result,1,"молния");if(family.equals("IMPALING"))anchor(original,1,"Aquatic",result,1,"водным");if(family.equals("VENOMOUS")){anchor(original,1,"+",result,1,"+");anchor(original,3,"Lasts",result,3,"Длительность");}result=TooltipTranslation.components(result,false);sequence=TooltipTranslation.styled(sequence,false);}
            check(joined(original).equals(String.join("\n",effect.stream().map(TestEnchantmentBatch59::clean).toList())),"immutable source");var shift=new ArrayList<Component>();shift.add(Component.literal("Enchanted Book"));shift.addAll(original);var shifted=TooltipTranslation.components(shift,true);check(shifted.getFirst()==shift.getFirst()&&joined(shifted.subList(1,shifted.size())).equals(target),"Shift");
            for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,original.getFirst(),null,ch,null);check(SkyRuClient.npcChat(msg)==msg,"whole ordinary chat");}var signed=new GuiMessage(0,original.getFirst(),new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");
            var broken=new ArrayList<Component>(original);broken.set(broken.size()-1,Component.literal(plain(broken.getLast())+" UNKNOWN"));check(joined(TooltipTranslation.components(broken,false)).equals(joined(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbour");
            var whole=new ArrayList<Component>();whole.add(row("Enchanted Book",mode,seed));for(String s:full)whole.add(row(s,mode,seed));var actual=TooltipTranslation.components(whole,false);var expectedFull=new ArrayList<String>();expectedFull.add("Зачарованная книга");String[] targetRows=target.split("\n");for(int i=0;i<full.size();i++)expectedFull.add(i>=3&&i<3+effect.size()?targetRows[i-3]:footer(full.get(i)));check(joined(actual).equals(String.join("\n",expectedFull)),"whole card "+id+" mode"+mode+" => "+joined(actual));check(numeric.invoke(null,whole).equals(numeric.invoke(null,actual)),"whole-card source values/styles");
        }
        for(String key:List.of(clean(effect.getFirst()),family.equals("THUNDERLORD")?"lightning":family.equals("CLEAVE")?" blocks of the target.":family.equals("IMPALING")?"Aquatic":"per second per hit, stacking globally")){var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));var original=new ArrayList<Component>();for(String s:effect)original.add(row(s,2,8));check(joined(TooltipTranslation.components(original,false)).equals(joined(original.stream().map(SkyRuClient::component).toList())),"override group");flag("dictionary",db);}
    }
    private static void compositeSource(Path root,OfflineDictionary db)throws Exception{
        var source=Component.empty().append(Component.literal("VI: ").setStyle(Style.EMPTY.withInsertion("rank original"))).append(Component.literal("Experiments").setStyle(Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/source-first")))).append(Component.literal(", ").setStyle(Style.EMPTY.withInsertion("punctuation original"))).append(Component.literal("Bazaar").setStyle(Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/source-second"))));
        var result=SkyRuClient.component(source);check(plain(result).equals("VI: Эксперименты, Базар"),"two independently styled sources");anchor(List.of(source),0,"Experiments",List.of(result),0,"Эксперименты");anchor(List.of(source),0,"Bazaar",List.of(result),0,"Базар");
        var a=(List<?>)styles.invoke(null,source);var b=(List<?>)styles.invoke(null,result);check(a.get(plain(source).indexOf(",")).equals(b.get(plain(result).indexOf(","))),"source comma copied");
        for(String key:List.of("VI: Experiments, Bazaar","Experiments","Bazaar")){
            var dir=root.resolve("composite-override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));var actual=SkyRuClient.component(source);
            if(key.startsWith("VI:")){
                check(actual==source,"mixed whole-caption override preserves original instead of per-child fallback");var seq=source.getVisualOrderText();check(SkyRuClient.sequence(seq,false)==seq,"mixed whole-caption sequence identity");
                var uniform=Component.literal("VI: Experiments, Bazaar").setStyle(Style.EMPTY.withInsertion("uniform custom caption"));var custom=SkyRuClient.component(uniform);check(plain(custom).equals("Мой текст"),"uniform whole-caption override applied");check(((List<?>)styles.invoke(null,custom)).getFirst().equals(uniform.getStyle()),"whole-caption custom current style");
            }else check(plain(actual).contains("Мой текст")&&!plain(actual).equals("VI: Эксперименты, Базар"),"fragment override applied "+key);
        }flag("dictionary",db);
        var helper=Class.forName("ru.wither.skyru.EnchantBookFooter").getDeclaredMethod("edits",String.class);helper.setAccessible(true);for(String s:List.of("Cleave VII","Impaling VI","Thunderlord VIII","Venomous VIII","VI: Experiments, Unknown","VI: Experiments, Bazaar, Unknown","VII: Fateful Stinger UNKNOWN"))check(((List<?>)helper.invoke(null,s)).isEmpty(),"closed new caption refuses "+s);
    }
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);joined=TestSacks55.class.getDeclaredMethod("joined",List.class);joined.setAccessible(true);numeric=TestSacks55.class.getDeclaredMethod("numeric",List.class);numeric.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);int primary=0,frozen=0;
        var dir=Path.of(args[1]);for(var r:JsonParser.parseString(Files.readString(dir.resolve("ENCHANTMENT_PRIMARY_0.15.59.json"))).getAsJsonObject().getAsJsonArray("records")){var o=r.getAsJsonObject();test(o.get("id").getAsString(),o.getAsJsonArray("effect_legacy").asList().stream().map(JsonElement::getAsString).toList(),o.getAsJsonArray("full_lore_legacy").asList().stream().map(JsonElement::getAsString).toList(),root,db);primary++;}
        for(var r:JsonParser.parseString(Files.readString(dir.resolve("item-context.json"))).getAsJsonArray()){var o=r.getAsJsonObject();String id=o.get("id").getAsString();if(!id.contains(";")||!FAMILIES.contains(id.split(";")[0]))continue;var lore=o.getAsJsonArray("lore").asList().stream().map(JsonElement::getAsString).toList();int end=3;while(end<lore.size()&&!lore.get(end).isEmpty())end++;test(id,lore.subList(3,end),lore,root,db);frozen++;}
        check(primary==25&&frozen==25,"25 current/25 separate frozen cards");compositeSource(root,db);flag("enabled",false);var original=List.<Component>of(Component.literal("Increases the chance of a"));check(TooltipTranslation.components(original,false)==original,"disabled");
        System.out.println("PASS: enchantment batch59 "+checks+" assertions;25 pinned current/25 frozen complete cards/4 families,4 layouts/5 fresh actions/5 repeats,conditions/caps/values/actions/Shift/chat/overrides;not all enchants/live server");
    }
}
