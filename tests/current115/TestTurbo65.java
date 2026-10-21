import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Independent full-card Russian expectations for the pinned 91 books and two gourds. */
public final class TestTurbo65 {
    private static int checks;
    private static Method fixture,reviewed;
    private static final Pattern NUM=Pattern.compile("[0-9]+(?:[.,][0-9]+)*|(?<![\\p{L}])(?:VII|VI|IV|III|II|V|I)(?![\\p{L}])");
    private static final String[] ROMAN={"I","II","III","IV","V","VI","VII"};
    // Authored expectations, not read from the runtime's template registry.
    private static final Map<String,String[]> CROPS=Map.ofEntries(
        Map.entry("TURBO_CACTUS",new String[]{"Турбокактус","кактусов","Cactus"}),
        Map.entry("TURBO_CANE",new String[]{"Турботростник","сахарного тростника","Sugar Cane"}),
        Map.entry("TURBO_CARROT",new String[]{"Турбоморковь","моркови","Carrot"}),
        Map.entry("TURBO_COCO",new String[]{"Турбокакао","какао-бобов","Cocoa Beans"}),
        Map.entry("TURBO_MELON",new String[]{"Турбоарбуз","ломтиков арбуза","Melon Slice"}),
        Map.entry("TURBO_MOONFLOWER",new String[]{"Турболунный цветок","лунных цветов","Moonflower"}),
        Map.entry("TURBO_MUSHROOMS",new String[]{"Турбогрибы","грибов","Mushroom"}),
        Map.entry("TURBO_POTATO",new String[]{"Турбокартофель","картофеля","Potato"}),
        Map.entry("TURBO_PUMPKIN",new String[]{"Турботыква","тыкв","Pumpkin"}),
        Map.entry("TURBO_ROSE",new String[]{"Турбороза","диких роз","Wild Rose"}),
        Map.entry("TURBO_SUNFLOWER",new String[]{"Турбоподсолнух","подсолнухов","Sunflower"}),
        Map.entry("TURBO_WARTS",new String[]{"Турбонарост","адского нароста","Nether Wart"}),
        Map.entry("TURBO_WHEAT",new String[]{"Турбопшеница","пшеницы","Wheat"}));
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String text(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,style,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static String text(Component c){return text(c.getVisualOrderText());}
    private static String joined(List<Component> rows){return String.join("\n",rows.stream().map(TestTurbo65::text).toList());}
    private static List<String> strings(JsonArray a){return a.asList().stream().map(JsonElement::getAsString).toList();}
    private static Component row(String s,int mode,int seed)throws Exception{return (Component)fixture.invoke(null,s,mode,seed);}
    private static List<Style> styles(FormattedCharSequence s){var out=new ArrayList<Style>();s.accept((i,style,cp)->{for(int n=0;n<Character.charCount(cp);n++)out.add(style);return true;});return out;}
    private static List<String> values(String s){var out=new ArrayList<String>();var m=NUM.matcher(s);while(m.find())out.add(m.group());return out;}
    private static List<Style> valueStyles(List<Component> rows){var out=new ArrayList<Style>();for(var c:rows){var all=styles(c.getVisualOrderText());var m=NUM.matcher(text(c));while(m.find())out.addAll(all.subList(m.start(),m.end()));}return out;}
    private static void anchor(List<Component> a,int ar,String from,List<Component> b,int br,String to){int x=text(a.get(ar)).indexOf(from),y=text(b.get(br)).indexOf(to);check(x>=0&&y>=0,"anchor "+from+" -> "+to);check(styles(a.get(ar).getVisualOrderText()).get(x).equals(styles(b.get(br).getVisualOrderText()).get(y)),"current semantic style/action "+from);}
    private static List<String> expected(JsonObject card){
        String id=card.get("id").getAsString();var lore=strings(card.getAsJsonArray("lore_legacy"));
        if(!id.contains(";")){String old=id.equals("TURBO_GOURD")?"V":"VI",next=old.equals("V")?"VI":"VII";var out=new ArrayList<>(List.of(id.equals("TURBO_GOURD")?"Турботыква":"Зачарованная турботыква","Можно объединить на наковальне","","На фермерском инструменте повышает","все зачарования «Турбоурожай» "+old,"до «Турбоурожай» "+next+".","Нужно зачарование «Турбоурожай» "+old,"на предмете (хотя бы одно).",""));if(id.equals("TURBO_GOURD")){out.add("Нажмите ПКМ, чтобы посмотреть рецепты!");out.add("");}out.add(id.equals("TURBO_GOURD")?"LEGENDARY":"MYTHIC");return out;}
        String family=id.split(";")[0];int level=Integer.parseInt(id.split(";")[1]);var crop=CROPS.get(family);String number=List.of("5","10","15","20","25","30","35").get(level-1);check(clean(lore.get(3)).equals("Grants +"+number+"☘ "+crop[2]+" Fortune."),"source value/stat reviewed "+id);
        var out=new ArrayList<String>();out.add("Зачарованная книга");
        for(int i=0;i<lore.size();i++){String s=clean(lore.get(i));if(i==2){out.add(crop[0]+" "+ROMAN[level-1]);continue;}if(i==3){out.add("Даёт +"+number+"☘ удачи сбора "+crop[1]+".");continue;}if(s.startsWith("Requires ")){check(level==4||level==5,"no invented requirement level");String contest=(family.equals("TURBO_WARTS")?"Nether Warts":crop[2])+" Contest";check(s.equals("Requires "+(level==4?"Bronze":"Silver")+" in a "+contest+"!"),"current exact medal condition "+id);out.add("Требуется "+(level==4?"бронза":"серебро")+" в конкурсе "+crop[1]+"!");continue;}
            out.add(switch(s){case "Combinable in Anvil"->"Можно объединить на наковальне";case "Sources:"->"Источники:";case "Applied To:"->"Применяется к:";case " - Jacob's Farming Contest (I)"->" - Фермерский конкурс Джейкоба (I)";case " - Bazaar (I-V)"->" - Базар (I-V)";case " - Turbo Gourd (VI)"->" - Турботыква (VI)";case " - Enchanted Turbo Gourd (VII)"->" - Зачарованная турботыква (VII)";case " - Farming Tool"->" - Фермерский инструмент";default->{check(s.isEmpty()||Set.of("COMMON","UNCOMMON","RARE","EPIC","LEGENDARY","MYTHIC").contains(s),"unreviewed footer "+id+" "+s);yield s;}});
        }return out;
    }
    private static void test(JsonObject card,Path root,OfflineDictionary db)throws Exception{
        String id=card.get("id").getAsString();var source=new ArrayList<String>();source.add(card.get("name_legacy").getAsString());source.addAll(strings(card.getAsJsonArray("lore_legacy")));String target=String.join("\n",expected(card));
        check(values(String.join("\n",source.stream().map(TestTurbo65::clean).toList())).equals(values(target)),"authored decimal and Roman values "+id);
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            var input=new ArrayList<Component>();for(String s:source)input.add(row(s,mode,seed));var result=TooltipTranslation.components(input,false);var sequence=TooltipTranslation.styled(input.stream().map(Component::getVisualOrderText).toList(),false);
            for(int repeat=0;repeat<5;repeat++){
                check(joined(result).equals(target),id+" mode"+mode+" => "+joined(result));check(String.join("\n",sequence.stream().map(TestTurbo65::text).toList()).equals(target),"sequence whole card "+id);check(values(joined(input)).equals(values(joined(result))),"all source numbers/ranks "+id);check(valueStyles(input).equals(valueStyles(result)),"all numeric/Roman glyph actions "+id);
                for(int r=0;r<result.size();r++)check(styles(result.get(r).getVisualOrderText()).equals(styles(sequence.get(r))),"both current style paths "+id);
                result=TooltipTranslation.components(result,false);sequence=TooltipTranslation.styled(sequence,false);
            }
            check(joined(input).equals(String.join("\n",source.stream().map(TestTurbo65::clean).toList())),"immutable source "+id);
            check(TooltipTranslation.components(input,true).getFirst()==input.getFirst(),"Shift original title "+id);
            if(id.contains(";")){String family=id.split(";")[0];var crop=CROPS.get(family);anchor(input,4,crop[2]+" Fortune",result,4,"удачи сбора "+crop[1]);if(id.endsWith(";4")||id.endsWith(";5")){anchor(input,5,id.endsWith(";4")?"Bronze":"Silver",result,5,id.endsWith(";4")?"бронза":"серебро");anchor(input,5,(family.equals("TURBO_WARTS")?"Nether Warts":crop[2])+" Contest",result,5,"конкурсе "+crop[1]);}}
            else{anchor(input,4,"Turbo-Crop",result,4,"«Турбоурожай»");anchor(input,5,"Turbo-Crop",result,5,"«Турбоурожай»");}
        }
        if(!id.contains(";"))for(int seed=0;seed<5;seed++){
            var input=new ArrayList<Component>();for(String raw:source){var original=row(raw,2,seed);String s=text(original);var current=styles(original.getVisualOrderText());var numeric=new BitSet(s.length());var m=NUM.matcher(s);while(m.find())numeric.set(m.start(),m.end());var fragment=Component.empty();for(int at=0;at<s.length();){int end=at+Character.charCount(s.codePointAt(at));var style=current.get(at);if(numeric.get(at))style=style.withColor(0x110000+seed*0x1000+at*0x101).withInsertion("current Roman "+seed+"/"+at).withClickEvent(new ClickEvent.RunCommand("/roman "+seed+" "+at));fragment.append(Component.literal(s.substring(at,end)).setStyle(style));at=end;}input.add(fragment);}
            var out=TooltipTranslation.components(input,false);var seq=TooltipTranslation.styled(input.stream().map(Component::getVisualOrderText).toList(),false);check(joined(out).equals(target),"Roman gradient full upgrade");check(valueStyles(input).equals(valueStyles(out)),"fresh individual Roman colors/actions copied");for(int r=0;r<out.size();r++)check(styles(out.get(r).getVisualOrderText()).equals(styles(seq.get(r))),"both paths Roman gradient");
        }
        if(id.endsWith(";1"))for(String key:List.of(clean(source.get(4)),CROPS.get(id.split(";")[0])[2]+" Fortune")){
            var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМоя удача\n");flag("dictionary",new OfflineDictionary(dir));var input=row(source.get(4),2,17);var result=TooltipTranslation.components(List.of(input),false).getFirst();check(text(result).equals(text(SkyRuClient.component(input))),"user stat override not masked");flag("dictionary",db);
        }
    }
    private static Map<String,JsonObject> rows(Path path)throws Exception{var out=new HashMap<String,JsonObject>();for(var raw:JsonParser.parseString(Files.readString(path)).getAsJsonArray()){var row=raw.getAsJsonObject();check(out.put(row.get("id").getAsString(),row)==null,"unique ID");}return out;}
    private static void differential(Path project,Path current)throws Exception{
        var before=rows(project.resolve("build/catalogue-fixes64-419a81e71817498daf02854f93d77619/audit/items.json"));var after=rows(current.resolve("items.json"));check(before.keySet().equals(after.keySet())&&after.size()==8937,"complete pinned catalogue");var changed=new JsonArray();
        for(var e:after.entrySet()){String id=e.getKey();var a=before.get(id);var b=e.getValue();check(a.get("source_lore").equals(b.get("source_lore")),"same public source "+id);check(a.get("numeric_change_candidate").equals(b.get("numeric_change_candidate"))&&a.get("numeric_style_change_candidate").equals(b.get("numeric_style_change_candidate")),"no new values/style flags "+id);check(!b.get("new_effective_style_candidate").getAsBoolean()&&!b.get("lost_effective_style_candidate").getAsBoolean(),"no new/lost style sets "+id);
            boolean different=false;for(String f:List.of("translated_title","component_lore","sequence_lore"))different|=!a.get(f).equals(b.get(f));if(!different)continue;changed.add(id);check(CROPS.containsKey(id.split(";")[0])||id.equals("TURBO_GOURD")||id.equals("ENCHANTED_TURBO_GOURD"),"unexpected output change "+id);if(b.get("book").getAsBoolean())check(!b.get("effect_latin_word_candidate").getAsBoolean(),"all chosen core effects Russian "+id);
        }
        check(changed.size()==93,"exactly93 reviewed changed cards, got "+changed.size());Files.writeString(current.resolve("reviewed-changed-ids65.json"),new GsonBuilder().setPrettyPrinting().create().toJson(changed));
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),project=Path.of(args[1]).toAbsolutePath().getParent();fixture=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);fixture.setAccessible(true);
        var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var source=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("TURBO_PRIMARY_0.15.65.json"))).getAsJsonObject();check(source.get("source_commit").getAsString().equals("a225a02217776299ab3fb5cf5d9c0b155c4fc445"),"pinned current source");var counts=new HashMap<String,Integer>();int upgrades=0;
        for(var raw:source.getAsJsonArray("records")){var card=raw.getAsJsonObject();String id=card.get("id").getAsString();test(card,root,db);if(id.contains(";"))counts.merge(id.split(";")[0],1,Integer::sum);else upgrades++;}check(counts.keySet().equals(CROPS.keySet())&&counts.values().stream().allMatch(n->n==7)&&upgrades==2,"all13 families/all7 levels/both upgrades");
        var type=Class.forName("ru.wither.skyru.ReviewedLoreStyles");reviewed=type.getDeclaredMethod("reviewed",List.class,int.class);reviewed.setAccessible(true);
        for(String s:List.of("Grants +40☘ Cactus Fortune.","Grants +5☘ Unknown Fortune.","Requires Gold in a Cactus Contest!","Requires Silver in a Nether Wart Contest!","Turbo-Cacti VIII"))check(reviewed.invoke(null,List.of(s),0)==null,"closed finite rules refuse "+s);
        var badTransition=List.of("When applied to Farming Tool,","increases ALL Turbo-Crop V","enchantments to Turbo-Crop VII.");for(int at=0;at<badTransition.size();at++)check(reviewed.invoke(null,badTransition,at)==null,"unreviewed V-to-VII transition rejected");
        for(String key:List.of("Requires Bronze in a Cactus Contest!","Bronze","Cactus Contest")){
            var dir=root.resolve("medal-override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМоё условие\n");flag("dictionary",new OfflineDictionary(dir));var input=row("§7Requires §cBronze §7in a §aCactus Contest§7!",2,18);var expected=SkyRuClient.component(input);var actual=TooltipTranslation.components(List.of(input),false).getFirst();check(text(actual).equals(text(expected)),"medal rule respects override "+key);check(styles(actual.getVisualOrderText()).equals(styles(expected.getVisualOrderText())),"override keeps current actions");flag("dictionary",db);
        }
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,Component.literal("Requires Bronze in a Cactus Contest!"),null,channel,null);check(SkyRuClient.npcChat(msg)==msg,"ordinary chat untouched");}
        differential(project,Path.of(args[2]));flag("enabled",false);var original=List.<Component>of(Component.literal("Grants +5☘ Cactus Fortune."));check(TooltipTranslation.components(original,false)==original,"disabled identity");
        System.out.println("PASS: Turbo65 "+checks+" assertions;91 full books/13 families/I-VII plus2 upgrades,4 layouts/5 fresh styles/5 repeats;source decimal+Roman values/actions/conditions/Shift/overrides;8937-card differential/exact93 reviewed changes;not all enchants or live FPS");
    }
}
