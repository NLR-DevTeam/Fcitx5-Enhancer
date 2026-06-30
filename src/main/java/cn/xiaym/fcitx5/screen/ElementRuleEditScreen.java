package cn.xiaym.fcitx5.screen;

import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.rules.ElementRule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ElementRuleEditScreen extends Screen {
    private final Screen parentScreen;
    private final ElementRule elementRule;
    private final Runnable removeCallback;
    private final Consumer<ElementRule> saveCallback;

    public ElementRuleEditScreen(Screen parent, ElementRule elementRule, Runnable removeCallback, Consumer<ElementRule> saveCallback) {
        super(Component.translatable("fcitx5.screen.elementRuleEdit.title"));
        this.parentScreen = parent;
        this.elementRule = elementRule;
        this.removeCallback = removeCallback;
        this.saveCallback = saveCallback;
    }

    public static Component getYesOrNoText(boolean b) {
        MutableComponent text = Component.translatable("fcitx5.controls." + b);
        return text.setStyle(text.getStyle().withColor(b ? 0xFF00FF00 /* GREEN */ : 0xFFDF505 /* LIGHT_RED */));
    }

    public static Button createRemoveButton(int x, int y, Runnable callback) {
        boolean[] confirmed = { false };
        MutableComponent message = Component.translatable("fcitx5.controls.remove");
        return Button.builder(message.setStyle(message.getStyle()
                .withColor(0xFFDF5050 /* LIGHT_RED */)), button -> {
            if (!confirmed[0]) {
                MutableComponent confirm = Component.translatable("fcitx5.controls.remove.confirm");
                button.setMessage(confirm.setStyle(confirm.getStyle().withColor(CommonColors.RED)));
                confirmed[0] = true;
                return;
            }

            callback.run();
        }).width(98).pos(x, y).build();
    }

    public static String nullSafe(String str) {
        return str == null ? "-" : str;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        EditBox commentField = addRenderableWidget(new EditBox(font, centerX - 10, 40, 150, 20, Component.empty()));
        EditBox screenClassField = addRenderableWidget(new EditBox(font, centerX - 10, 70, 150, 20, Component.empty()));
        EditBox elementClassField = addRenderableWidget(new EditBox(font, centerX - 10, 100, 150, 20, Component.empty()));
        commentField.setMaxLength(65535);
        screenClassField.setMaxLength(65535);
        elementClassField.setMaxLength(65535);
        commentField.setValue(elementRule.comment() == null ? "" : elementRule.comment());
        screenClassField.setValue(nullSafe(elementRule.screenClassName()));
        elementClassField.setValue(nullSafe(elementRule.elementClassName()));

        boolean[] shouldBlock = { elementRule.shouldBlock() };
        addRenderableWidget(Button.builder(getYesOrNoText(shouldBlock[0]), button -> {
            shouldBlock[0] = !shouldBlock[0];
            button.setMessage(getYesOrNoText(shouldBlock[0]));
        }).bounds(centerX - 10, 130, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), _ -> onClose()).width(98)
                .pos(centerX - 150, height - 30).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), _ -> {
            saveCallback.accept(new ElementRule(commentField.getValue(), screenClassField.getValue(), elementClassField.getValue(), shouldBlock[0]));
            onClose();
        }).width(98).pos(centerX - 50, height - 30).build());

        addRenderableWidget(createRemoveButton(centerX + 50, height - 30, () -> {
            removeCallback.run();
            onClose();
        })).active = elementRule.comment() != null /* Disable if is newly created */;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        super.extractRenderState(context, mouseX, mouseY, a);
        int centerX = width / 2;

        // Title
        context.centeredText(font, Component.translatable("fcitx5.screen.elementRuleEdit.title"), centerX, 20, CommonColors.WHITE);

        // Fields
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.comment"), centerX - 140, 48, CommonColors.WHITE);
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.screenClass"), centerX - 140, 78, CommonColors.WHITE);
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.elementClass"), centerX - 140, 108, CommonColors.WHITE);
        context.text(font, Component.translatable("fcitx5.screen.ruleEdit.item.shouldBlock"), centerX - 140, 138, CommonColors.WHITE);
    }

    @Override
    public void onClose() {
        Main.setScreen(parentScreen);
    }
}
