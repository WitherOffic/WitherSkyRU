import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Full current27 cards plus distinct frozen formats; independent human expectations. */
public final class TestEnchantmentBatch54 {
    private static int checks;
    private static final Set<String> FAMILIES=Set.of("CRITICAL","CUBISM","ENDER_SLAYER","GIANT_KILLER");
    private static final Pattern NUM=Pattern.compile("[0-9]+(?:[.,][0-9]+)?");
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static String joined(List<Component> c){return String.join("\n",c.stream().map(TestEnchantmentBatch54::plain).toList());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static List<String> numbers(String s){var out=new ArrayList<String>();var m=NUM.matcher(s);while(m.find())out.add(m.group());return out;}
    private static List<Style> numericStyles(List<Component> rows){var out=new ArrayList<Style>();for(var row:rows){var ss=styles(row);var m=NUM.matcher(plain(row));while(m.find())out.addAll(ss.subList(m.start(),m.end()));}return out;}
    private static Component row(String raw,int mode,int seed){
        var base=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/original-enchant54-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hint "+seed))).withInsertion("frame "+seed);var parsed=Component.literal(raw).setStyle(base);
        if(mode==0)return parsed;if(mode==1)return Component.literal(clean(raw)).setStyle(base.withColor(0xaaaaaa));
        String text=plain(parsed);var ss=styles(parsed);var out=Component.empty();for(int i=0;i<text.length();i++){Style s=ss.get(i);if(mode==3&&Character.isDigit(text.charAt(i)))s=s.withColor(0x100000+i*0x101).withInsertion("digit "+seed+"/"+i);out.append(Component.literal(text.substring(i,i+1)).setStyle(s));}return out;
    }
    private static Style at(Component c,String word){int p=plain(c).indexOf(word);check(p>=0,"missing "+word);return styles(c).get(p);}
    private static String expected(String family,List<String> source){var n=numbers(String.join("\n",source));return switch(family){
        case "CRITICAL"->{String icon=source.getFirst().contains("☠")?"☠":"";yield source.size()==1?"Увеличивает "+icon+" критический урон на "+n.get(0)+"%.":"Увеличивает "+icon+" критический урон на\n"+n.get(0)+"%.";}
        case "CUBISM"->source.size()==2?"Увеличивает урон по  кубическим\nсуществам на "+n.get(0)+"%.":"Увеличивает урон по\n"+source.get(1).replace("Magma Cubes","магмовым кубам").replace("Slimes","слизням").replace("Creepers","криперам").replace("and","а также")+"\n"+source.get(2).replace("Magma Cubes","магмовым кубам").replace("Creepers","криперам").replace(" by "," на ");
        case "ENDER_SLAYER"->"Увеличивает урон по "+(source.getFirst().contains("⊙")?"⊙":"")+" существам Края\nна "+n.get(0)+"%.";
        default->"Увеличивает урон на "+n.get(0)+"%\nза каждый процент, на который\nздоровье цели превышает ваше.\nБонус — не более "+n.get(1)+"%.";};}
    private static String footer(String raw){String s=clean(raw);var pairs=new String[][]{{"Combinable in Anvil","Можно объединить на наковальне"},{"Critical","Критический удар"},{"Cubism","Кубизм"},{"Ender Slayer","Истребитель эндеров"},{"Giant Killer","Убийца великанов"},{"Titan Killer","Убийца титанов"},{"Sources:","Источники:"},{"Source:","Источник:"},{"Applied To:","Применяется к:"},{"Requirements:","Требования:"},{"Conflicts:","Несовместимо с:"},{"Sven Packmaster","Свен — вожак стаи"},{"Pearl Dealer","Торговец жемчугом"},{"End Stone Idol","Идол из эндерняка"},{"Enchantment Table","Стол зачарований"},{"Experiments","Эксперименты"},{"Bazaar","Базар"},{"Dark Auction","Тёмный аукцион"},{"Scorpius","Скорпиус"},{"Fishing Weapon","Рыболовное оружие"},{"Longsword","Длинный меч"},{"Sword","Меч"},{"Gauntlet","Рукавица"},{"Bow","Лук"}};for(var p:pairs)s=s.replace(p[0],p[1]);return s.replaceAll("Enchanting Level ([0-9]+)","Зачарование: уровень $1");}
    private static void test(String id,List<String> effect,List<String> full,Path root,OfflineDictionary db)throws Exception{
        String family=id.split(";")[0],target=expected(family,effect.stream().map(TestEnchantmentBatch54::clean).toList());check(numbers(clean(String.join("\n",effect))).equals(numbers(target)),"source values "+id);
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            final int layout=mode,frame=seed;var original=effect.stream().map(s->row(s,layout,frame)).toList();var result=TooltipTranslation.components(original,false);var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
            for(int n=0;n<5;n++){check(joined(result).equals(target),id+" mode"+mode+" => "+joined(result));check(String.join("\n",seq.stream().map(AuditMiningStyles::plain).toList()).equals(target),"Sequence expected");check(numericStyles(original).equals(numericStyles(result)),"number per-glyph actions");
                if(family.equals("CUBISM")&&original.size()==3){int at=plain(original.get(1)).contains("Magma Cubes")?1:2;check(at(original.get(at),"Magma Cubes").equals(at(result.get(at),"магмовым кубам")),"old creature color/action");}
                else if(!family.equals("GIANT_KILLER")){String from=family.equals("CRITICAL")?"Crit Damage":family.equals("CUBISM")?"Cubic":"Ender",to=family.equals("CRITICAL")?"критический урон":family.equals("CUBISM")?"кубическим":"существам Края";check(at(original.getFirst(),from).equals(at(result.getFirst(),to)),"semantic color/action "+from);}
                result=TooltipTranslation.components(result,false);seq=TooltipTranslation.styled(seq,false);
            }
            var withTitle=new ArrayList<Component>();withTitle.add(Component.literal("Enchanted Book"));withTitle.addAll(original);var shifted=TooltipTranslation.components(withTitle,true);check(shifted.getFirst()==withTitle.getFirst(),"Shift title identity");check(joined(shifted.subList(1,shifted.size())).equals(target),"Shift effect");
            check(joined(original).equals(String.join("\n",effect.stream().map(TestEnchantmentBatch54::clean).toList())),"source immutable");
            var msg=new GuiMessage(0,original.getFirst(),null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"whole player chat");var signed=new GuiMessage(0,original.getFirst(),new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed chat");
            if(original.size()>1){var broken=new ArrayList<Component>(original);broken.set(broken.size()-1,Component.literal(effect.getLast()+" UNKNOWN"));check(joined(TooltipTranslation.components(broken,false)).equals(joined(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbour declines");}
        }
        if(full!=null){var rows=new ArrayList<Component>();rows.add(Component.literal("Enchanted Book"));for(String s:full)rows.add(row(s,0,7));var result=TooltipTranslation.components(rows,false);var expectedRows=new ArrayList<String>();expectedRows.add("Зачарованная книга");String[] targetRows=target.split("\n");for(int i=0;i<full.size();i++)expectedRows.add(i>=3&&i<3+effect.size()?targetRows[i-3]:collectionFooter(full.get(i)));check(joined(result).equals(String.join("\n",expectedRows)),"all full card rows "+id+" => "+joined(result));check(numericStyles(rows).equals(numericStyles(result)),"full card numeric glyphs");}
        for(String key:List.of(clean(effect.getFirst()),family.equals("CRITICAL")?"Crit Damage":family.equals("CUBISM")?(effect.size()==3?"Magma Cubes":"Cubic"):family.equals("ENDER_SLAYER")?"Ender":"health that your target has")){var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));var original=effect.stream().map(s->row(s,2,9)).toList();check(joined(TooltipTranslation.components(original,false)).equals(joined(original.stream().map(SkyRuClient::component).toList())),"override group declines "+id+" / "+key);flag("dictionary",db);}
    }
    private static String collectionFooter(String raw){
        // Frozen source lists are historical; keep their facts rather than insert current sources.
        return footer(raw).replace("Diamond Collection","Коллекция: Алмаз")
            .replace("Ender Pearl Collection","Коллекция: Эндер-жемчуг")
            .replace("Ghast Tear Collection","Коллекция: Слеза гаста")
            .replace("Pumpkin Collection","Коллекция: Тыква").replace("Wolf Slayer","Охота на волков");
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);int primary=0,older=0;
        var sourceDir=Path.of(args[1]);var primaryRecords=JsonParser.parseString(Files.readString(sourceDir.resolve("ENCHANTMENT_PRIMARY_0.15.54.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var r:primaryRecords){var o=r.getAsJsonObject();test(o.get("id").getAsString(),o.getAsJsonArray("effect_legacy").asList().stream().map(JsonElement::getAsString).toList(),o.getAsJsonArray("full_lore_legacy").asList().stream().map(JsonElement::getAsString).toList(),root,db);primary++;}
        var frozen=JsonParser.parseString(Files.readString(sourceDir.resolve("item-context.json"))).getAsJsonArray();for(var r:frozen){var o=r.getAsJsonObject();String id=o.get("id").getAsString();if(!FAMILIES.contains(id.split(";")[0])||!id.contains(";"))continue;var lore=o.getAsJsonArray("lore").asList().stream().map(JsonElement::getAsString).toList();int start=3,end=start;while(end<lore.size()&&!lore.get(end).isEmpty())end++;test(id,lore.subList(start,end),lore,root,db);older++;}
        test("CRITICAL;7",List.of("§7Increases §9☠ Crit Damage §7by","§a100%§7."),null,root,db);test("CRITICAL;7",List.of("§7Increases §9 Crit Damage §7by","§a100%§7."),null,root,db);
        check(primary==27&&older==27,"27 primary and27 frozen cards");flag("enabled",false);var original=List.<Component>of(Component.literal("Increases damage dealt to  Cubic"));check(TooltipTranslation.components(original,false)==original,"disabled identity");
        System.out.println("PASS: enchantment batch54 "+checks+" assertions;27 pinned primary/27 frozen full cards,4 families,4 styles/5 fresh actions/5 repeats +critical alternate wrap;not all enchants/live server");
    }
}
