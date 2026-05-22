package com.vertylauncher.feature.game

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.vertylauncher.feature.controller.SmartControllerConfig
import com.vertylauncher.feature.controller.SmartControllerView

class GameActivity : AppCompatActivity() {

    private lateinit var surfaceView: LwjglSurfaceView
    private lateinit var controllerView: SmartControllerView
    private lateinit var controllerConfig: SmartControllerConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val versionId = intent.getStringExtra("version_id") ?: "1.20.1"
        val username = intent.getStringExtra("username") ?: "Player"

        controllerConfig = SmartControllerConfig(this)

        val frame = FrameLayout(this)
        surfaceView = LwjglSurfaceView(this)
        frame.addView(surfaceView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        controllerView = SmartControllerView(this).apply {
            config = controllerConfig.loadConfig()
            onInputEvent = { event -> handleControllerInput(event) }
        }
        frame.addView(controllerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        setContentView(frame)
    }

    private fun handleControllerInput(event: SmartControllerView.InputEvent) {
        when (event) {
            is SmartControllerView.InputEvent.KeyPress -> NativeBridge.sendKeyEvent(mapKeyToLwjgl(event.key), event.state)
            is SmartControllerView.InputEvent.KeyRelease -> NativeBridge.sendKeyEvent(mapKeyToLwjgl(event.key), false)
            is SmartControllerView.InputEvent.MouseMove -> NativeBridge.sendMouseMove(event.deltaX, event.deltaY)
            is SmartControllerView.InputEvent.AxisMove -> {}
            else -> {}
        }
    }

    private fun mapKeyToLwjgl(key: String): Int {
        return when {
            key.contains("space") -> 32
            key.contains("w") -> 87
            key.contains("a") -> 65
            key.contains("s") -> 83
            key.contains("d") -> 68
            key.contains("e") -> 69
            key.contains("shift") -> 16
            key.contains("control") -> 17
            else -> key.lastOrNull()?.code ?: 0
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
