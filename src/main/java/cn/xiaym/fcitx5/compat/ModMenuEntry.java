package cn.xiaym.fcitx5.compat;

import cn.xiaym.fcitx5.Main;
import cn.xiaym.fcitx5.config.ModConfig;
import cn.xiaym.fcitx5.screen.IncompatibleNoticeScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

@SuppressWarnings("unused")
public class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return Main.IS_LINUX ? ModConfig::createScreen : IncompatibleNoticeScreen::new;
    }
}
