package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Banka
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BankaGrubu(val banka: Banka, val hesaplar: List<HesapBakiyesi>, val kartlar: List<KartBilgisi>)

data class HesaplarDurumu(
    val bankalar: List<BankaGrubu>,
    val nakit: List<HesapBakiyesi>,
    val bankasizKartlar: List<KartBilgisi>,
)

class HesaplarVm(db: TibiVeritabani) : ViewModel() {
    /** Bankalar kendi sırasıyla; hesabı da kartı da olmayan banka gösterilmez. */
    val durum: Flow<HesaplarDurumu> = combine(
        db.bankaDao().tumu(), db.hesapDao().bakiyeler(), db.kartDao().kartlar(),
    ) { bankalar, hesaplar, kartlar ->
        HesaplarDurumu(
            bankalar = bankalar
                .map { b -> BankaGrubu(b, hesaplar.filter { it.bankaId == b.id }, kartlar.filter { it.bankaId == b.id }) }
                .filter { it.hesaplar.isNotEmpty() || it.kartlar.isNotEmpty() },
            nakit = hesaplar.filter { it.bankaId == null },
            bankasizKartlar = kartlar.filter { it.bankaId == null },
        )
    }
}
