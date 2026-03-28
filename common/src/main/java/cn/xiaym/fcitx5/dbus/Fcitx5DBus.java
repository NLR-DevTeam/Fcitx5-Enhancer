package cn.xiaym.fcitx5.dbus;

import cn.xiaym.fcitx5.Fcitx5;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The D-Bus related binding interface.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Fcitx5DBus {
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_INACTIVE = 1;
    public static final int STATE_ACTIVE = 2;
    private static final Executor ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();
    private static DBusConnection connection;
    private static Fcitx5Controller1 controller;

    static {
        tryInit();
    }

    private static void tryInit() {
        try {
            connection = DBusConnectionBuilder.forSessionBus()
                    .withDisconnectCallback(new IDisconnectCallback() {
                        @Override
                        public void disconnectOnError(IOException ex) {
                            Fcitx5.LOGGER.warn("D-Bus connection is closed unexpectedly due to an exception.", ex);
                        }
                    })
                    .build();
            connection.connect();
        } catch (Exception ex) {
            connection = null;
            Fcitx5.LOGGER.warn("Unable to connect to local D-Bus (session), please check your environment. Relevant functionalities will be disabled.", ex);
        }

        if (connection != null) {
            try {
                controller = connection.getRemoteObject("org.fcitx.Fcitx5", "/controller", Fcitx5Controller1.class);
            } catch (Exception ex) {
                connection = null;
                controller = null;

                Fcitx5.LOGGER.warn("Can't get Fcitx5 Controller. Relevant functionalities will be disabled.", ex);
            }
        } else {
            controller = null;
        }
    }

    private static boolean checkDBus() {
        if (connection == null) {
            return false;
        }

        if (!connection.isConnected()) {
            tryInit();
            return checkDBus();
        }

        return true;
    }

    public static int getState() {
        return checkDBus() ? controller.state() : STATE_UNKNOWN;
    }

    public static CompletableFuture<Integer> getStateAsync() {
        return CompletableFuture.supplyAsync(Fcitx5DBus::getState, ASYNC_EXECUTOR);
    }

    public static void activate() {
        if (!checkDBus()) {
            return;
        }

        controller.activate();
    }

    public static void deactivate() {
        if (!checkDBus()) {
            return;
        }

        controller.deactivate();
    }
}
