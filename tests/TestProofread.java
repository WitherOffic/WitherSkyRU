import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import ru.wither.skyru.*;

/** Independent Russian expectations, not generated from translations.json. No game or network. */
public class TestProofread {
    private static int checks;
    private static void check(String label, boolean ok) { if (!ok) throw new AssertionError(label); checks++; }
    private static void eq(OfflineDictionary db, String source, String target) {
        String got = db.translate(source); check(source + " => " + got + "; expected " + target, target.equals(got));
    }
    private static void flag(String name, Object value) throws Exception {
        var field = SkyRuClient.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }
    private static String plain(FormattedCharSequence text) {
        StringBuilder out = new StringBuilder(); text.accept((i, s, cp) -> { out.appendCodePoint(cp); return true; }); return out.toString();
    }
    private static String choose(int n, String one, String few, String many) {
        int last=n%100; return last>=11&&last<=14 ? many : last%10==1 ? one : last%10>=2&&last%10<=4 ? few : many;
    }
    private static final String[][] CASES = {
        {"Each click allows you to climb 2 blocks, up to 4.","Каждое нажатие поднимает вас на 2 блока, максимум — 4."},
        {"Each click allows you to climb 4 blocks, up to\n16.","Каждое нажатие поднимает вас на 4 блока, максимум —\n16."},
        {"Every 2 pet levels, you gain +1\nMining Speed and +1 Mining Fortune.","За повышение уровня питомца на 2 уровня вы получаете +1\nскорости добычи и +1 удачи добычи."},
        {"Find Enigma Souls across the Rift\nDimension.","Находите души Энигмы по всему измерению\nРазлома."},
        {"Every 4 Souls, go back to Enigma for\nmore upgrades.","За каждые 4 собранные души возвращайтесь к Энигме за\nновыми улучшениями."},
        {"Each adds +20 Rift Time to this item.","Каждая душа добавляет +20 времени в Разломе к этому предмету."},
        {"Every 1 Zombies killed during a\ndungeon run by this weapon gives\nthe wielder +1  Strength.","Каждый зомби, убитый во время\nпохода в подземелье этим оружием, даёт\nвладельцу +1  силы."},
        {"Pulling a mob multiplies its damage taken by 2x\nand stops it from flying.","Притягивание моба удваивает получаемый им урон\nи не даёт ему летать."},
        {"Breathe 5x as long underwater when\nwearing!","Позволяет дышать под водой в 5 раз дольше, когда\nпредмет надет!"},
        {"Field Mice are 3x more likely to be\ncaught in this type of trap!","Полевые мыши в 3 раза чаще\nпопадаются в такие ловушки!"},
        {"Grants +0.5 ☯ Social Wisdom.","Даёт +0.5 ☯ мудрости общения."},
        {"Grants +{stat} Dwarven Metal Fortune.","Даёт +{stat} удачи добычи дварфских металлов."},
        {"Cultivating Needed","Нужно зачарование «Возделывание»"},
        {"Cultivating cannot be combined!","«Возделывание» нельзя объединить!"},
        {"with special abilities from another\ndimension.","особыми способностями из другого\nизмерения."},
        {"You take +50% damage while holding\nthis weapon.","Вы получаете на +50% больше урона, держа\nэто оружие."},
        {"Your Critter Capsules are 35% more\nlikely to catch  Critters.","Ваши капсулы зверьков на 35% чаще\nловят  зверьков."},
        {"Upgrade your Eagle Pet in the Safari\nEssence Shop!","Улучшите своего питомца-орла в магазине\nэссенции Сафари!"},
        {"Anniversary Barn Skin on The\nGarden!","юбилейный облик амбара в\nСаду!"},
        {"Bounces up to 4 targets, each\nbounce dealing 15% less damage.","Отскакивает между целями, которых может быть до 4; каждый\nотскок наносит на 15% меньше урона."},
        {"Melee hits arc with cursed energy,\nchaining to up to 5 nearby enemies.","Удары ближнего боя испускают проклятую энергию,\nкоторая поражает до 5 ближайших врагов."},
        {"While active,  Pests will take 4x as\nlong to spawn.","Пока действует,  вредителям требуется в 4 раза больше времени\nдля появления."},
        {"damaging up to 3 of your foes! Damage","нанося урон не более чем 3 врагам! Урон"},
        {"Place in a Garden plot and fill with\nbait to catch  Pests while offline.","Разместите на участке Сада и заполните\nнаживкой для ловли  вредителей, пока вас нет в игре."},
        {"Trust no one.\nNot even your own shadow.\nIt's always acting shady.","Никому не доверяйте.\nДаже собственной тени.\nОна всегда ведёт себя подозрительно."},
        {"lots of times. Grants 2x stats on The\nCatacombs Floor VII.","много раз. Удваивает характеристики на\nVII этаже Катакомб."},
        {"Goldor, and Necron in The Catacombs","Голдора и Некрона в Катакомбах"},
        {"the Catacombs eons ago. Never\ndefeated, feared by anything living\nAND dead.","Катакомбы много веков назад. Никогда\nне побеждённые, внушающие страх всему живому\nи мёртвому."},
        {"Applies the Sticky reforge when\ncombined with a Fishing Net.","Накладывает перековку «Липкий» при\nобъединении с рыболовной сетью."}
    };
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        flag("dictionary",db); flag("enabled",true); flag("onlyHypixel",false);
        for (var pair : CASES) {
            eq(db,pair[0],pair[1]);
            List<Component> components = new ArrayList<>(); components.add(Component.literal("Test title"));
            for (String row : pair[0].split("\n",-1)) components.add(Component.literal(row));
            var translated=TooltipTranslation.components(components,false);
            String actual=String.join("\n",translated.subList(1,translated.size()).stream().map(Component::getString).toList());
            check("component contextual paragraph " + pair[0], actual.equals(pair[1]));
            var sequences=components.stream().map(c -> FormattedCharSequence.forward(c.getString(),Style.EMPTY)).toList();
            var styled=TooltipTranslation.styled(sequences,false);
            check("styled contextual paragraph " + pair[0],String.join("\n",styled.subList(1,styled.size()).stream().map(TestProofread::plain).toList()).equals(pair[1]));
            var twice=TooltipTranslation.styled(styled,false);
            check("contextual translation is idempotent",twice.stream().map(TestProofread::plain).toList().equals(styled.stream().map(TestProofread::plain).toList()));
            check("source components unmodified",String.join("\n",components.subList(1,components.size()).stream().map(Component::getString).toList()).equals(pair[0]));
        }
        for(String n:List.of("12.5","25","37.5","50","62.5","75","29.75"))
            eq(db,"Grants +"+n+" ☘ Farming\nFortune, which increases your\nchance for multiple crops.","Даёт +"+n+" ☘ фермерской\nудачи, которая повышает\nшанс получить больше урожая.");
        for(String n:List.of("10","20","30","45","29.75"))
            eq(db,"Grants +"+n+" ☘ Mining\nFortune, which increases your\nchance for multiple drops.","Даёт +"+n+" ☘ шахтёрской\nудачи, которая повышает\nшанс получить несколько предметов.");
        for(int n=1;n<=250;n++) {
            eq(db,"Grants +"+n+" Tuning Points.","Даёт +"+n+" "+choose(n,"очко","очка","очков")+" настройки.");
            eq(db,"Teleport "+n+" blocks ahead of you.","Телепортирует на "+n+" "+choose(n,"блок","блока","блоков")+" вперёд.");
            eq(db,"Each click allows you to climb "+n+" blocks, up to 999.","Каждое нажатие поднимает вас на "+n+" "+choose(n,"блок","блока","блоков")+", максимум — 999.");
            eq(db,"Every "+n+" pet levels, you gain +1","За повышение уровня питомца на "+n+" "+choose(n,"уровень","уровня","уровней")+" вы получаете +1");
            eq(db,"Maximum Capacity: "+n+" Pests","Максимальная вместимость: "+n+" "+choose(n,"вредитель","вредителя","вредителей"));
            eq(db,"Breathe "+n+"x as long underwater when","Позволяет дышать под водой в "+n+" "+choose(n,"раз","раза","раз")+" дольше, когда");
            check("genitive blocks, not blind substitution",TranslationGrammar.polish("в радиусе "+n+" блоков").equals("в радиусе "+n+" "+choose(n,"блока","блоков","блоков")));
        }
        eq(db,"Grants +2.5 Tuning Points.","Даёт +2.5 очка настройки.");
        eq(db,"Breathe 2.5x as long underwater when","Позволяет дышать под водой в 2.5 раза дольше, когда");
        check("fractional genitive blocks",TranslationGrammar.polish("в радиусе 2.5 блоков").equals("в радиусе 2.5 блока"));
        eq(db,"Garden!","Сад!"); eq(db,"dimension.","измерение."); eq(db,"Farming","Фермерство");
        eq(db,"Dwarven Metal Fortune","Удача добычи дварфских металлов");
        eq(db,"Heart of the Mountain XP","Опыт Сердца горы");
        eq(db,"The End Barn Skin","Облик амбара «Край»");
        eq(db,"Unrecognised item in an unknown dimension.","Unrecognised item in an unknown dimension.");
        Style grey=Style.EMPTY.withColor(0xaaaaaa), green=Style.EMPTY.withColor(0x55ff55).withBold(true)
            .withFont(new FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("witherskyru","inter_medium")))
            .withClickEvent(new ClickEvent.RunCommand("/unchanged")).withHoverEvent(new HoverEvent.ShowText(Component.literal("unchanged")));
        var source=List.of(FormattedCharSequence.forward("COMMON Test title",grey),
            FormattedCharSequence.composite(FormattedCharSequence.forward("Grants ",grey),FormattedCharSequence.forward("+12.5 ☘ Farming",green)),
            FormattedCharSequence.composite(FormattedCharSequence.forward("Fortune, ",green),FormattedCharSequence.forward("which increases your",grey)),
            FormattedCharSequence.forward("chance for multiple crops.",grey));
        var styled=TooltipTranslation.styled(source,true);
        check("Shift keeps original rarity/title",plain(styled.getFirst()).equals("COMMON Test title"));
        check("coloured stat gets correct case",plain(styled.get(1)).equals("Даёт +12.5 ☘ удачи"));
        check("coloured continuation coherent",plain(styled.get(2)).equals("фермерства, повышая шанс"));
        int statAt=plain(styled.get(1)).indexOf("удачи");
        // Native composite sequences report indexes local to each constituent.
        // Count emitted UTF-16 units independently when checking semantic spans.
        int[] statPosition={0},continuationPosition={0};
        styled.get(1).accept((i,s,cp) -> { if(Character.isDigit(cp)||statPosition[0]>=statAt||cp=='☘') check("stat styles/events preserved",s.equals(green));statPosition[0]+=Character.charCount(cp);return true; });
        styled.get(2).accept((i,s,cp) -> {check("stat, comma and prose keep their own actions",s.equals(continuationPosition[0]<"фермерства, ".length()?green:grey));continuationPosition[0]+=Character.charCount(cp);return true;});
        check("original styled source unchanged",plain(source.get(1)).equals("Grants +12.5 ☘ Farming"));
        check("early termination preserved",!styled.get(1).accept((i,s,cp) -> false));
        int[] glyphPosition={0};String statText=plain(styled.get(1));
        styled.get(1).accept((i,s,cp) -> { check("native composite glyph order and nonnegative fragment indexes",i>=0&&statText.codePointAt(glyphPosition[0])==cp);glyphPosition[0]+=Character.charCount(cp);return true;});
        check("complete glyph coverage",glyphPosition[0]==statText.length());
        List<Component> richComponents=List.of(Component.literal("Test title"),Component.literal("Anniversary Barn Skin on The"),Component.literal("Garden!").setStyle(green));
        var richResult=TooltipTranslation.components(richComponents,false);
        check("component context retains font and actions",richResult.get(2).getSiblings().getFirst().getStyle().equals(green));
        var mixed=List.of(FormattedCharSequence.forward("Test title",grey),FormattedCharSequence.forward("Find Enigma Souls across the Rift",grey),
            FormattedCharSequence.composite(FormattedCharSequence.forward("Dimen",grey),FormattedCharSequence.forward("sion.",green)));
        var mixedResult=TooltipTranslation.styled(mixed,false);
        check("unsupported style split rolls back whole coupled correction",plain(mixedResult.get(1)).equals(db.translate("Find Enigma Souls across the Rift")));
        check("unrelated Garden heading remains nominative",plain(TooltipTranslation.styled(List.of(FormattedCharSequence.forward("Title",grey),FormattedCharSequence.forward("Garden!",green)),false).get(1)).equals("Сад!"));
        Path overrides=Path.of(args[0]).resolve("proofread-custom-overrides"); Files.createDirectories(overrides);
        Files.writeString(overrides.resolve("overrides.tsv"),"Garden!\tМой Сад!\nGrants +21 Tuning Points.\tЛичная подпись\n");
        var custom=new OfflineDictionary(overrides); custom.setRecordUnknown(false);
        eq(custom,"Anniversary Barn Skin on The\nGarden!","юбилейный облик амбара в\nМой Сад!");
        eq(custom,"Grants +21 Tuning Points.","Личная подпись");
        System.out.println("PASS: independent proofreading " + checks + " assertions");
    }
}
