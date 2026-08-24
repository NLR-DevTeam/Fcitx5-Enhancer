#include <atomic>
#include <cstring>
#include <chrono>
#include <future>
#include <mutex>
#include <thread>
#include <sys/socket.h>
#include <X11/Xlib.h>
#include "cn_xiaym_fcitx5_Fcitx5.h"

static constexpr auto FCITX5_WINDOW_NAME = "Fcitx5 Input Window";

static Display *g_display = nullptr;
static Window g_root = 0;
static Window g_fcitx5Win = 0;
static bool g_windowMapped = false;

static bool g_typing = false;
static std::atomic<bool> g_shutdown{false};
static std::atomic<bool> g_errorOccurred{false};

static Atom g_netWmNameAtom = None;
static Atom g_utf8StringAtom = None;

static int errorHandler(Display *, XErrorEvent *) {
    g_errorOccurred = true;
    return 0;
}

static int ioErrorHandler(Display *) {
    return 0;
}

// JNI
static JavaVM *g_jvm = nullptr;
static jclass g_class = nullptr;
static jmethodID g_onTypingChanged = nullptr;

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    g_jvm = vm;
    JNIEnv *env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_OK && env) {
        if (const auto cls = env->FindClass("cn/xiaym/fcitx5/Fcitx5")) {
            g_class = reinterpret_cast<jclass>(env->NewGlobalRef(cls));
            env->DeleteLocalRef(cls);
            g_onTypingChanged = env->GetStaticMethodID(
                g_class, "onNativeTypingStateChanged", "(Z)V");
        }
    }

    return JNI_VERSION_1_6;
}

static void notifyJava(JNIEnv *env, const bool typing) {
    if (!g_class || !g_onTypingChanged) {
        return;
    }

    env->CallStaticVoidMethod(g_class, g_onTypingChanged, typing ? JNI_TRUE : JNI_FALSE);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

// X11 Helper
static bool isFcitx5Window(const Window win) {
    if (win == 0) {
        return false;
    }

    if (g_netWmNameAtom != None) {
        Atom actualType;
        int actualFormat;
        unsigned long nItems, bytesAfter;
        unsigned char *prop = nullptr;

        if (XGetWindowProperty(g_display, win, g_netWmNameAtom, 0, 1024, False,
                               AnyPropertyType, &actualType, &actualFormat,
                               &nItems, &bytesAfter, &prop) == Success && prop) {
            const auto match = actualType == g_utf8StringAtom &&
                               strcmp(reinterpret_cast<char *>(prop), FCITX5_WINDOW_NAME) == 0;
            XFree(prop);

            if (match) {
                return true;
            }
        }
    }

    char *name = nullptr;
    if (XFetchName(g_display, win, &name) > 0 && name) {
        const auto match = (strcmp(name, FCITX5_WINDOW_NAME) == 0);
        XFree(name);
        return match;
    }

    return false;
}

static bool ensureDisplay() {
    if (g_display) {
        return true;
    }

    g_display = XOpenDisplay(nullptr);
    if (!g_display) {
        return false;
    }

    g_root = DefaultRootWindow(g_display);
    XSetErrorHandler(errorHandler);
    XSetIOErrorHandler(ioErrorHandler);

    g_netWmNameAtom = XInternAtom(g_display, "_NET_WM_NAME", False);
    g_utf8StringAtom = XInternAtom(g_display, "UTF8_STRING", False);
    return true;
}

static void subscribeRoot() {
    XSelectInput(g_display, g_root, SubstructureNotifyMask);
    XFlush(g_display);
}

static bool scanWindows() {
    Window rootRet = 0, parentRet = 0;
    Window *children = nullptr;
    unsigned int childCount = 0;

    if (XQueryTree(g_display, g_root, &rootRet, &parentRet, &children, &childCount) == 0) {
        return false;
    }

    bool found = false;
    for (unsigned int i = 0; i < childCount; ++i) {
        if (isFcitx5Window(children[i])) {
            g_fcitx5Win = children[i];
            found = true;
            break;
        }
    }

    if (children) {
        XFree(children);
    }

    const auto wasTracked = (g_fcitx5Win != 0);
    if (!found) {
        g_fcitx5Win = 0;
        g_windowMapped = false;
    } else if (!wasTracked) {
        XWindowAttributes attr;
        g_errorOccurred = false;

        if (XGetWindowAttributes(g_display, g_fcitx5Win, &attr) != 0) {
            g_windowMapped = (attr.map_state == IsViewable);
        }
    }

    return found;
}

static void handleEvent(const XEvent &event) {
    switch (event.type) {
        case CreateNotify: {
            if (const auto win = event.xcreatewindow.window; isFcitx5Window(win)) {
                g_fcitx5Win = win;
                g_windowMapped = false;
            }

            break;
        }

        case DestroyNotify: {
            if (event.xdestroywindow.window == g_fcitx5Win) {
                g_fcitx5Win = 0;
                g_windowMapped = false;
            }

            break;
        }

        case MapNotify: {
            const auto win = event.xmap.window;
            if (g_fcitx5Win == 0 && isFcitx5Window(win)) {
                g_fcitx5Win = win;
            }

            if (win == g_fcitx5Win) {
                g_windowMapped = true;
            }

            break;
        }

        case UnmapNotify: {
            if (event.xunmap.window == g_fcitx5Win) {
                g_windowMapped = false;
            }

            break;
        }

        default:
            break;
    }
}

static void eventLoop(JNIEnv *attachedEnv, std::promise<bool> initialPromise) {
    const auto found = scanWindows();
    initialPromise.set_value(found);

    const auto initialTyping = found && g_windowMapped;
    g_typing = initialTyping;
    notifyJava(attachedEnv, initialTyping);

    auto lastRescan = std::chrono::steady_clock::now();
    auto x11Fd = ConnectionNumber(g_display);

    auto connectionAlive = [x11Fd]() -> bool {
        fd_set fds;
        FD_ZERO(&fds);
        FD_SET(x11Fd, &fds);
        timeval zero{.tv_sec = 0, .tv_usec = 0};
        if (select(x11Fd + 1, &fds, nullptr, nullptr, &zero) <= 0) {
            return true;
        }

        char c;
        return recv(x11Fd, &c, 1, MSG_PEEK | MSG_DONTWAIT) > 0;
    };

    while (!g_shutdown.load() && connectionAlive()) {
        while (XPending(g_display) > 0) {
            XEvent event;
            XNextEvent(g_display, &event);
            handleEvent(event);

            if (g_errorOccurred) {
                g_errorOccurred = false;
                scanWindows();
            }
        }

        if (const auto currentTyping = (g_fcitx5Win != 0) && g_windowMapped; currentTyping != g_typing) {
            g_typing = currentTyping;
            notifyJava(attachedEnv, currentTyping);
        }

        // wait for new event
        XFlush(g_display);
        fd_set fds;
        FD_ZERO(&fds);
        FD_SET(x11Fd, &fds);
        timeval timeout{.tv_sec = 0, .tv_usec = 200 * 1000};
        select(x11Fd + 1, &fds, nullptr, nullptr, &timeout);

        if (g_shutdown.load() || !connectionAlive()) {
            break;
        }

        // scan with an interval in case of lost events
        if (auto now = std::chrono::steady_clock::now();
            std::chrono::duration_cast<std::chrono::seconds>(now - lastRescan).count() >= 3) {
            scanWindows();
            lastRescan = now;
        }
    }

    if (g_typing) {
        g_typing = false;
        notifyJava(attachedEnv, false);
    }

    if (g_display && connectionAlive()) {
        XCloseDisplay(g_display);
    }

    g_display = nullptr;
}

// Lifecycle
static std::once_flag g_engineStarted;
static std::shared_future<bool> g_initialResultFuture;

static void startEngine() {
    std::promise<bool> promise;
    g_initialResultFuture = promise.get_future().share();

    std::thread([p = std::move(promise)]() mutable {
        JNIEnv *env = nullptr;
        const auto attachedOk = g_jvm &&
                                g_jvm->AttachCurrentThreadAsDaemon(
                                    reinterpret_cast<void **>(&env), nullptr) == JNI_OK &&
                                env != nullptr;

        if (!ensureDisplay()) {
            if (attachedOk) {
                g_jvm->DetachCurrentThread();
            }

            p.set_value(false);
            return;
        }

        subscribeRoot();

        if (!attachedOk) {
            p.set_value(false);
            return;
        }

        eventLoop(env, std::move(p));
        g_jvm->DetachCurrentThread();
    }).detach();
}

static bool initialize() {
    std::call_once(g_engineStarted, startEngine);
    return g_initialResultFuture.wait_for(std::chrono::seconds(5)) == std::future_status::ready
           && g_initialResultFuture.get();
}

extern "C" {
JNIEXPORT void JNICALL Java_cn_xiaym_fcitx5_Fcitx5_initialize(JNIEnv *, jclass) {
    initialize();
}

JNIEXPORT jboolean JNICALL Java_cn_xiaym_fcitx5_Fcitx5_findWindow(JNIEnv *, jclass) {
    return initialize() ? JNI_TRUE : JNI_FALSE;
}
}
