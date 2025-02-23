package cn.xiaym.fcitx5;

/**
 * The native binding interface.
 */
public class Fcitx5 {
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
