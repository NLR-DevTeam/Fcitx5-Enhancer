package cn.xiaym.fcitx5.mixins;

import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.BuiltinRuleSet;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.config.rules.ScreenRule;
import cn.xiaym.fcitx5.dbus.Fcitx5DBus;
import cn.xiaym.fcitx5.screen.ElementRuleEditScreen;
import cn.xiaym.fcitx5.screen.ScreenRuleEditScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

//#if MC <= 12000
//$$ import java.lang.reflect.Field;
//$$ import java.util.Arrays;
//$$ import sun.misc.Unsafe;
//#endif
@Mixin(Mouse.class)
public class MouseMixin {
    @Unique
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    @Unique
    private static final long HANDLE = CLIENT.getWindow().getHandle();
    @Unique
    //#if MC > 12000
    private static final SystemToast.Type TOAST_TYPE = new SystemToast.Type(3000);
    //#else
    //$$ private static final SystemToast.Type TOAST_TYPE;
    //$$
    //$$ // Enum is SHIT, my JVM was fucked up for two times because of the Unsafe
    //$$ static {
    //$$     try {
    //$$         Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
    //$$         unsafeField.setAccessible(true);
    //$$         Unsafe unsafe = (Unsafe) unsafeField.get(null);
    //$$
    //$$         TOAST_TYPE = (SystemToast.Type) unsafe.allocateInstance(SystemToast.Type.class);
    //$$         long offset = unsafe.objectFieldOffset(Arrays.stream(SystemToast.Type.class.getDeclaredFields()).filter(it -> it.getType() == long.class).findFirst().get());
    //$$         unsafe.putLong(TOAST_TYPE, offset, 3000L);
    //$$     } catch (Exception ex) {
    //$$         throw new RuntimeException(ex);
    //$$     }
    //$$ }
    //#endif

    //#if MC > 12006
    @Shadow
    @Final
    private MinecraftClient client;
    //#endif

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    public void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window != HANDLE || CLIENT.currentScreen == null || !Main.selectingElement) {
            return;
        }

        ci.cancel();

        switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                if (Main.selectedElement == null) {
                    return;
                }

                ElementRule rule = ModConfig.getOrCreateElementRule(CLIENT.currentScreen.getClass()
                        .getName(), Main.selectedElement.getClass().getName());
                SystemToast.add(CLIENT.getToastManager(), TOAST_TYPE, Text.translatable("fcitx5.selector.completed"), Text.translatable(rule.comment() == null ? "fcitx5.selector.new" : "fcitx5.selector.existing"));

                CLIENT.setScreen(new ElementRuleEditScreen(CLIENT.currentScreen, rule, () -> ModConfig.userElementRules.remove(rule), newValue -> {
                    ModConfig.updateList(ModConfig.userElementRules, rule, newValue);
                    ModConfig.saveConfig();
                }));
            }

            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                ScreenRule rule = ModConfig.getOrCreateScreenRule(CLIENT.currentScreen.getClass().getName());
                SystemToast.add(CLIENT.getToastManager(), TOAST_TYPE, Text.translatable("fcitx5.selector.completed"), Text.translatable(rule.comment() == null ? "fcitx5.selector.new" : "fcitx5.selector.existing"));

                CLIENT.setScreen(new ScreenRuleEditScreen(CLIENT.currentScreen, rule, () -> ModConfig.userScreenRules.remove(rule), newValue -> {
                    ModConfig.updateList(ModConfig.userScreenRules, rule, newValue);
                    ModConfig.saveConfig();
                }));
            }

            default -> {
                return;
            }
        }

        Main.selectingElement = false;
    }

    //#if MC > 12006
    @WrapOperation(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"))
    public boolean onMouseClickedElement(Screen instance, double d, double e, int i, Operation<Boolean> original) {
        //#else
        //$$ @WrapOperation(method = "method_1611", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"))
        //$$ private static boolean onMouseClickedElement(Screen instance, double d, double e, int i, Operation<Boolean> original) {
        //$$ MinecraftClient client = MinecraftClient.getInstance();
        //#endif
        original.call(instance, d, e, i);

        assert client.currentScreen != null;
        boolean ruleFound;
        boolean shouldBlock;

        Optional<Element> hoveredOpt = instance.hoveredElement(d, e);
        if (hoveredOpt.isPresent()) {
            Element hovered = hoveredOpt.get();
            String screenClass = client.currentScreen.getClass().getName();
            String elementClass = hovered.getClass().getName();

            // First user, then built-in.
            ElementRule rule = ModConfig.getElementRule(screenClass, elementClass);
            if (rule == null) {
                rule = BuiltinRuleSet.getElementRule(screenClass, elementClass);
            }

            shouldBlock = (ruleFound = rule != null) && rule.shouldBlock();
        } else {
            ruleFound = false;
            shouldBlock = false;
        }


        Fcitx5DBus.getStateAsync().thenAcceptAsync(state -> {
            if (shouldBlock || (!ruleFound && Main.screenSuppressed)) {
                if (state != Fcitx5DBus.STATE_ACTIVE) {
                    return;
                }

                Main.initialState = state;
                Fcitx5DBus.deactivate();
                return;
            }

            if (Main.initialState == Fcitx5DBus.STATE_ACTIVE && state != Fcitx5DBus.STATE_ACTIVE) {
                Fcitx5DBus.activate();
            }
        });

        return hoveredOpt.isPresent();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == HANDLE && Main.selectingElement) {
            ci.cancel();
        }
    }

    @Inject(method = "onFilesDropped", at = @At("HEAD"), cancellable = true)
    //#if MC > 12006
    public void onFilesDropped(long window, List<Path> paths, int invalidFilesCount, CallbackInfo ci) {
        //#else
        //$$ public void onFilesDropped(long window, List<Path> paths, CallbackInfo ci) {
        //#endif
        if (window == HANDLE && Main.selectingElement) {
            ci.cancel();
        }
    }
}
