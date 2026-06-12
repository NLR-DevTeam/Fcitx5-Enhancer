package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.config.ModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    public void wrapRender(Screen instance, GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        original.call(instance, context, mouseX, mouseY, deltaTicks);
        Font textRenderer = minecraft.font;
        if (GlobalState.waylandPreedit != null && ModConfig.nativeWaylandOverlayEnabled) {
            int width = textRenderer.width(GlobalState.waylandPreedit);
            int x = ModConfig.nativeWaylandOverlayX, y = ModConfig.nativeWaylandOverlayY, padding = 3;

            context.fill(x - padding, y - padding, x + width + padding, y + 10 + padding, CommonColors.WHITE);
            context.text(textRenderer, GlobalState.waylandPreedit, x, y, CommonColors.BLACK, false);
        }

        if (!GlobalState.selectingElement || minecraft.screen == null) {
            return;
        }

        Optional<GuiEventListener> elementOpt = minecraft.screen.getChildAt(mouseX, mouseY);
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