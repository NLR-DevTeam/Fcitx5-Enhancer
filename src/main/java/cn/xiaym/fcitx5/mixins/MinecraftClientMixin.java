package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.dbus.Fcitx5DBus;
import cn.xiaym.fcitx5.Main;
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

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            if (!afterInGame) {
                afterInGame = true;
                Fcitx5DBus.getStateAsync().thenAcceptAsync(it -> Main.initialState = it);
            }

            Fcitx5DBus.deactivate();
        } else {
            afterInGame = false;

            if (Main.initialState == Fcitx5DBus.STATE_ACTIVE) {
                Fcitx5DBus.activate();
            }
        }

        boolean isChatScreen = screen instanceof ChatScreen;
        Main.chatScrOpening = isChatScreen;
        Main.allowToType = !isChatScreen;
    }
}
