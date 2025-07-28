package cn.xiaym.fcitx5.config.rules;

public class ScreenRule {
    public String screenClassName;
    public boolean shouldBlock;

    public ScreenRule(String screenClassName,  boolean shouldBlock) {
        this.screenClassName = screenClassName;
        this.shouldBlock = shouldBlock;
    }
}
