package app.tibi.ui.ozet

import androidx.lifecycle.ViewModel
import app.tibi.core.donem.Donem
import app.tibi.veri.Anahtarlar
import app.tibi.veri.DonemServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.AylikYuk
import app.tibi.veri.dao.DonemToplami
import app.tibi.veri.dao.HesapBakiyesi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class KartOzeti(val ad: String, val kullanimKurus: Long, val kendiLimitiKurus: Long?)

data class OzetDurumu(
    val hitap: String?,
    val donem: Donem,
    val gelirKurus: Long,
    val giderKurus: Long,
    val kalanKurus: Long,
    val hesaplar: List<HesapBakiyesi>,
    val kartlar: List<KartOzeti>,
    val taksitYuku: List<AylikYuk>,
    /** A3: maaştan henüz düşülmemiş avans toplamı. */
    val dusulecekAvansKurus: Long = 0,
)

class OzetVm(
    db: TibiVeritabani,
    donemServisi: DonemServisi,
    bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val donemVeToplam: Flow<Pair<Donem, DonemToplami>> = donemServisi.donem(bugun()).flatMapLatest { d ->
        db.hareketDao().donemToplami(d.baslangic, d.bitis).map { d to it }
    }
    private val donemToplamAvans: Flow<Triple<Donem, DonemToplami, Long>> =
        combine(donemVeToplam, db.avansDao().acikToplam()) { (d, t), avans -> Triple(d, t, avans) }

    val durum: Flow<OzetDurumu> = combine(
        donemToplamAvans,
        db.ayarDao().okuAkis(Anahtarlar.HITAP),
        db.hesapDao().bakiyeler(),
        db.kartDao().kartlar(),
        db.taksitDao().aylikYuk(bugun()),
    ) { (donem, toplam, avans), hitap, hesaplar, kartlar, yuk ->
        OzetDurumu(
            hitap = hitap,
            donem = donem,
            gelirKurus = toplam.gelirKurus,
            giderKurus = toplam.giderKurus,
            kalanKurus = toplam.gelirKurus - toplam.giderKurus,
            hesaplar = hesaplar,
            kartlar = kartlar.filter { it.anaKartId == null }.map { KartOzeti(it.ad, it.kullanimKurus, it.kendiLimitiKurus) },
            taksitYuku = yuk.take(3),
            dusulecekAvansKurus = avans,
        )
    }
}
