package ru.wither.skyru;

import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;

/** Receipt-local normalization, fresh actions and no retained message/style cache. */
public final class TestOptimization94 {
    private static int checks;
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
    private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
    public static void main(String[] args)throws Exception{
        var db=new OfflineDictionary(Path.of(args[0]));db.setRecordUnknown(false);
        flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",true);
        flag("npcDialogues",new NpcDialogueTranslator());flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("storyDialogues",NpcDialogueTranslator.storyDialogues());flag("winterDialogues",NpcDialogueTranslator.winterDialogues());flag("dungeonDialogues",NpcDialogueTranslator.dungeonDialogues());flag("questObjectives",NpcDialogueTranslator.questObjectives());flag("dungeonNotifications",NpcDialogueTranslator.dungeonNotifications());flag("chatNotifications",null);
        var story=NpcDialogueTranslator.storyDialogues();String key="[NPC] Fetchur: hi i need your help maybe",target="[NPC] Fetchur: привет мне наверно нужна твоя помощь";
        for(int i=0;i<40;i++){
            var style=Style.EMPTY.withColor(0x123400+i).withClickEvent(new ClickEvent.SuggestCommand("/fresh94 "+i)).withHoverEvent(new HoverEvent.ShowText(Component.literal("fresh94 "+i)));
            var original=Component.literal((i%2==0?"[01:02:03] ":"")+"§e"+key+(i%3==0?" (x17)":"")).setStyle(style);int[] reads={0};
            Component proxy=(Component)Proxy.newProxyInstance(Component.class.getClassLoader(),new Class<?>[]{Component.class},(p,m,a)->{if(m.getName().equals("getString"))reads[0]++;return m.invoke(original,a);});
            var input=new NpcDialogueTranslator.Input(proxy);check(reads[0]==1,"one current receipt snapshot");
            for(var provider:List.of(new NpcDialogueTranslator(),NpcDialogueTranslator.gardenDialogues(),NpcDialogueTranslator.questDialogues(),NpcDialogueTranslator.dungeonDialogues()))provider.translate(proxy,db,input);
            var result=story.translate(proxy,db,input);check(reads[0]==1,"closed providers never refold same input");
            check(result.getString().equals((i%2==0?"[01:02:03] ":"")+target+(i%3==0?" (x17)":"")),"time/counter current");
            check(input.counterStart()==input.counterStart(),"cached counter position only");reads[0]=0;
            var msg=new GuiMessage(0,proxy,null,GuiMessageSource.SYSTEM_SERVER,null);var out=SkyRuClient.npcChat(msg);check(out!=msg&&reads[0]==1,"dispatcher shares one text read among providers");
            reads[0]=0;var player=new GuiMessage(0,proxy,null,GuiMessageSource.PLAYER,null);check(SkyRuClient.npcChat(player)==player&&reads[0]==0,"player no snapshot");
            var signed=new GuiMessage(0,proxy,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed&&reads[0]==0,"signed no snapshot");
            var local=new GuiMessage(0,proxy,null,GuiMessageSource.SYSTEM_CLIENT,null);check(SkyRuClient.npcChat(local)==local&&reads[0]==0,"client no snapshot");
        }
        var mutable=Component.literal(key);var before=new NpcDialogueTranslator.Input(mutable);mutable.append(" changed");check(story.translate(mutable,db,before)==mutable,"mutated source declined by visible-text guard");check(story.translate(mutable,db,new NpcDialogueTranslator.Input(mutable))==mutable,"next receipt sees changed text");
        for(var f:NpcDialogueTranslator.Input.class.getDeclaredFields())check(f.getType()==String.class||f.getType()==int.class,"snapshot contains only receipt text and scalar positions");
        check(story.phraseCount()==10799,"translation corpus unchanged");check(!TechnicalText.active(),"no protected draw scope leak");
        System.out.println("PASS: optimization94 "+checks+" assertions;one receipt text read across closed providers;time/counters/mutation/current actions;player,signed,client guards;no retained style/component snapshots;corpus9042 unchanged;not live FPS");
    }
}
