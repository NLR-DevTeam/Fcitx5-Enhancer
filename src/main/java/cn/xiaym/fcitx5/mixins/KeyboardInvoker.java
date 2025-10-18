package cn.xiaym.fcitx5.mixins;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if MC >= 12110
import net.minecraft.client.input.CharInput;
//#endif

@Mixin(Keyboard.class)
public interface KeyboardInvoker {
    @Invoker
    void invokeOnChar(long window,
                      //#if MC >= 12110
                      CharInput charInput
                      //#else
                      //$$int codePoint, int modifiers
                      //#endif
    );
}
