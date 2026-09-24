package app.tibi.core.kart

import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.core.tarih.gun
import app.tibi.core.tarih.haftaSonuKaydir
import java.time.LocalDate
import java.time.YearMonth

data class KartTakvimi(val kesimGunu: Int, val sonOdemeGunu: Int) {
    init {
        require(kesimGunu in 1..31) { "Kesim günü 1-31 arası olmalı: $kesimGunu" }
        require(sonOdemeGunu in 1..31) { "Son ödeme günü 1-31 arası olmalı: $sonOdemeGunu" }
    }

    fun kesimTarihi(ay: YearMonth): LocalDate = ay.gun(kesimGunu)

    /** Harcamanın düşeceği ekstrenin kesim tarihi. Kesim günü dahil o ayın ekstresi. */
    fun ilgiliKesim(harcamaTarihi: LocalDate): LocalDate {
        val ay = YearMonth.from(harcamaTarihi)
        val buAy = kesimTarihi(ay)
        return if (!harcamaTarihi.isAfter(buAy)) buAy else kesimTarihi(ay.plusMonths(1))
    }

    /** Kesimi yapılmış ekstrenin son ödeme tarihi; hafta sonuysa sonraki iş günü. */
    fun sonOdeme(kesim: LocalDate): LocalDate {
        val kesimAyi = YearMonth.from(kesim)
        val ay = if (sonOdemeGunu > kesimGunu) kesimAyi else kesimAyi.plusMonths(1)
        return ay.gun(sonOdemeGunu).haftaSonuKaydir(HaftaSonuKurali.SONRAKI)
    }
}
