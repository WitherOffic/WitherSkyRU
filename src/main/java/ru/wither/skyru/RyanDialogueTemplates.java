package ru.wither.skyru;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import static ru.wither.skyru.ChatNotificationTranslator.Edit;
import static ru.wither.skyru.ChatNotificationTranslator.Part;

/** Five source-backed Ryan grammars, invoked only on server-chat receipt. */
final class RyanDialogueTemplates {
    private static final String PREFIX="[NPC] Ryan: ";
    private static final String END="(?: \\((?:x|×)[0-9]{1,6}\\))?$";
    private final Map<String,String> badges;
    private final String greetingTarget;
    private final List<String> greetingKeys;
    private final Set<String> greetingExcluded;
    private final Pattern greeting,upgrade,hold,progress,challenge;

    RyanDialogueTemplates() {
        try(var stream=RyanDialogueTemplates.class.getResourceAsStream("/data/witherskyru/npc-ryan117.json")) {
            if(stream==null)throw new IllegalStateException("Missing closed Ryan data");
            var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            greetingTarget=root.get("greeting_target").getAsString();
            var keys=new ArrayList<String>();for(var value:root.getAsJsonArray("greeting_override_keys"))keys.add(value.getAsString());greetingKeys=List.copyOf(keys);
            var excluded=new HashSet<String>();for(var value:root.getAsJsonArray("greeting_excluded_names"))excluded.add(value.getAsString());greetingExcluded=Set.copyOf(excluded);
            var names=new LinkedHashMap<String,String>();
            for(var entry:root.getAsJsonObject("badges").entrySet()) {
                if(names.put(entry.getKey(),entry.getValue().getAsString())!=null)throw new IllegalStateException("Duplicate badge");
            }
            if(names.size()!=29)throw new IllegalStateException("Unreviewed badge corpus");
            badges=Map.copyOf(names);
            String badge=names.keySet().stream().map(Pattern::quote).collect(java.util.stream.Collectors.joining("|","(?:",")"));
            var tiers=new ArrayList<String>();for(var value:root.getAsJsonArray("tiers"))tiers.add(value.getAsString());
            if(tiers.size()!=30||new HashSet<>(tiers).size()!=30)throw new IllegalStateException("Unreviewed trial corpus");
            String tier=tiers.stream().map(Pattern::quote).collect(java.util.stream.Collectors.joining("|","(?:",")"));
            String prefix="^"+Pattern.quote(PREFIX);
            greeting=Pattern.compile(prefix+Pattern.quote("A Campfire Initiate Badge I, for our latest initiate, ")+"(?:\\[([A-Za-z0-9_]{1,16}) head\\]|\\uFFFC)?([A-Za-z0-9_]{1,16})!"+END);
            upgrade=Pattern.compile(prefix+"Congrats! Your ("+badge+") was upgraded to a ("+badge+")!"+END);
            hold=Pattern.compile(prefix+"I need you to hold your ("+badge+") so I can upgrade it!"+END);
            String number="(?:[1-9][0-9]{0,8}|[1-9][0-9]{0,2}(?:,[0-9]{3}){1,2})";
            progress=Pattern.compile(prefix+"Next step is the Trial of Fire ("+tier+")! \\(("+number+") damage\\)"+END);
            challenge=Pattern.compile(prefix+"Try the Trial of Fire Trial ("+tier+") for a greater challenge!"+END);
        }catch(Exception invalid){throw new IllegalStateException("Invalid closed Ryan grammars",invalid);}
    }
    private static void edit(List<Edit> edits,int start,int end,String target) {
        if(end>start)edits.add(new Edit(start,end,List.of(new Part(target,start,end))));
    }
    private static void before(List<Edit> edits,int end,String target) {edit(edits,PREFIX.length(),end,target);}
    List<Edit> plan(String body,OfflineDictionary dictionary,NamedNpcTemplates legacy) {
        if(!body.startsWith(PREFIX)||body.length()>512)return List.of();
        Matcher match=greeting.matcher(body);
        if(match.matches()) {
            if(match.group(1)!=null&&!match.group(1).equals(match.group(2)))return List.of();
            if(greetingExcluded.contains(match.group(2))||dictionary.hasCustomOverride(greetingKeys))return List.of();
            if(dictionary.hasCustomOverride(List.of("Campfire Initiate Badge I","A Campfire Initiate Badge I, for our latest initiate, ")))return List.of();
            if(match.group(1)==null&&body.charAt(match.start(2)-1)!='\uFFFC')return legacy.plan(body,dictionary);
            int name=match.start(1)>=0?match.start(1)-1:match.start(2);
            if(name>0&&body.charAt(name-1)=='\uFFFC')name--;
            var edits=new ArrayList<Edit>(1);
            before(edits,name,greetingTarget);
            return edits; // Entire opaque head/name/action span is copied unchanged.
        }
        match=upgrade.matcher(body);
        if(match.matches()) {
            if(dictionary.hasCustomOverride(List.of(match.group(1),match.group(2),"Congrats!","was upgraded to a")))return List.of();
            var edits=new ArrayList<Edit>(5);int p=PREFIX.length();
            edit(edits,p,p+10,"Поздравляю! ");edit(edits,p+10,match.start(1),"Знак улучшен: ");
            edit(edits,match.start(1),match.end(1),badges.get(match.group(1)));
            edit(edits,match.end(1),match.start(2)," → ");
            edit(edits,match.start(2),match.end(2),badges.get(match.group(2)));return edits;
        }
        match=hold.matcher(body);
        if(match.matches()) {
            if(dictionary.hasCustomOverride(List.of(match.group(1),"I need you to hold your ")))return List.of();
            var edits=new ArrayList<Edit>(3);before(edits,match.start(1),"Возьми в руку «");
            edit(edits,match.start(1),match.end(1),badges.get(match.group(1)));
            edit(edits,match.end(1),match.end(1)+21,"», чтобы я мог улучшить знак!");return edits;
        }
        match=progress.matcher(body);
        if(match.matches()) {
            if(dictionary.hasCustomOverride(List.of("Trial of Fire","damage")))return List.of();
            var edits=new ArrayList<Edit>(3);int p=PREFIX.length();
            edit(edits,p,p+17,"Следующее ");edit(edits,p+17,match.start(1),"испытание огня — ");
            edit(edits,match.end(2),match.end(2)+8," урона)");return edits;
        }
        match=challenge.matcher(body);
        if(match.matches()) {
            if(dictionary.hasCustomOverride(List.of("Trial of Fire","Trial of Fire Trial ")))return List.of();
            var edits=new ArrayList<Edit>(3);int p=PREFIX.length();
            edit(edits,p,p+8,"Попробуй ");edit(edits,p+8,match.start(1),"испытание огня ");
            edit(edits,match.end(1),match.end(1)+25,", если хочешь задачу посложнее!");return edits;
        }
        return List.of();
    }
}
