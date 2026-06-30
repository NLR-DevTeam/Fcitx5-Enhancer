package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.config.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    //#if MC >= 260200
    @Shadow
    @Final
    public Gui gui;
    //#else
    //$$ @Shadow
    //$$ @org.jetbrains.annotations.Nullable
    //$$ public net.minecraft.client.gui.screens.Screen screen;
    //#endif

    @Shadow
    @Final
    private Window window;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(GameConfig args, CallbackInfo ci) {
        GlobalState.gameInitialized = true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTickEnd(CallbackInfo ci) {
        if (!GlobalState.selectingElement) {
            if (ModConfig.selectElementKey.matchesCurrentKey() &&
                    //#if MC >= 260200
                    gui.screen() != null
                //#else
                //$$ screen != null
                //#endif
            ) {
                GlobalState.selectingElement = true;
            }
        } else {
            if (InputConstants.isKeyDown(window, InputConstants.KEY_ESCAPE)) {
                GlobalState.selectingElement = false;
            }
        }
    }
}
