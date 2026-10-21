import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;
import com.google.gson.*;

/** Synthetic caller/context CPU cost; no font, GUI, game main or GPU. */
public final class BenchmarkRenderHotPaths {
    private static final List<String> CONTEXT63_STABLE=List.of("Items are sold for 50% of their","selling price.","Unknown 2","Unknown 3","Unknown 4","Unknown 5","Unknown 6","Unknown 7","Unknown 8","Unknown 9");
    private static final List<String> CONTEXT63_CHURN=List.of("The longer you hold this in your","inventory, the stronger you'll","become for 2 minutes after removing","it from your inventory! Max +50.","","Time Held: 12 Minutes","Unknown 6","Unknown 7","Unknown 8","Unknown 9");
    private static long context63Counter;
    private static volatile int consumed;
    private static boolean modern;
    private static java.util.function.Predicate<String> roster;
    private static boolean technicalBypass;
    private static ChatNotificationTranslator notices;
    private static java.util.concurrent.atomic.AtomicLong serverChecked;
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static int count(FormattedCharSequence sequence){int[] result={0};sequence.accept((i,s,p)->{result[0]++;return true;});return result[0];}
    private static int draw(int depth,Component input,boolean pipeline){
        if(depth>0)return draw(depth-1,input,pipeline);
        Component current=input;
        if(pipeline)current=modern?SkyRuClient.componentOutsideHud(input):HudTextFit.currentPanel()?input:SkyRuClient.component(input);
        var source=current.getVisualOrderText();
        if(!SkyRuClient.active()||technicalBypass&&TechnicalText.active())return count(source);
        try(var context=RenderContext.open()){return count(SkyRuClient.sequence(source,false));}
    }
    private static double median(double[] values){var copy=values.clone();Arrays.sort(copy);return (copy[1]+copy[2])/2;}
    private static void batch(List<Component> rows,int depth,boolean pipeline,String mode){
        if(mode.startsWith("context63_")){
            List<String> text=CONTEXT63_STABLE;
            if(mode.equals("context63_churn")){var fresh=new ArrayList<>(CONTEXT63_CHURN);fresh.set(5,"Time Held: "+(context63Counter++)+" Minutes");text=fresh;}
            else if(mode.equals("context63_oversized")){var fresh=new ArrayList<>(CONTEXT63_STABLE);fresh.set(9,"X".repeat(401));text=fresh;}
            consumed^=SkyRuClient.activeSearchDictionary().contextEdits(text).size();return;
        }
        // Exercise the enabled Hypixel gate without requiring a live connection.
        if(mode.equals("hypixel62_pipeline"))serverChecked.set(System.nanoTime());
        if(mode.equals("numeric62_dictionary")){var db=SkyRuClient.activeSearchDictionary();for(var row:rows)consumed^=db.translate(row.getString()).length();return;}
        if(mode.equals("sack61_components")||mode.equals("sack61_negative")){for(var row:TooltipTranslation.components(rows,false))consumed^=row.getString().length();return;}
        if(mode.equals("sack61_sequences")){for(var row:TooltipTranslation.styled(rows.stream().map(Component::getVisualOrderText).toList(),false))consumed^=count(row);return;}
        if(mode.startsWith("npc60_")){var channel=mode.equals("npc60_player")?net.minecraft.client.multiplayer.chat.GuiMessageSource.PLAYER:net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER;for(var row:rows)consumed^=SkyRuClient.npcChat(new net.minecraft.client.multiplayer.chat.GuiMessage(0,row,null,channel,null)).content().getString().length();return;}
        if(mode.equals("experiment57_notices")){for(var row:rows)consumed^=SkyRuClient.npcChat(new net.minecraft.client.multiplayer.chat.GuiMessage(0,row,null,net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER,null)).content().getString().length();return;}
        if(mode.equals("received56_dates")){for(var row:rows)consumed^=SkyRuClient.component(row).getString().length();return;}
        if(mode.startsWith("npc51_")||mode.startsWith("slayer")){for(var row:rows)consumed^=SkyRuClient.npcChat(new net.minecraft.client.multiplayer.chat.GuiMessage(0,row,null,net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER,null)).content().getString().length();return;}
        if(mode.equals("bestiary_receipts")||mode.equals("bestiary_centered_receipts")){for(var row:rows)consumed^=notices.translate(row,net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_SERVER,false).getString().length();return;}
        if(mode.equals("debug_pipeline")){
            for(var row:rows){if(technicalBypass){try(var debug=TechnicalText.open()){consumed^=draw(0,row,true);}}else consumed^=draw(0,row,true);}return;
        }
        if(mode.equals("roster_lookup")){for(var row:rows)consumed^=roster.test(row.getString())?1:0;return;}
        if(mode.equals("raw_sequence_baseline")){for(var row:rows)consumed^=count(row.getVisualOrderText());return;}
        if(mode.equals("tooltip_components")||mode.equals("reviewed_components")||mode.equals("bestiary_titles")||mode.equals("gui51_components")||mode.equals("fuel52_components")||mode.equals("enchant54_components")||mode.equals("sack55_components")||mode.equals("sack55_negative")||mode.equals("upgrade56_components")||mode.equals("experiment57_components")||mode.equals("enchant58_components")||mode.equals("enchant58_negative")||mode.equals("enchant59_components")||mode.equals("numeric59_negative")){for(var row:TooltipTranslation.components(rows,false))consumed^=row.getString().length();return;}
        if(mode.equals("tooltip_sequences")||mode.equals("reviewed_sequences")||mode.equals("gui51_sequences")||mode.equals("fuel52_sequences")||mode.equals("enchant54_sequences")||mode.equals("sack55_sequences")||mode.equals("upgrade56_sequences")||mode.equals("experiment57_sequences")||mode.equals("enchant58_sequences")||mode.equals("enchant59_sequences")){for(var row:TooltipTranslation.styled(rows.stream().map(Component::getVisualOrderText).toList(),false))consumed^=count(row);return;}
        for(var row:rows)consumed^=draw(depth,row,pipeline);
    }
    public static void main(String[] args)throws Exception{
        try{SkyRuClient.class.getMethod("componentOutsideHud",Component.class);modern=true;}
        catch(NoSuchMethodException older){modern=false;}
        try{Class.forName("ru.wither.skyru.TechnicalText");technicalBypass=true;}catch(ClassNotFoundException older){technicalBypass=false;}
        var names=new HashSet<String>();for(int i=0;i<128;i++)names.add("PerfUser"+i);names.add("Wither_101");names.add("Thor");
        try{var prepare=PlayerNames.class.getDeclaredMethod("prepare",Set.class);prepare.setAccessible(true);roster=(java.util.function.Predicate<String>)prepare.invoke(null,names);}
        catch(NoSuchMethodException older){roster=text->PlayerNames.contains(text,names);}
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var serverField=SkyRuClient.class.getDeclaredField("lastChecked");serverField.setAccessible(true);serverChecked=(java.util.concurrent.atomic.AtomicLong)serverField.get(null);
        notices=new ChatNotificationTranslator(db);
        flag("chatNotifications",notices);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());
        try{flag("winterDialogues",NpcDialogueTranslator.class.getMethod("winterDialogues").invoke(null));}catch(NoSuchMethodException older){}
        var winterReceipts=List.of("[NPC] Frosty: Welcome to Jerry's Workshop!","[NPC] Frosty: Here the Jerrys make and deliver Gifts to all of SkyBlock!","[NPC] Frosty: When it's time to deliver Gifts, Mount Jerry always erupts and the village is ravaged by heat-based enemies!","[NPC] Frosty: You'll need a Frosty the Snow Cannon to aid you in your defense against the Magma Grinches.","[NPC] Frosty: It holds and fires Snowballs, which are the best way the extinguish the enemy!","[NPC] Frosty: I can refill it at any time for a modest price!","[NPC] St. Jerry: 'Tis the Season of Jerry, Test_001!","[NPC] St. Jerry: You may have noticed some Gifts laying around.","[NPC] St. Jerry: Find them all and come back to me!","[NPC] St. Jerry: Find all of the Gifts around the village and come back to me! (x5)").stream().map(s->(Component)Component.literal(s)).toList();
        var negativeNpc=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal("[NPC] Unknown: Unrecorded dialogue "+i+" with original text.")).toList();
        var slayer53=List.of("SLAYER BOSS! The Revenant Horror II spawned!","Your Slayer Kill gave you 80 HP healing for 7 seconds!","[SkyHanni] You have gained 5 Bits.","[Skyblocker] Slayer убит за 5.43s!","NICE! SLAYER BOSS SLAIN!","» Talk to Maddox to claim your Zombie Slayer XP!","✆ RING... RING...","[NPC] Maddox: ✆ Someone answers!","Zombie Slayer LVL 3 - Next LVL in 445 XP!","» Slay 1,000 Combat XP worth of Zombies.").stream().map(s->(Component)Component.literal(s)).toList();
        var bestiaryTitles=List.of("Bestiary Milestone XLV","Bestiary Milestone XLVI","Bestiary Milestone XLVII","Bestiary Milestone XLVIII","Bestiary Milestone XLIX","Bestiary Milestone L","Bestiary Milestone LI","Bestiary Milestone LII","Bestiary Milestone LIII","Bestiary Milestone LIV")
            .stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withColor(0xff5555).withInsertion("original tier"))).toList();
        var slayer54=slayer53.stream().map(s->(Component)Component.literal(s.getString()+" (x27)").setStyle(Style.EMPTY.withInsertion("original counter"))).toList();
        var enchant54=List.of("§7Increases §9 Crit Damage §7by §a100%§7.","§7Increases damage dealt to §a Cubic","§7mobs by §a40%§7.","§7Increases damage dealt to §5 Ender","§7mobs by §a50%§7.","§7Increases damage dealt by §a1.2%","§7for each percent of extra","§7health that your target has","§7above you up to §a65%§7.","§9§lRARE").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original enchant54"))).toList();
        var bestiaryReceipts=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.empty()
            .append(Component.literal("--------------------\n\n     BESTIARY MILESTONE\n      ").withColor(0xffaa00))
            .append(Component.literal("XLII").withColor(0xaaaaaa)).append(Component.literal("➜XLIII").withColor(0xffff55))
            .append(Component.literal("\n\n       REWARD\n       +1").withColor(0xffaa00)).append(Component.literal("❤ Health").withColor(0xff5555))
            .append(Component.literal("\n\n--------------------"))).toList();
        var centeredReceipts=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal(switch(i%5){
            case 0->"[00:06:30] "+" ".repeat(26)+"§6§lBESTIARY MILESTONE";
            case 1->"[00:06:30] "+" ".repeat(35)+"§8XLII§6➡§eXLIII";
            case 2->"[00:06:30] "+" ".repeat(36)+"§6§lREWARD";
            case 3->"[00:06:30] "+" ".repeat(34)+"§8+1§c Health";
            default->"[00:06:24] >>> §6§lCLICK HERE §eto claim it! §6§l<<<";
        })).toList();
        var stable=List.of("Кошелёк: 165,813","Медь: 154","Сад","Конец осени, 15","Ур. 75 (32.9%)","09:10am ☀","Биты: 650/0","Цель:","Питомец: Кролик","www.hypixel.net").stream().map(Component::literal).map(c->(Component)c).toList();
        var titles=List.of("Gems: 75","Strong Aspect of the Dragons","Normal","Wait for Beth to revisit","Mining Fiesta","Rabbit","Diamond Pickaxe","Coal Minion VI","Ability: Throwing Axe","Sawdust: 5,011 (+2,283)").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withColor(0x55ffff).withInsertion(s))).toList();
        var rosterRows=List.of("Wither_101: pickaxe","Wither_101x","Thorns","[MVP+] PerfUser127","Gems: 75","Кошелёк: 165,813","Normal","Sawdust: 5,011 (+2,283)","[VIP] Thor: hello","Tell Beth about the secret lab when she revisits your Garden").stream().map(s->(Component)Component.literal(s)).toList();
        // Same supported paragraphs and styles in both compared versions; positive lookup cost.
        var reviewed=List.of("Книга","Saves arrows 3% of the time when","you fire your bow.",
            "Grants a 25% chance for mobs","and ores to drop double","experience.",
            "Scavenge 0.3 Coins per monster","level on kill.","Grants +15 ❤ Health.","Grants +3 ❈ Defense.")
            .stream().map(s->{var out=Component.empty().setStyle(Style.EMPTY.withColor(0xaaaaaa));
                var matcher=java.util.regex.Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(s);int end=0;
                while(matcher.find()){out.append(Component.literal(s.substring(end,matcher.start())));out.append(Component.literal(matcher.group()).setStyle(Style.EMPTY.withColor(0x55ff55)));end=matcher.end();}
                return (Component)out.append(Component.literal(s.substring(end)));}).toList();
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        var fuel52=List.of("§7Увеличивает скорость работы","§7your minion by §a50%§7 for 24","§7часов!","§7Increases the speed of","§7your minion by §a90%§7 for 5","§7hours!","§7Increases the speed of","§7your minion by §a5%§7 for 30","§7minutes!","§9§lRARE").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original fuel"))).toList();
        var gui51=List.of("§aПовторение §7the musical §dpattern §7to form the","§7longest chain of notes.",
            "§7Earn extra §3Enchanting Exp §7и","§7extra clicks on your next","§dSuperpairs!",
            "§7Learn all about how to §a/visit §7Игроки","§7across the SkyBlock universe!",
            "§7Teleports you to your §aСад§7,","§7where you can grow and harvest","§eКультуры §7and sell them to §bcustomers§7.")
            .stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/original-gui51")).withInsertion("original tooltip"))).toList();
        var sack55=List.of("§7Holds various items obtained from","§cCombat§7! Items you pickup go directly","§7into your sacks.","§7Capacity: §e2,240 of each item","§8Multiple sacks sum their capacity!","§a§lUNCOMMON SACK","§7Holds §dGemstones §7of all types and","§7tiers! Items you pickup go directly","§7into your sacks.","§7Capacity: §e2,560,000 of each gemstone type").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original sack55"))).toList();
        var sackNegative=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal("Holds various items obtained from UNKNOWN"+i).setStyle(Style.EMPTY.withInsertion("unknown original"))).toList();
        var sack61=List.of("A sack which contains other sacks.","Sackception!","Also accessible via /sacks","Obtain sack capacity upgrades as well","as exclusive bits shop sacks.","To Sack of Sacks","Get From Sacks","Enchanted Agronomy Sack","Sack of Sacks III","Stored:").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("current sack61").withClickEvent(new ClickEvent.RunCommand("/original-sack61")))).toList();
        var sack61Negative=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal("A sack which contains UNKNOWN"+i).setStyle(Style.EMPTY.withInsertion("unknown sack61"))).toList();
        var numeric62=List.of("165,813","650/0","75 (32.9%)","09:10","87,437","41,904 (+102)","+1❤","-2.5%","1,234.56","12:34:56").stream().map(s->(Component)Component.literal(s).withColor(0x55ffff)).toList();
        var final62=List.of("Обычный режим","Биты","Самоцветы:","Сумка мешков","К сумке мешков","Бой","Информация","Закрыть","Ваши сумки","Нажмите, чтобы посмотреть!").stream().map(s->(Component)Component.literal(s).withColor(0x55ffff)).toList();
        var upgrades56=List.of("§7Combine this item in an anvil with any","§7sack to increase its capacity by","§c+200% §7its §coriginal §7capacity.","§7This item can be combined with a sack","§7up to three times!","§8It's a pocket sack, to be stitched","§8inside another sack!","§7Your sacks can hold §a+1% §7more items.","§7You can Syphon this shard from","§7your §aHunting Box§7.").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original upgrade56"))).toList();
        var received56=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal("§7Received§dOctober §a1§c3§7, §b2§a0§e2§c6").setStyle(Style.EMPTY.withInsertion("original received56-"+i))).toList();
        var experiment57=List.of("§750 XP Levels","§325k-150k Enchanting Exp","§b7,000 Enchanting Exp (Pairs)","§7Stakes: §dMetaphysical","§7Remaining Clicks: §e22","§7Round: §e2","§7Timer: §a3s","Superpairs (Beginner)","Superpairs Rewards","§7Stored Experimental XP: §b42,000").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original experiment57"))).toList();
        var experimentNotices=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal(switch(i%4){case 0->"§eYou claimed the §r§dUltrasequencer §r§erewards!";case 1->"☕ You bought a bonus charge for the Experimentation Table! (1/3)";case 2->"§8 +§r§3400k Enchanting Exp";default->"§d§kXX§5 ULTRA-RARE BOOK! §d§kXX";}).setStyle(Style.EMPTY.withInsertion("original experiment notice"+i))).toList();
        var enchant58=List.of("§9Titan Killer VII","§7Increases damage dealt by §a20%","§7for every §a100 §7defense your","§7target has up to §a80%§7.","§9Thunderbolt VI","§7Every §c3 §7hits on a monster, strike","§elightning§7, dealing §a25% §7of the hit's","§7damage to up to 10 monsters within 2","§7blocks."," - §bEnchanting Level §a36").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original enchant58"))).toList();
        var enchantNegative=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal(" - Unknown source"+i+" (I-V)").setStyle(Style.EMPTY.withInsertion("original unknown book source"))).toList();
        var enchant59=List.of("§7Every §c3 §7hits on a monster,","§7strike §elightning§7, dealing","§a60% §7of the hit's damage.","§7Deals §a30% §7of your damage","§7dealt to other monsters within","§a4.8 §7blocks of the target.","§7Reduces the target's walk speed by","§a20% §7and deals §2+2% §7of your damage","§7per second per hit, stacking globally","§7up to §240 §7hits. Lasts §65s§7.").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original enchant59"))).toList();
        var numericNegative=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal("Increases experimental unknown rate"+i).setStyle(Style.EMPTY.withInsertion("original numeric-prefix negative"))).toList();
        var npcNew60=List.of("[NPC] Beth: WHAT? There are animals inside of here?!?! Jake is doing horrible experiments with these animals that he catches...","[NPC] Beth: Everything makes sense now...I'm sorry I yelled at you Jake. I'm so sorry.","[NPC] Jake: Those are my plushies! I preserve them in Jelly so they stay fresh!","[NPC] Jake: I am just making toys for animals...","[NPC] Gulliver: Buk bawk bakok Cluckpoint 2 puk pock < 52s?","[NPC] Gulliver: Cluck cluck! Bacawk! Gift Compass bakok.","[NPC] Malik: The Catacombs are an old maze under the island.","[NPC] Malik: Corpses are raised into a powerful army fueled by pain, misery and anguish.","[NPC] Maddox: Complete Slayer Quests and fight bosses to unlock exotic rewards!","[NPC] Maddox: ✆ Hello?").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original npc60"))).toList();
        var npcLegacy60=List.of("[NPC] Beth: WHAT? There are §banimals §finside of here?!?! §cJake §fis doing §chorrible experiments §fwith these §banimals §fthat he catches...","[NPC] Beth: Everything makes sense now...I'm sorry I yelled at you Jake. I'm so sorry.","[NPC] Jake: Those are my §bplushies§f! I preserve them in Jelly so they stay fresh!","[NPC] Jake: I am just making §btoys §ffor §banimals§f...","[NPC] Gulliver: Buk bawk bakok §eCluckpoint 2§r puk pock §e< 52s§r?","[NPC] Gulliver: Cluck cluck! Bacawk! §aGift Compass§r bakok.","[NPC] Malik: The §cCatacombs §rare an old maze under the island.","[NPC] Malik: Corpses are raised into a §apowerful army §rfueled by pain, misery and anguish.","[NPC] Maddox: Complete §5Slayer Quests §fand fight bosses to unlock exotic rewards!","[NPC] Maddox: ✆ Hello?").stream().map(s->(Component)Component.literal(s).setStyle(Style.EMPTY.withInsertion("original legacy npc60"))).toList();
        var npcExisting60=List.of("[NPC] Romero: Bring the item I gave you to Juliette!","[NPC] Romero: Could you find some gold for me, please?","[NPC] Romero: I'm here to get a flower bouquet for Juliette.","[NPC] Romero: Could you help out and get some flowers?","[NPC] Romero: I was thinking of a bouquet of roses.","[NPC] Romero: I'm going to need a full stack of those!","[NPC] Romero: Very cool, but regular emeralds will do the trick.","[NPC] Romero: I need red mushrooms.","[NPC] Romero: That's not the color of mushroom I'm looking for.","[NPC] Romero: EXCEPT... Could you enchant it?").stream().map(s->(Component)Component.literal(s)).toList();
        var npcUnknown60=java.util.stream.IntStream.range(0,10).mapToObj(i->(Component)Component.literal("[NPC] Unknown: Unvisited branch"+i)).toList();
        boolean allocation=bean.isThreadAllocatedMemorySupported();if(allocation&&!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();var report=new JsonObject();report.addProperty("label",args[1]);
        int warmBatches=Integer.getInteger("witherskyru.benchmark.warmup",1000),measuredBatches=Integer.getInteger("witherskyru.benchmark.measure",500);
        if(warmBatches<1000||warmBatches>100000||measuredBatches<500||measuredBatches>100000)throw new IllegalArgumentException("bounded benchmark batches");
        report.addProperty("warmup_batches",warmBatches);report.addProperty("measured_batches_per_round",measuredBatches);report.addProperty("rounds",4);
        report.addProperty("scope","Synthetic fixed 10-row text CPU APIs with scoped caller inspection; context63 modes measure one10-row plan incl identical input construction, not10 entire tooltip renders. Churn is unique each batch; oversized contains401-char row. Deep case adds32 recursive fixture frames. Component pipeline includes high-level hook policy then scoped sequence, no world/HUD budget/font/GPU. Roster case compares name-check algorithm with130 synthetic usernames, not a live connection. Medians of4x"+measuredBatches+" batches after"+warmBatches+" warm batches. Not FPS.");
        report.addProperty("loaded_client_source",SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toString());
for(String mode:List.of("raw_sequence_baseline","stable_scoped","titles_pipeline","stable_deep32","tooltip_components","tooltip_sequences","reviewed_components","reviewed_sequences","disabled_pipeline","roster_lookup","debug_pipeline","bestiary_titles","bestiary_receipts","bestiary_centered_receipts","gui51_components","gui51_sequences","npc51_receipts","npc51_negative","fuel52_components","fuel52_sequences","slayer53_receipts","slayer54_counters","enchant54_components","enchant54_sequences","sack55_components","sack55_sequences","sack55_negative","upgrade56_components","upgrade56_sequences","received56_dates","experiment57_components","experiment57_sequences","experiment57_notices","enchant58_components","enchant58_sequences","enchant58_negative","enchant59_components","enchant59_sequences","numeric59_negative","npc60_new","npc60_legacy","npc60_existing","npc60_unknown","npc60_player","sack61_components","sack61_sequences","sack61_negative","numeric62_pipeline","final62_pipeline","numeric62_dictionary","hypixel62_pipeline","context63_stable","context63_churn","context63_oversized")){
            String selected=System.getProperty("witherskyru.benchmark.scenario","");
            if(!selected.isEmpty()&&!mode.equals(selected))continue;
            boolean disabled=mode.startsWith("disabled"),pipeline=mode.contains("pipeline");int depth=mode.contains("deep")?32:0;
            flag("onlyHypixel",mode.equals("hypixel62_pipeline"));flag("onHypixel",true);
            flag("enabled",!disabled);var rows=mode.startsWith("numeric62")?numeric62:mode.equals("final62_pipeline")?final62:mode.equals("sack61_negative")?sack61Negative:mode.startsWith("sack61")?sack61:mode.equals("npc60_legacy")?npcLegacy60:mode.equals("npc60_existing")?npcExisting60:mode.equals("npc60_unknown")?npcUnknown60:mode.startsWith("npc60")?npcNew60:mode.equals("numeric59_negative")?numericNegative:mode.startsWith("enchant59")?enchant59:mode.equals("enchant58_negative")?enchantNegative:mode.startsWith("enchant58")?enchant58:mode.equals("experiment57_notices")?experimentNotices:mode.startsWith("experiment57")?experiment57:mode.equals("received56_dates")?received56:mode.startsWith("upgrade56")?upgrades56:mode.equals("sack55_negative")?sackNegative:mode.startsWith("sack55")?sack55:mode.startsWith("enchant54")?enchant54:mode.equals("slayer54_counters")?slayer54:mode.equals("slayer53_receipts")?slayer53:mode.startsWith("fuel52")?fuel52:mode.equals("npc51_receipts")?winterReceipts:mode.equals("npc51_negative")?negativeNpc:mode.startsWith("gui51")?gui51:mode.equals("bestiary_centered_receipts")?centeredReceipts:mode.equals("bestiary_titles")?bestiaryTitles:mode.equals("bestiary_receipts")?bestiaryReceipts:mode.startsWith("reviewed")?reviewed:mode.equals("roster_lookup")?rosterRows:mode.startsWith("titles")||mode.startsWith("tooltip")?titles:stable;
            for(int batch=0;batch<warmBatches;batch++)batch(rows,depth,pipeline,mode);
            double[] times=new double[4],bytes=new double[4];
            for(int round=0;round<4;round++){
                long memory=allocation?bean.getThreadAllocatedBytes(thread):0,begin=System.nanoTime();
                for(int batch=0;batch<measuredBatches;batch++)batch(rows,depth,pipeline,mode);
                times[round]=(System.nanoTime()-begin)/(measuredBatches*1e6);bytes[round]=allocation?(bean.getThreadAllocatedBytes(thread)-memory)/(double)measuredBatches:-1;
            }
            var entry=new JsonObject();entry.addProperty("ms_per10",median(times));entry.addProperty("allocated_bytes_per10",median(bytes));entry.add("rounds_ms",new Gson().toJsonTree(times));report.add(mode,entry);
            System.out.printf(Locale.ROOT,"HOT %s %s: %.4f ms / %.0f bytes per10; synthetic, not FPS%n",args[1],mode,median(times),median(bytes));
        }
        flag("enabled",true);Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report)+"\n");
    }
}
