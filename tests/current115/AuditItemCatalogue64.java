import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Complete snapshot screening, NOT a human proofread or certificate of live completeness. */
public final class AuditItemCatalogue64 {
    private static final Pattern COLOR=Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern PROSE=Pattern.compile("(?i)\\b(the|you|your|of|when|from|while|increases?|reduces?|grants?|saves?|gain|chance|damage|enemies|arrows|time|and|with|for|to|this|can)\\b");
    private static final Pattern NUMBER=Pattern.compile("[0-9]+(?:[.,][0-9]+)*");
    private static final Pattern LATIN=Pattern.compile("[A-Za-z]{2,}");
    private static String clean(String s){return COLOR.matcher(s).replaceAll("");}
    private static void flag(String name,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    private record Glyphs(String text,List<Style> styles){}
    private static Glyphs glyphs(FormattedCharSequence seq){var text=new StringBuilder();var styles=new ArrayList<Style>();seq.accept((i,s,p)->{text.appendCodePoint(p);for(int n=0;n<Character.charCount(p);n++)styles.add(s);return true;});return new Glyphs(text.toString(),styles);}
    private static Glyphs glyphs(Component c){return glyphs(sink->StringDecomposer.iterateFormatted(c,Style.EMPTY,sink));}
    private static List<String> numbers(List<Glyphs> rows){var values=new ArrayList<String>();for(var row:rows){var m=NUMBER.matcher(row.text());while(m.find())values.add(m.group());}return values;}
    private static List<Object> numericStyles(List<Glyphs> rows){var values=new ArrayList<Object>();for(var row:rows){var m=NUMBER.matcher(row.text());while(m.find())values.add(List.of(m.group(),List.copyOf(row.styles().subList(m.start(),m.end()))));}return values;}
    private static JsonArray strings(List<String> rows){var a=new JsonArray();for(String s:rows)a.add(s);return a;}
    private static JsonArray styleRuns(List<Glyphs> rows){
        var out=new JsonArray();for(var row:rows){var spans=new JsonArray();for(int at=0;at<row.text().length();){int end=at+1;var style=row.styles().get(at);while(end<row.text().length()&&style.equals(row.styles().get(end)))end++;var r=new JsonObject();r.addProperty("text",row.text().substring(at,end));if(style.getColor()!=null)r.addProperty("rgb",String.format(Locale.ROOT,"#%06x",style.getColor().getValue()));r.addProperty("bold",style.isBold());r.addProperty("italic",style.isItalic());r.addProperty("underlined",style.isUnderlined());r.addProperty("strikethrough",style.isStrikethrough());r.addProperty("obfuscated",style.isObfuscated());spans.add(r);at=end;}out.add(spans);}return out;
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var output=Path.of(args[1]);Files.createDirectories(output);
        var db=new OfflineDictionary(output.resolve("isolated"));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var source=JsonParser.parseString(Files.readString(root.resolve("catalogue.json"))).getAsJsonObject();
        var records=source.getAsJsonArray("records");var results=new JsonArray();var ids=new HashSet<String>();var families=new TreeSet<String>();
        int titleUnchanged=0,loreCards=0,proseCards=0,proseRows=0,numberFlags=0,numericStyleFlags=0,extraStyleFlags=0,lostStyleFlags=0,channelFlags=0,books=0,bookProse=0,loreRows=0,unchangedLore=0,translatedLore=0,bookTitlesUnchanged=0,bookEffectLatin=0,titleFormatFlags=0,petLevelControls=0,petLevelControlDiff=0;
        for(var raw:records){var item=raw.getAsJsonObject();String id=item.get("id").getAsString();ids.add(id);
            var original=new ArrayList<Component>();original.add(Component.literal(item.get("name_legacy").getAsString()));
            for(var line:item.getAsJsonArray("lore_legacy"))original.add(Component.literal(line.getAsString()));
            var current=TooltipTranslation.components(original,false);var seq=original.stream().map(Component::getVisualOrderText).toList();var styled=TooltipTranslation.styled(seq,false);
            var before=original.stream().map(AuditItemCatalogue64::glyphs).toList();var after=current.stream().map(AuditItemCatalogue64::glyphs).toList();var sequence=styled.stream().map(AuditItemCatalogue64::glyphs).toList();
            boolean titleSame=before.getFirst().text().equals(after.getFirst().text());if(titleSame)titleUnchanged++;
            String uniformTitle=clean(SkyRuClient.component(Component.literal(before.getFirst().text())).getString());boolean titleFormatDiff=!uniformTitle.equals(after.getFirst().text());if(titleFormatDiff)titleFormatFlags++;
            boolean isBook=item.get("itemid").getAsString().equals("minecraft:enchanted_book")&&id.matches("[A-Z0-9_]+;[1-9][0-9]{0,2}")&&item.get("name_plain").getAsString().equals("Enchanted Book");
            int effectEnd=4;
            if(isBook){books++;families.add(id.substring(0,id.indexOf(';')));if(before.get(3).text().equals(after.get(3).text()))bookTitlesUnchanged++;while(effectEnd<before.size()&&!before.get(effectEnd).text().isBlank())effectEnd++;}
            boolean prose=false;var candidateRows=new JsonArray();var plainRows=new ArrayList<String>();
            for(int i=1;i<after.size();i++){
                String text=after.get(i).text();plainRows.add(clean(db.translateForSearch(before.get(i).text())));
                if(!before.get(i).text().isBlank()){loreRows++;if(before.get(i).text().equals(text))unchangedLore++;else translatedLore++;}
                if(PROSE.matcher(text).find()){prose=true;proseRows++;candidateRows.add(i-1);}
            }
            if(after.size()>1)loreCards++;if(prose){proseCards++;if(isBook)bookProse++;}
            boolean changedNumbers=!numbers(before).equals(numbers(after));boolean changedNumericStyle=!numericStyles(before).equals(numericStyles(after));
            var allowed=new HashSet<Style>();for(var row:before)allowed.addAll(row.styles());var used=new HashSet<Style>();for(var row:after)used.addAll(row.styles());boolean extraStyle=!allowed.containsAll(used),lostStyle=!used.containsAll(allowed);
            boolean channelDiff=!after.stream().map(Glyphs::text).toList().equals(sequence.stream().map(Glyphs::text).toList());
            if(changedNumbers)numberFlags++;if(changedNumericStyle)numericStyleFlags++;if(extraStyle)extraStyleFlags++;if(lostStyle)lostStyleFlags++;if(channelDiff)channelFlags++;
            var result=new JsonObject();result.addProperty("id",id);result.addProperty("book",isBook);result.addProperty("source_url",item.get("source_url").getAsString());result.addProperty("source_title",before.getFirst().text());result.addProperty("translated_title",after.getFirst().text());result.addProperty("uniform_unformatted_title_result",uniformTitle);result.addProperty("title_format_difference_candidate",titleFormatDiff);result.addProperty("unchanged_title_candidate",titleSame);
            if(before.getFirst().text().contains("{LVL}")){
                String materialized=item.get("name_legacy").getAsString().replace("{LVL}","100");String colored=clean(SkyRuClient.component(Component.literal(materialized)).getString());String uniform=clean(SkyRuClient.component(Component.literal(clean(materialized))).getString());
                petLevelControls++;if(!colored.equals(uniform))petLevelControlDiff++;result.addProperty("synthetic_lvl100_title_colored",colored);result.addProperty("synthetic_lvl100_title_uniform",uniform);result.addProperty("synthetic_lvl100_title_difference",!colored.equals(uniform));
            }
            result.add("source_lore",strings(before.subList(1,before.size()).stream().map(Glyphs::text).toList()));result.add("component_lore",strings(after.subList(1,after.size()).stream().map(Glyphs::text).toList()));result.add("sequence_lore",strings(sequence.subList(1,sequence.size()).stream().map(Glyphs::text).toList()));result.add("plain_dictionary_lore",strings(plainRows));
            if(isBook){result.addProperty("source_enchantment_title",before.get(3).text());result.addProperty("translated_enchantment_title",after.get(3).text());boolean latin=LATIN.matcher(String.join("\n",after.subList(4,effectEnd).stream().map(Glyphs::text).toList())).find();if(latin)bookEffectLatin++;result.addProperty("effect_latin_word_candidate",latin);}
            result.add("english_prose_candidate_rows",candidateRows);result.addProperty("numeric_change_candidate",changedNumbers);result.addProperty("numeric_style_change_candidate",changedNumericStyle);result.addProperty("new_effective_style_candidate",extraStyle);result.addProperty("lost_effective_style_candidate",lostStyle);result.addProperty("component_sequence_text_difference_candidate",channelDiff);if(lostStyle||extraStyle||channelDiff||changedNumbers||titleFormatDiff){result.add("source_style_runs",styleRuns(before));result.add("translated_style_runs",styleRuns(after));}result.addProperty("manually_reviewed",false);results.add(result);
        }
        var api=JsonParser.parseString(Files.readString(root.resolve("official-items.json"))).getAsJsonObject();var official=new JsonArray();int officialOnly=0,officialUnchanged=0;
        for(var raw:api.getAsJsonArray("items")){var item=raw.getAsJsonObject();String id=item.get("id").getAsString(),name=item.get("name").getAsString();String target=clean(SkyRuClient.component(Component.literal(name)).getString());boolean same=clean(name).equals(target);if(same)officialUnchanged++;if(!ids.contains(id))officialOnly++;var r=new JsonObject();r.addProperty("id",id);r.addProperty("source_name",name);r.addProperty("translated_name",target);r.addProperty("unchanged_candidate",same);r.addProperty("missing_exact_repo_id_lore",!ids.contains(id));official.add(r);}
        var summary=new JsonObject();summary.addProperty("scope","All cards in pinned snapshot plus all official API names. Automated candidate screening ONLY; unchanged proper names/rarities and valid reorders are not necessarily errors. Colors: effective style membership and numeric glyph styles, not full semantic manual proof.");
        summary.addProperty("public_cards_scanned",records.size());summary.addProperty("cards_with_lore",loreCards);summary.addProperty("nonblank_lore_rows",loreRows);summary.addProperty("changed_lore_rows_not_quality_certified",translatedLore);summary.addProperty("unchanged_lore_rows_not_all_errors",unchangedLore);summary.addProperty("unchanged_title_candidates",titleUnchanged);summary.addProperty("english_prose_candidate_cards",proseCards);summary.addProperty("english_prose_candidate_rows",proseRows);summary.addProperty("numeric_change_candidate_cards",numberFlags);summary.addProperty("numeric_style_change_candidate_cards",numericStyleFlags);summary.addProperty("new_effective_style_candidate_cards",extraStyleFlags);summary.addProperty("lost_effective_style_candidate_cards",lostStyleFlags);summary.addProperty("component_sequence_text_difference_cards",channelFlags);summary.addProperty("book_cards_scanned",books);summary.addProperty("enchantment_families_scanned",families.size());summary.addProperty("book_unchanged_enchantment_titles",bookTitlesUnchanged);summary.addProperty("book_effect_latin_word_candidate_cards",bookEffectLatin);summary.addProperty("book_english_prose_candidate_cards",bookProse);summary.addProperty("official_names_scanned",official.size());summary.addProperty("official_unchanged_name_candidates",officialUnchanged);summary.addProperty("official_ids_without_exact_repo_lore",officialOnly);summary.addProperty("full_manual_proofread",false);summary.addProperty("all_live_variants_known",false);summary.addProperty("dictionary_entries",db.phraseCount());summary.addProperty("loaded_client_source",SkyRuClient.class.getProtectionDomain().getCodeSource().getLocation().toString());
        summary.addProperty("title_format_difference_candidate_cards",titleFormatFlags);
        summary.addProperty("synthetic_pet_lvl100_title_controls",petLevelControls);summary.addProperty("synthetic_pet_lvl100_title_difference_candidates",petLevelControlDiff);
        var gson=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();Files.writeString(output.resolve("summary.json"),gson.toJson(summary)+"\n");Files.writeString(output.resolve("items.json"),gson.toJson(results)+"\n");Files.writeString(output.resolve("official-names.json"),gson.toJson(official)+"\n");System.out.println(gson.toJson(summary));
    }
}
