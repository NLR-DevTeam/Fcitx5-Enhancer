package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class intercepts duplicated input events (using Main#allowToType),
 * which avoids 't' or '//' appearing in the input box unexpectedly.
 */
@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {
    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void onWrite(String text, CallbackInfo ci) {
        if (Main.chatScrOpening && !Main.allowToType) {
            ci.cancel();
        }
    }
}
