package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.*;
import cn.xiaym.fcitx5.config.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.main.GameConfig;
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

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private static boolean isWayland = false;
    @Shadow
    @Final
    public KeyboardHandler keyboardHandler;
    @Shadow
    @Nullable
    public Screen screen;
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
    private void onInit(GameConfig args, CallbackInfo ci) {
        if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND) {
            Fcitx5.LOGGER.info("Wayland detected - loading wayland support library...");
            Throwable throwable = Main.tryLoadLibrary("libwayland_support.so");
            if (throwable != null) {
                Fcitx5.LOGGER.warn("Can't load wayland support library, relevant functionalities will be disabled!", throwable);
                return;
            }

            Fcitx5Wayland.initialize(GLFWNativeWayland.glfwGetWaylandDisplay());
            Fcitx5Wayland.onCommitString = text -> {
                long handle = window.handle();
                for (int i = 0, len = text.length(); i < len; i++) {
                    int codePoint = Character.codePointAt(text, i);
                    ((KeyboardHandlerInvoker) keyboardHandler).invokeCharTyped(handle, new CharacterEvent(codePoint));
                }
            };

            Fcitx5Wayland.onPreeditString = text -> GlobalState.waylandPreedit = text;

            isWayland = true;
            onResolutionChanged(null);
        }

        GlobalState.gameInitialized = true;
    }

    @Inject(method = "resizeGui", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        if (isWayland) {
            Fcitx5Wayland.updateWindow(window.getScreenWidth(), window.getScreenHeight() + 50);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        checkBlocker(screen, screen);

        boolean screenOpening = screen != null;
        GlobalState.newScrOpening = screenOpening;
        GlobalState.allowToType = !screenOpening;
    }

    @Inject(method = "openChatScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getChat()Lnet/minecraft/client/gui/components/ChatComponent;"))
    public void openChatScreen(ChatComponent.ChatMethod method, CallbackInfo ci) {
        String text = method.prefix();
        if (!text.startsWith("/")) {
            return;
        }

        if (ModConfig.imBlockerEnabled && ModConfig.builtinCommandSuppressDirect) {
            IMBlockerListener.suppress();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTickEnd(CallbackInfo ci) {
        if (!GlobalState.selectingElement) {
            if (ModConfig.selectElementKey.matchesCurrentKey() && screen != null) {
                GlobalState.selectingElement = true;
            }
        } else {
            if (InputConstants.isKeyDown(window, InputConstants.KEY_ESCAPE)) {
                GlobalState.selectingElement = false;
            }
        }
    }
}
