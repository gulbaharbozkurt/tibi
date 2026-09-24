package app.tibi.veri

import app.tibi.core.donem.Donem
import app.tibi.core.donem.DonemHesaplayici
import app.tibi.core.donem.MaasKurali
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** "Bu dönem": maaş kuralından maaştan maaşa; kural yoksa takvim ayı. */
class DonemServisi(private val db: TibiVeritabani) {
    fun donem(bugun: LocalDate): Flow<Donem> = db.duzenliKuralDao().maasKuraliAkisi().map { k ->
        if (k == null) takvimAyi(bugun) else DonemHesaplayici.donem(MaasKurali(k.gun, k.haftaSonuKurali), bugun)
    }

    companion object {
        fun takvimAyi(bugun: LocalDate) = Donem(bugun.withDayOfMonth(1), bugun.withDayOfMonth(bugun.lengthOfMonth()))
    }
}
