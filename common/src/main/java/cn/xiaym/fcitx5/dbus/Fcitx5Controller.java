package cn.xiaym.fcitx5.dbus;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("org.fcitx.Fcitx.Controller1")
public interface Fcitx5Controller extends DBusInterface {
    @DBusMemberName("State")
    int state();

    @DBusMemberName("Activate")
    void activate();

    @DBusMemberName("Deactivate")
    void deactivate();
}
