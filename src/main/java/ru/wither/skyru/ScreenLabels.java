package ru.wither.skyru;

import java.util.*;

/** Closed visible labels. Never writes chat, commands, names or GUI input. */
final class ScreenLabels {
    private record Label(List<String> source,List<String> target,String sourceText,String targetText,List<String> keys) {}
    private static Label label(String... pairs){
        var a=new ArrayList<String>();var b=new ArrayList<String>();
        for(int i=0;i<pairs.length;i+=2){a.add(pairs[i]);b.add(pairs[i+1]);}
        String source=String.join("",a),target=String.join("",b);var keys=new ArrayList<>(a);keys.add(source);
        return new Label(List.copyOf(a),List.copyOf(b),source,target,List.copyOf(keys));
    }
    private static final List<Label> LABELS=List.of(
        label("To Sack of Sacks","К сумке мешков"),label("Get From Sacks","Взять из мешков"),
        label("Enchanted Agronomy Sack","Мешок для зачарованного урожая"),label("Agronomy Sacks","Мешки для урожая"),
        label("Small Sack of Sacks","Малая сумка мешков"),label("Sack of Sacks ","Сумка мешков ","III","III"),
        label("Slots","Ячейки"),label("A sack which contains other sacks.","Сумка для хранения других мешков."),
        label("Sackception!","Мешки внутри мешка!"),label("Also accessible via ","Также доступна через ","/sacks","/sacks"),
        label("Unlock additional slots in your Sack of","Открывает дополнительные ячейки в сумке"),
        label("Obtain sack capacity upgrades as well","Улучшения вместимости мешков и"),
        label("as exclusive bits shop sacks.","особые мешки из магазина битов."),
        label("From Pocket Sack-in-a-Sack","От карманного мешка для мешков"),
        label("for sack total (","за всё содержимое мешка ("),label("Empty sack!","Мешок пуст!"),
        label("Stored:","В запасе:"),label("Total items:","Всего предметов:"),
        label("Click to pickup!","Нажмите, чтобы забрать!"),label("Click to put items in!","Нажмите, чтобы положить предметы!"),
        label("Enchanted Book","Зачарованная книга"),label("Combinable in Anvil","Можно объединить на наковальне"),
        label("Sources:","Источники:"),label("Source:","Источник:"),label("Applied To:","Применяется к:"),
        label("Requirements:","Требования:"),label("Conflicts:","Несовместимо с:"),
        label("Diamond Collection","Коллекция: Алмаз"),
        label("Gunpowder Collection","Коллекция: Порох"),label("Pufferfish Collection","Коллекция: Иглобрюх"),
        label("Experiments","Эксперименты"),
        label("Main Experiment","Основной эксперимент"),label("Unknown experiment!","Неизвестный эксперимент!"),
        label("Renew Experiments","Добавить попытку экспериментов"),label("Experiment over","Эксперимент завершён"),
        label("Experiment Over","Эксперимент завершён"),label("Add-on locked!","Дополнительная игра недоступна!"),
        label("Clicks:","Ходы:"),label("Experimental XP:","Опыт экспериментов:"),
        label("Stored Experimental XP:","Накопленный опыт экспериментов:"),
        label("Experimentation Table RNG","Случайная добыча стола экспериментов"),
        label("Experimentation Table RNG Meter","Счётчик случайной добычи стола экспериментов"),
        label("To Experimentation Table","К столу экспериментов"),label("Required Enchanting:","Требуется уровень зачарования:"),
        label("Grid Size:","Размер поля:"),label("Possible pairs:","Возможные пары:"),label("Starting cost:","Стоимость запуска:"),
        label("Enchanting level too low!","Недостаточный уровень зачарования!"),
        label("Enchanting level!","уровень зачарования!"),
        label("Unlock this minigame with a higher","Для этой мини-игры нужен более высокий"),
        label("Rare chance to find ","Малая вероятность найти книги ","VII","VII"," books!","!"),
        label("Enchant items to earn ","Зачаровывайте предметы, чтобы получать ","Enchanting EXP","опыт зачарования","!","!"),
        label("Click any button!","Нажмите на любую ячейку!"),label("Click a second button!","Нажмите на вторую ячейку!"),
        label("Next button is instantly rewarded!","Следующая ячейка сразу даёт награду!"),
        label("Superpairs Powerup","Усиление Суперпар"),label("Remember the pattern!","Запомните последовательность!"),
        label("Right-click to consume!","Нажмите ПКМ, чтобы использовать!"),label("Consumable","Расходуемый предмет"),
        label("Enchanting Exp","Опыт зачарования"),
        label("Experiments","Эксперименты",", ",", ","Bazaar","Базар"),
        label("XX ","XX ","ULTRA-RARE BOOK!","ОЧЕНЬ РЕДКАЯ КНИГА!"," XX"," XX"),
        label("Your Bags","Ваши сумки"),label("Accessory Bag","Сумка аксессуаров"),label("Bazaar History","История Базара"),
        label("Bazaar Settings","Настройки Базара"),label("Bits","Биты"),label("Cancel","Отмена"),
        label("Click to disable!","Нажмите, чтобы выключить!"),label("Click to enable!","Нажмите, чтобы включить!"),
        label("Click to view!","Нажмите, чтобы посмотреть!"),label("Close","Закрыть"),label("Combat","Бой"),
        label("Confirm","Подтвердить"),label("Confirmations: Enabled!","Подтверждения: включены!"),
        label("Confirmations: OFF","Подтверждения: выключены"),label("Currently viewing!","Вы просматриваете этот раздел!"),
        label("Farming","Фермерство"),label("Fishing Bag","Сумка для рыбалки"),label("Go Back","Назад"),
        label("Information","Информация"),label("Insert inventory","Поместить содержимое инвентаря"),
        label("Manage Orders","Управление заказами"),label("Mining","Горное дело"),label("No Product Found","Товар не найден"),
        label("Oddities","Диковинки"),label("Pickup All","Забрать всё"),label("Potion Bag","Сумка зелий"),
        label("Previous Page","Предыдущая страница"),label("Quiver","Колчан"),label("Re-Center","Вернуться в центр"),
        label("Sack of Sacks","Сумка мешков"),label("Scroll Down","Прокрутить вниз"),label("Scroll Left","Прокрутить влево"),
        label("Scroll Right","Прокрутить вправо"),label("Scroll Up","Прокрутить вверх"),label("Search","Поиск"),
        label("Sell Inventory Now","Продать содержимое инвентаря"),label("Sell Item","Продать предмет"),
        label("Sell Sacks Now","Продать содержимое мешков"),label("Settings","Настройки"),label("SkyBlock Gems","Самоцветы SkyBlock"),
        label("Time Pocket","Карман времени"),label("To Bazaar","На Базар"),label("WARNING","ВНИМАНИЕ"),
        label("Woods & Fishes","Древесина и рыба"),
        label("Sack Size ","Вместимость мешков ","I","I"),
        label("Right-click to send to Hunting Box!","Нажмите ПКМ, чтобы отправить в охотничий ящик!"),
        label("Shift Right-click to move all!","Shift + ПКМ, чтобы переместить всё!"),
        label("EPIC ","EPIC ","FOREST SHARD","ЛЕСНОЙ ОСКОЛОК"," (ID E16)"," (ID E16)"),
        label("Multiple sacks sum their capacity!","Вместимость нескольких мешков складывается!"),
        label("Sacks sum their capacity.","Вместимость мешков складывается."),
        label("Вместимость нескольких мешков складывается!","Вместимость нескольких мешков складывается!"),
        label("Right Click to open sack!","Нажмите ПКМ, чтобы открыть мешок!"),
        label("Right Click to open!","Нажмите ПКМ, чтобы открыть!"),
        label("COMMON ","COMMON ","SACK","МЕШОК"),label("UNCOMMON ","UNCOMMON ","SACK","МЕШОК"),
        label("RARE ","RARE ","SACK","МЕШОК"),label("EPIC ","EPIC ","SACK","МЕШОК"),label("LEGENDARY ","LEGENDARY ","SACK","МЕШОК"),
        label("* ","* ","Soulbound","Привязан к профилю"," *"," *"),
        label("* ","* ","Co-op Soulbound","Привязан к кооперативу"," *"," *"),
        label("❣","❣"," "," ","Requires ","Требуется звание «","Novice Frog Collector","Начинающий собиратель лягушек",".","»."),
        label("❣","❣"," "," ","Requires ","Требуется звание «","Adept Frog Collector","Опытный собиратель лягушек",".","»."),
        label("Jerry","Джерри"," The Assistant"," — помощник"),
        label("Sam","Сэм"," the Garden Assistant"," — помощница в Саду"),
        label("Visits & Guesting","Посещения и гости"),
        label("Warp to ","Переместиться в ","Garden","Сад"),
        label("Add-on Experiment","Дополнительный эксперимент"),
        label("Click to ","Нажмите, чтобы ","learn","узнать больше","!","!"),
        label("Click to ","Нажмите, чтобы ","warp","переместиться","!","!"),
        label("☠ ","☠ ","Revenant Horror","Возрождённый ужас"),
        label("☠ ","☠ ","Tarantula Broodfather","Прародитель тарантулов"),
        label("☠ ","☠ ","Sven Packmaster","Свен — вожак стаи"),
        label("☠ ","☠ ","Voidgloom Seraph","Серафим мрачной пустоты"),
        label("☠ ","☠ ","Inferno Demonlord","Повелитель демонов Инферно"),
        label("☠ ","☠ ","Riftstalker Bloodfiend","Кровавый демон Разлома"),
        label("Talk to the ","Поговорите со ","Security Clerk","служащим безопасности"),
        label("Talk to ","Поговорите с ","Elizabeth","Элизабет"),
        label("Take a seat in the ","Займите место в ","Briefing Room","комнате инструктажа"),
        label("Take a seat in the ","Займите место в ","Briefin","комнате инструктажа"),
        label("Chat Prefix: ","Префикс в чате: ","[MVP++]","[MVP++]"),
        label("Seasonal Bundle: ","Сезонный набор: ","Full","Полный"),
        label("Paste the ","Разместите ","Greenhouse","теплицу"," on a plot at the "," на участке через ","Desk","стол"),
        label("Paste the ","Разместите ","Greenhouse","теплицу"," plot"," на участке"),
        label("Talk to ","Поговорите с ","Lone Adventurer","Одиноким искателем приключений"),
        label("Talk to ","Поговорите с ","Susan","Сьюзан"),
        label("Talk to ","Поговорите с ","Frosty","Фрости"),
        label("Wait for ","Дождитесь следующего визита ","Beth","Beth"," to revisit your "," в ваш ","Garden","Сад"," again",""),
        label("Wait for ","Дождитесь следующего визита ","Beth","Beth"," to revisit your "," в ваш ","Garden","Сад"),
        label("Wait for ","Дождитесь визита ","Beth","Beth"," to revisit",""),
        label("Jacob's Contest","Состязание Jacob"),
        label("Updating ranking...","Обновление места…"),
        label("Mining","Горнодобывающая"," "," ","Fiesta","фиеста"),
        label("- ","- ","Mining","Горнодобывающая"," "," ","Fiesta","фиеста"),
        label("- ","- ","Benediction","Благословение"),
        label("Normal","Обычный режим"),
        label("Normal.","Обычный режим."),
        label("Gems:","Самоцветы:"),
        label("Marauder","Мародёр"),label("Benediction","Благословение"),
        label("Ability: ","Способность: ","True Dwarth","Истинный дворф","  "," — ","RIGHT CLICK","ПКМ"),
        label("Ability: ","Способность: ","Throwing Axe","Метательный топор","  "," — ","RIGHT CLICK","ПКМ"),
        label("Ability: ","Способность: ","True Dwarth","Истинный дворф"),
        label("Ability: ","Способность: ","Park Enjoyer","Любитель Парка"),
        label("Ability: ","Способность: ","Park Enthusiast","Знаток Парка")
    );
    private static final Map<String,Label> BY_SOURCE=LABELS.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Label::sourceText,l->l));
    private static final Set<String> TARGETS=LABELS.stream().map(Label::targetText).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static String objectivePrefix(String text){
        for(String prefix:List.of("Objective: ","Цель: ")){
            if(!text.startsWith(prefix))continue;
            String body=text.substring(prefix.length());
            if(body.startsWith("Wait for Beth to revisit")||body.startsWith("Take a seat in the ")
                ||body.equals("Talk to the Security Clerk")||body.equals("Talk to Elizabeth"))return prefix;
        }
        return null;
    }
    static String wrappedTranslation(String text){
        String prefix=objectivePrefix(text);if(prefix==null)return null;
        String body=text.substring(prefix.length());
        var label=BY_SOURCE.get(body);return label==null?null:"Цель: "+label.targetText();
    }
    static boolean finalText(String text){
        if(text.startsWith("Цель: "))text=text.substring(6);
        return TARGETS.contains(text);
    }
    static List<String> keys(String text){
        String prefix=objectivePrefix(text);
        if(prefix!=null){var result=new ArrayList<>(keys(text.substring(prefix.length())));result.add(text);result.add("Objective");return List.copyOf(result);}
        var label=BY_SOURCE.get(text);return label==null?List.of(text):label.keys();
    }
    static List<OfflineDictionary.TitleEdit> edits(String text,String target){
        String prefix=objectivePrefix(text);
        if(prefix!=null&&Objects.equals(wrappedTranslation(text),target)){
            var result=new ArrayList<OfflineDictionary.TitleEdit>();
            if(!prefix.equals("Цель: "))result.add(new OfflineDictionary.TitleEdit(0,prefix.length(),"Цель: "));
            for(var edit:edits(text.substring(prefix.length()),target.substring(6)))
                result.add(new OfflineDictionary.TitleEdit(prefix.length()+edit.start(),prefix.length()+edit.end(),edit.replacement()));
            return List.copyOf(result);
        }
        var label=BY_SOURCE.get(text);if(label!=null&&label.targetText().equals(target)){
            var edits=new ArrayList<OfflineDictionary.TitleEdit>();int at=0;
            for(int i=0;i<label.source().size();i++){
                int end=at+label.source().get(i).length();
                if(!label.source().get(i).equals(label.target().get(i)))edits.add(new OfflineDictionary.TitleEdit(at,end,label.target().get(i)));
                at=end;
            }return List.copyOf(edits);
        }return List.of();
    }
}
