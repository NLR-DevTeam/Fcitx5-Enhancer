package cn.xiaym.fcitx5;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Client entry point of the mod. <br>
 *
 * @see Fcitx5
 */
public class Main implements ClientModInitializer {
    /**
     * Indicates if the operating system is Linux.
     * <br><br>
     * Fcitx5-Enhancer is **not** able to run on OSs other than Linux, and it will cause crashes initially.
     * We use this constant to avoid crashes and make it safe for mod-pack developers.
     */
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
    private static ModContainer parentMod;

    public static Throwable tryLoadLibrary(String name) {
        Path dataDir = FabricLoader.getInstance().getGameDir().resolve(".fcitx5-enhancer");
        if (Files.notExists(dataDir)) {
            try {
                Files.createDirectory(dataDir);
            } catch (IOException ex) {
                Fcitx5.LOGGER.error("Failed to create data directory!");
                throw new RuntimeException(ex);
            }
        }

        Path nativeLib = dataDir.resolve(name);
        try {
            if (Files.exists(nativeLib)) {
                Fcitx5.LOGGER.info("Found custom library {}, loading...", name);
            } else {
                Optional<Path> pathOptional = parentMod.findPath("native/" + name);
                if (pathOptional.isEmpty()) {
                    Fcitx5.LOGGER.error("Can't find built-in native library {} in Fcitx5-Enhancer's JAR!", name);
                    Fcitx5.LOGGER.error("And we can't find your custom native library, the game WILL CRASH.");
                    throw new IllegalStateException();
                }

                Fcitx5.LOGGER.info("Extracting and loading built-in library {}...", name);

                nativeLib = dataDir.resolve("internal_" + name);
                Files.copy(pathOptional.get(), nativeLib, StandardCopyOption.REPLACE_EXISTING);
            }

            System.load(nativeLib.toAbsolutePath().toString());
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }

    public static void setScreen(Screen screen) {
        //#if MC >= 260200
        Minecraft.getInstance().setScreenAndShow(screen);
        //#else
        //$$ Minecraft.getInstance().setScreen(screen);
        //#endif
    }

    public static Screen getScreen() {
        //#if MC >= 260200
        return Minecraft.getInstance().gui.screen();
        //#else
        //$$ return Minecraft.getInstance().screen;
        //#endif
    }

    public static ToastManager getToastManager() {
        //#if MC >= 260200
        return Minecraft.getInstance().gui.toastManager();
        //#else
        //$$ return Minecraft.getInstance().getToastManager();
        //#endif
    }

    @Override
    public void onInitializeClient() {
        if (!IS_LINUX) {
            Fcitx5.LOGGER.warn("Warning - You're trying loading Fcitx5-Enhancer on non-Linux platform, which is unsupported! - Disabling.");
            return;
        }

        Optional<ModContainer> parent = FabricLoader.getInstance().getModContainer("fcitx5-enhancer");
        if (parent.isEmpty()) {
            throw new IllegalStateException("Can't find parent mod, please do not install fcitx5-enhancer's sub-JAR separately.");
        }

        parentMod = parent.get();

        Throwable throwable = tryLoadLibrary("libfcitx5_detector.so");
        if (throwable != null) {
            Fcitx5.LOGGER.error("Failed to load native library for Fcitx5-Enhancer!");
            Fcitx5.LOGGER.error("Please refer to the README at GitHub for more information:");
            Fcitx5.LOGGER.error(" -> https://github.com/NLR-DevTeam/Fcitx5-Enhancer");

            throw new RuntimeException(throwable);
        }

        if (!Fcitx5.findWindow()) {
            Fcitx5.LOGGER.warn("Fcitx5 is not running, this mod will not take effect unless it's started.");
        }
    }
}
