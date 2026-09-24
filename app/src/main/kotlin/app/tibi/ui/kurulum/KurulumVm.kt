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
import app.tibi.veri.GecmisTaksitGirisi
import app.tibi.veri.KayitHatasi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import app.tibi.veri.tablo.TaksitTuru
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

data class KartGirdisi(
    val ad: String = "",
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
            val id = db.hesapDao().ekle(Hesap(ad = g.ad.trim(), tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun()))
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

    suspend fun bitir(): Sonuc {
        db.ayarDao().yaz(Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        return Sonuc.Tamam
    }
}
