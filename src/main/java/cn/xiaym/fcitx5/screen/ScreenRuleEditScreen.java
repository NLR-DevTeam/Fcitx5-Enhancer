package cn.xiaym.fcitx5.screen;

import cn.xiaym.fcitx5.config.rules.ScreenRule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static cn.xiaym.fcitx5.screen.ElementRuleEditScreen.*;

public class ScreenRuleEditScreen extends Screen {
    private final Screen parentScreen;
    private final ScreenRule screenRule;
    private final Runnable removeCallback;
    private final Consumer<ScreenRule> saveCallback;

    public ScreenRuleEditScreen(Screen parent, ScreenRule screenRule, Runnable removeCallback, Consumer<ScreenRule> saveCallback) {
        super(Component.translatable("fcitx5.screen.screenRuleEdit.title"));
        this.parentScreen = parent;
        this.screenRule = screenRule;
        this.removeCallback = removeCallback;
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        EditBox commentField = addRenderableWidget(new EditBox(font, centerX - 10, 40, 150, 20, Component.empty()));
        EditBox screenClassField = addRenderableWidget(new EditBox(font, centerX - 10, 70, 150, 20, Component.empty()));
        commentField.setMaxLength(65535);
        screenClassField.setMaxLength(65535);
        commentField.setValue(screenRule.comment() == null ? "" : screenRule.comment());
        screenClassField.setValue(nullSafe(screenRule.screenClassName()));

        boolean[] shouldBlock = { screenRule.shouldBlock() };
        addRenderableWidget(Button.builder(getYesOrNoText(shouldBlock[0]), button -> {
            shouldBlock[0] = !shouldBlock[0];
            button.setMessage(getYesOrNoText(shouldBlock[0]));
        }).bounds(centerX - 10, 100, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose()).width(98)
                .pos(centerX - 150, height - 30).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), _ -> {
            saveCallback.accept(new ScreenRule(commentField.getValue(), screenClassField.getValue(), shouldBlock[0]));
            onClose();
        }).width(98).pos(centerX - 50, height - 30).build());

        addRenderableWidget(createRemoveButton(centerX + 50, height - 30, () -> {
            removeCallback.run();
            onClose();
        })).active = screenRule.comment() != null /* Disable if is newly created */;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        super.extractRenderState(context, mouseX, mouseY, a);
        int centerX = width / 2;

        // Title
        context.centeredText(font, Component.translatable("fcitx5.screen.screenRuleEdit.title"), centerX, 20, CommonColors.WHITE);

        // Fields
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.comment"), centerX - 140, 48, CommonColors.WHITE);
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.screenClass"), centerX - 140, 78, CommonColors.WHITE);
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.shouldBlock"), centerX - 140, 108, CommonColors.WHITE);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }
}
