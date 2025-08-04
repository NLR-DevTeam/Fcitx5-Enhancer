package cn.xiaym.fcitx5.screen;

import cn.xiaym.fcitx5.config.rules.ElementRule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.function.Consumer;

public class ElementRuleEditScreen extends Screen {
    private final Screen parentScreen;
    private final ElementRule elementRule;
    private final Runnable removeCallback;
    private final Consumer<ElementRule> saveCallback;

    public ElementRuleEditScreen(Screen parent, ElementRule elementRule, Runnable removeCallback, Consumer<ElementRule> saveCallback) {
        super(Text.translatable("fcitx5.screen.elementRuleEdit.title"));
        this.parentScreen = parent;
        this.elementRule = elementRule;
        this.removeCallback = removeCallback;
        this.saveCallback = saveCallback;
    }

    public static Text getYesOrNoText(boolean b) {
        return Text.translatable("fcitx5.controls." + b).withColor(b ? 0xFF00FF00 /* GREEN */ : Colors.LIGHT_RED);
    }

    public static ButtonWidget createRemoveButton(int x, int y, Runnable callback) {
        boolean[] confirmed = { false };
        return ButtonWidget.builder(Text.translatable("fcitx5.controls.remove")
                .withColor(Colors.LIGHT_RED), button -> {
            if (!confirmed[0]) {
                button.setMessage(Text.translatable("fcitx5.controls.remove.confirm").withColor(Colors.RED));
                confirmed[0] = true;
                return;
            }

            callback.run();
        }).width(98).position(x, y).build();
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        TextFieldWidget commentField = addDrawableChild(new TextFieldWidget(textRenderer, centerX - 10, 40, 150, 20, null));
        TextFieldWidget screenClassField = addDrawableChild(new TextFieldWidget(textRenderer, centerX - 10, 70, 150, 20, null));
        TextFieldWidget elementClassField = addDrawableChild(new TextFieldWidget(textRenderer, centerX - 10, 100, 150, 20, null));
        commentField.setText(elementRule.comment);
        screenClassField.setText(elementRule.screenClassName);
        elementClassField.setText(elementRule.elementClassName);

        boolean[] shouldBlock = { elementRule.shouldBlock };
        addDrawableChild(ButtonWidget.builder(getYesOrNoText(shouldBlock[0]), button -> {
            shouldBlock[0] = !shouldBlock[0];
            button.setMessage(getYesOrNoText(shouldBlock[0]));
        }).dimensions(centerX - 10, 130, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close()).width(98)
                .position(centerX - 150, height - 30).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            saveCallback.accept(new ElementRule(commentField.getText(), screenClassField.getText(), elementClassField.getText(), shouldBlock[0]));
            close();
        }).width(98).position(centerX - 50, height - 30).build());

        addDrawableChild(createRemoveButton(centerX + 50, height - 30, () -> {
            removeCallback.run();
            close();
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        int centerX = width / 2;

        // Title
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.elementRuleEdit.title"), centerX, 20, Colors.WHITE);

        // Fields
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.comment"), centerX - 140, 48, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.screenClass"), centerX - 140, 78, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.elementClass"), centerX - 140, 108, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.shouldBlock"), centerX - 140, 138, Colors.WHITE);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parentScreen);
        }
    }
}
