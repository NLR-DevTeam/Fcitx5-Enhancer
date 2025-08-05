package cn.xiaym.fcitx5.config.rules;

public record ScreenRule(String comment, String screenClassName, boolean shouldBlock) {
    public static ScreenRule create(Class<?> clazz, boolean shouldBlock) {
        return new ScreenRule(null, clazz.getName(), shouldBlock);
    }

    public ScreenRule modifyShouldBlock(boolean newValue) {
        return new ScreenRule(comment, screenClassName, newValue);
    }
}