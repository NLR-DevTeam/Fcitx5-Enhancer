package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.IMBlockerListener;
import cn.xiaym.fcitx5.config.ModConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        //#if MC >= 260200
        Gui.class
        //#else
        //$$ net.minecraft.client.Minecraft.class
        //#endif
)
public class GuiMixin {
    @Unique
    private static void checkBlocker(Screen screen, Screen prevScreen) {
        if (!ModConfig.imBlockerEnabled) {
            return;
        }

        IMBlockerListener.onScreenOpen(screen, prevScreen);
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo ci) {
        checkBlocker(screen, screen);

        boolean screenOpening = screen != null;
        GlobalState.newScrOpening = screenOpening;
        GlobalState.allowToType = !screenOpening;
    }

    @Inject(method = "openChatScreen", at = @At(value = "INVOKE", target =
            //#if MC >= 260200
            "Lnet/minecraft/client/gui/components/ChatComponent;openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V"
            //#else
            //$$ "Lnet/minecraft/client/gui/Gui;getChat()Lnet/minecraft/client/gui/components/ChatComponent;"
            //#endif
    ))
    public void openChatScreen(ChatComponent.ChatMethod method, CallbackInfo ci) {
        String text = method.prefix();
        if (!text.startsWith("/")) {
            return;
        }

        if (ModConfig.imBlockerEnabled && ModConfig.builtinCommandSuppressDirect) {
            IMBlockerListener.suppress();
        }
    }
}
