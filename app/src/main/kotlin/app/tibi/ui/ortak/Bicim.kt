package app.tibi.ui.ortak

import app.tibi.core.donem.Donem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val TR: Locale = Locale.forLanguageTag("tr")
private val KISA = DateTimeFormatter.ofPattern("d MMM", TR)

fun LocalDate.kisa(): String = format(KISA)

fun LocalDate.gunBasligi(bugun: LocalDate): String = when (this) {
    bugun -> "Bugün · ${kisa()}"
    bugun.minusDays(1) -> "Dün · ${kisa()}"
    else -> "${kisa()}, ${dayOfWeek.getDisplayName(TextStyle.FULL, TR)}"
}

fun Donem.aralik(): String = "${baslangic.kisa()} – ${bitis.kisa()}"
