package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.dbus.Fcitx5DBus;
import net.minecraft.client.MinecraftClient;
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
    private static void commandLaterDisable() {
        if (!ModConfig.imBlockerEnabled || !ModConfig.builtinCommandDisableLater) {
            return;
        }

        Fcitx5DBus.getStateAsync().thenAcceptAsync(it -> {
            if (it != Fcitx5DBus.STATE_INACTIVE) {
                Fcitx5DBus.deactivate();
                Main.suppress();
            }
        });
    }

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void onWrite(String text, CallbackInfo ci) {
        if (Main.chatScrOpening && !Main.allowToType) {
            ci.cancel();
        }

        if ("/".equals(text) && this.text.isEmpty()) {
            commandLaterDisable();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(method = "setText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;onChanged(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void onSetText(String text, CallbackInfo ci) {
        if (!Main.chatScrOpening || !Objects.requireNonNull(MinecraftClient.getInstance().currentScreen).children()
                .contains(this) || text == null || !text.startsWith("/")) {
            return;
        }

        commandLaterDisable();
    }
}
