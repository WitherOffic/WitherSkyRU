import java.nio.file.*;
import java.util.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.multiplayer.chat.*;

/** Deterministic correctness/limits/context lifetime; timing uses a separate benchmark. */
public class TestPerformanceCaches {
    private static int checks;
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    private static void flag(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static Object edits(OfflineDictionary db,String text)throws Exception{var m=OfflineDictionary.class.getDeclaredMethod("displayEdits",String.class);m.setAccessible(true);return m.invoke(db,text);}
    private static List<Style> styles(FormattedCharSequence seq){var list=new ArrayList<Style>();seq.accept((i,s,p)->{for(int n=0;n<Character.charCount(p);n++)list.add(s);return true;});return list;}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);var db=new OfflineDictionary(root);db.setRecordUnknown(false);flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(String source:List.of("Normal","Кошелёк: 165,813","FuturePerfProbeZX_49123","Sawdust: 5,011 (+2,283)","Strong Aspect of the Dragons")){
            String plain=db.translate(source);Object first=edits(db,source);
            for(int i=0;i<100;i++){check(edits(db,source)==first,"stable positive/negative plan cache");check(db.translateForSearch(source).equals(plain),"quiet result unchanged");}
            check(((Map<?,?>)field(db,"displayCache")).containsKey(source),"negative plans cached");
        }
        for(int i=0;i<50;i++){
            var label=Style.EMPTY.withColor(0x100000+i).withInsertion("label "+i);
            var number=Style.EMPTY.withColor(0x200000+i).withClickEvent(new ClickEvent.RunCommand("/original "+i));
            String value=i%2==0?"75":"76";var source=Component.empty().append(Component.literal("Gems: ").setStyle(label)).append(Component.literal(value).setStyle(number));
            var translated=SkyRuClient.component(source);String target="Самоцветы: "+value;check(translated.getString().equals(target),"current number");
            var list=styles(translated.getVisualOrderText());int at=target.indexOf(value);
            for(int n=0;n<at;n++)check(list.get(n).equals(label),"current label style");for(int n=at;n<target.length();n++)check(list.get(n).equals(number),"current numeric style/action");
            check(source.getString().equals("Gems: "+value),"source unchanged");
            for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var message=new GuiMessage(0,source,null,channel,null);check(SkyRuClient.npcChat(message)==message,"whole ordinary chat protected");}
        }
        var mutable=Component.literal("Normal");check(SkyRuClient.component(mutable).getString().equals("Обычный режим"),"initial mutable input");mutable.append(" future");check(!SkyRuClient.component(mutable).getString().equals("Обычный режим"),"no stale mutable-identity cache");
        var custom=root.resolve("custom");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Normal\tМой режим\nGems:\tМои камни:\n");
        var own=new OfflineDictionary(custom);own.setRecordUnknown(false);flag("dictionary",own);check(SkyRuClient.component(Component.literal("Normal")).getString().equals("Мой режим"),"new dictionary, separate cache");
        check(SkyRuClient.component(Component.literal("Gems: 75")).getString().equals("Мои камни: 75"),"label override");flag("dictionary",db);
        flag("enabled",false);check(SkyRuClient.component(Component.literal("Normal")).getString().equals("Normal"),"toggle not cached");flag("enabled",true);
        var observed=new OfflineDictionary(root.resolve("observed"));String unknown="UnseenPerformanceProbeABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for(int i=0;i<5;i++)observed.translateForSearch(unknown);check(((Set<?>)field(observed,"misses")).isEmpty(),"quiet lookup never records");
        observed.translate(unknown);check(((Set<?>)field(observed,"misses")).contains(unknown),"later normal lookup records quiet-cached miss");
        String russian="Кошелёк: 234,567 (+12)";String quiet=observed.translateForSearch(russian);check(observed.translate(russian).equals(quiet),"quiet Cyrillic result safely promoted");
        check(((Map<?,?>)field(observed,"cache")).get(russian).equals(quiet),"normal cache populated");
        check(!((Set<?>)field(observed,"misses")).contains(russian),"Cyrillic original logging policy unchanged");
        var fold=OfflineDictionary.class.getDeclaredMethod("fold",String.class);fold.setAccessible(true);
        var initial=OfflineDictionary.class.getDeclaredMethod("mandatoryInitial",java.util.regex.Pattern.class);initial.setAccessible(true);
        for(String expression:List.of("^A?B$","^A*B$","^A{0,2}B$","^A$|^B$","^A\\Q(\\E|B$","^(A|B)$"))check((Integer)initial.invoke(null,java.util.regex.Pattern.compile(expression))==0,"uncertain roots use complete fallback");
        check((Integer)initial.invoke(null,java.util.regex.Pattern.compile("^Arrow ([0-9]+)$"))=='a',"mandatory anchored literal");
        var candidates=OfflineDictionary.class.getDeclaredMethod("candidateRules",String.class);candidates.setAccessible(true);
        for(String source:List.of("Saves arrows 3% of the time","Кошелёк: 100","123 xp","a","A","Known item","ſomething","İtem","ıtem","^prefix","","Grants +25 Mining Speed","Wait for Beth to revisit")){
            var selected=(List<?>)candidates.invoke(db,source);
            for(Object rule:(List<?>)field(db,"rules")){var pattern=rule.getClass().getDeclaredMethod("pattern");pattern.setAccessible(true);var p=(java.util.regex.Pattern)pattern.invoke(rule);
                if(p.matcher(source).matches())check(selected.contains(rule),"no excluded matching pattern, including Unicode: "+p);
            }
        }
        for(String key:List.of("  Diamond  Pickaxe  ","Diamond\tPickaxe","Diamond\nPickaxe","Diamond\u000bPickaxe","Diamond\u00a0Pickaxe"))check(fold.invoke(null,key).equals(key.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT)),"original folding semantics");
        for(String code:List.of("§a","§L","§r","§x","")){String text=code+"SKYBLOCK";check(SkyBlockBrand.header(text)==text.replaceAll("(?i)§[0-9a-fk-or]","").strip().equalsIgnoreCase("SKYBLOCK"),"original formatting semantics");}
        var frames=List.of("at.hannibal2.skyhanni.Widget#render","me.owdding.customscoreboard.core.Renderer#onRender","me.jfenn.scoreboardoverhaul.Renderer#draw",
            "de.hysky.skyblocker.skyblock.tabhud.Panel#draw","net.minecraft.client.gui.Gui#displayScoreboardSidebar","net.minecraft.client.gui.Gui#extract",
            "net.minecraft.client.gui.components.ChatComponent#extractRenderState","net.minecraft.client.gui.components.EditBox#render",
            "net.minecraft.client.gui.components.EditBoxExtra#render","net.minecraft.client.gui.components.MultiLineEditBox#render",
            "net.minecraft.client.gui.components.PlayerTabOverlay#getNameForDisplay","net.minecraft.client.gui.components.PlayerTabOverlay#extract",
            "net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen#extractRenderState","net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen#render",
            "net.minecraft.client.gui.screens.ChatScreen#extractRenderState","net.minecraft.client.gui.screens.inventory.InventoryScreen#render",
            "net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip#extractText","foreign.chat.Widget#render","foreign.ToolTip#draw","foreign.Widget#renderTooltip","foreign.Widget#extract");
        for(String first:frames)for(String second:frames){var list=List.of(first,second);var context=RenderContext.classify(list);
            check(context.protectedText()==UserInputText.protectedDisplayFrames(list),"user context equivalence "+list);
            check(context.hud()==HudTextFit.eligibleFrames(list),"HUD context equivalence "+list);
            check(context.tooltip()==TooltipTextFit.eligibleFrames(list),"tooltip context equivalence "+list);
        }
        var current=RenderContext.class.getDeclaredField("CURRENT");current.setAccessible(true);var local=(ThreadLocal<?>)current.get(null);check(local.get()==null,"no retained frame context");
        try(var outer=RenderContext.open()){
            Object first=RenderContext.current();check(RenderContext.current()==first,"one snapshot per draw");
            try(var nested=RenderContext.open()){check(RenderContext.current()!=first,"nested draw inspects new callers");}
            check(RenderContext.current()==first,"nested restoration");
            try{try(var nested=RenderContext.open()){throw new IllegalStateException("synthetic");}}catch(IllegalStateException expected){}
            check(RenderContext.current()==first,"exception restoration");
        }
        check(local.get()==null,"scope cleared");
        for(int i=0;i<50;i++){
            var style=Style.EMPTY.withColor(0x300000+i).withInsertion("fresh "+i);
            int[] visits={0};String original="Gems: "+i;
            FormattedCharSequence counted=sink->{visits[0]++;return FormattedCharSequence.forward(original,style).accept(sink);};
            var converted=SkyRuClient.sequence(counted);
            check(visits[0]==1,"current visible text and Style runs collected in one traversal");
            var output=new StringBuilder();converted.accept((n,s,p)->{check(s.equals(style),"direct glyph visitor keeps fresh Style");output.appendCodePoint(p);return true;});
            check(output.toString().equals("Самоцветы: "+i),"direct glyph output");
        }
        var first=Component.literal("Пример ".repeat(70));var unchanged=Component.literal(" без изменений ");
        var tree=Component.empty().append(first).append(Component.literal("Normal")).append(unchanged).append(Component.literal("Gems:"));
        var rebuilt=SkyRuClient.component(tree);
        check(rebuilt.getString().equals(first.getString()+"Обычный режим"+unchanged.getString()+"Самоцветы:"),"lazy sibling list retains prior/later unchanged children");
        check(rebuilt.getSiblings().get(0)==first&&rebuilt.getSiblings().get(2)==unchanged,"unchanged sibling identity retained");
        check(tree.getString().contains("Normal"),"input tree unmodified");
        check(SkyRuClient.textOutsideHud("Normal").equals("Обычный режим"),"scoped string hook");
        check(SkyRuClient.componentOutsideHud(Component.literal("Normal")).getString().equals("Обычный режим"),"scoped component hook");
        var output=new StringBuilder();SkyRuClient.sequenceOutsideHud(FormattedCharSequence.forward("Normal",Style.EMPTY)).accept((n,s,p)->{output.appendCodePoint(p);return true;});
        check(output.toString().equals("Обычный режим"),"scoped sequence hook");
        check(local.get()==null,"all GUI hook scopes cleared");
        var tooltipRows=List.of(Component.literal("Gems: 99"),Component.literal("Пример"),Component.literal("Normal"));
        var componentRows=TooltipTranslation.components(new ArrayList<Component>(tooltipRows),false);
        check(componentRows.get(0).getString().equals("Самоцветы: 99")&&componentRows.get(2).getString().equals("Обычный режим"),"whole-tooltip component scope result");
        check(componentRows.get(1)==tooltipRows.get(1),"unchanged tooltip row identity");
        var sequenceRows=TooltipTranslation.styled(tooltipRows.stream().map(Component::getVisualOrderText).toList(),false);
        output.setLength(0);sequenceRows.get(0).accept((n,s,p)->{output.appendCodePoint(p);return true;});
        check(output.toString().equals("Самоцветы: 99"),"whole-tooltip sequence scope result");
        check(local.get()==null,"whole-tooltip contexts cleared");
        flag("enabled",false);var original=Component.literal("Normal");
        check(SkyRuClient.componentOutsideHud(original)==original&&SkyRuClient.textOutsideHud("Normal").equals("Normal"),"inactive GUI hook no work");flag("enabled",true);
        var boundaries=new ArrayList<String>(Collections.nCopies(65,"foreign.Widget#extract"));
        boundaries.set(48,"at.hannibal2.skyhanni.Widget#render");boundaries.set(63,"net.minecraft.client.gui.components.EditBox#render");
        check(!RenderContext.classify(boundaries).hud()&&RenderContext.classify(boundaries).protectedText(),"panel48/protection64 limits retained");
        boundaries.set(63,"foreign.Widget#extract");boundaries.set(64,"net.minecraft.client.gui.components.EditBox#render");
        check(!RenderContext.classify(boundaries).protectedText(),"no protection beyond64 caller frames");
        for(int type=0;type<300;type++)for(int method=0;method<40;method++)RenderContext.classify(List.of("foreign.Class"+type+"#method"+method));
        var metadataCache=fieldStatic(RenderContext.class,"FRAME_FLAGS");
        var classification=(Map<?,?>)(metadataCache instanceof ThreadLocal<?> threadLocal?threadLocal.get():metadataCache);
        check(classification.size()<=256,"bounded immutable class classification");
        for(Object methods:classification.values())check(((Map<?,?>)methods).size()<=32,"bounded immutable method classification");
        var lore=Class.forName("ru.wither.skyru.ReviewedLoreStyles");
        for(Object group:(List<?>)fieldStatic(lore,"GROUPS"))for(Object row:(List<?>)group){
            var rowType=row.getClass();var textMethod=rowType.getDeclaredMethod("text");textMethod.setAccessible(true);
            String authored=(String)textMethod.invoke(row);check(textMethod.invoke(row)==authored,"reviewed row text prepared once");
            Object pattern=field(row,"numeric");
            if(pattern!=null){
                var bind=rowType.getDeclaredMethod("bind",String.class);bind.setAccessible(true);
                String firstValue=authored.replace("{0}","37.5").replace("{1}","2");
                Object firstBound=bind.invoke(row,firstValue);check(firstBound!=null&&textMethod.invoke(firstBound).equals(firstValue),"precompiled numeric row binds current values");
                String nextValue=authored.replace("{0}","38.5").replace("{1}","3");
                Object nextBound=bind.invoke(row,nextValue);check(nextBound!=null&&textMethod.invoke(nextBound).equals(nextValue),"later numeric values not cached stale");
                check(field(row,"numeric")==pattern&&bind.invoke(row,"Unrelated future row 12345")==null,"numeric Pattern reused; unrelated rows excluded");
            }
        }
        var prepare=PlayerNames.class.getDeclaredMethod("prepare",Set.class);prepare.setAccessible(true);
        var names=new HashSet<String>(List.of("Wither_101","Thor","NORMAL","12","a-b","Имя","Player Name",""));names.add(null);
        var indexed=(java.util.function.Predicate<String>)prepare.invoke(null,names);
        for(String name:names)if(name!=null&&!name.isEmpty())for(String prefix:List.of("","[MVP+] ","x","§a","💎","_","Русский "))for(String suffix:List.of("",": hi","x","_","💎"," §r")){
            String sample=prefix+name+suffix;check(indexed.test(sample)==PlayerNames.contains(sample,names),"indexed nickname exact old token semantics "+sample);
        }
        for(String sample:List.of("Thorns","normal","Кошелёк: 12,345","[VIP] Wither_101: pickaxe","No player here","Wither_101x","Player Name"))check(indexed.test(sample)==PlayerNames.contains(sample,names),"indexed names no false positives");
        names.add("NewArrival");var updated=(java.util.function.Predicate<String>)prepare.invoke(null,names);
        check(updated.test("NewArrival")&&!indexed.test("NewArrival"),"roster rebuild sees arrivals; old snapshot not mutable");
        var display=Style.EMPTY.withColor(0x123456).withInsertion("fresh prefix");
        var untranslated=FormattedCharSequence.forward("💎 Пример ",display);
        var changing=FormattedCharSequence.forward("Normal",Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/original")));
        var tail=FormattedCharSequence.forward(" 123",display);int[] visits={0};
        FormattedCharSequence multiple=sink->{visits[0]++;return FormattedCharSequence.composite(List.of(untranslated,changing,tail)).accept(sink);};
        var result=SkyRuClient.sequence(multiple);var actual=new StringBuilder();var actualStyles=styles(result);
        result.accept((n,s,p)->{actual.appendCodePoint(p);return true;});
        check(visits[0]==1&&actual.toString().equals("💎 Пример Обычный режим 123"),"single pass late changed run/astral prefix");
        check(actualStyles.get(0).equals(display)&&actualStyles.getLast().equals(display),"UTF16 astral prefix/tail styles retained");
        var unchangedSource=FormattedCharSequence.forward("Пример 123",display);check(SkyRuClient.sequence(unchangedSource)==unchangedSource,"negative fallback no copied sequence");
        check(!TechnicalText.active(),"no global debug flag");
        try(var debug=TechnicalText.open()){
            check(TechnicalText.active()&&UserInputText.rendering(),"technical draw protected without stack inspection");
            int[] read={0};FormattedCharSequence technical=sink->{read[0]++;return FormattedCharSequence.forward("Normal",display).accept(sink);};
            check(SkyRuClient.sequence(technical)==technical&&read[0]==0,"technical draw no source traversal");
            var header=Component.literal("Normal");check(SkyRuClient.componentOutsideHud(header)==header&&SkyRuClient.textOutsideHud("Normal").equals("Normal"),"F3 original high-level APIs");
            try(var nested=TechnicalText.open()){check(TechnicalText.active(),"nested debug scope");}
            check(TechnicalText.active(),"nested debug restores outer");
            try{try(var nested=TechnicalText.open()){throw new IllegalStateException("synthetic F3 failure");}}catch(IllegalStateException expected){}
            check(TechnicalText.active(),"debug exception restores outer");
        }
        check(!TechnicalText.active()&&SkyRuClient.textOutsideHud("Normal").equals("Обычный режим"),"normal HUD translation restored after F3 draw");
        check(RenderContext.classify(List.of("net.minecraft.client.gui.components.DebugScreenOverlay#extractLines")).protectedText(),"debug fallback recognition");
        check(RenderContext.classify(List.of("net.minecraft.client.gui.components.debugchart.FpsDebugChart#drawString")).protectedText(),"debug chart fallback recognition");
        check(!RenderContext.classify(List.of("at.hannibal2.skyhanni.Widget#render")).protectedText(),"other HUD not blocked by F3 visibility");
        for(int i=0;i<4200;i++){String text="Кошелёк: "+i;edits(db,text);db.translateForSearch(text);}
        for(String name:List.of("cache","titleCache","quietCache","displayCache","reorderedCache","styleFailureCache"))check(((Map<?,?>)field(db,name)).size()<=4096,"bounded memory "+name);
        check(((Map<?,?>)field(db,"ruleBuckets")).size()<=128,"bounded prefix index");
        System.out.println("PASS: performance caches "+checks+" assertions; fresh styles/actions/values, separate bounded plans/quiet cache, log semantics, context equivalence/lifetime and whole-chat guards; no live game");
    }
    private static Object fieldStatic(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
}
