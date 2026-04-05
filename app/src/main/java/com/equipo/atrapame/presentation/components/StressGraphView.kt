package com.equipo.atrapame.presentation.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class StressGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>() // Valores de 0f a 1f
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00F4FF") // Cyan Neón
        style = Paint.Style.STROKE
        strokeWidth = 6f
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#00F4FF"))
    }
    
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300F4FF") // Cyan con transparencia
        style = Paint.Style.FILL
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44FFFFFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val path = Path()
    private val fillPath = Path()

    fun setData(timelineMapList: List<Map<String, Any>>) {
        dataPoints.clear()
        // Suponemos que la "probabilidad de estrés" se mapeó en maxAudioAmplitude u otra combinada. 
        // Generaremos el Stress Score a base de (audio + anti-sonrisa) o directamente de un campo.
        // Simularemos o calcularemos un valor general 0f - 1f por cada segundo.
        for (item in timelineMapList) {
            val smile = (item["smilingProb"] as? Number)?.toFloat() ?: 0f
            val audio = (item["audioAmplitude"] as? Number)?.toFloat() ?: 0f
            // Fórmula simple: Mucho audio y poca sonrisa = Alto estrés (maximo audio ~25000 ref, limitémoslo)
            var stress = 0f
            if (audio > 2000) stress += 0.5f else stress += (audio / 4000f)
            stress += (1f - smile) * 0.5f
            if (stress > 1f) stress = 1f
            dataPoints.add(stress)
        }
        
        postInvalidate() // Redibujar
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required para Blur/Shadow (Neón)

        val w = width.toFloat()
        val h = height.toFloat()

        // Dibujar Grid central
        canvas.drawLine(0f, h/2f, w, h/2f, gridPaint)
        canvas.drawLine(0f, 0f, w, 0f, gridPaint)
        canvas.drawLine(0f, h, w, h, gridPaint)

        if (dataPoints.isEmpty()) return

        path.reset()
        fillPath.reset()

        val stepX = w / (dataPoints.size - 1).coerceAtLeast(1).toFloat()
        
        // Empezar paths
        path.moveTo(0f, h - (dataPoints[0] * h))
        fillPath.moveTo(0f, h)
        fillPath.lineTo(0f, h - (dataPoints[0] * h))

        for (i in 1 until dataPoints.size) {
            val x = i * stepX
            val y = h - (dataPoints[i] * h)
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
