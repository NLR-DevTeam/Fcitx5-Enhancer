package cn.xiaym.fcitx5.config;

import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.config.rules.ScreenRule;
import cn.xiaym.fcitx5.screen.ElementRuleEditScreen;
import cn.xiaym.fcitx5.screen.ScreenRuleEditScreen;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class UserRuleListSqrEntry<T> extends AbstractListListEntry<T, UserRuleListSqrEntry.UserRuleCeil<T>, UserRuleListSqrEntry<T>> {
    private static final Minecraft MC = Minecraft.getInstance();

    public UserRuleListSqrEntry(Component fieldName, List<T> value, boolean defaultExpanded, Consumer<List<T>> saveConsumer, Supplier<T> instanceCreator) {
        super(fieldName, value, defaultExpanded, null, saveConsumer, null, Component.translatable("controls.reset"), false, false, false, UserRuleCeil::new);
        this.createNewInstance = entry -> new UserRuleCeil<>(instanceCreator.get(), entry);
        this.resetWidget.visible = false;
    }

    @Override
    public UserRuleListSqrEntry<T> self() {
        return this;
    }

    public static class UserRuleCeil<T> extends AbstractListCell<T, UserRuleCeil<T>, UserRuleListSqrEntry<T>> {
        private final UserRuleListSqrEntry<T> parentEntry;
        private final Button manageButton;
        private final List<GuiEventListener> widgets;
        private T value;
        private long lastTouch;

        @SuppressWarnings("unchecked")
        public UserRuleCeil(T value, UserRuleListSqrEntry<T> parentEntry) {
            super(value, parentEntry);
            this.value = value;
            this.parentEntry = parentEntry;

            manageButton = Button.builder(Component.translatable("fcitx5.controls.manage"), _ -> {
                if (this.value instanceof ElementRule elementRule) {
                    Main.setScreen(new ElementRuleEditScreen(Main.getScreen(), elementRule, this::handleDeletion, newValue -> {
                        this.value = (T) newValue;
                        parentEntry.save();
                    }));

                    return;
                } else if (this.value instanceof ScreenRule screenRule) {
                    Main.setScreen(new ScreenRuleEditScreen(Main.getScreen(), screenRule, this::handleDeletion, newValue -> {
                        this.value = (T) newValue;
                        parentEntry.save();
                    }));

                    return;
                }

                throw new IllegalStateException("Can't handle rule of type " + value.getClass());
            }).width(148).build();

            widgets = Collections.singletonList(manageButton);
        }

        public static String getSimpleClassName(String qualifiedName) {
            if (qualifiedName == null) {
                return "-";
            }

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
        public Optional<Component> getError() {
            return Optional.empty();
        }

        @Override
        public int getCellHeight() {
            return 20;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            // Background gradient
            long timePast = System.currentTimeMillis() - this.lastTouch;
            int alpha = timePast <= 100L ? 255 : Mth.ceil((double) 255.0F - (double) (Math.min((float) (timePast - 100L), 500.0F) / 500.0F) * (double) 255.0F);
            alpha = alpha * 36 / 255 << 24;
            context.fillGradient(0, y, MC.getWindow()
                    .getGuiScaledWidth(), y + entryHeight, 16777215 | alpha, 16777215 | alpha);

            if (isMouseOver(mouseX, mouseY)) {
                lastTouch = System.currentTimeMillis();
            }

            // Content
            if (value instanceof ElementRule(
                    String comment, String screenClassName, String elementClassName, boolean shouldBlock
            )) {
                MutableComponent text = Component.literal((comment == null || comment
                        .isEmpty()) ? "%s#%s".formatted(getSimpleClassName(screenClassName), getSimpleClassName(elementClassName)) : "%s (%s#%s)".formatted(comment, getSimpleClassName(screenClassName), getSimpleClassName(elementClassName)));
                context.text(MC.font, text.setStyle(text.getStyle()
                        .withColor(shouldBlock ? 0xFF00FF00 /* GREEN */ : 0xFFDF505 /* LIGHT_RED */)), x, y + 6, getPreferredTextColor());
            } else if (value instanceof ScreenRule(String comment, String screenClassName, boolean shouldBlock)) {
                MutableComponent text = Component.literal((comment == null || comment
                        .isEmpty()) ? getSimpleClassName(screenClassName) : "%s (%s)".formatted(comment, getSimpleClassName(screenClassName)));
                context.text(MC.font, text.setStyle(text.getStyle()
                        .withColor(shouldBlock ? 0xFF00FF00 /* GREEN */ : 0xFFDF505 /* LIGHT_RED */)), x, y + 6, getPreferredTextColor());
            } else {
                throw new IllegalStateException("Can't handle rule of type " + value.getClass());
            }

            manageButton.setPosition(x + entryWidth - 150, y);
            manageButton.extractRenderState(context, mouseX, mouseY, delta);
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput builder) {
            // TODO implement
        }

        @Override
        @NotNull
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        @NotNull
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        public void handleDeletion() {
            if (this.value instanceof ElementRule rule) {
                ModConfig.userElementRules.remove(rule);
            } else if (this.value instanceof ScreenRule rule) {
                ModConfig.userScreenRules.remove(rule);
            }

            parentEntry.cells.remove(this);
            parentEntry.widgets.remove(this);
            parentEntry.narratables.remove(this);

            if (parentEntry.getFocused() == this) {
                parentEntry.setFocused(null);
            }
        }
    }
}
