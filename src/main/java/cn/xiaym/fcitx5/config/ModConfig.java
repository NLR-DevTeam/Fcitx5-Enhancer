package cn.xiaym.fcitx5.config;

import cn.xiaym.fcitx5.Fcitx5;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fcitx5-enhancer.json");
    public static boolean imBlockerEnabled = true;
    public static boolean restoreInitialState = false;

    static {
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
            Object value = null;
            if (type == boolean.class) {
                value = object.get(name).getAsBoolean();
            }

            configField.set(null, value);
        }
    }

    private static void saveConfig() {
        try {
            JsonObject object = new JsonObject();

            Field[] configFields = ModConfig.class.getFields();
            for (Field configField : configFields) {
                configField.setAccessible(true);

                String name = configField.getName();
                Object value = configField.get(null);
                Class<?> type = configField.getType();
                if (type == boolean.class) {
                    object.addProperty(name, (boolean) value);
                }
            }

            Files.writeString(CONFIG_PATH, GSON.toJson(object));
        } catch (Exception ex) {
            Fcitx5.LOGGER.error("Failed to save config!", ex);
        }
    }

    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Text.translatable("fcitx5.config.title"))
                .setSavingRunnable(ModConfig::saveConfig);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        builder.getOrCreateCategory(Text.translatable("fcitx5.config.category.general"))
                .addEntry(entryBuilder
                        .startBooleanToggle(Text.translatable("fcitx5.config.general.imBlockerEnabled.title"), imBlockerEnabled)
                        .setTooltip(Text.translatable("fcitx5.config.general.imBlockerEnabled.tooltip"))
                        .setDefaultValue(true)
                        .setSaveConsumer(newValue -> imBlockerEnabled = newValue)
                        .build())
                .addEntry(entryBuilder
                        .startBooleanToggle(Text.translatable("fcitx5.config.general.restoreInitialState.title"), restoreInitialState)
                        .setTooltip(Text.translatable("fcitx5.config.general.restoreInitialState.tooltip"))
                        .setDefaultValue(false)
                        .setSaveConsumer(newValue -> restoreInitialState = newValue)
                        .build());

        return builder.build();
    }
}
