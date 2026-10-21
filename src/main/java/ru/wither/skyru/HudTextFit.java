package ru.wither.skyru;

import java.util.List;

/** Presentation-only HUD policy: no changes to Font metrics or server data. */
public final class HudTextFit {
    public static final float MIN_READABLE_SCALE = 0.85f;
    private HudTextFit() {}

    /** Tooltip lines are not fixed-width HUD labels; never reject their translation. */
    public static boolean fixedWidth(boolean hud, boolean tooltip) { return hud && !tooltip; }

    public static boolean currentPanel() {
        if (!SkyRuClient.hudDisplayActive()) return false;
        // Called only for drawn text. Never retained between frames or worlds.
        return RenderContext.current().hud();
    }

    public static boolean eligibleFrames(List<String> frames) {
        boolean panel = false;
        for (String frame : frames) {
            String lower = frame.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("chatcomponent#") || lower.contains(".chat.")
                || lower.contains("tooltip") || lower.contains(".screens.")
                    && !frame.startsWith("net.minecraft.client.gui.screens.ChatScreen#")) return false;
            panel |= frame.startsWith("me.owdding.customscoreboard.core.")
                || frame.startsWith("de.hysky.skyblocker.skyblock.tabhud.")
                || frame.startsWith("at.hannibal2.skyhanni.")
                || ((frame.startsWith("net.minecraft.client.gui.Gui#") || frame.startsWith("net.minecraft.client.gui.Hud#"))
                    && (lower.contains("scoreboard") || lower.contains("sidebar")))
                || lower.contains("scoreboardoverhaul");
        }
        return panel;
    }

    public static float scale(int originalWidth, int translatedWidth) {
        if (originalWidth <= 0 || translatedWidth <= originalWidth) return 1f;
        // Small margin covers rounding and shadow; never enlarges short lines.
        return Math.min(1f, Math.max(MIN_READABLE_SCALE,
            Math.max(1, originalWidth - 1) / (float) translatedWidth));
    }

    /** Fixed-height panels cannot safely wrap. Keep the original instead of tiny/overflowing text. */
    public static boolean canFit(int originalWidth, int translatedWidth) {
        if (translatedWidth <= Math.max(0, originalWidth)) return true;
        return originalWidth > 0 && translatedWidth * scale(originalWidth, translatedWidth) <= originalWidth;
    }

    /** A line's English width is not its panel's available width. */
    public static int panelBudget(int panelLeft, int panelWidth, int x,
        net.minecraft.client.gui.TextAlignment alignment, int originalWidth) {
        if (panelWidth <= 0 || panelWidth > 16384 || originalWidth < 0) return originalWidth;
        long right = (long) panelLeft + panelWidth;
        int originalLeft = alignment.calculateLeft(x, originalWidth);
        if (originalLeft < panelLeft || (long) originalLeft + originalWidth > right) return originalWidth;
        long available = switch (alignment) {
            case LEFT -> right - x;
            case RIGHT -> (long) x - panelLeft;
            case CENTER -> 2L * Math.min((long) x - panelLeft, right - x);
        };
        return (int) Math.max(originalWidth, Math.min(panelWidth, available));
    }

    /** Convert live GUI bounds to the local coordinates used by the drawn text. */
    public static int posedPanelBudget(int panelLeft, int panelWidth, int x,
        net.minecraft.client.gui.TextAlignment alignment, int originalWidth, org.joml.Matrix3x2fc pose) {
        if (pose == null || !Float.isFinite(pose.m00()) || !Float.isFinite(pose.m20())
            || pose.m00() <= 0 || Math.abs(pose.m01()) > 0.0001f || Math.abs(pose.m10()) > 0.0001f)
            return originalWidth;
        double left = (panelLeft - (double)pose.m20()) / pose.m00();
        double right = (panelLeft + (double)panelWidth - pose.m20()) / pose.m00();
        if (!Double.isFinite(left) || !Double.isFinite(right) || Math.abs(left) > 16384 || Math.abs(right) > 16384)
            return originalWidth;
        int localLeft = (int)Math.ceil(left), localRight = (int)Math.floor(right);
        return panelBudget(localLeft, localRight - localLeft, x, alignment, originalWidth);
    }

    public static int currentBudget(net.minecraft.client.gui.TextAlignment alignment, int x, int originalWidth,
        org.joml.Matrix3x2fc pose) {
        int scoped=OverhaulTextBudget.available(alignment,x,originalWidth);
        if(scoped!=originalWidth)return scoped;
        boolean custom = RenderContext.current().customScoreboard();
        return custom ? CustomScoreboardBudget.available(alignment, x, originalWidth, pose) : originalWidth;
    }

    /** Short HUD terminology only; digits, percentages and arbitrary prose stay intact. */
    public static String compact(String text) {
        return text.replace("Пыль драгоценных камней", "Пыль самоцветов")
            .replace("пыль драгоценных камней", "пыль самоцветов")
            .replace("Драгоценные камни", "Самоцветы")
            .replace("драгоценные камни", "самоцветы")
            .replace("Драгоценный камень", "Самоцвет")
            .replace("драгоценный камень", "самоцвет")
            .replace("Шахтёрские задания", "Шахт. задания")
            .replace("шахтёрские задания", "шахт. задания");
    }

    /** Second chance only after a full translation does not fit. Keep numeric/style spans. */
    public static net.minecraft.util.FormattedCharSequence narrow(net.minecraft.util.FormattedCharSequence source){
        var text=new StringBuilder();var styles=new java.util.ArrayList<net.minecraft.network.chat.Style>();
        source.accept((i,s,p)->{text.appendCodePoint(p);for(int n=0;n<Character.charCount(p);n++)styles.add(s);return true;});
        String visible=text.toString();var edits=new java.util.ArrayList<OfflineDictionary.TitleEdit>();
        if(visible.strip().equals("Обычный режим")){
            int at=visible.indexOf("Обычный режим");edits.add(new OfflineDictionary.TitleEdit(at,at+13,"Обычный"));
        }
        if(visible.strip().equals("Обновление места…")){
            int at=visible.indexOf("Обновление места…");edits.add(new OfflineDictionary.TitleEdit(at,at+17,"Пересчёт места…"));
        }
        // Both source event words have explicitly reviewed corresponding style spans.
        if(visible.strip().matches("(?:- )?Горнодобывающая фиеста")){
            int at=visible.indexOf("Горнодобывающая");edits.add(new OfflineDictionary.TitleEdit(at,at+15,"Праздник"));
            at=visible.indexOf("фиеста");edits.add(new OfflineDictionary.TitleEdit(at,at+6,"добычи"));
        }
        if(visible.strip().matches("(?:- )?Благословение")){
            int at=visible.indexOf("Благословение");edits.add(new OfflineDictionary.TitleEdit(at,at+13,"Благослов."));
        }
        if(visible.stripLeading().startsWith("Самоцветы:")){
            int at=visible.indexOf("Самоцветы:");edits.add(new OfflineDictionary.TitleEdit(at,at+9,"Камни"));
        }
        if(visible.stripLeading().startsWith("Питомец: ")){
            int at=visible.indexOf("Питомец:");edits.add(new OfflineDictionary.TitleEdit(at,at+7,"Пит."));
        }
        if(edits.isEmpty())return source;
        for(var e:edits)for(int i=e.start()+1;i<e.end();i++)if(!styles.get(i).equals(styles.get(e.start())))return source;
        var out=new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();int at=0;
        for(var e:edits){append(out,visible,styles,at,e.start());out.add(net.minecraft.util.FormattedCharSequence.forward(e.replacement(),styles.get(e.start())));at=e.end();}
        append(out,visible,styles,at,visible.length());return net.minecraft.util.FormattedCharSequence.composite(out);
    }
    private static void append(java.util.List<net.minecraft.util.FormattedCharSequence> out,String text,java.util.List<net.minecraft.network.chat.Style> styles,int at,int end){
        while(at<end){int stop=at+1;while(stop<end&&styles.get(stop).equals(styles.get(at)))stop++;out.add(net.minecraft.util.FormattedCharSequence.forward(text.substring(at,stop),styles.get(at)));at=stop;}
    }
}
