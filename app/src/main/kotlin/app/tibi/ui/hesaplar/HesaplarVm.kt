package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import kotlinx.coroutines.flow.Flow

class HesaplarVm(db: TibiVeritabani) : ViewModel() {
    val hesaplar: Flow<List<HesapBakiyesi>> = db.hesapDao().bakiyeler()
    val kartlar: Flow<List<KartBilgisi>> = db.kartDao().kartlar()
}
