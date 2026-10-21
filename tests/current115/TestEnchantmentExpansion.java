import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Reviewed batches48/49, not all live enchants. Four synthetic style layouts. */
public class TestEnchantmentExpansion {
    private static int checks;private static Path root;
    private static final Map<String,Set<String>> PHASES=Map.of(
        "armor",Set.of("FEATHER_FALLING","BLAST_PROTECTION","FIRE_PROTECTION","PROJECTILE_PROTECTION"),
        "vitality",Set.of("STRONG_MANA","HARDENED_MANA","FEROCIOUS_MANA"),
        "fishing",Set.of("SPIKED_HOOK","LUCK_OF_THE_SEA","CASTER"),
        "healing",Set.of("VAMPIRISM","SYPHON","LIFE_STEAL"),
        "pests",Set.of("BUG_BLENDER"),"champion",Set.of("CHAMPION"),
        "smite",Set.of("SMITE"),"arthropods",Set.of("BANE_OF_ARTHROPODS"),"lethality",Set.of("LETHALITY"));
    private static final Pattern NUMBER=Pattern.compile("[0-9]+(?:[.,][0-9]+)?");
    private static final Pattern VALUE=Pattern.compile("[+]?[0-9]+(?:[.,][0-9]+)?%?");
    private static final List<String> TERMS=List.of("True Defense","Attack Speed","Treasure Chance","Farming Fortune","Combat XP","Vitality","Strength","Defense","Защита","Cooldown","Arthropod","Skeletal","Wither","Undead","Pests");
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/body48-original"));
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withBold(true).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE_STYLE=Style.EMPTY.withColor(0x55ff55).withInsertion("original value");
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void eq(String a,String b){check(a.equals(b),a+" != "+b);}
    private static List<String> numbers(String s){var m=NUMBER.matcher(s);var out=new ArrayList<String>();while(m.find())out.add(m.group());return out;}
    private static Component row(String text,int mode,int seed){
        var out=Component.empty();int at=0;
        while(at<text.length()){
            String term=null;if(mode>=2)for(String t:TERMS)if(text.startsWith(t,at)){term=t;break;}
            if(term!=null){out.append(Component.literal(term).setStyle(TERM));at+=term.length();continue;}
            var value=VALUE.matcher(text);value.region(at,text.length());
            if(mode>0&&value.lookingAt()){
                String token=value.group();if(mode==3)for(int i=0;i<token.length();i++)out.append(Component.literal(token.substring(i,i+1)).setStyle(VALUE_STYLE.withColor(0x110000+seed*0x100+i*0x101).withInsertion("digit "+seed+"/"+i)));
                else out.append(Component.literal(token).setStyle(VALUE_STYLE));at=value.end();continue;
            }
            int point=text.codePointAt(at);String c=new String(Character.toChars(point));
            out.append(Component.literal(c).setStyle(mode>=2&&"❤❈☠༕🦴Ж".contains(c)?TERM:BODY));at+=Character.charCount(point);
        }return out;
    }
    private static List<Style> styles(Component c){var list=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)list.add(s);return true;});return list;}
    private static Component legacy(String raw){var out=Component.empty();StringDecomposer.iterateFormatted(Component.literal(raw).setStyle(BODY),Style.EMPTY,(i,s,p)->{out.append(Component.literal(new String(Character.toChars(p))).setStyle(s));return true;});return out;}
    private static void testLegacy(String family,String source,JsonObject captured){
        var original=new ArrayList<Component>();for(var line:captured.getAsJsonArray("legacy_effect"))original.add(legacy(line.getAsString()));
        if(family.equals("LETHALITY")){eq(joined(original).replace("","❈").replace("\n"," "),source.replace("\n"," "));source=joined(original);}else eq(joined(original),source);
        String target=expected(family,source);var result=TooltipTranslation.components(original,false);var sequence=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
        for(int repeat=0;repeat<5;repeat++){
            eq(joined(result),target);eq(String.join("\n",sequence.stream().map(AuditMiningStyles::plain).toList()),target);
            check(numericStyles(result).equals(numericStyles(original)),"actual legacy numeric colours/actions");eq(joined(original),source);
            result=TooltipTranslation.components(result,false);sequence=TooltipTranslation.styled(sequence,false);
        }
    }
    private static String joined(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String expected(String id,String source){
        var n=numbers(source);String x=n.getFirst(),icon=source.contains("")?"":"❈";return switch(id){
            case "FEATHER_FALLING"->"Увеличивает высоту безопасного падения\nдо получения урона на\n"+x+" и уменьшает урон от падения"+(source.split("\n")[2].endsWith(" by")?" на\n":"\nна ")+n.get(1)+"%.";
            case "BLAST_PROTECTION"->"Даёт +"+x+" ❈ защиты\nот взрывов.";
            case "PROJECTILE_PROTECTION"->"Даёт +"+x+" ❈ защиты\nот снарядов.";
            case "FIRE_PROTECTION"->"Даёт +"+x+" истинной защиты от\nогня и лавы.";
            case "STRONG_MANA"->"Преобразует "+x+"% израсходованной  жизненной силы в \nсилу на "+n.get(1)+" с. (максимум "+n.get(2)+"\nсилы).";
            case "HARDENED_MANA"->"Преобразует "+x+"% израсходованной  жизненной силы в \nзащиту на "+n.get(1)+" с. (максимум "+n.get(2)+" защиты).";
            case "FEROCIOUS_MANA"->"Преобразует "+x+"% израсходованной  жизненной силы в \nскорость атаки на "+n.get(1)+" с. (максимум "+n.get(2)+"\nскорости атаки).";
            case "SPIKED_HOOK"->"Удочка наносит на "+x+"% больше урона\nсуществам.";
            case "LUCK_OF_THE_SEA"->"Даёт +"+x+" шанса выловить сокровище,\nповышая вероятность получить\nсокровище при рыбалке.";
            case "CASTER"->"С шансом "+x+"% не расходует\nнаживку.";
            case "VAMPIRISM"->"Восстанавливает "+x+"❤ здоровья при убийстве существа.\n(Перезарядка: "+n.get(1)+" с.)";
            case "SYPHON"->"Восстанавливает +"+x+" жизненной силы при каждом\nпопадании по существу.\n(Перезарядка: "+n.get(1)+" с.)";
            case "LIFE_STEAL"->"Восстанавливает "+x+"❤ здоровья при попадании по существу.";
            case "BUG_BLENDER"->"Даёт +"+x+" удачи фермерства при\nсборе  вредителей пылесосом.";
            case "CHAMPION"->"Даёт на "+x+"% больше опыта боя.\n"+n.get(1)+"-й удар по существу даёт +"+n.get(2)+"\nмонеты и +"+n.get(3)+" сфер опыта.";
            case "SMITE"->"Увеличивает урон по ☠ иссушителям,\n༕ нежити и 🦴 скелетным существам на\n"+x+"%.";
            case "BANE_OF_ARTHROPODS"->"Увеличивает урон по Ж\nчленистоногим существам на "+x+"%.";
            case "LETHALITY"->source.split("\n")[0].endsWith(" your")
                ?"Снижает "+icon+" защиту цели\nна "+x+"% на "+n.get(1)+" с при каждом\nударе ближнего боя. Складывается до "+n.get(2)+"\nраз."
                :"Снижает "+icon+" защиту\nцели на "+x+"% на "+n.get(1)+" с\nпри каждом ударе\nближнего боя. Складывается до "+n.get(2)+" раз.";
            default->throw new AssertionError(id);
        };
    }
    private static List<Style> numericStyles(List<Component> rows){var out=new ArrayList<Style>();for(var c:rows){var m=NUMBER.matcher(c.getString());var ss=styles(c);while(m.find())out.addAll(ss.subList(m.start(),m.end()));}return out;}
    private static List<String> semanticTargets(String family){return switch(family){
        case "BLAST_PROTECTION","PROJECTILE_PROTECTION"->List.of("защиты");
        case "FIRE_PROTECTION"->List.of("истинной защиты");
        case "STRONG_MANA"->List.of("жизненной силы","силу","силы");
        case "HARDENED_MANA"->List.of("жизненной силы","защиту","защиты");
        case "FEROCIOUS_MANA"->List.of("жизненной силы","скорость атаки","скорости атаки");
        case "LUCK_OF_THE_SEA"->List.of("шанса выловить сокровище");
        case "VAMPIRISM"->List.of("Перезарядка");
        case "SYPHON"->List.of("жизненной силы","Перезарядка");
        case "BUG_BLENDER"->List.of("удачи фермерства","вредителей");
        case "CHAMPION"->List.of("опыта боя");
        case "SMITE"->List.of("иссушителям","нежити","скелетным");
        case "BANE_OF_ARTHROPODS"->List.of("членистоногим");
        case "LETHALITY"->List.of("защиту");
        default->List.of();};}
    private static void terms(List<Component> rows,String family){
        for(var c:rows){var ss=styles(c);String text=c.getString();
            for(String term:semanticTargets(family))for(int at=text.indexOf(term);at>=0;at=text.indexOf(term,at+term.length()))
                for(int i=at;i<at+term.length();i++)check(ss.get(i).equals(TERM),"semantic term style/action: "+term);
            for(int i=0;i<text.length();i++)if("❤❈☠༕🦴Ж".indexOf(text.charAt(i))>=0)check(ss.get(i).equals(TERM),"original stat glyph style/action");
        }
    }
    private static void test(String family,String source,OfflineDictionary db){
        String target=expected(family,source);eq(String.join("/",numbers(source)),String.join("/",numbers(target)));
        for(int mode=0;mode<4;mode++){
            var original=new ArrayList<Component>();for(String line:source.split("\n"))original.add(row(line,mode,checks%31));
            var result=TooltipTranslation.components(original,false);var styled=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
            for(int pass=0;pass<5;pass++){
                eq(joined(result),target);eq(String.join("\n",styled.stream().map(AuditMiningStyles::plain).toList()),target);check(numericStyles(result).equals(numericStyles(original)),"numeric actions/gradients");
                eq(db.translateForSearch(target),target);if(mode>=2)terms(result,family);result=TooltipTranslation.components(result,false);styled=TooltipTranslation.styled(styled,false);
            }
            eq(joined(original),source);var withTitle=new ArrayList<Component>();withTitle.add(Component.literal("Enchanted Book"));withTitle.addAll(original);var shift=TooltipTranslation.components(withTitle,true);eq(shift.getFirst().getString(),"Enchanted Book");eq(joined(shift.subList(1,shift.size())),target);
            for(var ch:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,original.getFirst(),null,ch,null);check(SkyRuClient.npcChat(msg)==msg,"ordinary input/chat not translated");}
            var broken=new ArrayList<Component>(original);broken.set(broken.size()-1,row(source.split("\n")[original.size()-1]+" UNKNOWN",mode,1));
            eq(joined(TooltipTranslation.components(broken,false)),joined(broken.stream().map(SkyRuClient::component).toList()));
        }
        var overrideKeys=new ArrayList<>(List.of(source.split("\n")[0],"{0}",numbers(source).getFirst()));
        if(family.equals("LETHALITY")){overrideKeys.add("Defense");overrideKeys.add(source.split("\n")[0].replace("Защита","Defense"));}
        for(String key:overrideKeys){
            Path custom;
            try{custom=root.resolve("override-"+checks);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(custom));}catch(Exception failure){throw new RuntimeException(failure);}
            var original=Arrays.stream(source.split("\n")).map(s->row(s,2,1)).toList();
            eq(joined(TooltipTranslation.components(original,false)),joined(original.stream().map(SkyRuClient::component).toList()));
        }
        try{flag("dictionary",db);}catch(Exception failure){throw new RuntimeException(failure);}
    }
    public static void main(String[] args)throws Exception{
        root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        String phase=args.length>2?args[2]:"all";Set<String> families=phase.equals("all")?PHASES.values().stream().flatMap(Set::stream).collect(java.util.stream.Collectors.toSet()):PHASES.get(phase);
        var items=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();int cards=0;
        var legacyFile=Path.of(args[1]).resolve("ENCHANTMENT_LEGACY_0.15.49.json");var captured=new HashMap<String,JsonObject>();
        if(!Files.exists(legacyFile))throw new AssertionError("Missing frozen49 legacy corpus");
        for(var record:JsonParser.parseString(Files.readString(legacyFile)).getAsJsonObject().getAsJsonArray("records")){var r=record.getAsJsonObject();captured.put(r.get("id").getAsString(),r);}
        check(captured.size()==26,"frozen legacy card count");
        for(var raw:items){var item=raw.getAsJsonObject();String id=item.get("id").getAsString();String family=id.split(";")[0];if(!id.contains(";")||!families.contains(family)||!item.get("name").getAsString().equals("Enchanted Book"))continue;
            var body=new ArrayList<String>();var lore=item.getAsJsonArray("lore");for(int i=3;i<lore.size()&&!lore.get(i).getAsString().isBlank();i++)body.add(lore.get(i).getAsString());String source=String.join("\n",body);test(family,source,db);if(captured.containsKey(id))testLegacy(family,source,captured.get(id));
            if(family.equals("LETHALITY")){var n=numbers(source);String wide="Reduces the ❈ Defense of your\ntarget by "+n.get(0)+"% for "+n.get(1)+"s each time you\nhit them with melee. Stacks up to "+n.get(2)+"\ntimes.";
                for(String glyph:List.of("❈","")){if(!glyph.equals("❈"))test(family,source.replace("❈",glyph),db);test(family,wide.replace("❈",glyph),db);test(family,source.replace("Defense","Защита").replace("❈",glyph),db);test(family,wide.replace("Defense","Защита").replace("❈",glyph),db);}}
            cards++;
        }
        int wanted=switch(phase){case "armor"->31;case "vitality"->30;case "fishing"->20;case "healing"->16;case "pests"->5;case "champion"->1;case "smite","arthropods"->7;case "lethality"->6;default->123;};check(cards==wanted,"frozen count "+cards);
        flag("enabled",false);var source=List.<Component>of(Component.literal("Heal 2.4❤ every time you hit a mob."));check(TooltipTranslation.components(source,false)==source,"disabled identity");flag("enabled",true);
        System.out.println("PASS: enchantment expansion "+phase+" /"+checks+" assertions;"+cards+" cards/"+families.size()+" families, four styles/five repeats, original quantities/actions/Shift/neighbours/chat; not all server");
    }
}
