package cn.xiaym.fcitx5.screen;

import cn.xiaym.fcitx5.config.rules.ScreenRule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.function.Consumer;

import static cn.xiaym.fcitx5.screen.ElementRuleEditScreen.*;

public class ScreenRuleEditScreen extends Screen {
    private final Screen parentScreen;
    private final ScreenRule screenRule;
    private final Runnable removeCallback;
    private final Consumer<ScreenRule> saveCallback;

    public ScreenRuleEditScreen(Screen parent, ScreenRule screenRule, Runnable removeCallback, Consumer<ScreenRule> saveCallback) {
        super(Text.translatable("fcitx5.screen.screenRuleEdit.title"));
        this.parentScreen = parent;
        this.screenRule = screenRule;
        this.removeCallback = removeCallback;
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        TextFieldWidget commentField = addDrawableChild(new TextFieldWidget(textRenderer, centerX - 10, 40, 150, 20, null));
        TextFieldWidget screenClassField = addDrawableChild(new TextFieldWidget(textRenderer, centerX - 10, 70, 150, 20, null));
        commentField.setMaxLength(65535);
        screenClassField.setMaxLength(65535);
        commentField.setText(screenRule.comment() == null ? "" : screenRule.comment());
        screenClassField.setText(nullSafe(screenRule.screenClassName()));

        boolean[] shouldBlock = { screenRule.shouldBlock() };
        addDrawableChild(ButtonWidget.builder(getYesOrNoText(shouldBlock[0]), button -> {
            shouldBlock[0] = !shouldBlock[0];
            button.setMessage(getYesOrNoText(shouldBlock[0]));
        }).dimensions(centerX - 10, 100, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close()).width(98)
                .position(centerX - 150, height - 30).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            saveCallback.accept(new ScreenRule(commentField.getText(), screenClassField.getText(), shouldBlock[0]));
            close();
        }).width(98).position(centerX - 50, height - 30).build());

        addDrawableChild(createRemoveButton(centerX + 50, height - 30, () -> {
            removeCallback.run();
            close();
        })).active = screenRule.comment() != null /* Disable if is newly created */;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        //#if MC <= 12000
        //$$ this.renderBackground(context);
        //#endif

        super.render(context, mouseX, mouseY, deltaTicks);
        int centerX = width / 2;

        // Title
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.screenRuleEdit.title"), centerX, 20, Colors.WHITE);

        // Fields
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.comment"), centerX - 140, 48, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.screenClass"), centerX - 140, 78, Colors.WHITE);
        context.drawTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.ruleEdit.item.shouldBlock"), centerX - 140, 108, Colors.WHITE);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parentScreen);
        }
    }
}
