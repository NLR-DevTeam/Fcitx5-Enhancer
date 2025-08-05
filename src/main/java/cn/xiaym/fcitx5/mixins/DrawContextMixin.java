package cn.xiaym.fcitx5.mixins;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;

//#if MC <= 12105
//$$ import cn.xiaym.fcitx5.Main;
//$$ import cn.xiaym.fcitx5.compat.legacy.Rect;
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.client.render.RenderLayer;
//$$ import net.minecraft.client.render.VertexConsumer;
//$$ import net.minecraft.text.OrderedText;
//$$ import net.minecraft.util.Identifier;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC >= 12105
//$$ import java.util.function.Function;
//#endif
//#endif

@Mixin(DrawContext.class)
public class DrawContextMixin {
    //#if MC <= 12105
    //$$ @Inject(method = "draw()V", at = @At("HEAD"), cancellable = true)
    //$$ public void onDraw(CallbackInfo ci) {
    //$$     if (Main.simulateDrawing) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "fill(Lnet/minecraft/client/render/RenderLayer;IIIIII)V", at = @At("HEAD"), cancellable = true)
    //$$ public void onFill(RenderLayer layer, int x1, int y1, int x2, int y2, int z, int color, CallbackInfo ci) {
    //$$     if (Main.simulateDrawing) {
    //$$         if (x1 < x2) {
    //$$             int i = x1;
    //$$             x1 = x2;
    //$$             x2 = i;
    //$$         }
    //$$
    //$$         if (y1 < y2) {
    //$$             int i = y1;
    //$$             y1 = y2;
    //$$             y2 = i;
    //$$         }
    //$$
    //$$         Main.simulatedRectSet.add(new Rect(x1, y1, x2, y2));
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "fillGradient(Lnet/minecraft/client/render/VertexConsumer;IIIIIII)V", at = @At("HEAD"), cancellable = true)
    //$$ public void onFillGradient(VertexConsumer vertexConsumer, int startX, int startY, int endX, int endY, int z, int colorStart, int colorEnd, CallbackInfo ci) {
    //$$     if (Main.simulateDrawing) {
    //$$         Main.simulatedRectSet.add(new Rect(startX, startY, endX, endY));
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //#if MC < 12105
    //$$ @Inject(method = "drawTexturedQuad(Lnet/minecraft/util/Identifier;IIIIIFFFF)V", at = @At("HEAD"), cancellable = true)
    //$$ public void onDrawTexturedQuad1(Identifier texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, CallbackInfo ci) {
    //$$     if (Main.simulateDrawing) {
    //$$         Main.simulatedRectSet.add(new Rect(x1, y1, x2, y2));
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "drawTexturedQuad(Lnet/minecraft/util/Identifier;IIIIIFFFFFFFF)V", at = @At("HEAD"), cancellable = true)
    //$$ public void onDrawTexturedQuad2(Identifier texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha, CallbackInfo ci) {
    //$$     if (Main.simulateDrawing) {
    //$$         Main.simulatedRectSet.add(new Rect(x1, y1, x2, y2));
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#else
    //$$ @Inject(method = "drawTexturedQuad(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIIIFFFFI)V", at = @At("HEAD"), cancellable = true)
    //$$ public void onDrawTexturedQuad_12105(Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x1, int x2, int y1, int y2, float u1, float u2, float v1, float v2, int color, CallbackInfo ci) {
    //$$     if (Main.simulateDrawing) {
    //$$         Main.simulatedRectSet.add(new Rect(x1, y1, x2, y2));
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
    //$$
    //$$ @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"), cancellable = true)
    //$$ public void onDrawText1(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
    //$$     if (Main.simulateDrawing) {
    //$$         int width = textRenderer.getWidth(text);
    //$$         int height = 9;
    //$$
    //$$         Main.simulatedRectSet.add(new Rect(x, y, x + width, y + height));
    //$$         cir.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    //$$ public void onDrawText2(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
    //$$     if (Main.simulateDrawing) {
    //$$         int width = textRenderer.getWidth(text);
    //$$         int height = 9;
    //$$
    //$$         Main.simulatedRectSet.add(new Rect(x, y, x + width, y + height));
    //$$         cir.cancel();
    //$$     }
    //$$ }
    //#endif
}
