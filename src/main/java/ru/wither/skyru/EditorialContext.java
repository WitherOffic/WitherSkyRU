package ru.wither.skyru;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Closed neighbouring-line fixes, never guessed prose or global replacements of headings. */
public final class EditorialContext {
    private EditorialContext() {}
    public record Edit(int row, String before, String after, boolean optional) {
        public Edit(int row, String before, String after) { this(row, before, after, false); }
    }
    private static final Pattern FARMING = Pattern.compile("^Grants \\+[0-9]+(?:[.,][0-9]+)? ☘ Farming$");
    private static final Pattern MINING = Pattern.compile("^Grants \\+[0-9]+(?:[.,][0-9]+)? ☘ Mining$");
    private static final Pattern GARDEN_SKIN = Pattern.compile("^.+ (?:Barn Skin|Greenhouse Skin|Skin) on The$");
    private static final Pattern CONTEXT_MATCH_0=Pattern.compile("Time Held: [0-9]+ Minutes");
    private static final Pattern CONTEXT_MATCH_1=Pattern.compile("The composter must have [0-9][0-9,.]*");
    private static final Pattern CONTEXT_MATCH_2=Pattern.compile("Applies the [A-Za-z]+ reforge when");
    private static final Pattern CONTEXT_MATCH_3=Pattern.compile("After (?:15|30|60)m, a unique  Critter will be");
    private static final Pattern CONTEXT_MATCH_4=Pattern.compile("Grants \\+[0-9]+(?:[.,][0-9]+)? Treasure Chance while");
    private static final Pattern CONTEXT_MATCH_5=Pattern.compile("Gain \\+[0-9]+(?:[.,][0-9]+)?☯ Farming Wisdom and");
    private static final Pattern CONTEXT_MATCH_6=Pattern.compile("\\+[0-9]+(?:[.,][0-9]+)?☘ Farming Fortune\\.");
    // Prepared once; no paragraph/sub-list allocation on every tooltip frame.
    private record Closed(List<String> source,int row,String before,String after) {}
    private static final List<Closed> CLOSED=List.of(
        new Closed(List.of("The top players who increase","their collection the most earn","Jacob's Tickets and unique","rewards!"),3,"Награды!","награды!"),
        new Closed(List.of("Purchasing slots for Coins from","Jacobus in the Combat Settlement in","the Hub."),2,"Хаб.","в Хабе."),
        new Closed(List.of("Stores up to 20% of your Co-op","account."),1,"account.","счёта."),
        new Closed(List.of("Holds various items obtained from","Foraging! Items you pickup go","directly into your sacks."),0,"Хранит различные предметы, полученные из","Хранит ресурсы, полученные при"),
        new Closed(List.of("Holds various items obtained from","Foraging! Items you pickup go","directly into your sacks."),2,"прямо в ваши мешки.","сразу попадают в ваши мешки."),
        new Closed(List.of("While on Foraging Islands, throw your Axe at a","tree! It will sweep 50% of the logs it normally","would!"),2,"обычно!","числа брёвен."),
        new Closed(List.of("Requires an open area to place","trees. Minions also work when you","are offline!"),0,"Для установки требуется свободное место","Нужно свободное место для выращивания"),
        new Closed(List.of("This item can be used as a minion","upgrade."),1,"Улучшить.","улучшение миньона."),
        new Closed(List.of("Automatically smelts materials","produced by minions before","compacting them into their enchanted","form."),3,"форму.","предметы."),
        new Closed(List.of("This item can be used as a minion","upgrade. Prevents your minions from","getting distracted by flies."),0,"Этот предмет можно использовать как","Используется как улучшение миньона."),
        new Closed(List.of("This item can be used as a minion","upgrade. Place it in any mob","spawning minion and it will turn","nearby mobs into their corrupted","forms."),0,"Этот предмет можно использовать как","Улучшение для миньона."),
        new Closed(List.of("This item can be placed inside any","minion. Automatically sells generated","items when the minion has no space."),0,"Этот предмет можно поместить в любой","Установите этот предмет в любого"),
        new Closed(List.of("Items are sold for 50% of their","selling price."),1,"цену продажи.","цены продажи."),
        new Closed(List.of("Items are sold for 70% of their","selling price."),1,"цену продажи.","цены продажи."),
        new Closed(List.of("While equipped on a Voidling minion,","output is increased by 3% per minion","level."),2,"уровня.","уровень этого миньона.")
    );
    public static List<Edit> edits(List<String> source) {
        List<Edit> edits = new ArrayList<>();
        for(var paragraph:CLOSED)closedMinion(source,edits,paragraph.source(),paragraph.row(),paragraph.before(),paragraph.after());
        // This timer measures inventory storage, not time spent holding the item.
        for(int i=0;i+5<source.size();i++){
            if(source.get(i).equals("The longer you hold this in your")
                &&source.get(i+1).equals("inventory, the stronger you'll")
                &&source.get(i+2).equals("become for 2 minutes after removing")
                &&source.get(i+3).equals("it from your inventory! Max +50.")
                &&source.get(i+4).isEmpty()&&CONTEXT_MATCH_0.matcher(source.get(i+5)).matches())
            {
                edits.add(new Edit(i+5,"Время в руках:","Время хранения:",true));
                edits.add(new Edit(i+5,"Time Held: ","Время хранения: ",true));
                edits.add(new Edit(i+5," Minutes"," минут",true));
            }
        }
        for(int i=0;i+1<source.size();i++){
            if(source.get(i).equals("❣ Requires Master Mode The Catacombs Floor IV")&&source.get(i+1).equals("Completion."))
                edits.add(new Edit(i+1,"Выполнение.",""));
            if(source.get(i).equals("Berserk: +10 Damage, +75 Strength, +10")&&source.get(i+1).equals("Speed"))
                edits.add(new Edit(i+1,"Скорость","скорости"));
            if(source.get(i).equals("Tank: +100 Defense, +5 Vitality, +5 True")&&source.get(i+1).equals("Defense"))
                edits.add(new Edit(i+1,"Защита","защиты"));
            // One readable sentence replaces a redundant wrapped noun, retaining
            // -2% verbatim and the original two-row tooltip structure.
            if(source.get(i).equals("Take -2%  Damage from  Undead")&&source.get(i+1).equals("mobs."))
                edits.add(new Edit(i+1,"существ.",""));
            if(source.get(i).equals("Grants +10 Mending while in")&&source.get(i+1).equals(" Dungeons."))
                edits.add(new Edit(i+1,"Подземелья.","подземельях."));
        }
        // Shared standalone words remain headings elsewhere; only these complete
        // observed Dungeon item paragraphs inflect their last row.
        for(int i=0;i+2<source.size();i++){
            if(source.get(i).equals("Your Critical Hits deal 100% more")
                &&source.get(i+1).equals("damage if you are behind your")&&source.get(i+2).equals("target."))
                edits.add(new Edit(i+2,"цель.","цели."));
            if(source.get(i).equals("➤ The damage blocked scales with")
                &&source.get(i+1).equals("your Dungeon Stat Boost while in The")&&source.get(i+2).equals("Catacombs."))
                edits.add(new Edit(i+2,"Катакомб.","Катакомбах."));
            if(source.get(i).equals("Allows you to open an additional")
                &&source.get(i+1).equals("treasure chest at the end of a")&&source.get(i+2).equals("dungeon!"))
                edits.add(new Edit(i+2,"Подземелье!","подземелья!"));
            if(i+4<source.size()&&source.get(i).equals("Place an explosive trap that")
                &&source.get(i+1).equals("triggers when mobs walk over it")
                &&source.get(i+2).equals("dealing 20,000-250,000 damage")
                &&source.get(i+3).equals("based on the dungeon floor you are")&&source.get(i+4).equals("on."))
                edits.add(new Edit(i+4,"Вкл.","подземелья."));
        }
        // The whole observed trap paragraph makes the subject of "catch it" explicit.
        for (int i = 0; i + 2 < source.size(); i++) {
            if (!source.get(i).equals("Traps can be placed near the spawn")
                || !source.get(i+1).equals("location of a Shard creature, with")
                || !source.get(i+2).equals("time it will eventually catch it.")) continue;
            edits.add(new Edit(i, "Ловушки можно устанавливать у мест появления", "Ловушки можно ставить рядом с местом"));
            edits.add(new Edit(i+1, "существ-осколков; спустя", "появления существа-осколка; со временем"));
            edits.add(new Edit(i+2, "некоторое время они будут пойманы.", "ловушка поймает его."));
        }
        // The inventory lore splits grammar across differently coloured runs. Only
        // this complete, reviewed source paragraph may inflect its named terms.
        for (int i = 0; i + 3 < source.size(); i++) {
            if (!source.get(i).strip().equals("Fill your composter with Crops, like")
                || !source.get(i + 1).strip().equals("Wheat or Enchanted Potato, to turn")
                || !source.get(i + 2).strip().equals("them into Organic Matter. Organic")
                || !source.get(i + 3).strip().equals("matter is used to create Compost.")) continue;
            edits.add(new Edit(i, "Культуры", "культурами", true));
            edits.add(new Edit(i, ", like", ", например", true));
            edits.add(new Edit(i + 1, "Пшеница", "пшеницей", true));
            edits.add(new Edit(i + 1, "Wheat or ", "пшеницей или ", true));
            edits.add(new Edit(i + 1, "Зачарованный картофель", "зачарованным картофелем", true));
            edits.add(new Edit(i + 1, "to turn", "чтобы превратить", true));
            edits.add(new Edit(i + 2, "them into", "их в", true));
            edits.add(new Edit(i + 2, "Органическое сырьё", "органическое сырьё", true));
            edits.add(new Edit(i + 2, ". Organic", ".", true));
            edits.add(new Edit(i + 3, "matter is used to create", "Из него производится", true));
            edits.add(new Edit(i + 3, "Вещество используется для создания", "Из него производится", true));
            edits.add(new Edit(i + 3, "Компост", "компост", true));
        }
        for (int i = 0; i + 2 < source.size(); i++) {
            if (source.get(i).equals("Impacts your Minions, Chocolate")
                && source.get(i+1).equals("Factory, Aging Items, Kat, Fann,")
                && source.get(i+2).equals("Greenhouse, and The Forge.")) {
                edits.add(new Edit(i+1,"фабрики","фабрику",true));
                edits.add(new Edit(i+1,"выдерживаемых предметов","выдерживаемые предметы",true));
                edits.add(new Edit(i+2,"теплицы","теплицу",true));
                edits.add(new Edit(i+2,"кузницы","кузницу",true));
            }
            if (source.get(i + 1).strip().equals("organic matter stored to start")
                && source.get(i + 2).strip().equals("creating compost.")
                && CONTEXT_MATCH_1.matcher(source.get(i).strip()).matches())
                edits.add(new Edit(i + 2, "производство компоста.", "производства компоста.", true));
        }
        for (int i = 1; i < source.size(); i++) {
            String previous = source.get(i - 1).strip(), current = source.get(i).strip();
            if (i >= 2 && source.get(i-2).equals("Grants +25 Mining Spread while")
                && previous.equals("Mayor Cole's Mining Fiesta perk is") && current.equals("active.")) {
                edits.add(new Edit(i-2,"добычи","добычи,"));
                edits.add(new Edit(i-1,"бонус","пока бонус"));
            }
            if (previous.equals("Grants +50%  Mining Spread") && current.equals("towards Blocks."))
                edits.add(new Edit(i,"к блокам.","при добыче блоков."));
            if (i >= 2 && source.get(i-2).equals("Hits through Piercing deal +7.5%")
                && previous.equals("damage than the previous one, up to") && current.equals("5 victims."))
                edits.add(new Edit(i-1,"урона, чем предыдущий, вплоть до","больше урона, чем предыдущее; не более"));
            if (i >= 2 && source.get(i-2).equals("Convert every {1} Speed you")
                && previous.equals("have above 100 into +1 Block") && current.equals("Fortune.")) {
                edits.add(new Edit(i-1,"блочной","удачи добычи"));
                edits.add(new Edit(i,"удачи.","блоков."));
            }
            if (previous.equals("This arrow explodes after 2s or on") && current.equals("impact."))
                edits.add(new Edit(i,"попадания.","попадании."));
            if (previous.equals("The explosion deals the arrow's") && current.equals("damage split to mobs within 2.5 blocks.")) {
                edits.add(new Edit(i-1,"наносит","распределяет"));
                edits.add(new Edit(i,"урона, распределённого между","между"));
            }
            if (current.equals("combined with a Mining Tool.") && CONTEXT_MATCH_2.matcher(previous).matches())
                edits.add(new Edit(i, "применить", "применении"));
            if (i >= 2 && source.get(i - 2).strip().equals("Put a bunch of Amber together and")
                && previous.equals("it'll make more Amber - Throin the") && current.equals("Forger.")) {
                edits.add(new Edit(i - 2, " и", ", и", true));
                edits.add(new Edit(i - 1, "он произведёт", "получится", true));
                edits.add(new Edit(i, "кузнеца.", "кузнец."));
            }
            if (previous.equals("Find Goblins from mining 40% more") && current.equals("often."))
                edits.add(new Edit(i, "часто.", "чаще."));
            if (i >= 2 && source.get(i - 2).strip().equals("Combine x9 of this item at the")
                && previous.equals("Thaumaturgist in the Hub to unlock the"))
            {
                edits.addAll(PowerStoneTranslation.edits(i,current));
            }
            if (previous.equals("Accessories have never tasted so") && current.equals("sweet."))
                edits.add(new Edit(i,"сладкий.","сладкими."));
            if (i>=2 && source.get(i-2).equals("Has to be handled with gloves- or")
                && previous.equals("the user will quickly develop") && current.equals("frostbite.")) {
                edits.add(new Edit(i-2,"Нужно брать в перчатках — или","Берите только в перчатках — иначе"));
                edits.add(new Edit(i,"Обморожение.","обморожение."));
            }
            if (previous.equals("Grants +1 extra Accessory Bag") && current.equals("slots.")) {
                edits.add(new Edit(i - 1, "Добавляет ", "Увеличивает на ", true));
                edits.add(new Edit(i - 1, "Даёт ", "Увеличивает на ", true));
                edits.add(new Edit(i - 1, "в ", "вместимость ", true));
                edits.add(new Edit(i - 1, "extra ", "вместимость ", true));
                edits.add(new Edit(i - 1, "сумку аксессуаров", "сумки аксессуаров", true));
                edits.add(new Edit(i - 1, "Сумка аксессуаров", "сумки аксессуаров", true));
                edits.add(new Edit(i - 1, ":", "", true));
                edits.add(new Edit(i, "ячейки.", "в ячейках."));
            }
            if (i >= 2 && source.get(i - 2).strip().equals("Use on a Tree with an active Tree")
                && previous.equals("Protection Order to lather it with") && current.equals("Honeycomb!"))
                edits.add(new Edit(i, "Медовые соты", "медовыми сотами"));
            if (i >= 2 && source.get(i - 2).strip().equals("Upon use, a unique  Critter will")
                && previous.equals("INSTANTLY be attracted to the") && current.equals("Honeycomb!"))
                edits.add(new Edit(i, "Медовые соты", "медовым сотам"));
            if (current.equals("attracted to the Honeycomb!") && CONTEXT_MATCH_3.matcher(previous).matches()) {
                edits.add(new Edit(i - 1, "мин", " мин", true));
                edits.add(new Edit(i - 1, "появится ", "", true));
                edits.add(new Edit(i - 1, ",", "", true));
                edits.add(new Edit(i, "привлечённых", "придёт к", true));
                edits.add(new Edit(i, "медовыми сотами", "медовым сотам", true));
            }
            if (previous.equals("Click another player to initiate a") && current.equals("game of Four in a Row!"))
                edits.add(new Edit(i, "игра", "игру"));
            if (current.equals("underwater.") && CONTEXT_MATCH_4.matcher(previous).matches())
                edits.add(new Edit(i - 1, "шанса сокровищ,", "шанса сокровищ"));
            if (previous.equals("Cleans a 5x5 area of grass, leaves") && current.equals("and flowers."))
                edits.add(new Edit(i, "и цветы.", "и цветов."));
            if (previous.startsWith("Gain +") && CONTEXT_MATCH_5.matcher(previous).matches()
                && CONTEXT_MATCH_6.matcher(current).matches())
                edits.add(new Edit(i, "Удача фермерства", "удачи фермерства", true));
            if (previous.equals("Adds +3,000 Fuel to a refuelable") && current.equals("machine."))
                edits.add(new Edit(i - 1, "заправляемый", "заправляемое", true));
            if (current.equals("Fortune, which increases your")) {
                boolean farming=FARMING.matcher(previous).matches(),mining=MINING.matcher(previous).matches();
                if (farming) edits.add(new Edit(i - 1, "Фермерство", "фермерской"));
                if (mining) edits.add(new Edit(i - 1, "Горное дело", "шахтёрской"));
                if (farming || mining) {
                    edits.add(new Edit(i, "Удача,", "удачи,", true));
                    edits.add(new Edit(i, "which increases your", "которая повышает", true));
                }
            }
            if (previous.equals("Find Enigma Souls across the Rift") && current.equals("Dimension.")) {
                edits.add(new Edit(i - 1, "Разлому", "измерению"));
                edits.add(new Edit(i, "измерении.", "Разлома."));
            }
            if (previous.equals("Every 4 Souls, go back to Enigma for") && current.equals("more upgrades."))
                edits.add(new Edit(i, "больше улучшений.", "новыми улучшениями."));
            if (previous.equals("with special abilities from another") && current.equals("dimension."))
                edits.add(new Edit(i, "измерение.", "измерения."));
            if (previous.equals("You take +50% damage while holding") && current.equals("this weapon."))
                edits.add(new Edit(i, "этого оружия.", "это оружие."));
            if (previous.equals("Upgrade your Eagle Pet in the Safari") && current.equals("Essence Shop!")) {
                edits.add(new Edit(i - 1, "в сафари", "в магазине"));
                edits.add(new Edit(i, "Магазин эссенции!", "эссенции Сафари!"));
            }
            if (current.equals("Garden!") && GARDEN_SKIN.matcher(previous).matches())
                edits.add(new Edit(i, "Сад!", "Саду!"));
            if (previous.equals("lots of times. Grants 2x stats on The") && current.equals("Catacombs Floor VII."))
                edits.add(new Edit(i, "этажа VII Катакомб.", "VII этаже Катакомб."));
            if (previous.equals("Trust no one.") && current.equals("Not even your own shadow."))
                edits.add(new Edit(i, "Даже собственная тень.", "Даже собственной тени."));
            if (previous.equals("Not even your own shadow.") && current.equals("It's always acting shady."))
                edits.add(new Edit(i, "Он всегда", "Она всегда"));
            if (previous.equals("defeated, feared by anything living") && current.equals("AND dead."))
                edits.add(new Edit(i, "И мёртвым.", "и мёртвому."));
        }
        return edits;
    }
    /** Used by plain multiline strings too; single unrelated fragments remain untouched. */
    private static void closedMinion(List<String> source,List<Edit> edits,List<String> paragraph,int row,String before,String after){
        for(int start=0;start+paragraph.size()<=source.size();start++){
            boolean same=true;for(int n=0;n<paragraph.size();n++)if(!source.get(start+n).equals(paragraph.get(n))){same=false;break;}
            if(same)edits.add(new Edit(start+row,before,after));
        }
    }

    public static List<String> apply(List<Edit> edits, List<String> translated) {
        List<String> out = new ArrayList<>(translated);
        for (Edit edit : edits) {
            String row = out.get(edit.row());
            int at = row.indexOf(edit.before());
            if (at < 0 && edit.optional()) continue;
            if (at < 0 || row.indexOf(edit.before(), at + edit.before().length()) >= 0) return translated;
            out.set(edit.row(), row.substring(0, at) + edit.after() + row.substring(at + edit.before().length()));
        }
        return out;
    }
}
