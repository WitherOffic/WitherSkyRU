import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Two user screenshots plus synthetic tiers; never executes server actions or a game. */
public class TestBestiaryScreenshots {
    private static int checks;
    private static final Style HEADER=Style.EMPTY.withColor(0xffaa00).withBold(true).withInsertion("original heading");
    private static final Style OLD=Style.EMPTY.withColor(0xaaaaaa).withHoverEvent(new HoverEvent.ShowText(Component.literal("old tier")));
    private static final Style NEXT=Style.EMPTY.withColor(0xffff55).withInsertion("next tier");
    private static final Style HEALTH=Style.EMPTY.withColor(0xff5555).withHoverEvent(new HoverEvent.ShowText(Component.literal("original health")));
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static void span(Component c,String word,Style style){spanAt(c,c.getString().indexOf(word),word.length(),style);}
    private static void spanAt(Component c,int at,int length,Style style){check(at>=0,"missing span");var ss=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)ss.add(s);return true;});for(int i=at;i<at+length;i++)check(ss.get(i).equals(style),"style/action at "+at);}
    private static String roman(int n){var out=new StringBuilder();int[] values={100,90,50,40,10,9,5,4,1};String[] glyphs={"C","XC","L","XL","X","IX","V","IV","I"};for(int i=0;i<values.length;i++)while(n>=values[i]){out.append(glyphs[i]);n-=values[i];}return out.toString();}
    private static GuiMessage message(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static Component server(Component c,String target){
        String original=c.getString();var result=SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();
        check(result.getString().equals(target),original+" => "+result.getString());check(c.getString().equals(original),"original immutable");
        for(int n=0;n<5;n++)check(SkyRuClient.npcChat(message(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"idempotent server");
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=message(c,channel);check(SkyRuClient.npcChat(msg)==msg,"channel unchanged");}
        var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed unchanged");return result;
    }
    private static Component block(String old,String next,String border,String time){
        return Component.empty().append(Component.literal(time+border+"\n"+time+"\n"+time+"     "))
            .append(Component.literal("BESTIARY MILESTONE").setStyle(HEADER)).append(Component.literal("\n"+time+"         "))
            .append(Component.literal(old).setStyle(OLD)).append(Component.literal("➜").setStyle(OLD)).append(Component.literal(next).setStyle(NEXT))
            .append(Component.literal("\n"+time+"\n"+time+"       ")).append(Component.literal("REWARD").setStyle(HEADER))
            .append(Component.literal("\n"+time+"         +1").setStyle(OLD)).append(Component.literal("❤ Health").setStyle(HEALTH))
            .append(Component.literal("\n"+time+"\n"+time+border));
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("chatNotifications",new ChatNotificationTranslator(db));
        for(int n=1;n<=200;n++){
            String tier=roman(n),title="Bestiary Milestone "+tier,target="Этап Бестиария "+tier;
            var c=Component.empty().append(Component.literal("Bestiary Milestone").setStyle(HEALTH)).append(Component.literal(" "+tier).setStyle(NEXT));
            check(db.translateForSearch(title).equals(target),"plain/search title "+title);
            Component result=SkyRuClient.component(c);check(result.getString().equals(target),"split title "+title);span(result,tier,NEXT);span(result,"Этап Бестиария",HEALTH);
            var s=SkyRuClient.sequence(c.getVisualOrderText());check(plain(s).equals(target),"sequence title");
            for(int r=0;r<5;r++){result=SkyRuClient.component(result);s=SkyRuClient.sequence(s);check(result.getString().equals(target)&&plain(s).equals(target),"repeated title");}
            check(TooltipTranslation.components(List.of(c),true).getFirst()==c,"Shift original component");
            check(plain(TooltipTranslation.styled(List.of(c.getVisualOrderText()),true).getFirst()).equals(title),"Shift original sequence");check(c.getString().equals(title),"immutable title");
        }
        var display=Class.forName("ru.wither.skyru.InterfaceDisplay").getDeclaredMethod("edits",String.class,java.util.function.Function.class);display.setAccessible(true);
        for(String bad:List.of("Bestiary Milestone IIII","Bestiary Milestone VX","Bestiary Milestone XLV Nick","Bestiary Milestone 45","Bestiary Milestone ","Bestiary Milestone XLV!"))check(((List<?>)display.invoke(null,bad,(java.util.function.Function<String,String>)db::translateForSearch)).isEmpty(),"new title grammar rejects "+bad);
        for(String time:List.of("","[00:06:30] "))for(String border:List.of("--------------------","━━━━━━━━━━━━━━━━━━━━"))for(String[] tiers:new String[][]{{"XLII","XLIII"},{"I","II"},{"XCIX","C"}}){
            var c=block(tiers[0],tiers[1],border,time);var result=server(c,c.getString().replace("BESTIARY MILESTONE","ЭТАП БЕСТИАРИЯ").replace("REWARD","НАГРАДА").replace("Health","Здоровье"));
            span(result,"ЭТАП БЕСТИАРИЯ",HEADER);span(result,"НАГРАДА",HEADER);span(result,tiers[0],OLD);spanAt(result,result.getString().indexOf("➜")+1,tiers[1].length(),NEXT);span(result,"Здоровье",HEALTH);span(result,"+1",OLD);
        }
        for(String caption:List.of("CLICK HERE to claim it!",">>> CLICK HERE to claim it! <<<")){
            var click=new ClickEvent.RunCommand("/original-claim");var hover=new HoverEvent.ShowText(Component.literal("original claim hint"));var action=HEADER.withClickEvent(click).withHoverEvent(hover);var suffix=NEXT.withClickEvent(click).withHoverEvent(hover);
            int start=caption.indexOf("CLICK HERE");var c=Component.empty().append(Component.literal(caption.substring(0,start)).setStyle(action))
                .append(Component.literal("CLICK HERE").setStyle(action)).append(Component.literal(" ").setStyle(action)).append(Component.literal("to claim it!").setStyle(suffix))
                .append(Component.literal(caption.substring(start+"CLICK HERE to claim it!".length())).setStyle(action));
            var result=server(c,caption.replace("CLICK HERE","НАЖМИТЕ ЗДЕСЬ").replace("to claim it!","чтобы забрать награду!"));span(result,"НАЖМИТЕ ЗДЕСЬ",action);span(result,"чтобы забрать награду!",suffix);
            check(action.getClickEvent()==click&&action.getHoverEvent()==hover,"same original action, never invoked");
        }
        server(Component.literal("BESTIARY MILESTONE").setStyle(HEADER),"ЭТАП БЕСТИАРИЯ");
        for(String time:List.of("","[00:06:30] ")){
            server(Component.literal(time+" ".repeat(26)+"BESTIARY MILESTONE").setStyle(HEADER),time+" ".repeat(26)+"ЭТАП БЕСТИАРИЯ");
            server(Component.literal(time+" ".repeat(36)+"REWARD").setStyle(HEADER),time+" ".repeat(36)+"НАГРАДА");
            var reward=Component.empty().append(Component.literal(time+" ".repeat(34)+"+1").setStyle(OLD)).append(Component.literal(" Health").setStyle(HEALTH));
            var result=server(reward,time+" ".repeat(34)+"+1 Здоровье");span(result,"+1",OLD);span(result," Здоровье",HEALTH);
            server(Component.literal(time+" ".repeat(35)+"XLII➡XLIII").setStyle(NEXT),time+" ".repeat(35)+"XLII➡XLIII");
        }
        for(int padding:new int[]{0,1,6,15,61})for(String word:List.of("REWARD","+1 Health","REWARD Nick","+1 Health Nick")){
            var c=Component.literal(" ".repeat(padding)+word);check(SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content()==c,"unpadded/malformed notice remains original");
        }
        for(String glyph:List.of("❤","")){
            String legacy="§3§m--------------------§r\n\n    §6§lBESTIARY MILESTONE§r\n      §8XLII§6➜§eXLIII\n\n       §6§lREWARD§r\n       §8+1§c"+glyph+" Health§r\n\n§3§m--------------------";
            var c=Component.literal(legacy).setStyle(OLD);String target=plain(c.getVisualOrderText()).replace("BESTIARY MILESTONE","ЭТАП БЕСТИАРИЯ").replace("REWARD","НАГРАДА").replace("Health","Здоровье");
            var result=server(c,target);check(plain(result.getVisualOrderText()).equals(target),"legacy block glyph/colors "+glyph);
        }
        var legacyTitle=Component.literal("§cBestiary Milestone §fXLV");check(plain(SkyRuClient.component(legacyTitle).getVisualOrderText()).equals("Этап Бестиария XLV"),"legacy title rendering");
        var mixed=Component.empty().append(Component.literal("BESTIARY ").setStyle(HEADER)).append(Component.literal("MILE").setStyle(NEXT)).append(Component.literal("STONE").setStyle(HEADER));
        check(SkyRuClient.npcChat(message(mixed,GuiMessageSource.SYSTEM_SERVER)).content()==mixed,"unknown internal semantic gradient fails closed");
        for(String source:List.of("REWARD","+1❤ Health","BESTIARY MILESTONE\nXLII➜XLIII\nREWARD\n+1❤ Health\nNick: hello","BESTIARY MILESTONE\nIIII➜V\nREWARD\n+1❤ Health","BESTIARY MILESTONE\nXLII➜XLIII\nREWARD\n+1❤ Health Nick",">>> CLICK HERE to claim it! <<< Nick")){
            var c=Component.literal(source);check(SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content()==c,"unrecognized block/neighbor original "+source);
        }
        var sample=block("XLII","XLIII","--------------------","");
        for(String key:List.of("BESTIARY MILESTONE","REWARD","Health",">>> CLICK HERE to claim it! <<<","CLICK HERE","to claim it!")){
            var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");var translator=new ChatNotificationTranslator(new OfflineDictionary(dir));
            var input=key.contains("claim")||key.equals("CLICK HERE")?Component.literal(">>> CLICK HERE to claim it! <<<"):sample;
            check(translator.translate(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"own override "+key);
        }
        var dir=root.resolve("title-override");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),"Bestiary Milestone XLV\tМой этап XLV\n");flag("dictionary",new OfflineDictionary(dir));
        check(SkyRuClient.component(Component.literal("Bestiary Milestone XLV")).getString().equals("Мой этап XLV"),"full title override");flag("dictionary",db);
        flag("translateGameChat",false);check(SkyRuClient.npcChat(message(sample,GuiMessageSource.SYSTEM_SERVER)).content()==sample,"notice toggle");flag("translateGameChat",true);
        flag("enabled",false);check(SkyRuClient.component(Component.literal("Bestiary Milestone XLV")).getString().equals("Bestiary Milestone XLV"),"disabled title");check(SkyRuClient.npcChat(message(sample,GuiMessageSource.SYSTEM_SERVER)).content()==sample,"disabled server");flag("enabled",true);
        check(UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")),"typed input protected");check(SkyRuClient.component(Component.literal("SKYBLOCK")).getString().equals("SKYBLOCK"),"brand original");
        System.out.println("PASS: bestiary screenshots "+checks+" assertions; 200 synthetic tiers, exact server rewards/click labels; no game");
    }
}
