package skycubedsmoke;

import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.StringTag;
import net.fabricmc.loader.api.FabricLoader;
import ru.wither.skyru.*;

/** Real isolated client, actual vendor bytecode and native handler; no server or real account. */
public final class SkyCubedSmoke117 {
    private static int frames,checks;private static boolean done;private static final long start=System.nanoTime();
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
    private static Object value(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static Object call(Object owner,String method)throws Exception{var m=owner.getClass().getDeclaredMethod(method);m.setAccessible(true);return m.invoke(owner);}
    private static void exercise()throws Exception{
        boolean present=FabricLoader.getInstance().isModLoaded("skycubed");
        check(present==Boolean.getBoolean("witherskyru.skycubed.smoke.present"),"expected vendor presence");
        if(!present){
            check(!new CompatibilityMixinPlugin().shouldApplyMixin("tech.thatgravyboat.skycubed.features.overlays.dialogue.DialogueOverlay","ru.wither.skyru.mixin.SkyCubedDialogueMixin"),"absent vendor skipped");return;
        }
        var type=Class.forName("tech.thatgravyboat.skycubed.features.overlays.dialogue.DialogueOverlay");
        check(Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().contains("witherskyru$")).count()==3,"three real Mixin injections applied: "+Arrays.toString(type.getDeclaredMethods()));
        Object overlay=type.getField("INSTANCE").get(null);var queue=(List<?>)value(overlay,"queue");check(queue.isEmpty(),"fresh native queue");
        var pre=Class.forName("tech.thatgravyboat.skyblockapi.api.events.chat.ChatReceivedEvent$Pre");var handler=type.getDeclaredMethod("onChatReceived",pre);handler.setAccessible(true);
        String body="You should go talk to my assistant Kysha, right next to the Fusion Machine in the back.";
        var raw=Component.literal("[NPC] David Hunterborough: "+body).withStyle(Style.EMPTY.withColor(0xFFFFFF));
        Object event=pre.getConstructor(Component.class).newInstance(raw);handler.invoke(overlay,event);
        check(queue.size()==1,"exactly one native queue insertion per receipt");Object pair=queue.getFirst();
        check(((Component)call(pair,"getFirst")).getString().equals("David Hunterborough"),"native NPC lookup key remains English");
        check(((Component)call(pair,"getSecond")).getString().equals("Поговори с моей помощницей Кишей. Она сзади, рядом с устройством слияния."),"real parsed queue body Russian");
        check(pre.getMethod("getComponent").invoke(event)==raw&&raw.getString().endsWith(body),"received event and original chat remain English/unchanged");
        for(boolean custom:List.of(false,true)){
            ClickEvent action=custom?new ClickEvent.Custom(Identifier.parse("hypixel:dialogue"),Optional.of(StringTag.valueOf("opaque-option"))):new ClickEvent.RunCommand("/dialogue_select opaque");
            var style=Style.EMPTY.withColor(0x55FF55).withClickEvent(action).withInsertion("same-insertion").withHoverEvent(new HoverEvent.ShowText(Component.literal("same-hover")));
            var source=Component.literal("Select an option: [").append(Component.literal("Anything else?").setStyle(style)).append("]");
            handler.invoke(overlay,pre.getConstructor(Component.class).newInstance(source));
            var options=(List<?>)value(overlay,"options");check(options.size()==1,"native option count");Object option=options.getFirst();
            var caption=(Component)call(option,"getComponent");check(caption.getString().equals("Что-нибудь ещё?"),"real native option Russian");
            caption.visit((s,t)->{if(!t.isEmpty())check(style.equals(s),"native option live style/hover/click");return Optional.empty();},Style.EMPTY);
            check(call(option,"getKeys")!=null,"native numeric key binding retained");
            if(custom)check(call(option,"getPayload").equals(action),"native Custom id/payload identical");
            else check(call(option,"getCommand").equals("/dialogue_select opaque"),"native command identical");
            check(source.getString().equals("Select an option: [Anything else?]"),"original option receipt unchanged");
        }
        handler.invoke(overlay,pre.getConstructor(Component.class).newInstance(Component.literal("[NPC] David Hunterborough: Unknown overlay sentence ZXQ117.")));
        check(queue.size()==2,"no extra queue accumulation");check(((Component)call(queue.get(1),"getSecond")).getString().equals("Unknown overlay sentence ZXQ117."),"native missing text preserved");
        var create=type.getDeclaredMethod("createOptionsDisplay");create.setAccessible(true);Object display=create.invoke(overlay);
        check(display!=null,"native option display built with translated actions");
        var reset=type.getDeclaredMethod("reset");reset.setAccessible(true);reset.invoke(overlay);
        check(((List<?>)value(overlay,"options")).isEmpty(),"native reset clears options");
    }
    public static void frame(){
        if(done)return;var mc=Minecraft.getInstance();
        try{
            if(System.nanoTime()-start>150_000_000_000L)throw new AssertionError("smoke timeout");
            if(mc.gui.screen()!=null&&mc.gui.screen().getClass().getSimpleName().equals("AccessibilityOnboardingScreen")){mc.gui.screen().onClose();return;}
            if(!(mc.gui.screen() instanceof TitleScreen)||mc.gui.overlay()!=null||++frames<40)return;
            exercise();finish(mc,true,"Native overlay parser/queue/options/actions and real client startup; no Hypixel connection.");
        }catch(Throwable failure){failure.printStackTrace();finish(mc,false,failure.toString());}
    }
    private static void finish(Minecraft mc,boolean pass,String detail){
        done=true;try{Files.writeString(Path.of("skycubed-smoke-result.txt"),(pass?"PASS":"FAIL")+"\nchecks="+checks+"\n"+detail+"\n");}catch(Exception ignored){}
        System.out.println("SKYCUBED117_CLIENT "+(pass?"PASS":"FAIL")+" checks="+checks+" "+detail);mc.stop();
    }
}
