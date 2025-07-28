package cn.xiaym.fcitx5.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

public class IncompatibleNoticeScreen extends Screen {
    private final Screen parentScreen;

    public IncompatibleNoticeScreen(Screen parent) {
        super(Text.translatable("fcitx5.screen.incompatibleNotice.title"));
        parentScreen = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> close())
                .position(width / 2 - 75, height / 2 + 25).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int centerX = width / 2;
        int centerY = height / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.incompatibleNotice.title")
                .formatted(Formatting.BOLD), centerX, centerY - 20, Colors.WHITE);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("fcitx5.screen.incompatibleNotice.content"), centerX, centerY, Colors.WHITE);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parentScreen);
        }
    }
}
