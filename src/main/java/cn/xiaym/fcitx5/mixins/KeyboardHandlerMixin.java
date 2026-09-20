package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Fcitx5;
import cn.xiaym.fcitx5.GlobalState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin intercepts key events when the user is typing using Fcitx 5,
 * which avoids unexpected screen closing or interaction.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Unique
    private static final long HANDLE = Minecraft.getInstance().getWindow().handle();

    @Unique
    private static final int[] PREVENT_KEYS = {
            InputConstants.KEY_BACKSPACE,
            InputConstants.KEY_ESCAPE, InputConstants.KEY_RETURN,
            InputConstants.KEY_TAB,
            InputConstants.KEY_UP, InputConstants.KEY_DOWN,
            InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT
    };

    @Unique
    private static boolean keyShouldBeIntercepted(int keyCode) {
        for (int key : PREVENT_KEYS) {
            if (key == keyCode) {
                return true;
            }
        }

        return false;
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        int key = input.key();
        GlobalState.allowToType = true;
        if (window != HANDLE) {
            return;
        }

        if (GlobalState.selectingElement) {
            ci.cancel();
            return;
        }

        if (keyShouldBeIntercepted(key)) {
            if (Fcitx5.shouldInterceptPreventKey()) {
                ci.cancel();
            }
        } else {
            Fcitx5.noteUserInput();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    public void onChar(long window, CharacterEvent input, CallbackInfo ci) {
        if (window == HANDLE) {
            if (GlobalState.selectingElement) {
                ci.cancel();
            } else {
                Fcitx5.noteUserInput();
            }
        }
    }
}
