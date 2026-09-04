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
@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "unused"})
public class Fcitx5 {
    public static final Logger LOGGER = LogManager.getLogger("Fcitx5-Enhancer");

    /**
     * Updated automatically by the native event-driven watcher.
     */
    private static final AtomicBoolean userTypingCache = new AtomicBoolean(false);

    /**
     * One-shot grace deadline (monotonic nanos) for the key that ends a
     * composition, which may reach the game after the input window is already
     * gone (deferred event delivery); {@code 0} when disarmed.
     */
    private static volatile long compositionEndGraceUntilNanos = 0L;
    private static final long COMPOSITION_END_GRACE_NANOS = 1_500_000_000L; // 1.5 s

    /**
     * Deadline until which arming the end-key grace is suppressed, set when a
     * PREVENT key was already intercepted while the user was still typing.
     */
    private static volatile long suppressArmingUntilNanos = 0L;
    private static final long SUPPRESS_ARMING_NANOS = 250_000_000L; // 250 ms

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
     * Whether a PREVENT key arriving at the game must be intercepted. Consumes
     * the one-shot end-key grace when it is what gets the key intercepted.
     *
     * @return {@code true} to cancel the key event
     */
    public static boolean shouldInterceptPreventKey() {
        final long now = System.nanoTime();
        if (userTypingCache.get()) {
            // This key may be the one ending the composition; its unmap event
            // arrives right after, so keep the next key from being swallowed.
            suppressArmingUntilNanos = now + SUPPRESS_ARMING_NANOS;
            return true;
        }

        if (compositionEndGraceUntilNanos != 0L && now < compositionEndGraceUntilNanos) {
            compositionEndGraceUntilNanos = 0L; // one-shot, consumed
            return true;
        }

        return false;
    }

    /**
     * Called for ordinary input reaching the game after the composition ended
     * (e.g. committed text), which makes the end-key grace unnecessary.
     */
    public static void noteUserInput() {
        if (!userTypingCache.get()) {
            compositionEndGraceUntilNanos = 0L;
        }
    }

    /**
     * Clears the end-key grace on screen changes, where no end-key reaches the game.
     */
    public static void disarmEndKeyGrace() {
        compositionEndGraceUntilNanos = 0L;
    }

    /**
     * Called by the native library when the typing state changes.
     *
     * @param typing whether the user is typing
     */
    private static void onNativeTypingStateChanged(boolean typing) {
        if (userTypingCache.getAndSet(typing) == typing) {
            return;
        }

        if (typing) {
            compositionEndGraceUntilNanos = 0L;
            return;
        }

        // Arm the grace unless the end-key was already handled while typing.
        if (suppressArmingUntilNanos <= System.nanoTime()) {
            compositionEndGraceUntilNanos = System.nanoTime() + COMPOSITION_END_GRACE_NANOS;
        }
    }
}
