package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.IMBlockerListener;
import cn.xiaym.fcitx5.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * This class intercepts duplicated input events (using Main#allowToType),
 * which avoids 't' or '//' appearing in the input box unexpectedly.
 */
@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {
    @Shadow
    private String text;

    @Unique
    private static void disableOnCommand() {
        if (!ModConfig.imBlockerEnabled || !ModConfig.builtinCommandDisableLater) {
            return;
        }

        IMBlockerListener.dispatchAsync(IMBlockerListener::tryDeactivate);
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void onWrite(String text, CallbackInfo ci) {
        if (GlobalState.newScrOpening && !GlobalState.allowToType) {
            ci.cancel();
        }

        if ("/".equals(text) && this.text.isEmpty()) {
            disableOnCommand();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(method = "setText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;onChanged(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void onSetText(String text, CallbackInfo ci) {
        Screen screen = Objects.requireNonNull(MinecraftClient.getInstance().currentScreen);
        if (!GlobalState.newScrOpening || !(screen instanceof ChatScreen) || !screen.children().contains(this) || text == null || !text.startsWith("/")) {
            return;
        }

        disableOnCommand();
    }
}
