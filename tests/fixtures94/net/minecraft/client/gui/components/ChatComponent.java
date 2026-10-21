package net.minecraft.client.gui.components;

import net.minecraft.util.FormattedCharSequence;
import ru.wither.skyru.SkyRuClient;
import ru.wither.skyru.TechnicalText;

/** Benchmark-only exact caller identity. Never included in a mod or correctness JVM. */
public final class ChatComponent {
    public static FormattedCharSequence draw(FormattedCharSequence source,boolean scoped){
        if(scoped){try(var context=TechnicalText.open()){return SkyRuClient.sequenceOutsideHud(source);}}
        return SkyRuClient.sequenceOutsideHud(source);
    }
}
