import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.*;

/** Independent observed Healer expectations and complete pinned utility cards, not all Dungeons. */
public final class TestDungeons68 {
    private static int checks;private static Method fixture;
    private static final Pattern NUM=Pattern.compile("[0-9]+(?:[.,][0-9]+)*|(?<![\\p{L}])(?:VII|VI|IV|III|II|V|I)(?![\\p{L}])");
    private static final Pattern ENGLISH=Pattern.compile("\\b(?:heals|everyone|your|the|and|for|of|inside|second|shoots|arrows|damage|while|consume|charges|allows|monsters|shield|cooldown|Launches|maximum|friendly|player|Wish|Revive|Rapid Fire|Dungeon Breaker)\\b",Pattern.CASE_INSENSITIVE);
    private static final Map<String,String> LABELS=Map.ofEntries(Map.entry("Wish","Желание"),Map.entry("Healing Circle","Круг исцеления"),Map.entry("Guided Sheep","Управляемая овца"),Map.entry("Thunderstorm","Гроза"),Map.entry("Throwing Axe","Метательный топор"),Map.entry("Ragnarok","Рагнарёк"),Map.entry("Explosive Shot","Взрывной выстрел"),Map.entry("Rapid Fire","Беглый огонь"),Map.entry("Seismic Wave","Сейсмическая волна"),Map.entry("Castle of Stone","Каменная крепость"));
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static String text(Component c){return plain(c.getVisualOrderText());}
    private static String joined(List<Component> rows){return String.join("\n",rows.stream().map(TestDungeons68::text).toList());}
    private static List<Style> styles(FormattedCharSequence seq){var out=new ArrayList<Style>();seq.accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
    private static List<Object> numbers(List<Component> rows){var out=new ArrayList<Object>();for(var row:rows){String text=text(row);var all=styles(row.getVisualOrderText());var m=NUM.matcher(text);while(m.find())out.add(List.of(m.group(),List.copyOf(all.subList(m.start(),m.end()))));}return out;}
    private static Component row(String source,int mode,int seed)throws Exception{return (Component)fixture.invoke(null,source,mode,seed);}
    private static List<String> strings(JsonArray a){var out=new ArrayList<String>();for(var v:a)out.add(v.getAsString());return out;}
    private static void anchor(List<Component> source,List<Component> result,String en,String ru,String id){for(int r=0;r<source.size();r++){int a=text(source.get(r)).indexOf(en);if(a<0)continue;int b=text(result.get(r)).indexOf(ru);check(b>=0,id+" semantic anchor "+en+" -> "+ru);var expected=styles(source.get(r).getVisualOrderText()).get(a);var got=styles(result.get(r).getVisualOrderText());for(int at=b;at<b+ru.length();at++)check(got.get(at).equals(expected),id+" current semantic color/action "+ru);}}
    private static void paths(List<Component> input,String expected,String id){
        var before=joined(input);var numeric=numbers(input);var output=TooltipTranslation.components(input,false);var sequence=TooltipTranslation.styled(input.stream().map(Component::getVisualOrderText).toList(),false);
        for(int repeat=0;repeat<3;repeat++){
            if(expected!=null)check(joined(output).equals(expected),id+" complete text: "+joined(output));
            check(output.size()==input.size()&&sequence.size()==input.size(),id+" row count");
            check(numeric.equals(numbers(output)),id+" fresh numeric glyph colors and actions");
            for(int r=0;r<output.size();r++){check(text(output.get(r)).equals(plain(sequence.get(r))),id+" component/sequence text row"+r+" repeat"+repeat+": "+text(output.get(r))+" / "+plain(sequence.get(r)));check(styles(output.get(r).getVisualOrderText()).equals(styles(sequence.get(r))),id+" component/sequence current styles row"+r);}
            var next=TooltipTranslation.components(output,false);check(joined(next).equals(joined(output)),id+" idempotence: "+joined(output)+" => "+joined(next));output=next;sequence=TooltipTranslation.styled(sequence,false);
        }
        check(joined(input).equals(before),id+" source immutable");check(TooltipTranslation.components(input,true).getFirst()==input.getFirst(),id+" Shift preserves original title identity");
    }
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var sources=Path.of(args[1]);fixture=TestSacks55.class.getDeclaredMethod("row",String.class,int.class,int.class);fixture.setAccessible(true);
        var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        var observed=JsonParser.parseString(Files.readString(sources.resolve("DUNGEON_ORB_OBSERVED_0.15.68.json"))).getAsJsonObject();
        for(var raw:observed.getAsJsonArray("records")){
            var record=raw.getAsJsonObject();String id=record.get("id").getAsString();var source=strings(record.getAsJsonArray("source_rows"));String target="Сфера подземелий\n"+String.join("\n",strings(record.getAsJsonArray("target_rows")));
            for(int mode=0;mode<3;mode++)for(int seed=0;seed<5;seed++){
                var card=new ArrayList<Component>();card.add(row("§6Dungeon Orb",mode,seed));for(String s:source){String colored=s.replace("20%","§a20%§7").replace("10 ","§b10§7 ").replace("25%","§a25%§7").replace("1%","§a1%§7");card.add(row("§7"+colored,mode,seed));}paths(card,target,id);
            }
            // English reconstructions and synthetic level/scaling values are NOT new live captures.
            for(String value:List.of("0","2","12.5","100")){
                var card=new ArrayList<Component>();card.add(Component.literal("Dungeon Orb"));for(String s:source)card.add(Component.literal(s.replace("секунды.","seconds").replace("здоровья.","health.").replace("секунду.","second.").replace("20%",value+"%").replace("1%",value+"%")));
                var result=TooltipTranslation.components(card,false);check(!ENGLISH.matcher(joined(result)).find(),id+" synthetic value "+value);check(numbers(card).equals(numbers(result)),id+" synthetic values retained");
            }
        }
        var catalogue=JsonParser.parseString(Files.readString(sources.resolve("DUNGEONS_PRIMARY_0.15.68.json"))).getAsJsonObject();check(catalogue.getAsJsonArray("records").size()==19,"19 full pinned cards");
        for(var raw:catalogue.getAsJsonArray("records")){
            var item=raw.getAsJsonObject();String id=item.get("id").getAsString();for(int mode=0;mode<4;mode++)for(int seed=0;seed<3;seed++){
                var input=new ArrayList<Component>();input.add(row(item.get("name_legacy").getAsString(),mode,seed));for(String s:strings(item.getAsJsonArray("lore_legacy")))input.add(row(s,mode,seed));paths(input,null,id);
                var result=TooltipTranslation.components(input,false);String translated=joined(result);check(!ENGLISH.matcher(translated).find(),id+" remaining English prose: "+translated);var latin=Pattern.compile("[A-Za-z]+").matcher(translated);while(latin.find())check(Set.of("UNCOMMON","RARE","EPIC","LEGENDARY","SPECIAL","I","II","III","IV","V","VI","VII").contains(latin.group()),id+" only deliberate rarity/Roman Latin remains: "+latin.group());if(id.equals("BOSS_SPIRIT_BOW"))anchor(input,result,"Thorn","Торна",id);if(id.equals("DUNGEONBREAKER"))anchor(input,result,"The Catacombs","Катакомбах",id);
            }
        }
        var armor=JsonParser.parseString(Files.readString(sources.resolve("DUNGEON_ARMOR_0.15.68.json"))).getAsJsonObject();check(armor.getAsJsonArray("records").size()==8,"eight complete armor paragraphs");
        for(var raw:armor.getAsJsonArray("records")){var p=raw.getAsJsonObject();String id=p.get("id").getAsString();for(int mode=0;mode<4;mode++)for(int seed=0;seed<5;seed++){var card=new ArrayList<Component>();card.add(Component.literal("Unknown armor title"));for(String s:strings(p.getAsJsonArray("source_rows")))card.add(row(s,mode,seed));paths(card,"Unknown armor title\n"+String.join("\n",strings(p.getAsJsonArray("target_rows"))),id);}}
        for(var label:LABELS.entrySet()){check(db.translate(label.getKey()).equals(label.getValue()),"ten ability names "+label);var input=List.<Component>of(Component.literal("Dungeon Orb"),Component.literal("Ability: "+label.getKey()+" LEFT CLICK"));check(joined(TooltipTranslation.components(input,false)).contains(label.getValue()),"ability heading "+label);}
        check(db.translate("Museum (Dungeons)").equals("Музей (подземелья)"),"museum label");
        var reviewed=Class.forName("ru.wither.skyru.ReviewedLoreStyles").getDeclaredMethod("reviewed",List.class,int.class);reviewed.setAccessible(true);
        var invalid=List.of("Heals everyone in your group to full","health and grants them a shield for","20% of their maximum health. The","cooldown is reduced by 10 seconds","for every player below 25% mana.");for(int i=0;i<invalid.size();i++)check(reviewed.invoke(null,invalid,i)==null,"near match/wrong stat rejected");
        var override=root.resolve("custom");Files.createDirectories(override);Files.writeString(override.resolve("overrides.tsv"),"Heals everyone in your group to full\tМоя способность\n");var own=new OfflineDictionary(override);own.setRecordUnknown(false);flag("dictionary",own);var first=observed.getAsJsonArray("records").get(0).getAsJsonObject();var input=new ArrayList<Component>();input.add(Component.literal("Dungeon Orb"));for(String s:strings(first.getAsJsonArray("source_rows")))input.add(Component.literal(s));check(joined(TooltipTranslation.components(input,false)).contains("Моя способность"),"override takes precedence");flag("dictionary",db);
        for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=new GuiMessage(0,Component.literal("Wish"),null,channel,null);check(SkyRuClient.npcChat(msg)==msg,"player/client chat untouched");}
        flag("enabled",false);check(TooltipTranslation.components(input,false)==input,"disabled identity");
        System.out.println("PASS: Dungeons68 "+checks+" assertions; observed Healer paragraphs,19 full pinned cards,8 complete armor paragraphs/4 layouts,ten ability labels,fresh styles/actions/numbers/repeats/Shift/override/negative scope;not all Dungeons or game FPS");
    }
}
