import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Independent human expectations for65 current storage paragraphs, not all live menus. */
public final class TestSacks55{
    private static int checks;
    private static final Pattern NUMBER=Pattern.compile("[0-9]+(?:[.,][0-9]+)*");
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static String joined(List<Component> c){return String.join("\n",c.stream().map(TestSacks55::plain).toList());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static List<Style> numeric(List<Component> rows){var out=new ArrayList<Style>();for(var row:rows){var ss=styles(row);var m=NUMBER.matcher(plain(row));while(m.find())out.addAll(ss.subList(m.start(),m.end()));}return out;}
    private static Component row(String raw,int mode,int seed){
        var base=Style.EMPTY.withInsertion("frame "+seed).withClickEvent(new ClickEvent.RunCommand("/original-sack-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hint "+seed)));
        var original=Component.literal(raw).setStyle(base);if(mode==0)return original;if(mode==1)return Component.literal(clean(raw)).setStyle(base);
        String text=plain(original);var ss=styles(original);var out=Component.empty();for(int i=0;i<text.length();i++){var style=ss.get(i);if(mode==3&&Character.isDigit(text.charAt(i)))style=style.withColor(0x100000+i*0x101).withInsertion("digit "+seed+"/"+i);out.append(Component.literal(text.substring(i,i+1)).setStyle(style));}return out;
    }
    private static String expected(List<String> lines){
        String s=String.join("\n",lines);
        if(s.startsWith("Trophy ")){boolean fish=s.startsWith("Trophy Fish"),bronze=s.contains("BRONZE");return (fish?"Трофейные рыбы ":"Трофейные лягушки ")+(bronze?"бронзового":"серебряного")+"\nкачества при подборе сразу попадают в ваши мешки.";}
        if(s.startsWith("Holds Gemstones"))return "Хранит драгоценные камни всех типов и\nкачеств. Подобранные камни\nсразу попадают в ваши мешки.";
        if(s.startsWith("Holds most commonly dropped Runes"))return "Хранит распространённые руны\nвсех уровней. Подобранные руны\nсразу попадают в ваши мешки.";
        if(s.startsWith("Holds most commonly found Flowers"))return "Хранит распространённые цветы!\nПодобранные цветы сразу попадают\nв ваши мешки.";
        if(s.startsWith("Holds various Baits"))return "Хранит разные наживки для рыбалки! Подобранные предметы\nсразу попадают\nв ваши мешки.";
        String place=null;for(String[] p:new String[][]{{"Combat","в бою"},{"Farming","при фермерстве"},{"Fishing","при рыбалке"},{"Foraging","при лесорубстве"},{"Husbandry","при животноводстве"},{"Mining","при добыче"}})if(s.contains(p[0]))place=p[1];
        if(s.startsWith("Holds various enchanted items"))return "Хранит зачарованные ресурсы,\nполученные "+place+". Подобранные предметы\nсразу попадают в ваши мешки.";
        if(s.startsWith("Holds various items"))return place.equals("при лесорубстве")?"Хранит ресурсы, полученные при\nлесорубстве. Подобранные предметы\nсразу попадают в ваши мешки.":"Хранит ресурсы, полученные\n"+place+". Подобранные предметы\nсразу попадают в ваши мешки.";
        if(s.startsWith("Holds Lava Fishing"))return "Хранит ресурсы лавовой рыбалки,\nполученные на Багровом острове. Подобранные предметы\nсразу попадают в ваши мешки.";
        for(String[] p:new String[][]{{"Slayers","при охоте на боссов"},{"Spooky Festival","на Жутком фестивале"},{"various Events","на различных событиях"},{"Catacombs","в Катакомбах"},{"Crystal Hollows","в Кристальных пещерах"},{"Dwarven Mines","в Дварфских шахтах"},{"Garden","в Саду"},{"Jerry's Workshop","в Мастерской Джерри"},{"Crimson Isle","на Багровом острове"},{"End!","в Крае"},{"Witch!","в Год ведьмы"},{"Greenhouse","в теплице"}})if(s.contains(p[0]))place=p[1];
        if(place==null)throw new AssertionError("unreviewed source "+s);
        return (s.startsWith("Holds Mutations")?"Хранит мутации, найденные":"Хранит ресурсы, полученные")+"\n"+place+". Подобранные предметы\nсразу попадают в ваши мешки.";
    }
    private static String title(String source){
        for(String[] p:new String[][]{{"Crystal Hollows Sack","Мешок Кристальных пещер"},{"Dwarven Sack","Дварфский мешок"},{"Dungeon Sack","Мешок подземелий"},{"Events Sack","Мешок событий"},{"Flower Sack","Мешок цветов"},{"Garden Sack","Мешок Сада"},{"Rune Sack","Мешок рун"},{"Spooky Sack","Жуткий мешок"},{"Winter Sack","Зимний мешок"},{"Witch's Sack","Мешок ведьмы"},{"Bronze Trophy Fishing Sack","Мешок бронзовой трофейной рыбы"},{"Silver Trophy Fishing Sack","Мешок серебряной трофейной рыбы"},{"Bronze Trophy Frog Sack","Мешок бронзовых трофейных лягушек"},{"Silver Trophy Frog Sack","Мешок серебряных трофейных лягушек"}})if(source.equals(p[0]))return p[1];
        String size=source.startsWith("Beginner ")?"Начальный":source.startsWith("Small ")?"Малый":source.startsWith("Medium ")?"Средний":"Большой";String body=source.substring(source.indexOf(' ')+1).replace(" Sack","");
        var terms=Map.ofEntries(Map.entry("Agronomy","урожая"),Map.entry("Combat","боевых ресурсов"),Map.entry("Fishing","улова"),Map.entry("Foraging","ресурсов лесорубства"),Map.entry("Husbandry","продукции животноводства"),Map.entry("Mining","ресурсов добычи"),Map.entry("Bait","наживок"),Map.entry("Dragon","драконьих ресурсов"),Map.entry("Gemstone","драгоценных камней"),Map.entry("Lava Fishing","лавового улова"),Map.entry("Mutations","мутаций"),Map.entry("Nether","ресурсов Багрового острова"),Map.entry("Slayer","ресурсов боссов"),Map.entry("Enchanted Agronomy","зачарованного урожая"),Map.entry("Enchanted Combat","зачарованных боевых ресурсов"),Map.entry("Enchanted Fishing","зачарованного улова"),Map.entry("Enchanted Foraging","зачарованных ресурсов лесорубства"),Map.entry("Enchanted Husbandry","зачарованной продукции животноводства"),Map.entry("Enchanted Mining","зачарованных ресурсов добычи"));
        if(!terms.containsKey(body))throw new AssertionError("unreviewed name "+source);return size+" мешок для "+terms.get(body);
    }
    private static String footer(String s){s=clean(s);if(s.startsWith("Capacity: ")){var n=NUMBER.matcher(s);if(!n.find())throw new AssertionError(s);return "Вместимость: "+n.group()+" ед. "+(s.endsWith("gemstone type")?"каждого типа камней":s.endsWith("rune tier")?"каждого уровня рун":s.endsWith("per trophy")?"каждого вида трофеев":"каждого предмета");}
        return switch(s){case "Multiple sacks sum their capacity!"->"Вместимость нескольких мешков складывается!";case "Sacks sum their capacity."->"Вместимость мешков складывается.";case "Right Click to open!"->"Нажмите ПКМ, чтобы открыть!";case "Right Click to open sack!"->"Нажмите ПКМ, чтобы открыть мешок!";case "❣ Requires Novice Frog Collector."->"❣ Требуется звание «Начинающий собиратель лягушек».";case "❣ Requires Adept Frog Collector."->"❣ Требуется звание «Опытный собиратель лягушек».";case "❣ Requires Hunting Skill 15."->"❣ Требуется уровень навыка «Охота» 15.";case "* Soulbound *"->"* Привязан к профилю *";case "* Co-op Soulbound *"->"* Привязан к кооперативу *";default->s.replace(" SACK"," МЕШОК");};}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);int cards=0;
        var source=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SACK_PRIMARY_0.15.55.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var e:source){var r=e.getAsJsonObject();var lore=r.getAsJsonArray("lore_legacy").asList().stream().map(JsonElement::getAsString).toList();String first=clean(lore.getFirst());if(!(first.startsWith("Holds ")||first.startsWith("Trophy ")))continue;cards++;int end=0;while(end<lore.size()&&!clean(lore.get(end)).isBlank())end++;var paragraph=lore.subList(0,end);String target=expected(paragraph.stream().map(TestSacks55::clean).toList());
            for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){final int layout=mode,frame=seed;var original=paragraph.stream().map(s->row(s,layout,frame)).toList();var result=TooltipTranslation.components(original,false);var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);for(int n=0;n<5;n++){check(joined(result).equals(target),r.get("id")+" mode"+mode+" => "+joined(result));check(String.join("\n",seq.stream().map(AuditMiningStyles::plain).toList()).equals(target),"sequence");check(numeric(original).equals(numeric(result)),"numbers actions");result=TooltipTranslation.components(result,false);seq=TooltipTranslation.styled(seq,false);}check(joined(original).equals(String.join("\n",paragraph.stream().map(TestSacks55::clean).toList())),"source immutable");
                var shift=new ArrayList<Component>();shift.add(Component.literal(r.get("title_legacy").getAsString()));shift.addAll(original);var shifted=TooltipTranslation.components(shift,true);check(shifted.getFirst()==shift.getFirst()&&joined(shifted.subList(1,shifted.size())).equals(target),"Shift");var msg=new GuiMessage(0,original.getFirst(),null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"player");var signed=new GuiMessage(0,original.getFirst(),new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");
                var broken=new ArrayList<Component>(original);broken.set(broken.size()-1,Component.literal(plain(broken.getLast())+" UNKNOWN"));check(joined(TooltipTranslation.components(broken,false)).equals(joined(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbour");
                var whole=new ArrayList<Component>();whole.add(row(r.get("title_legacy").getAsString(),layout,frame));for(var s:lore)whole.add(row(s,layout,frame));var actual=TooltipTranslation.components(whole,false);var expectedRows=new ArrayList<String>();expectedRows.add(title(clean(r.get("title_legacy").getAsString())));var tr=target.split("\n");for(int i=0;i<lore.size();i++)expectedRows.add(i<end?tr[i]:footer(lore.get(i)));check(joined(actual).equals(String.join("\n",expectedRows)),"whole card "+r.get("id")+" => "+joined(actual));check(numeric(whole).equals(numeric(actual)),"whole card values");
            }
            var dir=root.resolve("override-"+cards);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),clean(paragraph.getFirst())+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));var original=paragraph.stream().map(s->row(s,2,8)).toList();check(joined(TooltipTranslation.components(original,false)).equals(joined(original.stream().map(SkyRuClient::component).toList())),"override");flag("dictionary",db);
        }
        check(cards==65,"65 repository storage cards; extra large currently no description");
        var mixed=List.of(row("§7Хранит различные предметы, полученные из",0,7),row("§cБой§7! Items you pickup go directly",0,7),row("§7в ваши мешки.",0,7));check(joined(TooltipTranslation.components(mixed,false)).equals("Хранит ресурсы, полученные\nв бою. Подобранные предметы\nсразу попадают в ваши мешки."),"exact partial screenshot");
        for(String date:List.of("ReceivedOctober 13, 2026","Received October 13, 2026","ПолученоOctober 13, 2026")){var c=row(date,2,7);var out=SkyRuClient.component(c);check(plain(out).equals("Получено: 13 октября 2026"),"human received date");check(numeric(List.of(c)).equals(numeric(List.of(out))),"original day/year actions");}
        for(int mode=0;mode<4;mode++){var capacity=row("§7Вместимость: §eПо 2,240 каждого предмета",mode,7);var out=SkyRuClient.component(capacity);check(plain(out).equals("Вместимость: 2,240 ед. каждого предмета"),"partial capacity");check(numeric(List.of(capacity)).equals(numeric(List.of(out))),"partial capacity numeric actions");}
        var combat=mixed.get(1);var translated=TooltipTranslation.components(mixed,false).get(1);check(styles(combat).get(plain(combat).indexOf("Бой")).equals(styles(translated).get(plain(translated).indexOf("в бою"))),"combat semantic style/action");
        flag("enabled",false);var original=List.<Component>of(Component.literal("Holds various items obtained from"));check(TooltipTranslation.components(original,false)==original,"disabled");
        System.out.println("PASS: sacks55 "+checks+" assertions;65 current full cards/4 styles/5 fresh actions/5 repeats, exact partial screenshot, dates/values/actions/Shift/player/overrides; not all live Sack GUI or upgrades");
    }
}
