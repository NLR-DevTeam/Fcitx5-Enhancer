package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.dbus.Fcitx5DBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private static boolean afterInGame = false;

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
}
