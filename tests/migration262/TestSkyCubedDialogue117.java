import java.nio.file.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.StringTag;
import ru.wither.skyru.*;

/** Independent expectations through existing engines; no foreign runtime or production HTTP. */
public final class TestSkyCubedDialogue117 {
    private static int checks;
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static void field(String name,Object value)throws Exception{var f=SkyRuClient.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static List<Style> styles(Component c){var list=new ArrayList<Style>();c.visit((style,text)->{for(int i=0;i<text.length();i++)list.add(style);return Optional.empty();},Style.EMPTY);return list;}
    private static Style live(int color,int seed,ClickEvent click){return Style.EMPTY.withColor(color).withShadowColor(seed).withInsertion("live-"+seed).withHoverEvent(new HoverEvent.ShowText(Component.literal("hover-"+seed))).withClickEvent(click);}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args[0]);Files.createDirectories(root);var db=new OfflineDictionary(root);db.setRecordUnknown(false);
        field("dictionary",db);field("enabled",true);field("onlyHypixel",false);field("translateNpcChat",true);field("translateGameChat",true);
        field("npcDialogues",new NpcDialogueTranslator());field("storyDialogues",NpcDialogueTranslator.storyDialogues());
        field("questDialogues",NpcDialogueTranslator.questDialogues());field("gardenDialogues",NpcDialogueTranslator.gardenDialogues());
        field("chatNotifications",new ChatNotificationTranslator(db));
        var name=Component.literal("David Hunterborough");
        String english="You should go talk to my assistant Kysha, right next to the Fusion Machine in the back.";
        String russian="Поговори с моей помощницей Кишей. Она сзади, рядом с устройством слияния.";
        for(int seed:List.of(1,2))for(boolean fragmented:List.of(false,true)){
            var action=new ClickEvent.RunCommand("/unchanged_dialogue "+seed);
            var ordinary=live(0xAAAAAA,seed,action);var kysha=live(0x55FFFF,seed,action);var machine=live(0xFFAA00,seed,action);
            var body=Component.empty();String[] bits={"You should go talk to my assistant ","Kysha",", right next to the ","Fusion Machine ","in the back."};Style[] look={ordinary,kysha,ordinary,machine,ordinary};
            for(int n=0;n<bits.length;n++){if(fragmented)for(char c:bits[n].toCharArray())body.append(Component.literal(String.valueOf(c)).setStyle(look[n]));else body.append(Component.literal(bits[n]).setStyle(look[n]));}
            var before=body.copy();var translated=SkyCubedDialogueBridge.dialogue(name,body);
            check(translated.getString().equals(russian),"David full authored translation: "+translated.getString());
            check(body.equals(before)&&name.getString().equals("David Hunterborough"),"original receipt/name unchanged");
            var style=styles(translated);for(int i=0;i<style.size();i++){
                Style expected=i>=russian.indexOf("Кишей")&&i<russian.indexOf("Кишей")+5?kysha:i>=russian.indexOf("устройством слияния")&&i<russian.indexOf("устройством слияния")+"устройством слияния".length()?machine:ordinary;
                check(style.get(i).equals(expected),"fresh semantic colour/action "+i);
            }
            check(SkyCubedDialogueBridge.dialogue(name,translated)==translated,"translated body idempotent");
            var message=Component.literal("[NPC] ").append(name).append(": ").append(body);
            var system=new GuiMessage(0,message,null,GuiMessageSource.SYSTEM_SERVER,null);
            check(SkyRuClient.npcChat(system).content().getString().equals("[NPC] David Hunterborough: "+russian),"ordinary chat remains translated");
            var player=new GuiMessage(0,message,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(player)==player,"player spoof remains untouched");
            for(ClickEvent click:List.of(action,new ClickEvent.Custom(Identifier.parse("hypixel:dialogue"),Optional.of(StringTag.valueOf("opaque-option-"+seed))))){
                var optionStyle=live(0x55FF55,seed,click);var group=Component.literal("[").append(Component.literal("Anything else?").setStyle(optionStyle)).append("]");
                var saved=group.copy();var out=SkyCubedDialogueBridge.options(group);
                check(out.getString().equals("[Что-нибудь ещё?]"),"existing option family: "+out.getString());
                check(group.equals(saved),"source options unchanged");
                for(var current:styles(out).subList(1,out.getString().length()-1))check(current.equals(optionStyle),"option command/custom/hover/insertion unchanged");
                check(SkyCubedDialogueBridge.options(out)==out,"options idempotent");
            }
        }
        var unknown=Component.literal("This unknown overlay sentence ZXQ117 remains English.");
        check(SkyCubedDialogueBridge.dialogue(name,unknown)==unknown,"missing dialogue unchanged");
        var unknownOptions=Component.literal("[Unknown choice ZXQ117]");check(SkyCubedDialogueBridge.options(unknownOptions)==unknownOptions,"missing option unchanged");
        var partial=Component.literal("Уже по-русски: ").append(Component.literal("Click to view!"));
        check(SkyCubedDialogueBridge.dialogue(name,partial).getString().equals("Уже по-русски: Нажмите, чтобы посмотреть!"),"mixed component reuses dictionary");
        check(SkyCubedDialogueBridge.options(Component.literal("[Уже по-русски] [Click to view!]")).getString().equals("[Уже по-русски] [Нажмите, чтобы посмотреть!]"),"partially translated option group");
        check(SkyCubedDialogueBridge.options(Component.literal("[Уже по-русски] [Anything else?]")).getString().equals("[Уже по-русски] [Что-нибудь ещё?]"),"mixed group reuses individual NPC choice family");
        var mixedNpc=Component.literal("Уже по-русски: ").append(Component.literal(english));
        check(SkyCubedDialogueBridge.dialogue(name,mixedNpc).getString().equals("Уже по-русски: "+russian),"mixed group reuses complete NPC sentence engine");
        check(SkyCubedDialogueBridge.name(Component.literal("Thaumaturgist")).getString().equals("Тауматург"),"supported name uses current dictionary");
        var opaque=Component.translatable("gui.yes");check(SkyCubedDialogueBridge.dialogue(name,opaque)==opaque,"opaque component unchanged");
        try(var scope=TechnicalText.open()){
            var guarded=Component.literal("[Anything else?]");check(SkyCubedDialogueBridge.options(guarded)==guarded,"technical/input scope protected");
        }
        var privateChat=Component.literal("From Player: private input text");
        var user=new GuiMessage(0,privateChat,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(user)==user,"private player chat untouched");
        field("translateNpcChat",false);check(SkyCubedDialogueBridge.dialogue(name,Component.literal(english)).getString().equals(english),"NPC toggle respected");
        check(SkyCubedDialogueBridge.options(Component.literal("[Anything else?]")).getString().equals("[Anything else?]"),"option toggle respected");field("translateNpcChat",true);
        field("enabled",false);check(SkyCubedDialogueBridge.dialogue(name,unknown)==unknown,"master switch respected");field("enabled",true);
        var custom=root.resolve("override");Files.createDirectories(custom);Files.writeString(custom.resolve("overrides.tsv"),"Anything else?\tДругая авторская замена\n");var override=new OfflineDictionary(custom);override.setRecordUnknown(false);field("dictionary",override);
        field("chatNotifications",new ChatNotificationTranslator(override));
        check(SkyCubedDialogueBridge.options(Component.literal("[Anything else?]")).getString().equals("[Другая авторская замена]"),"existing option override wins");field("dictionary",db);field("chatNotifications",new ChatNotificationTranslator(db));
        String a="486147e525b113246e9806e6601b509966ea33868044e477602db34b15550906",b="4237b364e266db0e8f886303e8be5a6b095d3dfa5a128ba4cb43231caf014bd0";
        check(CompatibilityMixinPlugin.supportedSkyCubed("1.10.0","4.2.41",a,b),"verified vendor accepted");
        for(String version:List.of("","1.10.1","1.9.0"))check(!CompatibilityMixinPlugin.supportedSkyCubed(version,"4.2.41",a,b),"unknown SkyCubed skipped");
        check(!CompatibilityMixinPlugin.supportedSkyCubed("1.10.0","4.2.42",a,b),"unknown publisher skipped");
        check(!CompatibilityMixinPlugin.supportedSkyCubed("1.10.0","4.2.41","different",b),"same-version changed bytecode skipped");
        check(!CompatibilityMixinPlugin.supportedSkyCubed("1.10.0","4.2.41",a,"different"),"changed publisher provenance skipped");
        System.out.println("PASS SkyCubed dialogue117: "+checks+" assertions; existing NPC/options, live semantic styles, Custom actions, overrides, mixed text, misses, chat privacy and version gates; not live Hypixel");
    }
}
