package cn.xiaym.fcitx5;

import net.minecraft.client.gui.Element;

//#if MC <= 12105
//$$ import cn.xiaym.fcitx5.compat.legacy.Rect;
//$$ import java.util.HashSet;
//$$ import java.util.Set;
//#endif

public class GlobalState {
    public static boolean gameInitialized = false;

    /**
     * Indicates if a new screen is opening, used to intercept the user's input event.
     *
     * @see cn.xiaym.fcitx5.mixins.TextFieldWidgetMixin
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
     * @see cn.xiaym.fcitx5.mixins.KeyboardMixin
     * @see cn.xiaym.fcitx5.mixins.TextFieldWidgetMixin
     */
    public static boolean allowToType = false;

    public static boolean selectingElement = false;
    public static Element selectedElement = null;

    public static String waylandPreedit = null;

    //#if MC <= 12105
    //$$ public static boolean simulateDrawing = false;
    //$$ public static Set<Rect> simulatedRectSet = new HashSet<>();
    //#endif
}
