package ru.wither.skyru;

import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.*;

/** Closed reviewed paragraphs, with semantic styles rather than per-run word guesses. */
final class ReviewedLoreStyles {
    private record Part(int source,String target) {}
    private static boolean parameter(String s){return s.length()==3&&s.charAt(0)=='{'&&s.charAt(1)>='0'&&s.charAt(1)<='9'&&s.charAt(2)=='}';}
    private record Row(List<String> source,List<Part> target,String text,Pattern numeric,String prefix,String suffix,String constantTarget) {
        Row(List<String> source,List<Part> target){this(source,target,String.join("",source),numericPattern(source),numericPrefix(source),numericSuffix(source),constantTarget(source,target));}
        private static String constantTarget(List<String> source,List<Part> target){
            for(String piece:source)if(parameter(piece))return null;
            var text=new StringBuilder();for(var part:target)text.append(part.target());return text.toString();
        }
        private static Pattern numericPattern(List<String> source){
            if(source.stream().noneMatch(ReviewedLoreStyles::parameter))return null;
            StringBuilder regex=new StringBuilder("^");
            for(String piece:source)regex.append(parameter(piece)
                ?"([0-9]{1,8}(?:[.,][0-9]{1,6})?)":Pattern.quote(piece));
            return Pattern.compile(regex.append('$').toString());
        }
        private static String numericPrefix(List<String> source){
            var prefix=new StringBuilder();
            for(String piece:source){if(parameter(piece))break;prefix.append(piece);}
            return prefix.toString();
        }
        private static String numericSuffix(List<String> source){
            int start=source.size();
            while(start>0&&!parameter(source.get(start-1)))start--;
            return String.join("",source.subList(start,source.size()));
        }
        Row bind(String actual){
            if(text().equals(actual))return this;
            if(!shape(actual))return null;
            var match=numeric.matcher(actual);
            if(!match.matches())return null;
            var boundSource=new ArrayList<String>();var replacements=new HashMap<Integer,String>();int capture=1;
            for(int i=0;i<source.size();i++){
                String piece=source.get(i);String value=parameter(piece)?match.group(capture++):piece;
                boundSource.add(value);if(!value.equals(piece))replacements.put(i,value);
            }
            var boundTarget=new ArrayList<Part>();
            for(var part:target)boundTarget.add(new Part(part.source(),replacements.containsKey(part.source())
                && part.target().equals(source.get(part.source()))?replacements.get(part.source()):part.target()));
            // A successful bind is concrete; do not rebuild its template regex/prefix.
            // Runtime numeric bindings do not build/retain a second target string.
            return new Row(List.copyOf(boundSource),List.copyOf(boundTarget),actual,null,actual,"",null);
        }
        boolean shape(String actual){return text.equals(actual)||actual.length()<=400&&numeric!=null&&actual.startsWith(prefix)&&actual.endsWith(suffix);}
    }
    private record Match(Row row) {}
    // Only known partially translated source pieces, prepared once with the index.
    private static final Map<String,String> ENGLISH_ALIASES=Map.ofEntries(
        Map.entry("Защита","Defense"),Map.entry("Повторение","Repeat"),Map.entry("и","and"),Map.entry("Суперпары","Superpairs"),
        Map.entry("Сад","Garden"),Map.entry("Культуры","Crops"),Map.entry("игроки","players"),Map.entry("Игроки","players"),
        Map.entry("Увеличивает скорость работы","Increases the speed of"),Map.entry("часов","hours"),Map.entry("минут","minutes"));
    private static Row row(String... pairs){
        var source=new ArrayList<String>();var target=new ArrayList<Part>();
        for(int i=0;i<pairs.length;i+=2){source.add(pairs[i]);target.add(new Part(i/2,pairs[i+1]));}
        return new Row(List.copyOf(source),List.copyOf(target));
    }
    private static Row healingCooldown(){
        return new Row(List.of("(","{0}","s"," ","Cooldown",")."),List.of(
            new Part(0,"("),new Part(4,"Перезарядка"),new Part(3,": "),new Part(1,"{0}"),
            new Part(3," "),new Part(2,"с"),new Part(5,".)")));
    }
    private static List<Row> lethalityRows(String defense,String icon,boolean wide){
        if(wide)return List.of(
            row("Reduces the ","Снижает ",icon,icon," "," ",defense,"защиту"," "," ","of your","цели"),
            row("target by ","на ","{0}","{0}","%","%"," "," ","for ","на ","{1}","{1}","s"," с"," "," ","each time you","при каждом"),
            row("hit them with melee. Stacks up to ","ударе ближнего боя. Складывается до ","{0}","{0}"),row("times.","раз."));
        return List.of(row("Reduces the ","Снижает ",icon,icon," "," ",defense,"защиту"," ","","of",""),
            row("your target by ","цели на ","{0}","{0}","%","%"," "," ","for ","на ","{1}","{1}","s"," с"),
            row("each time you hit them with","при каждом ударе"),
            row("melee. Stacks up to ","ближнего боя. Складывается до ","{0}","{0}"," "," ","times.","раз."));
    }
    private static List<Row> chronomatron(String repeat){
        return List.of(row(repeat,"Повторяйте"," "," ","the musical ","","pattern","мелодию,"," "," ","to form the","чтобы составить"),
            row("longest chain of notes.","самую длинную цепочку нот."));
    }
    private static List<Row> addonReward(String and,String pairs){
        return List.of(row("Earn extra ","Даёт дополнительный ","Enchanting Exp","опыт зачарования"," "," ",and,"и"),
            row("extra clicks on your next","дополнительные ходы в следующих"),row(pairs,"Суперпарах","!","!"));
    }
    private static List<Row> gardenWarp(String garden,String crops){
        return List.of(row("Teleports you to your ","Перемещает в ваш ",garden,"Сад",",",","),
            row("where you can grow and harvest","где можно выращивать и собирать"),
            row(crops,"урожай"," "," ","and sell them to ","и продавать его ","customers","посетителям",".","."));
    }
    private static List<Row> guesting(String players){
        return List.of(row("Learn all about how to ","Узнайте, как с помощью ","/visit","/visit"," "," ",players,"посещать игроков"),
            row("across the ","в мире ","SkyBlock","SkyBlock"," universe!","!"));
    }
    private static List<Row> minionFuel(String head,String unit,boolean oldWrap){
        String ru=unit.equals("hours")||unit.equals("часов")?"ч":"мин";
        if(oldWrap)return List.of(row(head,"Ускоряет работу миньона"),
            row("your minion by ","на ","{0}","{0}","%","%"),
            row("for ","в течение ","{0}","{0}"," "," ",unit,ru,"!","!"));
        return List.of(row(head,"Ускоряет работу миньона"),
            row("your minion by ","на ","{0}","{0}","%","%"," for "," в течение ","{1}","{1}"),
            row(unit,ru,"!","!"));
    }
    private static List<Row> critical(String icon,boolean wrapped){
        if(wrapped)return List.of(row("Increases ","Увеличивает ",icon,icon," "," ","Crit Damage","критический урон"," "," ","by","на"),row("{0}","{0}","%","%",".","."));
        return List.of(row("Increases ","Увеличивает ",icon,icon," "," ","Crit Damage","критический урон"," "," ","by ","на ","{0}","{0}","%","%",".","."));
    }
    private static List<Row> oldCubism(String first,String second,String last){
        return List.of(row("Increases damage dealt to","Увеличивает урон по"),
            row(first,first.equals("Magma Cubes")?"магмовым кубам":first.equals("Creepers")?"криперам":"слизням",", ",", ",second,second.equals("Magma Cubes")?"магмовым кубам":"слизням",", and",", а также"),
            row(last,last.equals("Creepers")?"криперам":"магмовым кубам"," by "," на ","{0}","{0}","%","%",".","."));
    }
    private static final List<List<Row>> BASE_GROUPS=List.of(
        List.of(row("Every ","После каждых ","{0}","{0}"," "," ","hits on a monster,","попаданий по монстру"),
            row("strike ","","lightning","молния",", dealing"," наносит"),row("{0}","{0}","%","%"," "," ","of the hit's damage.","урона этого удара.")),
        List.of(row("Increases damage dealt to ","Увеличивает урон по ","⚓","⚓"),
            row("Aquatic","водным"," mobs by "," мобам на ","{0}","{0}","%","%",".",".")),
        List.of(row("Reduces the target's walk speed by","Замедляет передвижение цели на"),
            row("{0}","{0}","%","%"," "," ","and deals ","и наносит ","+","+","{1}","{1}","%","%"," "," ","of your damage","вашего урона"),
            row("per second per hit, stacking globally","в секунду за попадание. Общий предел —"),
            row("up to ","","{0}","{0}"," "," ","hits. Lasts ","попаданий. Длительность: ","{1}","{1}","s"," с",".",".")),
        List.of(row("Increases the chance of a","Повышает шанс выпадения"),
            row("Monster dropping an item by","предмета с монстра на"),row("{0}","{0}","%","%",".",".")),
        List.of(row("Increases damage dealt by ","Увеличивает урон на ","{0}","{0}","%","%"),
            row("for each percent of health","за каждый процент здоровья,"),row("missing on your target.","недостающего цели.")),
        List.of(row("Increases damage dealt by ","Увеличивает урон на ","{0}","{0}","%","%"),
            row("for each percent of health","за каждый процент здоровья,"),row("your target has.","оставшегося у цели.")),
        List.of(row("Increases damage dealt by ","Увеличивает урон на ","{0}","{0}","%","%"),
            row("for every ","за каждые ","100","100"," defense your"," защиты цели."),
            row("target has up to ","Бонус — не более ","{0}","{0}","%","%",".",".")),
        List.of(row("Every ","После каждых ","3","3"," "," ","hits on a monster,","попаданий по монстру"),
            row("strike ","","lightning","молния",", dealing"," наносит"),
            row("{0}","{0}","%","%"," "," ","of the hit's damage to up","урона этого удара не более"),
            row("to ","","10","10"," monsters within "," монстрам в радиусе ","2","2"," blocks."," блоков.")),
        List.of(row("Every ","После каждых ","3","3"," "," ","hits on a monster, strike","попаданий по монстру"),
            row("lightning","молния",", dealing "," наносит ","{0}","{0}","%","%"," "," ","of the hit's","урона этого удара"),
            row("damage to up to ","не более ","10","10"," monsters within "," монстрам в радиусе ","2","2"),row("blocks.","блоков.")),
        List.of(row("Place this item on your island!","Разместите этот предмет на своём острове!")),
        List.of(row("Conduct wild enchanting experiments to","Проводите безумные опыты зачарования,"),
            row("gain bonus ","получая дополнительный ","Enchanting Exp","опыт зачарования"," "," ","and ","и ","items","предметы","!","!")),
        List.of(row("Drinking this serum shortens","Сыворотка сокращает длину"),
            row("the chain required for ","цепочки, необходимой для ","Superpairs","Суперпар"),
            row("add-on experiments by ","в дополнительных опытах на ","{0}","{0}","!","!")),
        List.of(row("Drinking this serum shortens","Сыворотка сокращает длину"),
            row("the chain required for ","цепочки, необходимой для ","Суперпары","Суперпар"),
            row("add-on experiments by ","в дополнительных опытах на ","{0}","{0}","!","!")),
        List.of(row("You may drink up to ","Можно выпить не более ","{0}","{0}"," "," ","doses","доз"),
            row("of the serum before the taste","сыворотки, пока её вкус"),
            row("becomes ","не станет ","too abstract","слишком абстрактным",".",".")),
        critical("",false),critical("☠",false),critical("",true),critical("☠",true),
        oldCubism("Magma Cubes","Slimes","Creepers"),oldCubism("Creepers","Slimes","Magma Cubes"),oldCubism("Slimes","Magma Cubes","Creepers"),
        List.of(row("Increases damage dealt to ","Увеличивает урон по ","",""," "," ","Cubic","кубическим"),row("mobs by ","существам на ","{0}","{0}","%","%",".",".")),
        List.of(row("Increases damage dealt to ","Увеличивает урон по ","",""," "," ","Ender","существам Края"),row("mobs by ","на ","{0}","{0}","%","%",".",".")),
        List.of(row("Increases damage dealt to ","Увеличивает урон по ","⊙","⊙"," "," ","Ender","существам Края"),row("mobs by ","на ","{0}","{0}","%","%",".",".")),
        List.of(row("Increases damage dealt by ","Увеличивает урон на ","{0}","{0}","%","%"),row("for each percent of extra","за каждый процент, на который"),row("health that your target has","здоровье цели превышает ваше."),row("above you up to ","Бонус — не более ","{0}","{0}","%","%",".",".")),
        // Fuel screenshot52: styled numeric fragments and the frozen older Wheel wrap.
        minionFuel("Increases the speed of","hours",false),minionFuel("Increases the speed of","часов",false),
        minionFuel("Increases the speed of","minutes",false),minionFuel("Increases the speed of","минут",false),
        minionFuel("increases the speed of","hours",false),minionFuel("increases the speed of","часов",false),
        minionFuel("increases the speed of","minutes",false),minionFuel("increases the speed of","минут",false),
        minionFuel("Увеличивает скорость работы","hours",false),minionFuel("Увеличивает скорость работы","часов",false),
        minionFuel("Увеличивает скорость работы","minutes",false),minionFuel("Увеличивает скорость работы","минут",false),
        minionFuel("Increases the speed of","hours",true),minionFuel("Increases the speed of","часов",true),
        minionFuel("increases the speed of","hours",true),minionFuel("increases the speed of","часов",true),
        minionFuel("Увеличивает скорость работы","hours",true),minionFuel("Увеличивает скорость работы","часов",true),
        // User screenshot51: original and already partially translated input.
        chronomatron("Repeat"),chronomatron("Повторение"),
        addonReward("and","Superpairs"),addonReward("и","Superpairs"),
        addonReward("and","Суперпары"),addonReward("и","Суперпары"),
        gardenWarp("Garden","Crops"),gardenWarp("Сад","Crops"),
        gardenWarp("Garden","Культуры"),gardenWarp("Сад","Культуры"),
        gardenWarp("Garden","crops"),gardenWarp("Сад","crops"),
        guesting("players"),guesting("игроки"),guesting("Игроки"),
        List.of(row("If ","Если ","Necron","Некрон"," is the right-hand of"," — правая рука"),
            row("the ","","Wither King","Короля иссушителей",", this dark",", то этот тёмный"),
            row("demigod is the left-hand.","полубог — его левая рука.")),
        // Reviewed enchantment paragraphs: whole sentence, including styled numbers.
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}","",""," "," ","Health","здоровья"," "," ","and ","и ","+","+","{1}","{1}","",""," "," ","True","истинной"),
            row("Defense","защиты"," "," ","per digit in your ","за каждую цифру в показателе ","Accessory","силы"),row("Power","аксессуаров",".",".")),
        List.of(row("Deals ","Наносит ","{0}","{0}","%","%"," "," ","of your damage","от вашего урона"),
            row("dealt to other monsters within","другим существам в радиусе"),row("{0}","{0}"," "," ","blocks of the target.","блока от цели.")),
        lethalityRows("Defense","❈",false),lethalityRows("Защита","❈",false),
        lethalityRows("Defense","❈",true),lethalityRows("Защита","❈",true),
        lethalityRows("Defense","",false),lethalityRows("Защита","",false),
        lethalityRows("Defense","",true),lethalityRows("Защита","",true),
        List.of(row("Increases damage dealt to ","Увеличивает урон по ","☠","☠"," "," ","Wither","иссушителям",",",","),
            row("༕","༕"," "," ","Undead","нежити"," "," ","and ","и ","🦴","🦴"," "," ","Skeletal","скелетным"," mobs by"," существам на"),
            row("{0}","{0}","%","%",".",".")),
        List.of(row("Increases damage dealt to ","Увеличивает урон по ","Ж","Ж"),
            row("Arthropod","членистоногим"," mobs by "," существам на ","{0}","{0}","%","%",".",".")),
        List.of(row("Gain ","Даёт на ","{0}","{0}","%","%"," "," ","extra ","больше ","Combat XP","опыта боя",". The","."),
            row("2","2","nd","-й"," hit on a mob grants "," удар по существу даёт ","+","+","{0}","{0}"),
            row("coins","монеты"," "," ","&","и"," "," ","+","+","{0}","{0}"," "," ","exp orbs.","сфер опыта.")),
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}","",""," "," ","Farming Fortune","удачи фермерства"," "," ","when","при"),
            row("vacuuming ","сборе ","",""," "," ","Pests","вредителей","."," пылесосом.")),
        List.of(row("Heal ","Восстанавливает ","{0}","{0}","❤","❤"," every time you kill a mob."," здоровья при убийстве существа."),healingCooldown()),
        List.of(denseRow("Regen ","Восстанавливает ","+","+","{0}","{0}","",""," "," ","Vitality","жизненной силы"," every time you"," при каждом"),
            row("hit a mob.","попадании по существу."),healingCooldown()),
        // The captured red run includes the space after the heart, not the prose.
        List.of(row("Heal ","Восстанавливает ","{0}","{0}","❤ ","❤ ","every time you hit a mob.","здоровья при попадании по существу.")),
        List.of(row("Fishing rod deals ","Удочка наносит на ","{0}","{0}","%","%"," more damage"," больше урона"),row("to monsters.","существам.")),
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}","",""," "," ","Treasure Chance","шанса выловить сокровище",", which",","),
            row("increases the chance of fishing","повышая вероятность получить"),row("treasure.","сокровище при рыбалке.")),
        List.of(new Row(List.of("{0}","%"," chance to not consume"),List.of(new Part(2,"С шансом "),new Part(0,"{0}"),new Part(1,"%"),new Part(2," не расходует"))),
            row("bait.","наживку.")),
        List.of(row("Convert ","Преобразует ","{0}","{0}","%","%"," of "," израсходованной ","",""," "," ","Vitality","жизненной силы"," used as "," в ","",""),
            row("Strength","силу"," for "," на ","{0}","{0}","s. (Max "," с. (максимум ","{1}","{1}","",""),
            row("Strength","силы",").",").")),
        List.of(row("Convert ","Преобразует ","{0}","{0}","%","%"," of "," израсходованной ","",""," "," ","Vitality","жизненной силы"," used as "," в ","",""),
            row("Defense","защиту"," for "," на ","{0}","{0}","s. (Max "," с. (максимум ","{1}","{1}","",""," "," ","Defense","защиты",").",").")),
        List.of(row("Convert ","Преобразует ","{0}","{0}","%","%"," of "," израсходованной ","",""," "," ","Vitality","жизненной силы"," used as "," в ","",""),
            row("Attack Speed","скорость атаки"," for "," на ","{0}","{0}","s. (Max "," с. (максимум ","{1}","{1}","",""),
            row("Attack Speed","скорости атаки",").",").")),
        List.of(row("Increases how high you can fall","Увеличивает высоту безопасного падения"),
            row("before taking fall damage by","до получения урона на"),
            row("{0}","{0}"," and reduces fall damage by"," и уменьшает урон от падения на"),
            row("{0}","{0}","%","%",".",".")),
        List.of(row("Increases how high you can fall","Увеличивает высоту безопасного падения"),
            row("before taking fall damage by","до получения урона на"),
            row("{0}","{0}"," and reduces fall damage"," и уменьшает урон от падения"),
            row("by ","на ","{0}","{0}","%","%",".",".")),
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}"," "," ","❈","❈"," "," ","Defense","защиты"),
            row("against explosions.","от взрывов.")),
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}"," "," ","❈","❈"," "," ","Defense","защиты"),
            row("against projectiles.","от снарядов.")),
        List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}","",""," "," ","True Defense","истинной защиты"," against"," от"),
            row("fire and lava.","огня и лавы.")),
        // FIRE_PROTECTION;7 has this missing first letter in the frozen original.
        List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}","",""," "," ","True Defense","истинной защиты"," against"," от"),
            row("ire and lava.","огня и лавы.")),
        List.of(row("Saves arrows ","С шансом ","{0}","{0}","%","%"," of the time"," сохраняет стрелы"),
            row("when you fire your bow.","при выстреле из лука.")),
        List.of(row("Saves arrows ","С шансом ","{0}","{0}","%","%"," of the time when"," сохраняет стрелы"),
            row("you fire your bow.","при выстреле из лука.")),
        List.of(denseRow("Grants a ","С шансом ","{0}","{0}","%","%"," chance for mobs"," существа"),
            row("and ores to drop double","и руда дают двойной"),row("experience.","опыт.")),
        List.of(denseRow("Grants a ","С шансом ","{0}","{0}","%","%"," chance for",""),
            row("mobs and ores to drop double","существа и руда дают двойной"),row("experience.","опыт.")),
        List.of(new Row(List.of("Scavenge ","{0}"," ","Coins"," per monster"),
            List.of(new Part(0,"За убийство даёт "),new Part(3,"монеты"),new Part(2,": "),new Part(1,"{0}"),new Part(4," за каждый"))),
            row("level on kill.","уровень убитого существа.")),
        List.of(row("Increases melee damage dealt by","Увеличивает урон в ближнем бою на"),row("{0}","{0}","%","%")),
        List.of(row("Increases bow damage by ","Увеличивает урон лука на ","{0}","{0}","%","%",".",".")),
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}"," "," ","❤","❤"," "," ","Health","здоровья",".",".")),
        List.of(row("Grants ","Даёт ","+","+","{0}","{0}"," "," ","❈","❈"," "," ","Defense","защиты",".",".")),
        List.of(row("Every ","Каждые ","3","3"," SkyBlock days, I hold a contest"," дня SkyBlock проходят состязания"),
            row("for ","по ","3","3"," "," ","Farming","фермерским"," Collections."," коллекциям.")),
        List.of(row("The contests last ","Состязание длится ","1","1"," SkyBlock day."," день SkyBlock."),
            row("(","(","20","20"," minutes)."," минут).")),
        List.of(row("The top players who increase","Игроки с наибольшим приростом"),
            row("their collection the most earn","коллекции получают"),
            row("Jacob's Tickets","билеты Джейкоба"," and "," и ","unique","уникальные"),
            row("rewards!","награды!")),
        List.of(row("Co-ops do ","Результаты участников кооператива ","NOT","НЕ"," pool their collection!"," суммируются!")),
        List.of(row("You may participate in ","Можно соревноваться только по ","1","1"," collection"," коллекции"),
            row("contest per event!","за одно событие!")),
        List.of(row("Minions do not count in contests!","Ресурсы, добытые миньонами, не учитываются!")),
        List.of(row("Increase the maximum amount of","Увеличивает максимальный запас"),
            row("organic matter that can be stored to","органического сырья до")),
        List.of(row("Change the selected plot to this","Примените к выбранному участку"),
            row("preset!","этот шаблон!")),
        // Complete paragraphs transcribed from the updated bug document.
        List.of(row("Consume to apply a pungent smell","Используйте духи: резкий запах"),
            row("upon yourself, confusing ","собьёт с толку","The",""),
            row("Matriarch","Матриарха"," in the "," на ","Crimson Isle","Багровом острове"," into",", заставив"),
            row("releasing new pearls.","выпустить новые жемчужины.")),
        List.of(row("You can expand your Accessory Bag","Расширить сумку аксессуаров"),
            row("by doing any of the following:","можно следующими способами:")),
        List.of(row("Increasing your ","Повышайте уровень ","Redstone Collection","коллекции редстоуна",".",".")),
        List.of(row("Purchasing slots for ","Покупайте ячейки за ","Coins","монеты"," from"," у"),
            row("Jacobus","Якобуса"," in the "," в ","Combat Settlement","Поселении воинов"," in",""),
            row("the ","в ","Hub","Хабе",".",".")),
        List.of(row("Unlocking slots from ","Открывайте ячейки у ","Elizabeth","Элизабет"," in the"," в"),
            row("Community Center","Общественном центре"," in the "," в ","Hub","Хабе",".",".")),
        List.of(row("MVP++","MVP++"," contains every perk from "," включает все привилегии рангов ","VIP","VIP",",",","),
            row("VIP+","VIP+",", ",", ","MVP","MVP",", "," и ","MVP+","MVP+",", and then more!",", а также новые бонусы!")),
        List.of(row("Click the book below to view all","Нажмите на книгу ниже, чтобы увидеть все"),
            new Row(List.of("Hypixel"," perks for this rank!"),List.of(new Part(1,"привилегии этого ранга на "),new Part(0,"Hypixel"),new Part(1,"!")))),
        List.of(row("Stores up to ","Лимит — ","20%","20%"," "," ","of your ","от вместимости ","Co-op","совместного"),row("account.","счёта.")),
        List.of(row("Both accounts accrue interest,","Оба счёта приносят проценты,"),
            row("but you will only earn ","но доход по личному счёту — лишь ","20%","20%"," of it",""),
            row("on your personal account.","от дохода по совместному счёту.")),
        List.of(row("While on ","На ","Foraging Islands","островах лесорубства",", throw your Axe at a"," бросьте топор в"),
            row("tree! It will sweep ","дерево: он срубит ","50%","50%"," of the logs it normally"," от обычного"),
            row("would!","числа брёвен.")),
        List.of(row("Shows the way towards the nearest ","Указывает путь к ближайшему ","Emissary","посланнику"),
            row("while in the ","в ","Dwarven Mines","Дварфских шахтах",".",".")),
        List.of(row("Holds various items obtained from","Хранит ресурсы, полученные при"),
            row("Foraging","лесорубстве","!",""," Items you pickup go",". Подобранные предметы"),
            row("directly into your sacks.","сразу попадают в ваши мешки.")),
        List.of(row("Gain ","Даёт ","+","+","{0}","{0}"," "," ","Foraging Fortune","удачи лесорубства"," while in "," в ","The Park","Парке",".",".")),
        List.of(row("Requires an open area to place","Нужно свободное место для выращивания"),
            row("trees. Minions also work when you","деревьев. Миньоны работают, даже когда вы"),
            row("are offline!","вне игры!")),
        List.of(row("When creating an ","При создании предмета «","Inferno Apex","Вершина Инферно",",","»"),
            row("create ","миньон производит ","2","2","."," штуки.")),
        List.of(row("This item can be used as a minion","Этот предмет можно использовать как"),
            row("upgrade.","улучшение миньона.")),
        List.of(row("Automatically smelts materials","Сначала автоматически переплавляет материалы,"),
            row("produced by minions before","добытые миньонами, а затем"),
            row("compacting them into their enchanted","превращает их в зачарованные"),
            row("form.","предметы.")),
        List.of(row("This item can be used as a minion","Используется как улучшение миньона."),
            row("upgrade. Prevents your minions from","Не даёт вашим миньонам"),
            row("getting distracted by flies.","отвлекаться на мух.")),
        List.of(row("This item can be used as a minion","Улучшение для миньона."),
            row("upgrade. Place it in any mob","Установите его в миньона,"),
            row("spawning minion and it will turn","призывающего мобов: он превратит"),
            row("nearby mobs into their corrupted","ближайших мобов в осквернённые"),
            row("forms.","формы.")),
        List.of(row("This item can be placed inside any","Установите этот предмет в любого"),
            row("minion. Automatically sells generated","миньона. Воронка автоматически продаёт добычу,"),
            row("items when the minion has no space.","когда хранилище миньона заполнено.")),
        List.of(row("Items are sold for ","Предметы продаются за ","50%","50%"," of their"," от их"),
            row("selling price.","цены продажи.")),
        List.of(row("Items are sold for ","Предметы продаются за ","70%","70%"," of their"," от их"),
            row("selling price.","цены продажи.")),
        List.of(row("Placing it in a minion will reduce the","Установка в миньона снижает"),
            row("minion's output by ","производство миньона на ","50%","50%",", but it will",", но он"),
            row("start generating ","начинает производить ","Soulflow","поток душ"," items.",".")),
        List.of(row("While equipped on a ","В ","Voidling minion","миньоне порождения пустоты",",",""),
            row("output is increased by ","производство растёт на ","3%","3%"," per minion"," за каждый"),
            row("level.","уровень этого миньона.")),
        List.of(row("Craft with minion ","Создавайте улучшения для ","Large Storage","больших хранилищ"),
            row("chests to upgrade their size to ","миньона на ","21","21"),
            row("and ","и ","27","27"," slots."," ячеек.")),
        List.of(row("The longer you hold this in your","Чем дольше утяжелители находятся в вашем"),
            row("inventory, the stronger you'll","инвентаре, тем больше силы вы"),
            row("become for ","получите на ","2","2"," minutes after removing"," минуты после удаления"),
            row("it from your inventory! Max ","их из инвентаря! Максимум ","+50","+50",".","."),
            new Row(List.of(),List.of()),row("Time Held: ","Время хранения: ","{0}","{0}"," Minutes"," минут")),
        List.of(new Row(List.of("Your ","Critical Hits"," deal ","100%"," more"),List.of(new Part(0,"Ваши "),new Part(1,"критические удары"),new Part(2," наносят на "),new Part(3,"100%"),new Part(4," больше"))),
            new Row(List.of("damage if you are behind your"),List.of(new Part(0,"урона, если вы находитесь позади"))),
            new Row(List.of("target."),List.of(new Part(0,"цели.")))),
        List.of(new Row(List.of("On teleport: Your next melee hit"),List.of(new Part(0,"После телепортации ваш следующий удар ближнего боя"))),
            new Row(List.of("within ","5","s deals ","10%"," more damage."),List.of(new Part(0,"в течение "),new Part(1,"5"),new Part(2," с наносит на "),new Part(3,"10%"),new Part(4," больше урона.")))),
        List.of(new Row(List.of("Piece Bonus: ","Sinew"),List.of(new Part(0,"Бонус части комплекта: "),new Part(1,"Жилистость")))),
        List.of(new Row(List.of("Piece Bonus: ","Bloodrush"),List.of(new Part(0,"Бонус части комплекта: "),new Part(1,"Прилив крови")))),
        List.of(new Row(List.of("Piece Bonus: ","Salubrious"),List.of(new Part(0,"Бонус части комплекта: "),new Part(1,"Оздоровление")))),
        List.of(new Row(List.of("Instead of dying, gain damage"),List.of(new Part(0,"Вместо смерти вы получаете"))),
            new Row(List.of("immunity and ","+20","  ","Strength"," for ","3","s"),List.of(new Part(0,"неуязвимость к урону и "),new Part(1,"+20"),new Part(2,"  "),new Part(3,"силы"),new Part(4," на "),new Part(5,"3"),new Part(6," с"))),
            new Row(List.of("and fully replenish your health. The"),List.of(new Part(0,"и полностью восстанавливаете здоровье."))),
            new Row(List.of("cooldown and ","Strength"," bonus you"),List.of(new Part(0,"Перезарядка и бонус "),new Part(1,"силы"))),
            new Row(List.of("receive improve based on your"),List.of(new Part(0,"улучшаются в зависимости от вашего"))),
            new Row(List.of("Dungeoneering Skill"," level. This ability"),List.of(new Part(1,"уровня "),new Part(0,"навыка прохождения подземелий"),new Part(1,". Способность"))),
            new Row(List.of("only works while in ","The Catacombs","!"),List.of(new Part(0,"работает только в "),new Part(1,"Катакомбах"),new Part(2,"!")))),
        List.of(new Row(List.of("❣ Requires ","Master Mode"," ","The Catacombs"," Floor ","IV"),List.of(new Part(0,"❣ Требуется пройти «"),new Part(3,"Катакомбы"),new Part(2,"», этаж "),new Part(5,"IV"),new Part(4," в "),new Part(1,"мастер-режиме"),new Part(4,"."))),
            new Row(List.of("Completion."),List.of())),
        List.of(new Row(List.of("Deals double damage when coming"),List.of(new Part(0,"Наносит двойной урон при"))),
            new Row(List.of("back. Pierces up to ","10"," foes."),List.of(new Part(0,"возвращении. Пронзает до "),new Part(1,"10"),new Part(2," врагов.")))),
        List.of(new Row(List.of("Can be used to defuse ","Dungeon"),List.of(new Part(0,"Позволяет обезвреживать ловушки "),new Part(1,"подземелий"),new Part(0,"."))),
            new Row(List.of("traps. Successful defusing of traps"),List.of(new Part(0,"Успешное обезвреживание ловушек"))),
            new Row(List.of("may increase your  ","Intelligence","!"),List.of(new Part(0,"может повысить ваш  "),new Part(1,"интеллект"),new Part(2,"!")))),
        List.of(new Row(List.of("Slash in a huge arc, dealing ","135%"," melee"),List.of(new Part(0,"Рубит по широкой дуге, нанося "),new Part(1,"135%"),new Part(2," урона ближнего боя"))),
            new Row(List.of("damage to all enemies hit!"),List.of(new Part(0,"всем задетым врагам!")))),
        List.of(new Row(List.of("Heal for ","500","❤ and heal players within ","8"),List.of(new Part(0,"Восстанавливает вам "),new Part(1,"500"),new Part(2,"❤; игрокам в радиусе "),new Part(3,"8"))),
            new Row(List.of("blocks for ","100","❤."),List.of(new Part(0,"блоков — "),new Part(1,"100"),new Part(2,"❤.")))),
        List.of(new Row(List.of("Heal for ","420","❤ and heal players within ","7"),List.of(new Part(0,"Восстанавливает вам "),new Part(1,"420"),new Part(2,"❤; игрокам в радиусе "),new Part(3,"7"))),
            new Row(List.of("blocks for ","84","❤."),List.of(new Part(0,"блоков — "),new Part(1,"84"),new Part(2,"❤.")))),
        List.of(new Row(List.of("Heal for ","320","❤ and heal players within ","7"),List.of(new Part(0,"Восстанавливает вам "),new Part(1,"320"),new Part(2,"❤; игрокам в радиусе "),new Part(3,"7"))),
            new Row(List.of("blocks for ","64","❤."),List.of(new Part(0,"блоков — "),new Part(1,"64"),new Part(2,"❤.")))),
        List.of(new Row(List.of("Produces a cone of ice in front of the caster"),List.of(new Part(0,"Создаёт перед вами конус льда,"))),
            new Row(List.of("that deals ","17,000"," damage to mobs and freezes"),List.of(new Part(0,"который наносит мобам "),new Part(1,"17,000"),new Part(2," урона и замораживает"))),
            new Row(List.of("them in place for ","5"," seconds! Frozen mobs"),List.of(new Part(0,"их на месте на "),new Part(1,"5"),new Part(2," секунд! Замороженные мобы"))),
            new Row(List.of("take ","10%"," increased damage!"),List.of(new Part(0,"получают на "),new Part(1,"10%"),new Part(2," больше урона!")))),
        List.of(new Row(List.of("Produces a cone of ice in front of the caster"),List.of(new Part(0,"Создаёт перед вами конус льда,"))),
            new Row(List.of("that deals ","19,114.1"," damage to mobs and"),List.of(new Part(0,"который наносит мобам "),new Part(1,"19,114.1"),new Part(2," урона и"))),
            new Row(List.of("freezes them in place for ","5"," seconds! Frozen"),List.of(new Part(0,"замораживает их на месте на "),new Part(1,"5"),new Part(2," секунд! Замороженные"))),
            new Row(List.of("mobs take ","10%"," increased damage!"),List.of(new Part(0,"мобы получают на "),new Part(1,"10%"),new Part(2," больше урона!")))),
        List.of(new Row(List.of("The longer you hold this in your"),List.of(new Part(0,"Чем дольше утяжелители находятся в вашем"))),
            new Row(List.of("inventory, the stronger you'll"),List.of(new Part(0,"инвентаре, тем больше силы вы"))),
            new Row(List.of("become for ","2"," minutes after removing"),List.of(new Part(0,"получите на "),new Part(1,"2"),new Part(2," минуты после удаления"))),
            new Row(List.of("it from your inventory! Max ","+50","."),List.of(new Part(0,"их из инвентаря! Максимум "),new Part(1,"+50"),new Part(2,".")))),
        List.of(new Row(List.of("Shoots a guided ","spirit bat",", following your aim"),List.of(
            new Part(0,"Выпускает "),new Part(1,"призрачную летучую мышь"),new Part(0,", управляемую "),new Part(2,"вашим прицелом"))),
            row("and exploding for ","и взрывающуюся, нанося ","2,000","2,000"," damage."," урона.")),
        List.of(row("➤ The damage blocked scales with","➤ Блокируемый урон зависит от"),
            row("your ","вашего ","Dungeon Stat Boost","усиления характеристик подземелий"," while in The"," в"),
            row("Catacombs","Катакомбах",".",".")),
        List.of(row("Berserk","Берсерк",": ",": ","+10","+10"," "," ","Damage","урона",", ",", ","+75","+75"," "," ","Strength","силы",", ",", ","+10","+10","",""),row("Speed","скорости")),
        List.of(row("Tank","Танк",": ",": ","+100","+100"," "," ","Defense","защиты",", ",", ","+5","+5"," "," ","Vitality","жизненной силы",", ",", ","+5","+5"," "," ","True","истинной"),row("Defense","защиты")),
        List.of(row("Applies the ","Накладывает перековку «","Ancient","Древний"," reforge when","» при"),
            row("combined with ","объединении с ","Armor","бронёй",".",".")),
        List.of(row("The ","","Zombie","Зомби"," it came from was",", которому принадлежала эта плоть,"),
            row("probably a great warrior before its","вероятно, был великим воином до своей"),row("death.","смерти.")),
        List.of(new Row(List.of("Take ","-2%","  ","Damage"," from "," ","Undead"),List.of(
            new Part(1,"-2%"),new Part(2,"  "),new Part(0,"к получаемому "),new Part(3,"урону"),new Part(4," от "),new Part(5," "),new Part(6,"нежити"),new Part(0,"."))),
            row("mobs.","")),
        List.of(row("Grants ","Даёт ","+10","+10"," "," ","Mending","силы исцеления"," while in"," в"),
            row(" "," ","Dungeons","подземельях",".",".")),
        List.of(row("While in ","В ","The ","","Catacombs","Катакомбах",", consume "," расходует ","1","1","⸕","⸕"),
            row("charge to break a block. ","заряд для разрушения блока. ","20","20"," blocks can"," блоков можно"),
            row("be broken at a time, and re-appear after","сломать одновременно; они появляются снова через"),
            row("10","10","s"," с",". ",". ","2","2","⸕","⸕"," charges are regenerated each"," заряда восстанавливаются каждую"),row("second.","секунду.")),
        List.of(row("Throw a potion which gives all teammates in a","Бросьте зелье, дающее всем союзникам в"),
            new Row(List.of("10"," block radius ","200"," HP worth of absorption"),List.of(
                new Part(1,"радиусе "),new Part(0,"10"),new Part(1," блоков"),new Part(3," поглощение на "),new Part(2,"200"),new Part(3," единиц здоровья"))),
            row("for ","на ","3","3"," seconds."," секунды.")),
        List.of(row("Allows you to open an additional","Позволяет открыть дополнительный"),
            row("treasure chest at the end of a","сундук с сокровищами в конце"),row("dungeon!","подземелья!")),
        List.of(row("Runs around and draws attention","Бегает вокруг и привлекает внимание"),
            row("from nearby mobs, very handy for","ближайших мобов — удобный способ"),
            row("creating a distraction!","отвлечь противников!")),
        List.of(row("Place an explosive trap that","Установите взрывную ловушку:"),
            row("triggers when mobs walk over it","она срабатывает, когда на неё наступает моб,"),
            row("dealing ","нанося ","20,000","20,000","-","-","250,000","250,000"," "," ","damage","урона."),
            row("based on the dungeon floor you are","Урон зависит от текущего этажа"),row("on.","подземелья.")),
        List.of(row("Allows you to teleport to any teammate! Grants","Позволяет телепортироваться к любому союзнику! Даёт"),
            row("1","1"," "," ","second of immunity after teleporting,","секунду неуязвимости после телепортации,"),
            row("immunity is cancelled upon dealing damage.","неуязвимость прекращается при нанесении урона.")),
        List.of(row("Blows up cracked brick walls and","Взрывает потрескавшиеся кирпичные стены и"),
            row("crypts, which are typically found in","склепы, которые обычно встречаются в"),
            row("Dungeons","подземельях"," and the "," и ","Crystal Hollows","Кристальных пещерах",".",".")),
        List.of(row("The ","Выбранный ","Mayor","мэр"," is selected until the next"," остаётся в должности"),
            row("election ends!","до завершения следующих выборов!")),
        List.of(row("The center of ","Сердцевина ","Magma Cubes","магмовых кубов"," is the"," — это"),
            row("hottest part...also the hardest. Giant","самая горячая и самая твёрдая часть. Гигантские"),
            row("ones can even create stones like","кубы могут даже создавать такие камни, как"),
            row("this one.","этот.")),
        List.of(row("Rewarded to every ","Выдаётся каждому ","Necromancer","некроманту"," who",", который"),
            row("passes the final exam.","сдаст выпускной экзамен.")),
        List.of(row("Applies the ","Накладывает перековку «","Renowned","Прославленный"," reforge when","» при"),
            row("combined with ","объединении с ","Armor","бронёй",".",".")),
        List.of(row("Applies the ","Накладывает перековку «","Spiked","Шипастый"," reforge when","» при"),
            row("combined with ","объединении с ","Armor","бронёй",".",".")),
        List.of(row("A cold and hardened heart that was","Холодное окаменевшее сердце, которое"),
            row("once brimming with eager love.","когда-то было полно пылкой любви.")),
        List.of(row("Mobs are ","Мобы на ","1%","1%"," more likely to drop"," чаще оставляют"),
            row("candy","конфеты"," during the "," во время ","Spooky Festival","Жуткого фестиваля",".",".")),
        List.of(row("Grants ","Даёт ","+5","+5"," "," ","Speed","скорости"," for "," на ","5s","5 с"," after"," после"),
            row("using a ","использования способности ","Transmission","перемещения"," ability.",".")),
        List.of(row("Replaces the damage reduction from","Заменяет снижение урона от"),
            row("the ","зачарования «","Piercing","Пронзающая стрела"," enchant to ","» эффектом «","Skewer","Насквозь",".","».")),
        List.of(row("Passive: ","Пассивный эффект: ","Skewer","Насквозь"),
            row("Hits through ","Попадания с «","Piercing","Пронзающей стрелой"," deal ","» наносят на ","+7.5%","+7.5%"),
            row("damage than the previous one, up to","больше урона, чем предыдущее; не более"),
            row("5","5"," victims."," целей.")),
        List.of(row("Grants ","Даёт ","+25","+25"," "," ","Mining Spread","широты добычи"," while",","),
            // The Russian condition moves ordinary prose before the mayor's name.
            new Row(List.of("Mayor Cole","'s ","Mining Fiesta"," perk is"),List.of(
                new Part(1,"пока бонус мэра "),new Part(0,"Коула"),new Part(3," «"),new Part(2,"Горная фиеста"),new Part(3,"»"))),
            row("active.","активен.")),
        List.of(row("Grants ","Даёт ","+50%","+50%","  ","  ","Mining Spread","широты добычи"),
            row("towards ","при добыче ","Blocks","блоков",".",".")),
        List.of(row("Convert every ","Преобразует каждые ","{1}","{1}"," "," ","Speed","скорости"," you",""),
            row("have above ","сверх ","100","100"," into "," в ","+1","+1"," "," ","Block","удачи добычи"),
            row("Fortune","блоков",".",".")),
        List.of(row("Right-click your summoned pet to","Нажмите ПКМ по призванному питомцу, чтобы"),
            row("ride it! Moves faster based on your","оседлать его! Скорость передвижения зависит от вашей"),
            row(" "," ","Speed","скорости",".",".")),
        List.of(row("Every ","Каждые ","{0}","{0}"," seconds, the next"," с следующий добытый"),
            row("Gemstone","драгоценный камень"," you mine gives "," даёт добычу ","2x","x2"," drops",".")),
        List.of(row("Increases ","Увеличивает ","Coin","количество монет"," gain from "," из ","Griffin","грифоньих"),
            row("Burrows","нор"," by "," на ","+2.5%","+2.5%",".",".")),
        List.of(row("Can be gifted to any eligible ","Можно подарить любому подходящему ","NPC","персонажу"," to",","),
            row("invite them to visit your ","чтобы пригласить его посетить ваш ","Garden","Сад",".",".")),
        List.of(row("Reach out to the ","Связывается с ","King","королём"," and open the"," и открывает"),
            row("Commissions","меню шахтёрских заданий"," menu.",".")),
        List.of(row("Grants ","Даёт ","+5","+5","  ","  ","Mining Speed","скорости добычи"," and "," и ","+2.5","+2.5"),
            row(" "," ","Ore Fortune","удачи добычи руды"," while in the Mines of"," в шахтах"),
            row("Divan","Дивана",".",".")),
        List.of(row("Commissions","Шахтёрские задания"," grant "," дают ","+10","+10"," "," ","HOTM Exp","опыта Сердца горы",".",".")),
        List.of(row("Wear this hat to a ","Наденьте этот шлем на встречу ","Cult of the Fallen","Культа Падшей"),
            row("Star","звезды"," meeting.",".")),
        List.of(row("Gain ","Даёт ","+30","+30","",""," "," ","Speed","скорости"," while in the"," в"),
            row("Mushroom Desert","Грибной пустыне",".",".")),
        List.of(row("Earn ","Даёт ","+1","+1"," "," ","pelt","шкуру"," from tracker quests."," за задания охотника.")),
        List.of(new Row(List.of("Earn ","x1.1"," ","Pelts"," from tracker quests."),List.of(
            new Part(3,"Количество шкур"),new Part(4," за задания охотника: "),new Part(1,"x1.1"),new Part(0,".")))),
        List.of(row("Collect ","Соберите ","100","100"," "," ","Pelts","шкур"," to upgrade this",", чтобы улучшить"),
            row("accessory","аксессуар"," to "," до редкости ","UNCOMMON","UNCOMMON",".",".")),
        List.of(row("Indicates the approximate ","Показывает примерный ","height difference","перепад высот"),
            row("and ","и ","angle","угол"," between you and your "," между вами и ","tracked mob","отслеживаемой целью",".",".")),
        List.of(new Row(List.of("Gains ","1",""," ","Sweep"," every ","2,000"," ","Logs"),List.of(
            new Part(0,"Получает "),new Part(1,"1"),new Part(2,""),new Part(3," "),new Part(4,"размаха"),
            new Part(5," за каждые "),new Part(6,"2,000"),new Part(7," "),new Part(8,"брёвен"),new Part(0,","))),
            row("cut on ","срубленных на ","Galatea","Галатее",". (max ",". (максимум ","20","20",")",")")),
        List.of(row("Applies the ","Накладывает перековку «","Groovy","Зажигательный"," reforge when","» при"),
            row("combined with ","объединении с ","Armor","бронёй",".",".")),
        List.of(row("Applies the ","При объединении добавляет усилитель ","ꕮ","ꕮ"," "," ","Sweep","размаха"," booster when"," для"),
            row("combined with ","","Foraging Armor","лесорубской брони",",",","),
            row("Equipment","экипировки",", or "," или ","Axes","топоров",".",".")),
        List.of(row("Grants ","Даёт ","",""," "," ","Sweep","размах",".",".")),
        List.of(new Row(List.of("Apply to an item with a ","Common Sweep"),List.of(
            new Part(0,"Примените к предмету с "),new Part(1,"обычным усилителем размаха"),new Part(0,","))),
            new Row(List.of("Booster"," to upgrade it to an"),List.of(new Part(1,"чтобы улучшить этот "),new Part(0,"усилитель"),new Part(1," до"))),
            row("Uncommon Sweep Booster","необычного усилителя размаха",", which",", который"),
            row("increases the amount of ","увеличивает количество ","",""," "," ","Sweep","размаха"," it",","),
            row("gives.","которое даёт этот усилитель.")),
        List.of(new Row(List.of("Gains ","1",""," ","Sweep"," every ","20,000"," ","Logs"),List.of(
            new Part(0,"Получает "),new Part(1,"1"),new Part(2,""),new Part(3," "),new Part(4,"размаха"),
            new Part(5," за каждые "),new Part(6,"20,000"),new Part(7," "),new Part(8,"брёвен"),new Part(0,","))),
            row("cut on ","срубленных на ","Galatea","Галатее",". (max ",". (максимум ","40","40",")",")")),
        List.of(row("Applies the ","При объединении добавляет ","४","४"," "," ","Fighting","боевой"," booster when"," усилитель для"),
            row("combined with ","","Foraging Armor","лесорубской брони",",",","),
            row("Equipment","экипировки",", or "," или ","Axes","топоров",".",".")),
        List.of(row("Increases damage against ","Увеличивает урон по существам ","Foraging","острова"),
            row("Island","лесорубства"," creatures.",".")),
        List.of(row("Apply to an item with a ","Примените к предмету с ","Common","обычным"),
            row("Fighting Booster","боевым усилителем"," to upgrade it to an",", чтобы улучшить его до"),
            row("Uncommon Fighting Booster","необычного боевого усилителя",", which",", который"),
            row("increases damage against ","увеличивает урон по мобам ","Foraging","острова"),
            row("Island","лесорубства"," mobs.",".")),
        List.of(row("Applies the ","Добавляет усилитель ","⎋","⎋"," "," ","Foraging Fortune","удачи лесорубства"),
            row("booster when combined with ","при объединении с ","Foraging","лесорубской"),
            row("Armor","бронёй",", ",", ","Equipment","экипировкой",", or "," или ","Axes","топорами",".",".")),
        List.of(row("Grants ","Даёт ","",""," "," ","Foraging Fortune","удачу лесорубства",".",".")),
        List.of(row("Applies the ","Добавляет усилитель ","⸙","⸙"," "," ","Foraging Wisdom","мудрости лесорубства"),
            row("booster when combined with ","при объединении с ","Foraging","лесорубской"),
            row("Armor","бронёй",", ",", ","Equipment","экипировкой",", or "," или ","Axes","топорами",".",".")),
        List.of(row("Grants ","Даёт ","☯","☯"," "," ","Foraging Wisdom","мудрость лесорубства",".",".")),
        List.of(row("Applies the ","Добавляет усилитель ","☯","☯"," "," ","Common Hunting Wisdom","обычной мудрости охоты"),
            row("booster when combined with ","при объединении с ","Foraging","лесорубской"),
            row("Armor","бронёй",", ",", ","Equipment","экипировкой",", or "," или ","Axes","топорами",".",".")),
        List.of(row("Grants ","Даёт ","☯","☯"," "," ","Hunting Wisdom","мудрость охоты",".","."))
    );
    private static List<List<Row>> sacks(){
        var out=new ArrayList<List<Row>>();
        out.add(List.of(row("A sack which contains other sacks.","Сумка для хранения других мешков."),
            row("Sackception!","Мешки внутри мешка!"),
            row("Also accessible via ","Также доступна через ","/sacks","/sacks")));
        out.add(List.of(row("Obtain sack capacity upgrades as well","Улучшения вместимости мешков и"),
            row("as exclusive bits shop sacks.","особые мешки из магазина битов.")));
        out.add(List.of(row("Combine this item in an anvil with any","Объедините этот предмет на наковальне"),
            row("sack to increase its capacity by","с любым мешком. Прирост вместимости:"),
            row("+","+","{0}","{0}","%","%"," "," ","its ","от ","original","исходной"," "," ","capacity.","вместимости.")));
        out.add(List.of(row("This item can be combined with a sack","С одним мешком можно объединить"),
            row("up to three times!","не более трёх таких предметов!")));
        out.add(List.of(row("It's a pocket sack, to be stitched","Карманный мешок, который можно"),
            row("inside another sack!","вшить в другой мешок!")));
        out.add(List.of(row("Your sacks can hold ","Мешки вмещают на ","+","+","{0}","{0}","%","%"," "," ","more items.","больше предметов.")));
        out.add(List.of(row("You can Syphon this shard from","Этот осколок можно извлечь"),
            row("your ","из ","Hunting Box","охотничьего ящика",".",".")));
        String[][] types={{"Combat","Бой","в бою","! Items you pickup go directly"},{"Farming","Фермерство","при фермерстве","! Items you pickup go directly"},{"Fishing","Рыбалка","при рыбалке","! Items you pickup go directly"},{"Foraging","Лесозаготовка","при лесорубстве","! Items you pickup go"},{"Husbandry","Животноводство","при животноводстве","! Items you pickup go"},{"Mining","Добыча","при добыче","! Items you pickup go directly"}};
        for(var t:types){
            for(String head:List.of("Holds various items obtained from","Хранит различные предметы, полученные из"))
                for(String term:List.of(t[0],t[1]))for(String end:List.of(t[3].endsWith("directly")?"into your sacks.":"directly into your sacks.","в ваши мешки."))
                    out.add(List.of(row(head,t[0].equals("Foraging")?"Хранит ресурсы, полученные при":"Хранит ресурсы, полученные"),row(term,t[0].equals("Foraging")?"лесорубстве":t[2],t[3],". Подобранные предметы"),row(end,"сразу попадают в ваши мешки.")));
            out.add(List.of(row("Holds various enchanted items","Хранит зачарованные ресурсы,"),
                row("obtained from ","полученные ",t[0],t[2],"! Items you",". Подобранные предметы"),
                row("pickup go directly into your sacks.","сразу попадают в ваши мешки.")));
        }
        String[][] locations={
            {"Holds items commonly dropped by","","Slayers","при охоте на боссов","! Items you pickup go directly","into your sacks."},
            {"Holds items commonly found during","the ","Spooky Festival","на Жутком фестивале","! Items you pickup","go directly into your sacks."},
            {"Holds items commonly found in","","various Events","на различных событиях","! Items you pickup go","directly into your sacks."},
            {"Holds items commonly found in The","","Catacombs","в Катакомбах","! Items you pickup go","directly into your sacks."},
            {"Holds items commonly found in the","","Crystal Hollows","в Кристальных пещерах","! Items you pickup go","directly into your sacks."},
            {"Holds items commonly found in the","","Dwarven Mines","в Дварфских шахтах","! Items you pickup go","directly into your sacks."},
            {"Holds items commonly found in the","","Garden","в Саду","! Items you pickup go directly","into your sacks."},
            {"Holds items commonly found on","","Jerry's Workshop","в Мастерской Джерри","! Items you pickup","go directly into your sacks."},
            {"Holds items commonly found on the","","Crimson Isle","на Багровом острове","! Items you pickup go","directly into your sacks."},
            {"Holds items commonly found on The","","End","в Крае","! Items you pickup go directly into","your sacks."},
            {"Holds items found during the Year of","the ","Witch","в Год ведьмы","! Items you pickup go","directly into your sacks."},
            {"Holds Mutations found in the","","Greenhouse","в теплице","! Items you pickup go","directly into your sacks."}};
        for(var t:locations){
            Row first=t[0].endsWith(" The")?row(t[0].substring(0,t[0].length()-3),"Хранит ресурсы, полученные","The","")
                :t[0].startsWith("Holds Mutations")?row("Holds ","Хранит ","Mutations","мутации"," ","","found in the",", найденные")
                :t[0].endsWith("Year of")?row("Holds items found during the ","Хранит ресурсы, полученные","Year of","")
                :row(t[0],"Хранит ресурсы, полученные");
            Row second=t[2].equals("various Events")?row("various ","на различных ","Events","событиях",t[4],". Подобранные предметы")
                :row(t[1],"",t[2],t[3],t[4],". Подобранные предметы");
            out.add(List.of(first,second,row(t[5],"сразу попадают в ваши мешки.")));
        }
        out.add(List.of(row("Holds Lava Fishing items commonly","Хранит ресурсы лавовой рыбалки,"),
            row("found in the ","полученные ", "Crimson Isle","на Багровом острове","! Items you",". Подобранные предметы"),
            row("pickup go directly into your sacks.","сразу попадают в ваши мешки.")));
        out.add(List.of(row("Holds ","Хранит ","Gemstones","драгоценные камни"," "," ","of all types and","всех типов и"),
            row("tiers! Items you pickup go directly","качеств. Подобранные камни"),
            row("into your sacks.","сразу попадают в ваши мешки.")));
        out.add(List.of(row("Holds most commonly dropped ","Хранит распространённые ","Runes","руны"),
            row("and their tiers! Items you pickup go","всех уровней. Подобранные руны"),
            row("directly into your sacks","сразу попадают в ваши мешки.")));
        out.add(List.of(row("Holds most commonly found ","Хранит распространённые ","Flowers","цветы","!","!"),
            row("Items you pickup go directly into","Подобранные цветы сразу попадают"),
            row("your sacks.","в ваши мешки.")));
        out.add(List.of(row("Holds various ","Хранит разные ","Baits","наживки"," "," ","for ","для ","Fishing","рыбалки","! Items","! Подобранные предметы"),
            row("you pickup go directly into your","сразу попадают"),
            row("sacks.","в ваши мешки.")));
        for(String trophy:List.of("Fish","Frog"))for(String quality:List.of("BRONZE","SILVER"))
            out.add(List.of(row("Trophy "+trophy,trophy.equals("Fish")?"Трофейные рыбы":"Трофейные лягушки"," "," ","pickups of ","",quality,quality.equals("BRONZE")?"бронзового":"серебряного"),
                row("quality go directly into your sacks.","качества при подборе сразу попадают в ваши мешки.")));
        return out;
    }
    /** Captured a225a022 catalogue corrections; immutable index entries, not frame-time scans. */
    private static List<List<Row>> catalogue64(){
        var out=new ArrayList<List<Row>>();
        out.add(List.of(row("Reduces the ability mana cost of","Снижает расход маны на способности"),
            row("this item by ","этого предмета на ","{0}","{0}","%","%",".",".")));
        for(String skill:List.of("Mining","Farming"))out.add(List.of(
            row("Grants ","Даёт ","+","+","{0}","{0}"," "," ","☘ ","☘ ",skill,"удачи"),
            row("Fortune",skill.equals("Mining")?"добычи":"фермерства",", ",", ","which increases your","повышая шанс"),
            row(skill.equals("Mining")?"chance for multiple drops.":"chance for multiple crops.",
                skill.equals("Mining")?"получить больше ресурсов за раз.":"получить больше урожая за раз.")));
        out.add(List.of(row("Grants ","Даёт ","+","+","{0}","{0}"," "," ","Sea Creature","к шансу выловить"),
            row("Chance","морское существо",".",".")));
        out.add(List.of(row("Avoids breaking stems and baby","Не позволяет ломать стебли и молодые"),row("crops.","культуры.")));
        out.add(List.of(row("▲ ","▲ ","Delicate","Деликатность"," cannot be combined!"," нельзя объединять!")));
        for(String prefix:List.of("- "," - "))out.add(List.of(row(prefix,prefix,"Wand","Жезл")));
        for(String n:List.of("3","6")){
            String unit=n.equals("3")?"блока.":"блоков.";
            out.add(List.of(row("Increases knockback by ","Увеличивает отбрасывание на ",n,n),row("blocks.",unit)));
            out.add(List.of(row("Increases arrow knockback by","Увеличивает отбрасывание стрелами на"),row(n,n," blocks."," "+unit)));
        }
        // Each perk is a closed paragraph. Numeric fragments copy current glyph styles.
        for(var label:new String[][]{{"Finnegan (Mayor)","Финнеган (мэр)"},{"Candidate","Кандидат"},
            {"Pest Eradicator","Истребитель вредителей"},{"Grand Feast","Великий пир"},
            {"GOATed","Лучший из лучших"},{"Blooming Business","Процветающий бизнес"}})
            out.add(List.of(row(label[0],label[1])));
        out.add(List.of(row("The duration of Pesthunter","Бонус охотника на вредителей"),
            row("Phillip's ","Филиппа к ","☘ Farming Fortune ","☘ удаче фермерства","bonus",""),
            row("is now ","теперь длится ","{0}","{0}","m"," мин",". Decreases the spawn",". Перезарядка появления"),
            row("cooldown of ","","Pests ","вредителей ","by ","сокращена на ","{0}","{0}","%.","%.")));
        out.add(List.of(row("Finnegan upgrades the ","Финнеган превращает ","Harvest","Праздник"),
            row("Feast ","урожая ","into the ","в ","Grand Feast","Великий пир",", making",", который"),
            row("it last a full SkyBlock year. Also","длится целый год SkyBlock. Также"),
            row("opens the ","открывается ","Grand Bakery","Большая пекарня",", where",", где"),
            row("Kernels ","Зёрна ","can be spent on more","можно обменять на"),row("exclusive items.","особые предметы.")));
        out.add(List.of(row("Jacob's Farming Contest ","В фермерских конкурсах Джейкоба","brackets",""),
            row("include ","на ","{0}","{0}","% ","% ","more players","больше игроков"),row("each.","попадают в каждую категорию наград.")));
        out.add(List.of(row("All Garden Visitors reward ","Все посетители Сада дают ","Fine","Хорошую"),
            row("Flour","муку"," and "," и ","+","+","{0}","{0}","% Copper ","% меди ","for","за заказы."),
            row("accepting offers. Invites ","Приглашает ","{0}","{0}"," "," ","unique","уникальных"),
            row("visitors to your ","посетителей в ваш ","Garden ","Сад. ","and makes","Посетители"),
            row("higher rarity visitors ","высокой редкости на ","{0}","{0}","% ","% ","more","чаще"),
            row("likely to show up.","появляются в Саду.")));
        String[][] trophies={
            {"Blessed Frog","Благословенная лягушка"},{"Blobfish","Рыба-капля"},{"Bullfrog","Лягушка-бык"},
            {"Cave Frog","Пещерная лягушка"},{"Common Frog","Обычная лягушка"},{"Exploding Frog","Взрывная лягушка"},
            {"Flyfish","Летающая рыба"},{"Golden Fish","Золотая рыба"},{"Gusher","Рыба-фонтан"},
            {"Highlands Frog","Высокогорная лягушка"},{"Karate Fish","Рыба-каратист"},{"Lavahorse","Лавовый конёк"},
            {"Leap Frog","Лягушка-прыгун"},{"Mana Ray","Мановый скат"},{"Moldfin","Плеснеплавник"},
            {"Obfuscated-1","Неопознанная рыба-1"},{"Obfuscated-2","Неопознанная рыба-2"},{"Obfuscated-3","Неопознанная рыба-3"},
            {"Puddle Jumper","Прыгун по лужам"},{"Reality Hopper","Прыгун между реальностями"},{"Sea Frog","Морская лягушка"},
            {"Skeleton Fish","Рыба-скелет"},{"Slugfish","Рыба-слизень"},{"Soul Fish","Рыба душ"},
            {"Steaming-Hot Flounder","Обжигающая камбала"},{"Sulphur Skitter","Серный юрк"},{"Tree Frog","Древесная лягушка"},
            {"Vanille","Ваниль"},{"Volcanic Stonefish","Вулканическая рыба-камень"},{"Wetlands Frog","Болотная лягушка"}};
        String[][] grades={{"BRONZE","бронзовый"},{"SILVER","серебряный"},{"GOLD","золотой"},{"DIAMOND","алмазный"}};
        for(var trophy:trophies)for(var grade:grades){
            if(trophy[0].startsWith("Obfuscated-"))out.add(List.of(row("Obfuscated-","Неопознанная рыба-",
                trophy[0].substring(11),trophy[0].substring(11)," "," — ",grade[0],grade[1]+" трофей")));
            else out.add(List.of(row(trophy[0]+" ",trophy[1]+" — ",grade[0],grade[1]+" трофей")));
        }
        return out;
    }
    /** All captured Turbo I–VII values are finite: exact immutable rows, no new numeric regex. */
    private static List<List<Row>> turbo65(){
        var out=new ArrayList<List<Row>>();
        String[][] crops={
            {"Cactus","кактусов","Turbo-Cacti","Турбокактус"},
            {"Sugar Cane","сахарного тростника","Turbo-Cane","Турботростник"},
            {"Carrot","моркови","Turbo-Carrot","Турбоморковь"},
            {"Cocoa Beans","какао-бобов","Turbo-Cocoa","Турбокакао"},
            {"Melon Slice","ломтиков арбуза","Turbo-Melon","Турбоарбуз"},
            {"Moonflower","лунных цветов","Turbo-Moonflower","Турболунный цветок"},
            {"Mushroom","грибов","Turbo-Mushrooms","Турбогрибы"},
            {"Potato","картофеля","Turbo-Potato","Турбокартофель"},
            {"Pumpkin","тыкв","Turbo-Pumpkin","Турботыква"},
            {"Wild Rose","диких роз","Turbo-Rose","Турбороза"},
            {"Sunflower","подсолнухов","Turbo-Sunflower","Турбоподсолнух"},
            {"Nether Wart","адского нароста","Turbo-Warts","Турбонарост"},
            {"Wheat","пшеницы","Turbo-Wheat","Турбопшеница"}};
        String[] levels={"I","II","III","IV","V","VI","VII"};
        String[] values={"5","10","15","20","25","30","35"};
        for(var crop:crops){
            for(int i=0;i<levels.length;i++){
                out.add(List.of(row(crop[2]+" ",crop[3]+" ",levels[i],levels[i])));
                out.add(List.of(row("Grants ","Даёт ","+","+",values[i],values[i],"☘ ","☘ ",crop[0]+" Fortune","удачи сбора "+crop[1],".",".")));
            }
            String contest=(crop[0].equals("Nether Wart")?"Nether Warts":crop[0])+" Contest";
            for(String medal:List.of("Bronze","Silver"))out.add(List.of(
                row("Requires ","Требуется ",medal+" ",medal.equals("Bronze")?"бронза ":"серебро ","in a ","в ",contest,"конкурсе "+crop[1],"!","!")));
        }
        for(String prefix:List.of("- "," - ")){
            out.add(List.of(row(prefix,prefix,"Turbo Gourd","Турботыква"," ("," (","VI","VI",")",")")));
            out.add(List.of(row(prefix,prefix,"Enchanted Turbo Gourd","Зачарованная турботыква"," ("," (","VII","VII",")",")")));
            out.add(List.of(row(prefix,prefix,"Jacob's Farming Contest","Фермерский конкурс Джейкоба"," ("," (","I","I",")",")")));
            out.add(List.of(row(prefix,prefix,"Farming Tool","Фермерский инструмент")));
        }
        out.add(List.of(row("Turbo Gourd","Турботыква")));
        out.add(List.of(row("Enchanted Turbo Gourd","Зачарованная турботыква")));
        out.add(List.of(row("Right-click to view recipes!","Нажмите ПКМ, чтобы посмотреть рецепты!")));
        for(String old:List.of("V","VI")){
            String next=old.equals("V")?"VI":"VII";
            out.add(List.of(row("When applied to Farming Tool,","На фермерском инструменте повышает"),
                row("increases ALL ","все зачарования ","Turbo-Crop ","«Турбоурожай» ",old,old),
                row("enchantments to ","до ","Turbo-Crop ","«Турбоурожай» ",next,next,".",".")));
            out.add(List.of(row("Requires at least one ","Нужно зачарование ","Turbo-Crop ","«Турбоурожай» ",old,old),
                row("enchantment!","на предмете (хотя бы одно).")));
        }
        return out;
    }
    /** Observed Healer orb, plus pinned dungeon utility cards. No per-frame scans or new hooks. */
    private static List<List<Row>> dungeon68(){
        var out=new ArrayList<List<Row>>();
        out.add(List.of(row("Gear Score: ","Рейтинг снаряжения: ","{0}","{0}")));
        out.add(List.of(row("Shot Cooldown: ","Перезарядка выстрела: ","{0}","{0}","s"," с")));
        for(String[] stat:new String[][]{{"Damage","Урон"},{"Strength","Сила"},{"Health","Здоровье"},{"Defense","Защита"},{"Intelligence","Интеллект"},{"Speed","Скорость"}})
            out.add(List.of(row(stat[0]+": ",stat[1]+": ","+","+","{0}","{0}")));
        out.add(List.of(row("Crit Damage: ","Критический урон: ","+","+","{0}","{0}","%","%")));
        out.add(List.of(row("This item can be reforged!","Этот предмет можно перековать!")));
        out.add(List.of(row("Dungeons only!","Только в подземельях!")));
        out.add(List.of(row("Teleport to an alive player!","Телепортируйтесь к живому игроку!")));
        out.add(List.of(row("Revive a player who has died in a dungeon.","Воскресите игрока, погибшего в подземелье.")));
        out.add(List.of(row("ONE TIME USE.","ОДНОРАЗОВЫЙ.")));
        out.add(List.of(row("This item will vanish from your","Этот предмет исчезнет из вашего"),row("inventory at the end of the Dungeon!","инвентаря в конце подземелья!")));
        out.add(List.of(row("When entering a Dungeon, this stone","При входе в подземелье этот камень"),row("adapts to its user and allows them to","подстраивается под владельца и позволяет ему"),row("use their class abilities.","использовать способности своего класса.")));
        out.add(List.of(row("Cooldown: ","Перезарядка: ","{0}","{0}","s"," с")));
        out.add(List.of(row("Shortbow: Instantly shoots!","Короткий лук: стреляет мгновенно!")));
        out.add(List.of(row("Spirit Item: ","Призрачный предмет: ","When turned into a ghost,","когда вы становитесь призраком,"),row("this item becomes a Ghost Ability.","он превращается в призрачную способность.")));
        out.add(List.of(row("Spirit Ability: ","Призрачная способность: ","Spirit Bomb","Призрачная бомба"," "," ")));
        out.add(List.of(row("Orb of Life","Сфера жизни"," "," ","RIGHT CLICK TEAMMATE","ПКМ ПО СОЮЗНИКУ")));
        out.add(List.of(row("Grants ","Даёт ","+","+","{0}","{0}","",""," "," ","Health Regen","регенерации здоровья",".",".")));
        for(String[] drain:new String[][]{{"Mana Drain","Расход маны"},{"Vitality Drain","Расход живучести"}})
            out.add(List.of(row(drain[0]+": ",drain[1]+": ","{0}","{0}"," per second"," в секунду")));
        out.add(List.of(row("Max duration: ","Макс. длительность: ","{0}","{0}","s"," с")));
        out.add(List.of(row("Charges: ","Заряды: ","{0}","{0}","/","/","{1}","{1}","⸕","⸕")));
        out.add(List.of(row("Spawns a wither minion every ","Призывает иссушителя каждые ","{0}","{0}"),
            row("seconds up to a maximum ","с; одновременно — не более ","{0}","{0}"," "," ","wither.","иссушителя."),
            row("Your withers will travel to and","Ваши иссушители летят к"),row("explode on nearby enemies.","ближайшим врагам и взрываются.")));
        for(boolean extra:List.of(false,true))out.add(List.of(row("Increase the damage you deal with","Увеличивает урон от стрел"),
            row(extra?"arrows by an extra ":"arrows by ",extra?"дополнительно на ":"на ","{0}","{0}","%","%",".",".")));
        out.add(List.of(row("Reduces the damage you take from","Уменьшает урон от иссушителей"),
            row("withers by ","на ","{0}","{0}","%","%"," "," ","and increases your","и увеличивает"),
            row("arrow damage by ","урон от стрел на ","{0}","{0}","%","%",".",".")));
        out.add(List.of(row("Every ","За каждые ","{0}","{0}"," "," ","Catacombs levels, this armor piece","уровней Катакомб эта часть брони"),
            row("gains +","получает +","{0}","{0}","%","%"," "," ","stats.","к характеристикам.")));
        out.add(List.of(row("Grants additional bonuses based on your","Даёт дополнительные бонусы в зависимости"),row("selected dungeon class!","от выбранного класса подземелий!")));
        out.add(List.of(row("Tank","Танк",": +",": +","{0}","{0}","",""," "," ","Defense","защиты"," and reduces damage"," и уменьшает урон"),
            row("taken by ","на ","{0}","{0}","%","%"," "," ","for each piece you have equipped","за каждую надетую часть комплекта,"),
            row("if you are hit by the same monster within ","если тот же монстр попадает по вам в течение ","{0}","{0}"),row("seconds.","с.")));
        // Skill requirements already have a dedicated semantic span renderer.
        // Do not mask its current skill/prose/level styles with a generic whole phrase.
        for(String floor:List.of("I","II","III","IV","V","VI","VII"))out.add(List.of(row("❣","❣"," "," ","Requires ","Требуется прохождение ","The ","","Catacombs","Катакомб"," Floor ",", этаж ",floor,floor," Completion.",".")));
        for(String rarity:List.of("UNCOMMON","RARE","EPIC","LEGENDARY","SPECIAL"))for(String[] type:new String[][]{{"ITEM","ПРЕДМЕТ"},{"BOW","ЛУК"},{"PICKAXE","КИРКА"}})
            out.add(List.of(row(rarity,rarity," DUNGEON "," ",type[0],type[1]+" ПОДЗЕМЕЛИЙ")));
        for(String[] ability:new String[][]{{"Wish","Желание"},{"Healing Circle","Круг исцеления"},{"Guided Sheep","Управляемая овца"},{"Thunderstorm","Гроза"},{"Throwing Axe","Метательный топор"},{"Ragnarok","Рагнарёк"},{"Explosive Shot","Взрывной выстрел"},{"Rapid Fire","Беглый огонь"},{"Seismic Wave","Сейсмическая волна"},{"Castle of Stone","Каменная крепость"},{"Revive","Воскрешение"},{"Spirit Leap","Призрачный скачок"},{"Dungeon Breaker","Разрушитель подземелий"},{"Spirit Bomb","Призрачная бомба"},{"Stun Potion","Зелье оглушения"},{"Absorption Potion","Зелье поглощения"},{"Haunt","Призрак"}})
            for(String gap:List.of(" ","  "))for(String button:List.of("LEFT CLICK","RIGHT CLICK","DIG"))
                out.add(List.of(row("Ability: ","Способность: ",ability[0],ability[1],gap,gap,button,button.equals("LEFT CLICK")?"ЛКМ":button.equals("RIGHT CLICK")?"ПКМ":"ПРИ ДОБЫЧЕ")));
        for(boolean partial:List.of(false,true))for(String unit:partial?List.of(" секунды."):List.of(" seconds"," seconds.")){
            out.add(List.of(
                row("Heals everyone in your group to full","Полностью восстанавливает здоровье всей"),
                row("health and grants them a shield for","группы и даёт каждому щит на"),
                row("{0}","{0}","%","%"," of their maximum health. The"," от максимального здоровья."),
                row("cooldown is reduced by ","Перезарядка короче на ","{0}","{0}",unit," с"),
                row("for every player below ","за каждого игрока со здоровьем ниже ","{0}","{0}","%","%",partial?" здоровья.":" health.",".")));
            out.add(List.of(
                row("Launches a healing orb that creates","Бросает исцеляющую сферу, создающую"),
                row("a healing circle where it lands. The","круг исцеления в месте падения."),
                row("healing circle heals friendly players","Союзники внутри круга восстанавливают"),
                row("inside for ","по ","{0}","{0}","%","%"," of their health every"," своего здоровья"),
                row(partial?"секунду.":"second.","каждую секунду.")));
        }
        out.add(List.of(row("Place an explosive trap that","Устанавливает взрывную ловушку,"),
            row("triggers when mobs walk over it","срабатывающую при наступании моба:"),
            row("dealing ","наносит ","{0}","{0}","-","–","{1}","{1}"," "," ","damage","урона"),
            row("based on the dungeon floor you are","в зависимости от текущего этажа"),row("on.","подземелья.")));
        out.add(List.of(row("Allows you to teleport to any teammate! Grants","Перемещает к любому союзнику. Даёт"),
            row("{0}","{0}"," second of immunity after teleporting,"," с неуязвимости после перемещения;"),
            row("immunity is cancelled upon dealing damage.","она исчезает при нанесении урона.")));
        out.add(List.of(row("Throw a potion which temporarily stuns all","Бросает зелье, временно оглушающее всех"),
            row("monsters in a ","монстров в радиусе ","{0}","{0}"," block radius."," блоков.")));
        out.add(List.of(row("Shoots ","Выпускает ","{0}","{0}"," Arrows per second for "," стрел в секунду в течение ","{1}","{1}"),
            row("seconds! Arrows deal ","с! Стрелы наносят ","{0}","{0}","%","%"," of what"," от своего"),
            row("they would normally deal.","обычного урона.")));
        out.add(List.of(row("While in ","В ","The Catacombs","Катакомбах",", consume "," расходует ","{0}","{0}","⸕","⸕"),
            row("charge to break a block. ","заряд на разрушение блока. До ","{0}","{0}"," blocks can"," блоков"),
            row("be broken at a time, and re-appear after","можно сломать одновременно. Они вернутся через"),
            row("{0}","{0}","s"," с",". ",". ","{1}","{1}","⸕","⸕"," charges are regenerated each"," заряда восстанавливаются каждую"),
            row("second.","секунду.")));
        out.add(List.of(row("An extremely fragile but powerful","Очень хрупкий, но мощный"),
            row("bow, capable of damaging ","лук, способный ранить ","Thorn","Торна",",",","),
            row("despite him being a spirit!","хотя тот и является духом!")));
        out.add(List.of(row("Replaces the arrows that you shoot","Заменяет выпускаемые стрелы"),
            row("with exploding wither skulls!","взрывающимися черепами иссушителя!")));
        out.add(List.of(row("Your arrows have a ","Ваши стрелы с шансом ","{0}","{0}","%","%"," "," ","chance to","могут"),
            row("bounce to another target after it","перелететь к другой цели после"),row("hits something.","попадания.")));
        for(String icon:List.of("","☠")){
            out.add(List.of(row("Deals ","Наносит ","+","+","{0}","{0}","%","%"," "," ","damage to ","урона по ",icon,icon," "," ","Undead","неживым"),row("mobs.","существам.")));
            out.add(List.of(row("Deals ","Наносит ","+","+","{0}","{0}","%","%"," "," ","damage to ","урона по ",icon,icon," "," ","Undead","неживым"," "," ","Mobs","существам"),
                row("for every ","за каждый ","{0}","{0}","%","%"," "," ","of your missing health.","недостающего вам здоровья.")));
        }
        out.add(List.of(row("A shard of a Dungeon Blessing,","Осколок благословения подземелий,"),
            row("capable of being bestowed upon","который можно передать"),
            row("another player for a continuous buff,","другому игроку для постоянного усиления,"),
            row("but draining mana.","расходующего ману.")));
        out.add(List.of(row("Shoots a spirit that deals ","Выпускает духа, наносящего ","{0}","{0}"),row("damage on impact.","урона при попадании.")));
        return out;
    }
    // A style change commonly falls on a space ("Catacombs §7XP"). Keep that
    // space separate rather than rejecting or flattening the semantic word.
    private static Row denseRow(String... pairs){
        var source=new ArrayList<String>();var target=new ArrayList<Part>();
        for(int n=0;n<pairs.length;n+=2){
            String from=pairs[n],to=pairs[n+1];int a=0,b=from.length();
            while(a<b&&from.charAt(a)==' ')a++;
            while(b>a&&from.charAt(b-1)==' ')b--;
            if(a==b){target.add(new Part(source.size(),to));source.add(from);continue;}
            int ta=0,tb=to.length();
            if(a>0)while(ta<tb&&to.charAt(ta)==' ')ta++;
            if(b<from.length())while(tb>ta&&to.charAt(tb-1)==' ')tb--;
            if(a>0){target.add(new Part(source.size(),to.substring(0,ta)));source.add(from.substring(0,a));}
            target.add(new Part(source.size(),to.substring(ta,tb)));source.add(from.substring(a,b));
            if(b<from.length()){target.add(new Part(source.size(),to.substring(tb)));source.add(from.substring(b));}
        }
        return new Row(List.copyOf(source),List.copyOf(target));
    }
    /** Reviewed against the pinned public catalogue, 2026-10-16. Existing indexed
     * tooltip path only: no new HUD/frame hooks, provider or mutable style cache. */
    private static List<List<Row>> dense77(){
        var out=new ArrayList<List<Row>>();
        // Book effects. Numeric slots bind the actual displayed level values.
        out.add(List.of(denseRow("Gain ","Даёт ","+","+","{0}","{0}","☯ Mining Wisdom","☯ мудрости горного дела"," "," ","and a","и"),
            denseRow("{0}","{0}","%","%"," "," ","chance to drop an","шанса получить"),denseRow("enchanted item.","зачарованный предмет.")));
        for(String icon:List.of("☘",""))out.add(List.of(
            denseRow("Gain ","Даёт ","+","+","{0}","{0}","☯ Farming Wisdom","☯ мудрости фермерства"," "," ","and","и"),
            denseRow("+","+","{0}","{0}",icon+" Farming Fortune",icon+" удачи фермерства",".",".")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}"," Sea Creature"," шанса встречи"),
            denseRow("Chance","с морскими существами"," "," ","and ","и ","+","+","{0}","{0}","☯ Fishing","☯ мудрости"),
            denseRow("Wisdom","рыбалки"," "," ","when killing Sea","при уничтожении"),denseRow("Creatures.","морских существ.")));
        out.add(List.of(denseRow("Gain ","Даёт ","+","+","{0}","{0}","%","%"," "," ","Catacombs","опыта Катакомб"," XP & "," и ","+","+","{1}","{1}","%","%"),
            denseRow("Class","опыта класса"," XP, doubled on ","; удваивается при оценке ","S+","S+"," ","","runs.","."),
            denseRow("Grants ","Даёт ","+","+","{0}","{0}","❤","❤"," per 10 "," за каждые 10 уровней ","Catacombs","Катакомб"," levels.",".")));
        out.add(List.of(denseRow("Increases the chance for ","Повышает шанс выловить ","Treasure","сокровища"),
            denseRow("catches to be ","качества ","GREAT","ОТЛИЧНОЕ"," "," ","or","или"),
            denseRow("OUTSTANDING","ВЫДАЮЩЕЕСЯ"," "," ","by ","на ","{0}","{0}","%","%",".",".")));
        out.add(List.of(denseRow("Gain a ","Даёт ","{0}","{0}","%","%"," "," ","chance to spawn a","шанса выловить"),denseRow("Corrupted Sea Creature.","осквернённое морское существо.")));
        out.add(List.of(denseRow("Gain ","Даёт ","+","+","{0}","{0}"," Defense"," защиты"," "," ","for ","на ","{1}","{1}","s"," с"," "," ","on the","при первом"),denseRow("first hit from an enemy.","попадании противника по вам.")));
        out.add(List.of(denseRow("Reduces how much you are slowed","Уменьшает замедление"),denseRow("in the water by ","при движении в воде на ","{0}","{0}","%","%",".",".")));
        out.add(List.of(denseRow("Grants a ","Даёт ","{0}","{0}","%","%"," "," ","chance for mobs","шанса получить"),
            denseRow("and ores to drop double","вдвое больше опыта с мобов"),denseRow("experience.","и при добыче руды.")));
        out.add(List.of(denseRow("Ignites your enemies for ","Поджигает врагов на ","{0}","{0}","s"," с",",",","),
            denseRow("dealing ","нанося ","{0}","{0}","%","%"," "," ","of your damage","вашего урона"),denseRow("per second.","в секунду.")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}"," True Defense"," истинной защиты"," "," ","against","от"),denseRow("fire and lava.","огня и лавы.")));
        out.add(List.of(denseRow("Increases melee damage dealt by","Увеличивает урон ближнего боя на"),
            denseRow("{0}","{0}","%","%"," "," ","for the first hit on a","при первом попадании"),denseRow("mob.","по мобу.")));
        out.add(List.of(denseRow("Sea creatures start with ","У морских существ на ","{0}","{0}","%","%"),denseRow("reduced health.","меньше начального здоровья.")));
        for(String icon:List.of("☘",""))out.add(List.of(
            denseRow("Grants ","Даёт ","{0}","{0}",icon+" Farming Fortune",icon+" удачи фермерства"," "," ","for","×"),
            denseRow("crops, multiplied by your ","этап её развития в ","Garden","Саду"),
            denseRow("milestone for that crop.","для соответствующей культуры.")));
        out.add(List.of(denseRow("Heal ","Восстанавливает ","{0}","{0}","❤","❤"," "," ","every time you hit a mob.","при каждом попадании по мобу.")));
        out.add(List.of(denseRow("Regain ","Восстанавливает ","{0}","{0}","%","%"," "," ","of your mana on","вашей маны"),denseRow("hit.","при попадании.")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}"," Health Regen"," восстановления здоровья"," "," ","while out","вне"),denseRow("of combat.","боя.")));
        out.add(List.of(denseRow("Regen ","Даёт ","+","+","{0}","{0}"," Vitality"," жизненной силы"," "," ","every time you","при каждом"),denseRow("hit a mob.","попадании по мобу.")));
        out.add(List.of(denseRow("Reduces the ability mana cost of","Снижает расход маны способностей"),denseRow("this item by ","этого предмета на ","{0}","{0}","%","%",".",".")));
        for(String[] type:new String[][]{{"⸙","Woodland","лесным"},{"","Arthropod","членистоногим"},{"✈","Airborne","летающим"},{"♆","Magmatic","магматическим"}})
            out.add(List.of(denseRow("Increases damage dealt to ","Увеличивает урон по ",type[0],type[0]),
                denseRow(type[1],type[2]," "," ","mobs by ","мобам на ","{0}","{0}","%","%",".",".")));
        // Pet mechanics: keep current values, units, icons and semantic colors.
        for(String[] name:new String[][]{{"Absorb","Поглощение"},{"Compact","Уплотнение"},{"Expertise","Мастерство"},{"Hecatomb","Гекатомба"},{"Cultivating","Возделывание"}})
            out.add(List.of(denseRow("▲","▲"," "," ",name[0],"«"+name[1]+"»"," cannot be combined!"," нельзя объединить!")));
        var logs=List.of("1k","5k","25k","100k","300k","1.5M","5M","10M","25M");
        var levels=List.of("II","III","IV","V","VI","VII","VIII","IX","X");
        for(int i=0;i<logs.size();i++)out.add(List.of(denseRow("Break ","Срубите ",logs.get(i),logs.get(i)," Logs for Tier "," брёвен для ступени ",levels.get(i),levels.get(i))));
        out.add(List.of(denseRow("Increases the chance to catch","Повышает шанс выловить"),
            denseRow("higher-tiered ","более качественные ","Trophies","трофеи"," "," ","by ","на ","{0}","{0}","%","%",".",".")));
        out.add(List.of(denseRow("Grants a ","С шансом ","{0}","{0}","%","%"," "," ","chance for ","","Tree Gifts","дары дерева"),denseRow("to contain double loot.","содержат вдвое больше добычи.")));
        out.add(List.of(denseRow("Makes fish swim ","Рыба подплывает к поплавку на ","{0}","{0}","%","%"," "," ","faster towards","быстрее"),denseRow("your bobber.","при ожидании поклёвки.")));
        for(String[] type:new String[][]{{"explosions.","взрывов."},{"projectiles.","снарядов."}})
            out.add(List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}"," "," "," Defense"," защиты"," "," ","against","от"),denseRow(type[0],type[1])));
        // Additional interacting pet mechanics, including experiment rewards.
        out.add(List.of(denseRow("Zaps your enemies for ","Бьёт врагов на ","{0}","{0}","x","×"," "," ","your ","вашего ","",""),
            denseRow("Intelligence","интеллекта"," "," ","every ","каждые ","3","3","s"," с",".",".")));
        out.add(List.of(denseRow("Regenerate ","Восстанавливает на ","{2}","{2}","%","%"," "," ","extra mana, doubled","больше маны; вдвое больше"),denseRow("when near or in water","в воде и рядом с водой.")));
        out.add(List.of(denseRow("Gain ","Даёт ","+","+","{3}","{3}","%","%"," "," ","chance to find ","шанса найти ","ultra rare","сверхредкие"),
            denseRow("books in ","книги в ","Superpairs","Суперпарах",".",".")));
        out.add(List.of(denseRow("Slime and Magma Cube minions work","Миньоны слизней и магмовых кубов работают на"),
            denseRow("{0}","{0}","%","%"," "," ","faster while on your island","быстрее, пока вы на своём острове.")));
        out.add(List.of(denseRow("Deal ","Наносит на ","{1}","{1}","%","%"," "," ","more damage to slimes","больше урона слизням.")));
        out.add(List.of(denseRow("Buffs the stats of ","Усиливает характеристики ","Rekindled Ember","возрождённых углей"),
            denseRow("Armor","брони"," "," ","by ","на ","{2}","{2}","%","%",".",".")));
        out.add(List.of(denseRow("Take ","Получаемый урон на ","{0}","{0}","%","%"," "," ","less damage from ","меньше от ","",""),
            denseRow("Skeletal","скелетных"," "," ","mobs.","мобов.")));
        out.add(List.of(denseRow("Deal ","Наносит на ","{1}","{1}","%","%"," "," ","more damage to ","больше урона "," Wither"," иссушителям"),denseRow("mobs.","и подобным существам.")));
        out.add(List.of(denseRow("Upon hitting an enemy inflict the","При попадании накладывает иссушение,"),
            denseRow("wither effect for ","наносящее ","{2}","{2}","%","%"," "," ","damage over 3","урона за 3"),denseRow("seconds","секунды.")));
        out.add(List.of(denseRow("Melee attacks reduce healing by ","Удары ближнего боя снижают исцеление на ","{1}","{1}","%","%"),
            denseRow("for ","на ","10","10","s"," с",".",".")));
        out.add(List.of(denseRow("Deal ","Наносит на ","+","+","{2}","{2}","%","%"," "," ","damage against targets with","больше урона целям, если"),
            denseRow("no other mobs within ","в радиусе ","15","15"," "," ","blocks.","блоков нет других мобов.")));
        out.add(List.of(denseRow("Reduce the damage taken from","Уменьшает получаемый урон"),denseRow("zombies by ","от зомби на ","{0}","{0}","%","%",".",".")));
        out.add(List.of(denseRow("Deal ","Наносит на ","{1}","{1}","%","%"," "," ","more damage to ","больше урона "," Undead"," нежити"),denseRow("mobs",".")));
        out.add(List.of(denseRow("Increases all stats on ","Усиливает характеристики ","Undead ༕","неживой ༕"),
            denseRow("armor by ","брони на ","{2}","{2}","%","%",".",".")));
        // Complete mob paragraphs, not loose replacements of generic words.
        out.add(List.of(denseRow("Found near the ","Обитает возле ","Black Lagoon","Чёрной лагуны"),denseRow("in the Rift.","в Разломе.")));
        out.add(List.of(denseRow("Shoots terrifying arrows that deal","Стреляет устрашающими стрелами,"),denseRow("9sф Damage.","наносящими 9sф урона.")));
        out.add(List.of(denseRow("Also has a melee attack that deals","Также атакует в ближнем бою,"),denseRow("2sф Damage.","нанося 2sф урона.")));
        out.add(List.of(denseRow("The bee equivalent of a UFO.","НЛО в мире пчёл.")));
        out.add(List.of(denseRow("So cute you just want to take one","Такой милый, что хочется забрать"),denseRow("home.","его домой.")));
        out.add(List.of(denseRow("Though still an adolescent, it will","Пока ещё детёныш, но готов"),denseRow("defend honey with its life.","защищать мёд ценой собственной жизни.")));
        out.add(List.of(denseRow("Loves honey, the bees are just","Обожает мёд; пчёлы — лишь"),denseRow("collateral.","сопутствующие потери.")));
        out.add(List.of(denseRow("Watch the foliage for the slightest","Следите за малейшим движением"),denseRow("sign of movement.","среди листвы.")));
        out.add(List.of(denseRow("Unaware it even eats critters, just","Даже не знает, что ест зверьков:"),denseRow("thinks the floor tastes nice.","просто считает пол вкусным.")));
        out.add(List.of(denseRow("While at less than ","При здоровье ниже ","{0}","{0}","% HP","%",", reduce",":"),
            denseRow("incoming damage by ","получаемый урон снижается на ","{0}","{0}","%","%",". Additionally,",". Также даёт"),
            denseRow("gain a temporary shield equal to ","временный щит на ","{0}","{0}","%","%"),
            denseRow("of your maximum health and deal ","максимального здоровья и на ","{0}","{0}","%","%"),denseRow("more damage.","увеличивает наносимый урон.")));
        out.add(List.of(denseRow("Your iron plating causes ","Железная броня отражает ","{0}","{0}","%","%"," "," ","of",""),
            denseRow("attacks to ricochet and hit the","атак обратно"),denseRow("attacker","в атакующего.")));
        out.add(List.of(denseRow("Every 5 hits, throw the enemy up into","Каждые 5 ударов подбрасывает врага"),
            denseRow("the air and deal ","в воздух и наносит ","5","5","x","-кратный"," "," ","damage ","урон ","(5s","(5 с"),denseRow("cooldown)","перезарядки).")));
        out.add(List.of(denseRow("Gives a ","Даёт ","{1}","{1}","%","%"," "," ","shield after not taking","щита, если не получать"),denseRow("damage for 10s","урон в течение 10 с.")));
        out.add(List.of(denseRow("Increases your total mana by ","Увеличивает максимальный запас маны на ","{2}","{2}","%","%"),denseRow("while in dungeons","в подземельях.")));
        out.add(List.of(denseRow("Jump potions also give ","Зелья прыгучести также дают ","+","+","{0}","{0}"," "," ","speed","скорости.")));
        out.add(List.of(denseRow("Farming minions work ","Фермерские миньоны работают на ","{2}","{2}","%","%"," "," ","faster","быстрее,"),denseRow("while on your island","пока вы на своём острове.")));
        out.add(List.of(denseRow("Earn ","Даёт ","+","+","{0}","{0}","%","%"," "," ","more Exp when mining.","дополнительного опыта при добыче.")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{2}","{2}"," Mining Speed"," скорости добычи"," "," ","and","и"),
            denseRow("permanent ","постоянную ","Haste ","Спешку ","{3}","{3}",".",".")));
        for(String icon:List.of("","⊙"))out.add(List.of(denseRow("Take ","Получаемый урон на ","{0}","{0}","%","%"," "," ","less damage from ","меньше от ",icon+" Ender",icon+" существ Края"),denseRow("mobs",".")));
        out.add(List.of(denseRow("Buffs Transmission abilities, granting","Усиливает способности перемещения, давая"),
            denseRow("{1}","{1}"," "," ","weapon damage for 5s on use","урона оружия на 5 с после применения.")));
        out.add(List.of(denseRow("Increases your odds to find a","Повышает шанс найти"),denseRow("special Zealot by ","особого фанатика на ","{2}","{2}","%","%",".",".")));
        // Both observed Stomp line wraps and both fortune icons.
        out.add(List.of(denseRow("Gain ","Даёт ","{0}","{0}"," Defense"," защиты"," "," ","for every 100 ","за каждые 100 ","",""),denseRow("Speed","скорости",".",".")));
        out.add(List.of(denseRow("Gain ","Даёт ","{0}","{0}"," Defense"," защиты"," "," ","for every 100","за каждые 100"),denseRow(" Speed"," скорости",".",".")));
        out.add(List.of(denseRow("Gain ","Даёт ","{1}","{1}"," Health"," здоровья"," "," ","for every 10 ","за каждые 10 ","",""),denseRow("Defense","защиты",".",".")));
        for(String icon:List.of("","☘"))out.add(List.of(denseRow("Grants ","Даёт ","+","+","{2}","{2}",icon+" Farming Fortune",icon+" удачи фермерства",", which",", что"),
            denseRow("increases your chance for multiple","повышает шанс получить несколько"),denseRow("drops.","предметов за один сбор.")));
        // Golems: mining pets, equipment, ingredient lore and the active sword.
        out.add(List.of(denseRow("Gain ","Даёт ","{0}","{0}","%","%"," "," ","more ","дополнительной ","Glacite Powder","глацитовой пыли"," "," ","from","из"),denseRow("most sources.","большинства источников.")));
        for(String tail:List.of("while in","while in the"))out.add(List.of(denseRow("Gain ","Даёт ","+","+","{1}","{1}"," "," "," Mining Fortune"," удачи добычи"," "," ",tail,"в"),denseRow("Glacite Mineshafts","Глацитовых шахтах",".",".")));
        out.add(List.of(denseRow("Gain ","Даёт ","+","+","{2}","{2}"," "," "," Pristine"," безупречности"," "," ","for every","за каждый"),
            denseRow("Frozen Corpse","замёрзший труп"," ",", ","you've looted in the","который вы обыскали"),denseRow("current ","в текущей ","Glacite Mineshaft","Глацитовой шахте",".",".")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{0}","{0}"," Mining Speed"," скорости добычи"," "," ","when mining","при добыче"),denseRow("Mithril","мифрила",".",".")));
        out.add(List.of(denseRow("Increases all ","Увеличивает все ","Combat Stats","боевые характеристики"," "," ","by ","на ","+","+","{1}","{1}","%","%"),denseRow("on ","на ","Mining Islands","шахтёрских островах",".",".")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{2}","{2}","% ᠅ Mithril Powder","% ᠅ мифриловой пыли"," "," ","from","из"),denseRow("all sources.","всех источников.")));
        out.add(List.of(denseRow("Grants ","Даёт ","+","+","{3}","{3}","%  Magic Find","%  шанса редкой находки"," "," ","while on","на"),denseRow("Mining Islands","шахтёрских островах",".",".")));
        out.add(List.of(denseRow("Grants the wearer ","Даёт владельцу ","Absorption ","Поглощение ","{0}","{0}"," for","на"),
            denseRow("{0}","{0}"," seconds"," с"," when they kill an enemy."," при убийстве врага.")));
        out.add(List.of(denseRow("Drops an ","Выбрасывает ","iron ingot","железный слиток"," "," ","when you sneak!","при приседании!")));
        out.add(List.of(denseRow("An ancient plant imbued with the","Древний цветок, наполненный"),denseRow("unyielding power of truth.","несокрушимой силой истины.")));
        out.add(List.of(denseRow("Punch the ground, damaging enemies in a","Ударяет по земле, нанося врагам"),
            denseRow("hexagon around you for ","в шестиугольнике вокруг вас ","{0}","{0}"," base Magic"," базового магического"),denseRow("Damage.","урона.")));
        return out;
    }
    private static final List<List<Row>> GROUPS=allGroups();
    private static List<List<Row>> allGroups(){var out=new ArrayList<List<Row>>(sacks());out.addAll(BASE_GROUPS);out.addAll(catalogue64());out.addAll(turbo65());out.addAll(dungeon68());out.addAll(dense77());return List.copyOf(out);}
    private record Group(int id,List<Row> rows,List<String> templateKeys,boolean paragraphEnd) {}
    private record Candidate(Group group,int start,List<Row> bound) {}
    /** Numeric-prefix trie is frozen at class initialization; lookups allocate nothing. */
    private static final class PrefixBuilder {
        final SortedMap<Character,PrefixBuilder> children=new TreeMap<>();final List<Group> own=new ArrayList<>();
        void add(String prefix,Group group){var node=this;for(int i=0;i<prefix.length();i++)node=node.children.computeIfAbsent(prefix.charAt(i),c->new PrefixBuilder());node.own.add(group);}
        PrefixNode freeze(List<Group> ancestors){
            var terminal=this;var tail=new StringBuilder();
            // Compress only chains with no new candidate. Ancestor candidates
            // remain the fallback if a whole tail does not match current text.
            while(terminal.own.isEmpty()&&terminal.children.size()==1){var edge=terminal.children.entrySet().iterator().next();tail.append(edge.getKey());terminal=edge.getValue();}
            var candidates=new ArrayList<Group>(ancestors);candidates.addAll(terminal.own);candidates.sort(Comparator.comparingInt(Group::id));var ordered=List.copyOf(candidates);
            var chars=new char[terminal.children.size()];var nodes=new PrefixNode[terminal.children.size()];int i=0;
            for(var entry:terminal.children.entrySet()){chars[i]=entry.getKey();nodes[i++]=entry.getValue().freeze(ordered);}
            return new PrefixNode(tail.toString(),chars,nodes,ordered);
        }
    }
    private static final class PrefixNode {
        private final String tail;private final char[] chars;private final PrefixNode[] children;private final List<Group> candidates;
        PrefixNode(String tail,char[] chars,PrefixNode[] children,List<Group> candidates){this.tail=tail;this.chars=chars;this.children=children;this.candidates=candidates;}
        List<Group> groups(String text){
            var node=this;var fallback=List.<Group>of();int offset=0;
            for(;;){
                if(!text.startsWith(node.tail,offset))return fallback;
                offset+=node.tail.length();if(offset>=text.length()||node.chars.length==0)return node.candidates;
                char c=text.charAt(offset);int at=node.chars.length==1?(node.chars[0]==c?0:-1):Arrays.binarySearch(node.chars,c);
                if(at<0)return node.candidates;fallback=node.candidates;node=node.children[at];offset++;
            }
        }
    }
    /** Immutable template-only index; never stores current text, styles or override decisions. */
    private static final class GroupIndex {
        final Map<String,List<Group>> constantHeads;
        private final PrefixNode numericHeads;final List<Group> empty;final int longest;
        GroupIndex(){
            var numeric=new PrefixBuilder();var constants=new HashMap<String,List<Group>>();var blank=new ArrayList<Group>();int max=0;
            for(int id=0;id<GROUPS.size();id++){
                var rows=GROUPS.get(id);max=Math.max(max,rows.size());var first=rows.getFirst();
                var keys=new ArrayList<String>();for(var row:rows){keys.add(row.text());keys.addAll(row.source());
                    // A prior renderer may have translated only this known stat.
                    // Original English overrides must still veto the closed alias.
                    for(String piece:row.source()){String alias=ENGLISH_ALIASES.get(piece);if(alias!=null){keys.add(alias);keys.add(row.text().replace(piece,alias));}}}
                // The short Syphon publication omits a cooldown. It must not
                // swallow the first two rows of a longer, malformed paragraph.
                // Classify once while building the index, not in a frame loop.
                boolean paragraphEnd=rows.size()==2
                    &&first.text().equals("Regen +{0} Vitality every time you")
                    &&rows.get(1).text().equals("hit a mob.");
                var group=new Group(id,rows,List.copyOf(keys),paragraphEnd);
                if(first.text().isEmpty()){blank.add(group);continue;}
                // Constant heads need an exact template lookup, not every paragraph
                // beginning with the same letter. Numeric heads use their full prefix.
                if(first.numeric()==null){constants.computeIfAbsent(first.text(),k->new ArrayList<>()).add(group);continue;}
                if(first.prefix().isEmpty()){
                    // Empty numeric prefixes can begin only with an ASCII digit, or
                    // the literal placeholder accepted by bind's exact-text branch.
                    numeric.add(first.text().substring(0,1),group);for(char c='0';c<='9';c++)numeric.add(String.valueOf(c),group);
                }else numeric.add(first.prefix(),group);
            }
            numericHeads=numeric.freeze(List.of());
            var combined=new HashMap<String,List<Group>>();
            for(var entry:constants.entrySet()){
                var candidates=new ArrayList<Group>(entry.getValue());
                candidates.addAll(numericHeads.groups(entry.getKey()));
                candidates.sort(Comparator.comparingInt(Group::id));
                combined.put(entry.getKey(),List.copyOf(candidates));
            }
            constantHeads=Map.copyOf(combined);
            empty=List.copyOf(blank);longest=max;
        }
        List<Group> groups(String text){
            if(text.isEmpty())return empty;
            var exact=constantHeads.get(text);if(exact!=null)return exact;
            return numericHeads.groups(text);
        }
    }
    private static final GroupIndex GROUP_INDEX=new GroupIndex();
    private static Match reviewed(List<String> rows,int index){
        if(index<0||index>=rows.size())return null;Candidate best=null;
        for(int start=Math.max(0,index-GROUP_INDEX.longest+1);start<=index;start++)for(var candidate:GROUP_INDEX.groups(rows.get(start))){
            // Original precedence is group id first, then earliest start. Never let
            // an overridden earlier paragraph silently fall through to another one.
            if(best!=null&&candidate.id()>=best.group().id())continue;
            var group=candidate.rows();if(start+group.size()>rows.size()||index>=start+group.size())continue;
            if(candidate.paragraphEnd()&&start+group.size()<rows.size()
                &&!rows.get(start+group.size()).isBlank())continue;
            // Reject a wrong constant unit/last row before allocating bound numeric
            // pieces. Template-only preflight: final regex/override/precedence unchanged.
            if(!group.getFirst().shape(rows.get(start)))continue;
            boolean possible=true;for(int i=1;i<group.size();i++)if(!group.get(i).shape(rows.get(start+i))){possible=false;break;}
            if(!possible)continue;
            var first=group.getFirst().bind(rows.get(start));if(first==null)continue;
            boolean matches=true;var bound=new ArrayList<Row>(group.size());bound.add(first);
            for(int i=1;i<group.size();i++){var row=group.get(i).bind(rows.get(start+i));if(row==null){matches=false;break;}bound.add(row);}
            if(matches)best=new Candidate(candidate,start,bound);
        }
        if(best==null)return null;
        if(!SkyRuClient.tooltipPowerAllowed(best.group().templateKeys()))return null;
        // Constant keys were checked above. Only concrete substitutions are fresh.
        var keys=new ArrayList<String>();
        for(int r=0;r<best.bound().size();r++){
            var row=best.bound().get(r);var template=best.group().rows().get(r);if(row==template)continue;
            keys.add(row.text());
            for(int p=0;p<row.source().size();p++)if(!row.source().get(p).equals(template.source().get(p)))keys.add(row.source().get(p));
        }
        if(!keys.isEmpty()&&!SkyRuClient.tooltipPowerAllowed(keys))return null;
        return new Match(best.bound().get(index-best.start()));
    }
    static FormattedCharSequence render(FormattedCharSequence original,List<String> rows,int index){
        var match=reviewed(rows,index);if(match==null)return null;
        return render(original,match);
    }
    private static FormattedCharSequence render(FormattedCharSequence original,Match match){
        var current=StyledRuns.collect(original);if(!current.text.equals(match.row().text()))return original;
        // A whole-row uniform style has no semantic boundaries to remap. Only
        // fixed authored text is retained; the Style and all actions are fresh.
        if(match.row().constantTarget()!=null&&current.runs.size()==1)
            return FormattedCharSequence.forward(match.row().constantTarget(),current.runs.getFirst().style());
        var source=match.row().source();int[] offsets=new int[source.size()+1];Style[] semanticStyles=new Style[source.size()];
        for(int i=0;i<source.size();i++){
            offsets[i+1]=offsets[i]+source.get(i).length();boolean copied=false,copyOnly=true;
            for(var part:match.row().target())if(part.source()==i){copied=true;copyOnly&=part.target().equals(source.get(i));}
            if(!copied||!copyOnly){if(!current.uniform(offsets[i],offsets[i+1]))return original;semanticStyles[i]=current.styleAt(offsets[i]);}
        }
        var out=new ArrayList<FormattedCharSequence>();
        for(var part:match.row().target())if(!part.target().isEmpty()){
            int i=part.source();
            if(part.target().equals(source.get(i)))current.appendOriginal(out,offsets[i],offsets[i+1]);
            else out.add(FormattedCharSequence.forward(part.target(),semanticStyles[i]));
        }
        return FormattedCharSequence.composite(out);
    }
    static Component render(Component original,List<String> rows,int index){
        var match=reviewed(rows,index);if(match==null||!literalOnly(original,0))return null;
        FormattedCharSequence sequence=sink->StringDecomposer.iterateFormatted(original,Style.EMPTY,sink);
        var next=render(sequence,match);
        if(next==sequence)return original;
        var out=Component.empty();
        StringBuilder run=new StringBuilder();Style[] style={null};
        Runnable flush=()->{if(!run.isEmpty()){out.append(Component.literal(run.toString()).setStyle(style[0]));run.setLength(0);}};
        next.accept((i,s,cp)->{if(!s.equals(style[0]))flush.run();style[0]=s;run.appendCodePoint(cp);return true;});flush.run();
        return out;
    }
    private static boolean literalOnly(Component source,int depth){
        if(depth>18||!(source.getContents() instanceof PlainTextContents))return false;
        for(var child:source.getSiblings())if(!literalOnly(child,depth+1))return false;
        return true;
    }
}
