package ru.wither.skyru;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

/** Called only AFTER the pinned SkyCubed parser matches unsigned server-game messages.
 * No foreign types, secondary dictionary, message cache, packets or queue insertion.
 */
public final class SkyCubedDialogueBridge {
    private static final java.util.regex.Pattern OPTIONS=java.util.regex.Pattern.compile("\\[([^\\[\\]\\r\\n]{1,180})\\]");
    private SkyCubedDialogueBridge() {}
    public static Component dialogue(Component name,Component body){
        if(name==null||body==null||!SkyRuClient.overlayDialogueActive()||!literalTree(body,0))return body;
        String speaker=LegacyFormatting.strip(name.getString());
        if(!speaker.matches("[A-Za-z][A-Za-z '.-]{1,35}")||body.getString().length()>4096)return body;
        try{
            String prefix="[NPC] "+speaker+": ";
            var envelope=Component.literal("[NPC] ").append(name).append(": ").append(body);
            Component translated=SkyRuClient.overlayChat(envelope),result=body;
            if(translated!=envelope){
                String text=LegacyFormatting.strip(translated.getString());
                if(text.startsWith(prefix))result=slice(translated,prefix.length(),text.length());
            }else if(!body.getSiblings().isEmpty())result=dialogueFragments(name,body,Style.EMPTY);
            // Existing component rules can finish mixed Russian/English fragments.
            result=SkyRuClient.overlayComponent(result);
            return sameText(body,result)?body:result;
        }catch(RuntimeException unsupported){return body;}
    }
    /** Input is the bracketed options group already captured by SkyCubed's server regex.
     * Reuses the entire existing choice family, retaining action/hover components.
     */
    public static Component options(Component group){
        if(group==null||!SkyRuClient.overlayDialogueActive()||group.getString().length()>4096||!literalTree(group,0))return group;
        try{
            var envelope=Component.literal("Select an option: ").append(group);
            Component translated=SkyRuClient.overlayChat(envelope),result=group;
            if(translated!=envelope){
                String text=LegacyFormatting.strip(translated.getString());int first=text.indexOf('[');
                if(first>=0)result=slice(translated,first,text.length());
            }
            result=optionFragments(SkyRuClient.overlayComponent(result));
            return sameText(group,result)?group:result;
        }catch(RuntimeException unsupported){return group;}
    }
    public static Component name(Component original){
        if(original==null||!SkyRuClient.overlayDialogueActive())return original;
        try{return SkyRuClient.overlayComponent(original);}catch(RuntimeException unsupported){return original;}
    }
    private static boolean sameText(Component a,Component b){return a==b||LegacyFormatting.strip(a.getString()).equals(LegacyFormatting.strip(b.getString()));}
    private static Component optionFragments(Component group){
        String text=LegacyFormatting.strip(group.getString());
        var buttons=OPTIONS.matcher(text);
        var out=Component.empty();int end=0,count=0;boolean changed=false;
        while(buttons.find()){
            if(++count>11)return group;
            var label=slice(group,buttons.start(1),buttons.end(1));Component next=label;
            var wrapped=Component.literal("Select an option: [").append(label).append("]");
            var translated=SkyRuClient.overlayChat(wrapped);
            if(translated!=wrapped){
                String value=LegacyFormatting.strip(translated.getString());int first=value.indexOf('['),last=value.lastIndexOf(']');
                if(first>=0&&last>first)next=slice(translated,first+1,last);
            }
            next=SkyRuClient.overlayComponent(next);
            changed|=!sameText(label,next);out.append(slice(group,end,buttons.start(1))).append(next);end=buttons.end(1);
        }
        return changed?out.append(slice(group,end,text.length())):group;
    }
    /** Complete known NPC sentences in a mixed component still use the NPC engine.
     * Never infer word boundaries or match an arbitrary substring of a literal.
     */
    private static Component dialogueFragments(Component name,Component original,Style parent){
        Style style=original.getStyle().applyTo(parent);
        String source=((net.minecraft.network.chat.contents.PlainTextContents)original.getContents()).text();
        Component head=Component.literal(source).setStyle(style);boolean changed=false;
        if(!source.isEmpty()){
            String prefix="[NPC] "+LegacyFormatting.strip(name.getString())+": ";
            var wrapped=Component.literal("[NPC] ").append(name).append(": ").append(head);
            var translated=SkyRuClient.overlayChat(wrapped);
            if(translated!=wrapped){
                String text=LegacyFormatting.strip(translated.getString());
                if(text.startsWith(prefix)){head=slice(translated,prefix.length(),text.length());changed=!sameText(head,Component.literal(source));}
            }
        }
        var out=Component.empty().append(head);
        for(var child:original.getSiblings()){
            Component next=dialogueFragments(name,child,style);changed|=next!=child;out.append(next);
        }
        return changed?out:original;
    }
    /** Fresh live styles, including Custom click payloads; the original tree stays untouched. */
    private static Component slice(Component source,int start,int end){
        var out=Component.empty();int[] at={0};var text=new StringBuilder();Style[] previous={null};
        StringDecomposer.iterateFormatted(source,Style.EMPTY,(index,style,point)->{
            int next=at[0]+Character.charCount(point);
            if(at[0]>=start&&next<=end){
                if(!style.equals(previous[0])&&text.length()>0){out.append(Component.literal(text.toString()).setStyle(previous[0]));text.setLength(0);}
                previous[0]=style;text.appendCodePoint(point);
            }
            at[0]=next;return next<end;
        });
        if(text.length()>0)out.append(Component.literal(text.toString()).setStyle(previous[0]));
        return out;
    }
    private static boolean literalTree(Component source,int depth){
        if(depth>18||!(source.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents))return false;
        for(var child:source.getSiblings())if(!literalTree(child,depth+1))return false;
        return true;
    }
}
