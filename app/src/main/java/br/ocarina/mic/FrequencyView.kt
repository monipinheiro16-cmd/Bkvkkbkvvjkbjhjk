package br.ocarina.mic

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.ln

class FrequencyView(c: Context) : View(c) {
    private val hist = FloatArray(150)
    private var rms = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    fun push(hz: Float, r: Float) { System.arraycopy(hist, 1, hist, 0, hist.size - 1); hist[hist.size - 1] = hz; rms = r; postInvalidate() }
    private fun y(hz: Float): Float { val a = ln(230f); val b = ln(720f); return height * (1f - (ln(hz) - a) / (b - a)) }
    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF15151F.toInt())
        p.textSize = 26f
        for (n in OcaNote.values()) {
            p.color = 0x55FFFFFF; p.strokeWidth = 2f; c.drawLine(0f, y(n.hz), width.toFloat(), y(n.hz), p)
            p.color = 0xFFB0B0C0.toInt(); c.drawText(n.rotulo, 8f, y(n.hz) - 6f, p)
        }
        p.color = 0xFF4CD964.toInt(); p.strokeWidth = 5f
        val dx = width / hist.size.toFloat()
        for (i in 1 until hist.size) if (hist[i] > 0 && hist[i - 1] > 0) c.drawLine((i - 1) * dx, y(hist[i - 1]), i * dx, y(hist[i]), p)
        p.color = 0xFFFFC107.toInt(); c.drawRect(0f, height - 10f, (rms * 6f).coerceIn(0f, 1f) * width, height.toFloat(), p)
    }
}
