package app.tibi.ui.kurulum

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import app.tibi.core.donem.Donem
import app.tibi.core.donem.DonemHesaplayici
import app.tibi.core.donem.MaasKurali
import app.tibi.core.kart.KartTakvimi
import app.tibi.core.kart.PlanliTaksit
import app.tibi.core.kart.TaksitPlanlayici
import app.tibi.core.para.Kurus
import app.tibi.core.para.kurusCoz
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.GecmisTaksitGirisi
import app.tibi.veri.KayitHatasi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.tablo.Banka
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.dao.KartTaksidi
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

enum class KurulumAdimi(val baslik: String) {
    HOS_GELDIN("Hoş geldin"),
    HESAPLAR("Bankalar ve nakit"),
    GELIR("Gelir"),
    KARTLAR("Kartlar"),
    TAKSITLER("Devam eden taksitler"),
}

data class KartGirdisi(
    val ad: String = "",
    /** Yalnızca ana kartta okunur; boşsa kart bankasız kalır. Ek/sanal kart ana kartın bankasını alır. */
    val bankaAdi: String = "",
    val tur: KartTuru = KartTuru.ANA,
    val anaKartId: Long? = null,
    val son4: String = "",
    val kesimGunu: String = "",
    val sonOdemeGunu: String = "",
    val bankaLimiti: String = "",
    val kendiLimiti: String = "",
    val asgariOran: String = "40",
    val kesilmisEkstre: String = "",
    val donemIci: String = "",
)

enum class TaksitModu { AYLIK, TOPLAM, KALAN }

data class TaksitGirdisi(
    val kartId: Long? = null,
    val aciklama: String = "",
    val tur: TaksitTuru = TaksitTuru.ALISVERIS,
    val mod: TaksitModu = TaksitModu.AYLIK,
    val tutar: String = "",
    val sayi: String = "",
    val siradaki: String = "",
)

class KurulumVm(
    internal val db: TibiVeritabani,
    internal val kayit: KayitServisi,
    internal val bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val hesaplar: Flow<List<HesapBakiyesi>> = db.hesapDao().bakiyeler()
    val kartlar: Flow<List<KartBilgisi>> = db.kartDao().kartlar()
    val maasKurali: Flow<DuzenliKural?> = db.duzenliKuralDao().maasKuraliAkisi()
    val bankalar: Flow<List<Banka>> = db.bankaDao().tumu()

    suspend fun hitapKaydet(ad: String): Sonuc {
        val temiz = ad.trim()
        if (temiz.isEmpty()) return Sonuc.Hata("Sana nasıl hitap edelim? Bir ad yaz.")
        db.ayarDao().yaz(Ayar(Anahtarlar.HITAP, temiz))
        return Sonuc.Tamam
    }

    /** Banka hesabı bir bankaya bağlanır (yoksa açılır); nakit bankayı yok sayar. Boş ad "Vadesiz" / "Nakit" olur. */
    suspend fun hesapEkle(bankaAdi: String, ad: String, tur: HesapTuru, bakiyeMetni: String, maasHesabi: Boolean): Sonuc {
        if (tur == HesapTuru.KREDI_KARTI) return Sonuc.Hata("Kredi kartları Kartlar adımında eklenir.")
        if (tur == HesapTuru.BANKA && bankaAdi.isBlank()) return Sonuc.Hata("Hesabın bağlı olduğu bankayı yaz.")
        val bakiye = if (bakiyeMetni.isBlank()) Kurus.SIFIR else kurusCoz(bakiyeMetni)
            ?: return Sonuc.Hata("Bakiyeyi 8.450,00 biçiminde yaz.")
        val temizAd = ad.trim().ifEmpty { if (tur == HesapTuru.BANKA) "Vadesiz" else "Nakit" }
        return try {
            db.withTransaction {
                val bankaId = if (tur == HesapTuru.BANKA) kayit.bankaBulVeyaEkle(bankaAdi) else null
                db.hesapDao().ekle(Hesap(ad = temizAd, tur = tur, acilisBakiyeKurus = bakiye.deger, acilisTarihi = bugun(),
                    maasHesabi = maasHesabi, bankaId = bankaId))
            }
            Sonuc.Tamam
        } catch (e: KayitHatasi) {
            Sonuc.Hata(e.message ?: "Hesap kaydedilemedi.")
        }
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

    private class GirdiHatasi(mesaj: String) : Exception(mesaj)

    private fun tutarVeyaBos(metin: String, alan: String): Long? =
        if (metin.isBlank()) null else (kurusCoz(metin) ?: throw GirdiHatasi("$alan tutarını 15.000 biçiminde yaz.")).deger

    private fun gun(metin: String, alan: String): Int =
        metin.toIntOrNull()?.takeIf { it in 1..31 } ?: throw GirdiHatasi("$alan 1 ile 31 arasında olmalı.")

    suspend fun kartEkle(g: KartGirdisi): Sonuc = try {
        if (g.ad.isBlank()) throw GirdiHatasi("Karta bir ad ver, ör. \"Bonus\".")
        if (!Regex("""\d{4}""").matches(g.son4)) throw GirdiHatasi("Kartın son 4 hanesini yaz.")
        val ana = if (g.tur == KartTuru.ANA) null else {
            val id = g.anaKartId ?: throw GirdiHatasi("Ek ve sanal kart için ana kartı seç.")
            db.kartDao().getir(id) ?: throw GirdiHatasi("Seçilen ana kart bulunamadı.")
        }
        val kesim = ana?.kesimGunu ?: gun(g.kesimGunu, "Kesim günü")
        val sonOdeme = ana?.sonOdemeGunu ?: gun(g.sonOdemeGunu, "Son ödeme günü")
        val bankaLimiti = if (ana == null) tutarVeyaBos(g.bankaLimiti, "Banka limiti") else null
        val kendiLimiti = if (ana == null) tutarVeyaBos(g.kendiLimiti, "Kendi limitin") else null
        val oran = g.asgariOran.toIntOrNull()?.takeIf { it in 0..100 } ?: throw GirdiHatasi("Asgari ödeme oranı 0 ile 100 arasında olmalı.")
        val kesilmis = if (ana == null) tutarVeyaBos(g.kesilmisEkstre, "Kesilmiş ekstre") else null
        val donemIci = if (ana == null) tutarVeyaBos(g.donemIci, "Dönem içi harcama") else null
        db.withTransaction {
            val bankaId = if (ana != null) db.hesapDao().getir(ana.hesapId)?.bankaId
                else g.bankaAdi.takeIf { it.isNotBlank() }?.let { kayit.bankaBulVeyaEkle(it) }
            val id = db.hesapDao().ekle(Hesap(ad = g.ad.trim(), tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun(), bankaId = bankaId))
            db.kartDao().ekle(
                Kart(hesapId = id, kartTuru = g.tur, anaKartId = ana?.hesapId, son4 = g.son4, kesimGunu = kesim, sonOdemeGunu = sonOdeme,
                    bankaLimitiKurus = bankaLimiti, kendiLimitiKurus = kendiLimiti, asgariOranBinde = oran * 10)
            )
            if (kesilmis != null && kesilmis > 0) kayit.acilisEkstresi(id, Kurus(kesilmis), bugun())
            if (donemIci != null && donemIci > 0) kayit.gecmisTaksit(GecmisTaksitGirisi.KalanBorc(Kurus(donemIci), 1), id, null,
                TaksitTuru.ALISVERIS, bugun(), "Kurulum: dönem içi harcamalar")
        }
        Sonuc.Tamam
    } catch (e: GirdiHatasi) {
        Sonuc.Hata(e.message!!)
    } catch (e: KayitHatasi) {
        Sonuc.Hata(e.message ?: "Kart kaydedilemedi.")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val taksitler: Flow<List<KartTaksidi>> = kartlar.flatMapLatest { liste ->
        val analar = liste.filter { it.anaKartId == null }
        if (analar.isEmpty()) flowOf(emptyList())
        else combine(analar.map { db.taksitDao().aktifTaksitler(it.hesapId, bugun()) }) { it.toList().flatten() }
    }

    private fun taksitGirisi(g: TaksitGirdisi): GecmisTaksitGirisi {
        val tutar = kurusCoz(g.tutar)?.takeIf { it.deger > 0 } ?: throw GirdiHatasi("Tutarı 1.850 biçiminde yaz.")
        val sayi = g.sayi.toIntOrNull()?.takeIf { it >= 1 } ?: throw GirdiHatasi("Taksit sayısını yaz.")
        return when (g.mod) {
            TaksitModu.KALAN -> GecmisTaksitGirisi.KalanBorc(tutar, sayi)
            else -> {
                val siradaki = g.siradaki.toIntOrNull()?.takeIf { it in 1..sayi }
                    ?: throw GirdiHatasi("Sıradaki ekstrede kaçıncı taksit olduğu 1 ile $sayi arasında olmalı.")
                if (g.mod == TaksitModu.AYLIK) GecmisTaksitGirisi.Aylik(tutar, sayi, siradaki)
                else GecmisTaksitGirisi.Toplam(tutar, sayi, siradaki)
            }
        }
    }

    private suspend fun takvim(kartId: Long): KartTakvimi {
        val k = db.kartDao().getir(kartId) ?: throw GirdiHatasi("Kart bulunamadı.")
        val ana = k.anaKartId?.let { db.kartDao().getir(it) } ?: k
        return KartTakvimi(ana.kesimGunu, ana.sonOdemeGunu)
    }

    suspend fun planOnizleme(g: TaksitGirdisi): List<PlanliTaksit>? = try {
        val kartId = g.kartId ?: throw GirdiHatasi("Kart seç.")
        val tk = takvim(kartId)
        when (val giris = taksitGirisi(g)) {
            is GecmisTaksitGirisi.Aylik -> TaksitPlanlayici.gecmisAylik(giris.aylik, giris.toplam, giris.siradakiNo, bugun(), tk)
            is GecmisTaksitGirisi.Toplam -> TaksitPlanlayici.gecmisToplam(giris.tutar, giris.toplam, giris.siradakiNo, bugun(), tk)
            is GecmisTaksitGirisi.KalanBorc -> TaksitPlanlayici.kalanBorc(giris.tutar, giris.kalanSayi, bugun(), tk)
        }
    } catch (e: GirdiHatasi) { null } catch (e: IllegalArgumentException) { null }

    suspend fun taksitEkle(g: TaksitGirdisi): Sonuc = try {
        val kartId = g.kartId ?: throw GirdiHatasi("Taksitin yansıdığı kartı seç.")
        kayit.gecmisTaksit(taksitGirisi(g), kartId, null, g.tur, bugun(), g.aciklama.trim().ifEmpty { null })
        Sonuc.Tamam
    } catch (e: GirdiHatasi) {
        Sonuc.Hata(e.message!!)
    } catch (e: KayitHatasi) {
        Sonuc.Hata(e.message ?: "Taksit kaydedilemedi.")
    }

    suspend fun bitir(): Sonuc {
        db.ayarDao().yaz(Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        return Sonuc.Tamam
    }
}
