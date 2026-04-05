package com.equipo.atrapame.presentation.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.equipo.atrapame.R
import com.equipo.atrapame.data.models.CellType
import com.equipo.atrapame.data.models.GameState
import com.equipo.atrapame.data.models.Position
import kotlin.math.min

class IsometricBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gameState: GameState? = null

    private var tileWidth: Float = 0f
    private var tileHeight: Float = 0f
    private var obstacleHeight: Float = 0f
    private var originX: Float = 0f
    private var originY: Float = 0f

    private val tilePath = Path()
    private val obstaclePath = Path()

    private val tileLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cell_empty)
        style = Paint.Style.FILL
    }

    private val tileDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_darker_bg)
        style = Paint.Style.FILL
    }

    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_cyan)
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        setShadowLayer(4f, 0f, 0f, ContextCompat.getColor(context, R.color.tron_cyan_glow))
    }

    private val obstacleTopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(
            ContextCompat.getColor(context, R.color.tron_cyan),
            Color.BLACK,
            0.3f // Color techo cyan brillante
        )
        style = Paint.Style.FILL
    }

    private val obstacleLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(
            ContextCompat.getColor(context, R.color.tron_cyan),
            Color.BLACK,
            0.5f // Color pared izquierda cyan oscuro
        )
        style = Paint.Style.FILL
    }

    private val obstacleRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.blendARGB(
            ContextCompat.getColor(context, R.color.tron_cyan),
            Color.BLACK,
            0.7f // Color pared derecha un poco más oscura para 3D
        )
        style = Paint.Style.FILL
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_cyan)
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 0f, ContextCompat.getColor(context, R.color.tron_cyan_glow))
    }

    private val enemyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_orange)
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 0f, ContextCompat.getColor(context, R.color.tron_orange_glow))
    }

    private val entityBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_cyan_glow)
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val entityShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ColorUtils.setAlphaComponent(Color.BLACK, 60)
        style = Paint.Style.FILL
    }

    private val axisPaintX = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_cyan)
        strokeWidth = dp(1.5f)
        alpha = 100
    }

    private val axisPaintY = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_orange)
        strokeWidth = dp(1.5f)
        alpha = 100
    }

    private val axisPaintZ = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_purple)
        strokeWidth = dp(1.5f)
        alpha = 100
    }

    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tron_cyan_glow)
        textSize = sp(10f)
        style = Paint.Style.FILL
        typeface = android.graphics.Typeface.MONOSPACE
        alpha = 150
    }

    fun setGameState(state: GameState) {
        if (gameState == state) return
        gameState = state
        computeTileMetrics()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeTileMetrics()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val state = gameState ?: return
        if (tileWidth <= 0f || tileHeight <= 0f) return

        drawAxes(canvas, state)
        drawTiles(canvas, state)
        drawEntities(canvas, state)
    }

    private fun drawTiles(canvas: Canvas, state: GameState) {
        val rows = state.rows
        val cols = state.cols

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val base = logicToScreen(Position(row, col))
                val paint = if ((row + col) % 2 == 0) tileLightPaint else tileDarkPaint
                drawTile(canvas, base, paint)

                when (state.board[row][col]) {
                    CellType.OBSTACLE -> drawObstacle(canvas, base)
                    else -> Unit
                }
            }
        }
    }

    private fun drawEntities(canvas: Canvas, state: GameState) {
        val entities = listOf(
            state.playerPosition to playerPaint,
            state.enemyPosition to enemyPaint
        ).sortedBy { (position, _) -> position.row + position.col }

        for ((position, paint) in entities) {
            val base = logicToScreen(position)
            drawEntity(canvas, base, paint)
        }
    }

    private fun drawTile(canvas: Canvas, base: PointF, paint: Paint) {
        val halfWidth = tileWidth / 2f
        val halfHeight = tileHeight / 2f

        tilePath.reset()
        tilePath.moveTo(base.x, base.y)
        tilePath.lineTo(base.x + halfWidth, base.y + halfHeight)
        tilePath.lineTo(base.x, base.y + tileHeight)
        tilePath.lineTo(base.x - halfWidth, base.y + halfHeight)
        tilePath.close()

        canvas.drawPath(tilePath, paint)
        canvas.drawPath(tilePath, gridLinePaint)
    }

    private fun drawObstacle(canvas: Canvas, base: PointF) {
        val halfWidth = tileWidth / 2f
        val halfHeight = tileHeight / 2f
        val topY = base.y - obstacleHeight

        // Draw left side
        obstaclePath.reset()
        obstaclePath.moveTo(base.x - halfWidth, base.y + halfHeight)
        obstaclePath.lineTo(base.x - halfWidth, topY + halfHeight)
        obstaclePath.lineTo(base.x, topY + tileHeight)
        obstaclePath.lineTo(base.x, base.y + tileHeight)
        obstaclePath.close()
        canvas.drawPath(obstaclePath, obstacleLeftPaint)

        // Draw right side
        obstaclePath.reset()
        obstaclePath.moveTo(base.x + halfWidth, base.y + halfHeight)
        obstaclePath.lineTo(base.x + halfWidth, topY + halfHeight)
        obstaclePath.lineTo(base.x, topY + tileHeight)
        obstaclePath.lineTo(base.x, base.y + tileHeight)
        obstaclePath.close()
        canvas.drawPath(obstaclePath, obstacleRightPaint)

        // Draw top
        tilePath.reset()
        tilePath.moveTo(base.x, topY)
        tilePath.lineTo(base.x + halfWidth, topY + halfHeight)
        tilePath.lineTo(base.x, topY + tileHeight)
        tilePath.lineTo(base.x - halfWidth, topY + halfHeight)
        tilePath.close()
        canvas.drawPath(tilePath, obstacleTopPaint)
        canvas.drawPath(tilePath, gridLinePaint)
    }

    private fun drawEntity(canvas: Canvas, base: PointF, paint: Paint) {
        val centerX = base.x
        val centerY = base.y + tileHeight * 0.6f
        val radius = tileWidth * 0.18f

        val shadowRect = RectF(
            centerX - radius * 1.4f,
            base.y + tileHeight - radius * 0.3f,
            centerX + radius * 1.4f,
            base.y + tileHeight + radius * 0.3f
        )
        canvas.drawOval(shadowRect, entityShadowPaint)

        canvas.drawCircle(centerX, centerY, radius, paint)
        canvas.drawCircle(centerX, centerY, radius, entityBorderPaint)
    }

    private fun drawAxes(canvas: Canvas, state: GameState) {
        val base = logicToScreen(Position(0, 0))

        val axisLength = tileWidth * (state.rows + state.cols) / 4f

        // X axis (towards positive columns)
        canvas.drawLine(
            base.x,
            base.y,
            base.x + axisLength,
            base.y + axisLength * tileHeight / tileWidth,
            axisPaintX
        )
        canvas.drawText("X", base.x + axisLength + dp(4f), base.y + axisLength * tileHeight / tileWidth, axisTextPaint)

        // Y axis (towards positive rows)
        canvas.drawLine(
            base.x,
            base.y,
            base.x - axisLength,
            base.y + axisLength * tileHeight / tileWidth,
            axisPaintY
        )
        canvas.drawText("Y", base.x - axisLength - dp(12f), base.y + axisLength * tileHeight / tileWidth, axisTextPaint)

        // Z axis (height)
        canvas.drawLine(
            base.x,
            base.y,
            base.x,
            base.y - axisLength * 0.75f,
            axisPaintZ
        )
        canvas.drawText("Z", base.x + dp(4f), base.y - axisLength * 0.75f - dp(4f), axisTextPaint)
    }

    private fun logicToScreen(position: Position): PointF {
        val screenX = originX + (position.col - position.row) * tileWidth / 2f
        val screenY = originY + (position.col + position.row) * tileHeight / 2f
        return PointF(screenX, screenY)
    }

    private fun computeTileMetrics() {
        val state = gameState ?: return
        val rows = state.rows
        val cols = state.cols
        if (rows == 0 || cols == 0) return

        val availableWidth = (width - paddingLeft - paddingRight).toFloat()
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        if (availableWidth <= 0f || availableHeight <= 0f) return

        val tileWidthFromWidth = availableWidth * 2f / (rows + cols)
        val tileHeightFromHeight = availableHeight * 2f / (rows + cols + 2f)
        tileWidth = min(tileWidthFromWidth, tileHeightFromHeight * 2f)
        tileHeight = tileWidth / 2f
        obstacleHeight = tileHeight * 1.5f // Hacer paredes un 50% más altas para que no se pierdan

        val boardWidth = (rows + cols) * tileWidth / 2f
        val boardHeight = (rows + cols) * tileHeight / 2f

        val horizontalOffset = paddingLeft + (availableWidth - boardWidth) / 2f + rows * tileWidth / 2f
        val verticalOffset = paddingTop + (availableHeight - (boardHeight + obstacleHeight)) / 2f

        originX = horizontalOffset
        originY = verticalOffset
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
    }
}