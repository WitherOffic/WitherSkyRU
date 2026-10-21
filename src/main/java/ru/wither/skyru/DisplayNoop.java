package ru.wither.skyru;

/** Proven unchanged display forms only; no guessed Cyrillic/unknown-prose shortcut. */
final class DisplayNoop {
    private DisplayNoop(){}
    // A shared, stateless sink: no strings, styled runs, caller walks or retained
    // render objects for a number-only sequence. Non-numeric prose stops at its
    // first non-whitelisted glyph and follows the ordinary translation path.
    private static final net.minecraft.util.FormattedCharSink NUMERIC=(i,s,cp)->number(cp);
    static boolean numbers(net.minecraft.util.FormattedCharSequence text){return text.accept(NUMERIC);}
    static boolean text(String text){return numbers(text)||ScreenLabels.finalText(text)||SkyBlockBrand.header(text);}
    static boolean numbers(String text){
        for(int i=0;i<text.length();i++){
            if(!number(text.charAt(i)))return false;
        }
        return true;
    }
    private static boolean number(int c){
            if(c>='0'&&c<='9')return true;
            return switch(c){
                case ' ','\t','\n','\r','\f','\u000b','.',',','+','%','(',')',':','/','-','×','*','±','❤','✦','☆','★','✧','☘','⚔','❈','☠','✿','♣','♦','⛏' -> true;
                default -> false;
            };
    }
}
