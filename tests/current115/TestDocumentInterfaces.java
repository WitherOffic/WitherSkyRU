import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Observed document forms plus explicitly synthetic boundary/identity/style cases. */
public class TestDocumentInterfaces {
    private static int checks;
    private static final Style BODY=Style.EMPTY.withColor(0xaaaaaa).withInsertion("body");
    private static final Style TERM=Style.EMPTY.withColor(0x55ff55).withHoverEvent(new HoverEvent.ShowText(Component.literal("term")));
    private static final Style NUM=Style.EMPTY.withColor(0xffff55).withClickEvent(new ClickEvent.RunCommand("/original-number"));
    private static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
    private static void flag(String key,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(Component c){var a=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int k=0;k<Character.charCount(p);k++)a.add(s);return true;});return a;}
    private static MutableComponent row(String a,String b,String tail){return Component.empty().append(Component.literal(a).setStyle(BODY)).append(Component.literal(b).setStyle(TERM)).append(Component.literal(tail).setStyle(BODY));}
    private static void ui(OfflineDictionary db,Component source,String target){
        var c=SkyRuClient.component(source);check(c.getString().equals(target),source.getString()+" => "+c.getString());
        check(db.translate(source.getString()).equals(target),"plain: "+db.translate(source.getString()));
        check(plain(SkyRuClient.sequence(source.getVisualOrderText())).equals(target),"sequence");
        for(int n=0;n<5;n++){c=SkyRuClient.component(c);check(c.getString().equals(target),"five repeats");}
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var intro=row("View all your ","Rotten Flesh Collection","");
        ui(db,intro,"Прогресс коллекции «Гнилая плоть»");
        var c=SkyRuClient.component(intro);int at=c.getString().indexOf("Гнилая");check(styles(c).get(at).equals(TERM),"collection item style");
        ui(db,row("Progress to ","Rotten Flesh IV",": 79.4%"),"Прогресс коллекции «Гнилая плоть» до IV: 79.4%");
        var progress=row("Progress to ","Rotten Flesh IV",": ").append(Component.literal("79.4%").setStyle(NUM));
        var renderedProgress=SkyRuClient.component(progress);
        check(renderedProgress.getString().equals("Прогресс коллекции «Гнилая плоть» до IV: 79.4%"),"coloured progress");
        check(styles(renderedProgress).subList(renderedProgress.getString().length()-5,renderedProgress.getString().length()).stream().allMatch(NUM::equals),"progress percentage action/style");
        ui(db,row("","Rotten Flesh IV"," Rewards:"),"Гнилая плоть IV — награды:");
        ui(db,row("","Enchanted Rotten Flesh"," Recipe"),"Зачарованная гнилая плоть — рецепт");
        for(String name:List.of("Rotten Flesh","Wheat","Cobblestone"))for(String level:List.of("I","IV","X","XL")){
            String text="Progress to "+name+" "+level+": 79.4%";
            String target="Прогресс коллекции «"+db.translateForSearch(name)+"» до "+level+": 79.4%";
            ui(db,row("Progress to ",name+" "+level,": 79.4%"),target);
        }
        check(db.translate("Progress to Rotten Flesh IIII: 120%").equals("Progress to Rotten Flesh IIII: 120%"),"invalid collection bounds unchanged");
        var nickRows=new ArrayList<Component>();nickRows.add(Component.literal("Co-op Contributions:"));
        for(String nick:List.of("Wither_101","Pickaxe","Home","Thor","Alpha_2"))nickRows.add(Component.literal("[MVP+] "+nick+": 473").setStyle(TERM));
        nickRows.add(Component.literal(""));nickRows.add(Component.literal("Rotten Flesh IV Rewards:"));
        var roster=TooltipTranslation.components(nickRows,false);
        for(int n=1;n<=5;n++){
            check(roster.get(n)==nickRows.get(n),"identity component untouched");
            check(SkyRuClient.component(roster.get(n))==roster.get(n),"later renderer keeps offline contributor");
            check(plain(SkyRuClient.sequence(roster.get(n).getVisualOrderText())).equals(nickRows.get(n).getString()),"glyph renderer nick unchanged");
        }
        String rosterText=String.join("\n",nickRows.stream().map(Component::getString).toList());
        for(String nick:List.of("Wither_101","Pickaxe","Home","Thor","Alpha_2"))check(db.translate(rosterText).contains("[MVP+] "+nick+": 473"),"plain roster nick unchanged");
        var legacyRoster=List.<Component>of(Component.literal("§7Co-op Contributions:"),Component.literal("§b[MVP+] Pickaxe: §e474"));
        check(TooltipTranslation.components(legacyRoster,false).get(1)==legacyRoster.get(1),"legacy roster unchanged");
        check(db.translate("§7Co-op Contributions:\n§b[MVP+] Pickaxe: §e474").contains("§b[MVP+] Pickaxe: §e474"),"legacy plain roster unchanged");
        check(!db.translate("Speed: 473").equals("Speed: 473"),"non-roster stat still translates");
        var goal=Component.empty().append(Component.literal("Paste the ").setStyle(BODY)).append(Component.literal("Greenhouse").setStyle(TERM)).append(Component.literal(" on a plot at the ").setStyle(BODY)).append(Component.literal("Desk").setStyle(TERM));
        ui(db,goal,"Разместите теплицу на участке через стол");
        check(styles(SkyRuClient.component(goal)).getLast().equals(TERM),"Desk action/style");
        for(int count:List.of(1,2,5,11,21,24,111)){
            String noun=(count%100>=11&&count%100<=14)?"вредителей":count%10==1?"вредитель":count%10>=2&&count%10<=4?"вредителя":"вредителей";
            String verb=count%10==1&&count%100!=11?"появился":"появились";
            var pest=Component.empty().append(Component.literal(count+" ").setStyle(NUM)).append(Component.literal((count==1?"Pest":"Pests")+" Spawned in ").setStyle(TERM)).append(Component.literal("3").setStyle(NUM)).append(Component.literal("!").setStyle(TERM));
            ui(db,pest,count+" "+noun+" "+verb+" на участке № 3!");
            var st=styles(SkyRuClient.component(pest));check(st.get(0).equals(NUM)&&st.get(st.size()-2).equals(NUM),"unchanged pest/plot count styles");
        }
        for(String[] pair:new String[][]{{"Personal Bank Account","Личный банковский счёт"},{"Your very own account!","Ваш личный счёт!"},{"In-season:","В сезоне:"},{"Active:","Активно:"},{"Farming Level:","Уровень фермерства:"},{"Farming XP/h:","Опыт фермерства/ч:"},{"Coins/h:","Монет/ч:"},{"Pet:","Питомец:"},{"Sawdust:","Опилки:"}})ui(db,Component.literal(pair[0]).setStyle(BODY),pair[1]);
        var chat=new ChatNotificationTranslator(db);check(chat.collectionFamilyCount()==4,"four collection notice shapes");
        String[][] notices={{"COLLECTION LEVEL UP Rotten Flesh III➜IV","УРОВЕНЬ КОЛЛЕКЦИИ ПОВЫШЕН Гнилая плоть III➜IV"},{"REWARDS","НАГРАДЫ"},{"Enchanted Rotten Flesh Recipe","Зачарованная гнилая плоть — рецепт"},{"+4 SkyBlock XP","+4 опыта SkyBlock"}};
        for(var pair:notices){
            String src="[02:04:15]    "+pair[0]+" ";String target="[02:04:15]    "+pair[1]+" ";
            Component input=Component.literal(src).setStyle(TERM),result=chat.translate(input,GuiMessageSource.SYSTEM_SERVER,false);
            check(result.getString().equals(target),"collection server chat: "+result.getString());check(input.getString().equals(src),"chat source immutable");
            check(styles(result).stream().allMatch(TERM::equals),"chat actions preserved");
            for(int n=0;n<5;n++){result=chat.translate(result,GuiMessageSource.SYSTEM_SERVER,false);check(result.getString().equals(target),"chat idempotent");}
            check(chat.translate(input,GuiMessageSource.PLAYER,false)==input,"all player text untouched");
            check(chat.translate(input,GuiMessageSource.SYSTEM_SERVER,true)==input,"signed untouched");
            check(chat.translate(input,GuiMessageSource.SYSTEM_CLIENT,false)==input,"server-only collection notices");
        }
        String src=String.join("\n",Arrays.stream(notices).map(a->"[02:04:15]    "+a[0]).toList()),target=String.join("\n",Arrays.stream(notices).map(a->"[02:04:15]    "+a[1]).toList());
        check(chat.translate(Component.literal(src),GuiMessageSource.SYSTEM_SERVER,false).getString().equals(target),"multiline collection");
        var header=Component.empty().append(Component.literal("    COLLECTION LEVEL UP ").setStyle(BODY)).append(Component.literal("Rotten Flesh").setStyle(TERM)).append(Component.literal(" III➜").setStyle(BODY)).append(Component.literal("IV").setStyle(NUM));
        var translated=chat.translate(header,GuiMessageSource.SYSTEM_SERVER,false);
        check(translated.getString().equals("    УРОВЕНЬ КОЛЛЕКЦИИ ПОВЫШЕН Гнилая плоть III➜IV"),"styled collection banner");
        check(styles(translated).getLast().equals(NUM),"new level preserved");check(styles(translated).get(translated.getString().indexOf("Гнилая")).equals(TERM),"item name action preserved");
        var legacyHeader=Component.literal("§6§l    COLLECTION LEVEL UP §eRotten Flesh §7III§a➜§eIV");
        var legacyTranslated=chat.translate(legacyHeader,GuiMessageSource.SYSTEM_SERVER,false);
        check(legacyTranslated.getString().equals("    УРОВЕНЬ КОЛЛЕКЦИИ ПОВЫШЕН Гнилая плоть III➜IV"),"legacy collection banner");
        var legacyStyles=styles(legacyTranslated);int arrow=legacyTranslated.getString().indexOf("➜");
        check(legacyStyles.get(arrow).getColor().getValue()==0x55ff55,"legacy arrow green");
        check(legacyStyles.get(arrow-1).getColor().getValue()==0xaaaaaa,"old level gray");
        check(legacyStyles.getLast().getColor().getValue()==0xffff55,"new level yellow");
        check(legacyStyles.get(4).isBold(),"heading bold preserved");
        var jump=row("    ","CLICK HERE"," to teleport to the plot!");
        check(chat.translate(jump,GuiMessageSource.SYSTEM_SERVER,false).getString().equals("    НАЖМИТЕ ЗДЕСЬ, чтобы переместиться на участок!"),"indented click notice");
        for(String s:List.of("pickaxe","Pickaxe","Wither_101","COLLECTION LEVEL UP Rotten Flesh III➜IV","Wheat Recipe","REWARDS","+4 SkyBlock XP","/msg Wither_101 pickaxe")){
            var message=new GuiMessage(1,Component.literal(s),null,GuiMessageSource.PLAYER,null);
            check(SkyRuClient.npcChat(message)==message,"player message entirely unchanged: "+s);
        }
        var custom=root.resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Progress to Rotten Flesh IV: 79.4%\tМой прогресс\nEnchanted Rotten Flesh Recipe\tМой рецепт\nSkyBlock XP\tМой опыт\n");
        var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);
        check(SkyRuClient.component(Component.literal("Progress to Rotten Flesh IV: 79.4%")).getString().equals("Мой прогресс"),"UI override");
        var ownChat=new ChatNotificationTranslator(own);
        for(String s:List.of("Enchanted Rotten Flesh Recipe","+4 SkyBlock XP")){
            var input=Component.literal(s);check(ownChat.translate(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"notification yields to override");
        }
        flag("dictionary",db);flag("enabled",false);check(SkyRuClient.component(goal)==goal,"disabled UI unchanged");
        System.out.println("PASS: Document interfaces "+checks+" assertions; collection/Garden shapes, roster identities, styles, input/chat, five repeats and overrides");
    }
}
