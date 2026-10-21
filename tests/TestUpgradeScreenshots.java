import java.nio.file.*;
import java.io.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

/** Screenshot reproductions plus synthetic variants, never live server packets. */
public class TestUpgradeScreenshots {
    private static int checks;
    private static NpcDialogueTranslator retainedNpc;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff);
    private static final Style TERM=Style.EMPTY.withColor(0x55ffff).withClickEvent(new ClickEvent.RunCommand("/original-term"));
    private static final Style VALUE=Style.EMPTY.withColor(0xffff55).withInsertion("original value");
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String key,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,v);}
    private static GuiMessage msg(Component c,GuiMessageSource channel){return new GuiMessage(0,c,null,channel,null);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(FormattedCharSequence s){var out=new ArrayList<Style>();s.accept((i,t,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(t);return true;});return out;}
    private static void span(Component c,String s,Style wanted){int at=c.getString().indexOf(s);check(at>=0,"missing "+s);var ss=styles(c.getVisualOrderText());for(int i=at;i<at+s.length();i++)check(ss.get(i).equals(wanted),"style/action "+s);}
    private static void ui(Component c,String target){
        check(SkyRuClient.component(c).getString().equals(target),"component "+c.getString()+" => "+SkyRuClient.component(c).getString());
        var s=SkyRuClient.sequence(c.getVisualOrderText(),true);check(plain(s).equals(target),"HUD "+c.getString()+" => "+plain(s));
        for(int n=0;n<5;n++){s=SkyRuClient.sequence(s,true);check(plain(s).equals(target),"HUD repeat "+c.getString()+" => "+plain(s));}
        var player=msg(c,GuiMessageSource.PLAYER);check(SkyRuClient.npcChat(player)==player,"whole player text protected");
    }
    private static Component server(Component c,String target){
        String originalText=c.getString();
        // Seven frozen pet-specific utterances retain their already verified wording.
        if(c.getString().contains("[NPC] Kat: ")){
            var retained=retainedNpc.translate(c);
            if(retained!=c)target=retained.getString();
        }
        var original=msg(c,GuiMessageSource.SYSTEM_SERVER);var result=SkyRuClient.npcChat(original).content();
        check(result.getString().equals(target),c.getString()+" => "+result.getString());
        check(c.getString().equals(originalText),"immutable original server component");
        for(int n=0;n<5;n++)check(SkyRuClient.npcChat(msg(result,GuiMessageSource.SYSTEM_SERVER)).content()==result,"server repeat");
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var p=msg(c,channel);check(SkyRuClient.npcChat(p)==p,"channel protection");}
        var signed=new GuiMessage(0,c,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protection");return result;
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("chatNotifications",new ChatNotificationTranslator(db));
        retainedNpc=new NpcDialogueTranslator();flag("npcDialogues",retainedNpc);flag("translateNpcChat",true);flag("translateGameChat",true);
        int pets=0,kat=0,levels=0;
        try(var reader=new BufferedReader(new InputStreamReader(TestUpgradeScreenshots.class.getResourceAsStream("/data/witherskyru/search-item-names.txt"),java.nio.charset.StandardCharsets.UTF_8))){
            for(String line;(line=reader.readLine())!=null;){
                if(!line.startsWith("[Lvl {LVL}] ")&&!line.startsWith("[Lvl 100] "))continue;
                int end=line.indexOf("] ")+2;String pet=line.substring(end),translated=db.translateForSearch(line);
                check(translated.startsWith("[Ур. "),"known pet title "+line);String name=translated.substring(translated.indexOf("] ")+2);pets++;
                var card=Component.empty().append(Component.literal("Pet: ").setStyle(BODY)).append(Component.literal(pet).setStyle(TERM));ui(card,"Питомец: "+name);span(SkyRuClient.component(card),name,TERM);
                for(String rarity:List.of("UNCOMMON","RARE","EPIC","LEGENDARY","MYTHIC"))for(String suffix:List.of(""," (x17)"," (×2)")){
                    for(String[] shape:new String[][]{
                        {"I was able to upgrade your pet "," to ",".","Ваш питомец «","» улучшен до редкости ","."},
                        {"I'll get your "," upgraded to "," in no time!","Я скоро улучшу питомца «","» до редкости ","!"}}){
                        var c=Component.empty().append(Component.literal("[22:18:12] [NPC] Kat: "+shape[0]).setStyle(BODY)).append(Component.literal(pet).setStyle(TERM)).append(Component.literal(shape[1]).setStyle(BODY)).append(Component.literal(rarity).setStyle(VALUE)).append(Component.literal(shape[2]+suffix).setStyle(BODY));
                        var result=server(c,"[22:18:12] [NPC] Kat: "+shape[3]+name+shape[4]+rarity+shape[5]+suffix);span(result,name,TERM);span(result,rarity,VALUE);kat++;
                    }
                }
                var care=Component.empty().append(Component.literal("[NPC] Kat: I'm currently taking care of your ").setStyle(BODY)).append(Component.literal(pet).setStyle(TERM)).append(Component.literal("!").setStyle(BODY));server(care,"[NPC] Kat: Я сейчас занимаюсь питомцем «"+name+"»!");kat++;
            }
        }
        check(pets==90,"90 known pet identities, not 90 Kat-compatible upgrade paths: "+pets);
        ui(Component.literal("Pet: Silverfish"),"Питомец: Чешуйница");ui(Component.literal("Pet: Rabbit"),"Питомец: Кролик");
        for(String[] pair:new String[][]{{"Village","Деревня"},{"Normal","Обычный режим"},{"Gems: 75","Самоцветы: 75"},{"Talk to Susan","Поговорите с Сьюзан"},{"- Benediction","- Благословение"},{"- Mining Fiesta","- Горнодобывающая фиеста"},{"Jacob's Contest","Состязание Jacob"},{"Updating ranking...","Обновление места…"},{"Plot - Cactus","Участок - Кактус"},{"Sawdust: 41,904 (+10%)","Опилки: 41,904 (+10%)"}})ui(Component.literal(pair[0]).setStyle(BODY),pair[1]);
        var talk=Component.empty().append(Component.literal("Talk to ").setStyle(BODY)).append(Component.literal("Susan").setStyle(TERM));ui(talk,"Поговорите с Сьюзан");span(SkyRuClient.component(talk),"Сьюзан",TERM);
        for(String count:List.of("0","4.43k","1,234","1.05M")){
            var c=Component.empty().append(Component.literal("Collected ").setStyle(BODY)).append(Component.literal(count).setStyle(VALUE));ui(c,"Собрано "+count);span(SkyRuClient.component(c),count,VALUE);
        }
        for(String tool:List.of("Cactus Knife Mk. I","Cactus Knife Mk. II","Cactus Knife Mk. III","Euclid's Wheat Sickle Mk. III","Blessed Advanced Gardening Hoe","Diamond Pickaxe"))for(String level:List.of("1","7","100"))for(String suffix:List.of(""," (x17)"," (×2)")){
            var c=Component.empty().append(Component.literal("[22:24:12] ").setStyle(BODY)).append(Component.literal("LEVEL UP!").setStyle(TERM)).append(Component.literal(" Your ").setStyle(BODY)).append(Component.literal(tool).setStyle(TERM)).append(Component.literal(" just leveled up to Level ").setStyle(BODY)).append(Component.literal(level).setStyle(VALUE)).append(Component.literal("!"+suffix).setStyle(BODY));
            var result=server(c,"[22:24:12] УРОВЕНЬ ПОВЫШЕН! Предмет «"+db.translateForSearch(tool)+"» достиг уровня "+level+"!"+suffix);
            int at=result.getString().indexOf("достиг уровня ")+"достиг уровня ".length();var ss=styles(result.getVisualOrderText());
            for(int i=at;i<at+level.length();i++)check(ss.get(i).equals(VALUE),"actual item level style/action");levels++;
        }
        for(String[] pair:new String[][]{{"Normal","Обычный"},{"- Mining Fiesta","- Праздник добычи"},{"- Benediction","- Благослов."},{"Gems: 75","Камни: 75"},{"Updating ranking...","Пересчёт места…"},{"Pet: Silverfish","Пит.: Чешуйница"},{"Pet: Rabbit","Пит.: Кролик"}}){var s=Component.literal(pair[0]).getVisualOrderText();check(plain(SkyRuClient.narrowHud(s,SkyRuClient.sequence(s,true))).equals(pair[1]),"narrow fallback "+pair[0]);}
        var split=Component.empty().append(Component.literal("- Mining ").setStyle(BODY)).append(Component.literal("Fiesta").setStyle(TERM));var narrow=SkyRuClient.narrowHud(split.getVisualOrderText(),SkyRuClient.sequence(split.getVisualOrderText(),true));check(plain(narrow).equals("- Праздник добычи"),"split narrow");check(styles(narrow).getLast().equals(TERM),"narrow event action");
        for(String source:List.of("[NPC] Kat: I was able to upgrade your pet Wither_101 to EPIC.","[NPC] Kat: I was able to upgrade your pet Mithril Golem to epic.","[NPC] Kat: I was able to upgrade your pet Mithril Golem to COMMON.","LEVEL UP! Your Wither_101 just leveled up to Level 7!","LEVEL UP! Your Diamond Pickaxe just leveled up to Level 0!","pickaxe mining speed Wither_101")){var c=msg(Component.literal(source),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(c)==c,"unknown/malformed untouched");}
        for(String[] p:new String[][]{{"[NPC] Kat: I was able to upgrade your pet Mithril Golem to EPIC.","Mithril Golem"},{"[NPC] Kat: I was able to upgrade your pet Rabbit to EPIC.","Rabbit"},{"[NPC] Kat: I was able to upgrade your pet Rabbit to EPIC.","[NPC] Kat: I was able to upgrade your pet Rabbit to EPIC."},{"LEVEL UP! Your Cactus Knife Mk. I just leveled up to Level 7!","Cactus Knife Mk. I"}}){var dir=Path.of(args[0]).resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),p[1]+"\tМой термин\n");var own=new OfflineDictionary(dir);flag("dictionary",own);flag("chatNotifications",new ChatNotificationTranslator(own));var c=msg(Component.literal(p[0]),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(c)==c,"semantic/full override, including retained exact NPC line");}
        for(String[] p:new String[][]{{"Gems: 75","Gems:","Мои камни: 75"},{"Pet: Silverfish","[Lvl {LVL}] Silverfish","Питомец: Моя чешуйница"}}){var dir=Path.of(args[0]).resolve("hud-override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),p[1]+"\t"+(p[1].startsWith("[Lvl")?"[Ур. {LVL}] Моя чешуйница":"Мои камни:")+"\n");var own=new OfflineDictionary(dir);flag("dictionary",own);var s=Component.literal(p[0]).getVisualOrderText();var translated=SkyRuClient.sequence(s,true);if(p[0].startsWith("Gems"))check(plain(translated).equals(p[2]),"HUD label override");check(SkyRuClient.narrowHud(s,translated)==translated,"custom HUD never shortened");}
        flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));
        flag("translateNpcChat",false);var c=msg(Component.literal("[NPC] Kat: I was able to upgrade your pet Mithril Golem to EPIC."),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(c)==c,"NPC toggle");flag("translateNpcChat",true);
        flag("translateGameChat",false);c=msg(Component.literal("LEVEL UP! Your Cactus Knife Mk. I just leveled up to Level 7!"),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(c)==c,"notice toggle");flag("translateGameChat",true);
        var brand=Component.literal("SKYBLOCK").getVisualOrderText();check(SkyRuClient.sequence(brand,true)==brand,"original brand");
        System.out.println("PASS: upgrade screenshots "+checks+" assertions; "+pets+" pet identities, "+kat+" synthetic Kat forms, "+levels+" item-level forms; no live game");
    }
}
