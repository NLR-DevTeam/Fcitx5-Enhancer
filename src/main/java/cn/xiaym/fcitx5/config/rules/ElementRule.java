package cn.xiaym.fcitx5.config.rules;

public class ElementRule {
    public String comment;
    public String screenClassName;
    public String elementClassName;
    public boolean shouldBlock;

    public ElementRule(String comment, String screenClassName, String elementClassName, boolean shouldBlock) {
        this.comment = comment;
        this.screenClassName = screenClassName;
        this.elementClassName = elementClassName;
        this.shouldBlock = shouldBlock;
    }
}
