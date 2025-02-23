package cn.xiaym.fcitx5;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * This is the client entry point of the mod. <br/>
 * It loads the native library, and does nothing else.
 *
 * @see Fcitx5
 */
@SuppressWarnings("SimplifyOptionalCallChains")
public class Main implements ClientModInitializer {
    /**
     * Indicates if the operating system is Windows.
     * <br/><br/>
     * Fcitx5-Enhancer is **not** able to run on Windows, and it will cause crashes initially.
     * We use this constant to avoid crashes and make it safe for mod-pack developers.
     */
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    private static final Logger LOGGER = LogManager.getLogger("Fcitx5-Enhancer");

    /**
     * Indicates if the chat screen is open, used to intercept the user's input event.
     *
     * @see cn.xiaym.fcitx5.mixins.TextFieldWidgetMixin
     */
    public static boolean chatScrOpening = false;

    /**
     * Decides if the user can pass input events to the game. <br/>
     * Used to intercept duplicated input events. <br/>
     * <br/>
     * The internal logic is: <br/>
     *  * First, the user opens the chat screen, here becomes false; <br/>
     *  * Then, the user clicked on their keyboard, here becomes true.
     */
    public static boolean allowToType = false;

    @Override
    public void onInitializeClient() {
        if (IS_WINDOWS) {
            LOGGER.warn("Warning - You're trying loading Fcitx5-Enhancer on Windows, which is unsupported! - Disabling.");
            return;
        }

        Optional<ModContainer> parent = FabricLoader.getInstance().getModContainer("fcitx5-enhancer");
        if (!parent.isPresent()) {
            throw new IllegalStateException("Can't find parent mod, please do not install fcitx5-enhancer's sub-JAR separately.");
        }

        Path dataDir = FabricLoader.getInstance().getGameDir().resolve(".fcitx5-enhancer");
        if (Files.notExists(dataDir)) {
            try {
                Files.createDirectory(dataDir);
            } catch (IOException ex) {
                LOGGER.error("Failed to create data directory!");
                throw new RuntimeException(ex);
            }
        }

        Path nativeLib = dataDir.resolve("native.so");
        try {
            if (Files.exists(nativeLib)) {
                LOGGER.info("Found .fcitx5-enhancer/native.so, loading...");
            } else {
                Optional<Path> pathOptional = parent.get().findPath("native/libfcitx5_detector.so");
                if (!pathOptional.isPresent()) {
                    LOGGER.error("Can't find built-in native library in fcitx5-enhancer's JAR!");
                    LOGGER.error("And we can't find your custom native library, the game WILL CRASH.");
                    throw new NullPointerException();
                }

                LOGGER.info("Extracting and loading built-in native library...");
                nativeLib = dataDir.resolve(".tmp_internal.so");

                Files.copy(pathOptional.get(), nativeLib, StandardCopyOption.REPLACE_EXISTING);
            }

            System.load(nativeLib.toAbsolutePath().toString());
            LOGGER.info("Successfully loaded the native library.");
        } catch (Exception ex) {
            LOGGER.error("Failed to load native library for Fcitx5-Enhancer!");
            LOGGER.error("-> This library is compiled for Linux x86_64 (glibc 2.40), and your platform is incompatible,");
            LOGGER.error("   please compile the native library yourself and place it at:");
            LOGGER.error("    .minecraft/.fcitx5-enhancer/native.so");

            throw new RuntimeException(ex);
        }

        if (!Fcitx5.findWindow()) {
            LOGGER.warn("Fcitx5 is not running, this mod will not take effect unless it's started.");
        }
    }
}
