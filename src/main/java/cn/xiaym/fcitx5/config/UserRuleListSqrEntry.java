package cn.xiaym.fcitx5.config;

import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.config.rules.ScreenRule;
import cn.xiaym.fcitx5.screen.ElementRuleEditScreen;
import cn.xiaym.fcitx5.screen.ScreenRuleEditScreen;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class UserRuleListSqrEntry<T> extends AbstractListListEntry<T, UserRuleListSqrEntry.UserRuleCeil<T>, UserRuleListSqrEntry<T>> {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    public UserRuleListSqrEntry(Text fieldName, List<T> value, boolean defaultExpanded, Consumer<List<T>> saveConsumer) {
        super(fieldName, value, defaultExpanded, null, saveConsumer, null, Text.translatable("controls.reset"), false, false, false, UserRuleCeil::new);
        this.insertButtonEnabled = false;
        this.resetWidget.visible = false;
    }

    @Override
    public UserRuleListSqrEntry<T> self() {
        return this;
    }

    public static class UserRuleCeil<T> extends AbstractListListEntry.AbstractListCell<T, UserRuleCeil<T>, UserRuleListSqrEntry<T>> {
        private final UserRuleListSqrEntry<T> parentEntry;
        private final ButtonWidget manageButton;
        private final List<Element> widgets;
        private T value;
        private long lastTouch;
        private boolean removed = false;

        @SuppressWarnings("unchecked")
        public UserRuleCeil(T value, UserRuleListSqrEntry<T> parentEntry) {
            super(value, parentEntry);
            this.value = value;
            this.parentEntry = parentEntry;

            manageButton = ButtonWidget.builder(Text.translatable("fcitx5.controls.manage"), btn -> {
                if (this.value instanceof ElementRule elementRule) {
                    MC.setScreen(new ElementRuleEditScreen(MC.currentScreen, elementRule, this::handleDeletion, newValue -> this.value = (T) newValue));
                    return;
                } else if (this.value instanceof ScreenRule screenRule) {
                    MC.setScreen(new ScreenRuleEditScreen(MC.currentScreen, screenRule, this::handleDeletion, newValue -> this.value = (T) newValue));
                    return;
                }

                throw new IllegalStateException("Can't handle rule of type " + value.getClass());
            }).width(148).build();

            widgets = Collections.singletonList(manageButton);
        }

        public static String getSimpleClassName(String qualifiedName) {
            if (!qualifiedName.contains(".")) {
                return qualifiedName;
            }

            return qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public Optional<Text> getError() {
            return Optional.empty();
        }

        @Override
        public int getCellHeight() {
            return 20;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            // Background gradient
            long timePast = System.currentTimeMillis() - this.lastTouch;
            int alpha = timePast <= 100L ? 255 : MathHelper.ceil((double) 255.0F - (double) (Math.min((float) (timePast - 100L), 500.0F) / 500.0F) * (double) 255.0F);
            alpha = alpha * 36 / 255 << 24;
            context.fillGradient(0, y, MC.getWindow()
                    .getScaledWidth(), y + entryHeight, 16777215 | alpha, 16777215 | alpha);

            if (isMouseOver(mouseX, mouseY)) {
                lastTouch = System.currentTimeMillis();
            }

            // Content
            if (value instanceof ElementRule elementRule) {
                MutableText text = Text.literal((elementRule.comment == null || elementRule.comment.isEmpty()) ? "%s#%s".formatted(getSimpleClassName(elementRule.screenClassName), getSimpleClassName(elementRule.elementClassName)) : "%s (%s#%s)".formatted(elementRule.comment, getSimpleClassName(elementRule.screenClassName), getSimpleClassName(elementRule.elementClassName)));
                context.drawTextWithShadow(MC.textRenderer, text.withColor(elementRule.shouldBlock ? 0xFF00FF00 /* GREEN */ : Colors.LIGHT_RED), x, y + 6, getPreferredTextColor());
            } else if (value instanceof ScreenRule screenRule) {
                MutableText text = Text.literal((screenRule.comment == null || screenRule.comment.isEmpty()) ? getSimpleClassName(screenRule.screenClassName) : "%s (%s)".formatted(screenRule.comment, getSimpleClassName(screenRule.screenClassName)));
                context.drawTextWithShadow(MC.textRenderer, text.withColor(screenRule.shouldBlock ? 0xFF00FF00 /* GREEN */ : Colors.LIGHT_RED), x, y + 6, getPreferredTextColor());
            } else {
                throw new IllegalStateException("Can't handle rule of type " + value.getClass());
            }

            manageButton.setPosition(x + entryWidth - 150, y);
            manageButton.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (removed) {
                return false;
            }

            parentEntry.setFocused(this);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            // TODO implement
        }

        @Override
        public List<? extends Element> children() {
            return widgets;
        }

        @Override
        public SelectionType getType() {
            return SelectionType.NONE;
        }

        public void handleDeletion() {
            parentEntry.cells.remove(this);
            parentEntry.widgets.remove(this);
            parentEntry.narratables.remove(this);

            if (parentEntry.getFocused() == this) {
                parentEntry.setFocused(null);
            }

            removed = true;
        }
    }
}
