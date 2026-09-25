package app.tibi.ui.ortak

import app.tibi.core.donem.Donem
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

/** Bankaya bağlı hesabı "Garanti BBVA · Vadesiz" diye, bankasızı yalnızca adıyla gösterir. */
fun hesapEtiketi(bankaAdi: String?, ad: String): String = if (bankaAdi != null) "$bankaAdi · $ad" else ad

/** Kalan limit: artıdaysa "1.294,26 ₺ kaldı", eksideyse "1.705,74 ₺ aşıldı". */
fun limitMetni(kalanKurus: Long): String =
    if (kalanKurus < 0) "${Kurus(-kalanKurus).bicimle()} aşıldı" else "${Kurus(kalanKurus).bicimle()} kaldı"

/** DatePicker milisaniyeyi UTC gece yarısı olarak verir; cihaz saat dilimi kullanılırsa gün kayar. */
fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.utcTarih(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
