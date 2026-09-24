package app.tibi.ui.giris

import androidx.lifecycle.ViewModel
import app.tibi.core.para.kurusCoz
import app.tibi.ui.Sonuc
import app.tibi.veri.KayitHatasi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kategori
import app.tibi.veri.tablo.KategoriYonu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class GirisTuru { HARCAMA, GELIR, TRANSFER }

data class HesapSecenegi(val id: Long, val ad: String, val kart: Boolean)

class HizliGirisVm(
    private val db: TibiVeritabani,
    private val kayit: KayitServisi,
    val bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val giderKategorileri: Flow<List<Kategori>> = db.kategoriDao().kullanimSirali(KategoriYonu.GIDER)
    val gelirKategorileri: Flow<List<Kategori>> = db.kategoriDao().kullanimSirali(KategoriYonu.GELIR)
    val hesaplar: Flow<List<HesapSecenegi>> = db.hesapDao().tumu().map { liste ->
        liste.sortedBy { it.tur == HesapTuru.KREDI_KARTI }.map { HesapSecenegi(it.id, it.ad, it.tur == HesapTuru.KREDI_KARTI) }
    }

    suspend fun kaydet(
        tur: GirisTuru, tutarMetni: String, hesapId: Long?, hedefHesapId: Long?, kategoriId: Long?,
        taksit: Int, erteleme: Int, tarih: LocalDate, aciklama: String,
    ): Sonuc {
        val tutar = kurusCoz(tutarMetni)?.takeIf { it.deger > 0 } ?: return Sonuc.Hata("Tutarı 1.249,90 biçiminde yaz.")
        val kaynak = hesapId ?: return Sonuc.Hata("Hesap ya da kart seç.")
        val not = aciklama.trim().ifEmpty { null }
        return try {
            when (tur) {
                GirisTuru.HARCAMA -> {
                    val kart = db.hesapDao().getir(kaynak)?.tur == HesapTuru.KREDI_KARTI
                    kayit.harcama(tutar, tarih, kaynak, kategoriId, if (kart) taksit else 1, if (kart) erteleme else 0, not)
                }
                GirisTuru.GELIR -> {
                    if (db.hesapDao().getir(kaynak)?.tur == HesapTuru.KREDI_KARTI) return Sonuc.Hata("Gelir bir banka hesabına ya da nakde girer.")
                    kayit.gelir(tutar, tarih, kaynak, kategoriId, not)
                }
                GirisTuru.TRANSFER -> {
                    val hedef = hedefHesapId ?: return Sonuc.Hata("Paranın gideceği hesabı seç.")
                    if (db.hesapDao().getir(hedef)?.tur == HesapTuru.KREDI_KARTI) kayit.kartOdemesi(tutar, tarih, kaynak, hedef)
                    else kayit.transfer(tutar, tarih, kaynak, hedef, not)
                }
            }
            Sonuc.Tamam
        } catch (e: KayitHatasi) {
            Sonuc.Hata(e.message ?: "Kaydedilemedi.")
        }
    }
}
