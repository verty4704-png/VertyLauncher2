package com.vertylauncher.feature.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withSave
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class SmartControllerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val elements = mutableListOf<RenderedElement>()
    private val activePointers = mutableMapOf<Int, ActivePointer>()

    var onInputEvent: ((InputEvent) -> Unit)? = null
    var config: SmartControllerConfig.ControllerProfile? = null
        set(value) { field = value; value?.let { loadElements(it) }; invalidate() }

    private fun loadElements(profile: SmartControllerConfig.ControllerProfile) {
        elements.clear()
        val scaleX = width / profile.screenWidthDp.toFloat()
        val scaleY = height / profile.screenHeightDp.toFloat()
        profile.elements.forEach { elements.add(RenderedElement(it, scaleX, scaleY)) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        config?.let { loadElements(it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx); val y = event.getY(idx)
                elements.forEach { el ->
                    if (el.contains(x, y) && !el.isActive) {
                        el.isActive = true; el.activePointerId = pid
                        activePointers[pid] = ActivePointer(el, x, y)
                        handlePress(el, x, y)
                        return@forEach
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val x = event.getX(i); val y = event.getY(i)
                    activePointers[pid]?.let { handleMove(it, x, y) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                activePointers[pid]?.let { ptr ->
                    ptr.element.isActive = false; ptr.element.activePointerId = -1
                    handleRelease(ptr); activePointers.remove(pid)
                }
            }
        }
        invalidate(); return true
    }

    private fun handlePress(el: RenderedElement, x: Float, y: Float) {
        when (val e = el.element) {
            is SmartControllerConfig.ControllerElement.Button -> {
                if (e.behavior.triggerMode == SmartControllerConfig.TriggerMode.TOGGLE) el.toggleState = !el.toggleState
                emitEvent(InputEvent.KeyPress(e.mapping.keys.first(), el.toggleState))
            }
            is SmartControllerConfig.ControllerElement.Joystick -> { el.joystickX = x; el.joystickY = y }
            else -> {}
        }
    }

    private fun handleMove(ptr: ActivePointer, x: Float, y: Float) {
        when (val e = ptr.element.element) {
            is SmartControllerConfig.ControllerElement.Joystick -> {
                val dx = x - ptr.element.scaledX; val dy = y - ptr.element.scaledY
                val maxDist = ptr.element.scaledSize / 2
                val dist = hypot(dx, dy).coerceAtMost(maxDist)
                val angle = atan2(dy, dx)
                ptr.element.joystickX = ptr.element.scaledX + cos(angle) * dist
                ptr.element.joystickY = ptr.element.scaledY + sin(angle) * dist
                val normX = (cos(angle) * dist / maxDist).coerceIn(-1f, 1f)
                val normY = (sin(angle) * dist / maxDist).coerceIn(-1f, 1f)
                if (e.mapping.type == SmartControllerConfig.InputType.MOUSE) {
                    emitEvent(InputEvent.MouseMove(normX * e.behavior.sensitivity, normY * e.behavior.sensitivity))
                } else {
                    emitEvent(InputEvent.AxisMove(e.mapping.keys, normX, normY))
                }
            }
            else -> {}
        }
    }

    private fun handleRelease(ptr: ActivePointer) {
        when (val e = ptr.element.element) {
            is SmartControllerConfig.ControllerElement.Button -> {
                if (e.behavior.triggerMode != SmartControllerConfig.TriggerMode.TOGGLE) {
                    emitEvent(InputEvent.KeyRelease(e.mapping.keys.first()))
                }
            }
            is SmartControllerConfig.ControllerElement.Joystick -> {
                ptr.element.joystickX = ptr.element.scaledX; ptr.element.joystickY = ptr.element.scaledY
                emitEvent(InputEvent.AxisMove(e.mapping.keys, 0f, 0f))
            }
            else -> {}
        }
    }

    private fun emitEvent(e: InputEvent) { onInputEvent?.invoke(e) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        elements.forEach { drawElement(canvas, it) }
    }

    private fun drawElement(canvas: Canvas, r: RenderedElement) {
        canvas.withSave {
            when (val e = r.element) {
                is SmartControllerConfig.ControllerElement.Button -> drawButton(canvas, r, e)
                is SmartControllerConfig.ControllerElement.Joystick -> drawJoystick(canvas, r, e)
                is SmartControllerConfig.ControllerElement.DPad -> drawDPad(canvas, r, e)
                else -> {}
            }
        }
    }

    private fun drawButton(canvas: Canvas, r: RenderedElement, e: SmartControllerConfig.ControllerElement.Button) {
        val s = e.style
        val rect = RectF(r.scaledX, r.scaledY, r.scaledX + r.scaledWidth, r.scaledY + r.scaledHeight)
        val pressed = r.isActive || (e.behavior.triggerMode == SmartControllerConfig.TriggerMode.TOGGLE && r.toggleState)
        val alpha = ((if (pressed) 1.0f else s.opacity) * 255).toInt()
        paint.color = parseColor(s.backgroundColor, alpha)
        canvas.drawRoundRect(rect, s.cornerRadius, s.cornerRadius, paint)
        if (s.borderWidth > 0) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = s.borderWidth
            paint.color = parseColor(s.borderColor, alpha)
            canvas.drawRoundRect(rect, s.cornerRadius, s.cornerRadius, paint)
            paint.style = Paint.Style.FILL
        }
        paint.color = parseColor(s.foregroundColor, alpha)
        paint.textSize = r.scaledHeight * 0.4f; paint.textAlign = Paint.Align.CENTER
        canvas.drawText(e.style.text ?: e.id, rect.centerX(), rect.centerY() + paint.textSize / 3, paint)
    }

    private fun drawJoystick(canvas: Canvas, r: RenderedElement, e: SmartControllerConfig.ControllerElement.Joystick) {
        val s = e.style; val alpha = (s.opacity * 255).toInt()
        paint.color = parseColor(s.backgroundColor, alpha)
        canvas.drawCircle(r.scaledX, r.scaledY, r.scaledSize / 2, paint)
        paint.color = parseColor(s.foregroundColor, alpha)
        canvas.drawCircle(r.joystickX, r.joystickY, r.scaledSize / 4, paint)
    }

    private fun drawDPad(canvas: Canvas, r: RenderedElement, e: SmartControllerConfig.ControllerElement.DPad) {
        val s = e.style; val alpha = (s.opacity * 255).toInt()
        paint.color = parseColor(s.backgroundColor, alpha)
        val c = r.scaledSize / 2; val t = r.scaledSize / 3
        canvas.drawRect(r.scaledX - t/2, r.scaledY - c, r.scaledX + t/2, r.scaledY + c, paint)
        canvas.drawRect(r.scaledX - c, r.scaledY - t/2, r.scaledX + c, r.scaledY + t/2, paint)
    }

    private fun parseColor(hex: String, alpha: Int): Int {
        val c = Color.parseColor(hex)
        return (alpha shl 24) or (c and 0x00FFFFFF)
    }

    private inner class RenderedElement(val element: SmartControllerConfig.ControllerElement, scaleX: Float, scaleY: Float) {
        val scaledX = element.x * scaleX; val scaledY = element.y * scaleY
        val scaledSize = when (element) { is SmartControllerConfig.ControllerElement.Joystick -> element.size * scaleX; is SmartControllerConfig.ControllerElement.DPad -> element.size * scaleX; else -> 0f }
        val scaledWidth = when (element) { is SmartControllerConfig.ControllerElement.Button -> element.width * scaleX; is SmartControllerConfig.ControllerElement.GestureZone -> element.width * scaleX; else -> scaledSize }
        val scaledHeight = when (element) { is SmartControllerConfig.ControllerElement.Button -> element.height * scaleY; is SmartControllerConfig.ControllerElement.GestureZone -> element.height * scaleY; else -> scaledSize }
        var isActive = false; var activePointerId = -1; var toggleState = false
        var joystickX = scaledX; var joystickY = scaledY
        fun contains(x: Float, y: Float): Boolean = when (element) {
            is SmartControllerConfig.ControllerElement.Joystick, is SmartControllerConfig.ControllerElement.DPad -> hypot(x - scaledX, y - scaledY) <= scaledSize / 2
            else -> x >= scaledX && x <= scaledX + scaledWidth && y >= scaledY && y <= scaledY + scaledHeight
        }
    }

    private data class ActivePointer(val element: RenderedElement, val startX: Float, val startY: Float)

    sealed class InputEvent {
        data class KeyPress(val key: String, val state: Boolean = true) : InputEvent()
        data class KeyRelease(val key: String) : InputEvent()
        data class MouseMove(val deltaX: Float, val deltaY: Float) : InputEvent()
        data class AxisMove(val keys: List<String>, val x: Float, val y: Float) : InputEvent()
        data class Scroll(val direction: Float) : InputEvent()
    }
}
