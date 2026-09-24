package app.tibi.core.tarih

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class HaftaSonuKurali { ONCEKI, SONRAKI, AYNI }

/** Ayın verilen günü; ay kısaysa (31 → şubat) ayın son günü. */
fun YearMonth.gun(gun: Int): LocalDate = atDay(minOf(gun, lengthOfMonth()))

/** Cumartesi/pazar gelen tarihi kurala göre cumaya ya da pazartesiye kaydırır. */
fun LocalDate.haftaSonuKaydir(kural: HaftaSonuKurali): LocalDate = when (kural) {
    HaftaSonuKurali.AYNI -> this
    HaftaSonuKurali.ONCEKI -> when (dayOfWeek) {
        DayOfWeek.SATURDAY -> minusDays(1)
        DayOfWeek.SUNDAY -> minusDays(2)
        else -> this
    }
    HaftaSonuKurali.SONRAKI -> when (dayOfWeek) {
        DayOfWeek.SATURDAY -> plusDays(2)
        DayOfWeek.SUNDAY -> plusDays(1)
        else -> this
    }
}
