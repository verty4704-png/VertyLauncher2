#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VertyLauncher", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VertyLauncher", __VA_ARGS__)

static ANativeWindow *g_window = NULL;

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_setSurface(JNIEnv *env, jclass clazz, jobject surface) {
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = NULL;
    }
    if (surface) {
        g_window = ANativeWindow_fromSurface(env, surface);
        LOGI("Surface acquired: %p", g_window);
    }
}

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_releaseSurface(JNIEnv *env, jclass clazz) {
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = NULL;
        LOGI("Surface released");
    }
}

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_onSurfaceChanged(JNIEnv *env, jclass clazz, jint width, jint height) {
    LOGI("Surface changed: %dx%d", width, height);
}

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_sendKeyEvent(JNIEnv *env, jclass clazz, jint keyCode, jboolean pressed) {
    LOGI("Key event: code=%d pressed=%d", keyCode, pressed);
}

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_sendMouseMove(JNIEnv *env, jclass clazz, jfloat deltaX, jfloat deltaY) {
    LOGI("Mouse move: %.2f %.2f", deltaX, deltaY);
}

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_sendMouseButton(JNIEnv *env, jclass clazz, jint button, jboolean pressed) {
    LOGI("Mouse button: %d pressed=%d", button, pressed);
}

JNIEXPORT jint JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_startJVM(JNIEnv *env, jclass clazz, jobjectArray args) {
    LOGI("JVM start requested with args");
    jsize len = (*env)->GetArrayLength(env, args);
    for (jsize i = 0; i < len; i++) {
        jstring str = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char *arg = (*env)->GetStringUTFChars(env, str, NULL);
        LOGI("  arg[%d] = %s", i, arg);
        (*env)->ReleaseStringUTFChars(env, str, arg);
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_vertylauncher_feature_game_NativeBridge_stopJVM(JNIEnv *env, jclass clazz) {
    LOGI("JVM stop requested");
}
