package cn.xiaym.fcitx5;

import cn.xiaym.fcitx5.dbus.Fcitx5Controller;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;

import java.io.IOException;

/**
 * The D-Bus related binding interface.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class Fcitx5DBus {
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_INACTIVE = 1;
    public static final int STATE_ACTIVE = 2;
    private static DBusConnection connection;
    private static Fcitx5Controller controller;

    static {
        // DON'T SPAM!
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getLoggerConfig("org.freedesktop.dbus").setLevel(Level.OFF);

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
                controller = connection.getRemoteObject("org.fcitx.Fcitx5", "/controller", Fcitx5Controller.class);
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
        }

        return true;
    }

    public static int getState() {
        if (!checkDBus()) {
            return STATE_UNKNOWN;
        }

        return controller.state();
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
