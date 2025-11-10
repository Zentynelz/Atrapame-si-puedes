package com.equipo.atrapame.presentation.game.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.equipo.atrapame.R
import com.equipo.atrapame.data.models.Direction
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class VirtualJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.joystick_base)
        style = Paint.Style.FILL
    }

    private val baseBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
        alpha = 150
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.joystick_handle)
        style = Paint.Style.FILL
    }

    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
        alpha = 90
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var handleRadius = 0f
    private var handleX = 0f
    private var handleY = 0f

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var currentDirection: Direction? = null
    private var directionListener: ((Direction?) -> Unit)? = null

    init {
        isClickable = true
        if (contentDescription == null) {
            contentDescription = context.getString(R.string.joystick_content_description)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val diameter = min(w, h).toFloat()
        baseRadius = diameter / 2f * 0.9f
        handleRadius = baseRadius * 0.35f
        centerX = w / 2f
        centerY = h / 2f
        resetHandle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseRadius <= 0f) return

        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, baseBorderPaint)

        canvas.drawLine(centerX - baseRadius, centerY, centerX + baseRadius, centerY, guidePaint)
        canvas.drawLine(centerX, centerY - baseRadius, centerX, centerY + baseRadius, guidePaint)

        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                parent?.requestDisallowInterceptTouchEvent(true)
                updateHandlePosition(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) {
                    updateHandlePosition(event.getX(pointerIndex), event.getY(pointerIndex))
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetHandle()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == activePointerId) {
                    resetHandle()
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setOnDirectionListener(listener: (Direction?) -> Unit) {
        directionListener = listener
    }

    private fun resetHandle() {
        handleX = centerX
        handleY = centerY
        activePointerId = MotionEvent.INVALID_POINTER_ID
        notifyDirectionChange(null)
        invalidate()
    }

    private fun updateHandlePosition(x: Float, y: Float) {
        val dx = x - centerX
        val dy = y - centerY

        val distance = hypot(dx, dy)
        val clampedDistance = min(distance, baseRadius)
        val ratio = if (distance > 0f) clampedDistance / distance else 0f

        handleX = centerX + dx * ratio
        handleY = centerY + dy * ratio

        val direction = resolveDirection(dx, dy, clampedDistance / max(baseRadius, 1f))
        notifyDirectionChange(direction)

        invalidate()
    }

    private fun resolveDirection(dx: Float, dy: Float, normalizedDistance: Float): Direction? {
        val activationThreshold = 0.35f
        if (normalizedDistance < activationThreshold) {
            return null
        }

        return if (abs(dx) > abs(dy)) {
            if (dx > 0f) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0f) Direction.DOWN else Direction.UP
        }
    }

    private fun notifyDirectionChange(direction: Direction?) {
        if (direction != currentDirection) {
            currentDirection = direction
            directionListener?.invoke(direction)
        }
    }
}