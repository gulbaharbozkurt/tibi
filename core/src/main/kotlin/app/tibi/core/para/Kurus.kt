package app.tibi.core.para

/** Para tutarı, kuruş cinsinden. 79,99 ₺ = Kurus(7999). */
@JvmInline
value class Kurus(val deger: Long) : Comparable<Kurus> {
    operator fun plus(diger: Kurus) = Kurus(deger + diger.deger)
    operator fun minus(diger: Kurus) = Kurus(deger - diger.deger)
    operator fun times(adet: Int) = Kurus(deger * adet)
    override fun compareTo(other: Kurus) = deger.compareTo(other.deger)

    /** n eşit parçaya böler; bölünemeyen kuruşlar ilk parçaya eklenir. */
    fun bol(n: Int): List<Kurus> {
        require(n >= 1) { "Parça sayısı en az 1 olmalı: $n" }
        val taban = deger / n
        val artan = deger - taban * n
        return List(n) { i -> Kurus(if (i == 0) taban + artan else taban) }
    }

    /** Binde oranla çarpar (400 = %40), yarım kuruşu yukarı yuvarlar. */
    fun oran(binde: Int): Kurus = Kurus(Math.floorDiv(deger * binde + 500, 1000L))

    companion object {
        val SIFIR = Kurus(0)
        fun tl(lira: Long, kurus: Int = 0) = Kurus(lira * 100 + kurus)
    }
}

fun Iterable<Kurus>.topla(): Kurus = fold(Kurus.SIFIR) { t, k -> t + k }
