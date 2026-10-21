import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

/** Recompose expected complete targets from authored TSV/header labels,not packaged plans. */
public final class TestEggTargets114 {
    static void verify(Path root)throws Exception{
        var locations=new LinkedHashMap<String,String>();
        for(String line:Files.readAllLines(root.resolve("EGG_LOCATIONS_0.15.114_REVIEWED.tsv"))){
            if(line.isBlank()||line.startsWith("#"))continue;String[] f=line.split("\\|",2);String text=f[1];
            if(text.startsWith("[")){var value=new StringBuilder();for(var part:JsonParser.parseString(text).getAsJsonArray())value.append(part.isJsonArray()?part.getAsJsonArray().get(1).getAsString():part.getAsString());text=value.toString();}
            if(locations.put(f[0],text)!=null)throw new AssertionError("Duplicate authored location");
        }
        var h=JsonParser.parseString(Files.readString(root.resolve("EGG_HEADERS_0.15.114_REVIEWED.json"))).getAsJsonObject();var expected=new HashMap<String,String>();
        var inputs=JsonParser.parseString(Files.readString(root.resolve("EXPANSION_PROBE_0.15.114.json"))).getAsJsonObject().getAsJsonArray("records");
        for(var raw:inputs){var r=raw.getAsJsonObject();String id=r.get("id").getAsString();if(!locations.containsKey(id))continue;String source=r.get("source").getAsString(),target=locations.get(id);expected.put(source,target);
            for(var egg:h.getAsJsonObject("egg_names").entrySet()){
                String s=h.get("header_source").getAsString()+h.get("prose_source").getAsString()+egg.getKey()+" "+source+"!";
                String t=h.get("header_target").getAsString()+h.get("prose_target").getAsString()+egg.getValue().getAsString()+" "+target+"!";
                if(expected.put(s,t)!=null)throw new AssertionError("Composition collision");
            }
        }
        var actual=new HashMap<String,String>();try(var stream=TestEggTargets114.class.getResourceAsStream("/data/witherskyru/eggs-expansion114.json")){for(var raw:JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("records")){var r=raw.getAsJsonObject();if(actual.put(r.get("source").getAsString(),r.get("translation").getAsString())!=null)throw new AssertionError("Packaged collision");}}
        if(locations.size()!=224||expected.size()!=1568||!actual.equals(expected))throw new AssertionError("Every authored target independently recomposed");
        System.out.println("PASS:224 independent destination targets+1344 independently recomposed six-envelope targets");
    }
}
