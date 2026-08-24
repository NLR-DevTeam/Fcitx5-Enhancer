package cn.xiaym.fcitx5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The native binding interface.
 * <p>
 * The native library runs its own event-driven X11 watcher on a daemon
 * thread: it tracks the lifecycle of the Fcitx5 input window and mirrors
 * the "user is typing" state back into {@link #userTypingCache} through
 * {@link #onNativeTypingStateChanged(boolean)}.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Fcitx5 {
    public static final Logger LOGGER = LogManager.getLogger("Fcitx5-Enhancer");

    /**
     * Updated automatically by the native event-driven watcher.
     */
    private static final AtomicBoolean userTypingCache = new AtomicBoolean(false);

    /**
     * Initializes the native event-driven X11 watcher.
     */
    public static native void initialize();

    /**
     * Finds the Fcitx5 window.
     *
     * @return whether the window exists
     */
    protected static native boolean findWindow();

    /**
     * @return whether the user is typing
     */
    public static boolean userTyping() {
        return userTypingCache.get();
    }

    /**
     * Called by the native library when the typing state changes.
     *
     * @param typing whether the user is typing
     */
    private static void onNativeTypingStateChanged(boolean typing) {
        if (userTypingCache.getAndSet(typing) != typing) {
            LOGGER.debug("User typing state changed: {}", typing);
        }
    }
}
