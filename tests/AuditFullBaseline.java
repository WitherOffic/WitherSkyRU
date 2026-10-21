import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.*;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import ru.wither.skyru.*;

/** Baseline inventory, not a live server crawl or a manual-quality certificate. */
public class AuditFullBaseline {
    private static final Pattern COLOR=Pattern.compile("(?i)§[0-9a-fk-or]");
    private static String clean(String value) {
        return COLOR.matcher(value).replaceAll("").replaceFirst("^\\[\\d{2}:\\d{2}:\\d{2}\\] ","");
    }
    private static String category(String body) {
        if(body.startsWith("You haven't claimed your Spooky Rewards")) return "spooky_rewards";
        if(body.startsWith("Talk to the Spooky Man in the Hub")) return "spooky_objective";
        if(body.startsWith("Teleported you to ")) return "teleport";
        if(body.startsWith("[SkyHanni] You have ") && body.contains(" in stash")) return "skyhanni_stash";
        if(body.startsWith("Started pasting ") && body.contains(" preset on Garden Plot - ")) return "garden_preset";
        if(body.startsWith("Remember, you have to be on the island for the resources")) return "garden_presence";
        if(body.startsWith("Your ") && body.endsWith(" is Ready to pick up! [call]")) return "pet_ready";
        if(body.startsWith("You need the Cookie Buff to use this feature")) return "cookie_required";
        if(body.startsWith("Obtain a Booster Cookie from the community shop")) return "cookie_obtain";
        if(body.startsWith("[DropVision] v") && body.endsWith("/dp gui")) return "dropvision_startup";
        if(body.startsWith("RARE DROP! ")) return "rare_drop";
        if(body.startsWith("The Goblin was carrying a bag of coins!")) return "goblin_coins";
        if(body.startsWith("BESTIARY ") || body.strip().equals("BESTIARY")) return "bestiary";
        if(body.startsWith("You reached a ") && body.endsWith(" Kill Combo!")) return "kill_combo";
        if(body.equals("Your Kill Combo has expired!")) return "kill_combo";
        if(body.startsWith("The sound of pickaxes clashing") || body.startsWith("Find the Powder Ghast near ")) return "powder_ghast";
        if(body.startsWith("[SkyHanni] New rabbits are available at Hoppity's Shop!")) return "rabbit_shop";
        if(body.startsWith("[Skyblocker] Found a Chocolate ")) return "chocolate_egg";
        if(body.startsWith("[Sacks] +")) return "sacks";
        if(body.equals("Your Agronomy Sack is full of Seeds!")) return "sack_full";
        if(body.startsWith("GROSS! A ") && body.contains(" Pest has appeared in Plot - ")) return "garden_pest";
        if(body.contains("Farming Fortune has been reduced by ") && body.endsWith("%!")) return "pest_fortune";
        if(body.equals("CLICK HERE to teleport to the plot!")) return "pest_warp";
        if(body.equals("Inventory full! Don't forget to check out your Storage inside the SkyBlock Menu!")
            || body.equals("Inventory full? Don't forget to check out your Storage inside the SkyBlock Menu!")) return "inventory_full";
        if(body.equals("You cannot transfer anything into sacks!")) return "sack_transfer";
        if(body.equals("[SkyHanni] The Broodmother was killed!")) return "broodmother";
        return null;
    }
    public static void main(String[] args) throws Exception {
        Path project=Path.of(args[0]), snapshot=Path.of(args[1]), output=Path.of(args[2]);
        String version=args[3];
        var db=new OfflineDictionary(snapshot.resolve("isolated-config-"+version)); db.setRecordUnknown(false);
        var chat=new ChatNotificationTranslator(db);
        var npc=new NpcDialogueTranslator();
        int total=0, changed=0, unchangedEnglish=0, partialLatin=0;
        Pattern english=Pattern.compile("[A-Za-z]{2}");
        for(String row:Files.readAllLines(project.resolve("translation-source/corpus.jsonl"))) {
            String source=JsonParser.parseString(row).getAsJsonObject().get("source").getAsString();
            String target=db.translateForSearch(source); total++;
            if(!target.equals(source)) {changed++;if(english.matcher(target).find()) partialLatin++;}
            else if(english.matcher(source).find()) unchangedEnglish++;
        }
        Set<String> notificationBodies=new TreeSet<>(),npcBodies=new TreeSet<>();
        int logChatLines=0,unclassified=0;
        for(String line:Files.readAllLines(snapshot.resolve("latest.log"))) {
            int at=line.indexOf("[CHAT] ");if(at<0) continue;logChatLines++;
            String body=clean(line.substring(at+7));
            if(body.startsWith("[NPC] ")) {npcBodies.add(body);continue;}
            if(category(body)!=null) notificationBodies.add(body); else unclassified++;
        }
        Map<String,Integer> notificationCounts=new TreeMap<>();
        var notificationRows=new JsonArray(); int translatedNotifications=0;
        for(String source:notificationBodies) {
            String target=chat.translate(Component.literal(source),GuiMessageSource.SYSTEM_SERVER,false).getString();
            boolean edited=!source.equals(target);if(edited) translatedNotifications++;
            notificationCounts.merge(category(source),1,Integer::sum);
            var row=new JsonObject();row.addProperty("category",category(source));row.addProperty("source",source);
            row.addProperty("translation",target);row.addProperty("changed",edited);notificationRows.add(row);
        }
        int translatedNpc=0;
        for(String source:npcBodies) if(!npc.translate(Component.literal(source)).getString().equals(source)) translatedNpc++;
        Set<String> unknowns=new HashSet<>();
        for(String line:Files.readAllLines(snapshot.resolve("unknown.tsv"))) if(!line.isBlank()) unknowns.add(clean(line).strip());
        int currentChanged=0;
        for(String source:unknowns) if(!db.translateForSearch(source).equals(source)) currentChanged++;
        var result=new JsonObject();result.addProperty("version",version);
        result.addProperty("corpus_sources",total);result.addProperty("dictionary_changed_sources",changed);
        result.addProperty("unchanged_sources",total-changed);result.addProperty("unchanged_with_latin_candidates",unchangedEnglish);
        result.addProperty("changed_with_remaining_latin_candidates",partialLatin);
        result.addProperty("latin_is_not_automatically_an_error",true);
        result.addProperty("log_chat_lines",logChatLines);result.addProperty("unclassified_chat_lines_not_exported",unclassified);
        result.addProperty("reviewed_notification_unique",notificationBodies.size());result.addProperty("reviewed_notification_changed",translatedNotifications);
        result.add("reviewed_notification_categories",new Gson().toJsonTree(notificationCounts));result.add("reviewed_notifications",notificationRows);
        result.addProperty("npc_observed_unique",npcBodies.size());result.addProperty("npc_observed_changed",translatedNpc);
        result.addProperty("unknown_display_unique",unknowns.size());result.addProperty("unknown_display_now_changed_by_dictionary",currentChanged);
        result.addProperty("private_chat_or_unknown_text_exported",false);
        result.addProperty("chat_replayed_as_unsigned_system_notification",true);
        result.addProperty("actual_log_channel_metadata_unavailable",true);
        result.addProperty("full_manual_proofread",false);result.addProperty("live_server_total_known",false);
        result.addProperty("game_launched",false);
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");
        System.out.println("BASELINE: "+version+" corpus="+total+" changed="+changed+" unchanged="+(total-changed)
            +" reviewed notifications="+translatedNotifications+"/"+notificationBodies.size()+" NPC="+translatedNpc+"/"+npcBodies.size()
            +" display log unique="+unknowns.size()+" current dictionary changed="+currentChanged);
    }
}
