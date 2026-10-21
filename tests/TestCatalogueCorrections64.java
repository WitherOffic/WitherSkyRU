import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.net.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Authored expectations, current captured styles, and full pinned-catalogue differential. */
public final class TestCatalogueCorrections64 {
    private static int checks;
    private static final Set<String> FAMILIES=Set.of("LIFE_STEAL","ULTIMATE_WISE","FORTUNE","HARVESTING","SUNDER","ANGLER","DELICATE","KNOCKBACK","PUNCH");
    private static final Pattern NUMBER=Pattern.compile("[0-9]+(?:[.,][0-9]+)*");
    private static Method fixture,oldComponents;
    private static final class Previous extends URLClassLoader {
        Previous(URL url){super(new URL[]{url},TestCatalogueCorrections64.class.getClassLoader());}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(!name.startsWith("ru.wither.skyru."))return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
        }
        public URL getResource(String name){if(name.startsWith("data/witherskyru/")){var r=findResource(name);if(r!=null)return r;}return super.getResource(name);}
    }
    private static void oldFlag(Class<?> type,String name,Object value)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String text(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,style,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static String text(Component c){return text(c.getVisualOrderText());}
    private static String joined(List<Component> rows){return String.join("\n",rows.stream().map(TestCatalogueCorrections64::text).toList());}
    private static List<Style> styles(FormattedCharSequence s){var out=new ArrayList<Style>();s.accept((i,style,cp)->{for(int n=0;n<Character.charCount(cp);n++)out.add(style);return true;});return out;}
    private static List<String> numbers(String s){var out=new ArrayList<String>();var m=NUMBER.matcher(s);while(m.find())out.add(m.group());return out;}
    private static List<Style> numeric(List<Component> rows){var out=new ArrayList<Style>();for(var c:rows){String s=text(c);var ss=styles(c.getVisualOrderText());var m=NUMBER.matcher(s);while(m.find())out.addAll(ss.subList(m.start(),m.end()));}return out;}
    private static List<String> strings(JsonArray a){return a.asList().stream().map(JsonElement::getAsString).toList();}
    private static Component row(String s,int mode,int seed)throws Exception{return (Component)fixture.invoke(null,s,mode,seed);}
    private static String expected(String family,List<String> source){var n=numbers(String.join("\n",source.stream().map(TestCatalogueCorrections64::clean).toList()));return switch(family){
        case "LIFE_STEAL"->"Восстанавливает "+n.getFirst()+"❤ здоровья при попадании по существу.";
        case "ULTIMATE_WISE"->"Снижает расход маны на способности\nэтого предмета на "+n.getFirst()+"%.";
        case "FORTUNE"->"Даёт +"+n.getFirst()+" ☘ удачи\nдобычи, повышая шанс\nполучить больше ресурсов за раз.";
        case "HARVESTING","SUNDER"->"Даёт +"+n.getFirst()+" ☘ удачи\nфермерства, повышая шанс\nполучить больше урожая за раз.";
        case "ANGLER"->"Даёт +"+n.getFirst()+" к шансу выловить\nморское существо.";
        case "DELICATE"->"Не позволяет ломать стебли и молодые\nкультуры.";
        case "KNOCKBACK"->"Увеличивает отбрасывание на "+n.getFirst()+"\n"+(n.getFirst().equals("3")?"блока.":"блоков.");
        default->"Увеличивает отбрасывание стрелами на\n"+n.getFirst()+" "+(n.getFirst().equals("3")?"блока.":"блоков.");};}
    private static void anchor(List<Component> a,int ar,String before,List<Component> b,int br,String after){
        int x=text(a.get(ar)).indexOf(before),y=text(b.get(br)).indexOf(after);check(x>=0&&y>=0,"semantic anchor "+before+" -> "+after);
        check(styles(a.get(ar).getVisualOrderText()).get(x).equals(styles(b.get(br).getVisualOrderText()).get(y)),"semantic style/action "+before);
    }
    private static void effect(JsonObject card,Path root,OfflineDictionary db)throws Exception{
        String id=card.get("id").getAsString(),family=id.split(";")[0];var lore=strings(card.getAsJsonArray("lore_legacy"));int end=3;while(end<lore.size()&&!lore.get(end).isEmpty())end++;
        var source=lore.subList(3,end);String target=expected(family,source);
        check(numbers(String.join("\n",source.stream().map(TestCatalogueCorrections64::clean).toList())).equals(numbers(target)),"authored numbers "+id);
        for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            var input=new ArrayList<Component>();for(String s:source)input.add(row((seed&1)==1?s.replace("§6Fortune§7, ","§6Fortune, §7"):s,mode,seed));var result=TooltipTranslation.components(input,false);var sequences=TooltipTranslation.styled(input.stream().map(Component::getVisualOrderText).toList(),false);
            for(int repeat=0;repeat<5;repeat++){
                check(joined(result).equals(target),id+" mode "+mode+" actual "+joined(result));
                check(String.join("\n",sequences.stream().map(TestCatalogueCorrections64::text).toList()).equals(target),"matching sequence "+id);
                check(numeric(input).equals(numeric(result)),"all numeric glyph colors/actions "+id);
                for(int r=0;r<result.size();r++)check(styles(result.get(r).getVisualOrderText()).equals(styles(sequences.get(r))),"both paths current effective styles "+id);
                result=TooltipTranslation.components(result,false);sequences=TooltipTranslation.styled(sequences,false);
            }
            if(family.equals("FORTUNE")||family.equals("HARVESTING")||family.equals("SUNDER")){anchor(input,0,family.equals("FORTUNE")?"Mining":"Farming",result,0,"удачи");anchor(input,1,"Fortune",result,1,family.equals("FORTUNE")?"добычи":"фермерства");}
            if(family.equals("ANGLER")){anchor(input,0,"Sea Creature",result,0,"к шансу выловить");anchor(input,1,"Chance",result,1,"морское существо");}
            var shifted=new ArrayList<Component>();shifted.add(row("Enchanted Book",mode,seed));shifted.addAll(input);check(TooltipTranslation.components(shifted,true).getFirst()==shifted.getFirst(),"Shift title identity");
            check(joined(input).equals(String.join("\n",source.stream().map(TestCatalogueCorrections64::clean).toList())),"source immutable");
            if(input.size()>1){var broken=new ArrayList<>(input);broken.set(broken.size()-1,Component.literal(text(broken.getLast())+" UNREVIEWED"));var old=(List<Component>)oldComponents.invoke(null,broken,false);var next=TooltipTranslation.components(broken,false);check(joined(next).equals(joined(old)),"unknown neighbours preserve delivered62 fallback "+id);for(int r=0;r<next.size();r++)check(styles(next.get(r).getVisualOrderText()).equals(styles(old.get(r).getVisualOrderText())),"unknown-neighbour style oracle "+id);}
            var whole=new ArrayList<Component>();whole.add(row(card.get("name_legacy").getAsString(),mode,seed));for(String s:lore)whole.add(row(s,mode,seed));var full=TooltipTranslation.components(whole,false);check(joined(full.subList(4,end+1)).equals(target),"complete book effect "+id);
            if(family.equals("ULTIMATE_WISE"))check(joined(full).contains(" - Жезл")&&!joined(full).contains("Wand"),"mixed Wand footer");
            if(family.equals("DELICATE"))check(joined(full).contains("▲ Деликатность нельзя объединять!"),"warning fully translated");
        }
        for(String key:List.of(clean(source.getFirst()),family.equals("FORTUNE")||family.equals("HARVESTING")||family.equals("SUNDER")?"Fortune":clean(source.getLast()))){
            var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));var input=new ArrayList<Component>();for(String s:source)input.add(row(s,2,11));check(joined(TooltipTranslation.components(input,false)).equals(joined(input.stream().map(SkyRuClient::component).toList())),"custom override veto "+id);flag("dictionary",db);
        }
    }
    private static final String FINNEGAN="Кандидат\n\nИстребитель вредителей\nБонус охотника на вредителей\nФилиппа к ☘ удаче фермерства\nтеперь длится 60 мин. Перезарядка появления\nвредителей сокращена на 20%.\n\nВеликий пир\nФиннеган превращает Праздник\nурожая в Великий пир, который\nдлится целый год SkyBlock. Также\nоткрывается Большая пекарня, где\nЗёрна можно обменять на\nособые предметы.\n\nЛучший из лучших\nВ фермерских конкурсах Джейкоба\nна 10% больше игроков\nпопадают в каждую категорию наград.\n\nПроцветающий бизнес\nВсе посетители Сада дают Хорошую\nмуку и +10% меди за заказы.\nПриглашает 5 уникальных\nпосетителей в ваш Сад. Посетители\nвысокой редкости на 20% чаще\nпоявляются в Саду.";
    private static void finnegan(JsonObject card)throws Exception{
        var lore=strings(card.getAsJsonArray("lore_legacy"));for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            var input=new ArrayList<Component>();input.add(row(card.get("name_legacy").getAsString(),mode,seed));for(String s:lore)input.add(row(s,mode,seed));var out=TooltipTranslation.components(input,false);var seq=TooltipTranslation.styled(input.stream().map(Component::getVisualOrderText).toList(),false);
            for(int repeat=0;repeat<5;repeat++){check(joined(out.subList(1,out.size())).equals(FINNEGAN),"whole Finnegan mode "+mode+" repeat "+repeat+" "+joined(out));check(String.join("\n",seq.subList(1,seq.size()).stream().map(TestCatalogueCorrections64::text).toList()).equals(FINNEGAN),"Finnegan sequence");check(numbers(joined(input)).equals(numbers(joined(out))),"Finnegan no extra value");check(numeric(input).equals(numeric(out)),"Finnegan numeric glyph styles/actions");out=TooltipTranslation.components(out,false);seq=TooltipTranslation.styled(seq,false);}
            anchor(input,23,"Fine",out,23,"Хорошую");anchor(input,24,"Flour",out,24,"муку");anchor(input,24,"Copper",out,24,"меди");anchor(input,26,"Garden",out,26,"Сад");
        }
    }
    private static Map<String,JsonObject> readRows(Path file)throws Exception{var out=new HashMap<String,JsonObject>();for(var raw:JsonParser.parseString(Files.readString(file)).getAsJsonArray()){var row=raw.getAsJsonObject();check(out.put(row.get("id").getAsString(),row)==null,"unique ID");}return out;}
    private static void trophyGuards(Path root,OfflineDictionary db)throws Exception{
        for(String key:List.of("Vanille BRONZE","Vanille","BRONZE")){
            var dir=root.resolve("trophy-override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой трофей\n");flag("dictionary",new OfflineDictionary(dir));
            var input=row("§9Vanille §8§lBRONZE",2,17);var expected=SkyRuClient.component(input);var output=TooltipTranslation.components(List.of(input),false).getFirst();
            check(text(output).equals(text(expected)),"trophy group does not mask user override "+key);check(styles(output.getVisualOrderText()).equals(styles(expected.getVisualOrderText())),"trophy override current actions "+key);
            if(key.equals("Vanille BRONZE"))check(text(TooltipTranslation.components(List.of(Component.literal(key)),false).getFirst()).equals("Мой трофей"),"uniform whole-title user override");
        }
        flag("dictionary",db);
        var input=Component.empty().append(Component.literal("Van").setStyle(Style.EMPTY.withColor(0x112233).withInsertion("first action"))).append(Component.literal("ille ").setStyle(Style.EMPTY.withColor(0x445566).withInsertion("second action"))).append(Component.literal("BRONZE").setStyle(Style.EMPTY.withBold(true).withColor(0x778899)));
        check(TooltipTranslation.components(List.of(input),false).getFirst()==input,"unknown multicolor name is not flattened");
    }
    private static void differential(Path project,Path candidate)throws Exception{
        var before=readRows(project.resolve("build/item-audit64-a4de0b74b10243fba6713d30f70978f7/final62/items.json"));var after=readRows(candidate.resolve("items.json"));check(before.keySet().equals(after.keySet())&&after.size()==8937,"same complete source coverage");int changed=0,trophies=0;var changedIDs=new JsonArray();
        for(var entry:after.entrySet()){String id=entry.getKey();var a=before.get(id);var b=entry.getValue();boolean different=false;for(String field:List.of("translated_title","component_lore","sequence_lore"))different|=!a.get(field).equals(b.get(field));
            check(a.get("source_lore").equals(b.get("source_lore")),"unchanged source "+id);check(!b.get("new_effective_style_candidate").getAsBoolean()&&!b.get("lost_effective_style_candidate").getAsBoolean(),"no introduced/dropped style sets "+id);
            if(!different)continue;changed++;changedIDs.add(id);boolean trophy=id.matches(".*_(BRONZE|SILVER|GOLD|DIAMOND)")&&a.get("title_format_difference_candidate").getAsBoolean();
            check(FAMILIES.contains(id.split(";")[0])||trophy||id.equals("FINNEGAN_MAYOR_MONSTER")||id.equals("BINGHOE"),"unreviewed output change "+id);
            if(trophy){trophies++;check(!b.get("title_format_difference_candidate").getAsBoolean(),"trophy color/uniform equal "+id);check(a.get("uniform_unformatted_title_result").equals(b.get("translated_title")),"authored existing trophy wording "+id);check(a.get("component_lore").equals(b.get("component_lore"))&&a.get("sequence_lore").equals(b.get("sequence_lore")),"title-only trophy scope "+id);}
            if(FAMILIES.contains(id.split(";")[0]))check(!b.get("effect_latin_word_candidate").getAsBoolean(),"selected core completely Russian "+id);
            if(id.equals("BINGHOE"))for(String field:List.of("component_lore","sequence_lore")){var old=a.getAsJsonArray(field);var next=b.getAsJsonArray(field);check(next.get(8).getAsString().equals("культуры."),"BINGHOE reviewed noun");for(int r=0;r<old.size();r++)if(r!=8)check(old.get(r).equals(next.get(r)),"BINGHOE only reviewed noun changed");}
        }
        check(changed==158&&trophies==120,"158 changed cards /120 title corrections; Punch II already had identical wording");Files.writeString(candidate.resolve("reviewed-changed-ids.json"),new GsonBuilder().setPrettyPrinting().create().toJson(changedIDs));
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),project=Path.of(args[1]).toAbsolutePath().getParent();fixture=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);fixture.setAccessible(true);
        var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        Path baseline=project.resolve("build/libs/WitherSkyRU-26.1.2-fabric-0.15.62-dev.jar");
        check(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(baseline))).equalsIgnoreCase("07D7632DD8C5772E1E54BCEF0A8E9B882BA87F30E1AB7569686224CDEFBAC4C3"),"pinned delivered62 oracle");
        try(var previous=new Previous(baseline.toUri().toURL())){
        var client=previous.loadClass("ru.wither.skyru.SkyRuClient");var dict=previous.loadClass("ru.wither.skyru.OfflineDictionary");var oldDb=dict.getConstructor(Path.class).newInstance(root.resolve("old"));dict.getMethod("setRecordUnknown",boolean.class).invoke(oldDb,false);oldFlag(client,"dictionary",oldDb);oldFlag(client,"enabled",true);oldFlag(client,"onlyHypixel",false);oldComponents=previous.loadClass("ru.wither.skyru.TooltipTranslation").getMethod("components",List.class,boolean.class);
        var catalogue=JsonParser.parseString(Files.readString(project.resolve("build/item-audit64-a4de0b74b10243fba6713d30f70978f7/catalogue.json"))).getAsJsonObject();int books=0,trophies=0;
        for(var raw:catalogue.getAsJsonArray("records")){var card=raw.getAsJsonObject();String id=card.get("id").getAsString();if(FAMILIES.contains(id.split(";")[0])){effect(card,root,db);books++;}if(id.equals("FINNEGAN_MAYOR_MONSTER"))finnegan(card);
            if(id.matches(".*_(BRONZE|SILVER|GOLD|DIAMOND)")&&clean(card.get("name_legacy").getAsString()).matches(".* (BRONZE|SILVER|GOLD|DIAMOND)")){
                String source=card.get("name_legacy").getAsString();String uniform=text(SkyRuClient.component(Component.literal(clean(source))));for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
                    var input=row(source,mode,seed);var output=TooltipTranslation.components(List.of(input),false).getFirst();check(text(output).equals(uniform),"all captured trophy ranks "+id);String grade=clean(source).substring(clean(source).lastIndexOf(' ')+1);String translated=switch(grade){case "BRONZE"->"бронзовый";case "SILVER"->"серебряный";case "GOLD"->"золотой";default->"алмазный";};anchor(List.of(input),0,grade,List.of(output),0,translated);check(TooltipTranslation.components(List.of(input),true).getFirst()==input,"trophy Shift identity");check(numeric(List.of(input)).equals(numeric(List.of(output))),"Obfuscated rank digits preserved");}trophies++;
            }
        }
        check(books==37&&trophies==120,"all selected cards/levels");trophyGuards(root,db);differential(project,Path.of(args[2]));
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var c=Component.literal("Heal 2.4❤ every time you hit a mob.");var msg=new GuiMessage(0,c,null,channel,null);check(SkyRuClient.npcChat(msg)==msg,"ordinary chat not translated");}
        flag("enabled",false);var input=List.<Component>of(Component.literal("Vanille BRONZE"));check(TooltipTranslation.components(input,false)==input,"disabled identity");
        System.out.println("PASS: catalogue corrections64 "+checks+" assertions;37 full book levels/9 families,120 current trophy titles,Finnegan;4 layouts/5 fresh styles/5 repeats;8937-card pinned differential,158 reviewed changes;not all items fixed or live FPS");
        }
    }
}
