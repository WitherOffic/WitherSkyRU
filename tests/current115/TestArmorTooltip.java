import java.nio.file.Path;
import java.util.List;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

/** Regression fixture from user's Ender Chestplate screenshot, no game startup. */
public class TestArmorTooltip {
    private static int checks;
    private static void check(String name, boolean value) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    private static void flag(String name, Object value) throws Exception {
        var field = SkyRuClient.class.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }
    private static String plain(FormattedCharSequence sequence) {
        StringBuilder out = new StringBuilder();
        sequence.accept((i, style, cp) -> { out.appendCodePoint(cp); return true; });
        return out.toString();
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        flag("dictionary", db); flag("enabled", true); flag("onlyHypixel", false);
        Style white = Style.EMPTY.withColor(0xaaaaaa);
        Style red = Style.EMPTY.withColor(0xff5555);
        Style blue = Style.EMPTY.withColor(0x55ffff);
        Style green = Style.EMPTY.withColor(0x55ff55);
        Style gold = Style.EMPTY.withColor(0xffff55);
        Style purple = Style.EMPTY.withColor(0xaa00aa);
        var title = SkyRuClient.sequence(FormattedCharSequence.forward("Light Ender Chestplate", purple));
        check("reforged armor translates", plain(title).equals("Лёгкий Эндерский нагрудник"));
        title.accept((i, style, cp) -> { check("title color", style.equals(purple)); return true; });
        String[][] equipment = {
            {"Fierce Strong Dragon Helmet", "Свирепый Шлем сильного дракона"},
            {"Fierce Superior Dragon Chestplate", "Свирепый Нагрудник превосходного дракона"},
            {"Fierce Shadow Assassin Leggings", "Свирепые Поножи теневого убийцы"},
            {"Fierce Angler Boots", "Свирепые Ботинки рыболова"},
            {"Fierce Reaper Mask", "Свирепая Маска жнеца"},
            {"Fierce Atomsplit Katana", "Свирепая Катана расщепления атомов"},
            {"Fierce Frozen Scythe", "Свирепая Ледяная коса"},
            {"Fierce Shadow Fury", "Свирепая Теневая ярость"},
            {"Fierce Crystallized Heart", "Свирепое Кристальное сердце"},
            {"Fierce Spider Queen's Stinger", "Свирепое Жало королевы пауков"},
            {"Fierce Wither Goggles", "Свирепые Очки иссушителя"},
            {"Fierce Mithril Coat", "Свирепое Мифриловое пальто"},
            {"Fierce Astraea", "Свирепая Астрея"},
            {"Fierce Scylla", "Свирепая Сцилла"},
            {"Fierce Valkyrie", "Свирепая Валькирия"},
            {"Fierce Pickonimbus 2000", "Свирепый Кирконимбус 2000"},
            {"Fierce Bingonimbus 2000", "Свирепый Бингонимбус 2000"},
            {"Fierce Mithril Drill SX-R226", "Свирепый Мифриловый бур SX-R226"},
            {"Fierce Necron's Blade (Unrefined)", "Свирепый Клинок Некрона (необработанный)"},
            {"Fierce Gauss Carrot Shovel Mk. III", "Свирепая Морковная лопата Гаусса, модификация III"},
            {"Fierce Yeti Rod", "Свирепая Удочка йети"},
            {"Fierce Shredder", "Свирепый Измельчитель"},
            {"Fierce Hellfire Rod", "Свирепая Удочка адского пламени"},
            {"Fierce Auger Rod", "Свирепая Буровая удочка"},
            {"Fierce Rod of the Sea", "Свирепая Морская удочка"},
            {"Fierce Armadillo Mask", "Свирепая Маска броненосца"},
            {"Fierce Frog Mask", "Свирепая Маска лягушки"},
            {"Fierce Gemstone Gauntlet", "Свирепая Рукавица драгоценных камней"},
            {"Fierce Amethyst Gauntlet", "Свирепая Аметистовая рукавица"},
            {"Fierce Pendant of Divan", "Свирепый Кулон Дивана"},
            {"Fierce AquaMaster™ X", "Свирепая Лейка AquaMaster™ X"},
            {"Fierce Abysmal Lasso", "Свирепое Бездонное лассо"}
        };
        Style nameStyle = purple.withItalic(true).withClickEvent(new ClickEvent.RunCommand("/viewrecipe"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover")));
        Style modifierStyle = red.withBold(true).withClickEvent(new ClickEvent.RunCommand("/reforges"));
        for (String[] item : equipment) {
            for (String mark : List.of("", " ✪✪✪", " ➎", " ★✦")) {
                check("title syntax " + item[0] + mark, db.translate(item[0] + mark).equals(item[1] + mark));
                String base = item[0].substring(7);
                // Separately coloured modifier and a name fragmented into many same-style siblings.
                MutableComponent source = Component.empty().append(Component.literal("Fierce").setStyle(modifierStyle))
                    .append(Component.literal(" ").setStyle(white));
                for (String letter : base.split("(?<=.)")) source.append(Component.literal(letter).setStyle(nameStyle));
                source.append(Component.literal(mark).setStyle(gold));
                Component translated = SkyRuClient.component(source);
                check("split components " + item[0], translated.getString().equals(item[1] + mark));
                check("item source unchanged", source.getString().equals(item[0] + mark));
                int[] cursor = {0};
                StringDecomposer.iterateFormatted(translated, Style.EMPTY, (runIndex, s, cp) -> {
                    int i = cursor[0]; cursor[0] += Character.charCount(cp);
                    if (i < item[1].indexOf(' ')) check("modifier style/actions", s.equals(modifierStyle));
                    else if (i > item[1].indexOf(' ') && i < item[1].length()) check("name style/actions", s.equals(nameStyle));
                    else if (i >= item[1].length()) check("upgrade style", s.equals(gold));
                    return true;
                });
                var sequence = FormattedCharSequence.composite(FormattedCharSequence.forward("Fierce", modifierStyle),
                    FormattedCharSequence.forward(" ", white), FormattedCharSequence.forward(base, nameStyle),
                    FormattedCharSequence.forward(mark, gold));
                check("styled title " + item[0], plain(SkyRuClient.sequence(sequence)).equals(item[1] + mark));
                check("Shift original component", TooltipTranslation.components(List.of(source),true).getFirst()==source);
                check("Shift original sequence", plain(TooltipTranslation.styled(List.of(sequence),true).getFirst()).equals(item[0]+mark));
                check("protected Shift survives rendering", plain(SkyRuClient.sequence(TooltipTranslation.styled(List.of(sequence),true).getFirst())).equals(item[0]+mark));
            }
        }
        Component translatedKey = Component.translatable("item.minecraft.diamond_sword");
        check("vanilla translatable key retained", SkyRuClient.component(translatedKey)==translatedKey);
        Component unknown = Component.literal("ZX-9910 Equipment").setStyle(nameStyle);
        check("unknown item not guessed", SkyRuClient.component(unknown)==unknown);
        check("command not an item", db.translate("/give Fierce Shadow Fury").equals("/give Fierce Shadow Fury"));
        check("rarity retained with upgraded name", db.translate("EPIC Fierce Mithril Drill SX-R226 ✪✪✪")
            .equals("EPIC Свирепый Мифриловый бур SX-R226 ✪✪✪"));
        String[][] stats = {{"Health", "Здоровье", "+120 (+15)"}, {"Defense", "Защита", "+64 (+4)"},
            {"Crit Chance", "Шанс критического удара", "+2% (+2%)"},
            {"Crit Damage", "Критический урон", "+4% (+4%)"}, {"Speed", "Скорость", "+4 (+4)"}};
        for (String[] row : stats) {
            var original = FormattedCharSequence.composite(FormattedCharSequence.forward(row[0] + ": ", white),
                FormattedCharSequence.forward(row[2], red));
            var translated = SkyRuClient.sequence(original);
            check("stat translates " + row[0], plain(translated).equals(row[1] + ": " + row[2]));
            check("original intact", plain(original).equals(row[0] + ": " + row[2]));
            translated.accept((i, style, cp) -> { check("stat styles retained", style.equals(white) || style.equals(red)); return true; });
        }
        var rebound = FormattedCharSequence.composite(
            FormattedCharSequence.forward("Gives ", white), FormattedCharSequence.forward("50% ", green),
            FormattedCharSequence.forward("chance to rebound ", white), FormattedCharSequence.forward("9%", green));
        String reboundText = plain(SkyRuClient.sequence(rebound));
        check("rebound fragment translates: " + reboundText, reboundText.equals("Даёт 50% шанса отразить 9%"));
        var multiply = FormattedCharSequence.composite(FormattedCharSequence.forward("multiplied by ", white),
            FormattedCharSequence.forward("2x", purple), FormattedCharSequence.forward(" while on the End", white));
        String text = plain(SkyRuClient.sequence(multiply));
        check("multiplication fragment translates", text.startsWith("умножаются на 2x"));
        check("multiplier retained", text.contains("2x"));
        for (String[] row : List.of(new String[]{"✦ Missing enchantments: ", "✦ Не хватает зачарований: ", "11", "(hold Shift)"},
            new String[]{"✦ Not maxed: ", "✦ Не улучшено до предела: ", "2", "(hold Shift)"})) {
            var source = FormattedCharSequence.composite(FormattedCharSequence.forward(row[0], blue),
                FormattedCharSequence.forward(row[2] + " ", gold), FormattedCharSequence.forward(row[3], blue));
            check("upgrade hint translates", plain(SkyRuClient.sequence(source)).equals(row[1] + row[2] + " (удерживайте Shift)"));
        }
        check("soulbound translates", db.translate("* Co-op Soulbound *").contains("Привязан к кооперативу"));
        check("creation label translates, timestamp retained", db.translate("Created at: 2026-10-07 03:11:46 GMT+07:00")
            .equals("Дата создания: 2026-10-07 03:11:46 GMT+07:00"));
        check("rarity remains English", db.translate("EPIC CHESTPLATE").equals("EPIC НАГРУДНИК"));
        check("tooltip does not reject long Russian stat", !HudTextFit.fixedWidth(false, true));
        check("tooltip wins over incidental HUD frame", !HudTextFit.fixedWidth(true, true));
        System.out.println("PASS: armor tooltip regression " + checks + " assertions");
    }
}
