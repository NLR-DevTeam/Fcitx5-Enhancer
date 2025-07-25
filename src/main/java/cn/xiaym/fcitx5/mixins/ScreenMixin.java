package cn.xiaym.fcitx5.mixins;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {
//    @Shadow
//    public abstract List<? extends Element> children();
//
//    @Inject(method = "render", at = @At("RETURN"))
//    private void init(CallbackInfo ci) {
//        System.out.println(children());
//    }
}
