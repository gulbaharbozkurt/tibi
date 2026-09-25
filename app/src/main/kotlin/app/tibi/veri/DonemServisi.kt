package app.tibi.veri

import app.tibi.core.donem.Donem
import app.tibi.core.donem.DonemHesaplayici
import app.tibi.core.donem.MaasKurali
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** "Bu dönem": maaş kuralından maaştan maaşa; kural yoksa takvim ayı. */
class DonemServisi(private val db: TibiVeritabani) {
    /** Bir tarihi içeren dönemi veren fonksiyon; maaş kuralı değiştikçe yenilenir. */
    val bulucu: Flow<(LocalDate) -> Donem> = db.duzenliKuralDao().maasKuraliAkisi().map { k ->
        val kural = k?.let { MaasKurali(it.gun, it.haftaSonuKurali) }
        val bul: (LocalDate) -> Donem = { t -> if (kural == null) takvimAyi(t) else DonemHesaplayici.donem(kural, t) }
        bul
    }

    fun donemIcin(tarih: LocalDate): Flow<Donem> = bulucu.map { it(tarih) }

    fun donem(bugun: LocalDate): Flow<Donem> = donemIcin(bugun)

    companion object {
        fun takvimAyi(bugun: LocalDate) = Donem(bugun.withDayOfMonth(1), bugun.withDayOfMonth(bugun.lengthOfMonth()))
    }
}
