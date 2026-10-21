import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;

public class TestOverlaySearch {
    private static int checks;
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static void flag(String key,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,v);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("searchAliases",null);flag("enabled",true);flag("onlyHypixel",false);
        check(CompatibilityMixinPlugin.supportedSkyblockerVersion("6.10.4+26.1.2"),"inspected version");
        check(!CompatibilityMixinPlugin.supportedSkyblockerVersion("6.10.5+26.1.2"),"unknown version skipped");
        check(!CompatibilityMixinPlugin.supportedSkyblockerVersion("6.10.4+26.1.1"),"other Minecraft skipped");
        for(String[] pair:List.of(new String[]{"Diamond Pickaxe","ки"},new String[]{"Diamond Pickaxe","КИРКА"},new String[]{"Diamond Pickaxe","кирка алмазная"},new String[]{"Fishing Rod","удо"},new String[]{"Enchanted Wheat","зачарованная пшеница"},new String[]{"Blessed Advanced Gardening Hoe","мотыга"})){
            for(int repeat=0;repeat<5;repeat++)check(SkyRuClient.overlaySearchMatch(pair[0],pair[1]),"Russian suggestion: "+Arrays.toString(pair));
        }
        for(String query:List.of("pickaxe",""," ","несуществующийZX9921","кирка /команда","я".repeat(101)))check(!SkyRuClient.overlaySearchMatch("Diamond Pickaxe",query),"unknown/English/native query unchanged: "+query);
        var custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Diamond Pickaxe\tОсобая копалка\n");
        var own=new OfflineDictionary(custom);flag("dictionary",own);flag("searchAliases",null);check(SkyRuClient.overlaySearchMatch("Diamond Pickaxe","копалка"),"override alias");check(!SkyRuClient.overlaySearchMatch("Diamond Pickaxe","алмазная"),"override replaces alias");
        flag("enabled",false);check(!SkyRuClient.overlaySearchMatch("Diamond Pickaxe","копалка"),"disabled no supplement");
        System.out.println("PASS: Skyblocker overlay search "+checks+" assertions; Russian partial/multiword names, native English, overrides, disabled/version guards");
    }
}
