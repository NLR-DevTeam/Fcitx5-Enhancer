package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Fcitx5;
import cn.xiaym.fcitx5.GlobalState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
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
            GLFW.GLFW_KEY_BACKSPACE,
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER,
            GLFW.GLFW_KEY_TAB,
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT
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

        if (keyShouldBeIntercepted(key) && Fcitx5.userTyping()) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    public void onChar(long window, CharacterEvent input, CallbackInfo ci) {
        if (window == HANDLE && GlobalState.selectingElement) {
            ci.cancel();
        }
    }
}
