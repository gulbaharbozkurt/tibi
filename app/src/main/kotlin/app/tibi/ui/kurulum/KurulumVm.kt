package app.tibi.ui.kurulum

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import app.tibi.core.donem.Donem
import app.tibi.core.donem.DonemHesaplayici
import app.tibi.core.donem.MaasKurali
import app.tibi.core.para.Kurus
import app.tibi.core.para.kurusCoz
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    val maasKurali: Flow<DuzenliKural?> = db.duzenliKuralDao().maasKuraliAkisi()

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

    fun donemOnizleme(gunMetni: String, haftaSonu: HaftaSonuKurali): Donem? {
        val gun = gunMetni.toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        return DonemHesaplayici.donem(MaasKurali(gun, haftaSonu), bugun())
    }

    suspend fun maasKaydet(tutarMetni: String, gunMetni: String, haftaSonu: HaftaSonuKurali, hesapId: Long?): Sonuc {
        val tutar = kurusCoz(tutarMetni)?.takeIf { it.deger > 0 } ?: return Sonuc.Hata("Maaş tutarını 45.000 biçiminde yaz.")
        val gun = gunMetni.toIntOrNull()?.takeIf { it in 1..31 } ?: return Sonuc.Hata("Maaşın yattığı gün 1 ile 31 arasında olmalı.")
        if (hesapId == null) return Sonuc.Hata("Maaşın yattığı hesabı seç.")
        db.withTransaction {
            db.duzenliKuralDao().maasKuraliAkisi().first()?.let { db.duzenliKuralDao().guncelle(it.copy(aktif = false)) }
            db.duzenliKuralDao().ekle(
                DuzenliKural(
                    ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = tutar.deger,
                    periyot = Periyot.AYLIK, gun = gun, haftaSonuKurali = haftaSonu, hesapId = hesapId,
                    kategoriId = db.kategoriDao().adIle("Maaş")?.id, baslangic = bugun(),
                )
            )
        }
        return Sonuc.Tamam
    }

    suspend fun bitir(): Sonuc {
        db.ayarDao().yaz(Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        return Sonuc.Tamam
    }
}
