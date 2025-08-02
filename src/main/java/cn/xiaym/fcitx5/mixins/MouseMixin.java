package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;

@Mixin(Mouse.class)
public class MouseMixin {
    @Unique
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    @Unique
    private static final long HANDLE = CLIENT.getWindow().getHandle();

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    public void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window != HANDLE || !Main.selectingElement) {
            return;
        }

        ci.cancel();

        switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                if (Main.selectedElement == null) {
                    break;
                }

                System.out.println("Selecting element: " + Main.selectedElement);
            }

            case  GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                assert CLIENT.currentScreen != null;
                System.out.println("Selecting screen: " + CLIENT.currentScreen);
            }

            default -> {
                return;
            }
        }

        Main.selectingElement = false;
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == HANDLE && Main.selectingElement) {
            ci.cancel();
        }
    }

    @Inject(method = "onFilesDropped", at = @At("HEAD"), cancellable = true)
    public void onFilesDropped(long window, List<Path> paths, int invalidFilesCount, CallbackInfo ci) {
        if (window == HANDLE && Main.selectingElement) {
            ci.cancel();
        }
    }
}
