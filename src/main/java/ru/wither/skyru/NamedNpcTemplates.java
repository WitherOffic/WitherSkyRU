package ru.wither.skyru;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ru.wither.skyru.ChatNotificationTranslator.Edit;
import static ru.wither.skyru.ChatNotificationTranslator.Part;

/** Closed one-capture NPC templates. Only immutable strings/offsets are prepared, never styles or events. */
final class NamedNpcTemplates {
    private record Rule(String before,String after,List<String> keys,List<String> excluded,List<List<Edit>> plans,int captureKind) {}
    private final Map<String,List<Rule>> rules;

    NamedNpcTemplates() {
        try {
            var grouped=new HashMap<String,List<Rule>>();var unique=new java.util.HashSet<String>();
            for(String resource:new String[]{"/data/witherskyru/npc-names103.json","/data/witherskyru/npc-structured113.json","/data/witherskyru/npc-structured115.json"})
            try(var stream=NamedNpcTemplates.class.getResourceAsStream(resource)) {
            if(stream==null)throw new IllegalStateException("Missing reviewed name templates");
            try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
                var root=JsonParser.parseReader(reader).getAsJsonObject();
                if(root.get("schema").getAsInt()!=1)throw new IllegalStateException("Unsupported name template schema");
                for(var raw:root.getAsJsonArray("templates")) {
                    var row=raw.getAsJsonObject();String prefix=row.get("prefix").getAsString();
                    int separator=prefix.indexOf(": ",6);
                    if(!prefix.startsWith("[NPC] ")||separator<7||!(prefix.length()==separator+2||prefix.substring(separator+2).equals("✆ ")))
                        throw new IllegalStateException("Invalid closed NPC prefix");
                    var before=row.getAsJsonObject("before");var after=row.getAsJsonObject("after");
                    String left=before.get("source").getAsString(),right=after.get("source").getAsString();
                    int captureKind=row.has("capture_type")?switch(row.get("capture_type").getAsString()){
                        case "username" -> 0;case "integer" -> 1;case "stat_number" -> 2;
                        default -> throw new IllegalStateException("Unknown closed NPC capture kind");
                    }:0;
                    if(prefix.length()+left.length()+right.length()>4096||!unique.add(prefix+left+"{name}"+right))
                        throw new IllegalStateException("Duplicate or oversized name template");
                    var keys=new ArrayList<String>();for(var key:row.getAsJsonArray("override_keys"))keys.add(key.getAsString());
                    var excluded=new ArrayList<String>();for(var word:row.getAsJsonArray("excluded_names"))excluded.add(word.getAsString());
                    var beforeEdits=new ArrayList<Edit>(1);compile(before,prefix.length(),beforeEdits);
                    var plans=new ArrayList<List<Edit>>(16);
                    // Sixteen bounded plans share source text only, with no live names, components or styles.
                    for(int length=1;length<=16;length++) {
                        var edits=new ArrayList<Edit>(2);
                        edits.addAll(beforeEdits);
                        compile(after,prefix.length()+left.length()+length,edits);
                        if(edits.isEmpty())throw new IllegalStateException("Identity name template");
                        plans.add(List.copyOf(edits));
                    }
                    grouped.computeIfAbsent(prefix,k->new ArrayList<>()).add(new Rule(left,right,List.copyOf(keys),List.copyOf(excluded),List.copyOf(plans),captureKind));
                }
            }
            }
            var frozen=new HashMap<String,List<Rule>>();grouped.forEach((key,value)->frozen.put(key,List.copyOf(value)));
            rules=Map.copyOf(frozen);
        }catch(Exception invalid){throw new IllegalStateException("Invalid reviewed name templates",invalid);}
    }

    private static void compile(JsonObject segment,int offset,List<Edit> edits) {
        String source=segment.get("source").getAsString(),target=segment.get("target").getAsString();
        var parts=new ArrayList<Part>();var actual=new StringBuilder();int cursor=0;
        for(var raw:segment.getAsJsonArray("parts")) {
            var p=raw.getAsJsonObject();int start=p.get("start").getAsInt(),end=p.get("end").getAsInt();String text=p.get("text").getAsString();
            if(start!=cursor||end<=start||end>source.length())throw new IllegalStateException("Invalid name-template style span");
            parts.add(new Part(text,offset+start,offset+end));actual.append(text);cursor=end;
        }
        if(cursor!=source.length()||!actual.toString().equals(target))throw new IllegalStateException("Incomplete name-template plan");
        if(!source.equals(target))edits.add(new Edit(offset,offset+source.length(),List.copyOf(parts)));
    }

    List<Edit> plan(String body,OfflineDictionary dictionary) {
        int separator=body.indexOf(": ",6);if(separator<7)return List.of();
        int prefix=separator+2;if(body.startsWith("✆ ",prefix))prefix+=2;
        var candidates=rules.get(body.substring(0,prefix));if(candidates==null)return List.of();
        int end=counterEnd(body);
        for(int r=0;r<candidates.size();r++) {
            var rule=candidates.get(r);
            int name=prefix+rule.before().length(),length=end-name-rule.after().length();
            if(length<1||length>16||!body.startsWith(rule.before(),prefix)||!body.regionMatches(name+length,rule.after(),0,rule.after().length()))continue;
            boolean valid=true;
            if(rule.captureKind()==0){
                for(int i=name;i<name+length;i++){char c=body.charAt(i);if(!(c>='A'&&c<='Z'||c>='a'&&c<='z'||c>='0'&&c<='9'||c=='_')){valid=false;break;}}
            }else valid=number(body,name,length,rule.captureKind());
            // Fixed greetings such as "Hey adventurer!" must retain their older exact translation.
            if(valid)for(int i=0;i<rule.excluded().size();i++){var excluded=rule.excluded().get(i);if(excluded.length()==length&&body.regionMatches(name,excluded,0,length)){valid=false;break;}}
            if(valid&&!dictionary.hasCustomOverride(rule.keys()))return rule.plans().get(length-1);
        }
        return List.of();
    }

    /** Bounded ASCII validation only. No regex, substrings, parsing or live-value cache. */
    private static boolean number(String body,int at,int length,int kind){
        int i=at,end=at+length;
        if(kind==2&&(body.charAt(i)=='-'||body.charAt(i)=='+')){if(++i==end)return false;}
        int first=i,digits=0,group=0;boolean comma=false;
        while(i<end&&body.charAt(i)!='.'){
            char c=body.charAt(i++);
            if(c>='0'&&c<='9'){digits++;group++;}
            else if(c==','){
                if(group==0||(!comma?group>3:group!=3))return false;
                comma=true;group=0;
            }else return false;
        }
        if(digits==0||group==0||comma&&group!=3||digits>1&&body.charAt(first)=='0')return false;
        if(i==end)return true;
        if(kind!=2||end-i-1<1||end-i-1>2)return false;
        while(++i<end){char c=body.charAt(i);if(c<'0'||c>'9')return false;}
        return true;
    }

    private static int counterEnd(String body) {
        int end=body.length();if(end<6||body.charAt(end-1)!=')')return end;
        int at=body.lastIndexOf(" (");if(at<0||end-at<5||end-at>10)return end;
        char kind=body.charAt(at+2);if(kind!='x'&&kind!='×')return end;
        for(int i=at+3;i<end-1;i++){char c=body.charAt(i);if(c<'0'||c>'9')return end;}
        return at;
    }
}
