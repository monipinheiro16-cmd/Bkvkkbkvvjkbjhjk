package br.ocarina.mic

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

/** Injeta toques longos nos botões virtuais do emulador: segura enquanto a nota soa. */
class OcarinaAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: OcarinaAccessibilityService? = null }
    private class Hold(val x: Float, val y: Float) { var stroke: StrokeDescription? = null; var soltar = false; var fim = false }
    private var hold: Hold? = null
    private val h = Handler(Looper.getMainLooper())

    override fun onServiceConnected() { instance = this; Prefs.init(this) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(i: android.content.Intent?): Boolean { instance = null; return super.onUnbind(i) }

    private fun caminho(x: Float, y: Float) = Path().apply { moveTo(x, y) }

    fun apertar(x: Float, y: Float) = h.post {
        soltar()
        val novo = Hold(x, y); hold = novo
        enviar(novo, StrokeDescription(caminho(x, y), 0, 80, true))
    }
    fun soltar() { hold?.soltar = true }

    private fun enviar(k: Hold, s: StrokeDescription) {
        k.stroke = s
        dispatchGesture(GestureDescription.Builder().addStroke(s).build(), object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { proximo(k) }
            override fun onCancelled(g: GestureDescription?) { if (hold === k) hold = null }
        }, h)
    }
    private fun proximo(k: Hold) {
        if (k.fim) return
        val continua = !k.soltar
        if (!continua) { k.fim = true; if (hold === k) hold = null }
        enviar(k, k.stroke!!.continueStroke(caminho(k.x, k.y), 0, if (continua) 80 else 20, continua))
    }

    /** Calibração dos botões: sobreposição transparente pede um toque em cada botão. */
    fun calibrarBotoes() {
        val fila = OcaNote.values().map { it.botao to it.rotulo }
        fun passo(i: Int) {
            if (i >= fila.size) return
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val tv = TextView(this).apply {
                text = "Toque no botão ${fila[i].second}\n(${i + 1}/${fila.size})"
                setTextColor(Color.WHITE); setBackgroundColor(0x88000000.toInt()); textSize = 22f; gravity = Gravity.CENTER
            }
            tv.setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    Prefs.setPos(fila[i].first, e.rawX, e.rawY)
                    wm.removeView(tv); h.postDelayed({ passo(i + 1) }, 300)
                }
                true
            }
            wm.addView(tv, WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT))
        }
        passo(0)
    }
}
