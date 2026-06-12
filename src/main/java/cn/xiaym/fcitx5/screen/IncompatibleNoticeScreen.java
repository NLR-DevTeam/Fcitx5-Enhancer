package cn.xiaym.fcitx5.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;

public class IncompatibleNoticeScreen extends Screen {
    private final Screen parentScreen;

    public IncompatibleNoticeScreen(Screen parent) {
        super(Component.translatable("fcitx5.screen.incompatibleNotice.title"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), _ -> onClose())
                .pos(width / 2 - 75, height / 2 + 25).build());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        super.extractRenderState(context, mouseX, mouseY, a);

        int centerX = width / 2;
        int centerY = height / 2;
        context.centeredText(font, Component.translatable("fcitx5.screen.incompatibleNotice.title")
                .withStyle(ChatFormatting.BOLD), centerX, centerY - 20, CommonColors.WHITE);
        context.centeredText(font, Component.translatable("fcitx5.screen.incompatibleNotice.content"), centerX, centerY, CommonColors.WHITE);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }
}
