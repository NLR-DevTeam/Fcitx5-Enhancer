#include <algorithm>
#include <cstring>

#include "text-input-unstable-v3-client-protocol.h"
#include "cn_xiaym_fcitx5_Fcitx5Wayland.h"

// Global Variables
wl_seat *waylandSeat = nullptr;
zwp_text_input_manager_v3 *textInputManager = nullptr;
zwp_text_input_v3 *textInput = nullptr;

// Registry Start
static void handleRegistryGlobal(void *, wl_registry *registry, const uint32_t name, const char *interface,
                                 const uint32_t version) {
    if (strcmp(interface, "wl_seat") == 0) {
        waylandSeat = static_cast<wl_seat *>(wl_registry_bind(registry, name, &wl_seat_interface,
                                                              std::min<uint32_t>(version, 4)));
        return;
    }

    if (strcmp(interface, "zwp_text_input_manager_v3") == 0) {
        textInputManager = static_cast<zwp_text_input_manager_v3 *>(wl_registry_bind(
            registry, name, &zwp_text_input_manager_v3_interface, 1));
    }
}

static void handleRegistryGlobalRemove(void *, wl_registry *, uint32_t) {
    if (waylandSeat) {
        wl_seat_destroy(waylandSeat);
    }

    if (textInputManager) {
        zwp_text_input_manager_v3_destroy(textInputManager);
    }

    if (textInput) {
        zwp_text_input_v3_destroy(textInput);
    }
}

static constexpr wl_registry_listener registryListener = {
    .global = handleRegistryGlobal,
    .global_remove = handleRegistryGlobalRemove
};
// Registry End

// text_input_v3 Listeners Start
static void handleTextInputEnter(void *, zwp_text_input_v3 *, wl_surface *) {
}

static void handleTextInputLeave(void *, zwp_text_input_v3 *, wl_surface *) {
}

static void callStringConsumer(JNIEnv *env, const char *methodName, const char *text) {
    auto const fcitx5Class = env->FindClass("cn/xiaym/fcitx5/Fcitx5Wayland");
    auto const consumerId = env->GetStaticFieldID(fcitx5Class, methodName, "Ljava/util/function/Consumer;");
    auto const consumerObject = env->GetStaticObjectField(fcitx5Class, consumerId);
    if (env->IsSameObject(consumerObject, nullptr)) {
        return;
    }

    auto const consumerClass = env->GetObjectClass(consumerObject);
    auto const acceptMethodId = env->GetMethodID(consumerClass, "accept", "(Ljava/lang/Object;)V");
    env->CallVoidMethod(consumerObject, acceptMethodId, env->NewStringUTF(text));

    env->DeleteLocalRef(consumerObject);
}

static void handleTextInputPreeditString(void *data, zwp_text_input_v3 *, const char *text, int32_t, int32_t) {
    auto *env = static_cast<JNIEnv *>(data);
    callStringConsumer(env, "onPreeditString", text);
}

static void handleTextInputCommitString(void *data, zwp_text_input_v3 *, const char *text) {
    auto *env = static_cast<JNIEnv *>(data);
    callStringConsumer(env, "onCommitString", text);
}

static void handleTextInputDeleteSurroundingText(void *, zwp_text_input_v3 *, uint32_t, uint32_t) {
}

static void handleTextInputDone(void *, zwp_text_input_v3 *, uint32_t) {
}

static constexpr zwp_text_input_v3_listener textInputListener = {
    .enter = handleTextInputEnter,
    .leave = handleTextInputLeave,
    .preedit_string = handleTextInputPreeditString,
    .commit_string = handleTextInputCommitString,
    .delete_surrounding_text = handleTextInputDeleteSurroundingText,
    .done = handleTextInputDone
};
// text_input_v3 Listeners End

extern "C" {
JNIEXPORT void JNICALL Java_cn_xiaym_fcitx5_Fcitx5Wayland_initialize(JNIEnv *env, jclass, const jlong displayId) {
    auto *display = reinterpret_cast<wl_display *>(displayId);
    wl_registry_add_listener(wl_display_get_registry(display), &registryListener, nullptr);
    wl_display_dispatch(display);
    wl_display_roundtrip(display);

    auto const fcitx5Class = env->FindClass("cn/xiaym/fcitx5/Fcitx5");
    auto const loggerId = env->GetStaticFieldID(fcitx5Class, "LOGGER", "Lorg/apache/logging/log4j/Logger;");
    auto const loggerObj = env->GetStaticObjectField(fcitx5Class, loggerId);
    auto const loggerClass = env->GetObjectClass(loggerObj);
    auto const warnMethodId = env->GetMethodID(loggerClass, "warn", "(Ljava/lang/Object;)V");

    if (!waylandSeat || !textInputManager) {
        env->CallVoidMethod(loggerObj, warnMethodId, env->NewStringUTF(
                                "Your Wayland compositor doesn't support text-input-v3 protocol, relevant functionalities will be disabled!"));
        return;
    }

    textInput = zwp_text_input_manager_v3_get_text_input(textInputManager, waylandSeat);
    zwp_text_input_v3_add_listener(textInput, &textInputListener, env);

    zwp_text_input_v3_enable(textInput);
    zwp_text_input_v3_commit(textInput);

    env->DeleteLocalRef(loggerObj);
}

JNIEXPORT void JNICALL Java_cn_xiaym_fcitx5_Fcitx5Wayland_updateWindow(JNIEnv *, jclass, jint width, jint height) {
    zwp_text_input_v3_set_cursor_rectangle(textInput, 0, height, width, height);
    zwp_text_input_v3_commit(textInput);
}
}
