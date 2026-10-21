import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

public class TestMinionLabels {
    private static int checks;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)a.add(s);return true;});return a;}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var cards=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("item-context.json"))).getAsJsonArray();var families=new TreeSet<String>();int rows=0;
        for(var card:cards){String name=card.getAsJsonObject().get("name").getAsString();if(!name.matches("[A-Za-z ]+ Minion [IVX]+"))continue;rows++;String family=name.substring(0,name.lastIndexOf(' ')),level=name.substring(name.lastIndexOf(' ')+1);families.add(family);
            String first=db.translate(family+" I");check(first.endsWith(" I")&&first.matches(".*[А-Яа-яЁё].*"),"canonical Russian family: "+name);String target=first.substring(0,first.length()-1)+level;
            check(db.translate(name).equals(target),"actual minion row: "+name+" -> "+db.translate(name));check(SkyRuClient.component(Component.literal(name)).getString().equals(target),"actual component");
            var tooltip=TooltipTranslation.components(List.of(Component.literal(name),Component.literal("RARE")),true);check(tooltip.getFirst().getString().equals(name),"Shift original title");check(tooltip.get(1).getString().equals("RARE"),"English rarity");
        }
        check(rows==719&&families.size()==61,"frozen minion title scope: "+rows+" / "+families.size());Style label=Style.EMPTY.withColor(0xffaa00).withClickEvent(new ClickEvent.RunCommand("/minion-original"));
        for(String family:families)for(int level=1;level<=12;level++){
            String number=Integer.toString(level),first=db.translate(family+" I"),target=first.substring(0,first.length()-1)+number;var c=Component.literal(family+" ").setStyle(label);
            for(int k=0;k<number.length();k++)c.append(Component.literal(number.substring(k,k+1)).setStyle(Style.EMPTY.withColor(0x550000+k*16).withInsertion("tier "+k)));
            String source=c.getString();check(db.translate(source).equals(target),"synthetic Arabic tier");var next=SkyRuClient.minionNameTag(c,false);
            for(int repeat=0;repeat<5;repeat++){check(next.getString().equals(target),"nameTag: "+next.getString());next=SkyRuClient.minionNameTag(next,false);}
            check(plain(SkyRuClient.sequence(c.getVisualOrderText())).equals(target),"FCS");var a=styles(c);var z=styles(next);
            for(int k=0;k<number.length();k++)check(a.get(source.length()-number.length()+k).equals(z.get(target.length()-number.length()+k)),"per-glyph tier unchanged");
            check(z.getFirst().equals(label),"label action preserved");check(c.getString().equals(source),"immutable nameTag");check(SkyRuClient.minionNameTag(c,true)==c,"player entity untouched");
            var msg=new GuiMessage(1,c,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(msg)==msg,"whole player message untouched");
        }
        check(SkyRuClient.minionNameTag(Component.literal("§6Coal Minion §e6"),false).getString().equals("Миньон угля 6"),"legacy world label");
        for(String name:List.of("Wither_101","Blacksmith","Foo Minion 6","Coal Minion 0","Coal Minion 13","Coal Minion XIV","Coal Minion IIV")){var c=Component.literal(name);check(SkyRuClient.minionNameTag(c,false)==c,"unknown NPC/name/level unchanged");}
        for(String key:List.of("Coal Minion 6","Coal Minion","Coal Minion I")){
            var dir=Path.of(args[0]).resolve("custom-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой миньон\n");var own=new OfflineDictionary(dir);flag("dictionary",own);
            check(own.translate(key).equals("Мой миньон"),"override final");check(SkyRuClient.minionNameTag(Component.literal("Coal Minion 6"),false).getString().equals(own.translate("Coal Minion 6")),"world renderer yields to override policy");
        }
        flag("dictionary",db);flag("enabled",false);var c=Component.literal("Coal Minion 6");check(SkyRuClient.minionNameTag(c,false)==c,"disabled");
        System.out.println("PASS: minion labels "+checks+" assertions; 719 real inventory titles, 61 families, 732 synthetic Arabic-tier world labels, original player entities, Shift, rarity, legacy colours and overrides");
    }
}
