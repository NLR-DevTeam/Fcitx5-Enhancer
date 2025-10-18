package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Fcitx5;
import cn.xiaym.fcitx5.Main;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 12110
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
//#endif

/**
 * This mixin intercepts key events when the user is typing using Fcitx 5,
 * which avoids unexpected screen closing or interaction.
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Unique
    private static final long HANDLE = MinecraftClient.getInstance().getWindow().getHandle();

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

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    //#if MC >= 12110
    public void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        int key = input.key();
        //#else
        //$$ public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        //#endif
        Main.allowToType = true;
        if (window != HANDLE) {
            return;
        }

        if (Main.selectingElement) {
            ci.cancel();
            return;
        }

        if (keyShouldBeIntercepted(key) && Fcitx5.userTyping()) {
            ci.cancel();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    //#if MC >= 12110
    public void onChar(long window, CharInput input, CallbackInfo ci) {
        //#else
        //$$ public void onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        //#endif
        if (window == HANDLE && Main.selectingElement) {
            ci.cancel();
        }
    }
}
