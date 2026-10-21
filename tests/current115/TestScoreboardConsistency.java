import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.multiplayer.chat.*;
import org.joml.Matrix3x2f;
import ru.wither.skyru.*;

/** Screenshot regressions: intact brand, closed HUD spans and actual panel capacity. */
public class TestScoreboardConsistency {
    private static int checks;
    private static final Style LABEL=Style.EMPTY.withColor(0xffaa00).withInsertion("label");
    private static final Style VALUE=Style.EMPTY.withColor(0x55ff55).withClickEvent(new ClickEvent.RunCommand("/original"));
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    private static void flag(String name,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,v);}
    private static String plain(FormattedCharSequence s){var b=new StringBuilder();s.accept((i,t,p)->{b.appendCodePoint(p);return true;});return b.toString();}
    private static List<Style> styles(FormattedCharSequence c){var s=new ArrayList<Style>();c.accept((i,t,p)->{for(int n=0;n<Character.charCount(p);n++)s.add(t);return true;});return s;}
    private static void translated(Component c,String target){
        check(SkyRuClient.component(c).getString().equals(target),c.getString()+" component => "+SkyRuClient.component(c).getString());
        var sequence=SkyRuClient.sequence(c.getVisualOrderText(),true);
        check(plain(sequence).equals(target),c.getString()+" sequence => "+plain(sequence));
        for(int n=0;n<5;n++){sequence=SkyRuClient.sequence(sequence,true);check(plain(sequence).equals(target),"idempotent HUD");}
        var message=new GuiMessage(1,c,null,GuiMessageSource.PLAYER,null);
        check(SkyRuClient.npcChat(message)==message,"player chat unchanged");
    }
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);
        for(String brand:List.of("SKYBLOCK","SkyBlock"," skyblock ","§eSKY§6BLOCK","§l§eSKYBLOCK§r")){
            check(SkyBlockBrand.header(brand),"complete brand");check(SkyRuClient.text(brand).equals(brand),"string brand");
            var c=Component.literal(brand).setStyle(LABEL);check(SkyRuClient.component(c)==c,"original component");
            var s=c.getVisualOrderText();for(int n=0;n<5;n++)check(SkyRuClient.sequence(s,true)==s,"original glyphs");
        }
        for(int split=1;split<8;split++){
            var c=Component.empty().append(Component.literal("SKYBLOCK".substring(0,split)).setStyle(LABEL))
                .append(Component.literal("SKYBLOCK".substring(split)).setStyle(VALUE));
            check(SkyRuClient.component(c)==c,"split brand component");var s=c.getVisualOrderText();check(SkyRuClient.sequence(s,true)==s,"split brand styles");
        }
        var rainbow=Component.empty();for(int i=0;i<8;i++)rainbow.append(Component.literal("SKYBLOCK".substring(i,i+1)).setStyle(LABEL.withColor(0x101010+i)));
        check(SkyRuClient.component(rainbow)==rainbow,"per-glyph brand");var glyphs=rainbow.getVisualOrderText();check(SkyRuClient.sequence(glyphs,true)==glyphs,"per-glyph original styles");
        for(String other:List.of("BLOCK","SKY","SKYBLOCK XP","Your SkyBlock Menu","SKYBLOCK!","SKYBLOCK\nObjective"))check(!SkyBlockBrand.header(other),"not broad brand suppression");
        var quest=Component.empty().append(Component.literal("Paste the ").setStyle(LABEL)).append(Component.literal("Greenhouse").setStyle(VALUE)).append(Component.literal(" plot").setStyle(LABEL));
        translated(quest,"Разместите теплицу на участке");check(styles(SkyRuClient.sequence(quest.getVisualOrderText(),true)).get(11).equals(VALUE),"greenhouse colour/action");
        var fiesta=Component.empty().append(Component.literal("- Mining ").setStyle(LABEL)).append(Component.literal("Fiesta").setStyle(VALUE));
        translated(fiesta,"- Горнодобывающая фиеста");check(styles(SkyRuClient.sequence(fiesta.getVisualOrderText(),true)).getLast().equals(VALUE),"fiesta action");
        translated(Component.literal("- Benediction").setStyle(LABEL),"- Благословение");
        translated(Component.literal("Normal").setStyle(LABEL),"Обычный режим");
        for(String end:List.of(" to revisit"," to revisit your Garden"," to revisit your Garden again")){
            var beth=Component.empty().append(Component.literal("Wait for ").setStyle(LABEL))
                .append(Component.literal("Beth").setStyle(VALUE)).append(Component.literal(end).setStyle(LABEL));
            String target=end.equals(" to revisit")?"Дождитесь визита Beth":"Дождитесь следующего визита Beth в ваш Сад";
            translated(beth,target);check(db.translate(beth.getString()).equals(target),"plain full/short goal");
            int at=target.indexOf("Beth");var next=styles(SkyRuClient.sequence(beth.getVisualOrderText(),true));
            for(int i=at;i<at+4;i++)check(next.get(i).equals(VALUE),"Beth unchanged name/style/action");
            for(String prefix:List.of("Objective: ","Цель: ")){
                var wrapped=Component.empty().append(Component.literal(prefix).setStyle(LABEL.withColor(0xffffff))).append(beth);
                translated(wrapped,"Цель: "+target);check(db.translate(wrapped.getString()).equals("Цель: "+target),"whole objective prefix/body");
            }
        }
        translated(Component.literal("Sawdust: 5,011 (+2,283)"),"Опилки: 5,011 (+2,283)");
        translated(Component.literal("Sawdust: 59,481"),"Опилки: 59,481");
        for(int indent=0;indent<=6;indent++)for(String percent:List.of("0%","52.1%","100%")){
            String padding=" ".repeat(indent);var cleanup=Component.empty().append(Component.literal(padding+"Cleanup: ").setStyle(LABEL)).append(Component.literal(percent).setStyle(VALUE));
            translated(cleanup,padding+"Очистка: "+percent);check(db.translate(cleanup.getString()).equals(padding+"Очистка: "+percent),"screenshot cleanup prefix/count/indent");
            check(styles(SkyRuClient.sequence(cleanup.getVisualOrderText(),true)).subList(padding.length()+9,padding.length()+9+percent.length()).stream().allMatch(VALUE::equals),"cleanup number and percent actions");
        }
        for(String crop:List.of("Carrot","Морковь")){
            var row=Component.empty().append(Component.literal("Plot").setStyle(LABEL))
                .append(Component.literal(" - ").setStyle(LABEL)).append(Component.literal(crop).setStyle(VALUE))
                .append(Component.literal(" ♟ x7").setStyle(VALUE));
            translated(row,"Участок - Морковь ♟ x7");
            check(styles(SkyRuClient.sequence(row.getVisualOrderText(),true)).getLast().equals(VALUE),"pest glyph/count unchanged");
        }
        OverhaulTextBudget.within(100,150,()->{
            check(HudTextFit.currentBudget(TextAlignment.LEFT,100,35,new Matrix3x2f())==150,"actual Overhaul name space, not English width");
            OverhaulTextBudget.within(200,80,()->check(OverhaulTextBudget.available(TextAlignment.LEFT,200,20)==80,"nested draw"));
            check(OverhaulTextBudget.available(TextAlignment.LEFT,100,35)==150,"nested restoration");
            check(OverhaulTextBudget.available(TextAlignment.LEFT,10,35)==35,"foreign coordinates excluded");
        });
        check(OverhaulTextBudget.available(TextAlignment.LEFT,100,35)==35,"scope removed");
        try{OverhaulTextBudget.within(100,150,()->{throw new IllegalStateException("test");});}catch(IllegalStateException expected){}
        check(OverhaulTextBudget.available(TextAlignment.LEFT,100,35)==35,"exception removes scope");
        check(CompatibilityMixinPlugin.supportedOverhaulVersion("1.4.3+mc26.1"),"inspected vendor version");
        check(CompatibilityMixinPlugin.supportedOverhaul151Version("1.5.1+mc26.1"),"new inspected vendor API");
        check(!CompatibilityMixinPlugin.supportedOverhaulVersion("1.5.1+mc26.1")&&!CompatibilityMixinPlugin.supportedOverhaul151Version("1.4.3+mc26.1"),"old/new descriptors never cross-enabled");
        for(String v:List.of("1.5.0+mc26.1","1.5.2+mc26.1","1.5.1","1.5.1+mc1.21"))check(!CompatibilityMixinPlugin.supportedOverhaul151Version(v),"unknown new API disabled");
        for(String v:List.of("1.4.3","1.4.4+mc26.1","1.4.3+mc1.21"))check(!CompatibilityMixinPlugin.supportedOverhaulVersion(v),"uninspected API disabled");
        for(String[] pair:new String[][]{{"Sawdust:","Опилки:"},{"Gems:","Самоцветы:"},{"Coins/h:","Монет/ч:"},{"Farming XP/h:","Опыт фермерства/ч:"},{"Farming Level:","Уровень фермерства:"},{"Yaw:","Поворот:"},{"Pitch:","Наклон:"}})
            for(String value:List.of("0","75","37,317","130,680","47.29%","-3.90","800/0")){
                var c=Component.empty().append(Component.literal(pair[0]+" ").setStyle(LABEL)).append(Component.literal(value).setStyle(VALUE));
                String target=pair[1]+" "+value;translated(c,target);check(db.translate(c.getString()).equals(target),"plain HUD label");
                check(styles(SkyRuClient.sequence(c.getVisualOrderText(),true)).subList(pair[1].length()+1,target.length()).stream().allMatch(VALUE::equals),"all numeric styles/actions");
            }
        var plot=Component.empty().append(Component.literal("Plot").setStyle(LABEL)).append(Component.literal(" - 3 ♟ x3").setStyle(VALUE));
        translated(plot,"Участок - 3 ♟ x3");check(styles(SkyRuClient.sequence(plot.getVisualOrderText(),true)).getLast().equals(VALUE),"plot glyphs");
        flag("fitHudText",false);translated(quest,"Разместите теплицу на участке");flag("fitHudText",true);
        check(!HudTextFit.canFit(35,70),"old short-line fallback reproduced");
        int budget=HudTextFit.panelBudget(900,150,900,TextAlignment.LEFT,35);
        check(budget==150&&HudTextFit.canFit(budget,70)&&HudTextFit.scale(budget,70)==1,"short label fits actual panel without shrinking");
        check(HudTextFit.posedPanelBudget(900,150,0,TextAlignment.LEFT,35,new Matrix3x2f().translate(900,100))==150,"translated row pose");
        check(HudTextFit.posedPanelBudget(900,150,0,TextAlignment.LEFT,35,new Matrix3x2f().translate(900,100).scale(.75f,.75f))==200,"scaled local width");
        for(int width=20;width<=400;width++)for(TextAlignment alignment:TextAlignment.values()){
            int left=800,x=switch(alignment){case LEFT->left;case CENTER->left+width/2;case RIGHT->left+width;};
            int original=Math.min(10,width);int available=HudTextFit.panelBudget(left,width,x,alignment,original);
            check(available>=original&&available<=width,"real bounds");
            for(int next:new int[]{original,width,width*3})if(HudTextFit.canFit(available,next)){
                float scale=HudTextFit.scale(available,next);int fitted=(int)Math.ceil(next*scale);int start=alignment.calculateLeft(x,fitted);
                check(scale>=.85f&&start>=left&&start+fitted<=left+width,"no overflow/tiny font");
            }
        }
        for(Matrix3x2f pose:List.of(new Matrix3x2f().rotate(.4f),new Matrix3x2f().scale(-1,1)))check(HudTextFit.posedPanelBudget(900,150,0,TextAlignment.LEFT,35,pose)==35,"unsupported pose bounded");
        check(HudTextFit.panelBudget(900,150,0,TextAlignment.LEFT,35)==35,"foreign coordinates not guessed");
        check(HudTextFit.eligibleFrames(List.of("me.owdding.customscoreboard.core.CustomScoreboardRenderer#onRender","net.minecraft.client.gui.screens.ChatScreen#extractRenderState")),"HUD remains translated while chat open");
        check(!HudTextFit.eligibleFrames(List.of("me.owdding.customscoreboard.core.CustomScoreboardRenderer#onRender","net.minecraft.client.gui.components.ChatComponent#extractRenderState")),"chat itself excluded");
        check(!HudTextFit.eligibleFrames(List.of("me.owdding.customscoreboard.core.CustomScoreboardRenderer#onRender","net.minecraft.client.gui.screens.inventory.InventoryScreen#extractRenderState")),"other screens excluded");
        var dir=Path.of(args[0]).resolve("custom");Files.createDirectories(dir);Files.writeString(dir.resolve("overrides.tsv"),"Gems:\tМои камни:\nPaste the Greenhouse plot\tМоя цель\nCleanup:\tМоя очистка:\n");
        flag("dictionary",new OfflineDictionary(dir));translated(Component.literal("Gems: 75"),"Мои камни: 75");translated(Component.literal("Paste the Greenhouse plot"),"Моя цель");
        translated(Component.literal("  Cleanup: 52.1%"),"  Моя очистка: 52.1%");
        flag("dictionary",db);flag("enabled",false);var off=quest.getVisualOrderText();check(SkyRuClient.sequence(off,true)==off,"disabled");
        System.out.println("PASS: scoreboard consistency "+checks+" assertions; original split SKYBLOCK, bounded labels/styles/values, real panel/pose budgets, overrides and player chat; no live game");
    }
}
