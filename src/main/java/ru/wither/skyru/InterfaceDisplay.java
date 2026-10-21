package ru.wither.skyru;

import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/** Screenshot-confirmed collection and Garden display shapes, not arbitrary prose. */
final class InterfaceDisplay {
    private static final String NAME="([A-Za-z][A-Za-z '’-]{0,80})";
    private static final String ROMAN="([IVXLCDM]{1,12})";
    private static final Pattern VALID_ROMAN=Pattern.compile("(?=[IVXLCDM]+$)M{0,3}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3})");
    private static final String BESTIARY_MILESTONE="Bestiary Milestone";
    private static final Pattern VIEW=Pattern.compile("^View all your "+NAME+" Collection$");
    private static final Pattern PROGRESS=Pattern.compile("^Progress to "+NAME+" "+ROMAN+": ([0-9]{1,3}(?:\\.[0-9]{1,3})?)%$");
    private static final Pattern REWARDS=Pattern.compile("^"+NAME+" "+ROMAN+" Rewards:$");
    private static final Pattern RECIPE=Pattern.compile("^"+NAME+" Recipe$");
    private static final Pattern PEST=Pattern.compile("^([1-9][0-9]{0,2}) (Pest|Pests) Spawned in ([0-9]{1,3})!$");
    private static final Pattern RANK_VALUE=Pattern.compile("^(Max Guests|Cosmetic Skill Multiplier|Equipment Wardrobe Slots|Loadout Slots|Profile Slots|Monthly Gems): ([0-9]{1,9}(?:\\.[0-9]{1,3})?(?:%|x)?)$");
    private static final Pattern HUD_VALUE=Pattern.compile("^ {0,6}(Sawdust|Cleanup|Gems|North Stars|Coins/h|Farming XP/h|Farming Level|Yaw|Pitch): ([-+]?[0-9][0-9,./%+() x-]{0,48})$");
    private static final Pattern PLOT=Pattern.compile("^Plot( - [0-9]{1,3}(?: [^\\r\\n]{0,24})?)$");
    private static final Pattern PET=Pattern.compile("^Pet: ([A-Za-z][A-Za-z '-]{0,60})$");
    private static final Pattern PLOT_CROP=Pattern.compile("^Plot( - )(Wheat|Carrot|Potato|Nether Wart|Sugar Cane|Mushroom|Cocoa Beans|Cactus|Pumpkin|Melon|Greenhouse|Пшеница|Морковь|Картофель|Нарост Незера|Сахарный тростник|Гриб|Какао-бобы|Кактус|Тыква|Арбуз|Теплица)(?: ♟(?: x[1-9][0-9]{0,2})?)?$");
    private static final Pattern COLLECTED=Pattern.compile("^(Collected) ([0-9][0-9,.]{0,24}[kKmMbB]?)$");
    private static final Pattern SAWDUST_BONUS=Pattern.compile("^(Sawdust): ([0-9][0-9,.]{0,24}) (\\(\\+[0-9]{1,3}%\\))$");
    private static final String PRICE_NUMBER="([0-9][0-9,.]{0,24})";
    private static final Pattern BITS_RATIO=Pattern.compile("^(Bits Cost:) "+PRICE_NUMBER+" (Coins per: bit)$");
    private static final Pattern BITS_COST_ROW=Pattern.compile("^"+PRICE_NUMBER+" (Bits) (Coins per bit:) "+PRICE_NUMBER+"$");
    private static final Pattern AVG_PRICE=Pattern.compile("^(3 Day Avg\\. Price:) "+PRICE_NUMBER+" (Coins)$");
    private static final String MARKET_NUMBER="(?:[0-9]{1,9}|[0-9]{1,3}(?:,[0-9]{3}){1,3})(?:\\.[0-9]{1,6})?";
    private static final Pattern MARKET_VOLUME=Pattern.compile("^(Vol:) ("+MARKET_NUMBER+"[kKmMbB]?/"+MARKET_NUMBER+"[kKmMbB]?)$");
    private static final Pattern NPC_PRICE=Pattern.compile("^(NPC Sell Price:|Цена продажи NPC:?) ?("+MARKET_NUMBER+") (coins|монеты|монет)$");
    private static final Pattern SACK_CAPACITY=Pattern.compile("^(Capacity:|Вместимость:) (По )?("+MARKET_NUMBER+") (of each item|каждого предмета|ед\\. каждого предмета|of each gemstone type|of each rune tier|per trophy)$");
    private static final Pattern CROP_COUNTER=Pattern.compile("^(Stage:|Times Mutated:|Mutates Remaining:) ([0-9]{1,6}(?:/[1-9][0-9]{0,5})?)$");
    private static final Pattern CROP_TIME=Pattern.compile("^(Age:|Next Stage:|Decay attempt in:) ([0-9]{1,6}[dhms](?: [0-9]{1,6}[dhms]){0,3})$");
    private static final Pattern TIME_UNIT=Pattern.compile("[0-9]{1,6}([dhms])");
    private static final Pattern CROP_DATE=Pattern.compile("^(Decays at:) (?:Mon|Tue|Wed|Thu|Fri|Sat|Sun) [0-9]{2}\\.[0-9]{2}\\.[0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2}(?: GMT[+-][0-9:]{1,5})?$");
    private static final Pattern CHAPTER=Pattern.compile("^(Chapter |Unlock Chapter )(I|II|III|IV|V|VI|VII)( Completion Rewards)?$");
    private static final Pattern CROP_MILESTONE=Pattern.compile("^(Requires: )?(Crop Analyzer Milestone)(?: ([IVXLCDM]{1,12}))?$");
    static boolean cropMilestoneShape(String text){return CROP_MILESTONE.matcher(text).matches();}
    static boolean hudShape(String text){return hudValue(text)!=null||PLOT.matcher(text).matches()||PET.matcher(text).matches()||PLOT_CROP.matcher(text).matches()||COLLECTED.matcher(text).matches()||SAWDUST_BONUS.matcher(text).matches();}
    /** Necessary literal prefix only. The original full regex still validates every accepted value. */
    private static Matcher hudValue(String text){
        int at=0;while(at<6&&at<text.length()&&text.charAt(at)==' ')at++;
        if(at==text.length())return null;
        boolean prefix=switch(text.charAt(at)){
            case 'S'->text.startsWith("Sawdust: ",at);
            case 'C'->text.startsWith("Cleanup: ",at)||text.startsWith("Coins/h: ",at);
            case 'G'->text.startsWith("Gems: ",at);
            case 'N'->text.startsWith("North Stars: ",at);
            case 'F'->text.startsWith("Farming XP/h: ",at)||text.startsWith("Farming Level: ",at);
            case 'Y'->text.startsWith("Yaw: ",at);
            case 'P'->text.startsWith("Pitch: ",at);
            default->false;
        };
        if(!prefix)return null;
        var match=HUD_VALUE.matcher(text);return match.matches()?match:null;
    }
    static boolean roman(String text){return text.length()<=12&&VALID_ROMAN.matcher(text).matches();}
    private static boolean bestiaryMilestone(String text){return text.startsWith(BESTIARY_MILESTONE+" ")&&roman(text.substring(BESTIARY_MILESTONE.length()+1));}
    static List<String> keys(String text){
        // This shape is disjoint from earlier non-HUD forms. Sawdust bonus has the same keys.
        var hud=hudValue(text);if(hud!=null)return List.of(text,hud.group(1)+":");
        if((text.startsWith("Capacity: ")||text.startsWith("Вместимость: "))&&SACK_CAPACITY.matcher(text).matches())return List.of(text,"Capacity:", "of each item","of each gemstone type","of each rune tier","per trophy");
        if(text.startsWith("Vol: ")&&MARKET_VOLUME.matcher(text).matches())return List.of(text,"Vol:");
        if((text.startsWith("NPC Sell Price:")||text.startsWith("Цена продажи NPC"))&&NPC_PRICE.matcher(text).matches())return List.of(text,"NPC Sell Price","NPC Sell Price:");
        if(bestiaryMilestone(text))return List.of(text,BESTIARY_MILESTONE);
        var milestone=CROP_MILESTONE.matcher(text);if(milestone.matches())return List.of(text,"Requires:","Crop Analyzer Milestone","Crop Analyzer");
        var pet=PET.matcher(text);if(pet.matches()){var keys=new ArrayList<>(KnownPetNames.keys(pet.group(1)));keys.add(text);keys.add("Pet:");return List.copyOf(keys);}
        var crop=PLOT_CROP.matcher(text);if(crop.matches())return List.of(text,"Plot",crop.group(2));
        if(COLLECTED.matcher(text).matches())return List.of(text,"Collected");
        if(SAWDUST_BONUS.matcher(text).matches())return List.of(text,"Sawdust:");
        for(var pattern:List.of(CROP_COUNTER,CROP_TIME,CROP_DATE)){var m=pattern.matcher(text);if(m.matches())return List.of(text,m.group(1));}
        if(BITS_RATIO.matcher(text).matches())return List.of(text,"Bits Cost:","Coins per: bit");
        if(BITS_COST_ROW.matcher(text).matches())return List.of(text,"Bits","Coins per bit:");
        if(AVG_PRICE.matcher(text).matches())return List.of(text,"3 Day Avg. Price:","Coins");
        if(PLOT.matcher(text).matches())return List.of(text,"Plot");
        var rank=RANK_VALUE.matcher(text);if(rank.matches())return List.of(text,rank.group(1)+":");
        if(text.matches("[✦★✪] Access to Alpha Tests"))return List.of(text,"Access to Alpha Tests");
        for(var pattern:List.of(VIEW,PROGRESS,REWARDS,RECIPE)){var m=pattern.matcher(text);if(m.matches())return List.of(text,m.group(1));}
        return List.of(text);
    }
    static List<OfflineDictionary.TitleEdit> edits(String text,Function<String,String> lookup){
        var hud=hudValue(text);if(hud!=null){
            String key=hud.group(1)+":",label=lookup.apply(key);
            // The old Sawdust-bonus branch uses exactly the same label/range.
            return label!=null&&!label.equals(key)?List.of(new OfflineDictionary.TitleEdit(hud.start(1),hud.end(1)+1,label)):List.of();
        }
        if(text.startsWith("Capacity: ")||text.startsWith("Вместимость: ")){
            var capacity=SACK_CAPACITY.matcher(text);if(capacity.matches()){
                var result=new ArrayList<OfflineDictionary.TitleEdit>();result.add(new OfflineDictionary.TitleEdit(0,capacity.end(1),"Вместимость:"));
                if(capacity.group(2)!=null)result.add(new OfflineDictionary.TitleEdit(capacity.start(2),capacity.end(2),""));
                result.add(new OfflineDictionary.TitleEdit(capacity.start(4),capacity.end(4),switch(capacity.group(4)){
                    case "of each gemstone type"->"ед. каждого типа камней";case "of each rune tier"->"ед. каждого уровня рун";
                    case "per trophy"->"ед. каждого вида трофеев";default->"ед. каждого предмета";}));return List.copyOf(result);
            }
        }
        if(text.startsWith("Vol: ")){
            var volume=MARKET_VOLUME.matcher(text);if(volume.matches()){
                String label=lookup.apply("Vol:");if(label!=null&&!label.equals("Vol:"))return List.of(new OfflineDictionary.TitleEdit(0,volume.end(1),label));
            }
        }
        if(text.startsWith("NPC Sell Price:")||text.startsWith("Цена продажи NPC")){
            var price=NPC_PRICE.matcher(text);if(price.matches()){
                String label=lookup.apply("NPC Sell Price:");if(label!=null&&!label.equals("NPC Sell Price:"))return List.of(
                    new OfflineDictionary.TitleEdit(0,price.start(2),label+" "),new OfflineDictionary.TitleEdit(price.start(3),price.end(3),"монет"));
            }
        }
        if(bestiaryMilestone(text)){
            String label=lookup.apply(BESTIARY_MILESTONE);
            if(label==null||label.equals(BESTIARY_MILESTONE))return List.of();
            return List.of(new OfflineDictionary.TitleEdit(0,BESTIARY_MILESTONE.length(),label));
        }
        var milestone=CROP_MILESTONE.matcher(text);if(milestone.matches()){
            if(milestone.group(3)!=null&&!roman(milestone.group(3)))return List.of();
            String name=lookup.apply("Crop Analyzer Milestone");if(name==null||name.equals("Crop Analyzer Milestone"))return List.of();
            var result=new ArrayList<OfflineDictionary.TitleEdit>();
            if(milestone.group(1)!=null){String label=lookup.apply("Requires:");if(label==null||label.equals("Requires:"))return List.of();result.add(new OfflineDictionary.TitleEdit(0,milestone.end(1),label+" "));}
            result.add(new OfflineDictionary.TitleEdit(milestone.start(2),milestone.end(2),name));return List.copyOf(result);
        }
        var pet=PET.matcher(text);if(pet.matches()){
            String name=KnownPetNames.translation(pet.group(1),lookup),label=lookup.apply("Pet:");
            if(name==null||label==null||label.equals("Pet:"))return List.of();
            return List.of(new OfflineDictionary.TitleEdit(0,4,label),new OfflineDictionary.TitleEdit(pet.start(1),pet.end(1),name));
        }
        var plotCrop=PLOT_CROP.matcher(text);if(plotCrop.matches()){
            String name=lookup.apply(plotCrop.group(2)),label=lookup.apply("Plot");
            if(label==null||label.equals("Plot"))return List.of();
            // A preceding renderer may already have translated just the crop span.
            if(plotCrop.group(2).matches("[А-Яа-яЁё -]+"))return List.of(new OfflineDictionary.TitleEdit(0,4,label));
            if(name==null||name.equals(plotCrop.group(2)))return List.of();
            return List.of(new OfflineDictionary.TitleEdit(0,4,label),new OfflineDictionary.TitleEdit(plotCrop.start(2),plotCrop.end(2),name));
        }
        var collected=COLLECTED.matcher(text);if(collected.matches())return List.of(new OfflineDictionary.TitleEdit(0,collected.end(1),"Собрано"));
        var bonus=SAWDUST_BONUS.matcher(text);if(bonus.matches()){
            String label=lookup.apply("Sawdust:");if(label==null||label.equals("Sawdust:"))return List.of();
            return List.of(new OfflineDictionary.TitleEdit(0,bonus.end(1)+1,label));
        }
        var chapter=CHAPTER.matcher(text);
        if(chapter.matches()){
            boolean unlock=chapter.group(1).startsWith("Unlock");
            if(unlock&&(chapter.group(3)!=null||chapter.group(2).equals("I")))return List.of();
            String prefix=unlock?"Открыть главу ":chapter.group(3)==null?"Глава ":"Награды за завершение главы ";
            var result=new ArrayList<OfflineDictionary.TitleEdit>();
            result.add(new OfflineDictionary.TitleEdit(0,chapter.start(2),prefix));
            if(chapter.group(3)!=null)result.add(new OfflineDictionary.TitleEdit(chapter.end(2),text.length(),""));
            return List.copyOf(result); // Retain the actual chapter tier with its original colour/actions.
        }
        var counter=CROP_COUNTER.matcher(text);
        var timer=CROP_TIME.matcher(text);
        var date=CROP_DATE.matcher(text);
        Matcher crop=counter.matches()?counter:timer.matches()?timer:date.matches()?date:null;
        if(crop!=null){
            if(crop==counter){
                boolean stage=crop.group(1).equals("Stage:");String value=crop.group(2);
                if(stage!=value.contains("/"))return List.of();
                if(stage){String[] values=value.split("/");if(Integer.parseInt(values[0])>Integer.parseInt(values[1]))return List.of();}
            }
            String label=lookup.apply(crop.group(1));
            if(label==null||label.equals(crop.group(1)))return List.of();
            var result=new ArrayList<OfflineDictionary.TitleEdit>();result.add(new OfflineDictionary.TitleEdit(0,crop.end(1),label));
            if(crop==timer){
                var units=TIME_UNIT.matcher(text);int order=-1;
                while(units.find()){
                    int next="dhms".indexOf(units.group(1));if(next<=order)return List.of();order=next;
                    String ru=switch(units.group(1)){case "d"->" дн";case "h"->" ч";case "m"->" мин";default->" с";};
                    result.add(new OfflineDictionary.TitleEdit(units.start(1),units.end(1),ru));
                }
            }
            return List.copyOf(result); // Original counters/calendar timestamps remain verbatim.
        }
        var ratio=BITS_RATIO.matcher(text);if(ratio.matches())return List.of(
            new OfflineDictionary.TitleEdit(ratio.start(1),ratio.end(1),"Монет за бит:"),
            new OfflineDictionary.TitleEdit(ratio.end(2),text.length(),""));
        var cost=BITS_COST_ROW.matcher(text);if(cost.matches())return List.of(
            new OfflineDictionary.TitleEdit(cost.start(2),cost.end(2),"битов"),
            new OfflineDictionary.TitleEdit(cost.start(3),cost.end(3),"Монет за бит:"));
        var average=AVG_PRICE.matcher(text);if(average.matches())return List.of(
            new OfflineDictionary.TitleEdit(average.start(1),average.end(1),"Средняя цена за 3 дня:"),
            new OfflineDictionary.TitleEdit(average.start(3),average.end(3),"монет"));
        if(PLOT.matcher(text).matches()){
            String label=lookup.apply("Plot");if(label!=null&&!label.equals("Plot"))return List.of(new OfflineDictionary.TitleEdit(0,4,label));
        }
        var rank=RANK_VALUE.matcher(text);if(rank.matches()){
            String label=lookup.apply(rank.group(1)+":");
            if(label!=null&&!label.equals(rank.group(1)+":"))return List.of(new OfflineDictionary.TitleEdit(0,rank.end(1)+1,label));
        }
        if(text.matches("[✦★✪] Access to Alpha Tests")){
            String label=lookup.apply("Access to Alpha Tests");
            if(label!=null&&!label.equals("Access to Alpha Tests"))return List.of(new OfflineDictionary.TitleEdit(2,text.length(),label));
        }
        Matcher m=PEST.matcher(text);if(m.matches()){
            int count=Integer.parseInt(m.group(1));if((count==1)!=m.group(2).equals("Pest"))return List.of();
            int last=count%100;boolean single=last%10==1&&last!=11;
            String noun=last>=11&&last<=14?"вредителей":last%10==1?"вредитель":last%10>=2&&last%10<=4?"вредителя":"вредителей";
            return List.of(new OfflineDictionary.TitleEdit(m.start(2),m.end(2),noun),
                new OfflineDictionary.TitleEdit(m.end(2),m.start(3),single?" появился на участке № ":" появились на участке № "));
        }
        for(var pattern:List.of(VIEW,PROGRESS,REWARDS,RECIPE)){
            m=pattern.matcher(text);if(!m.matches())continue;
            String name=lookup.apply(m.group(1));if(name==null||name.equals(m.group(1)))return List.of();
            var e=new ArrayList<OfflineDictionary.TitleEdit>();
            if(pattern==VIEW){e.add(new OfflineDictionary.TitleEdit(0,m.start(1),"Прогресс коллекции «"));e.add(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),name));e.add(new OfflineDictionary.TitleEdit(m.end(1),text.length(),"»"));}
            else if(pattern==PROGRESS){if(!roman(m.group(2))||Double.parseDouble(m.group(3))>100)return List.of();e.add(new OfflineDictionary.TitleEdit(0,m.start(1),"Прогресс коллекции «"));e.add(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),name));e.add(new OfflineDictionary.TitleEdit(m.end(1),m.start(2),"» до "));}
            else if(pattern==REWARDS){if(!roman(m.group(2)))return List.of();e.add(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),name));e.add(new OfflineDictionary.TitleEdit(m.end(2),text.length()," — награды:"));}
            else {e.add(new OfflineDictionary.TitleEdit(m.start(1),m.end(1),name));e.add(new OfflineDictionary.TitleEdit(m.end(1),text.length()," — рецепт"));}
            return List.copyOf(e);
        }return List.of();
    }
    static String translate(String text,Function<String,String> lookup){
        return applyEdits(text,edits(text,lookup));
    }
    /** Join an already validated local plan; no duplicate pattern/lookup pass. */
    static String applyEdits(String text,List<OfflineDictionary.TitleEdit> edits){
        if(edits.isEmpty())return null;var b=new StringBuilder();int at=0;
        for(var e:edits){b.append(text,at,e.start()).append(e.replacement());at=e.end();}
        return b.append(text,at,text.length()).toString();
    }
}
