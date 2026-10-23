import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Independent published UI titles through actual component and glyph hooks. */
public final class TestMelodyTitles117 {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object value)throws Exception {
        var field=SkyRuClient.class.getDeclaredField(name);field.setAccessible(true);field.set(null,value);
    }
    private static String text(FormattedCharSequence value){
        var out=new StringBuilder();value.accept((i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();
    }
    private static FormattedCharSequence glyphs(Component value){
        return sink->StringDecomposer.iterateFormatted(value,Style.EMPTY,sink);
    }
    public static void main(String[] args)throws Exception {
        Path config=Path.of(args[0]);Files.createDirectories(config);
        var dictionary=new OfflineDictionary(config);dictionary.setRecordUnknown(false);
        flag("dictionary",dictionary);flag("enabled",true);flag("onlyHypixel",false);
        var fixturePaths=new ArrayList<Path>();
        for(String arg:args)if(arg.startsWith("--fixture="))fixturePaths.add(Path.of(arg.substring(10)));
        if(fixturePaths.isEmpty())try(var paths=Files.newDirectoryStream(Path.of("translation-source"),"MELODY_ACCEPTANCE*.json")){
            for(var path:paths)fixturePaths.add(path);
        }
        var rows=new JsonArray();boolean bestPercent=false;
        for(var path:fixturePaths){
            var fixture=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for(var row:fixture.getAsJsonArray("records"))rows.add(row);
            bestPercent|=fixture.has("best_percent_template")&&fixture.get("best_percent_template").getAsBoolean();
        }
        var seen=new HashSet<String>();boolean missing=Arrays.asList(args).contains("--expect-missing");
        for(var item:rows){
            var row=item.getAsJsonObject();String source=row.get("source").getAsString(),target=row.get("translation").getAsString();
            check(seen.add(source),"unique source");
            for(int seed=1;seed<=2;seed++)for(int chunk:List.of(3,source.length())){
                var style=Style.EMPTY.withColor(0x55ff55).withShadowColor(seed).withBold(seed==2)
                    .withInsertion("melody-"+seed).withClickEvent(new ClickEvent.RunCommand("/keep-"+seed))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("hover-"+seed)));
                var input=Component.empty();
                for(int at=0;at<source.length();at+=chunk)input.append(Component.literal(source.substring(at,Math.min(at+chunk,source.length()))).setStyle(style));
                String expected=missing?source:target;
                var output=SkyRuClient.component(input);
                check(missing?!text(glyphs(output)).equals(target):text(glyphs(output)).equals(expected),"component "+source+" got "+text(glyphs(output)));
                var sequence=SkyRuClient.sequence(glyphs(input));
                check(missing?!text(sequence).equals(target):text(sequence).equals(expected),"glyphs "+source);
                sequence.accept((i,s,p)->{check(s.equals(style),"current full style preserved");return true;});
                check(input.getString().equals(source),"source tree immutable");
                var player=new GuiMessage(0,input,null,GuiMessageSource.PLAYER,null);
                check(SkyRuClient.npcChat(player)==player,"player chat untouched");
            }
            check(SkyRuClient.component(Component.literal(target)).getString().equals(target),"idempotent Russian title");
            if(!missing){
                var mixed=Component.empty().append(Component.literal(source.substring(0,1)).withStyle(Style.EMPTY.withColor(0x123456)))
                    .append(Component.literal(source.substring(1)).withStyle(Style.EMPTY.withColor(0x654321)));
                check(SkyRuClient.component(mixed)==mixed,"unreviewed mixed title style keeps original tree");
                var mixedGlyphs=glyphs(mixed);
                check(SkyRuClient.sequence(mixedGlyphs)==mixedGlyphs,"unreviewed mixed title style keeps original glyphs");
            }
        }
        if(!missing){
            var overrideDir=config.resolve("override");Files.createDirectories(overrideDir);
            // Actual override filename/loader, not a test-only branch.
            Files.writeString(overrideDir.resolve("overrides.tsv"),"Hymn to the Joy\tМоя мелодия\n");
            var custom=new OfflineDictionary(overrideDir);custom.setRecordUnknown(false);flag("dictionary",custom);
            check(SkyRuClient.component(Component.literal("Hymn to the Joy")).getString().equals("Моя мелодия"),"user override wins");
            flag("dictionary",dictionary);
        }
        if(bestPercent&&!missing)bestPercent(dictionary,config);
        System.out.println("PASS Melody titles117: "+checks+" assertions; "+seen.size()+" titles; baseline="+missing);
    }

    private static void bestPercent(OfflineDictionary dictionary,Path config)throws Exception {
        var labelStyle=Style.EMPTY.withColor(0xaaaaaa).withInsertion("label")
            .withClickEvent(new ClickEvent.RunCommand("/same-label"));
        var numberStyle=Style.EMPTY.withColor(0x55ff55).withBold(true).withInsertion("percent")
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("current-percent")));
        // Independent synthetic values exercise live data; these are not extra translated corpus lines.
        for(String number:List.of("0%","0.0%","37.5%","99.99%","100%!","100.00%!")){
            var input=Component.empty().append(Component.literal("Best: ").setStyle(labelStyle))
                .append(Component.literal(number).setStyle(numberStyle));
            String expected="Лучший результат: "+number;
            var output=SkyRuClient.component(input);check(output.getString().equals(expected),"live percent component "+number);
            var sequence=SkyRuClient.sequence(glyphs(input));check(text(sequence).equals(expected),"live percent glyphs "+number);
            int[] offset={0};
            sequence.accept((i,s,p)->{check(s.equals(offset[0]<"Лучший результат: ".length()?labelStyle:numberStyle),"label and live percent styles remain distinct");offset[0]+=Character.charCount(p);return true;});
            check(input.getString().equals("Best: "+number),"live source remains unchanged");
        }
        for(String invalid:List.of("Best: 101%","Best: 100.01%","Best: -1%","Best: 01%","Best: 1.234%","Best: NaN%","Best: 1e2%")){
            var input=Component.literal(invalid);
            check(SkyRuClient.component(input)==input,"invalid whole percent keeps component "+invalid);
            var sequence=glyphs(input);check(SkyRuClient.sequence(sequence)==sequence,"invalid whole percent keeps glyphs");
        }
        var overrideDir=config.resolve("percent-override");Files.createDirectories(overrideDir);
        Files.writeString(overrideDir.resolve("overrides.tsv"),"Best:\tМой рекорд:\n");
        var custom=new OfflineDictionary(overrideDir);custom.setRecordUnknown(false);flag("dictionary",custom);
        check(SkyRuClient.component(Component.literal("Best:")).getString().equals("Мой рекорд:"),"label override");
        var customInput=Component.literal("Best: 37.5%");
        check(!SkyRuClient.component(customInput).getString().startsWith("Лучший результат:"),"dynamic family yields to label override");
        flag("dictionary",dictionary);
    }
}
