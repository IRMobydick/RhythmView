package com.github.sumimakito.rhythmview.effect

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.github.sumimakito.rhythmview.RhythmView
import com.github.sumimakito.rhythmview.util.MathUtils
import com.github.sumimakito.rhythmview.wave.WavePoint
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A preset visual effect.
 *
 * Resolution of the data source should not be less than `resolution` here.
 *
 * When using with `PlaybackSource`, parameter `resolution` should not be larger than 1024, or the
 * capture size may exceeded the maximum capture size of the system.
 */
class RainbowRay @JvmOverloads constructor(rhythmView: RhythmView, private val resolution: Int = 256, private val waveSpeed: Float = 0.04f) : BaseEffect<Int>(rhythmView) {

    var color: Int = 0xffef9a9a.toInt()
    var alpha: Float = 0.65f
    private var frameId = 0
    private var wavePoints = ArrayList<WavePoint>()
    private var outerPoints = ArrayList<PointF>()
    private var innerPoints = ArrayList<PointF>()
    private var paintRay = Paint()
    private var delta: Float
    private val hsv = FloatArray(3)

    init {
        if (resolution < 4) throw RuntimeException("Division should be an integer larger than 4.")
        delta = 360f / resolution
        for (i in 0 until resolution) {
            wavePoints.add(WavePoint(0f, waveSpeed, 0f, 1f))
        }

        paintRay.isAntiAlias = true
        paintRay.style = Paint.Style.STROKE
        paintRay.strokeWidth = 3f

        computePoints()
    }

    private fun refillWave(wave: Array<Int>) {
        for (i in 0 until min(wavePoints.size, wave.size)) {
            wavePoints[i].changeTo(wave[i] / 256f)
        }
    }

    override fun onFrameRendered() {
        if (frameId % 2 == 0) {
            if (dataSource != null && dataSource!!.data != null) refillWave(dataSource!!.data!!)
        }

        for (wavePoint in wavePoints) {
            wavePoint.nextTick()
        }
        computePoints()

        frameId++
        if (frameId > 2) {
            frameId = 0
        }
    }

    override fun render(canvas: Canvas) {
        paintRay.alpha = floor(255f * alpha).toInt()

        for (i in 0 until resolution) {
            val start = innerPoints[i]
            val stop = outerPoints[i]
            canvas.drawLine(start.x, start.y, stop.x, stop.y, paintRay)

            /**
             * Apply color magic! Rua!
             */
            Color.colorToHSV(color, hsv)
            hsv[0] += max(1f, i.toFloat() / resolution * 360f)
            while (hsv[0] >= 360) hsv[0] -= 360f
            paintRay.color = Color.HSVToColor(hsv)
        }
    }

    private fun computePoints() {
        outerPoints.clear()
        for (i in 0 until wavePoints.size) {
            val deg = (i % 360) * delta
            innerPoints.add(MathUtils.getPointOnCircle(centerX, centerY, minDrawingRadius, deg))
            outerPoints.add(MathUtils.getPointOnCircle(centerX, centerY, minDrawingRadius + getWaveHeight(i), deg))
        }
    }

    private fun getWaveHeight(index: Int): Float {
        if (index < wavePoints.size) {
            return min(1f, max(0f, wavePoints[index].displayValue)) * maxDrawingWidth
        }
        return 0f
    }
}