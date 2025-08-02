package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Fcitx5;
import cn.xiaym.fcitx5.Fcitx5Wayland;
import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.dbus.Fcitx5DBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.ChatScreen;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private static boolean afterInGame = false;
    @Unique
    private static boolean isWayland = false;
    @Shadow
    @Nullable
    public Screen currentScreen;
    @Shadow
    @Final
    private Window window;

    @Unique
    private static void checkBlocker(Screen screen) {
        if (!ModConfig.imBlockerEnabled) {
            return;
        }

        if (screen == null) {
            if (!afterInGame) {
                afterInGame = true;
            } else {
                return;
            }

            if (Main.wasSuppressed()) {
                Main.unsuppress();
                return;
            }

            Fcitx5DBus.getStateAsync().thenAcceptAsync(it -> {
                Main.initialState = it;

                if (it != Fcitx5DBus.STATE_INACTIVE) {
                    Fcitx5DBus.deactivate();
                }
            });

            return;
        }

        afterInGame = false;

        Fcitx5DBus.getStateAsync().thenAcceptAsync(it -> {
            if (Main.initialState == Fcitx5DBus.STATE_ACTIVE && it != Fcitx5DBus.STATE_ACTIVE && !Main.wasSuppressed()) {
                Fcitx5DBus.activate();
            }
        });
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
                if (currentScreen == null) {
                    return;
                }

                for (char c : text.toCharArray()) {
                    currentScreen.charTyped(c, 0);
                }
            };

            Fcitx5Wayland.onPreeditString = text -> Main.waylandPreedit = text;

            isWayland = true;
            onResolutionChanged(null);
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        if (isWayland) {
            Fcitx5Wayland.updateWindow(window.getWidth(), window.getHeight() + 50);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        checkBlocker(screen);

        boolean isChatScreen = screen instanceof ChatScreen;
        Main.chatScrOpening = isChatScreen;
        Main.allowToType = !isChatScreen;
    }

    @Inject(method = "openChatScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", ordinal = 1))
    public void openChatScreen(String text, CallbackInfo ci) {
        if (text == null || !text.startsWith("/")) {
            return;
        }

        if (ModConfig.imBlockerEnabled && ModConfig.builtinCommandSuppressDirect) {
            Main.suppress();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTickEnd(CallbackInfo ci) {
        if (!Main.selectingElement) {
            if (ModConfig.selectElementKey.matchesCurrentKey()) {
                Main.selectingElement = true;
            }
        } else {
            if (InputUtil.isKeyPressed(window.getHandle(), InputUtil.GLFW_KEY_ESCAPE)) {
                Main.selectingElement = false;
            }
        }
    }
}
