import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;

/** Known reforge/enchantment composition; never starts Minecraft or a connection. */
public class TestForgedItems {
    private static int checks;
    private static void check(String label, boolean value) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static void eq(OfflineDictionary db, String source, String target) {
        String actual = db.translate(source);
        check(source + " => " + actual + " != " + target, actual.equals(target));
    }
    private static void flag(String name, Object value) throws Exception {
        var field = SkyRuClient.class.getDeclaredField(name);
        field.setAccessible(true); field.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        var db = new OfflineDictionary(Path.of(args[0])); db.setRecordUnknown(false);
        eq(db, "Epic Void Sword", "Эпический Меч Пустоты");
        eq(db, "Fair Void Sword", "Добротный Меч Пустоты");
        eq(db, "Odd Void Sword", "Странный Меч Пустоты");
        eq(db, "Legendary Void Sword", "Легендарный Меч Пустоты");
        eq(db, "EPIC Void Sword", "EPIC Меч Пустоты");
        eq(db, "Epic Bow", "Эпический Лук");
        eq(db, "Legendary Bow", "Легендарный Лук");
        eq(db, "EPIC BOW", "EPIC ЛУК");
        eq(db, "LEGENDARY BOW", "LEGENDARY ЛУК");
        eq(db, "Giant Cleaver ✪✪✪", "Тесак великана ✪✪✪");
        eq(db, "Hyper Cleaver ✪✪✪", "Гипертесак ✪✪✪");
        eq(db, "Treacherous Shredder ✪✪✪", "Коварный Измельчитель ✪✪✪");
        eq(db, "Salty Yeti Rod ✪✪✪", "Солёная Удочка йети ✪✪✪");
        eq(db, "Fierce Armadillo Mask ✪✪✪", "Свирепая Маска броненосца ✪✪✪");
        eq(db, "§5Epic Void Sword ✪✪✪", "§5Эпический Меч Пустоты ✪✪✪");
        eq(db, "§5§lEpic Void Sword§r", "§5§lЭпический Меч Пустоты§r");
        eq(db, "Epic Amber Necklace", "Эпическое Янтарное ожерелье");
        eq(db, "Fair Angler Boots", "Добротные Ботинки рыболова");
        eq(db, "Robust Advanced Gardening Hoe", "Крепкая Продвинутая садовая мотыга");
        eq(db, "Robust Basic Gardening Hoe", "Крепкая Простая садовая мотыга");
        eq(db, "Sturdy Advanced Gardening Hoe", "Прочная Продвинутая садовая мотыга");
        eq(db, "Blessed Advanced Gardening Hoe", "Благословенная Продвинутая садовая мотыга");
        eq(db, "Bountiful Advanced Gardening Hoe", "Щедрая Продвинутая садовая мотыга");
        eq(db, "Robust Diamond Hoe", "Крепкая Алмазная мотыга");
        eq(db, "Robust Hoe of No Tilling", "Крепкая Мотыга без вспашки");
        eq(db, "Robust Binghoe", "Крепкая Бингомотыга");
        eq(db, "Deep Fried Fishing Rod", "Жареная во фритюре Удочка");
        eq(db, "Lumberjack's Advanced Gardening Axe", "Продвинутый садовый топор лесоруба");
        eq(db, "Greater Spook Amber Necklace", "Янтарное ожерелье великого ужаса");
        eq(db, "Green Thumb Advanced Gardening Hoe", "Продвинутая садовая мотыга «Зелёные пальцы»");
        eq(db, "Robust Totally Unknown ZX-9921", "Robust Totally Unknown ZX-9921");
        eq(db, "Unknown ZX9921 Advanced Gardening Hoe", "Unknown ZX9921 Advanced Gardening Hoe");
        eq(db, "RARE Robust Advanced Gardening Hoe", "RARE Крепкая Продвинутая садовая мотыга");
        eq(db, "§9Robust Advanced Gardening Hoe ✪✪§r", "§9Крепкая Продвинутая садовая мотыга ✪✪§r");
        eq(db, "Epic", "Epic"); eq(db, "Legendary", "Legendary");
        eq(db, "COMMON Enchanted Book", "COMMON Зачарованная книга");
        eq(db, "Enchanted Mithril", "Зачарованный мифрил");
        eq(db, "Enchanted Titanium", "Зачарованный титан");
        eq(db, "Epic Totally Unknown ZX-9921", "Epic Totally Unknown ZX-9921");
        eq(db, "› Cleave V→VI, Critical V→VII", "› Рассечение V→VI, Критический удар V→VII");
        eq(db, "› Sharpness V→VII, Thunderlord V→VII", "› Острота V→VII, Повелитель грома V→VII");
        eq(db, "› Bane Of Arthropods, Champion, Cubism", "› Бич членистоногих, Чемпион, Кубизм");
        eq(db, "› Divine Gift, Dragon Hunter", "› Божественный дар, Охотник на драконов");
        eq(db, "› Ultimate enchant, Vampirism, Venomous", "› Ультимативное зачарование, Вампиризм, Ядовитость");
        eq(db, "› Fire Aspect, Unknown ZX-9921", "› Fire Aspect, Unknown ZX-9921");
        eq(db, "Sharpness V→Unknown", "Sharpness V→Unknown");
        eq(db, "Sharpness V, please buy", "Sharpness V, please buy");
        eq(db, "/reforges", "/reforges");
        eq(db, "§c◆ Missing enchantments:", "§c◆ Не хватает зачарований:");
        eq(db, "Reforge Item", "Перековка предмета");
        eq(db, "Reforge Overlay", "Панель перековки");
        eq(db, "Reforging items adds a random\nmodifier to the item that grants stat\nboosts.",
            "Перековка добавляет случайный\nмодификатор предмета, повышающий\nхарактеристики.");
        eq(db, "Reforges the above item, giving it a\nrandom item modifier that boosts its\nstats.",
            "Перековывает предмет сверху, добавляя\nслучайный модификатор, повышающий\nхарактеристики.");
        for (String prefix : List.of("Epic", "Legendary", "Fair", "Odd")) {
            String stem = db.translate(prefix + " Void Sword");
            for (String decoration : List.of("", " ✪", " ✪✪✪✪✪", " ★")) {
                eq(db, prefix + " Void Sword" + decoration, stem + decoration);
                eq(db, "§5§l" + prefix + " Void Sword" + decoration + "§r", "§5§l" + stem + decoration + "§r");
            }
        }
        Path data = Path.of(args[1]).getParent().resolve("src/main/resources/data/witherskyru");
        Set<String> suffixes=new HashSet<>();
        for(String row:Files.readAllLines(data.resolve("reforge-placement.tsv")))
            if(!row.isBlank() && !row.startsWith("#")) suffixes.add(row.split("\t")[0]);
        String[] bases={"Void Sword","Advanced Gardening Hoe","Amber Necklace","Angler Boots"};
        for(String row:Files.readAllLines(data.resolve("item-gender-overrides.tsv"))) {
            if(row.isBlank() || row.startsWith("#")) continue;
            String[] entry=row.split("\t");
            String form=switch(entry[1]) {case "f"->"Крепкая";case "n"->"Крепкое";case "p"->"Крепкие";default->"Крепкий";};
            eq(db,"Robust "+entry[0],form+" "+db.translate(entry[0]));
        }
        for(String row:Files.readAllLines(data.resolve("reforges.tsv"))) {
            if(row.isBlank() || row.startsWith("#")) continue;
            String[] forms=row.split("\t");
            for(int i=0;i<bases.length;i++) {
                String base=db.translate(bases[i]);
                String expected=suffixes.contains(forms[0])?base+" "+forms[i+1]:forms[i+1]+" "+base;
                eq(db,forms[0]+" "+bases[i],expected);
                eq(db,"§9"+forms[0]+" "+bases[i]+" ✪§r","§9"+expected+" ✪§r");
            }
        }
        for (String line : Files.readAllLines(data.resolve("enchantments.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String name = line.split("\t")[0];
            String target = db.translate(name);
            for (String level : List.of(" I", " V", " VII", " X", " 10", " V→VII", " 1→10")) {
                eq(db, name + level, target + level);
                eq(db, "› " + name + level + ", Sharpness V→VII", "› " + target + level + ", Острота V→VII");
            }
        }
        flag("dictionary", db); flag("enabled", true); flag("onlyHypixel", false);
        Style enchanted = Style.EMPTY.withColor(0xaa00aa).withItalic(true)
            .withClickEvent(new ClickEvent.RunCommand("/reforges"))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("original hover")));
        Component original = Component.literal("Epic Void Sword").setStyle(enchanted);
        Component translated = SkyRuClient.component(original);
        check("enchanted component name", translated.getString().equals("Эпический Меч Пустоты"));
        check("style/click/hover retained", translated.getStyle().equals(enchanted));
        check("original component unchanged", original.getString().equals("Epic Void Sword"));
        var sequence = FormattedCharSequence.forward("Epic Void Sword", enchanted);
        var next = SkyRuClient.sequence(sequence);
        StringBuilder text = new StringBuilder();
        next.accept((i, style, point) -> {
            check("glyph style retained", style.equals(enchanted)); text.appendCodePoint(point); return true;
        });
        check("enchanted sequence name", text.toString().equals("Эпический Меч Пустоты"));
        Component hoe=Component.literal("Robust Advanced Gardening Hoe").setStyle(enchanted);
        Component hoeNext=SkyRuClient.component(hoe);
        check("screenshot title",hoeNext.getString().equals("Крепкая Продвинутая садовая мотыга"));
        check("screenshot style/actions",hoeNext.getStyle().equals(enchanted));
        check("screenshot original item unchanged",hoe.getString().equals("Robust Advanced Gardening Hoe"));
        check("Shift retains original title",TooltipTranslation.components(List.of(hoe),true).getFirst()==hoe);
        var search=SkyRuClient.searchName(hoe,"мотыга");
        check("reforged hoe Russian search",search.getString().toLowerCase(Locale.ROOT).contains("мотыга"));
        check("reforged hoe English search untouched",SkyRuClient.searchName(hoe,"Gardening Hoe")==hoe);
        for(String[] row:List.of(
            new String[]{"Salty Yeti Rod","Солёная Удочка йети"},
            new String[]{"Treacherous Shredder","Коварный Измельчитель"},
            new String[]{"Salty Rod of the Sea","Солёная Морская удочка"},
            new String[]{"Treacherous Hellfire Rod","Коварная Удочка адского пламени"},
            new String[]{"Stiff Auger Rod","Жёсткая Буровая удочка"},
            new String[]{"Magnetic Winter Rod","Магнитная Зимняя удочка"},
            new String[]{"Lucky Phantom Rod","Удачливая Фантомная удочка"},
            new String[]{"Epic Bow","Эпический Лук"},
            new String[]{"Legendary Bow","Легендарный Лук"}
        )) {
            int boundary=row[0].indexOf(' ');
            String encoded="§a"+row[0].substring(0,boundary)+" §b"+row[0].substring(boundary+1)+"§6 ✪✪✪";
            Component raw=Component.literal(encoded).setStyle(enchanted);
            List<Style> originalStyles=new ArrayList<>();
            net.minecraft.util.StringDecomposer.iterateFormatted(raw,Style.EMPTY,(i,s,cp)-> {
                for(int n=0;n<Character.charCount(cp);n++) originalStyles.add(s);return true;
            });
            Component shown=SkyRuClient.component(raw);
            check("legacy-coloured upgrade "+row[0],shown.getString().equals(row[1]+" ✪✪✪"));
            check("legacy-coloured source intact",raw.getString().equals(encoded));
            int[] index={0};
            net.minecraft.util.StringDecomposer.iterateFormatted(shown,Style.EMPTY,(i,s,cp)-> {
                int at=index[0];index[0]+=Character.charCount(cp);
                int color=at <= row[1].indexOf(' ')?0x55ff55:at < row[1].length()?0x55ffff:0xffaa00;
                Style expectedStyle=originalStyles.get(at <= row[1].indexOf(' ')?0:at < row[1].length()?boundary+1:row[0].length());
                check("legacy native color",s.getColor()!=null && s.getColor().getValue()==color);
                check("legacy styles/actions retained",s.equals(expectedStyle));return true;
            });
            check("legacy original under Shift",TooltipTranslation.components(List.of(raw),true).getFirst()==raw);
        }
        System.out.println("PASS: forged/enchanted items " + checks + " assertions");
    }
}
