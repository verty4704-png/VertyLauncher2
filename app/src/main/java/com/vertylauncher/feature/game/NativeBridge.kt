package com.vertylauncher.feature.game

import android.view.Surface

object NativeBridge {
    init {
        System.loadLibrary("launcher_bridge")
    }

    @JvmStatic external fun setSurface(surface: Surface)
    @JvmStatic external fun releaseSurface()
    @JvmStatic external fun onSurfaceChanged(width: Int, height: Int)
    @JvmStatic external fun sendKeyEvent(keyCode: Int, pressed: Boolean)
    @JvmStatic external fun sendMouseMove(deltaX: Float, deltaY: Float)
    @JvmStatic external fun sendMouseButton(button: Int, pressed: Boolean)
    @JvmStatic external fun startJVM(args: Array<String>): Int
    @JvmStatic external fun stopJVM()
}
