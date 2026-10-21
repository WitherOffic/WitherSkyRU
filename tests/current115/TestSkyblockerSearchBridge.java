import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Actual installed vendor bytecode, isolated metadata-only fixture: no mod/client entrypoints or network. */
public class TestSkyblockerSearchBridge {
    private static int checks;
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static void field(Class<?> type,String name,Object value)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static void main(String[] args)throws Exception{
        var knot=new Knot(EnvType.CLIENT);var loader=knot.init(new String[]{"--gameDir",Path.of(args[0]).toAbsolutePath().toString(),"--version","26.2"});
        var client=Class.forName("ru.wither.skyru.SkyRuClient",true,loader);
        var dictionary=Class.forName("ru.wither.skyru.OfflineDictionary",true,loader);
        var db=dictionary.getConstructor(Path.class).newInstance(Path.of(args[0]).resolve("test-config"));dictionary.getMethod("setRecordUnknown",boolean.class).invoke(db,false);
        field(client,"dictionary",db);field(client,"enabled",true);field(client,"onlyHypixel",false);
        var manager=Class.forName("de.hysky.skyblocker.skyblock.searchoverlay.SearchOverManager",true,loader);
        check(Arrays.stream(manager.getDeclaredMethods()).filter(m->m.getName().contains("witherskyru$russianOverlayName")).count()==1,"real handler transformed");
        var location=Class.forName("de.hysky.skyblocker.skyblock.searchoverlay.SearchOverManager$SearchLocation",true,loader);
        var suggest=manager.getDeclaredMethod("updateSuggestions",Set.class,int.class);suggest.setAccessible(true);
        var input=manager.getDeclaredField("search");input.setAccessible(true);
        var candidates=new LinkedHashSet<>(List.of("Diamond Pickaxe","Iron Pickaxe","Enchanted Wheat","Fishing Rod","Rod of Legends","Unknown ZX9921"));var saved=new LinkedHashSet<>(candidates);
        var ids=Map.of("Diamond Pickaxe","DIAMOND_PICKAXE","Iron Pickaxe","IRON_PICKAXE");field(manager,"namesToNeuId",new HashMap<>(ids));
        for(String place:List.of("AUCTION","BAZAAR")){
            field(manager,"location",location.getField(place).get(null));
            for(String query:List.of("ки","КИРКА","кирка алмазная","удочка","удо","зачарованная пшеница","pickaxe","Wheat","несуществующийZX9921")){
                input.set(null,query);var results=(String[])suggest.invoke(null,candidates,5);
                int expected=query.contains("алмазная")||query.contains("пшеница")||query.equals("Wheat")?1:query.startsWith("несуществующий")?0:2;
                check(results.length==expected,place+" "+query+" => "+Arrays.toString(results));
                check(input.get(null).equals(query),"typed text not rewritten");check(candidates.equals(saved),"native catalogue not mutated");
                check(Arrays.stream(results).allMatch(candidates::contains),"results are original English names");
                check(manager.getDeclaredField("namesToNeuId").trySetAccessible(),"ID field readable");
                for(int repeat=0;repeat<5;repeat++)check(Arrays.equals((String[])suggest.invoke(null,candidates,5),results),"repeat stable");
            }
        }
        // The native original-name -> NEU ID lookup used by item previews stays intact.
        var id=manager.getDeclaredMethod("getItemId",String.class);id.setAccessible(true);
        check(id.invoke(null,"Diamond Pickaxe").equals("DIAMOND_PICKAXE"),"native selected original resolves to unchanged ID");
        field(client,"enabled",false);input.set(null,"кирка");check(((String[])suggest.invoke(null,candidates,5)).length==0,"disabled unchanged native filter");
        input.set(null,"pickaxe");check(((String[])suggest.invoke(null,candidates,5)).length==2,"English works disabled");
        System.out.println("PASS: Actual Skyblocker 6.11.0+26.2 overlay "+checks+" assertions; Bazaar/Auction native method, English originals/IDs/input unchanged; fixture does not test network submission or full pack");
    }
}
