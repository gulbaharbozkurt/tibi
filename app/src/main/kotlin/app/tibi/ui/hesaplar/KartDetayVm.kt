package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import app.tibi.core.kart.KartTakvimi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.dao.KartTaksidi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

data class KartDetayi(
    val kart: KartBilgisi,
    val ana: KartBilgisi,
    val kalanKurus: Long?,
    val siradakiKesim: LocalDate,
    val sonOdeme: LocalDate,
    val acikDonemKurus: Long,
    val taksitler: List<KartTaksidi>,
    val bagliKartlar: List<KartBilgisi>,
)

class KartDetayVm(db: TibiVeritabani, kartId: Long, bugun: () -> LocalDate = LocalDate::now) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val detay: Flow<KartDetayi?> = db.kartDao().kartlar().flatMapLatest { liste ->
        val kart = liste.firstOrNull { it.hesapId == kartId } ?: return@flatMapLatest flowOf(null)
        val ana = kart.anaKartId?.let { id -> liste.firstOrNull { it.hesapId == id } } ?: kart
        val takvim = KartTakvimi(ana.kesimGunu, ana.sonOdemeGunu)
        val kesim = takvim.ilgiliKesim(bugun())
        combine(db.taksitDao().kesimTutari(ana.hesapId, kesim), db.taksitDao().aktifTaksitler(ana.hesapId, bugun())) { donem, taksitler ->
            KartDetayi(
                kart = kart.copy(kullanimKurus = ana.kullanimKurus),
                ana = ana,
                kalanKurus = ana.kendiLimitiKurus?.let { it - ana.kullanimKurus },
                siradakiKesim = kesim,
                sonOdeme = takvim.sonOdeme(kesim),
                acikDonemKurus = donem,
                taksitler = taksitler,
                bagliKartlar = liste.filter { it.anaKartId == ana.hesapId },
            )
        }
    }
}
