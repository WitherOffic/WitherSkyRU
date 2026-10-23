package ru.wither.skyru;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Closed authored spans, indexed once during dictionary startup. No live styles are retained. */
final class BulkLore117 {
    record Plan(String target,List<String> keys,List<OfflineDictionary.TitleEdit> edits) {}
    record Match(Plan plan,int padding) {
        List<OfflineDictionary.TitleEdit> edits(String actualTarget){
            if(!actualTarget.strip().equals(plan.target()))return List.of();
            if(padding==0)return plan.edits();
            return plan.edits().stream().map(e->new OfflineDictionary.TitleEdit(e.start()+padding,e.end()+padding,e.replacement())).toList();
        }
    }
    private static final Map<String,Plan> PLANS=load();
    static void initialize(){/* Forces source loading at startup, before render hooks. */}
    static Match find(String source){
        int start=0,end=source.length();
        while(start<end&&source.charAt(start)==' ')start++;
        while(end>start&&source.charAt(end-1)==' ')end--;
        var plan=PLANS.get(start==0&&end==source.length()?source:source.substring(start,end));
        return plan==null?null:new Match(plan,start);
    }
    private static Map<String,Plan> load(){
        var entries=new HashMap<String,Plan>();
        for(String resource:List.of("bulk117-001.json","bulk117-002.json","bulk117-003.json","bulk117-004.json","bulk117-005.json","bulk117-006.json","bulk117-007.json")){
        try(var stream=BulkLore117.class.getResourceAsStream("/data/witherskyru/"+resource)){
            if(stream==null)throw new IllegalStateException("Missing reviewed bulk117 resource");
            try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                for(var value:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("records")){
                    var row=value.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();
                    var oldText=new StringBuilder();var newText=new StringBuilder();var keys=new LinkedHashSet<String>();
                    keys.add(source);var edits=new ArrayList<OfflineDictionary.TitleEdit>();
                    for(var part:row.getAsJsonArray("parts")){
                        var pair=part.getAsJsonArray();String from=pair.get(0).getAsString(),to=pair.get(1).getAsString();
                        if(from.isEmpty()){if(!to.isEmpty())throw new IllegalStateException("Unanchored bulk span");continue;}
                        int at=oldText.length();oldText.append(from);newText.append(to);keys.add(from);
                        if(!from.equals(to))edits.add(new OfflineDictionary.TitleEdit(at,at+from.length(),to));
                    }
                    if(!oldText.toString().equals(source)||!newText.toString().equals(target)||source.equals(target)||source.length()>400)
                        throw new IllegalStateException("Invalid reviewed bulk117 span mapping");
                    var plan=new Plan(target,List.copyOf(keys),List.copyOf(edits));
                    if(entries.putIfAbsent(source,plan)!=null)throw new IllegalStateException("Duplicate reviewed bulk117 source");
                }
            }
        }catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
        }
        return Map.copyOf(entries);
    }
}
