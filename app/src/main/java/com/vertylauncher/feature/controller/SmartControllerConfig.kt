package com.vertylauncher.feature.controller

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SmartControllerConfig(private val context: Context) {

    companion object {
        const val DEFAULT_CONFIG_NAME = "default_controller.json"
        const val CONFIG_DIR = "controllers"
    }

    private val configDir: File
        get() = File(context.filesDir, CONFIG_DIR).apply { mkdirs() }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun loadConfig(name: String = DEFAULT_CONFIG_NAME): ControllerProfile {
        val configFile = File(configDir, name)
        return if (configFile.exists()) {
            json.decodeFromString(configFile.readText())
        } else {
            createDefaultConfig().also { saveConfig(it, name) }
        }
    }

    fun saveConfig(profile: ControllerProfile, name: String = DEFAULT_CONFIG_NAME) {
        File(configDir, name).writeText(json.encodeToString(profile))
    }

    fun createDefaultConfig(): ControllerProfile {
        return ControllerProfile(
            name = "Default",
            version = 1,
            screenWidthDp = 360,
            screenHeightDp = 640,
            elements = listOf(
                ControllerElement.Joystick(
                    id = "joystick_left", x = 80f, y = 480f, size = 120f,
                    style = ElementStyle(backgroundColor = "#80000000", foregroundColor = "#FFFFFFFF", opacity = 0.6f, cornerRadius = 60f),
                    mapping = InputMapping(type = InputType.AXIS, keys = listOf("key.keyboard.w", "key.keyboard.a", "key.keyboard.s", "key.keyboard.d")),
                    behavior = JoystickBehavior(deadZone = 0.15f, sensitivity = 1.0f, lockX = false, lockY = false)
                ),
                ControllerElement.Joystick(
                    id = "joystick_right", x = 280f, y = 480f, size = 120f,
                    style = ElementStyle(backgroundColor = "#80000000", foregroundColor = "#FFFFFFFF", opacity = 0.6f, cornerRadius = 60f),
                    mapping = InputMapping(type = InputType.MOUSE, keys = listOf("mouse.move")),
                    behavior = JoystickBehavior(deadZone = 0.1f, sensitivity = 0.8f, lockX = false, lockY = false)
                ),
                ControllerElement.Button(
                    id = "btn_jump", x = 280f, y = 380f, width = 60f, height = 60f,
                    style = ElementStyle(backgroundColor = "#8060A0FF", foregroundColor = "#FFFFFFFF", cornerRadius = 30f, opacity = 0.7f, text = "J"),
                    mapping = InputMapping(type = InputType.KEYBOARD, keys = listOf("key.keyboard.space")),
                    behavior = ButtonBehavior(triggerMode = TriggerMode.HOLD, repeatDelay = 0, hapticFeedback = true)
                ),
                ControllerElement.Button(
                    id = "btn_attack", x = 220f, y = 420f, width = 70f, height = 70f,
                    style = ElementStyle(backgroundColor = "#80FF4040", foregroundColor = "#FFFFFFFF", cornerRadius = 35f, opacity = 0.7f, text = "A"),
                    mapping = InputMapping(type = InputType.MOUSE, keys = listOf("mouse.left")),
                    behavior = ButtonBehavior(triggerMode = TriggerMode.REPEAT, repeatDelay = 150, hapticFeedback = true)
                ),
                ControllerElement.Button(
                    id = "btn_use", x = 340f, y = 420f, width = 70f, height = 70f,
                    style = ElementStyle(backgroundColor = "#8040FF40", foregroundColor = "#FFFFFFFF", cornerRadius = 35f, opacity = 0.7f, text = "U"),
                    mapping = InputMapping(type = InputType.MOUSE, keys = listOf("mouse.right")),
                    behavior = ButtonBehavior(triggerMode = TriggerMode.HOLD, repeatDelay = 0, hapticFeedback = false)
                ),
                ControllerElement.Button(
                    id = "btn_inventory", x = 320f, y = 80f, width = 50f, height = 50f,
                    style = ElementStyle(backgroundColor = "#80FFA040", foregroundColor = "#FFFFFFFF", cornerRadius = 8f, opacity = 0.8f, text = "E"),
                    mapping = InputMapping(type = InputType.KEYBOARD, keys = listOf("key.keyboard.e")),
                    behavior = ButtonBehavior(triggerMode = TriggerMode.TOGGLE, repeatDelay = 0, hapticFeedback = true)
                ),
                ControllerElement.Button(
                    id = "btn_sneak", x = 40f, y = 380f, width = 55f, height = 55f,
                    style = ElementStyle(backgroundColor = "#80606060", foregroundColor = "#FFFFFFFF", cornerRadius = 8f, opacity = 0.6f, text = "S"),
                    mapping = InputMapping(type = InputType.KEYBOARD, keys = listOf("key.keyboard.left.shift")),
                    behavior = ButtonBehavior(triggerMode = TriggerMode.TOGGLE, repeatDelay = 0, hapticFeedback = true)
                ),
                ControllerElement.Button(
                    id = "btn_sprint", x = 100f, y = 380f, width = 55f, height = 55f,
                    style = ElementStyle(backgroundColor = "#80606060", foregroundColor = "#FFFFFFFF", cornerRadius = 8f, opacity = 0.6f, text = "R"),
                    mapping = InputMapping(type = InputType.KEYBOARD, keys = listOf("key.keyboard.left.control")),
                    behavior = ButtonBehavior(triggerMode = TriggerMode.HOLD, repeatDelay = 0, hapticFeedback = false)
                ),
                ControllerElement.GestureZone(
                    id = "gesture_camera", x = 0f, y = 0f, width = 360f, height = 300f,
                    style = ElementStyle(backgroundColor = "#00000000", foregroundColor = "#00000000", opacity = 0.0f),
                    mapping = InputMapping(type = InputType.MOUSE, keys = listOf("mouse.move")),
                    behavior = GestureBehavior(swipeSensitivity = 1.2f, pinchEnabled = true, doubleTapAction = "mouse.middle")
                )
            )
        )
    }

    @Serializable data class ControllerProfile(val name: String, val version: Int, val screenWidthDp: Int, val screenHeightDp: Int, val elements: List<ControllerElement>)
    @Serializable sealed class ControllerElement {
        abstract val id: String; abstract val x: Float; abstract val y: Float; abstract val style: ElementStyle; abstract val mapping: InputMapping
        @Serializable data class Button(override val id: String, override val x: Float, override val y: Float, val width: Float, val height: Float, override val style: ElementStyle, override val mapping: InputMapping, val behavior: ButtonBehavior) : ControllerElement()
        @Serializable data class Joystick(override val id: String, override val x: Float, override val y: Float, val size: Float, override val style: ElementStyle, override val mapping: InputMapping, val behavior: JoystickBehavior) : ControllerElement()
        @Serializable data class DPad(override val id: String, override val x: Float, override val y: Float, val size: Float, override val style: ElementStyle, override val mapping: InputMapping, val behavior: DPadBehavior) : ControllerElement()
        @Serializable data class GestureZone(override val id: String, override val x: Float, override val y: Float, val width: Float, val height: Float, override val style: ElementStyle, override val mapping: InputMapping, val behavior: GestureBehavior) : ControllerElement()
    }
    @Serializable data class ElementStyle(val backgroundColor: String = "#80000000", val foregroundColor: String = "#FFFFFFFF", val borderWidth: Float = 0f, val borderColor: String = "#00000000", val cornerRadius: Float = 0f, val opacity: Float = 1.0f, val icon: String? = null, val text: String? = null)
    @Serializable data class InputMapping(val type: InputType, val keys: List<String>)
    @Serializable enum class InputType { KEYBOARD, MOUSE, AXIS, SCROLL }
    @Serializable data class ButtonBehavior(val triggerMode: TriggerMode, val repeatDelay: Int, val hapticFeedback: Boolean)
    @Serializable enum class TriggerMode { PRESS, HOLD, REPEAT, TOGGLE }
    @Serializable data class JoystickBehavior(val deadZone: Float, val sensitivity: Float, val lockX: Boolean, val lockY: Boolean)
    @Serializable data class DPadBehavior(val diagonalEnabled: Boolean, val repeatRate: Int)
    @Serializable data class GestureBehavior(val swipeSensitivity: Float, val pinchEnabled: Boolean, val doubleTapAction: String? = null)
}
