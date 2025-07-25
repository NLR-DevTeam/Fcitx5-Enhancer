package cn.xiaym.fcitx5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The native binding interface.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Fcitx5 {
    public static final Logger LOGGER = LogManager.getLogger("Fcitx5-Enhancer");

    /**
     * Finds the Fcitx5 window.
     *
     * @return whether the window exists
     */
    protected static native boolean findWindow();

    /**
     * @return whether the user is typing
     */
    public static native boolean userTyping();
}
