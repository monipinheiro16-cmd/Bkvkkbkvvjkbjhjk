package br.ocarina.mic

/** Algoritmo YIN (de Cheveigné & Kawahara, 2002) com interpolação parabólica. */
class Yin(private val sr: Int, private val w: Int = 1024, minHz: Float = 230f, maxHz: Float = 720f,
          private val thr: Float = 0.15f) {
    private val tMin = (sr / maxHz).toInt()
    private val tMax = (sr / minHz).toInt()
    private val d = FloatArray(tMax + 2)
    val frameSize = w + tMax + 1

    /** Retorna a frequência fundamental em Hz, ou -1 se não houver tom claro. */
    fun detect(x: FloatArray): Float {
        for (t in 1..tMax + 1) {
            var s = 0f
            for (i in 0 until w) { val e = x[i] - x[i + t]; s += e * e }
            d[t] = s
        }
        var run = 0f
        d[0] = 1f
        for (t in 1..tMax + 1) { run += d[t]; d[t] = if (run == 0f) 1f else d[t] * t / run }
        var tau = -1
        var t = tMin
        while (t <= tMax) {
            if (d[t] < thr) { while (t + 1 <= tMax && d[t + 1] < d[t]) t++; tau = t; break }
            t++
        }
        if (tau < 0) return -1f
        val a = d[tau - 1]; val b = d[tau]; val c = d[tau + 1]
        val den = a - 2 * b + c
        val shift = if (den == 0f) 0f else 0.5f * (a - c) / den
        return sr / (tau + shift)
    }
}
