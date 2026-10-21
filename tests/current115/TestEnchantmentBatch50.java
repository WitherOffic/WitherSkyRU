import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** All eight current Cayenne/Cleave cards, including legacy formatting/full source footers. */
public final class TestEnchantmentBatch50 {
    private static int checks;
    private static final Pattern NUMBER=Pattern.compile("[0-9]+(?:[.,][0-9]+)?");
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/original-body50"));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withInsertion("original number");
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String text(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static String joined(List<Component> rows){return String.join("\n",rows.stream().map(TestEnchantmentBatch50::text).toList());}
    private static List<String> numbers(String s){var result=new ArrayList<String>();var m=NUMBER.matcher(s);while(m.find())result.add(m.group());return result;}
    private static List<Style> numericStyles(List<Component> rows){var result=new ArrayList<Style>();for(var row:rows){var styles=new ArrayList<Style>();row.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)styles.add(s);return true;});var m=NUMBER.matcher(text(row));while(m.find())result.addAll(styles.subList(m.start(),m.end()));}return result;}
    private static Style styleAt(Component c,String word){int at=text(c).indexOf(word);check(at>=0,"missing semantic word "+word);var ss=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)ss.add(s);return true;});return ss.get(at);}
    private static void semantic(List<Component> original,List<Component> result,String family){
        if(family.equals("CAYENNE"))for(String[] p:new String[][]{{"0","Health","0","здоровья"},{"0","True","0","истинной"},{"1","Defense","1","защиты"},{"1","Accessory","1","силы"},{"2","Power","2","аксессуаров"},{"0","","0",""},{"0","","0",""}})
            check(styleAt(original.get(Integer.parseInt(p[0])),p[1]).equals(styleAt(result.get(Integer.parseInt(p[2])),p[3])),"semantic colors/actions "+p[1]);
    }
    private static String footer(String s){return clean(s).replace("Combinable in Anvil","Можно объединить на наковальне").replace("Cayenne","Кайенский перец").replace("Cleave","Рассечение")
        .replace("Sources:","Источники:").replace("Applied To:","Применяется к:").replace("Requirements:","Требования:").replace("Chili Pepper Collection","Коллекция перца чили")
        .replace("Enchantment Table","Стол зачарований").replace("Experiments","Эксперименты").replace("Bazaar","Базар").replace("Gloves","Перчатки").replace("Belt","Пояс")
        .replace("Necklace","Ожерелье").replace("Bracelet","Браслет").replace("Cloak","Плащ").replace("Longsword","Длинный меч").replace("Sword","Меч").replace("Gauntlet","Рукавица").replace("Enchanting Level 4","Зачарование: уровень 4");}
    private static Component styled(String s,int mode){
        if(mode==0)return Component.literal(s);s=clean(s);if(mode==1)return Component.literal(s).setStyle(BODY);
        var out=Component.empty();int end=0;var m=NUMBER.matcher(s);
        while(m.find()){out.append(Component.literal(s.substring(end,m.start())).setStyle(BODY));String v=m.group();
            if(mode==3)for(int i=0;i<v.length();i++)out.append(Component.literal(v.substring(i,i+1)).setStyle(VALUE.withColor(0x110000+i*0x101).withInsertion("digit"+i)));
            else out.append(Component.literal(v).setStyle(VALUE));end=m.end();}
        return out.append(Component.literal(s.substring(end)).setStyle(BODY));
    }
    private static String expected(String family,String source){var n=numbers(source);return family.equals("CAYENNE")
        ?"Даёт +"+n.get(0)+" здоровья и +"+n.get(1)+" истинной\nзащиты за каждую цифру в показателе силы\nаксессуаров."
        :"Наносит "+n.get(0)+"% от вашего урона\nдругим существам в радиусе\n"+n.get(1)+" блока от цели.";}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var corpus=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("ENCHANTMENT_CURRENT_REPO_2026-10-13.json"))).getAsJsonObject();int cards=0;
        for(var raw:corpus.getAsJsonArray("records")){
            var record=raw.getAsJsonObject();String id=record.get("id").getAsString(),family=id.split(";")[0];if(!Set.of("CAYENNE","CLEAVE").contains(family))continue;cards++;
            var effects=record.getAsJsonArray("effect_legacy");String source=String.join("\n",effects.asList().stream().map(e->clean(e.getAsString())).toList()),target=expected(family,source);
            check(numbers(source).equals(numbers(target)),"source quantities "+id);
            for(int mode=0;mode<4;mode++){
                final int layout=mode;var original=effects.asList().stream().map(e->styled(e.getAsString(),layout)).toList();var result=TooltipTranslation.components(original,false);
                var sequences=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                for(int repeat=0;repeat<5;repeat++){
                    check(joined(result).equals(target),id+" mode"+mode+" => "+joined(result));
                    check(String.join("\n",sequences.stream().map(AuditMiningStyles::plain).toList()).equals(target),"sequence "+id);
                    check(numericStyles(original).equals(numericStyles(result)),"number styles/actions "+id);check(joined(original).equals(source),"immutable source "+id);
                    semantic(original,result,family);
                    result=TooltipTranslation.components(result,false);sequences=TooltipTranslation.styled(sequences,false);
                }
                var withTitle=new ArrayList<Component>();withTitle.add(Component.literal("Enchanted Book"));withTitle.addAll(original);
                var shift=TooltipTranslation.components(withTitle,true);check(text(shift.getFirst()).equals("Enchanted Book"),"Shift title");check(joined(shift.subList(1,shift.size())).equals(target),"Shift body");
                for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,original.getFirst(),null,ch,null);check(SkyRuClient.npcChat(msg)==msg,"ordinary channel protected");}
                var broken=new ArrayList<Component>(original);broken.set(broken.size()-1,styled(effects.get(effects.size()-1).getAsString()+" UNKNOWN",mode));
                check(joined(TooltipTranslation.components(broken,false)).equals(joined(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbor declines group");
            }
            var full=new ArrayList<Component>();full.add(Component.literal("Enchanted Book"));for(var line:record.getAsJsonArray("full_lore_legacy"))full.add(Component.literal(line.getAsString()));
            var result=TooltipTranslation.components(full,false);check(joined(result.subList(4,7)).equals(target),"whole card effect "+id);check(numericStyles(full).equals(numericStyles(result)),"whole card quantities/actions");
            String card=joined(result);check(card.contains("Источники:")&&card.contains("Применяется к:")&&card.contains("Можно объединить на наковальне"),"whole card headings "+id+" => "+card);
            var expectedFull=new ArrayList<String>();expectedFull.add("Зачарованная книга");int index=0;String[] expectedEffect=target.split("\n");
            for(var line:record.getAsJsonArray("full_lore_legacy")){expectedFull.add(index>=3&&index<6?expectedEffect[index-3]:footer(line.getAsString()));index++;}
            check(card.equals(String.join("\n",expectedFull)),"all full card rows "+id+" => "+card);
            check(card.contains(family.equals("CAYENNE")?"Кайенский перец":"Рассечение"),"whole card name");
            for(String key:List.of(clean(effects.get(0).getAsString()),family.equals("CAYENNE")?"Health":"blocks of the target.")){
                var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));
                var original=effects.asList().stream().map(e->styled(e.getAsString(),2)).toList();check(joined(TooltipTranslation.components(original,false)).equals(joined(original.stream().map(SkyRuClient::component).toList())),"custom group override");flag("dictionary",db);
            }
        }
        check(cards==8,"eight current cards");flag("enabled",false);var original=List.<Component>of(Component.literal("Deals 5% of your damage"));check(TooltipTranslation.components(original,false)==original,"disabled identity");flag("enabled",true);
        System.out.println("PASS: enchantment batch50 "+checks+" assertions;8 current full cards/2 families; four styles/five repeats; not all live server");
    }
}
