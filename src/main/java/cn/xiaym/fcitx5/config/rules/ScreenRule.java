package cn.xiaym.fcitx5.config.rules;

public class ScreenRule {
    public String comment;
    public String screenClassName;
    public boolean shouldBlock;

    public ScreenRule(String comment, String screenClassName,  boolean shouldBlock) {
        this.comment = comment;
        this.screenClassName = screenClassName;
        this.shouldBlock = shouldBlock;
    }
}
