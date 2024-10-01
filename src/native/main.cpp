#include <cstring>
#include <iostream>
#include <X11/Xlib.h>
#include "cn_xiaym_fcitx5_Fcitx5.h"

auto const display = XOpenDisplay(nullptr);
auto const rootWindow = DefaultRootWindow(display);

Window rootRet, parentRet;
Window *childList = nullptr;
unsigned int childNum = 0;

Window fcitx5Win = -1;

// X11 Error Handling
XErrorHandler oldHandler = nullptr;
bool errorOccurred = false;
int customHandler(Display *, XErrorEvent *) {
    errorOccurred = true;
    return 0;
}

bool findFcitx5Win() {
    XQueryTree(display, rootWindow, &rootRet, &parentRet, &childList, &childNum);

    for (unsigned int i = 0; i < childNum; ++i) {
        char *buffer = nullptr;
        XFetchName(display, childList[i], &buffer);

        if (buffer && strcmp(buffer, "Fcitx5 Input Window") == 0) {
            // Found!
            fcitx5Win = childList[i];

            XFree(buffer);
            return true;
        }

        XFree(buffer);
    }

    return false;
}

extern "C" {
JNIEXPORT jboolean JNICALL Java_cn_xiaym_fcitx5_Fcitx5_findWindow(JNIEnv *, jclass) {
    return findFcitx5Win();
}

JNIEXPORT jboolean JNICALL Java_cn_xiaym_fcitx5_Fcitx5_userTyping(JNIEnv *, jclass) {
    XWindowAttributes attr;
    for (unsigned int i = 0; i < 2; i++) {
        oldHandler = XSetErrorHandler(customHandler);
        XGetWindowAttributes(display, fcitx5Win, &attr);
        XSetErrorHandler(oldHandler);

        if (!errorOccurred) {
            break;
        }

        errorOccurred = false;

        if (!findFcitx5Win() /* Fcitx5 was killed */ || i == 1) {
            return false;
        }
    }

    // The status indicator should be ignored
    if (attr.width < 75) {
        return false;
    }

    return attr.map_state == 2;
}
}
