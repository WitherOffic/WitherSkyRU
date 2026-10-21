import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Exact visible labels only; regex/title selectors remain source metadata. */
public final class TestBagGuiLabels56{
    private static int checks;private static Method row,plain,styles;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static void labels(JsonElement e,Set<String> out){
        if(e.isJsonArray()){for(var v:e.getAsJsonArray())labels(v,out);return;}if(!e.isJsonObject())return;var o=e.getAsJsonObject();
        if(o.has("mode")&&o.get("mode").getAsString().equals("equals")&&o.has("name"))out.add(o.get("name").getAsString());
        if(o.has("type")&&o.get("type").getAsString().endsWith("title")&&o.has("title")){String s=o.get("title").getAsString();if(!s.contains(".*")&&!s.contains("(.")&&!s.contains("|"))out.add(s);}
        for(var f:o.entrySet())if(f.getValue().isJsonArray()||f.getValue().isJsonObject())labels(f.getValue(),out);
    }
    private static final String[][] EXPECTED={
        {"Your Bags","Ваши сумки"},{"Accessory Bag","Сумка аксессуаров"},{"Bazaar History","История Базара"},{"Bazaar Settings","Настройки Базара"},{"Bits","Биты"},{"Cancel","Отмена"},{"Click to disable!","Нажмите, чтобы выключить!"},{"Click to enable!","Нажмите, чтобы включить!"},{"Click to view!","Нажмите, чтобы посмотреть!"},{"Close","Закрыть"},{"Combat","Бой"},{"Confirm","Подтвердить"},{"Confirmations: Enabled!","Подтверждения: включены!"},{"Confirmations: OFF","Подтверждения: выключены"},{"Currently viewing!","Вы просматриваете этот раздел!"},{"Farming","Фермерство"},{"Fishing Bag","Сумка для рыбалки"},{"Go Back","Назад"},{"Information","Информация"},{"Insert inventory","Поместить содержимое инвентаря"},{"Manage Orders","Управление заказами"},{"Mining","Горное дело"},{"No Product Found","Товар не найден"},{"Oddities","Диковинки"},{"Pickup All","Забрать всё"},{"Potion Bag","Сумка зелий"},{"Previous Page","Предыдущая страница"},{"Quiver","Колчан"},{"Re-Center","Вернуться в центр"},{"Sack of Sacks","Сумка мешков"},{"Scroll Down","Прокрутить вниз"},{"Scroll Left","Прокрутить влево"},{"Scroll Right","Прокрутить вправо"},{"Scroll Up","Прокрутить вверх"},{"Search","Поиск"},{"Sell Inventory Now","Продать содержимое инвентаря"},{"Sell Item","Продать предмет"},{"Sell Sacks Now","Продать содержимое мешков"},{"Settings","Настройки"},{"SkyBlock Gems","Самоцветы SkyBlock"},{"Time Pocket","Карман времени"},{"To Bazaar","На Базар"},{"WARNING","ВНИМАНИЕ"},{"Woods & Fishes","Древесина и рыба"}};
    public static void main(String[] args)throws Exception{
        row=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);row.setAccessible(true);plain=TestSacks55.class.getDeclaredMethod("plain",Component.class);plain.setAccessible(true);styles=TestSacks55.class.getDeclaredMethod("styles",Component.class);styles.setAccessible(true);
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var raw=Files.readString(Path.of(args[1]).resolve("BAG_GUI_LABELS_0.15.56.json"));var capture=JsonParser.parseString(raw).getAsJsonObject();var observed=new TreeSet<String>();for(var r:capture.getAsJsonArray("records"))labels(r.getAsJsonObject().get("definition"),observed);
        var keys=new TreeSet<String>();for(var p:EXPECTED)keys.add(p[0]);check(observed.equals(keys)&&keys.size()==44,"44 exact cached labels, not regex selectors");
        for(var p:EXPECTED)for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){
            var c=(Component)row.invoke(null,p[0],mode,seed);var current=c;var seq=c.getVisualOrderText();
            for(int n=0;n<5;n++){current=SkyRuClient.component(current);seq=SkyRuClient.sequence(seq,false);check(plain.invoke(null,current).equals(p[1]),"component "+p[0]);check(AuditMiningStyles.plain(seq).equals(p[1]),"sequence "+p[0]);var a=(List<?>)styles.invoke(null,c);var b=(List<?>)styles.invoke(null,current);check(b.stream().allMatch(s->s.equals(a.getFirst())),"current style/events");}
            check(plain.invoke(null,c).equals(p[0]),"immutable source");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,c,null,channel,null);check(SkyRuClient.npcChat(msg)==msg,"whole chat");}
            var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed");
        }
        var dir=root.resolve("custom");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),"Sell Sacks Now\tМой текст\n");flag("dictionary",new OfflineDictionary(dir));check(SkyRuClient.component(Component.literal("Sell Sacks Now")).getString().equals("Мой текст"),"override");flag("dictionary",db);
        var f=RenderContext.class.getDeclaredField("CURRENT");f.setAccessible(true);@SuppressWarnings("unchecked")var local=(ThreadLocal<RenderContext.Snapshot>)f.get(null);var old=local.get();try{local.set(new RenderContext.Snapshot(true,false,false,false));for(String s:keys){var c=Component.literal(s);check(SkyRuClient.component(c)==c,"protected input snapshot");}}finally{if(old==null)local.remove();else local.set(old);}
        check(Files.readString(Path.of(args[1]).resolve("BAG_GUI_LABELS_0.15.56.json")).equals(raw),"cache metadata/IDs/conditions never changed");
        flag("enabled",false);var c=Component.literal("Search");check(SkyRuClient.component(c)==c,"disabled");
        System.out.println("PASS: bag GUI labels56 "+checks+" assertions;44 exact cached labels/3 definitions,4 layouts/5 fresh actions/5 repeats, input/chat/overrides;not dynamic titles/full tooltips/live GUI");
    }
}
