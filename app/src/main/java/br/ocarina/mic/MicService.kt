package br.ocarina.mic

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class MicService : Service(), AudioEngine.Callback {
    private var engine: AudioEngine? = null
    private val matcher = SongMatcher()
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Prefs.init(this)
        when (intent?.action) {
            "PARAR" -> { stopSelf(); return START_NOT_STICKY }
            "CAL" -> engine?.calibrarRuido()
            else -> {
                iniciarPrimeiroPlano()
                if (engine == null) engine = AudioEngine(this).also { it.start() }
                Session.rodando = true
            }
        }
        return START_NOT_STICKY
    }

    private fun iniciarPrimeiroPlano() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("oca", "Microfone da Ocarina", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, MicService::class.java).setAction("PARAR"), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "oca").setContentTitle("Ocarina ouvindo o microfone")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(Notification.Action.Builder(null, "Parar", stop).build()).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, n)
    }

    override fun onDestroy() { engine?.stop(); engine = null; Session.rodando = false; OcarinaAccessibilityService.instance?.soltar() }

    override fun onFrame(rms: Float, hz: Float, nota: OcaNote?) { Session.listener?.onFrame(rms, hz, nota) }
    override fun onNotaLigada(n: OcaNote) {
        Session.listener?.onNota(n)
        matcher.feed(n)?.let { Session.listener?.onMusica(it) }
        if (Prefs.injetar) Prefs.pos(n.botao)?.let { (x, y) -> OcarinaAccessibilityService.instance?.apertar(x, y) }
    }
    override fun onNotaDesligada(n: OcaNote) { if (Prefs.injetar) OcarinaAccessibilityService.instance?.soltar() }
}
