import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

/** Four recorded action shapes, Maniacs and analyser terminology; synthetic counts/styles. */
public class TestCommissionExpansion {
    private static int checks,variants;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff).withInsertion("source verb");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/original"));
    private static final Style NUMBER=Style.EMPTY.withColor(0x55ff55).withInsertion("actual quota");
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String k,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(k);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(FormattedCharSequence s){var out=new ArrayList<Style>();s.accept((i,t,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(t);return true;});return out;}
    private static void verify(Component c,String target,String count){
        String source=c.getString();var result=SkyRuClient.component(c);check(result.getString().equals(target),source+" => "+result.getString());
        check(plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(target),"glyph sequence");check(c.getString().equals(source),"immutable source");
        int oldAt=source.indexOf(count),newAt=target.indexOf(count);var old=styles(c.getVisualOrderText());var next=styles(result.getVisualOrderText());
        for(int i=0;i<count.length();i++)check(old.get(oldAt+i).equals(next.get(newAt+i)),"original numeric glyph/action");
        for(int n=0;n<5;n++)check(SkyRuClient.component(result).getString().equals(target),"five repeats");
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=new GuiMessage(0,c,null,channel,null);check(SkyRuClient.npcChat(m)==m,"whole user/client chat untouched");}
        variants++;
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        String[][] shapes={{"Open","chest","chests","Откройте","сундук","сундука","сундуков"},{"Loot","Frozen Corpse","Frozen Corpses","Обыщите","замёрзшее тело","замёрзших тела","замёрзших тел"},{"Discover","Mineshaft","Mineshafts","Посетите","глацитовую шахту","глацитовые шахты","глацитовых шахт"},{"Obtain","Suspicious Scrap","Suspicious Scrap","Получите","подозрительный обломок","подозрительных обломка","подозрительных обломков"}};
        for(String count:List.of("1","2","3","5","11","14","21","22","25","100","1,001"))for(String dot:List.of("",".")){
            long n=Long.parseLong(count.replace(",",""))%100;int form=n>=11&&n<=14?6:n%10==1?4:n%10>=2&&n%10<=4?5:6;
            for(var p:shapes){String noun=count.equals("1")?p[1]:p[2],source=p[0]+" "+count+" "+noun+dot,target=p[3]+" "+count+" "+p[form]+".";
                var c=Component.empty().append(Component.literal(p[0]+" ").setStyle(BODY));for(int i=0;i<count.length();i++)c.append(Component.literal(count.substring(i,i+1)).setStyle(NUMBER.withColor(0x100000+i)));c.append(Component.literal(" ").setStyle(BODY)).append(Component.literal(noun).setStyle(TERM)).append(Component.literal(dot).setStyle(BODY));
                check(db.translate(source).equals(target),"plain commission");verify(c,target,count);
            }
            for(boolean located:List.of(false,true)){String noun=form==4?"маньяка":"маньяков",area=located?" in the Glacite Mineshafts":"";String source="Slay "+count+" Maniacs"+area+dot,target="Победите "+count+" "+noun+(located?" в Глацитовых шахтах":"")+".";
                var c=Component.empty().append(Component.literal("Slay ").setStyle(BODY)).append(Component.literal(count).setStyle(NUMBER)).append(Component.literal(" ").setStyle(BODY)).append(Component.literal("Maniacs").setStyle(TERM)).append(Component.literal(area+dot).setStyle(BODY));check(db.translate(source).equals(target),"Maniacs plain");verify(c,target,count);
            }
        }
        check(db.translate("Discover 1 Mineshaft.").equals("Посетите 1 глацитовую шахту."),"enter any shaft, not spawn your own");
        check(db.translate("Mineshaft Explorer").equals("Посещение глацитовой шахты"),"commission label follows actual completion rule");
        check(db.translate("Introduction to the Crop Analyzer").equals("Знакомство с анализатором культур"),"station, not handheld plant tool");
        check(db.translate("Plant Diagnostics Tool").equals("Анализатор растений"),"handheld name retained");
        for(String tier:List.of("I","II","III","IV","V","VI","VII","VIII","IX","X"))for(boolean required:List.of(false,true)){
            var c=Component.empty();if(required)c.append(Component.literal("Requires: ").setStyle(BODY));c.append(Component.literal("Crop Analyzer Milestone").setStyle(TERM)).append(Component.literal(" ").setStyle(BODY)).append(Component.literal(tier).setStyle(NUMBER));
            String target=(required?"Требуется: ":"")+"Этап анализатора культур "+tier;check(db.translate(c.getString()).equals(target),"milestone plain "+c.getString()+" => "+db.translate(c.getString()));verify(c,target,tier);
        }
        for(String[] pair:new String[][]{{"Open 0 chests.","Откройте"},{"Loot 2 player corpses.","Обыщите"},{"Discover 2 Mineshaft.","Посетите"},{"Obtain 1 Wither_101.","Получите"},{"Slay 10 Maniacs in the Jungle.","Победите"}})check(!db.translate(pair[0]).startsWith(pair[1]),"closed shapes, no guessed place/identity");
        var custom=Path.of(args[0]).resolve("own");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Frozen Corpses\tМои тела\nCrop Analyzer Milestone\tМой этап\n");var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);check(!SkyRuClient.component(Component.literal("Loot 2 Frozen Corpses.")).getString().equals("Обыщите 2 замёрзших тела."),"semantic override");check(!SkyRuClient.component(Component.literal("Requires: Crop Analyzer Milestone III")).getString().equals("Требуется: Этап анализатора культур III"),"milestone override");flag("dictionary",db);
        System.out.println("PASS: commission expansion "+checks+" assertions; "+variants+" synthetic quota/tier variants, four action shapes + Maniacs, original numbers/styles/chat/overrides; no live game");
    }
}
