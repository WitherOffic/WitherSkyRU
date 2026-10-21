import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import ru.wither.skyru.*;

/** Eight current primary cards + exact mixed-language screenshot, no game/actions. */
public final class TestMinionFuel52 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static String joined(List<Component> c){return String.join("\n",c.stream().map(TestMinionFuel52::plain).toList());}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static Component row(String raw,int mode,int seed){
        var inherited=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/original-fuel-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("old hint "+seed))).withInsertion("frame "+seed);var c=Component.literal(raw).setStyle(inherited);
        if(mode==0)return c;if(mode==1)return Component.literal(clean(raw)).setStyle(inherited.withColor(0xaaaaaa));
        String text=plain(c);var ss=styles(c);var out=Component.empty();for(int i=0;i<text.length();i++){Style s=ss.get(i);if(mode==3&&Character.isDigit(text.charAt(i)))s=s.withColor(0x100000+i*0x101).withInsertion("digit "+seed+"/"+i);out.append(Component.literal(text.substring(i,i+1)).setStyle(s));}return out;
    }
    private static void span(Component old,String from,Component next,String target,boolean exact){int a=plain(old).indexOf(from),b=plain(next).indexOf(target);check(a>=0&&b>=0,"missing span "+from);var ss=styles(old);var tt=styles(next);if(exact)for(int i=0;i<from.length();i++)check(ss.get(a+i).equals(tt.get(b+i)),"original numeric/action "+from);else check(ss.get(a).equals(tt.get(b)),"semantic style "+from);}
    private static void duration(Component old,String value,Component next){int a=plain(old).lastIndexOf(value),b=plain(next).lastIndexOf(value);check(a>=0&&b>=0,"duration present");var ss=styles(old);var tt=styles(next);for(int i=0;i<value.length();i++)check(ss.get(a+i).equals(tt.get(b+i)),"duration glyph/action "+value);}
    private static void protectedChat(Component c){for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=new GuiMessage(0,c,null,channel,null);check(SkyRuClient.npcChat(m)==m,"whole chat identity");}var m=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(m)==m,"signed identity");}
    private static final Map<String,String[]> VALUES=Map.of("HAMSTER_WHEEL",new String[]{"50","24","ч"},"FOUL_FLESH",new String[]{"90","5","ч"},"CHARCOAL",new String[]{"5","30","мин"},"COAL",new String[]{"5","30","мин"},"COAL_BLOCK",new String[]{"5","5","ч"},"ENCHANTED_CHARCOAL",new String[]{"20","36","ч"},"ENCHANTED_COAL",new String[]{"10","24","ч"},"ENCHANTED_BREAD",new String[]{"5","12","ч"});
    private static List<String> expected(String pct,String duration,String unit,boolean old){return old?List.of("Ускоряет работу миньона","на "+pct+"%","в течение "+duration+" "+unit+"!"):List.of("Ускоряет работу миньона","на "+pct+"% в течение "+duration,unit+"!");}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var records=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("MINION_FUEL_0.15.52.json"))).getAsJsonObject().getAsJsonArray("records");check(records.size()==8,"eight current fuel cards");
        for(var raw:records){var card=raw.getAsJsonObject();String id=card.get("id").getAsString();var v=VALUES.get(id);check(v!=null,"reviewed ID");var source=new ArrayList<String>();source.add(card.get("title_legacy").getAsString());for(var l:card.getAsJsonArray("lore_legacy"))source.add(l.getAsString());int start=-1;for(int i=0;i<source.size();i++)if(clean(source.get(i)).equals("Increases the speed of"))start=i;check(start>=0,"known source wrap "+id);final int at=start;
            check(clean(source.get(at+1)).equals("your minion by "+v[0]+"% for "+v[1]),"primary actual values "+id);String unit=v[2].equals("ч")?"hours":"minutes";check(clean(source.get(at+2)).equals(unit+"!"),"primary actual unit");
            for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
                final int layout=mode,frame=seed;var original=source.stream().map(s->row(s,layout,frame)).toList();var result=TooltipTranslation.components(original,false);var target=expected(v[0],v[1],v[2],false);
                for(int n=0;n<5;n++){check(joined(result.subList(at,at+3)).equals(String.join("\n",target)),"whole current prose "+id+" => "+joined(result));span(original.get(at+1),v[0]+"%",result.get(at+1),v[0]+"%",true);duration(original.get(at+1),v[1],result.get(at+1));span(original.get(at),"Increases",result.get(at),"Ускоряет",false);result=TooltipTranslation.components(result,false);}
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);check(String.join("\n",seq.subList(at,at+3).stream().map(AuditMiningStyles::plain).toList()).equals(String.join("\n",target)),"Sequence full prose");
                check(joined(original).equals(String.join("\n",source.stream().map(TestMinionFuel52::clean).toList())),"source immutable");String rarity=clean(source.getLast()).split(" ")[0];check(plain(result.getLast()).equals(rarity)||plain(result.getLast()).startsWith(rarity+" "),"English rarity retained "+id+" mode"+mode+" => "+plain(result.getLast()));span(original.getLast(),rarity,result.getLast(),rarity,true);var held=TooltipTranslation.components(original,true);check(held.getFirst()==original.getFirst(),"Shift title identity");check(joined(held.subList(at,at+3)).equals(String.join("\n",target)),"Shift body");protectedChat(original.get(at+1));
            }
        }
        int forms=0;
        for(String head:List.of("Increases the speed of","increases the speed of","Увеличивает скорость работы"))for(boolean old:List.of(false,true))for(String unit:old?List.of("hours","часов"):List.of("hours","часов","minutes","минут"))for(String[] values:List.of(new String[]{"50","24"},new String[]{"5","5"},new String[]{"12.5","0.5"},new String[]{"0","0"})){
            forms++;final int frame=forms;String ru=unit.equals("hours")||unit.equals("часов")?"ч":"мин";var target=expected(values[0],values[1],ru,old);var source=old?List.of(head,"your minion by "+values[0]+"%","for "+values[1]+" "+unit+"!"):List.of(head,"your minion by "+values[0]+"% for "+values[1],unit+"!");var rows=source.stream().map(s->row(s,3,frame)).toList();var translated=TooltipTranslation.components(rows,false);check(joined(translated).equals(String.join("\n",target)),"closed partial/old/synthetic => "+joined(translated));
            span(rows.get(1),values[0]+"%",translated.get(1),values[0]+"%",true);int durationRow=old?2:1;duration(rows.get(durationRow),values[1],translated.get(durationRow));
            var broken=new ArrayList<Component>(rows);broken.set(2,Component.literal(source.get(2)+" UNKNOWN"));check(joined(TooltipTranslation.components(broken,false)).equals(joined(broken.stream().map(SkyRuClient::component).toList())),"unknown neighbour declines group");
        }check(forms==72,"72 closed synthetic layouts");
        var mixed=List.<Component>of(row("Увеличивает скорость работы",0,2),row("your minion by §a50%§7 for 24",0,2),row("часов!",0,2));check(joined(TooltipTranslation.components(mixed,false)).equals("Ускоряет работу миньона\nна 50% в течение 24\nч!"),"exact screenshot partial source");
        for(String key:List.of("Increases the speed of","hours","your minion by 50% for 24")){var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));check(!joined(TooltipTranslation.components(mixed,false)).equals("Ускоряет работу миньона\nна 50% в течение 24\nч!"),"English override on partial source");}flag("dictionary",db);
        for(String text:List.of("Vol: 37.23k/14.73k","Vol: 0/1","NPC Sell Price: 20,000.0 coins","Цена продажи NPC20,000.0 монеты")){
            var c=row(text,3,8);String target=text.startsWith("Vol:")?text.replace("Vol:","Объём:"):"Продажа НПС: 20,000.0 монет";var translated=SkyRuClient.component(c);check(plain(translated).equals(target),"market screenshot => "+plain(translated));String number=text.startsWith("Vol:")?text.substring(5):"20,000.0";span(c,number,translated,number,true);protectedChat(c);check(AuditMiningStyles.plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(target),"market Sequence");
        }
        flag("enabled",false);check(TooltipTranslation.components(mixed,false)==mixed,"disabled tooltip identity");check(UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")),"input original");
        System.out.println("PASS: minion fuel52 "+checks+" assertions;8 current primary fuel cards/72 closed layouts/exact mixed screenshot/fresh styles/values/actions/Shift/chat/overrides; not all fuel or live server");
    }
}
