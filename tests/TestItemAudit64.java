import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import com.google.gson.*;

/** Integrity and false-positive classification of the public snapshot audit, not runtime changes. */
public final class TestItemAudit64 {
    private static int checks;
    private static final Pattern NUM=Pattern.compile("[0-9]+(?:[.,][0-9]+)*"),LATIN=Pattern.compile("[A-Za-z]{2,}");
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static List<String> values(JsonArray rows,Set<Integer> skip){
        var out=new ArrayList<String>();for(int i=0;i<rows.size();i++){if(skip.contains(i))continue;var m=NUM.matcher(rows.get(i).getAsString());while(m.find())out.add(m.group());}return out;
    }
    private static List<String> numericStyles(JsonArray rows,Set<Integer> skip){
        var out=new ArrayList<String>();for(int i=0;i<rows.size();i++){if(skip.contains(i))continue;var text=new StringBuilder();var styles=new ArrayList<String>();for(var raw:rows.get(i).getAsJsonArray()){var r=raw.getAsJsonObject();String s=r.get("text").getAsString();text.append(s);var signature=r.deepCopy();signature.remove("text");for(int c=0;c<s.length();c++)styles.add(signature.toString());}var m=NUM.matcher(text);while(m.find())out.add(m.group()+":"+styles.subList(m.start(),m.end()));}return out;
    }
    private static String paragraph(JsonArray rows,int start,int end){var out=new ArrayList<String>();for(int i=start;i<end;i++)out.add(rows.get(i).getAsString());return String.join("\n",out);}
    private static String normalized(JsonArray rows){return paragraph(rows,0,rows.size()).strip().replaceAll("\\s+"," ");}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var data=JsonParser.parseString(Files.readString(root.resolve("catalogue.json"))).getAsJsonObject();var audit=JsonParser.parseString(Files.readString(root.resolve("final62/items.json"))).getAsJsonArray();var official=JsonParser.parseString(Files.readString(root.resolve("final62/official-names.json"))).getAsJsonArray();var summary=JsonParser.parseString(Files.readString(root.resolve("final62/summary.json"))).getAsJsonObject();
        check(data.get("commit").getAsString().equals("a225a02217776299ab3fb5cf5d9c0b155c4fc445"),"pinned source commit");
        var expected=new HashMap<String,JsonObject>();for(var raw:data.getAsJsonArray("records")){var r=raw.getAsJsonObject();check(expected.put(r.get("id").getAsString(),r)==null,"unique source ID");check(r.get("source_sha256").getAsString().matches("[0-9A-F]{64}"),"source hash recorded");}
        check(audit.size()==8937&&expected.size()==8937,"all8937 source cards audited");check(official.size()==5680,"all official names audited");
        int englishComp=0,englishEither=0,rawNumbers=0,canonicalUltimate=0,cosmetic=0,semantic=0,petControls=0,trophy=0;var families=new HashSet<String>();var badFamilies=new HashSet<String>();var shapes=new HashSet<String>();var seen=new HashSet<String>();
        for(var raw:audit){var r=raw.getAsJsonObject();String id=r.get("id").getAsString();check(seen.add(id)&&expected.containsKey(id),"one result per public ID");var source=expected.get(id);var before=r.getAsJsonArray("source_lore");var after=r.getAsJsonArray("component_lore");var sequence=r.getAsJsonArray("sequence_lore");
            check(before.size()==source.getAsJsonArray("lore_legacy").size()&&after.size()==before.size()&&sequence.size()==before.size(),"all rows retained "+id);
            check(!r.get("manually_reviewed").getAsBoolean(),"no false per-card human certificate");check(!r.get("new_effective_style_candidate").getAsBoolean()&&!r.get("lost_effective_style_candidate").getAsBoolean(),"effective style sets retained "+id);
            if(r.get("component_sequence_text_difference_candidate").getAsBoolean()){if(normalized(after).equals(normalized(sequence)))cosmetic++;else semantic++;}
            if(r.has("synthetic_lvl100_title_difference")){petControls++;check(!r.get("synthetic_lvl100_title_difference").getAsBoolean(),"raw pet placeholder not a live color bug");}
            if(r.get("title_format_difference_candidate").getAsBoolean()&&r.get("source_title").getAsString().matches(".* (BRONZE|SILVER|GOLD|DIAMOND)$"))trophy++;
            if(r.get("book").getAsBoolean()){
                families.add(id.substring(0,id.indexOf(';')));int end=3;while(end<before.size()&&!before.get(end).getAsString().isEmpty())end++;
                String a=paragraph(after,3,end),b=paragraph(sequence,3,end);boolean ca=LATIN.matcher(a).find(),cb=LATIN.matcher(b).find();if(ca)englishComp++;if(ca||cb){englishEither++;badFamilies.add(id.substring(0,id.indexOf(';')));}
                shapes.add(id.substring(0,id.indexOf(';'))+":"+NUM.matcher(paragraph(before,3,end)).replaceAll("#")+":"+NUM.matcher(a).replaceAll("#"));
                check(!before.get(2).getAsString().equals(after.get(2).getAsString()),"enchantment name exists "+id);
            }
            if(r.get("numeric_change_candidate").getAsBoolean()){
                rawNumbers++;
                if(r.get("book").getAsBoolean()){
                    int at=-1;for(int i=0;i<before.size();i++)if(before.get(i).getAsString().equals("You can only have 1 Ultimate")){check(at<0,"one canonical footer");at=i;}
                    check(at>=0&&after.get(at).getAsString().equals("На одном предмете может быть только одно"),"canonical1→word conversion only");
                    check(values(before,Set.of(at)).equals(values(after,Set.of(at))),"other book numbers unchanged");
                    check(numericStyles(r.getAsJsonArray("source_style_runs"),Set.of(at+1)).equals(numericStyles(r.getAsJsonArray("translated_style_runs"),Set.of(at+1))),"other book numeric glyph styles unchanged");canonicalUltimate++;
                }else if(id.equals("NECRON_BOSS")){
                    check(before.get(0).getAsString().startsWith("Necron is the boss of Floor 7"),"known ordinal source");check(after.get(0).getAsString().startsWith("Некрон — босс VII этажа"),"known ordinal translation");
                    check(values(before,Set.of(0,1,2,3,4)).equals(values(after,Set.of(0,1,2,3,4))),"boss statistics unchanged");
                    check(numericStyles(r.getAsJsonArray("source_style_runs"),Set.of(1,2,3,4,5)).equals(numericStyles(r.getAsJsonArray("translated_style_runs"),Set.of(1,2,3,4,5))),"boss statistic styles unchanged");
                }else{check(id.equals("FINNEGAN_MAYOR_MONSTER"),"all unexplained numeric flags reviewed");check(Collections.frequency(values(after,Set.of()),"20")==Collections.frequency(values(before,Set.of()),"20")+1,"confirmed extra20% in Finnegan");}
            }
        }
        check(families.size()==156&&shapes.size()==195,"all156 families/195 normalized effect shapes");check(englishComp==385&&englishEither==401&&badFamilies.size()==94,"actual mixed effects across both rendering paths");check(rawNumbers==118&&canonicalUltimate==116,"116 canonical footers/1 ordinal/1 genuine duplicate");check(cosmetic==5&&semantic==26,"31 channel flags separated into spacing and substantive differences");check(petControls==325&&trophy==120,"template controls and real trophy differences separated");check(!summary.get("full_manual_proofread").getAsBoolean()&&!summary.get("all_live_variants_known").getAsBoolean(),"scope limits retained");
        System.out.println("PASS: public catalogue audit64 "+checks+" assertions;8937 cards/5680 official names/156 families/195 text shapes,401 mixed effects in94 families;118 numeric flags=116 valid word conversions+1 ordinal+1 confirmed duplicate;325 pet controls,120 trophy title differences;not all live states or full human proofreading");
    }
}
