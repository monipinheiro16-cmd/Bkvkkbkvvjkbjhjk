package br.ocarina.mic

import kotlin.math.abs
import kotlin.math.ln

/** As cinco notas da Ocarina e o botão do N64 que cada uma representa. */
enum class OcaNote(val rotulo: String, val hz: Float, val botao: String, val letra: Char) {
    A("Ré · A", 293.66f, "A", 'A'),
    BAIXO("Fá · C↓", 349.23f, "C_BAIXO", 'D'),
    DIREITA("Lá · C→", 440.00f, "C_DIREITA", 'R'),
    ESQUERDA("Si · C←", 493.88f, "C_ESQUERDA", 'L'),
    CIMA("Ré agudo · C↑", 587.33f, "C_CIMA", 'U');

    companion object {
        /** Nota mais próxima dentro da tolerância (cents). oitava: -1, 0 ou +1 transpõe a entrada. */
        fun achar(hz: Float, oitava: Int, tolCents: Float): OcaNote? {
            if (hz <= 0f) return null
            val f = hz * Math.pow(2.0, -oitava.toDouble()).toFloat()
            var melhor: OcaNote? = null; var menor = Float.MAX_VALUE
            for (n in values()) {
                val c = abs(1200f * (ln(f / n.hz) / ln(2f)))
                if (c < menor) { menor = c; melhor = n }
            }
            return if (menor <= tolCents) melhor else null
        }
        fun daLetra(c: Char) = values().first { it.letra == c }
    }
}

/** Melodias (A=A, D=C↓, R=C→, L=C←, U=C↑). Conferir no jogo: foram escritas de memória. */
object Musicas {
    val lista = listOf(
        "Canção de Ninar de Zelda" to "LURLUR", "Canção de Epona" to "ULRULR",
        "Canção de Saria" to "DRLDRL", "Canção do Sol" to "RDURDU",
        "Canção do Tempo" to "RADRAD", "Canção da Tempestade" to "ADUADU",
        "Minueto da Floresta" to "AULRLR", "Bolero do Fogo" to "DADARDRD",
        "Serenata da Água" to "ADRRL", "Réquiem do Espírito" to "ADARDA",
        "Noturno da Sombra" to "LRRALRD", "Prelúdio da Luz" to "URURLU")
}

class SongMatcher {
    private val buf = StringBuilder(); private var ultimo = 0L
    fun feed(n: OcaNote): String? {
        val agora = System.currentTimeMillis()
        if (agora - ultimo > 5000) buf.clear()
        ultimo = agora; buf.append(n.letra)
        for ((nome, seq) in Musicas.lista) if (buf.endsWith(seq)) { buf.clear(); return nome }
        if (buf.length > 12) buf.delete(0, buf.length - 12)
        return null
    }
}
