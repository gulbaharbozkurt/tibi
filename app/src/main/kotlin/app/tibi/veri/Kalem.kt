package app.tibi.veri

import java.util.Locale

private val TURKCE: Locale = Locale.forLanguageTag("tr")
private val BOSLUKLAR = Regex("\\s+")

/** Harcamanın adı yazıldığı gibi; baştaki/sondaki boşluk silinir, aradaki boşluklar teke iner. Boşsa null. */
fun kalemAdi(ad: String): String? = ad.trim().replace(BOSLUKLAR, " ").ifEmpty { null }

/** Gruplama anahtarı: kalem adının Türkçe kurallarla küçük harfi ("PROBİS" → "probis"). Boşsa null. */
fun kalemAnahtari(ad: String): String? = kalemAdi(ad)?.lowercase(TURKCE)
