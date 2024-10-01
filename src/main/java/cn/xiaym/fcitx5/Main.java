package cn.xiaym.fcitx5;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class Main implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fcitx5-Enhancer");

    @Override
    public void onInitializeClient() {
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
                LOGGER.info("Extracting and loading built-in native library...");
                nativeLib = dataDir.resolve(".tmp_internal.so");

                Files.write(nativeLib, Objects.requireNonNull(Main.class.getClassLoader()
                                .getResourceAsStream("native/libfcitx5_detector.so"))
                        .readAllBytes(), StandardOpenOption.CREATE);
            }

            System.load(nativeLib.toAbsolutePath().toString());
            LOGGER.info("Successfully loaded the native library.");
        } catch (Exception ex) {
            LOGGER.error("Failed to load native library for Fcitx5-Enhancer!");
            LOGGER.error("-> This library is compiled for Linux x86_64 (glibc), if your platform is different,");
            LOGGER.error("   please compile the native library yourself and place it at:");
            LOGGER.error("    .minecraft/.fcitx5-enhancer/native.so");

            throw new RuntimeException(ex);
        }

        if (!Fcitx5.findWindow()) {
            LOGGER.warn("Fcitx5 is not running, this mod will not take effect unless it's started.");
        }
    }
}
