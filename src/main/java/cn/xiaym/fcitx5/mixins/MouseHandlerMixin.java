package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.GlobalState;
import cn.xiaym.fcitx5.IMBlockerListener;
import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.config.rules.ScreenRule;
import cn.xiaym.fcitx5.screen.ElementRuleEditScreen;
import cn.xiaym.fcitx5.screen.ScreenRuleEditScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Unique
    private static final long HANDLE = Minecraft.getInstance().getWindow().handle();
    @Unique
    private static final SystemToast.SystemToastId TOAST_TYPE = new SystemToast.SystemToastId(3000);

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    public void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        Screen screen = Main.getScreen();
        if (window != HANDLE || screen == null || !GlobalState.selectingElement) {
            return;
        }

        ci.cancel();

        switch (input.button()) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                if (GlobalState.selectedElement == null) {
                    return;
                }

                ElementRule rule = ModConfig.getOrCreateElementRule(screen.getClass()
                        .getName(), GlobalState.selectedElement.getClass().getName());
                SystemToast.add(Main.getToastManager(), TOAST_TYPE, Component.translatable("fcitx5.selector.completed"), Component.translatable(rule.comment() == null ? "fcitx5.selector.new" : "fcitx5.selector.existing"));

                Main.setScreen(new ElementRuleEditScreen(screen, rule, () -> ModConfig.userElementRules.remove(rule), newValue -> {
                    ModConfig.updateList(ModConfig.userElementRules, rule, newValue);
                    ModConfig.saveConfig();
                }));
            }

            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                ScreenRule rule = ModConfig.getOrCreateScreenRule(screen.getClass().getName());
                SystemToast.add(Main.getToastManager(), TOAST_TYPE, Component.translatable("fcitx5.selector.completed"), Component.translatable(rule.comment() == null ? "fcitx5.selector.new" : "fcitx5.selector.existing"));

                Main.setScreen(new ScreenRuleEditScreen(screen, rule, () -> ModConfig.userScreenRules.remove(rule), newValue -> {
                    ModConfig.updateList(ModConfig.userScreenRules, rule, newValue);
                    ModConfig.saveConfig();
                }));
            }

            default -> {
                return;
            }
        }

        GlobalState.selectingElement = false;
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    public boolean onMouseClickedElement(Screen instance, MouseButtonEvent click, boolean b, Operation<Boolean> original) {
        boolean originalResult = original.call(instance, click, b);
        Screen screen = Main.getScreen();
        if (screen == null) {
            return originalResult;
        }

        Optional<GuiEventListener> hoveredOpt = instance.getChildAt(click.x(), click.y());
        IMBlockerListener.onElementFocus(hoveredOpt.orElse(null), screen);

        return hoveredOpt.isPresent();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == HANDLE && GlobalState.selectingElement) {
            ci.cancel();
        }
    }

    @Inject(method = "onDrop", at = @At("HEAD"), cancellable = true)
    public void onFilesDropped(long window, List<Path> paths, int invalidFilesCount, CallbackInfo ci) {
        if (window == HANDLE && GlobalState.selectingElement) {
            ci.cancel();
        }
    }
}
