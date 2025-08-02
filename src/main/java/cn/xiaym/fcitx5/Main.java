package cn.xiaym.fcitx5;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

//#if MC <= 12105
//$$ import cn.xiaym.fcitx5.compat.legacy.Rect;
//$$ import java.util.HashSet;
//$$ import java.util.Set;
//#endif

/**
 * Client entry point of the mod, saving few global states. <br>
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
    private final static AtomicInteger SUPPRESS_COUNT = new AtomicInteger(0);
    /**
     * Indicates if the chat screen is open, used to intercept the user's input event.
     *
     * @see cn.xiaym.fcitx5.mixins.TextFieldWidgetMixin
     */
    public static boolean chatScrOpening = false;
    /**
     * Decides if the user can pass input events to the game. <br>
     * Used to intercept duplicated input events. <br>
     * <br/>
     * The internal logic is: <br>
     * * First, the user opens the chat screen, here becomes false; <br>
     * * Then, the user clicked on their keyboard, here becomes true.
     *
     * @see cn.xiaym.fcitx5.mixins.KeyboardMixin
     * @see cn.xiaym.fcitx5.mixins.TextFieldWidgetMixin
     */
    public static boolean allowToType = false;

    /**
     * Indicates if we can find D-Bus connection related classes, used to avoid ClassNotDefError.
     */
    public static boolean canFindDBus = false;
    /**
     * The initial state of the IME. Updates when returning to the playing screen.
     *
     * @see cn.xiaym.fcitx5.mixins.MinecraftClientMixin
     */
    public static int initialState;

    public static boolean selectingElement = false;
    public static Element selectedElement = null;

    //#if MC <= 12105
    //$$ public static boolean simulateDrawing = false;
    //$$ public static Set<Rect> simulatedRectSet = new HashSet<>();
    //#endif

    public static String waylandPreedit = null;

    private static ModContainer parentMod;

    public static void suppress() {
        SUPPRESS_COUNT.getAndIncrement();
    }

    public static void unsuppress() {
        SUPPRESS_COUNT.getAndDecrement();
    }

    public static boolean wasSuppressed() {
        int i = SUPPRESS_COUNT.get();
        if (i < 0) {
            SUPPRESS_COUNT.set(0);
        }

        return i > 0;
    }

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

        canFindDBus = true;
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
