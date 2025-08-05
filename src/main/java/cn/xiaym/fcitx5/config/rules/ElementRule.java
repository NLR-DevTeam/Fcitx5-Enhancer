package cn.xiaym.fcitx5.config.rules;

public record ElementRule(String comment, String screenClassName, String elementClassName, boolean shouldBlock) {
    public static ElementRule create(Class<?> screenClass, Class<?> elementClass, boolean shouldBlock) {
        return new ElementRule(null, screenClass.getName(), elementClass.getName(), shouldBlock);
    }

    public ElementRule modifyShouldBlock(boolean newValue) {
        return new ElementRule(comment, screenClassName, elementClassName, newValue);
    }
}
