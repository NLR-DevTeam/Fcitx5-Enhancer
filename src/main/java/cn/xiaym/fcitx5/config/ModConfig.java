package cn.xiaym.fcitx5.config;

import cn.xiaym.fcitx5.Fcitx5;
import cn.xiaym.fcitx5.config.rules.ElementRule;
import cn.xiaym.fcitx5.config.rules.ScreenRule;
import cn.xiaym.fcitx5.screen.ElementRuleEditScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fcitx5-enhancer.json");
    private static final HashMap<Class<?>, Serializer<Object>> INTERNAL_SERIALIZERS = new HashMap<>();
    private static final HashMap<Class<?>, Deserializer<Object>> INTERNAL_DESERIALIZERS = new HashMap<>();
    public static boolean imBlockerEnabled = true;
    public static boolean restoreInitialState = false;
    public static ModifierKeyCode selectElementKey = ModifierKeyCode.of(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_S), Modifier.of(false, true, true));
    public static boolean builtinCommandSuppressDirect = true;
    public static boolean builtinCommandDisableLater = true;
    @SuppressWarnings("unused")
    public static BuiltinRuleSet.RuleSetBinding[] builtinRuleSets = BuiltinRuleSet.RULESETS;
    public static List<ElementRule> userElementRules = new ArrayList<>();
    public static List<ScreenRule> userScreenRules = new ArrayList<>();
    public static boolean nativeWaylandOverlayEnabled = true;
    public static int nativeWaylandOverlayX = 10;
    public static int nativeWaylandOverlayY = 10;

    static {
        register(boolean.class, JsonObject::addProperty, (o, k) -> o.get(k).getAsBoolean());
        register(int.class, JsonObject::addProperty, (o, k) -> o.get(k).getAsInt());

        register(ModifierKeyCode.class, (o, k, v) -> {
            JsonObject serialized = new JsonObject();
            serialized.addProperty("base", v.getKeyCode().getCode());
            serialized.addProperty("modifier", v.getModifier().getValue());
            o.add(k, serialized);
        }, (o, k) -> {
            JsonObject serialized = o.getAsJsonObject(k);
            return ModifierKeyCode.of(InputUtil.Type.KEYSYM.createFromCode(serialized.get("base")
                    .getAsInt()), Modifier.of(serialized.get("modifier").getAsShort()));
        });

        register(List.class, (o, k, v) -> {
            JsonArray serialized = new JsonArray(v.size());

            switch (k) {
                case "userElementRules" -> {
                    for (Object it : v) {
                        JsonObject object = serializeElementRule((ElementRule) it);
                        serialized.add(object);
                    }
                }

                case "userScreenRules" -> {
                    for (Object it : v) {
                        JsonObject object = serializeScreenRule((ScreenRule) it);
                        serialized.add(object);
                    }
                }
            }

            o.add(k, serialized);
        }, (o, k) -> {
            JsonArray serialized = o.getAsJsonArray(k);
            List<Object> deserialized = new ArrayList<>(serialized.size());

            switch (k) {
                case "userElementRules" -> {
                    for (Object it : serialized) {
                        JsonObject object = (JsonObject) it;
                        deserialized.add(new ElementRule(object.get("comment").getAsString(), object.get("screenClass")
                                .getAsString(), object.get("elementClass").getAsString(), object.get("block")
                                .getAsBoolean()));
                    }
                }

                case "userScreenRules" -> {
                    for (Object it : serialized) {
                        JsonObject object = (JsonObject) it;
                        deserialized.add(new ScreenRule(object.get("comment").getAsString(), object.get("screenClass")
                                .getAsString(), object.get("block").getAsBoolean()));
                    }
                }
            }

            return deserialized;
        });

        register(BuiltinRuleSet.RuleSetBinding[].class, (o, k, v) -> {
            JsonObject serialized = new JsonObject();
            for (BuiltinRuleSet.RuleSetBinding ruleSet : v) {
                JsonObject ruleSetObject = new JsonObject();
                for (BuiltinRuleSet.ScreenRuleBinding screenRule : ruleSet.screenRules()) {
                    ruleSetObject.addProperty(screenRule.key, screenRule.screenRules[0].shouldBlock());
                }

                for (BuiltinRuleSet.ElementRuleBinding elementRule : ruleSet.elementRules()) {
                    ruleSetObject.addProperty(elementRule.key, elementRule.elementRules[0].shouldBlock());
                }

                serialized.add(ruleSet.key(), ruleSetObject);
            }

            o.add(k, serialized);
        }, (o, k) -> {
            JsonObject serialized = o.getAsJsonObject(k);
            for (BuiltinRuleSet.RuleSetBinding ruleSet : builtinRuleSets) {
                if (!serialized.has(ruleSet.key())) {
                    continue;
                }

                JsonObject ruleSetObject = serialized.getAsJsonObject(ruleSet.key());
                for (BuiltinRuleSet.ScreenRuleBinding screenRule : ruleSet.screenRules()) {
                    if (!ruleSetObject.has(screenRule.key)) {
                        continue;
                    }

                    boolean shouldBlock = ruleSetObject.get(screenRule.key).getAsBoolean();
                    for (int i = 0; i < screenRule.screenRules.length; i++) {
                        screenRule.screenRules[i] = screenRule.screenRules[i].modifyShouldBlock(shouldBlock);
                    }
                }

                for (BuiltinRuleSet.ElementRuleBinding elementRule : ruleSet.elementRules()) {
                    if (!ruleSetObject.has(elementRule.key)) {
                        continue;
                    }

                    boolean shouldBlock = ruleSetObject.get(elementRule.key).getAsBoolean();
                    for (int i = 0; i < elementRule.elementRules.length; i++) {
                        elementRule.elementRules[i] = elementRule.elementRules[i].modifyShouldBlock(shouldBlock);
                    }
                }
            }

            return builtinRuleSets;
        });

        try {
            if (Files.notExists(CONFIG_PATH)) {
                Files.writeString(CONFIG_PATH, "{}", StandardOpenOption.CREATE);
            } else {
                loadConfig();
            }
        } catch (Exception ex) {
            Fcitx5.LOGGER.error("Failed to load config!", ex);
        }
    }

    private static JsonObject serializeElementRule(ElementRule it) {
        JsonObject object = new JsonObject();
        object.addProperty("comment", it.comment() == null ? "" : it.comment());
        object.addProperty("screenClass", ElementRuleEditScreen.nullSafe(it.screenClassName()));
        object.addProperty("elementClass", ElementRuleEditScreen.nullSafe(it.elementClassName()));
        object.addProperty("block", it.shouldBlock());
        return object;
    }

    private static JsonObject serializeScreenRule(ScreenRule it) {
        JsonObject object = new JsonObject();
        object.addProperty("comment", it.comment() == null ? "" : it.comment());
        object.addProperty("screenClass", ElementRuleEditScreen.nullSafe(it.screenClassName()));
        object.addProperty("block", it.shouldBlock());
        return object;
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(Class<T> clazz, Serializer<T> serializer, Deserializer<T> deserializer) {
        INTERNAL_SERIALIZERS.put(clazz, (Serializer<Object>) serializer);
        INTERNAL_DESERIALIZERS.put(clazz, (Deserializer<Object>) deserializer);
    }

    private static void loadConfig() throws Exception {
        JsonObject object = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);

        Field[] configFields = ModConfig.class.getFields();
        for (Field configField : configFields) {
            configField.setAccessible(true);

            String name = configField.getName();
            if (!object.has(name)) {
                continue;
            }

            Class<?> type = configField.getType();
            if (INTERNAL_DESERIALIZERS.containsKey(type)) {
                configField.set(null, INTERNAL_DESERIALIZERS.get(type).deserialize(object, name));
            }
        }
    }

    public static void saveConfig() {
        try {
            JsonObject object = new JsonObject();

            Field[] configFields = ModConfig.class.getFields();
            for (Field configField : configFields) {
                configField.setAccessible(true);

                String name = configField.getName();
                Object value = configField.get(null);
                Class<?> type = configField.getType();
                if (INTERNAL_SERIALIZERS.containsKey(type)) {
                    INTERNAL_SERIALIZERS.get(type).serialize(object, name, value);
                }
            }

            Files.writeString(CONFIG_PATH, GSON.toJson(object));
        } catch (Exception ex) {
            Fcitx5.LOGGER.error("Failed to save config!", ex);
        }
    }

    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Text.translatable("fcitx5.config.title")).setSavingRunnable(ModConfig::saveConfig);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        builder.getOrCreateCategory(Text.translatable("fcitx5.config.category.general"))
                .addEntry(entryBuilder.startBooleanToggle(Text.translatable("fcitx5.config.general.imBlockerEnabled.title"), imBlockerEnabled)
                        .setTooltip(Text.translatable("fcitx5.config.general.imBlockerEnabled.tooltip"))
                        .setDefaultValue(true).setSaveConsumer(newValue -> imBlockerEnabled = newValue).build())
                .addEntry(entryBuilder.startBooleanToggle(Text.translatable("fcitx5.config.general.restoreInitialState.title"), restoreInitialState)
                        .setTooltip(Text.translatable("fcitx5.config.general.restoreInitialState.tooltip"))
                        .setDefaultValue(false).setSaveConsumer(newValue -> restoreInitialState = newValue).build())
                .addEntry(entryBuilder.startModifierKeyCodeField(Text.translatable("fcitx5.config.general.selectElement.title"), selectElementKey)
                        .setTooltip(Text.translatable("fcitx5.config.general.selectElement.tooltip"))
                        .setDefaultValue(ModifierKeyCode.of(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_S), Modifier.of(false, true, true)))
                        .setModifierSaveConsumer(newValue -> selectElementKey = newValue).build());

        // Built-in rules start
        ConfigCategory builtinRulesCategory = builder.getOrCreateCategory(Text.translatable("fcitx5.config.category.builtinRules"));
        SubCategoryBuilder commandInputSubCategory = entryBuilder.startSubCategory(Text.translatable("fcitx5.config.builtinRules.subCategory.commandInput"))
                .setExpanded(true);
        commandInputSubCategory.add(entryBuilder.startBooleanToggle(Text.translatable("fcitx5.config.builtinRules.commandInput.directInput.title"), builtinCommandSuppressDirect)
                .setTooltip(Text.translatable("fcitx5.config.builtinRules.commandInput.directInput.tooltip", MinecraftClient.getInstance().options.commandKey.getBoundKeyLocalizedText()))
                .setDefaultValue(true).setSaveConsumer(newValue -> builtinCommandSuppressDirect = newValue).build());
        commandInputSubCategory.add(entryBuilder.startBooleanToggle(Text.translatable("fcitx5.config.builtinRules.commandInput.later.title"), builtinCommandDisableLater)
                .setTooltip(Text.translatable("fcitx5.config.builtinRules.commandInput.later.tooltip"))
                .setDefaultValue(true).setSaveConsumer(newValue -> builtinCommandDisableLater = newValue).build());

        builtinRulesCategory.addEntry(commandInputSubCategory.build());
        BuiltinRuleSet.applyConfig(builtinRulesCategory, entryBuilder);
        // Built-in rules end

        builder.getOrCreateCategory(Text.translatable("fcitx5.config.category.userRules"))
                .addEntry(new UserRuleListSqrEntry<>(Text.translatable("fcitx5.config.userRules.subCategory.elementRules"), userElementRules, true, newValue -> userElementRules = newValue, () -> new ElementRule("", null, null, false)))
                .addEntry(new UserRuleListSqrEntry<>(Text.translatable("fcitx5.config.userRules.subCategory.screenRules"), userScreenRules, true, newValue -> userScreenRules = newValue, () -> new ScreenRule("", null, false)));

        builder.getOrCreateCategory(Text.translatable("fcitx5.config.category.nativeWayland"))
                .addEntry(entryBuilder.startBooleanToggle(Text.translatable("fcitx5.config.nativeWayland.preeditEnabled.title"), nativeWaylandOverlayEnabled)
                        .setTooltip(Text.translatable("fcitx5.config.nativeWayland.preeditEnabled.description"))
                        .setDefaultValue(true).setSaveConsumer(newValue -> nativeWaylandOverlayEnabled = newValue)
                        .build())
                .addEntry(entryBuilder.startIntField(Text.translatable("fcitx5.config.nativeWayland.preeditX.title"), nativeWaylandOverlayX)
                        .setTooltip(Text.translatable("fcitx5.config.nativeWayland.preeditX.description"))
                        .setDefaultValue(10).setSaveConsumer(newValue -> nativeWaylandOverlayX = newValue).build())
                .addEntry(entryBuilder.startIntField(Text.translatable("fcitx5.config.nativeWayland.preeditY.title"), nativeWaylandOverlayY)
                        .setTooltip(Text.translatable("fcitx5.config.nativeWayland.preeditY.description"))
                        .setDefaultValue(10).setSaveConsumer(newValue -> nativeWaylandOverlayY = newValue).build());

        return builder.build();
    }

    public static ElementRule getElementRule(String screenClass, String elementClass) {
        for (ElementRule rule : userElementRules) {
            if (Objects.equals(screenClass, rule.screenClassName()) && Objects.equals(elementClass, rule.elementClassName())) {
                return rule;
            }
        }

        return null;
    }

    public static ElementRule getOrCreateElementRule(String screenClass, String elementClass) {
        ElementRule elementRule = getElementRule(screenClass, elementClass);
        if (elementRule != null) {
            return elementRule;
        }

        return new ElementRule(null, screenClass, elementClass, true);
    }

    public static ScreenRule getOrCreateScreenRule(String screenClass) {
        for (ScreenRule rule : userScreenRules) {
            if (Objects.equals(screenClass, rule.screenClassName())) {
                return rule;
            }
        }

        return new ScreenRule(null, screenClass, true);
    }

    public static <T> void updateList(List<T> list, T old, T updated) {
        int i = list.indexOf(old);
        if (i != -1) {
            list.set(i, updated);
            return;
        }

        list.add(updated);
    }

    public static boolean screenRuleShouldBlock(String screenClass) {
        for (ScreenRule rule : userScreenRules) {
            if (Objects.equals(screenClass, rule.screenClassName()) && rule.shouldBlock()) {
                return true;
            }
        }

        return false;
    }

    @FunctionalInterface
    public interface Serializer<T> {
        void serialize(JsonObject jsonObject, String key, T value);
    }

    @FunctionalInterface
    public interface Deserializer<T> {
        T deserialize(JsonObject jsonObject, String key);
    }
}
