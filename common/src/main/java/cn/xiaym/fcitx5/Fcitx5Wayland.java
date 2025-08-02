package cn.xiaym.fcitx5;

import java.util.function.Consumer;

/**
 * Wayland native binding interface.
 */
public class Fcitx5Wayland {
    public static Consumer<String> onPreeditString;
    public static Consumer<String> onCommitString;

    public static native void initialize(long displayId);

    public static native void updateWindow(int width, int height);
}
