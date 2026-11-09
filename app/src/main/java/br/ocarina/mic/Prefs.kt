package br.ocarina.mic

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) { if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("oca", 0) }
    var sensibilidade: Int get() = sp.getInt("sens", 60); set(v) = sp.edit().putInt("sens", v).apply()
    var tolerancia: Int get() = sp.getInt("tol", 60); set(v) = sp.edit().putInt("tol", v).apply()
    var oitava: Int get() = sp.getInt("oit", 0); set(v) = sp.edit().putInt("oit", v).apply()
    var injetar: Boolean get() = sp.getBoolean("inj", false); set(v) = sp.edit().putBoolean("inj", v).apply()
    var ruido: Float get() = sp.getFloat("ruido", 0.01f); set(v) = sp.edit().putFloat("ruido", v).apply()
    var emulador: Int get() = sp.getInt("emu", 0); set(v) = sp.edit().putInt("emu", v).apply()
    fun pos(b: String): Pair<Float, Float>? =
        if (sp.contains("x_$b")) sp.getFloat("x_$b", 0f) to sp.getFloat("y_$b", 0f) else null
    fun setPos(b: String, x: Float, y: Float) = sp.edit().putFloat("x_$b", x).putFloat("y_$b", y).apply()
}

object Session {
    @Volatile var listener: Listener? = null
    @Volatile var rodando = false
    interface Listener { fun onFrame(rms: Float, hz: Float, nota: OcaNote?); fun onNota(n: OcaNote); fun onMusica(nome: String) }
}
