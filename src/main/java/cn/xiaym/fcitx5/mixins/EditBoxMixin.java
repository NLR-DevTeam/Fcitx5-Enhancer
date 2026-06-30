package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.IMBlockerListener;
import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
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
@Mixin(EditBox.class)
public class EditBoxMixin {
    @Shadow
    private String value;

    @Unique
    private static void disableOnCommand() {
        if (!ModConfig.imBlockerEnabled || !ModConfig.builtinCommandDisableLater) {
            return;
        }

        IMBlockerListener.dispatchAsync(IMBlockerListener::tryDeactivate);
    }

    @Inject(method = "insertText", at = @At("HEAD"), cancellable = true)
    private void onWrite(String text, CallbackInfo ci) {
        if (GlobalState.newScrOpening && !GlobalState.allowToType) {
            ci.cancel();
        }

        if ("/".equals(text) && this.value.isEmpty()) {
            disableOnCommand();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Inject(method = "setValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;onValueChange(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void onSetText(String text, CallbackInfo ci) {
        Screen screen = Objects.requireNonNull(Main.getScreen());
        if (!GlobalState.newScrOpening || !(screen instanceof ChatScreen) || !screen.children()
                .contains(this) || text == null || !text.startsWith("/")) {
            return;
        }

        disableOnCommand();
    }
}
