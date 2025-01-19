package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method =
            //#if MC > 11605
            "setScreen"
            //#else
            //$$ "openScreen"
            //#endif
            , at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        boolean isChatScreen = screen instanceof ChatScreen;
        Main.chatScrOpening = isChatScreen;
        Main.allowToType = !isChatScreen;
    }
}
