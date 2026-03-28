package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.*;
import cn.xiaym.fcitx5.config.ModConfig;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWayland;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 12110
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.hud.ChatHud;
//#endif

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private static boolean isWayland = false;
    @Shadow
    @Final
    public Keyboard keyboard;
    @Shadow
    @Nullable
    public Screen currentScreen;
    @Shadow
    @Final
    private Window window;

    @Unique
    private static void checkBlocker(Screen screen, Screen prevScreen) {
        if (!ModConfig.imBlockerEnabled) {
            return;
        }

        IMBlockerListener.onScreenOpen(screen, prevScreen);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(RunArgs args, CallbackInfo ci) {
        if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) {
            Fcitx5.LOGGER.info("Wayland detected - loading wayland support library...");
            Throwable throwable = Main.tryLoadLibrary("libwayland_support.so");
            if (throwable != null) {
                Fcitx5.LOGGER.warn("Can't load wayland support library, relevant functionalities will be disabled!", throwable);
                return;
            }

            Fcitx5Wayland.initialize(GLFWNativeWayland.glfwGetWaylandDisplay());
            Fcitx5Wayland.onCommitString = text -> {
                long handle = window.getHandle();
                for (int i = 0, len = text.length(); i < len; i++) {
                    int codePoint = Character.codePointAt(text, i);
                    ((KeyboardInvoker) keyboard).invokeOnChar(handle,
                            //#if MC >= 12110
                            new CharInput(codePoint, 0)
                            //#else
                            //$$ codePoint, 0
                            //#endif
                    );
                }
            };

            Fcitx5Wayland.onPreeditString = text -> GlobalState.waylandPreedit = text;

            isWayland = true;
            onResolutionChanged(null);
        }

        GlobalState.gameInitialized = true;
    }

    @Inject(method = "onResolutionChanged", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        if (isWayland) {
            Fcitx5Wayland.updateWindow(window.getWidth(), window.getHeight() + 50);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        checkBlocker(screen, currentScreen);

        boolean screenOpening = screen != null;
        GlobalState.newScrOpening = screenOpening;
        GlobalState.allowToType = !screenOpening;
    }

    //#if MC >= 12110
    @Inject(method = "openChatScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;getChatHud()Lnet/minecraft/client/gui/hud/ChatHud;"))
    public void openChatScreen(ChatHud.ChatMethod method, CallbackInfo ci) {
        String text = method.getReplacement();
        //#else
        //$$ @Inject(method = "openChatScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 1))
        //$$ public void openChatScreen(String text, CallbackInfo ci) {
        //#endif
        if (text == null || !text.startsWith("/")) {
            return;
        }

        if (ModConfig.imBlockerEnabled && ModConfig.builtinCommandSuppressDirect) {
            IMBlockerListener.suppress();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTickEnd(CallbackInfo ci) {
        if (!GlobalState.selectingElement) {
            if (ModConfig.selectElementKey.matchesCurrentKey() && currentScreen != null) {
                GlobalState.selectingElement = true;
            }
        } else {
            if (InputUtil.isKeyPressed(
                    //#if MC >= 12110
                    window,
                    //#else
                    //$$ window.getHandle(),
                    //#endif
                    InputUtil.GLFW_KEY_ESCAPE)) {
                GlobalState.selectingElement = false;
            }
        }
    }
}
