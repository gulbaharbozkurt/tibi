package app.tibi.core.donem

import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.core.tarih.gun
import app.tibi.core.tarih.haftaSonuKaydir
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class MaasKurali(val gun: Int, val haftaSonu: HaftaSonuKurali) {
    init { require(gun in 1..31) { "Maaş günü 1-31 arası olmalı: $gun" } }

    /** Verilen aydaki maaş tarihi, hafta sonu kuralı uygulanmış. */
    fun tarih(ay: YearMonth): LocalDate = ay.gun(gun).haftaSonuKaydir(haftaSonu)
}

/** Maaştan maaşa dönem; iki uç dahil. */
data class Donem(val baslangic: LocalDate, val bitis: LocalDate) {
    val gunSayisi: Int get() = ChronoUnit.DAYS.between(baslangic, bitis).toInt() + 1

    fun icerir(t: LocalDate): Boolean = !t.isBefore(baslangic) && !t.isAfter(bitis)

    /** Bugün dahil geçen gün / toplam gün, binde. */
    fun ilerlemeBinde(bugun: LocalDate): Int {
        val gecen = ChronoUnit.DAYS.between(baslangic, bugun).toInt() + 1
        return (gecen.coerceIn(0, gunSayisi) * 1000) / gunSayisi
    }
}

object DonemHesaplayici {
    fun donem(kural: MaasKurali, bugun: LocalDate): Donem {
        var ay = YearMonth.from(bugun).plusMonths(1)
        // Kaydırma tarihi ay sınırının ötesine taşıyabilir; bugünü geçmeyen en son maaşı geriye doğru ara.
        while (kural.tarih(ay).isAfter(bugun)) ay = ay.minusMonths(1)
        val sonraki = kural.tarih(ay.plusMonths(1))
        return Donem(kural.tarih(ay), sonraki.minusDays(1))
    }
}

/**
 * [baslangic] döneminden [adim] dönem ileri (+) ya da geri (−) gider.
 * Önceki dönem: başlangıçtan bir gün önceyi içeren dönem; sonraki: bitişten bir gün sonrayı içeren.
 * [bul] bir tarihi içeren dönemi verir (maaş kuralı ya da takvim ayı).
 */
fun kaydir(baslangic: Donem, adim: Int, bul: (LocalDate) -> Donem): Donem {
    var d = baslangic
    repeat(kotlin.math.abs(adim)) {
        d = if (adim < 0) bul(d.baslangic.minusDays(1)) else bul(d.bitis.plusDays(1))
    }
    return d
}
