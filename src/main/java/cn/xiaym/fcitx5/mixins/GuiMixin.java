package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Fcitx5;
import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.IMBlockerListener;
import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    private @Nullable Screen screen;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private static void checkBlocker(Screen screen, Screen prevScreen) {
        if (!ModConfig.imBlockerEnabled) {
            return;
        }

        IMBlockerListener.onScreenOpen(screen, prevScreen);
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        Fcitx5.disarmEndKeyGrace();
        checkBlocker(screen, this.screen);

        boolean screenOpening = screen != null;
        GlobalState.newScrOpening = screenOpening;
        GlobalState.allowToType = !screenOpening;

        GlobalState.selectingElement = false;
    }

    @Inject(method = "openChatScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V"))
    public void openChatScreen(ChatComponent.ChatMethod method, CallbackInfo ci) {
        String text = method.prefix();
        if (!text.startsWith("/")) {
            return;
        }

        if (ModConfig.imBlockerEnabled && ModConfig.builtinCommandSuppressDirect) {
            IMBlockerListener.suppress();
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    public void wrapRender(Screen instance, GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        original.call(instance, context, mouseX, mouseY, deltaTicks);
        Font textRenderer = minecraft.font;

        Screen screen = Main.getScreen();
        if (!GlobalState.selectingElement || screen == null) {
            return;
        }

        Optional<GuiEventListener> elementOpt = screen.getChildAt(mouseX, mouseY);
        GuiEventListener element;
        if (elementOpt.isEmpty() || !((GlobalState.selectedElement = element = elementOpt.get()) instanceof Renderable drawable)) {
            GlobalState.selectedElement = null;
            context.text(textRenderer, Component.translatable("fcitx5.selector.none"), 10, 10, CommonColors.WHITE, true);
            return;
        }

        context.text(textRenderer, Component.translatable("fcitx5.selector.selecting", element.getClass()
                .getSimpleName()), 10, 10, CommonColors.WHITE, true);

        GuiRenderState state = new GuiRenderState();
        GuiGraphicsExtractor vContext = new GuiGraphicsExtractor(minecraft, state, mouseX, mouseY);
        drawable.extractRenderState(vContext, mouseX, mouseY, deltaTicks);

        // Collect Elements
        List<ScreenRectangle> rectList = new ArrayList<>();
        state.forEachElement((simpleElementState) -> rectList.add(simpleElementState.bounds()), GuiRenderState.TraverseRange.ALL);

        // Typically PressableTextElement
        if (rectList.isEmpty()) {
            state.forEachText(textGuiElementRenderState -> rectList.add(textGuiElementRenderState.bounds()));
        }

        for (ScreenRectangle bounds : rectList) {
            if (bounds == null) {
                continue;
            }

            context.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), ARGB.color(50, 0, 0, 255));
        }
    }
}
