import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import com.google.gson.*;
import net.minecraft.nbt.*;

/** Read-only native SNBT parsing. Never loads a world or evaluates an item action. */
public final class CaptureRepositoryNbt117 {
    static final Set<String> VISIBLE=Set.of("Name","Lore","name","text","description","title","pages","filtered_pages","minecraft:item_name","minecraft:custom_name","minecraft:lore","minecraft:written_book_content","minecraft:writable_book_content");
    static final Gson GSON=new Gson();
    static java.io.BufferedWriter output;
    static int strings,visible;
    static void visit(Tag tag,List<String> path,String member)throws Exception{
        if(tag instanceof CompoundTag compound){for(var entry:compound.entrySet()){var next=new ArrayList<>(path);next.add(entry.getKey());visit(entry.getValue(),next,member);}}
        else if(tag instanceof ListTag list){for(int i=0;i<list.size();i++){var next=new ArrayList<>(path);next.add(Integer.toString(i));visit(list.get(i),next,member);}}
        else if(tag instanceof StringTag value){String s=value.value();if(!s.matches("(?s).*[A-Za-z].*"))return;boolean likely=path.stream().anyMatch(VISIBLE::contains);var row=new JsonObject();row.addProperty("source",s);row.addProperty("path",member);row.add("nbt_path",GSON.toJsonTree(path));row.addProperty("likely_visible_field",likely);output.write(GSON.toJson(row));output.newLine();strings++;if(likely)visible++;}
    }
    public static void main(String[] args)throws Exception{
        Path archive=Path.of(args[0]),root=Path.of(args[1]);Files.createDirectories(root);int files=0;var failures=new JsonArray();
        try(var z=new ZipFile(archive.toFile());var writer=Files.newBufferedWriter(root.resolve("SNBT_TEXT_OCCURRENCES.jsonl"))){output=writer;var members=z.entries();
            while(members.hasMoreElements()){var member=members.nextElement();if(member.isDirectory()||!member.getName().endsWith(".snbt"))continue;String name=member.getName().substring(member.getName().indexOf('/')+1);files++;
                if(member.getSize()>4*1024*1024)throw new AssertionError("Unexpected large SNBT file");String source=new String(z.getInputStream(member).readAllBytes(),StandardCharsets.UTF_8);Tag tag;
                try{tag=TagParser.parseCompoundFully(source);}catch(Exception invalid){var r=new JsonObject();r.addProperty("path",name);r.addProperty("error",invalid.toString());failures.add(r);continue;}visit(tag,List.of(),name);
            }
        }
        var summary=new JsonObject();summary.addProperty("snbt_files",files);summary.addProperty("parsed_files",files-failures.size());summary.add("parse_errors",failures);summary.addProperty("all_string_occurrences",strings);summary.addProperty("likely_visible_string_occurrences",visible);summary.addProperty("parser","Local Minecraft26.1.2 TagParser.parseCompoundFully,read-only NBT walk. Not an arbitrary-string regex lexer.");Files.writeString(root.resolve("SNBT_SUMMARY.json"),new GsonBuilder().setPrettyPrinting().create().toJson(summary));System.out.println(summary);
    }
}
