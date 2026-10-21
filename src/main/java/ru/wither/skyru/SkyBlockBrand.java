package ru.wither.skyru;

/** Protect the complete brand before a renderer splits it into coloured runs. */
public final class SkyBlockBrand {
    private SkyBlockBrand() {}
    public static boolean header(String text) {
        return text != null && LegacyFormatting.strip(text).strip().equalsIgnoreCase("SKYBLOCK");
    }
}
