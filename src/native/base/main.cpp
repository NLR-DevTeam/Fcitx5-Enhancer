#include <cstring>
#include <iostream>
#include <mutex>
#include <X11/Xlib.h>
#include "cn_xiaym_fcitx5_Fcitx5.h"

static Display *display = nullptr;
static Window rootWindow = 0;

static bool ensureDisplay() {
    if (display != nullptr) {
        return true;
    }

    display = XOpenDisplay(nullptr);
    if (!display) {
        return false;
    }

    rootWindow = DefaultRootWindow(display);
    return true;
}

static Window fcitx5Win = -1;

// X11 Error Handling
static bool errorOccurred = false;
static std::mutex x11_mutex;

static int customHandler(Display *, XErrorEvent *) {
    errorOccurred = true;
    return 0;
}

static bool findFcitx5Win() {
    if (!ensureDisplay()) {
        return false;
    }

    Window rootRet, parentRet;
    Window *childList = nullptr;
    unsigned int childNum = 0;

    if (XQueryTree(display, rootWindow, &rootRet, &parentRet, &childList, &childNum) == 0) {
        return false;
    }

    bool found = false;
    for (unsigned int i = 0; i < childNum; ++i) {
        char *buffer = nullptr;

        if (XFetchName(display, childList[i], &buffer) > 0 && buffer) {
            if (strcmp(buffer, "Fcitx5 Input Window") == 0) {
                fcitx5Win = childList[i];
                found = true;
            }

            XFree(buffer);
        }

        if (found) {
            break;
        }
    }

    if (childList) {
        XFree(childList);
    }

    return found;
}

extern "C" {
JNIEXPORT jboolean JNICALL Java_cn_xiaym_fcitx5_Fcitx5_findWindow(JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(x11_mutex);
    return findFcitx5Win();
}

JNIEXPORT jboolean JNICALL Java_cn_xiaym_fcitx5_Fcitx5_userTyping(JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(x11_mutex);

    if (!ensureDisplay()) {
        return false;
    }

    XWindowAttributes attr;
    for (unsigned int i = 0; i < 2; i++) {
        auto const oldHandler = XSetErrorHandler(customHandler);
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

    return attr.map_state == IsViewable;
}
}
