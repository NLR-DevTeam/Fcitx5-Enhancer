package cn.xiaym.fcitx5;

import cn.xiaym.fcitx5.config.BuiltinRuleSet;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.dbus.Fcitx5DBus;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class IMBlockerListener {
    private static int initialState = Fcitx5DBus.STATE_UNKNOWN;
    private static int screenInitialState = Fcitx5DBus.STATE_UNKNOWN;
    private static int lastSetState = Fcitx5DBus.STATE_UNKNOWN;
    private static boolean currentScreenBlocking = false;
    private static Screen lastCheckedScreen = null;
    private static boolean suppressed = false;

    public static void onScreenOpen(@Nullable Screen screen, @Nullable Screen previousScreen) {
        if (!GlobalState.gameInitialized) {
            return;
        }

        if (screen == null) {
            dispatchAsync(state -> {
                initialState = state;
                tryDeactivate(state);
            });

            return;
        }

        currentScreenBlocking = ModConfig.screenRuleShouldBlock(screen.getClass().getName());
        lastCheckedScreen = screen;

        if (currentScreenBlocking) {
            dispatchAsyncConditional(s -> ModConfig.enforceDeactivation || ensureConsistency(s), IMBlockerListener::tryDeactivate);
            return;
        }

        dispatchAsyncConditional(s -> (previousScreen == null && initialState == Fcitx5DBus.STATE_ACTIVE) || ensureConsistency(s), IMBlockerListener::tryActivate);
    }

    public static void onElementFocus(@Nullable Element element, @NotNull Screen screen) {
        if (!GlobalState.gameInitialized || screen != lastCheckedScreen) {
            return;
        }

        if (element != null) {
            String screenClassName = screen.getClass().getName();
            String elementClassName = element.getClass().getName();

            ElementRule rule = ModConfig.getElementRule(screenClassName, elementClassName);
            if (rule == null) {
                rule = BuiltinRuleSet.getElementRule(screenClassName, elementClassName);
            }

            if (rule != null) {
                if (rule.shouldBlock()) {
                    dispatchAsync(state -> {
                        screenInitialState = state;
                        tryDeactivate(state);
                    });

                    return;
                }

                dispatchAsyncConditional(IMBlockerListener::ensureConsistency, IMBlockerListener::tryActivate);
                return;
            }
        }

        if (currentScreenBlocking) {
            dispatchAsyncConditional(s -> ModConfig.enforceDeactivation || ensureConsistency(s), IMBlockerListener::tryDeactivate);
            return;
        }

        dispatchAsyncConditional(s -> screenInitialState == Fcitx5DBus.STATE_ACTIVE && ensureConsistency(s), IMBlockerListener::tryActivate);
    }

    public static void suppress() {
        suppressed = true;
    }

    public static void tryActivate(int state) {
        if (suppressed) {
            suppressed = false;
            return;
        }

        if (state != Fcitx5DBus.STATE_ACTIVE) {
            Fcitx5DBus.activate();
            lastSetState = Fcitx5DBus.STATE_ACTIVE;
        }
    }

    public static void tryDeactivate(int state) {
        if (state != Fcitx5DBus.STATE_INACTIVE) {
            Fcitx5DBus.deactivate();
            lastSetState = Fcitx5DBus.STATE_INACTIVE;
        }
    }

    public static void dispatchAsync(Consumer<Integer> target) {
        Fcitx5DBus.getStateAsync().thenAccept(target);
    }

    public static void dispatchAsyncConditional(Function<Integer, Boolean> condition, Consumer<Integer> target) {
        Fcitx5DBus.getStateAsync().thenAccept(state -> {
            if (condition.apply(state)) {
                target.accept(state);
            }
        });
    }

    public static boolean ensureConsistency(int state) {
        return state == lastSetState;
    }
}
