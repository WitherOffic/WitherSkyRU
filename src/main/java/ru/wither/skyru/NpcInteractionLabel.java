package ru.wither.skyru;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.StringDecomposer;

/** Exact non-player world CLICK prompt only. Never a dictionary substring or item ability. */
final class NpcInteractionLabel {
    private NpcInteractionLabel() {}
    static Component translate(Component source,OfflineDictionary dictionary){
        if(TechnicalText.active()||UserInputText.rendering())return source;
        if(dictionary.hasCustomOverride(java.util.List.of("CLICK")))return SkyRuClient.component(source);
        if(!literal(source,0,new int[]{0}))return source;
        Style[] style={null};boolean[] uniform={true};StringBuilder text=new StringBuilder(5);
        StringDecomposer.iterateFormatted(source,Style.EMPTY,(i,current,point)->{
            if(style[0]!=null&&!style[0].equals(current)){uniform[0]=false;return false;}
            style[0]=current;text.appendCodePoint(point);return text.length()<=5;
        });
        if(!uniform[0]||!text.toString().equals("CLICK"))return source;
        return Component.literal("НАЖМИТЕ").setStyle(style[0]);
    }
    private static boolean literal(Component c,int depth,int[] count){
        if(depth>18||++count[0]>128||!(c.getContents() instanceof PlainTextContents))return false;
        for(var child:c.getSiblings())if(!literal(child,depth+1,count))return false;
        return true;
    }
}
