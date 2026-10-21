import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import ru.wither.skyru.*;

/** Independent reviewed expectations; preserves noun/name colours even when reordered. */
public class TestPowerStones {
    private static int checks;
    private static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got+"; expected "+expected);checks++;}
    private static void flag(String key,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(key);f.setAccessible(true);f.set(null,value);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,st,cp)->{b.appendCodePoint(cp);return true;});return b.toString();}
    private static final String FIRST="Combine x9 of this item at the",SECOND="Thaumaturgist in the Hub to unlock the";
    private static final String[][] EXPECTED={
        {"Forceful","Напористая сила.","Напористую силу.","Напористую"},
        {"Bloody","силу «Кровавая».","силу «Кровавая».","Кровавая"},
        {"Bubba","Сила Bubba.","силу Bubba.","Bubba"},
        {"Crumbly","Рассыпчатая сила.","Рассыпчатую силу.","Рассыпчатую"},
        {"Shaded","силу «Теневая».","силу «Теневая».","Теневая"},
        {"Sanguisuge","Кровососущая сила.","Кровососущую силу.","Кровососущую"},
        {"Bizarre","силу «Причудливая».","силу «Причудливая».","Причудливая"},
        {"Sighted","силу «Зрячая».","силу «Зрячая».","Зрячая"},
        {"Adept","силу «Искусная».","силу «Искусная».","Искусная"},
        {"Itchy","Зудящая сила.","Зудящую силу.","Зудящую"},
        {"Frozen","Замороженная сила.","Замороженную силу.","Замороженную"},
        {"Slender","силу «Стройная».","силу «Стройная».","Стройная"},
        {"Demonic","Демоническая сила.","Демоническую силу.","Демоническую"},
        {"Silky","силу «Шёлковая».","силу «Шёлковая».","Шёлковая"},
        {"Hurtful","Болезненная сила.","Болезненную силу.","Болезненную"},
        {"Strong","силу «Сильная».","силу «Сильная».","Сильная"},
        {"Mythical","Мифическая сила.","Мифическую силу.","Мифическую"},
        {"Pleasant","Приятная сила.","Приятную силу.","Приятную"},
        {"Sweet","Сладкая сила.","Сладкую силу.","Сладкую"},
        {"Scorching","силу «Обжигающая».","силу «Обжигающая».","Обжигающая"},
        {"Buttery","Масляная сила.","Масляную силу.","Масляную"},
        {"Healthy","Здоровая сила.","Здоровую силу.","Здоровую"}};
    private static void styles(FormattedCharSequence row,String text,String token,Style term,Style noun){
        int at=text.indexOf(token);if(at<0)throw new AssertionError("expected term absent");
        int[] cursor={0};
        row.accept((local,s,cp)->{int i=cursor[0];cursor[0]+=Character.charCount(cp);
            if(!s.equals(i>=at && i<at+token.length()?term:noun))throw new AssertionError("term/prose colour or action lost: "+text);checks++;return true;});
    }
    private static FormattedCharSequence sequence(Component row){var pieces=new ArrayList<FormattedCharSequence>();StringDecomposer.iterateFormatted(row,Style.EMPTY,(i,s,cp)->{pieces.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});return FormattedCharSequence.composite(pieces);}
    private static void prose(OfflineDictionary db,String source,String target){
        var originals=new ArrayList<Component>();int row=0;
        for(String line:source.split("\n"))originals.add(Component.literal(line).setStyle(Style.EMPTY.withColor(0xaaaaaa+row++).withClickEvent(new ClickEvent.RunCommand("/flavour"))));
        var components=TooltipTranslation.components(originals,false);
        var styled=TooltipTranslation.styled(originals.stream().map(Component::getVisualOrderText).toList(),false);
        for(int pass=0;pass<5;pass++){
            eq(db.translate(source),target);eq(db.translate(target),target);
            eq(String.join("\n",components.stream().map(Component::getString).toList()),target);
            eq(String.join("\n",styled.stream().map(TestPowerStones::plain).toList()),target);
            for(int i=0;i<components.size();i++){
                Style expected=originals.get(i).getStyle();
                sequence(components.get(i)).accept((n,s,cp)->{if(!s.equals(expected))throw new AssertionError("flavour style/action lost");checks++;return true;});
                styled.get(i).accept((n,s,cp)->{if(!s.equals(expected))throw new AssertionError("styled flavour style/action lost");checks++;return true;});
            }
            components=TooltipTranslation.components(components,false);styled=TooltipTranslation.styled(styled,false);
        }
        eq(String.join("\n",originals.stream().map(Component::getString).toList()),source);
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        Style name=Style.EMPTY.withColor(0xaa00aa).withBold(true).withHoverEvent(new HoverEvent.ShowText(Component.literal("keep"))),
            noun=Style.EMPTY.withColor(0xaaaaaa).withClickEvent(new ClickEvent.RunCommand("/keep"));
        for(String[] entry:EXPECTED){
            String power=entry[0]+" power.",source=FIRST+"\n"+SECOND+"\n"+power;
            String target="Объедините 9 таких предметов у\nтауматурга в Хабе, чтобы открыть\n"+entry[2];
            eq(db.translate(source),target);eq(db.translate(power),entry[1]);
            var originals=List.<Component>of(Component.literal(FIRST),Component.literal(SECOND),Component.empty()
                .append(Component.literal(entry[0]).setStyle(name)).append(Component.literal(" power.").setStyle(noun)));
            var components=TooltipTranslation.components(originals,false);
            var sequences=TooltipTranslation.styled(originals.stream().map(Component::getVisualOrderText).toList(),false);
            for(int pass=0;pass<5;pass++){
                eq(String.join("\n",components.stream().map(Component::getString).toList()),target);
                eq(String.join("\n",sequences.stream().map(TestPowerStones::plain).toList()),target);
                eq(db.translate(target),target);
                styles(sequence(components.get(2)),entry[2],entry[3],name,noun);styles(sequences.get(2),entry[2],entry[3],name,noun);
                components=TooltipTranslation.components(components,false);sequences=TooltipTranslation.styled(sequences,false);
            }
            eq(originals.get(2).getString(),power);
            var title=new ArrayList<Component>();title.add(Component.literal("Original title"));title.addAll(originals);
            eq(TooltipTranslation.components(title,true).get(0).getString(),"Original title");
            eq(TooltipTranslation.components(title,true).get(3).getString(),entry[2]);
        }
        prose(db,"Accessories have never tasted so\nsweet.","Аксессуары ещё никогда не были такими\nсладкими.");
        prose(db,"Has to be handled with gloves- or\nthe user will quickly develop\nfrostbite.","Берите только в перчатках — иначе\nу владельца быстро разовьётся\nобморожение.");
        eq(db.translate("sweet."),"сладкий.");eq(db.translate("frostbite."),"Обморожение.");
        var unsupported=List.<Component>of(Component.literal(FIRST),Component.literal(SECOND),Component.empty()
            .append(Component.literal("Fro").setStyle(name)).append(Component.literal("zen power.").setStyle(noun)));
        eq(TooltipTranslation.components(unsupported,false).get(2).getString(),"Frozen power.");
        eq(plain(TooltipTranslation.styled(unsupported.stream().map(Component::getVisualOrderText).toList(),false).get(2)),"Frozen power.");
        var unknown=List.<Component>of(Component.literal(FIRST),Component.literal(SECOND),Component.literal("Invented power."));
        eq(TooltipTranslation.components(unknown,false).get(2).getString(),SkyRuClient.component(unknown.get(2)).getString());
        Path custom=Path.of(args[0]).resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Frozen power.\tМоя сила\n");
        var override=new OfflineDictionary(custom);override.setRecordUnknown(false);flag("dictionary",override);
        var customRows=List.<Component>of(Component.literal(FIRST),Component.literal(SECOND),Component.literal("Frozen power."));
        eq(TooltipTranslation.components(customRows,false).get(2).getString(),"Моя сила");
        eq(plain(TooltipTranslation.styled(customRows.stream().map(Component::getVisualOrderText).toList(),false).get(2)),"Моя сила");
        Path partial=Path.of(args[0]).resolve("partial");Files.createDirectories(partial);Files.writeString(partial.resolve("overrides.tsv"),"Frozen\tМой термин\n");
        var fragmentOverride=new OfflineDictionary(partial);fragmentOverride.setRecordUnknown(false);flag("dictionary",fragmentOverride);
        var partialRows=List.<Component>of(Component.literal(FIRST),Component.literal(SECOND),Component.empty()
            .append(Component.literal("Frozen").setStyle(name)).append(Component.literal(" power.").setStyle(noun)));
        String partialResult=TooltipTranslation.components(partialRows,false).get(2).getString();
        if(!partialResult.contains("Мой термин"))throw new AssertionError("fragment override overwritten");checks++;
        flag("dictionary",db);flag("enabled",false);eq(TooltipTranslation.components(customRows,false).get(2).getString(),"Frozen power.");
        System.out.println("PASS: power stones "+checks+" assertions; 22 names / five passes / semantic styles / original and override guards");
    }
}
