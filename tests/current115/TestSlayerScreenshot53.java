import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import ru.wither.skyru.*;

/** Sixteen bounded actual records plus synthetic families; no game/action execution. */
public final class TestSlayerScreenshot53 {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    private static String plain(Component c){return AuditMiningStyles.plain(c.getVisualOrderText());}
    private static String clean(String s){return s.replaceAll("(?i)§[0-9a-fk-or]","");}
    private static List<Style> styles(Component c){var out=new ArrayList<Style>();c.getVisualOrderText().accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static int bodyStart(String s){return s.startsWith("[")&&s.length()>10&&s.charAt(3)==':'&&s.charAt(6)==':'?11:0;}
    private static String body(String s){return s.substring(bodyStart(s)).strip();}
    private static GuiMessage msg(Component c,GuiMessageSource channel){return new GuiMessage(9,c,null,channel,null);}
    private static void span(Component from,String old,Component to,String next,boolean exact){String a=plain(from),b=plain(to);int x=a.indexOf(old,bodyStart(a)),y=b.indexOf(next,bodyStart(b));check(x>=0&&y>=0,"missing span "+old+" => "+next);var ss=styles(from);var tt=styles(to);if(exact)for(int i=0;i<old.length();i++)check(ss.get(x+i).equals(tt.get(y+i)),"original glyph/action "+old);else check(ss.get(x).equals(tt.get(y)),"semantic style/action "+old);}
    private static Component row(String raw,int mode,int seed){
        var inherited=Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/original-slayer-"+seed)).withHoverEvent(new HoverEvent.ShowText(Component.literal("original hint "+seed))).withInsertion("frame "+seed);var c=Component.literal(raw).setStyle(inherited);if(mode==0)return c;
        String text=plain(c);var ss=styles(c);var out=Component.empty();
        for(int i=0;i<text.length();i++){
            Style s=ss.get(i);String[][] anchors={{"Revenant Horror","55ff55"},{"Maddox","ffaa00"},{"Zombie","ffff55"},{"XP","ff55ff"},{"Combat XP","ff5555"},{"HP","ff5555"}};
            for(var a:anchors){int at=text.indexOf(a[0],bodyStart(text));int end=at+a[0].length();if(at>=0&&(end==text.length()||!Character.isLetter(text.charAt(end)))&&i>=at&&i<end)s=s.withColor(Integer.parseInt(a[1],16)).withInsertion("semantic "+a[0]+"/"+seed);}
            if(Character.isDigit(text.charAt(i)))s=s.withColor(0x100000+i*0x101).withInsertion("digit "+i+"/"+seed);
            out.append(Component.literal(text.substring(i,i+1)).setStyle(s));
        }return out;
    }
    private static final Map<String,String> OBSERVED=Map.ofEntries(
        Map.entry("SLAYER BOSS! The Revenant Horror II spawned!","БОСС ОХОТЫ! Возрождённый ужас II появился!"),
        Map.entry("Your Slayer Kill gave you 80 HP healing for 7 seconds!","За убийство босса — лечение: 80 здоровья на 7 с!"),
        Map.entry("[SkyHanni] You have gained 5 Bits.","[SkyHanni] Вы получили 5 битов."),
        Map.entry("[Skyblocker] Slayer убит за 5.43s!","[Skyblocker] Босс побеждён за 5.43 с!"),
        Map.entry("NICE! SLAYER BOSS SLAIN!","ОТЛИЧНО! БОСС ПОБЕЖДЁН!"),
        Map.entry("» Talk to Maddox to claim your Zombie Slayer XP!","» Поговорите с Мэддоксом и заберите опыт охоты на зомби!"),
        Map.entry("✆ RING...","✆ ЗВОНОК..."),Map.entry("✆ RING... RING...","✆ ЗВОНОК... ЗВОНОК..."),Map.entry("✆ RING... RING... RING...","✆ ЗВОНОК... ЗВОНОК... ЗВОНОК..."),
        Map.entry("[NPC] Maddox: ✆ Someone answers!","[NPC] Maddox: ✆ Кто-то ответил!"),
        Map.entry("[SkyHanni] Slayer quest took 38s to complete.","[SkyHanni] Задание охоты выполнено за 38 с."),
        Map.entry("[Skyblocker] Slayer убит за 10.26s!","[Skyblocker] Босс побеждён за 10.26 с!"),
        Map.entry("SLAYER QUEST COMPLETE!","ЗАДАНИЕ ОХОТЫ ЗАВЕРШЕНО!"),
        Map.entry("Zombie Slayer LVL 3 - Next LVL in 445 XP!","Охота на зомби, ур. 3 — до следующего уровня: 445 опыта!"),
        Map.entry("SLAYER QUEST STARTED!","ЗАДАНИЕ ОХОТЫ НАЧАТО!"),
        Map.entry("» Slay 1,000 Combat XP worth of Zombies.","» Наберите 1,000 опыта боя, убивая зомби."));
    private static String expected(String raw){String source=clean(raw),b=body(source),target=OBSERVED.get(b);check(target!=null,"reviewed observed body "+b);int start=bodyStart(source);while(start<source.length()&&source.charAt(start)==' ')start++;int end=source.length();while(end>start&&source.charAt(end-1)==' ')end--;return source.substring(0,start)+target+source.substring(end);}
    private static Component translated(Component c,String target,GuiMessageSource channel){
        String old=plain(c);var m=msg(c,channel);var result=SkyRuClient.npcChat(m);check(plain(result.content()).equals(target),old+" => "+plain(result.content()));check(result.addedTime()==9,"original time metadata");check(plain(c).equals(old),"original component immutable");
        for(int i=0;i<5;i++)check(SkyRuClient.npcChat(msg(result.content(),channel)).content()==result.content(),"idempotent queue translation");
        var player=msg(c,GuiMessageSource.PLAYER);check(SkyRuClient.npcChat(player)==player,"whole PLAYER identity");var signed=new GuiMessage(9,c,new MessageSignature(new byte[256]),channel,null);check(SkyRuClient.npcChat(signed)==signed,"whole signed identity");
        return result.content();
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("chatNotifications",new ChatNotificationTranslator(db));check(new ChatNotificationTranslator(db).slayerNotificationFamilyCount()==12,"twelve new notice families, NPC separately");
        var records=JsonParser.parseString(Files.readString(Path.of(args[1]).resolve("SLAYER_OBSERVED_0.15.53.json"))).getAsJsonObject().getAsJsonArray("records");check(records.size()==16,"16 exact observed messages");
        for(var raw:records)for(int mode=0;mode<2;mode++)for(int seed=0;seed<5;seed++){
            String source=raw.getAsJsonObject().get("source_legacy").getAsString();var c=row(source,mode,seed);String target=expected(source);var result=translated(c,target,GuiMessageSource.SYSTEM_SERVER);String visible=plain(c),b=body(visible);
            // Every original digit, timestamp, arrow and phone glyph is copied in place,
            // or into an unchanged numeric field; no inferred kill count from XP.
            var digits=java.util.regex.Pattern.compile("[0-9][0-9,.]*").matcher(visible.substring(bodyStart(visible)));while(digits.find())span(c,digits.group(),result,digits.group(),true);
            if(visible.startsWith("[03:")){var ss=styles(c);var tt=styles(result);for(int i=0;i<11;i++)check(ss.get(i).equals(tt.get(i)),"timestamp glyph style");}
            if(b.contains("Revenant Horror"))span(c,"Revenant Horror",result,"Возрождённый ужас",false);
            if(b.contains("Combat XP"))span(c,"Combat XP",result,"опыта боя",false);
            if(b.startsWith("» Talk")){span(c,"Maddox",result,"Мэддоксом",false);span(c,"Zombie",result,"зомби",false);span(c,"XP",result,"опыт",false);}
            if(b.startsWith("[NPC] Maddox"))span(c,"Maddox",result,"Maddox",true);
            if(b.contains(" HP "))span(c,"HP",result,"здоровья",false);
            boolean mod=b.startsWith("[SkyHanni]")||b.startsWith("[Skyblocker]");var client=msg(c,GuiMessageSource.SYSTEM_CLIENT);if(mod)translated(c,target,GuiMessageSource.SYSTEM_CLIENT);else check(SkyRuClient.npcChat(client)==client,"server-only shape rejected from local channel");
        }
        String[][] types={{"Zombie","зомби","Zombies"},{"Spider","пауков","Spiders"},{"Wolf","волков","Wolves"},{"Enderman","эндерменов","Endermen"},{"Blaze","всполохов","Blazes"},{"Vampire","вампиров","Vampires"}};
        for(var t:types)for(int seed=0;seed<5;seed++){
            translated(row("» Talk to Maddox to claim your "+t[0]+" Slayer XP!",1,seed),"» Поговорите с Мэддоксом и заберите опыт охоты на "+t[1]+"!",GuiMessageSource.SYSTEM_SERVER);
            translated(row(t[0]+" Slayer LVL 3 - Next LVL 4 in 1,000 XP!",0,seed),"Охота на "+t[1]+", ур. 3 — до ур. 4: 1,000 опыта!",GuiMessageSource.SYSTEM_SERVER);
            translated(row("» Slay 1,000 Combat XP worth of "+t[2]+".",1,seed),"» Наберите 1,000 опыта боя, убивая "+t[1]+".",GuiMessageSource.SYSTEM_SERVER);
        }
        String[][] bosses={{"Revenant Horror","Возрождённый ужас"},{"Tarantula Broodfather","Прародитель тарантулов"},{"Sven Packmaster","Свен — вожак стаи"},{"Voidgloom Seraph","Серафим мрачной пустоты"},{"Inferno Demonlord","Повелитель демонов Инферно"},{"Riftstalker Bloodfiend","Кровавый демон Разлома"}};
        for(var b:bosses)for(String tier:List.of("I","II","IV","V"))translated(row("SLAYER BOSS! The "+b[0]+" "+tier+" spawned!",0,2),"БОСС ОХОТЫ! "+b[1]+" "+tier+" появился!",GuiMessageSource.SYSTEM_SERVER);
        for(String[] pair:new String[][]{{"1","бит"},{"2","бита"},{"11","битов"},{"101","бит"},{"1,002","бита"}})translated(row("[SkyHanni] You have gained "+pair[0]+" Bits.",1,4),"[SkyHanni] Вы получили "+pair[0]+" "+pair[1]+".",GuiMessageSource.SYSTEM_CLIENT);
        for(String bad:List.of("SLAYER BOSS! The Unknown II spawned!","SLAYER BOSS! The Revenant Horror VX spawned!","Zombie Slayer LVL 3 - Next LVL 5 in 445 XP!","» Slay NaN Combat XP worth of Zombies.","[SkyHanni] Slayer quest took 1e3s to complete.","✆ RING... RING... RING... RING...","UnknownPlayer: SLAYER QUEST COMPLETE!","[SkyHanni] You have gained something Bits.")){var m=msg(Component.literal(bad),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(m)==m,"unknown/malformed original "+bad);}
        var opaque=msg(Component.translatable("SLAYER QUEST COMPLETE!"),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(opaque)==opaque,"non-literal contents unchanged");
        var mixed=Component.empty().append(Component.literal("SLAYER ").withColor(0x55ff55)).append(Component.literal("QUEST COMPLETE!").withColor(0xff5555));var mixedMessage=msg(mixed,GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(mixedMessage)==mixedMessage,"unknown mixed style inside replaced semantic field unchanged");
        for(String key:List.of("Slayer","Revenant Horror","Combat XP","Maddox","Bits","XP","[NPC] Maddox: ✆ Someone answers!")){
            var dir=root.resolve("override-"+checks);Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),key+"\tМой текст\n");var own=new OfflineDictionary(dir);flag("dictionary",own);flag("chatNotifications",new ChatNotificationTranslator(own));
            String source=switch(key){case "Revenant Horror"->"SLAYER BOSS! The Revenant Horror II spawned!";case "Combat XP"->"» Slay 1,000 Combat XP worth of Zombies.";case "Maddox","XP"->"» Talk to Maddox to claim your Zombie Slayer XP!";case "Bits"->"[SkyHanni] You have gained 5 Bits.";case "[NPC] Maddox: ✆ Someone answers!"->key;default->"SLAYER QUEST COMPLETE!";};var m=msg(Component.literal(source),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(m)==m,"override yields "+key);
        }
        flag("dictionary",db);flag("chatNotifications",new ChatNotificationTranslator(db));flag("translateNpcChat",false);var npc=msg(Component.literal("[NPC] Maddox: ✆ Someone answers!"),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(npc)==npc,"NPC toggle");flag("translateNpcChat",true);flag("translateGameChat",false);var game=msg(Component.literal("SLAYER QUEST COMPLETE!"),GuiMessageSource.SYSTEM_SERVER);check(SkyRuClient.npcChat(game)==game,"game toggle");flag("enabled",false);check(SkyRuClient.npcChat(npc)==npc,"disabled identity");
        check(UserInputText.editingFrames(List.of("net.minecraft.client.gui.components.EditBox#extractWidgetRenderState")),"input original");
        System.out.println("PASS: Slayer screenshot53 "+checks+" assertions;16 observed receipts/12 notice families+Maddox/known synthetic types/tiers;values/fresh styles/actions/overrides/channel/toggles;not all Slayer/live game");
    }
}
