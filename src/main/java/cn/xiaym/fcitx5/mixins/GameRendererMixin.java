package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.config.ModConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;

//#if MC > 12105
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
//#else
//$$ import cn.xiaym.fcitx5.compat.legacy.Rect;
//$$ import net.minecraft.client.render.VertexConsumerProvider;

//#if MC >= 12101
//$$ import net.minecraft.client.util.BufferAllocator;
//#else
//$$ import net.minecraft.client.render.BufferBuilder;
//#endif

//#endif

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    //#if MC > 12105
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public void wrapRender(Screen instance, DrawContext context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        original.call(instance,  context,  mouseX, mouseY, deltaTicks);
        //#else
        //$$ @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
        //$$ public void wrapRender(Screen instance, DrawContext context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        //$$ GlobalState.simulateDrawing = false;
        //$$ original.call(instance, context, mouseX, mouseY, deltaTicks);
        //#endif
        TextRenderer textRenderer = client.textRenderer;
        if (GlobalState.waylandPreedit != null && ModConfig.nativeWaylandOverlayEnabled) {
            int width = textRenderer.getWidth(GlobalState.waylandPreedit);
            int x = ModConfig.nativeWaylandOverlayX, y = ModConfig.nativeWaylandOverlayY, padding = 3;

            context.fill(x - padding, y - padding, x + width + padding, y + 10 + padding, Colors.WHITE);
            context.drawText(textRenderer, GlobalState.waylandPreedit, x, y, Colors.BLACK, false);
        }

        if (!GlobalState.selectingElement || client.currentScreen == null) {
            return;
        }

        Optional<Element> elementOpt = client.currentScreen.hoveredElement(mouseX, mouseY);
        Element element;
        if (elementOpt.isEmpty() || !((GlobalState.selectedElement = element = elementOpt.get()) instanceof Drawable drawable)) {
            GlobalState.selectedElement = null;
            context.drawText(textRenderer, Text.translatable("fcitx5.selector.none"), 10, 10, Colors.WHITE, true);
            return;
        }

        context.drawText(textRenderer, Text.translatable("fcitx5.selector.selecting", element.getClass()
                .getSimpleName()), 10, 10, Colors.WHITE, true);

        //#if MC > 12105
        GuiRenderState state = new GuiRenderState();
        //#if MC >= 12111
        DrawContext vContext = new DrawContext(client, state, mouseX, mouseY);
        //#else
        //$$ DrawContext vContext = new DrawContext(client, state);
        //#endif
        drawable.render(vContext, mouseX, mouseY, deltaTicks);

        // Collect Elements
        List<ScreenRect> rectList = new ArrayList<>();
        state.forEachSimpleElement((simpleElementState
                                    //#if MC <= 12108
                                    //$$, depth
                                    //#endif
        ) -> rectList.add(simpleElementState.bounds()), GuiRenderState.LayerFilter.ALL);

        // Typically PressableTextElement
        if (rectList.isEmpty()) {
            state.forEachTextElement(textGuiElementRenderState -> rectList.add(textGuiElementRenderState.bounds()));
        }

        for (ScreenRect bounds : rectList) {
            if (bounds == null) {
                continue;
            }

            context.fill(bounds.getLeft(), bounds.getTop(), bounds.getRight(), bounds.getBottom(), ColorHelper.getArgb(50, 0, 0, 255));
        }
        //#else
        //$$ // Let DrawContextMixin take over this, then we'll receive `Rect`s
        //$$ GlobalState.simulateDrawing = true;

        //#if MC >= 12101
        //$$ DrawContext vContext = new DrawContext(client, VertexConsumerProvider.immediate(new BufferAllocator(786432)));
        //#else
        //$$ DrawContext vContext = new DrawContext(client, VertexConsumerProvider.immediate(new BufferBuilder(786432)));
        //#endif

        //$$ drawable.render(vContext, mouseX, mouseY, deltaTicks);
        //$$ GlobalState.simulateDrawing = false;
        //$$
        //$$ Iterator<Rect> it = GlobalState.simulatedRectSet.iterator();
        //$$ while (it.hasNext()) {
        //$$     Rect rect = it.next();
        //$$     context.fill(rect.x1(), rect.y1(), rect.x2(), rect.y2(),

        //#if MC >= 12104
        //$$         ColorHelper.getArgb(50, 0, 0, 255)
        //#else
        //$$         ColorHelper.Argb.getArgb(50, 0, 0, 255)
        //#endif

        //$$     );
        //$$     it.remove();
        //$$ }
        //#endif
    }
}