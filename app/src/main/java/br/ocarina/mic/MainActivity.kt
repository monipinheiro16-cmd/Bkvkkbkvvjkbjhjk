package br.ocarina.mic

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity(), Session.Listener {
    private val emus = listOf(
        "M64Plus FZ" to "org.mupen64plusae.v3.fzurita", "M64Plus FZ Pro" to "org.mupen64plusae.v3.fzurita.pro",
        "RetroArch (64 bits)" to "com.retroarch.aarch64", "RetroArch" to "com.retroarch")
    private lateinit var fv: FrequencyView
    private lateinit var tNota: TextView; private lateinit var tMus: TextView; private lateinit var tInfo: TextView

    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
    private fun txt(t: String, s: Float = 16f) = TextView(this).apply { text = t; textSize = s; setTextColor(Color.WHITE) }
    private fun seek(rotulo: String, max: Int, v: Int, set: (Int) -> Unit, l: LinearLayout) {
        val t = txt("$rotulo: $v"); val s = SeekBar(this).apply { this.max = max; progress = v }
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) { set(p); t.text = "$rotulo: $p" }
            override fun onStartTrackingTouch(sb: SeekBar?) {}; override fun onStopTrackingTouch(sb: SeekBar?) {}
        }); l.addView(t); l.addView(s)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); Prefs.init(this)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        l.addView(txt("Ocarina · Microfone Real", 24f))
        fv = FrequencyView(this); l.addView(fv, LinearLayout.LayoutParams(-1, 520))
        tNota = txt("Nota: —", 26f); tMus = txt("Música: —", 18f); tInfo = txt("", 14f)
        l.addView(tNota); l.addView(tMus); l.addView(tInfo)
        l.addView(btn("▶ Iniciar microfone") { iniciar() }); l.addView(btn("■ Parar") { svc("PARAR") })
        l.addView(btn("Calibrar ruído (fique em silêncio 1,5 s)") { iniciar(); svc("CAL") })
        seek("Sensibilidade", 100, Prefs.sensibilidade, { Prefs.sensibilidade = it }, l)
        seek("Tolerância (cents)", 150, Prefs.tolerancia, { Prefs.tolerancia = it }, l)
        l.addView(txt("Oitava da voz/assobio (−1, 0, +1)"))
        l.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("−1 (mais grave)", "0", "+1 (mais agudo)"))
            setSelection(Prefs.oitava + 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(a: AdapterView<*>?, v: View?, p: Int, i: Long) { Prefs.oitava = p - 1 }
                override fun onNothingSelected(a: AdapterView<*>?) {}
            }
        })
        l.addView(txt("Emulador"))
        l.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, emus.map { it.first })
            setSelection(Prefs.emulador)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(a: AdapterView<*>?, v: View?, p: Int, i: Long) { Prefs.emulador = p }
                override fun onNothingSelected(a: AdapterView<*>?) {}
            }
        })
        l.addView(btn("Abrir emulador") { abrirEmulador() })
        l.addView(btn("1) Ativar serviço de acessibilidade") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        l.addView(btn("2) Calibrar botões A e C no emulador") {
            val s = OcarinaAccessibilityService.instance
            if (s == null) Toast.makeText(this, "Ative o serviço de acessibilidade primeiro", Toast.LENGTH_LONG).show()
            else { abrirEmulador(); window.decorView.postDelayed({ s.calibrarBotoes() }, 2500) }
        })
        l.addView(Switch(this).apply {
            text = "Enviar toques ao emulador (desligado = modo teste)"; setTextColor(Color.WHITE); isChecked = Prefs.injetar
            setOnCheckedChangeListener { _, c -> Prefs.injetar = c }
        })
        setContentView(ScrollView(this).apply { setBackgroundColor(0xFF0B0B12.toInt()); addView(l) })
    }

    private fun svc(a: String) = startService(Intent(this, MicService::class.java).setAction(a))
    private fun iniciar() {
        val need = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) need += Manifest.permission.RECORD_AUDIO
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) need += Manifest.permission.POST_NOTIFICATIONS
        if (need.isNotEmpty()) { requestPermissions(need.toTypedArray(), 1); return }
        if (!Session.rodando) startForegroundService(Intent(this, MicService::class.java).setAction("INICIAR"))
    }
    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        if (r.isNotEmpty() && r.all { it == PackageManager.PERMISSION_GRANTED }) iniciar()
        else Toast.makeText(this, "O microfone é necessário.", Toast.LENGTH_LONG).show()
    }
    private fun abrirEmulador() {
        val i = packageManager.getLaunchIntentForPackage(emus[Prefs.emulador].second)
        if (i == null) Toast.makeText(this, "Emulador não instalado", Toast.LENGTH_LONG).show() else startActivity(i)
    }
    override fun onStart() { super.onStart(); Session.listener = this }
    override fun onStop() { super.onStop(); Session.listener = null }
    override fun onFrame(rms: Float, hz: Float, nota: OcaNote?) = runOnUiThread {
        fv.push(hz, rms)
        tInfo.text = "Volume %.3f · ruído %.3f · %s".format(rms, Prefs.ruido, if (hz > 0) "%.1f Hz".format(hz) else "sem tom")
    }
    override fun onNota(n: OcaNote) = runOnUiThread { tNota.text = "Nota: ${n.rotulo}" }
    override fun onMusica(nome: String) = runOnUiThread { tMus.text = "Música: $nome ✔" }
}
