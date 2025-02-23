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

/**
 * This mixin intercepts key events when the user is typing using Fcitx 5,
 * which avoids unexpected screen closing or interaction.
 */
@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Unique
    private static final MinecraftClient INSTANCE = MinecraftClient.getInstance();

    @Unique
    private static final long HANDLE = INSTANCE.getWindow().getHandle();

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
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        Main.allowToType = true;

        if (window != HANDLE || !keyShouldBeIntercepted(key)) {
            return;
        }

        if (Fcitx5.userTyping()) {
            ci.cancel();
        }
    }
}
