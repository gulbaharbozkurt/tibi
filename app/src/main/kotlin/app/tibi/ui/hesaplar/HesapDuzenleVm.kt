package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import app.tibi.core.para.Kurus
import app.tibi.core.para.kurusCoz
import app.tibi.ui.Sonuc
import app.tibi.veri.KayitHatasi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.abs

data class HesapDuzenleDurumu(
    val ad: String,
    val tur: HesapTuru,
    val bankaAdi: String?,
    val maasHesabi: Boolean,
    val bakiyeKurus: Long,
)

/** Banka hesabı ya da elde nakit: ad/banka/maaş düzenleme, bakiye düzeltme, kapatma. Kapatılan hesapta durum null olur. */
class HesapDuzenleVm(
    private val db: TibiVeritabani,
    private val kayit: KayitServisi,
    private val hesapId: Long,
    private val bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val durum: Flow<HesapDuzenleDurumu?> = db.hesapDao().bakiyeler().map { liste ->
        liste.firstOrNull { it.id == hesapId }?.let { HesapDuzenleDurumu(it.ad, it.tur, it.bankaAdi, it.maasHesabi, it.bakiyeKurus) }
    }
    val bankalar: Flow<List<Banka>> = db.bankaDao().tumu()

    /** KurulumVm.hesapEkle ile aynı kurallar. Maaş hesabı seçilirse diğerlerinin işareti kalkar, maaş kuralı bu hesaba geçer. */
    suspend fun kaydet(ad: String, bankaAdi: String, maasHesabi: Boolean): Sonuc {
        val hesap = db.hesapDao().getir(hesapId) ?: return Sonuc.Hata("Hesap bulunamadı.")
        if (hesap.tur == HesapTuru.KREDI_KARTI) return Sonuc.Hata("Kartlar kart ekranından düzenlenir.")
        if (hesap.tur == HesapTuru.BANKA && bankaAdi.isBlank()) return Sonuc.Hata("Hesabın bağlı olduğu bankayı yaz.")
        val temizAd = ad.trim().ifEmpty { if (hesap.tur == HesapTuru.BANKA) "Vadesiz hesap" else "Elde nakit" }
        val maas = maasHesabi && hesap.tur == HesapTuru.BANKA
        return try {
            db.withTransaction {
                val bankaId = if (hesap.tur == HesapTuru.BANKA) kayit.bankaBulVeyaEkle(bankaAdi) else null
                db.hesapDao().guncelle(hesap.copy(ad = temizAd, bankaId = bankaId, maasHesabi = maas))
                if (maas) {
                    db.hesapDao().maasIsaretiniKaldir(hesapId)
                    db.duzenliKuralDao().maasKurali()?.takeIf { it.hesapId != hesapId }
                        ?.let { db.duzenliKuralDao().guncelle(it.copy(hesapId = hesapId)) }
                }
            }
            Sonuc.Tamam
        } catch (e: KayitHatasi) {
            Sonuc.Hata(e.message ?: "Hesap kaydedilemedi.")
        }
    }

    /** Fark tek bir DUZELTME hareketi olarak yazılır; geçmiş kayıtlar değişmez. Fark yoksa hiçbir şey yazılmaz. */
    suspend fun bakiyeDuzelt(gercekBakiyeMetni: String): Sonuc {
        val gercek = kurusCoz(gercekBakiyeMetni) ?: return Sonuc.Hata("Gerçek bakiyeyi 8.450,00 biçiminde yaz.")
        val simdiki = db.hesapDao().bakiye(hesapId).first()
        val fark = gercek.deger - simdiki
        if (fark == 0L) return Sonuc.Tamam
        return try {
            kayit.duzeltme(Kurus(abs(fark)), bugun(), hesapId, artis = fark > 0)
            Sonuc.Tamam
        } catch (e: KayitHatasi) {
            Sonuc.Hata(e.message ?: "Düzeltme kaydedilemedi.")
        }
    }

    /** Hesap arşivlenir: listelerden çıkar, geçmişi durur. Maaş kuralının hesabı kapatılamaz. */
    suspend fun kapat(): Sonuc {
        val hesap = db.hesapDao().getir(hesapId) ?: return Sonuc.Hata("Hesap bulunamadı.")
        if (db.duzenliKuralDao().maasKurali()?.hesapId == hesapId) {
            return Sonuc.Hata("Maaşın bu hesaba yatıyor; önce maaş hesabını değiştir.")
        }
        db.hesapDao().guncelle(hesap.copy(arsiv = true, maasHesabi = false))
        return Sonuc.Tamam
    }
}
