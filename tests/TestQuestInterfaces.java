import java.nio.file.*;
import java.util.*;
import java.io.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

/** Current-source interface sample, not a census of all 219 chapter tasks or all NPCs. */
public class TestQuestInterfaces {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xffffff).withClickEvent(new ClickEvent.RunCommand("/commissions"));
    private static final Style NUMBER=Style.EMPTY.withColor(0x55ff55).withInsertion("original amount");
    private static final Style MOB=Style.EMPTY.withColor(0x55ffff).withHoverEvent(new HoverEvent.ShowText(Component.literal("original mob")));
    private static final Style AREA=Style.EMPTY.withColor(0xffff55).withInsertion("original area");
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static List<Style> styles(Component source){var list=new ArrayList<Style>();StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)list.add(s);return true;});return list;}
    private static String plain(FormattedCharSequence source){var b=new StringBuilder();source.accept((i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static void span(Component source,String text,Style wanted){int start=source.getString().indexOf(text);check(start>=0,"missing "+text);var ss=styles(source);for(int n=start;n<start+text.length();n++)check(ss.get(n).equals(wanted),"style/action "+text);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("chatNotifications",new ChatNotificationTranslator(db));
        int labels=0;var seen=new HashSet<String>();
        try(var reader=new BufferedReader(new InputStreamReader(TestQuestInterfaces.class.getResourceAsStream("/data/witherskyru/quest-interface.tsv"),java.nio.charset.StandardCharsets.UTF_8))){
            for(String line;(line=reader.readLine())!=null;){if(line.isBlank()||line.startsWith("#"))continue;String[] p=line.split("\t",-1);check(p.length==2&&seen.add(p[0]),"invalid/duplicate interface row");labels++;
                check(db.translate(p[0]).equals(p[1]),"plain label "+p[0]);var input=Component.literal(p[0]).setStyle(MOB);var result=SkyRuClient.component(input);check(result.getString().equals(p[1]),"component label "+p[0]);
                for(var s:styles(result))check(s.equals(MOB),"whole label style/action "+p[0]);
                check(plain(SkyRuClient.sequence(input.getVisualOrderText())).equals(p[1]),"sequence label "+p[0]);check(input.getString().equals(p[0]),"label immutable");for(int r=0;r<5;r++)check(db.translate(p[1]).equals(p[1]),"label repeat");
                Path ownPath=Path.of(args[0]).resolve("label-"+labels);Files.createDirectories(ownPath);Files.writeString(ownPath.resolve("overrides.tsv"),p[0]+"\tМоя подпись\n");var own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);check(own.translate(p[0]).equals("Моя подпись"),"label override");
            }
        }
        check(labels==72,"72 reviewed short labels;all70 previous retained plus2 observed goals");
        for(String[] p:new String[][]{{"Learning to Compost","Учимся делать компост"},{"Rooting for You","Строительство теплицы"},{"Chapter IV Completion Rewards","Награды за завершение главы IV"},{"Quests & Chapters","Задания и главы"}})check(db.translate(p[0]).equals(p[1]),"independent authored expectation");
        for(String tier:List.of("I","II","III","IV","V","VI","VII")){
            for(String[] shape:new String[][]{{"Chapter ","","Глава "},{"Chapter "," Completion Rewards","Награды за завершение главы "},{"Unlock Chapter ","","Открыть главу "}}){
                if(shape[0].startsWith("Unlock")&&tier.equals("I"))continue;
                var input=Component.empty().append(Component.literal(shape[0]).setStyle(BODY)).append(Component.literal(tier).setStyle(NUMBER)).append(Component.literal(shape[1]).setStyle(BODY));var result=SkyRuClient.component(input);
                check(result.getString().equals(shape[2]+tier),"split chapter label "+input.getString());span(result,tier,NUMBER);check(plain(SkyRuClient.sequence(input.getVisualOrderText())).equals(shape[2]+tier),"chapter sequence");
            }
        }
        String[][] cases={
            {"Glacite Walkers","глацитового бродягу","глацитовых бродяг","глацитовых бродяг","",""},
            {"Goblins","гоблина","гоблинов","гоблинов"," in the Goblin Holdout"," в Убежище гоблинов"},
            {"Automatons","автоматона","автоматонов","автоматонов"," in the Precursor Remnant"," в Руинах предтеч"},
            {"Sludges","существо «Слизь»","существа «Слизь»","существ «Слизь»"," in the Jungle"," в Джунглях"},
            {"Team Treasurite Members","участника команды Treasurite","участников команды Treasurite","участников команды Treasurite"," in the Mithril Deposits"," в Мифриловых залежах"},
            {"Yogs","йога","йогов","йогов"," in the Magma Fields"," в Магмовых полях"},
            {"Thysts","тиста","тистов","тистов"," in the Jungle"," в Джунглях"}};
        int objectives=0;
        check(db.translate("Glacite Walker").equals("Глацитовый бродяга"),"commission mob matches existing entity label");check(db.translate("Sludge").equals("Слизь"),"Sludge identity matches existing dictionary");
        for(var p:cases)for(String count:List.of("1","2","5","11","12","14","21","22","25","50","100","1,000","1,001"))for(String dot:List.of("","."))for(boolean located:List.of(false,true)){
            if(located&&p[4].isEmpty())continue;long n=Long.parseLong(count.replace(",",""))%100;int form=n>=11&&n<=14?3:n%10==1?1:n%10>=2&&n%10<=4?2:3;
            String source="Slay "+count+" "+p[0]+(located?p[4]:"")+dot,target="Победите "+count+" "+p[form]+(located?p[5]:"")+".";objectives++;
            check(db.translate(source).equals(target),"whole commission "+source);
            var input=Component.empty().append(Component.literal("Slay ").setStyle(BODY)).append(Component.literal(count).setStyle(NUMBER)).append(Component.literal(" ").setStyle(BODY)).append(Component.literal(p[0]).setStyle(MOB)).append(Component.literal((located?p[4]:"")+dot).setStyle(AREA));
            var result=SkyRuClient.component(input);check(result.getString().equals(target),"styled commission "+source);span(result,count,NUMBER);span(result,p[form],MOB);if(located)span(result,p[5],AREA);
            check(input.getString().equals(source),"commission immutable");check(plain(SkyRuClient.sequence(input.getVisualOrderText())).equals(target),"commission sequence");
            for(int repeat=0;repeat<5;repeat++){check(db.translate(target).equals(target),"commission repeat plain");check(SkyRuClient.component(result).getString().equals(target),"commission repeat styled");}
        }
        for(String text:List.of("Slay 13 Future Mob ZQ","Slay 0 Automatons","Slay 13 Automatons in the Jungle","Slay 10 Sludges in a player's custom place"))check(!db.translate(text).startsWith("Победите "),"not a guessed quest "+text);
        var raw=Component.literal("§eSlay §a13 §bAutomatons§e in the Precursor Remnant.");var rawResult=SkyRuClient.component(raw);check(rawResult.getString().equals("Победите 13 автоматонов в Руинах предтеч."),"legacy-format commission");
        var ss=styles(rawResult);check(ss.get(rawResult.getString().indexOf("13")).getColor().getValue()==0x55ff55,"legacy number colour");check(ss.get(rawResult.getString().indexOf("автоматонов")).getColor().getValue()==0x55ffff,"legacy mob colour");
        Path ownPath=Path.of(args[0]).resolve("commission-own");Files.createDirectories(ownPath);Files.writeString(ownPath.resolve("overrides.tsv"),"Slay 13 Automatons in the Precursor Remnant.\tМоя цель\n");var own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);check(own.translate("Slay 13 Automatons in the Precursor Remnant.").equals("Моя цель"),"whole commission override");
        Files.writeString(ownPath.resolve("overrides.tsv"),"Automatons\tМои существа\n");own=new OfflineDictionary(ownPath);own.setRecordUnknown(false);flag("dictionary",own);check(!SkyRuClient.component(Component.literal("Slay 13 Automatons")).getString().equals("Победите 13 автоматонов."),"semantic override not masked");flag("dictionary",db);
        check(db.translate("Mutates Remaining: 12").equals("Ещё нужно мутаций: 12"),"minimum quota, not remaining planted crops");
        var sam=NpcDialogueTranslator.gardenDialogues();check(sam.translate(Component.literal("[NPC] Sam: Otherwise they'll completely dry out!")).getString().equals("[NPC] Sam: Без полива растения пересохнут и перестанут расти!"),"watering halts, not kills");
        for(String text:List.of("pickaxe mining speed Wither_101","Slay 13 Automatons","[NPC] Sam: Otherwise they'll completely dry out!","Learning to Compost"))for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){
            var message=new GuiMessage(0,Component.literal(text),null,channel,null);check(SkyRuClient.npcChat(message)==message,"whole player/client message unchanged");
        }
        check(SkyRuClient.component(Component.literal("SKYBLOCK")).getString().equals("SKYBLOCK"),"brand protected");
        System.out.println("PASS: Quest interfaces "+checks+" assertions; "+labels+" labels, "+objectives+" synthetic commission variants; five reviewed sections, not all server quests/live QA");
    }
}
