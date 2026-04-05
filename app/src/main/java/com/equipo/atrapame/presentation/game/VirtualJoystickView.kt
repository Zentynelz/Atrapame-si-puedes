package com.equipo.atrapame.presentation.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.equipo.atrapame.R
import com.equipo.atrapame.data.models.Direction
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * D-Pad isométrico con 4 flechas inclinadas para coincidir con la perspectiva
 * del tablero isométrico.
 *
 * Disposición visual (pantalla):
 *
 *           UP (↗)           ← hacia arriba-derecha en pantalla = fila -1
 *    LEFT (↙)    RIGHT (↗)   ← izquierda=col-1, derecha=col+1
 *          DOWN (↘)          ← hacia abajo-izquierda en pantalla = fila +1
 *
 * Las flechas están rotadas 45° para alinearse con los ejes isométricos.
 *
 * Al mantener presionada una dirección, se envía continuamente esa dirección
 * al listener; al soltar se envía null para detener el movimiento.
 */
class VirtualDPadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ─── Paints ───────────────────────────────────────────────────────────────

    private val armBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val armPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val armBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowIconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ─── State ────────────────────────────────────────────────────────────────

    private var pressedDirection: Direction? = null
    private var directionListener: ((Direction?) -> Unit)? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    // ─── Geometry ─────────────────────────────────────────────────────────────

    private var cx = 0f
    private var cy = 0f
    private var totalSize = 0f

    /**
     * Ángulos isométricos: las 4 direcciones están rotadas 45° respecto a un
     * D-Pad convencional para alinearse con la rejilla isométrica.
     *
     *  UP    → -90° convencional → -45° isométrico  (hacia arriba-derecha)
     *  RIGHT →   0° convencional →  +45° isométrico (hacia abajo-derecha)
     *  DOWN  →  90° convencional → +135° isométrico (hacia abajo-izquierda)
     *  LEFT  → 180° convencional → -135° isométrico (hacia arriba-izquierda)
     */
    private val armAngles = mapOf(
        Direction.UP    to -45f,
        Direction.RIGHT to  45f,
        Direction.DOWN  to 135f,
        Direction.LEFT  to -135f
    )

    private val armPaths  = mutableMapOf<Direction, Path>()
    private val arrowPaths = mutableMapOf<Direction, Path>()
    private val centerPath = Path()

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        isClickable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val cyan     = safeColor(context, R.color.tron_cyan,      Color.CYAN)
        val cyanGlow = safeColor(context, R.color.tron_cyan_glow, Color.CYAN)
        val darkBg   = safeColor(context, R.color.tron_darker_bg, Color.parseColor("#060E1A"))
        val grid     = safeColor(context, R.color.tron_grid,      Color.parseColor("#0D2030"))

        armBgPaint.apply {
            color = darkBg
            style = Paint.Style.FILL
        }

        armPressedPaint.apply {
            color = cyan
            alpha = 170
            style = Paint.Style.FILL
            setShadowLayer(22f, 0f, 0f, cyanGlow)
        }

        armBorderPaint.apply {
            color = cyan
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * resources.displayMetrics.density
            strokeJoin = Paint.Join.ROUND
            setShadowLayer(12f, 0f, 0f, cyanGlow)
        }

        arrowIconPaint.apply {
            color = cyan
            style = Paint.Style.FILL
            setShadowLayer(7f, 0f, 0f, cyanGlow)
        }

        centerFillPaint.apply {
            color = grid
            style = Paint.Style.FILL
        }

        centerBorderPaint.apply {
            color = cyan
            alpha = 90
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * resources.displayMetrics.density
        }

        glowPaint.apply {
            color = cyan
            alpha = 25
            style = Paint.Style.FILL
            setShadowLayer(30f, 0f, 0f, cyanGlow)
        }

        Direction.entries.forEach { armPaths[it] = Path() }
        Direction.entries.forEach { arrowPaths[it] = Path() }
    }

    private fun safeColor(ctx: Context, resId: Int, fallback: Int): Int = try {
        ContextCompat.getColor(ctx, resId)
    } catch (_: Exception) {
        fallback
    }

    // ─── Size & Paths ─────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        totalSize = min(w, h).toFloat()
        cx = w / 2f
        cy = h / 2f
        rebuildAllPaths()
    }

    private fun rebuildAllPaths() {
        val armLen    = totalSize * 0.44f
        val halfWidth = totalSize * 0.165f
        val gapFromCtr = totalSize * 0.135f
        val centerSize = totalSize * 0.135f

        Direction.entries.forEach { dir ->
            val angleDeg = armAngles[dir]!!
            buildArmPath(armPaths[dir]!!, angleDeg, armLen, halfWidth, gapFromCtr)
            buildArrowIconPath(arrowPaths[dir]!!, angleDeg, armLen, halfWidth)
        }

        centerPath.reset()
        centerPath.moveTo(cx, cy - centerSize)
        centerPath.lineTo(cx + centerSize, cy)
        centerPath.lineTo(cx, cy + centerSize)
        centerPath.lineTo(cx - centerSize, cy)
        centerPath.close()
    }

    private fun buildArmPath(
        path: Path,
        angleDeg: Float,
        armLen: Float,
        halfWidth: Float,
        gap: Float
    ) {
        val rad  = Math.toRadians(angleDeg.toDouble())
        val perp = rad + Math.PI / 2

        val dX = cos(rad).toFloat()
        val dY = sin(rad).toFloat()
        val pX = cos(perp).toFloat()
        val pY = sin(perp).toFloat()

        val innerW   = halfWidth * 0.55f
        val arrowWide = halfWidth * 1.0f
        val bodyW    = halfWidth * 0.85f

        fun pt(dist: Float, side: Float) =
            Pair(cx + dX * dist + pX * side, cy + dY * dist + pY * side)

        val baseL     = pt(gap,            +innerW)
        val baseR     = pt(gap,            -innerW)
        val midL      = pt(armLen * 0.50f, +bodyW)
        val midR      = pt(armLen * 0.50f, -bodyW)
        val shoulderL = pt(armLen * 0.68f, +arrowWide)
        val shoulderR = pt(armLen * 0.68f, -arrowWide)
        val tip       = pt(armLen,          0f)

        path.reset()
        path.moveTo(baseL.first,     baseL.second)
        path.lineTo(midL.first,      midL.second)
        path.lineTo(shoulderL.first, shoulderL.second)
        path.lineTo(tip.first,       tip.second)
        path.lineTo(shoulderR.first, shoulderR.second)
        path.lineTo(midR.first,      midR.second)
        path.lineTo(baseR.first,     baseR.second)
        path.close()
    }

    private fun buildArrowIconPath(
        path: Path,
        angleDeg: Float,
        armLen: Float,
        halfWidth: Float
    ) {
        val rad  = Math.toRadians(angleDeg.toDouble())
        val perp = rad + Math.PI / 2

        val dX = cos(rad).toFloat()
        val dY = sin(rad).toFloat()
        val pX = cos(perp).toFloat()
        val pY = sin(perp).toFloat()

        val iconSize = halfWidth * 0.32f
        val iconDist = armLen * 0.60f

        val tip = Pair(
            cx + dX * (iconDist + iconSize),
            cy + dY * (iconDist + iconSize)
        )
        val bL = Pair(
            cx + dX * (iconDist - iconSize) + pX * iconSize,
            cy + dY * (iconDist - iconSize) + pY * iconSize
        )
        val bR = Pair(
            cx + dX * (iconDist - iconSize) - pX * iconSize,
            cy + dY * (iconDist - iconSize) - pY * iconSize
        )

        path.reset()
        path.moveTo(tip.first, tip.second)
        path.lineTo(bL.first, bL.second)
        path.lineTo(bR.first, bR.second)
        path.close()
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (totalSize <= 0f) return

        Direction.entries.forEach { dir ->
            val path = armPaths[dir] ?: return@forEach
            val isDown = pressedDirection == dir

            if (isDown) canvas.drawPath(path, glowPaint)
            canvas.drawPath(path, if (isDown) armPressedPaint else armBgPaint)
            canvas.drawPath(path, armBorderPaint)

            val iconPaint = if (isDown) {
                Paint(arrowIconPaint).apply { color = Color.WHITE; clearShadowLayer() }
            } else {
                arrowIconPaint
            }
            arrowPaths[dir]?.let { canvas.drawPath(it, iconPaint) }
        }

        canvas.drawPath(centerPath, centerFillPaint)
        canvas.drawPath(centerPath, centerBorderPaint)
    }

    // ─── Touch ────────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                parent?.requestDisallowInterceptTouchEvent(true)
                processTouch(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) processTouch(event.getX(idx), event.getY(idx))
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
                notifyDirectionChange(null)          // ← soltar = detener movimiento
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    notifyDirectionChange(null)
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

    /**
     * Determina la dirección isométrica según el ángulo del toque respecto
     * al centro del D-Pad.
     *
     * Los sectores están rotados 45° respecto a un D-Pad convencional:
     *   315°..45°   → UP    (↗ arriba-derecha en pantalla)
     *    45°..135°  → RIGHT (↘ abajo-derecha)
     *   135°..225°  → DOWN  (↙ abajo-izquierda)
     *   225°..315°  → LEFT  (↖ arriba-izquierda)
     */
    private fun processTouch(x: Float, y: Float) {
        val dx = x - cx
        val dy = y - cy
        val dist = hypot(dx, dy)

        // Zona muerta central
        if (dist < totalSize * 0.11f) {
            notifyDirectionChange(null)
            return
        }

        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val norm = (angleDeg + 360f) % 360f

        // Sectores de 90° alineados con la perspectiva isométrica
        val direction = when {
            norm < 90f  -> Direction.RIGHT   // 0°..90°
            norm < 180f -> Direction.DOWN    // 90°..180°
            norm < 270f -> Direction.LEFT    // 180°..270°
            else        -> Direction.UP      // 270°..360°
        }

        notifyDirectionChange(direction)
    }

    private fun notifyDirectionChange(direction: Direction?) {
        if (direction != pressedDirection) {
            pressedDirection = direction
            directionListener?.invoke(direction)     // El ViewModel maneja el loop continuo
            invalidate()
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setOnDirectionListener(listener: (Direction?) -> Unit) {
        directionListener = listener
    }
}