import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Independent authored expectations against real display/tooltip hooks, not a count-only import test. */
public final class TestBulk117003 {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private record Glyph(String text,List<Style> styles) {}
    private static Glyph glyphs(FormattedCharSequence source){
        var text=new StringBuilder();var styles=new ArrayList<Style>();
        source.accept((i,s,p)->{text.appendCodePoint(p);for(int n=0;n<Character.charCount(p);n++)styles.add(s);return true;});
        return new Glyph(text.toString(),List.copyOf(styles));
    }
    private static FormattedCharSequence sequence(Component source){return sink->StringDecomposer.iterateFormatted(source,Style.EMPTY,sink);}
    private static Style fresh(Style source,int seed){
        return source.withShadowColor(seed).withInsertion("bulk-current-"+seed)
            .withClickEvent(new ClickEvent.RunCommand("/unchanged-bulk-"+seed))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("hover-bulk-"+seed)));
    }
    private static Component styled(String legacy,int seed,int chunk){
        var current=glyphs(sequence(Component.literal(legacy)));var out=Component.empty();
        for(int at=0;at<current.text().length();){
            int end=at+1;while(end<current.text().length()&&current.styles().get(end).equals(current.styles().get(at))&&end-at<chunk)end++;
            out.append(Component.literal(current.text().substring(at,end)).setStyle(fresh(current.styles().get(at),seed)));at=end;
        }return out;
    }
    private static void expected(FormattedCharSequence actual,String legacy,int seed,String label){
        var got=glyphs(actual);var wanted=glyphs(sequence(styled(legacy,seed,1000)));
        check(got.text().equals(wanted.text()),label+" got="+got.text()+" expected="+wanted.text());
        check(got.styles().equals(wanted.styles()),label+" live semantic styles/actions");
    }
    private static Component unsupportedStyle(JsonObject row){
        int at=0,changed=-1;
        for(var value:row.getAsJsonArray("parts")){
            var part=value.getAsJsonArray();String from=part.get(0).getAsString(),to=part.get(1).getAsString();
            if(changed<0&&!from.equals(to)&&from.length()>1){
                for(int n=0;n<from.length();n++)if(Character.isLetterOrDigit(from.charAt(n))){changed=at+n;break;}
            }at+=from.length();
        }
        if(changed<0)throw new AssertionError("No nonuniform negative span in independent fixture");
        var source=glyphs(sequence(styled(row.get("legacy").getAsString(),99,1000)));
        int padding=source.text().indexOf(row.get("source").getAsString());
        if(padding<0)throw new AssertionError("Source span missing from legacy fixture");
        changed+=padding; // Break the translated span, never harmless source padding.
        var out=Component.empty();
        for(int n=0;n<source.text().length();n++)out.append(Component.literal(source.text().substring(n,n+1))
            .setStyle(n==changed?source.styles().get(n).withInsertion("unreviewed-span-break"):source.styles().get(n)));
        return out;
    }
    public static void main(String[] args)throws Exception{
        var config=Path.of(args[0]);Files.createDirectories(config);var dictionary=new OfflineDictionary(config);dictionary.setRecordUnknown(false);
        flag("dictionary",dictionary);flag("enabled",true);flag("onlyHypixel",false);
        var fixture=JsonParser.parseString(Files.readString(Path.of("translation-source/BULK117_003_ACCEPTANCE.json"))).getAsJsonObject();
        var rows=fixture.getAsJsonArray("records");check(rows.size()==1000,"requested1000 new unique cases");var seen=new HashSet<String>();
        boolean baseline=Arrays.asList(args).contains("--expect-missing");int contextCases=0;
        for(var value:rows){
            var row=value.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();int id=row.get("batch_index").getAsInt();
            check(seen.add(source.strip().toLowerCase(Locale.ROOT)),"unique new source "+id);
            if(baseline){
                check(dictionary.translate(source).equals(source),"baseline dictionary miss "+id);
                check(glyphs(sequence(SkyRuClient.component(Component.literal(row.get("legacy").getAsString())))).text().strip().equals(source),"baseline component miss "+id);
                if(row.has("context_rows")){
                    var inputs=new ArrayList<Component>();for(var item:row.getAsJsonArray("context_rows"))inputs.add(Component.literal(item.getAsString()));
                    String text=glyphs(sequence(TooltipTranslation.components(inputs,false).get(row.get("context_row_index").getAsInt()))).text().strip();
                    check(text.equals(source),"baseline context already covered "+id+": "+text);
                }continue;
            }
            check(dictionary.translate(source).equals(target),"dictionary exact source "+id);
            for(int seed:List.of(1,2))for(int chunk:List.of(3,1000)){
                var input=styled(row.get("legacy").getAsString(),seed,chunk);String before=input.getString();
                expected(sequence(SkyRuClient.component(input)),row.get("expected_legacy").getAsString(),seed,"component "+id);
                expected(SkyRuClient.sequence(sequence(input)),row.get("expected_legacy").getAsString(),seed,"glyphs "+id);
                var single=TooltipTranslation.components(List.of(Component.literal("Bulk title"),input),false).get(1);
                expected(sequence(single),row.get("expected_legacy").getAsString(),seed,"tooltip "+id);
                check(input.getString().equals(before),"source immutable "+id);
                var player=new GuiMessage(0,input,null,GuiMessageSource.PLAYER,null);
                check(SkyRuClient.npcChat(player)==player,"player queue untouched "+id);
            }
            check(SkyRuClient.component(Component.literal(target)).getString().equals(target),"idempotent target "+id);
            var mixed=unsupportedStyle(row);
            var negativeResult=SkyRuClient.component(mixed);
            if(negativeResult!=mixed)Files.writeString(config.resolve("negative-failure.json"),new Gson().toJson(Map.of("index",id,"source",source,"actual",glyphs(sequence(negativeResult)).text(),"expected",glyphs(sequence(mixed)).text(),"same_styles",glyphs(sequence(negativeResult)).styles().equals(glyphs(sequence(mixed)).styles()))));
            check(negativeResult==mixed,"unreviewed semantic style keeps original component "+id);
            var mixedSequence=sequence(mixed);
            check(SkyRuClient.sequence(mixedSequence)==mixedSequence,"unreviewed semantic style keeps original glyphs "+id);
            if(row.has("context_rows")){
                var inputs=new ArrayList<Component>();for(var item:row.getAsJsonArray("context_rows"))inputs.add(Component.literal(item.getAsString()));
                var out=TooltipTranslation.components(inputs,false);int index=row.get("context_row_index").getAsInt();
                check(glyphs(sequence(out.get(index))).text().strip().equals(target),"actual complete item tooltip "+id+" got="+glyphs(sequence(out.get(index))).text());
                var liveInputs=new ArrayList<Component>();for(var item:row.getAsJsonArray("context_rows"))liveInputs.add(styled(item.getAsString(),7,3));
                var liveOut=TooltipTranslation.components(liveInputs,false);expected(sequence(liveOut.get(index)),row.get("expected_legacy").getAsString(),7,"live complete UI context "+id);contextCases++;
            }
        }
        for(var extraValue:fixture.getAsJsonArray("supplemental")){
            var row=extraValue.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();
            check(dictionary.translate(source).equals(target),"supplemental target already existed, not new");
            var inputs=new ArrayList<Component>();for(var item:row.getAsJsonArray("context_rows"))inputs.add(styled(item.getAsString(),8,3));
            var output=TooltipTranslation.components(inputs,false);int index=row.get("context_row_index").getAsInt();
            if(baseline){
                check(glyphs(sequence(output.get(index))).text().equals(source),"Agatha baseline full tooltip miss");
                check(glyphs(sequence(SkyRuClient.component(styled(row.get("legacy").getAsString(),8,3)))).text().equals(source),"Agatha baseline component miss");
            }else{
                expected(sequence(output.get(index)),row.get("expected_context_legacy").getAsString(),8,"Agatha reviewed semantic styles in full tooltip");
                for(int seed:List.of(1,2))for(int chunk:List.of(3,1000)){
                    var input=styled(row.get("legacy").getAsString(),seed,chunk);String before=input.getString();
                    expected(sequence(SkyRuClient.component(input)),row.get("expected_legacy").getAsString(),seed,"Agatha component");
                    expected(SkyRuClient.sequence(sequence(input)),row.get("expected_legacy").getAsString(),seed,"Agatha glyphs");
                    check(input.getString().equals(before),"Agatha source immutable");
                }
                var unreviewed=unsupportedStyle(row);check(SkyRuClient.component(unreviewed)==unreviewed,"Agatha unknown style guarded");
            }
        }
        if(!baseline){
            check(SkyRuClient.component(Component.literal("I love the warmth of the Holidays despite the snow")).getString().equals("I love the warmth of the Holidays despite the snow"),"required English quest sentence untouched");
            var customDir=config.resolve("override");Files.createDirectories(customDir);Files.writeString(customDir.resolve("overrides.tsv"),"Search all recipes by name!\tМой прогресс\n");
            var custom=new OfflineDictionary(customDir);custom.setRecordUnknown(false);flag("dictionary",custom);
            check(SkyRuClient.component(Component.literal("Search all recipes by name!")).getString().equals("Мой прогресс"),"full-caption user override wins");flag("dictionary",dictionary);
        }
        System.out.println("PASS bulk117-003: "+checks+" assertions;1000 new source cases;"+contextCases+" full item contexts;baseline="+baseline);
    }
}
