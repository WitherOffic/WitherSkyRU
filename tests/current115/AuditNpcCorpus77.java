import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Frozen corpus dispatch screening, NOT manual quality or live completeness certification. */
public final class AuditNpcCorpus77 {
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static String plain(Component input){var out=new StringBuilder();StringDecomposer.iterateFormatted(input,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
    private static Component queued(Component input){return SkyRuClient.npcChat(new GuiMessage(0,input,null,GuiMessageSource.SYSTEM_SERVER,null)).content();}
    private static String string(JsonObject row,String field){var value=row.get(field);return value==null||value.isJsonNull()?"":value.getAsString();}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);Files.createDirectories(root);var db=new OfflineDictionary(root.resolve("isolated"));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",new ChatNotificationTranslator(db));
        int total=0,tested=0,flatChanged=0,legacyChanged=0,both=0,allCyrillic=0;var exclusions=new TreeMap<String,Integer>();var perSpeaker=new TreeMap<String,int[]>();
        try(var output=Files.newBufferedWriter(root.resolve("DISPATCH_ROWS.jsonl"));var reader=Files.newBufferedReader(Path.of(args[1]).resolve("translation_queue.jsonl"))){String line;
            while((line=reader.readLine())!=null){var row=JsonParser.parseString(line).getAsJsonObject();total++;String source=string(row,"source"),speaker=string(row,"speaker"),tag=string(row,"tag");String reason=null;
                if(!Set.of("NPC","BOSS","SECURITY","STATUE").contains(tag))reason="not_tagged_dialogue";
                else if(row.get("historical_heading").getAsBoolean()||row.get("alpha_page").getAsBoolean())reason="historical_or_alpha";
                else if(row.get("variable_definition").getAsBoolean()||row.get("nonstandard_tag_layout").getAsBoolean()||row.get("unresolved_markup").getAsBoolean())reason="variable_or_markup_or_layout";
                int body=source.indexOf(": ")+2;
                if(reason==null&&(body<2||source.substring(body).matches("(?s).*(?:\\[[^]]+\\]|<[^>]+>|#|\\{).*")))reason="additional_placeholder_or_choice_guard";
                String legacy=row.get("source_legacy_line").getAsString().strip();
                if(reason==null&&!plain(Component.literal(legacy)).equals(source))reason="legacy_not_single_exact_line";
                if(reason!=null){exclusions.merge(reason,1,Integer::sum);continue;}
                tested++;String flat=plain(queued(Component.literal(source))),colored=plain(queued(Component.literal(legacy)));boolean a=!flat.equals(source),b=!colored.equals(source);if(a)flatChanged++;if(b)legacyChanged++;if(a&&b)both++;if(b&&colored.codePoints().anyMatch(c->c>=0x400&&c<=0x4ff)&&!colored.substring(Math.min(body,colored.length())).matches("(?s).*[A-Za-z]{3,}.*"))allCyrillic++;
                var counts=perSpeaker.computeIfAbsent(speaker,k->new int[4]);counts[0]++;if(a)counts[1]++;if(b)counts[2]++;if(a&&b)counts[3]++;
                var result=new JsonObject();for(String field:List.of("source","speaker","source_url","occurrence_id"))result.add(field,row.get(field));result.addProperty("flat_result",flat);result.addProperty("published_style_result",colored);result.addProperty("flat_changed_not_quality_proof",a);result.addProperty("published_style_changed_not_quality_proof",b);output.write(new Gson().toJson(result));output.newLine();
            }
        }
        var summary=new JsonObject();summary.addProperty("scope","Conservative literal current-publication subset, exact saved legacy lines. Changed text does NOT certify full translation, correctness, all dynamic/live branches or FPS. Cyrillic-without-Latin is only screening, not a quality score.");summary.addProperty("corpus_rows",total);summary.addProperty("tested_literal_rows",tested);summary.addProperty("flat_changed_not_quality_proof",flatChanged);summary.addProperty("published_style_changed_not_quality_proof",legacyChanged);summary.addProperty("both_changed_not_quality_proof",both);summary.addProperty("cyrillic_without_latin_body_screening",allCyrillic);summary.add("excluded",new Gson().toJsonTree(exclusions));summary.add("per_speaker_tested_flat_changed_legacy_changed_both",new Gson().toJsonTree(perSpeaker));
        var executable=Path.of(SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        summary.addProperty("runtime_jar_sha256",HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(executable))));
        summary.addProperty("queue_sha256",HexFormat.of().withUpperCase().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[1]).resolve("translation_queue.jsonl")))));
        if(tested+exclusions.values().stream().mapToInt(Integer::intValue).sum()!=total)throw new AssertionError("Partition");Files.writeString(root.resolve("DISPATCH_SUMMARY.json"),new GsonBuilder().setPrettyPrinting().create().toJson(summary));System.out.println("CORPUS DISPATCH: tested="+tested+";flat_changed="+flatChanged+";published_styles_changed="+legacyChanged+";both="+both+";NOT quality/FPS proof");
    }
}
