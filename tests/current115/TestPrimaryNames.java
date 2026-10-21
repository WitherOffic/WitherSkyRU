import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Closed public-name fixtures; synthetic reforges do not assert game legality. */
public class TestPrimaryNames {
    private static int checks;
    private static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,c)->{b.appendCodePoint(c);return true;});return b.toString();}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var fixtures=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("PRIMARY_NAME_REVIEW_56_60.json"))).getAsJsonObject();
        var cases=new ArrayList<String[]>();for(var raw:fixtures.getAsJsonArray("titles")){var r=raw.getAsJsonObject();cases.add(new String[]{r.get("source").getAsString(),r.get("target").getAsString(),r.get("gender").getAsString()});}
        for(var raw:fixtures.getAsJsonArray("gems")){var r=raw.getAsJsonObject();cases.add(new String[]{r.get("source").getAsString(),r.get("target").getAsString(),r.get("gender").getAsString()});cases.add(new String[]{r.get("iconSource").getAsString(),r.get("iconTarget").getAsString(),"skip"});}
        for(var raw:fixtures.getAsJsonArray("additional")){var r=raw.getAsJsonObject();cases.add(new String[]{r.get("source").getAsString(),r.get("target").getAsString(),"skip"});}
        Style title=Style.EMPTY.withColor(0x55ffff).withBold(false).withClickEvent(new ClickEvent.RunCommand("/inspect")),modifier=Style.EMPTY.withColor(0x55ff55).withBold(true);
        for(String[] row:cases)for(int repeat=0;repeat<5;repeat++){
            check(db.translateForSearch(row[0]).equals(row[1]),"search "+row[0]);
            check(db.translateForSearch(row[1]).equals(row[1]),"repeat target "+row[0]);
            var original=Component.literal(row[0]).setStyle(title);var result=TooltipTranslation.components(List.of(original,Component.literal("RARE")),false);
            check(result.getFirst().getString().equals(row[1]),"tooltip "+row[0]+" => "+result.getFirst().getString());
            check(result.get(1).getString().equals("RARE"),"English rarity");
            check(TooltipTranslation.components(List.of(original),true).getFirst().getString().equals(row[0]),"Shift");
            StringDecomposer.iterateFormatted(result.getFirst(),Style.EMPTY,(i,s,c)->{check(s.equals(title),"title style/actions");return true;});
            check(plain(SkyRuClient.sequence(FormattedCharSequence.forward(row[0],title))).equals(row[1]),"sequence");
            var message=new GuiMessage(0,original,null,GuiMessageSource.PLAYER,null);
            check(SkyRuClient.npcChat(message)==message,"whole player chat");
            if(!row[2].equals("skip"))for(String stars:List.of(""," ✪✪✪✪✪"," [10]")){
                String adj=switch(row[2]){case "f"->"Свирепая";case "n"->"Свирепое";case "p"->"Свирепые";default->"Свирепый";};
                var source=Component.literal("Fierce").setStyle(modifier).append(Component.literal(" ")).append(Component.literal(row[0]).setStyle(title)).append(Component.literal(stars));
                String expected=adj+" "+row[1]+stars;
                check(SkyRuClient.component(source).getString().equals(expected),"reforge component "+row[0]);
                check(db.translateForSearch("Fierce "+row[0]+stars).equals(expected),"reforge search "+row[0]);
                check(TooltipTranslation.components(List.of(source),true).getFirst().getString().equals(source.getString()),"reforged Shift");
            }
        }
        // All known modifiers, not just one adjective. These combinations are synthetics.
        Path project=Path.of(args[1]).getParent();var suffixes=new HashSet<String>();
        for(String line:Files.readAllLines(project.resolve("src/main/resources/data/witherskyru/reforge-placement.tsv")))if(!line.isBlank()&&!line.startsWith("#"))suffixes.add(line.split("\t")[0]);
        int compositions=0;
        for(String line:Files.readAllLines(project.resolve("src/main/resources/data/witherskyru/reforges.tsv"))){
            if(line.isBlank()||line.startsWith("#"))continue;var forms=line.split("\t");
            for(var raw:fixtures.getAsJsonArray("titles")){var row=raw.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("target").getAsString();int gender=switch(row.get("gender").getAsString()){case "f"->2;case "n"->3;case "p"->4;default->1;};
                String translated=suffixes.contains(forms[0])?target+" "+forms[gender]:forms[gender]+" "+target;
                for(String stars:List.of(""," ✪✪✪✪✪"," ★★★★★"," [10]"," ➊➋➌➍➎")){
                    check(db.translateForSearch(forms[0]+" "+source+stars).equals(translated+stars),"all-known modifier "+forms[0]+" "+source);compositions++;
                }
            }
        }
        Style sin=Style.EMPTY.withColor(0xaa0000).withBold(false).withClickEvent(new ClickEvent.RunCommand("/sin")),seeker=Style.EMPTY.withColor(0xaa00aa).withBold(false).withHoverEvent(new HoverEvent.ShowText(Component.literal("seeker")));
        for(String prefix:List.of("","Fierce "))for(String stars:List.of(""," ✪✪✪"," [10]"))for(int repeat=0;repeat<5;repeat++){
            var original=Component.literal(prefix).setStyle(modifier).append(Component.literal("Sin").setStyle(sin)).append(Component.literal("seeker Scythe").setStyle(seeker)).append(Component.literal(stars).setStyle(title));
            String expected=(prefix.isEmpty()?"":"Свирепая ")+"Коса искателя грехов"+stars;var translated=SkyRuClient.component(original);
            check(translated.getString().equals(expected),"primary compound colours "+translated.getString());
            check(SkyRuClient.component(translated).getString().equals(expected),"coloured repeat");
            int gap=prefix.isEmpty()?0:"Свирепая ".length();int[] at={0};StringDecomposer.iterateFormatted(translated,Style.EMPTY,(i,s,c)->{int n=at[0]++;Style wanted=n<gap?modifier:n<gap+"Коса искателя ".length()?seeker:n<gap+"Коса искателя грехов".length()?sin:title;check(s.equals(wanted),"semantic Sin/seeker/styles at"+n);return true;});
            check(TooltipTranslation.components(List.of(original),true).getFirst().getString().equals(original.getString()),"coloured Shift");
        }
        check(SkyRuClient.component(Component.literal("§4Sin§5seeker Scythe")).getString().equals("Коса искателя грехов"),"primary legacy");
        var ambiguous=Component.literal("Si").setStyle(sin).append(Component.literal("n").setStyle(seeker)).append(Component.literal("seeker Scythe").setStyle(seeker));
        check(SkyRuClient.component(ambiguous).getString().equals(ambiguous.getString()),"unknown split styles fail closed");
        Files.createDirectories(root.resolve("override"));Files.writeString(root.resolve("override/overrides.tsv"),"Sinseeker Scythe\tCUSTOM NAME\nCutlass\tCUSTOM SABRE\n");
        var custom=new OfflineDictionary(root.resolve("override"));custom.setRecordUnknown(false);flag("dictionary",custom);
        check(custom.translateForSearch("Cutlass").equals("CUSTOM SABRE"),"name override");
        var multicolour=Component.literal("Sin").setStyle(sin).append(Component.literal("seeker Scythe").setStyle(seeker));
        check(!SkyRuClient.component(multicolour).getString().contains("грехов"),"compound override not bypassed");
        System.out.println("PASS: primary names "+checks+" assertions;49 titles/60 gems+icons/two additional/known Sin colours/"+compositions+" synthetic modifier cases/five repeats/Shift/whole-chat/override guards; no live or legal-reforge claim");
    }
}
