package app.tibi.ui.hareketler

import androidx.lifecycle.ViewModel
import app.tibi.core.donem.Donem
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.TR
import app.tibi.veri.DonemServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HareketSatiri
import app.tibi.veri.tablo.HareketTuru
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

enum class HareketFiltresi(val turler: Set<HareketTuru>?) {
    TUMU(null),
    HARCAMA(setOf(HareketTuru.HARCAMA)),
    GELIR(setOf(HareketTuru.GELIR, HareketTuru.TAHSILAT, HareketTuru.AVANS)),
    TRANSFER(setOf(HareketTuru.TRANSFER, HareketTuru.KART_ODEME)),
}

data class GunGrubu(val tarih: LocalDate, val satirlar: List<HareketSatiri>)

fun HareketSatiri.eslesir(arama: String): Boolean {
    val a = arama.trim().lowercase(TR)
    if (a.isEmpty()) return true
    val alanlar = listOfNotNull(kategoriAdi, aciklama, kaynakAdi, hedefAdi, Kurus(tutarKurus).bicimle())
    return alanlar.any { it.lowercase(TR).contains(a) }
}

class HareketlerVm(
    private val db: TibiVeritabani,
    donemServisi: DonemServisi,
    bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val filtre = MutableStateFlow(HareketFiltresi.TUMU)
    val arama = MutableStateFlow("")
    val donem: Flow<Donem> = donemServisi.donem(bugun())

    @OptIn(ExperimentalCoroutinesApi::class)
    val gruplar: Flow<List<GunGrubu>> = donem.flatMapLatest { d ->
        combine(db.hareketDao().satirlar(d.baslangic, d.bitis), filtre, arama) { satirlar, f, a ->
            satirlar.filter { (f.turler == null || it.tur in f.turler) && it.eslesir(a) }
                .groupBy { it.tarih }
                .map { (tarih, liste) -> GunGrubu(tarih, liste) }
        }
    }

    suspend fun sil(id: Long) = db.hareketDao().sil(id)
}
