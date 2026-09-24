package app.tibi.ui.kurulum

import androidx.lifecycle.ViewModel
import app.tibi.core.para.Kurus
import app.tibi.core.para.kurusCoz
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class KurulumAdimi(val baslik: String) {
    HOS_GELDIN("Hoş geldin"),
    HESAPLAR("Hesaplar ve nakit"),
    GELIR("Gelir"),
    KARTLAR("Kartlar"),
    TAKSITLER("Devam eden taksitler"),
}

class KurulumVm(
    internal val db: TibiVeritabani,
    internal val kayit: KayitServisi,
    internal val bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val hesaplar: Flow<List<HesapBakiyesi>> = db.hesapDao().bakiyeler()
    val kartlar: Flow<List<KartBilgisi>> = db.kartDao().kartlar()

    suspend fun hitapKaydet(ad: String): Sonuc {
        val temiz = ad.trim()
        if (temiz.isEmpty()) return Sonuc.Hata("Sana nasıl hitap edelim? Bir ad yaz.")
        db.ayarDao().yaz(Ayar(Anahtarlar.HITAP, temiz))
        return Sonuc.Tamam
    }

    suspend fun hesapEkle(ad: String, tur: HesapTuru, bakiyeMetni: String, maasHesabi: Boolean): Sonuc {
        if (ad.isBlank()) return Sonuc.Hata("Hesaba bir ad ver, ör. \"Garanti vadesiz\".")
        if (tur == HesapTuru.KREDI_KARTI) return Sonuc.Hata("Kredi kartları Kartlar adımında eklenir.")
        val bakiye = if (bakiyeMetni.isBlank()) Kurus.SIFIR else kurusCoz(bakiyeMetni)
            ?: return Sonuc.Hata("Bakiyeyi 8.450,00 biçiminde yaz.")
        db.hesapDao().ekle(Hesap(ad = ad.trim(), tur = tur, acilisBakiyeKurus = bakiye.deger, acilisTarihi = bugun(), maasHesabi = maasHesabi))
        return Sonuc.Tamam
    }

    suspend fun bitir(): Sonuc {
        db.ayarDao().yaz(Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        return Sonuc.Tamam
    }
}
