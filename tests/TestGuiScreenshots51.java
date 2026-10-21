import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import ru.wither.skyru.*;

/** Four screenshots, complete closed paragraphs and five original cached definitions. */
public final class TestGuiScreenshots51 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String joined(List<Component> rows){return String.join("\n",rows.stream().map(TestGuiScreenshots51::plain).toList());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static Style at(Component c,String word){int start=plain(c).indexOf(word);check(start>=0,"missing "+word);return styles(c).get(start);}
    private static List<Map<String,String>> variants(JsonObject definition){
        List<Map<String,String>> result=new ArrayList<>();result.add(Map.of());
        for(var field:definition.entrySet()){var next=new ArrayList<Map<String,String>>();for(var previous:result)for(var raw:field.getValue().getAsJsonArray()){var row=new HashMap<>(previous);row.put(field.getKey(),raw.getAsString());next.add(Map.copyOf(row));}result=next;}
        return result;
    }
    private static String replace(String s,Map<String,String> choices){for(var e:choices.entrySet())s=s.replace(e.getKey(),e.getValue());return s;}
    private static Component row(String s,int mode,int seed){
        Style inherited=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/unchanged51-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hint "+seed))).withInsertion("original "+seed);
        if(mode==0)return Component.literal(s).setStyle(inherited);
        if(mode==1)return Component.literal(clean(s)).setStyle(inherited.withColor(0xaaaaaa));
        var parsed=Component.literal(s).setStyle(inherited);var out=Component.empty();var ss=styles(parsed);String text=plain(parsed);
        for(int i=0;i<text.length();){int cp=text.codePointAt(i);String token=new String(Character.toChars(cp));Style style=ss.get(i);if(mode==3&&Character.isDigit(cp))style=style.withColor(0x110000+i*0x101);out.append(Component.literal(token).setStyle(style));i+=Character.charCount(cp);}
        return out;
    }
    private static void protection(Component c){
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var message=new GuiMessage(0,c,null,channel,null);check(SkyRuClient.npcChat(message)==message,"whole ordinary chat original");}
        var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"whole signed original");
    }
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("Jerry The Assistant","Джерри — помощник"),Map.entry("Sam the Garden Assistant","Сэм — помощница в Саду"),
        Map.entry("Visits & Guesting","Посещения и гости"),Map.entry("Warp to Garden","Переместиться в Сад"),Map.entry("Add-on Experiment","Дополнительный эксперимент"),
        Map.entry("Click to learn!","Нажмите, чтобы узнать больше!"),Map.entry("Click to warp!","Нажмите, чтобы переместиться!"),Map.entry("Click to play!","Нажмите, чтобы играть!"),
        Map.entry("Play add-ons first!","Сначала пройдите дополнительные эксперименты!"),Map.entry("On cooldown!","На перезарядке!"),Map.entry("Experiments on cooldown!","Эксперименты на перезарядке!"),
        Map.entry("Chronomatron ➜ Stakes","Хрономатрон ➜ Сложность"),Map.entry("Ultrasequencer ➜ Stakes","Ультрасеквенсор ➜ Сложность"),Map.entry("Superpairs ➜ Stakes","Суперпары ➜ Сложность"),
        Map.entry("Beginner Experiment","Начальный эксперимент"),Map.entry("High Experiment","Продвинутый эксперимент"),Map.entry("Grand Experiment","Большой эксперимент"),
        Map.entry("Supreme Experiment","Высший эксперимент"),Map.entry("Transcendent Experiment","Трансцендентный эксперимент"),Map.entry("Metaphysical Experiment","Метафизический эксперимент"),
        Map.entry("Click to view boss!","Нажмите, чтобы посмотреть босса!"),Map.entry("Auto-Slayer","Автозапуск охоты на боссов"),
        Map.entry("☠ Revenant Horror","☠ Возрождённый ужас"),Map.entry("☠ Tarantula Broodfather","☠ Прародитель тарантулов"),Map.entry("☠ Sven Packmaster","☠ Свен — вожак стаи"),
        Map.entry("☠ Voidgloom Seraph","☠ Серафим мрачной пустоты"),Map.entry("☠ Inferno Demonlord","☠ Повелитель демонов Инферно"),Map.entry("☠ Riftstalker Bloodfiend","☠ Кровавый демон Разлома"));
    private static void cacheLabels(JsonElement e,Set<String> out){
        if(e.isJsonArray()){for(var v:e.getAsJsonArray())cacheLabels(v,out);return;}if(!e.isJsonObject())return;var o=e.getAsJsonObject();
        if(o.has("type")&&o.get("type").isJsonPrimitive()&&o.get("type").getAsString().equals("title")&&o.has("title"))out.add(o.get("title").getAsString());
        if(o.has("mode")&&o.get("mode").getAsString().equals("equals")&&o.has("name"))out.add(o.get("name").getAsString());
        for(var field:o.entrySet())if(field.getValue().isJsonObject()||field.getValue().isJsonArray())cacheLabels(field.getValue(),out);
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("chatNotifications",new ChatNotificationTranslator(db));
        int paragraphVariants=0;
        var records=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("GUI_SCREENSHOTS_0.15.51.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:records){var r=raw.getAsJsonObject();String expected=String.join("\n",r.getAsJsonArray("expected").asList().stream().map(JsonElement::getAsString).toList());
            for(var choice:variants(r.getAsJsonObject("variants"))){paragraphVariants++;var source=r.getAsJsonArray("source_legacy").asList().stream().map(e->replace(e.getAsString(),choice)).toList();
                for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
                    final int layout=mode,frame=seed;var original=source.stream().map(s->row(s,layout,frame)).toList();var result=TooltipTranslation.components(original,false);var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                    for(int repeat=0;repeat<5;repeat++){check(joined(result).equals(expected),r.get("key")+" mode"+mode+" => "+joined(result));check(String.join("\n",seq.stream().map(AuditMiningStyles::plain).toList()).equals(expected),"sequence expected");
                        for(var a:r.getAsJsonArray("anchors")){var anchor=a.getAsJsonArray();var old=original.get(anchor.get(0).getAsInt());var next=result.get(anchor.get(2).getAsInt());String from=replace(anchor.get(1).getAsString(),choice),to=anchor.get(3).getAsString();check(at(old,from).equals(at(next,to)),"semantic color/action "+from);}
                        result=TooltipTranslation.components(result,false);seq=TooltipTranslation.styled(seq,false);
                    }
                    check(joined(original).equals(String.join("\n",source.stream().map(TestGuiScreenshots51::clean).toList())),"original source immutable");
                    var withTitle=new ArrayList<Component>();withTitle.add(Component.literal(r.get("title").getAsString()));withTitle.addAll(original);var held=TooltipTranslation.components(withTitle,true);check(held.getFirst()==withTitle.getFirst(),"Shift title identity");check(joined(held.subList(1,held.size())).equals(expected),"Shift body translated");protection(original.getFirst());
                    var broken=new ArrayList<Component>(original);broken.set(broken.size()-1,row(source.getLast()+" UNKNOWN",mode,seed));check(joined(TooltipTranslation.components(broken,false)).equals(joined(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbors decline group");
                }
                for(String key:List.of(clean(r.getAsJsonArray("source_legacy").get(0).getAsString()),r.getAsJsonArray("anchors").get(0).getAsJsonArray().get(1).getAsString())){
                    var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));var original=source.stream().map(s->row(s,2,1)).toList();check(joined(TooltipTranslation.components(original,false)).equals(joined(original.stream().map(SkyRuClient::component).toList())),"English original override on partial input");flag("dictionary",db);
                }
            }
        }
        check(paragraphVariants==16,"sixteen closed language layouts");
        String[][] fullCards={
            {"§dChronomatron\n§7Add-on Experiment\n\n§aПовторение §7the musical §dpattern §7to form the\n§7longest chain of notes.\n\n§7Earn extra §3Enchanting Exp §7и\n§7extra clicks on your next\n§dSuperpairs!\n\n§eClick to play!\n§9§oMinecraft",
             "Хрономатрон\nДополнительный эксперимент\n\nПовторяйте мелодию, чтобы составить\nсамую длинную цепочку нот.\n\nДаёт дополнительный опыт зачарования и\nдополнительные ходы в следующих\nСуперпарах!\n\nНажмите, чтобы играть!\nMinecraft"},
            {"§aVisits & Guesting\n\n§7Learn all about how to §a/visit §7Игроки\n§7across the SkyBlock universe!\n\n§eClick to learn!\n§9§oMinecraft",
             "Посещения и гости\n\nУзнайте, как с помощью /visit посещать игроков\nв мире SkyBlock!\n\nНажмите, чтобы узнать больше!\nMinecraft"},
            {"§aWarp to Garden\n§7Teleports you to your §aСад§7,\n§7where you can grow and harvest\n§eКультуры §7and sell them to §bcustomers§7.\n\n§eClick to warp!\n§9§oMinecraft",
             "Переместиться в Сад\nПеремещает в ваш Сад,\nгде можно выращивать и собирать\nурожай и продавать его посетителям.\n\nНажмите, чтобы переместиться!\nMinecraft"},
            {"§c☠ Voidgloom Seraph\n§5Ender\n\n§7If Necron is the right-hand of\n§7the Wither King, this dark\n§7demigod is the left-hand.\n\n§cSlay Sven Packmaster IV!\n§9§oMinecraft",
             "☠ Серафим мрачной пустоты\nЭндер\n\nЕсли Некрон — правая рука\nКороля иссушителей, то этот тёмный\nполубог — его левая рука.\n\nУбить Свена-вожака IV!\nMinecraft"}
        };
        for(var card:fullCards)for(int seed=0;seed<5;seed++){
            final int frame=seed;var original=Arrays.stream(card[0].split("\n",-1)).map(s->row(s,0,frame)).toList();var result=TooltipTranslation.components(original,false);
            for(int repeat=0;repeat<5;repeat++){check(joined(result).equals(card[1]),"whole screenshot card => "+joined(result));result=TooltipTranslation.components(result,false);}
            check(joined(original).equals(clean(card[0])),"whole original card immutable");var held=TooltipTranslation.components(original,true);check(held.getFirst()==original.getFirst(),"whole Shift original title");
            check(joined(held.subList(1,held.size())).equals(card[1].substring(card[1].indexOf('\n')+1)),"whole Shift body/buttons");
        }
        var cache=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("GUI_LABELS_0.15.51.json"))).getAsJsonObject();var labels=new TreeSet<String>();for(var record:cache.getAsJsonArray("records"))cacheLabels(record.getAsJsonObject().get("definition"),labels);check(cache.getAsJsonArray("records").size()==5,"five cache sources");
        for(String source:labels){String value=db.translateForSearch(source);check(!value.equals(source),"cached visible label unchanged: "+source);String prose=value.replaceAll("\\b[IVXLCDM]{1,12}\\b","");check(!prose.matches(".*[A-Za-z]{3,}.*"),"cached label partly English: "+source+" => "+value);}
        for(var label:LABELS.entrySet())for(int seed=0;seed<5;seed++){
            var c=row(label.getKey(),1,seed);var result=SkyRuClient.component(c);check(plain(result).equals(label.getValue()),"label "+label.getKey()+" => "+plain(result));check(styles(result).stream().allMatch(s->s.equals(c.getStyle())),"fresh label styles/actions");check(AuditMiningStyles.plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(label.getValue()),"label sequence");protection(c);
            for(int repeat=0;repeat<5;repeat++){result=SkyRuClient.component(result);check(plain(result).equals(label.getValue()),"label repeat");}
        }
        Style icon=Style.EMPTY.withColor(0xffaa00).withInsertion("icon"),name=Style.EMPTY.withColor(0xff5555).withClickEvent(new ClickEvent.RunCommand("/unchanged-boss"));
        for(var label:LABELS.entrySet())if(label.getKey().startsWith("☠ ")){var c=Component.empty().append(Component.literal("☠ ").setStyle(icon)).append(Component.literal(label.getKey().substring(2)).setStyle(name));var result=SkyRuClient.component(c);check(plain(result).equals(label.getValue()),"split boss name");check(at(result,"☠").equals(icon)&&at(result,label.getValue().substring(2)).equals(name),"boss icon/name actions");}
        flag("enabled",false);var original=List.<Component>of(Component.literal("If Necron is the right-hand of"));check(TooltipTranslation.components(original,false)==original,"disabled identity");flag("enabled",true);
        check(UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")),"typed input stays original");
        System.out.println("PASS: GUI screenshots51 "+checks+" assertions;5 whole paragraphs/16 language layouts/"+labels.size()+" cached visible labels; four styles/fresh actions/Shift/chat/overrides; not all live menus");
    }
}
