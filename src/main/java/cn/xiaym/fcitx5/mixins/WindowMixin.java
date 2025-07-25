package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Fcitx5DBus;
import cn.xiaym.fcitx5.Main;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 12106
//$$ import net.minecraft.client.WindowEventHandler;
//$$ import net.minecraft.client.WindowSettings;
//$$ import net.minecraft.client.util.MonitorTracker;
//$$ import org.lwjgl.glfw.GLFW;
//#endif

@Mixin(Window.class)
public class WindowMixin {
    @Shadow
    @Final
    private long handle;

    @Unique
    private static void switchToInitial() {
        switch (Main.initialState) {
            case Fcitx5DBus.STATE_INACTIVE -> {
                Fcitx5DBus.activate();
                Main.initialState = Fcitx5DBus.STATE_ACTIVE;
            }

            case Fcitx5DBus.STATE_ACTIVE -> {
                Fcitx5DBus.deactivate();
                Main.initialState = Fcitx5DBus.STATE_INACTIVE;
            }
        }
    }

    //#if MC > 12006
    @Inject(method = "onMinimizeChanged", at = @At("HEAD"))
    public void onMinimizeChanged(long window, boolean minimized, CallbackInfo ci) {
        if (window != handle || !Main.canFindDBus) {
            //#else
            //$$ @Inject(method = "<init>", at = @At("TAIL"))
            //$$ public void init(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
            //$$     GLFW.glfwSetWindowIconifyCallback(handle, (w, b) -> onMinimizeChanged(handle, w, b));
            //$$ }
            //$$
            //$$ @Unique
            //$$ public static void onMinimizeChanged(long mcHandle, long window, boolean minimized) {
            //$$ if (window != mcHandle || !Main.canFindDBus) {
            //#endif
            return;
        }

        if (!minimized) {
            Main.initialState = Fcitx5DBus.getState();
            return;
        }

        switchToInitial();
    }

    @Inject(method = "onWindowFocusChanged", at = @At("HEAD"))
    public void onWindowFocusChanged(long window, boolean focused, CallbackInfo ci) {
        if (window != handle || !Main.canFindDBus) {
            return;
        }

        if (focused) {
            Main.initialState = Fcitx5DBus.getState();
            return;
        }

        switchToInitial();
    }
}
