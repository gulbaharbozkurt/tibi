package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.Sonuc
import app.tibi.ui.kurulum.GirdiHatasi
import app.tibi.ui.kurulum.KartDogrulayici
import app.tibi.ui.kurulum.KartGirdisi
import app.tibi.veri.KayitHatasi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.KartTuru
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** "15.000,00": düzenleme alanına geri yazılan tutar, " ₺" olmadan. */
private fun Long.alanMetni(): String = Kurus(this).bicimle().removeSuffix(" ₺")

/**
 * Kart düzenleme. Tür ve ana kart değişmez, açılış alanları yok sayılır.
 * Ek/sanal kartta yalnızca ad ve son 4 hane düzenlenir. Kesim günü değişince mevcut taksit satırları yeniden planlanmaz.
 */
class KartDuzenleVm(
    private val db: TibiVeritabani,
    private val kayit: KayitServisi,
    private val kartId: Long,
) : ViewModel() {
    val girdi: Flow<KartGirdisi?> = db.kartDao().kartlar().map { liste ->
        liste.firstOrNull { it.hesapId == kartId }?.let(::girdiye)
    }
    val bankalar: Flow<List<Banka>> = db.bankaDao().tumu()

    private fun girdiye(k: KartBilgisi) = KartGirdisi(
        ad = k.ad, bankaAdi = k.bankaAdi.orEmpty(), tur = k.kartTuru, anaKartId = k.anaKartId, son4 = k.son4,
        kesimGunu = k.kesimGunu.toString(), sonOdemeGunu = k.sonOdemeGunu.toString(),
        bankaLimiti = k.bankaLimitiKurus?.alanMetni().orEmpty(), kendiLimiti = k.kendiLimitiKurus?.alanMetni().orEmpty(),
        asgariOran = (k.asgariOranBinde / 10).toString(),
    )

    suspend fun kaydet(g: KartGirdisi): Sonuc = try {
        val kart = db.kartDao().getir(kartId) ?: throw GirdiHatasi("Kart bulunamadı.")
        val hesap = db.hesapDao().getir(kartId) ?: throw GirdiHatasi("Kart bulunamadı.")
        KartDogrulayici.kimlik(g)
        val ayar = if (kart.kartTuru == KartTuru.ANA) KartDogrulayici.anaKartAyarlari(g) else null
        db.withTransaction {
            if (ayar == null) {
                db.kartDao().guncelle(kart.copy(son4 = g.son4))
                db.hesapDao().guncelle(hesap.copy(ad = g.ad.trim()))
            } else {
                val bankaId = g.bankaAdi.takeIf { it.isNotBlank() }?.let { kayit.bankaBulVeyaEkle(it) }
                db.kartDao().guncelle(kart.copy(son4 = g.son4, kesimGunu = ayar.kesimGunu, sonOdemeGunu = ayar.sonOdemeGunu,
                    bankaLimitiKurus = ayar.bankaLimitiKurus, kendiLimitiKurus = ayar.kendiLimitiKurus, asgariOranBinde = ayar.asgariOranBinde))
                db.hesapDao().guncelle(hesap.copy(ad = g.ad.trim(), bankaId = bankaId))
                // Bağlı ek/sanal kartlar ana kartın bankasını ve günlerini izler.
                db.kartDao().bagliKartIdleri(kartId).drop(1).forEach { id ->
                    db.kartDao().getir(id)?.let { db.kartDao().guncelle(it.copy(kesimGunu = ayar.kesimGunu, sonOdemeGunu = ayar.sonOdemeGunu)) }
                    db.hesapDao().getir(id)?.let { db.hesapDao().guncelle(it.copy(bankaId = bankaId)) }
                }
            }
        }
        Sonuc.Tamam
    } catch (e: GirdiHatasi) {
        Sonuc.Hata(e.message!!)
    } catch (e: KayitHatasi) {
        Sonuc.Hata(e.message ?: "Kart kaydedilemedi.")
    }

    /** Kart arşivlenir; ana kart kapanırsa ona bağlı ek/sanal kartlar da kapanır. Geçmiş kayıtlar durur. */
    suspend fun kapat(): Sonuc {
        val kart = db.kartDao().getir(kartId) ?: return Sonuc.Hata("Kart bulunamadı.")
        db.withTransaction {
            val idler = if (kart.anaKartId == null) db.kartDao().bagliKartIdleri(kartId) else listOf(kartId)
            idler.forEach { id -> db.hesapDao().getir(id)?.let { db.hesapDao().guncelle(it.copy(arsiv = true)) } }
        }
        return Sonuc.Tamam
    }
}
