package ru.wither.skyru.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wither.skyru.*;
import java.util.ArrayList;
import java.util.List;

/** Manual helper on Hypixel sign editors. Does not close the screen or intercept packets. */
@Mixin(AbstractSignEditScreen.class)
public abstract class SignSearchMixin extends Screen {
    @Shadow @Final private String[] messages;
    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private boolean isFrontText;
    @Shadow private SignText text;
    @Shadow private int line;
    @Shadow private TextFieldHelper signField;
    @Unique private List<Button> witherskyru$suggestions;
    protected SignSearchMixin() { super(Component.empty()); }
    @Inject(method = "init()V", at = @At("RETURN"), require = 1)
    private void witherskyru$initNames(CallbackInfo ci) {
        if (!SkyRuClient.active()) return;
        witherskyru$suggestions = new ArrayList<>();
        int panelWidth = Math.min(180, Math.max(100, width / 3));
        addRenderableWidget(Button.builder(Component.literal("Название EN"), b -> witherskyru$findNames())
            .bounds(6, 18, panelWidth, 20).tooltip(Tooltip.create(Component.literal(
                "Введите русское название в первой строке. Выберите оригинал ниже, затем нажмите обычную кнопку «Готово». Ничего не отправляется автоматически."))).build());
        for (int i = 0; i < 5; i++) {
            Button result = Button.builder(Component.empty(), b -> witherskyru$chooseName(b.getMessage().getString()))
                .bounds(6, 42 + i * 24, panelWidth, 20).build();
            result.visible = false;
            witherskyru$suggestions.add(addRenderableWidget(result));
        }
    }
    @Unique private void witherskyru$findNames() {
        List<String> results = SkyRuClient.signSearchNames(messages[0]);
        for (int i = 0; i < witherskyru$suggestions.size(); i++) {
            Button button = witherskyru$suggestions.get(i);
            button.visible = i < results.size() || (results.isEmpty() && i == 0);
            button.active = i < results.size();
            if (button.active) {
                button.setMessage(Component.literal(results.get(i)));
                button.setTooltip(Tooltip.create(Component.literal(results.get(i))));
            } else if (button.visible) {
                button.setMessage(Component.literal("Нет совпадений"));
                button.setTooltip(Tooltip.create(Component.literal("Введите русское название в первой строке. Неизвестные названия не угадываются.")));
            }
        }
    }
    @Unique private void witherskyru$chooseName(String original) {
        // Revalidate against the CURRENT input: stale results cannot change a different query.
        if (!SkyRuClient.signSearchNames(messages[0]).contains(original)) return;
        line = 0;
        messages[0] = original;
        text = text.setMessage(0, Component.literal(original));
        sign.setText(text, isFrontText);
        signField.setCursorToEnd();
        for (Button result : witherskyru$suggestions) result.visible = false;
        // Only the normal vanilla screen closure submits. This helper never sends or closes.
    }
}
