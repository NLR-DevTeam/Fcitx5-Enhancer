package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Colors;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;

//#if MC > 12105
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
//$$ import cn.xiaym.fcitx5.compat.legacy.Rect;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//#if MC >= 12105
//$$ import net.minecraft.client.util.BufferAllocator;
//#else
//$$ import net.minecraft.client.render.BufferBuilder;
//#endif
//$$ import net.minecraft.client.render.VertexConsumerProvider;
//$$ import net.minecraft.client.gui.screen.Screen;
//#endif

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    //#if MC > 12105
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    public void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context, @Local(name = "i") int mouseX, @Local(name = "j") int mouseY) {
        //#else
        //$$ @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
        //$$ public void wrapRender(Screen instance, DrawContext context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        //$$ Main.simulateDrawing = false;
        //$$ original.call(instance, context, mouseX, mouseY, deltaTicks);
        //#endif
        if (!Main.selectingElement || client.currentScreen == null) {
            return;
        }

        Optional<Element> elementOpt = client.currentScreen.hoveredElement(mouseX, mouseY);
        if (elementOpt.isEmpty()) {
            return;
        }

        Element element = elementOpt.get();
        if (!(element instanceof Drawable drawable)) {
            return;
        }

        context.drawText(MinecraftClient.getInstance().textRenderer, "Selecting: " + drawable.getClass()
                .getName(), 10, 10, Colors.WHITE, true);

        //#if MC > 12105
        GuiRenderState state = new GuiRenderState();
        DrawContext vContext = new DrawContext(MinecraftClient.getInstance(), state);
        drawable.render(vContext, mouseX, mouseY, tickCounter.getDynamicDeltaTicks());

        // Collect Elements
        List<ScreenRect> rectList = new ArrayList<>();
        state.forEachSimpleElement((simpleElementState, depth) -> rectList.add(simpleElementState.bounds()), GuiRenderState.LayerFilter.ALL);

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
        //$$ Main.simulateDrawing = true;
        //#if MC >= 12105
        //$$ DrawContext vContext = new DrawContext(client, VertexConsumerProvider.immediate(new BufferAllocator(786432)));
        //#else
        //$$ DrawContext vContext = new DrawContext(client, VertexConsumerProvider.immediate(new BufferBuilder(786432)));
        //#endif
        //$$ drawable.render(vContext, mouseX, mouseY, deltaTicks);
        //$$ Main.simulateDrawing = false;
        //$$
        //$$ Iterator<Rect> it = Main.simulatedRectSet.iterator();
        //$$ while (it.hasNext()) {
        //$$     Rect rect = it.next();
        //$$     context.fill(rect.x1(), rect.y1(), rect.x2(), rect.y2(),
        //#if MC >= 12105
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