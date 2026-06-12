package cn.xiaym.fcitx5;

import cn.xiaym.fcitx5.mixins.EditBoxMixin;
import cn.xiaym.fcitx5.mixins.KeyboardHandlerMixin;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class GlobalState {
    public static boolean gameInitialized = false;

    /**
     * Indicates if a new screen is opening, used to intercept the user's input event.
     *
     * @see EditBoxMixin
     */
    public static boolean newScrOpening = false;

    /**
     * Decides if the user can pass input events to the game. <br>
     * Used to intercept duplicated input events. <br>
     * <br/>
     * The internal logic is: <br>
     * * First, the user opens the chat screen, here becomes false; <br>
     * * Then, the user clicked on their keyboard, here becomes true.
     *
     * @see KeyboardHandlerMixin
     * @see EditBoxMixin
     */
    public static boolean allowToType = false;

    public static boolean selectingElement = false;
    public static GuiEventListener selectedElement = null;

    public static String waylandPreedit = null;
}
