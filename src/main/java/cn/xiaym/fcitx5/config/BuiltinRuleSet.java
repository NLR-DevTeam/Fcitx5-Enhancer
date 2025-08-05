package cn.xiaym.fcitx5.config;

import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.config.rules.ScreenRule;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

public class BuiltinRuleSet {
    public static final RuleSetBinding[] RULESETS = {
            new RuleSetBinding("inventory", List.of(
                    new ScreenRuleBinding(
                            "inventory",
                            getTranslation("inventory.inventory.title"),
                            getTranslation("inventory.inventory.tooltip"),
                            true,
                            ScreenRule.create(InventoryScreen.class, true),
                            ScreenRule.create(CreativeInventoryScreen.class, true)
                    )
            ), List.of(
                    new ElementRuleBinding(
                            "creativeSearch",
                            getTranslation("inventory.creativeSearch.title"),
                            null,
                            false,
                            ElementRule.create(CreativeInventoryScreen.class, TextFieldWidget.class, false)
                    )
            )),

            new RuleSetBinding(
                    "interactable", List.of(
                    new ScreenRuleBinding(
                            "blocks",
                            getTranslation("interactable.blocks.title"),
                            getTranslation("interactable.blocks.tooltip"),
                            true,
                            // Sorted by creative items order
                            ScreenRule.create(CraftingScreen.class, true),
                            ScreenRule.create(StonecutterScreen.class, true),
                            ScreenRule.create(CartographyTableScreen.class, true),
                            ScreenRule.create(SmithingScreen.class, true),
                            ScreenRule.create(GrindstoneScreen.class, true),
                            ScreenRule.create(LoomScreen.class, true),
                            ScreenRule.create(FurnaceScreen.class, true),
                            ScreenRule.create(SmokerScreen.class, true),
                            ScreenRule.create(BlastFurnaceScreen.class, true),
                            ScreenRule.create(AnvilScreen.class, true),
                            ScreenRule.create(EnchantmentScreen.class, true),
                            ScreenRule.create(BrewingStandScreen.class, true),
                            ScreenRule.create(BeaconScreen.class, true),
                            ScreenRule.create(LecternScreen.class, true),
                            ScreenRule.create(GenericContainerScreen.class, true),
                            ScreenRule.create(ShulkerBoxScreen.class, true),

                            // Red stone
                            ScreenRule.create(Generic3x3ContainerScreen.class, true), // Dispenser, Dropper, etc.
                            //#if MC > 12000
                            ScreenRule.create(CrafterScreen.class, true),
                            //#endif
                            ScreenRule.create(HopperScreen.class, true),

                            // Administration
                            ScreenRule.create(CommandBlockScreen.class, true),
                            ScreenRule.create(JigsawBlockScreen.class, true),
                            ScreenRule.create(StructureBlockScreen.class, true)
                            //#if MC >= 12105
                            , ScreenRule.create(TestBlockScreen.class, true),
                            ScreenRule.create(TestInstanceBlockScreen.class, true)
                            //#endif
                    ),

                    new ScreenRuleBinding(
                            "items",
                            getTranslation("interactable.items.title"),
                            getTranslation("interactable.items.tooltip"),
                            true,
                            ScreenRule.create(BookScreen.class, true)
                    )
            ), List.of(
                    new ElementRuleBinding(
                            "anvilRename",
                            getTranslation("interactable.anvilRename.title"),
                            null,
                            false,
                            ElementRule.create(AnvilScreen.class, TextFieldWidget.class, false)
                    )
            ))
    };

    private static Text getTranslation(String key) {
        return Text.translatable("fcitx5.config.builtinRules." + key);
    }

    public static void applyConfig(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
        for (RuleSetBinding binding : RULESETS) {
            binding.applyConfig(category, entryBuilder);
        }
    }

    public static boolean screenRuleShouldBlock(String screenClass) {
        for (RuleSetBinding ruleSet : RULESETS) {
            for (ScreenRuleBinding ruleBinding : ruleSet.screenRules) {
                for (ScreenRule screenRule : ruleBinding.screenRules) {
                    if (Objects.equals(screenClass, screenRule.screenClassName()) && screenRule.shouldBlock()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static ElementRule getElementRule(String screenClass, String elementClass) {
        for (RuleSetBinding ruleSet : RULESETS) {
            for (ElementRuleBinding ruleBinding : ruleSet.elementRules) {
                for (ElementRule elementRule : ruleBinding.elementRules) {
                    if (Objects.equals(screenClass, elementRule.screenClassName()) && Objects.equals(elementClass, elementRule.elementClassName())) {
                        return elementRule;
                    }
                }
            }
        }

        return null;
    }

    public record RuleSetBinding(String key, List<ScreenRuleBinding> screenRules,
                                 List<ElementRuleBinding> elementRules) {
        public void applyConfig(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
            for (ScreenRuleBinding ruleBinding : screenRules) {
                category.addEntry(entryBuilder
                        .startBooleanToggle(ruleBinding.configTitle, ruleBinding.screenRules[0].shouldBlock())
                        .setTooltip(ruleBinding.configTooltip == null ? new Text[]{} : new Text[]{ ruleBinding.configTooltip })
                        .setDefaultValue(ruleBinding.defShouldBlock)
                        .setSaveConsumer(newValue -> {
                            for (int i = 0; i < ruleBinding.screenRules.length; i++) {
                                ruleBinding.screenRules[i] = ruleBinding.screenRules[i].modifyShouldBlock(newValue);
                            }
                        })
                        .build());
            }

            for (ElementRuleBinding ruleBinding : elementRules) {
                category.addEntry(entryBuilder
                        .startBooleanToggle(ruleBinding.configTitle, ruleBinding.elementRules[0].shouldBlock())
                        .setTooltip(ruleBinding.configTooltip == null ? new Text[]{} : new Text[]{ ruleBinding.configTooltip })
                        .setDefaultValue(ruleBinding.defShouldBlock)
                        .setSaveConsumer(newValue -> {
                            for (int i = 0; i < ruleBinding.elementRules.length; i++) {
                                ruleBinding.elementRules[i] = ruleBinding.elementRules[i].modifyShouldBlock(newValue);
                            }
                        })
                        .build());
            }
        }
    }

    public static final class ScreenRuleBinding {
        public final String key;
        public final Text configTitle;
        public final Text configTooltip;
        public final boolean defShouldBlock;
        public ScreenRule[] screenRules;

        public ScreenRuleBinding(String key, Text configTitle, Text configTooltip, boolean defShouldBlock, ScreenRule... screenRules) {
            this.key = key;
            this.configTitle = configTitle;
            this.configTooltip = configTooltip;
            this.screenRules = screenRules;
            this.defShouldBlock = defShouldBlock;
        }
    }

    public static final class ElementRuleBinding {
        public final String key;
        public final Text configTitle;
        public final Text configTooltip;
        public final boolean defShouldBlock;
        public ElementRule[] elementRules;

        public ElementRuleBinding(String key, Text configTitle, Text configTooltip, boolean defShouldBlock, ElementRule... elementRules) {
            this.key = key;
            this.configTitle = configTitle;
            this.configTooltip = configTooltip;
            this.defShouldBlock = defShouldBlock;
            this.elementRules = elementRules;
        }
    }
}
