import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;

/** Rank tooltip paragraphs and labels from screenshot eleven in the user's Google Doc. */
public class TestRankProse {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("original body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original term")));
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original-value"));
    private static final String[][] CASES={{"MVP++ contains every perk from VIP,\nVIP+, MVP, MVP+, and then more!","MVP++ включает все привилегии рангов VIP,\nVIP+, MVP и MVP+, а также новые бонусы!"},{"Click the book below to view all\nHypixel perks for this rank!","Нажмите на книгу ниже, чтобы увидеть все\nпривилегии этого ранга на Hypixel!"}};
    private static final String[][] EN={{"MVP++","VIP+","MVP+","MVP","VIP"},{"Hypixel"}};
    private static final String[][] RU={{"MVP++","VIP+","MVP+","MVP","VIP"},{"Hypixel"}};
    private static final String[][] NUM={{},{}};
    private static final String[][] NUM_RU={{},{}};
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String text(List<Component> rows){return String.join("\n",rows.stream().map(Component::getString).toList());}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Component> source(int n){var result=new ArrayList<Component>();
        for(String row:CASES[n][0].split("\n")){
            var c=Component.empty();int at=0;
            while(at<row.length()){
                String found=null;Style style=BODY;
                for(String t:EN[n])if(row.startsWith(t,at)){found=t;style=TERM;break;}
                for(String t:NUM[n])if(row.startsWith(t,at)){found=t;style=VALUE;break;}
                if(found==null)found=row.substring(at,at+1);
                c.append(Component.literal(found).setStyle(style));at+=found.length();
            }result.add(c);
        }return result;
    }
    private static void styles(List<Component> rows,int n){
        for(var row:rows){String line=row.getString();var expected=new ArrayList<Style>(Collections.nCopies(line.length(),BODY));
            for(String t:RU[n]){int at=-1;while((at=line.indexOf(t,at+1))>=0)for(int i=at;i<at+t.length();i++)expected.set(i,TERM);}
            for(String t:NUM_RU[n]){int at=-1;while((at=line.indexOf(t,at+1))>=0)for(int i=at;i<at+t.length();i++)expected.set(i,VALUE);}
            int[] at={0};StringDecomposer.iterateFormatted(row,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)check(s.equals(expected.get(at[0]++)),"style/action: "+line);return true;});
        }
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(int n=0;n<CASES.length;n++){
            var original=source(n);var result=TooltipTranslation.components(original,false);
            for(int repeat=0;repeat<5;repeat++){
                check(text(result).equals(CASES[n][1]),"whole paragraph: "+text(result));styles(result,n);
                var seq=TooltipTranslation.styled(original.stream().map(Component::getVisualOrderText).toList(),false);
                check(String.join("\n",seq.stream().map(TestRankProse::plain).toList()).equals(CASES[n][1]),"sequence paragraph");
                result=TooltipTranslation.components(result,false);
            }
            check(text(original).equals(CASES[n][0]),"source immutable");
            check(db.translate(CASES[n][0]).equals(CASES[n][1]),"plain full paragraph: "+db.translate(CASES[n][0]));
            var uniform=Arrays.stream(CASES[n][0].split("\n")).map(Component::literal).map(c->(Component)c).toList();
            check(text(TooltipTranslation.components(uniform,false)).equals(CASES[n][1]),"uniform paragraph");
            var card=new ArrayList<Component>();card.add(Component.literal("Dungeon Orb"));card.addAll(original);card.add(Component.literal("LEGENDARY DUNGEON ITEM"));
            var shifted=TooltipTranslation.components(card,true);check(shifted.getFirst().getString().equals("Dungeon Orb"),"Shift-original title");
            check(text(shifted.subList(1,1+original.size())).equals(CASES[n][1]),"Shift prose");check(shifted.getLast().getString().equals("LEGENDARY ПРЕДМЕТ ПОДЗЕМЕЛИЙ"),"English rarity");
            var custom=Path.of(args[0]).resolve("custom-"+n);Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),CASES[n][0].split("\n")[0]+"\tМоя фраза\n");
            var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);
            check(text(TooltipTranslation.components(uniform,false)).contains("Моя фраза"),"uniform override wins");
            check(!text(TooltipTranslation.components(original,false)).equals(CASES[n][1]),"semantic renderer yields to override");flag("dictionary",db);
        }
        flag("enabled",false);check(text(TooltipTranslation.components(source(0),false)).equals(CASES[0][0]),"disabled unchanged");

        flag("enabled",true);
        for(String[] pair:new String[][]{{"Chat Prefix: [MVP++]","Префикс в чате: [MVP++]"},{"Seasonal Bundle: Full","Сезонный набор: Полный"},{"Max Guests: 7","Лимит гостей: 7"},{"Cosmetic Skill Multiplier: 3x","Множитель косметических навыков: 3x"},{"Equipment Wardrobe Slots: 9","Ячейки гардероба снаряжения: 9"},{"Loadout Slots: 18","Ячейки наборов: 18"},{"Profile Slots: 4","Слоты профилей: 4"},{"Monthly Gems: 500","Самоцветы в месяц: 500"},{"✦ Access to Alpha Tests","✦ Доступ к альфа-тестированию"},{"and more...","и многое другое…"},{"Click to get store link!","Нажмите, чтобы получить ссылку на магазин!"}}){
            int at=pair[0].contains(": ")?pair[0].indexOf(": ")+2:pair[0].startsWith("✦ ")?2:pair[0].length();
            var source=Component.empty().append(Component.literal(pair[0].substring(0,at)).setStyle(BODY)).append(Component.literal(pair[0].substring(at)).setStyle(VALUE));
            var result=SkyRuClient.component(source);
            check(result.getString().equals(pair[1]),pair[0]+" => "+result.getString());
            check(db.translate(pair[0]).equals(pair[1]),"plain rank label: "+db.translate(pair[0]));
            check(plain(SkyRuClient.sequence(source.getVisualOrderText())).equals(pair[1]),"rank sequence");
            if(at<pair[0].length()){
                int target=pair[1].contains(": ")?pair[1].indexOf(": ")+2:2;int[] index={0};
                StringDecomposer.iterateFormatted(result,Style.EMPTY,(i,style,cp)->{check(style.equals(index[0]++<target?BODY:VALUE),"original rank value style/action");return true;});
            }
            for(int n=0;n<5;n++){result=SkyRuClient.component(result);check(result.getString().equals(pair[1]),"rank five repeats");}
            var message=new net.minecraft.client.multiplayer.chat.GuiMessage(1,source,null,net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER,null);
            check(SkyRuClient.npcChat(message)==message,"whole rank-like player message unchanged");
        }
        for(String number:List.of("0","7","7%","9","18","500","99999","3x","3.5x")){
            String text="Max Guests: "+number,target="Лимит гостей: "+number;
            check(db.translate(text).equals(target),"synthetic original rank numeric value");
        }
        for(int n=0;n<CASES.length;n++){
            var legacy=new ArrayList<Component>();
            for(var c:source(n)){
                var encoded=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,style,cp)->{encoded.append(style.equals(TERM)?"§b":style.equals(VALUE)?"§a":"§7").appendCodePoint(cp);return true;});
                legacy.add(Component.literal(encoded.toString()));
            }
            check(text(TooltipTranslation.components(legacy,false)).equals(CASES[n][1]),"rank legacy paragraph");
        }
        var ownPath=Path.of(args[0]).resolve("numeric-override");Files.createDirectories(ownPath);Files.writeString(ownPath.resolve("overrides.tsv"),"Max Guests:\tМой лимит:\n");
        var own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);flag("dictionary",own);
        check(!SkyRuClient.component(Component.literal("Max Guests: 7")).getString().equals("Лимит гостей: 7"),"rank label override wins");
        flag("dictionary",db);

        System.out.println("PASS: Rank prose "+checks+" assertions; two closed paragraphs and rank labels, five repeats, semantic events/numbers, Shift, overrides, English rarity");
    }
}
