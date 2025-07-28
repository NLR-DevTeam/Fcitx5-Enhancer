package cn.xiaym.fcitx5.config.rules;

public class ElementRule {
    public String screenClassName;
    public String elementClassName;
    public int elementOrdinary;
    public boolean shouldBlock;

    public ElementRule(String screenClassName, String elementClassName, int elementOrdinary, boolean shouldBlock) {
        this.screenClassName = screenClassName;
        this.elementClassName = elementClassName;
        this.elementOrdinary = elementOrdinary;
        this.shouldBlock = shouldBlock;
    }
}
