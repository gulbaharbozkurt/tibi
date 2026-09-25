package app.tibi.ui.kurulum

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.donem.Donem
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KurulumVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: KurulumVm
    private val bugun = LocalDate.parse("2026-09-24")

    @Before fun ac() {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        vm = KurulumVm(db, KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }) { bugun }
    }
    @After fun kapat() { db.close() }

    @Test
    fun `hitap kaydedilir, bos olamaz`() = runTest {
        assertIs<Sonuc.Hata>(vm.hitapKaydet("   "))
        assertEquals(Sonuc.Tamam, vm.hitapKaydet("  Gülbahar "))
        assertEquals("Gülbahar", db.ayarDao().oku(Anahtarlar.HITAP))
    }

    @Test
    fun `hesap eklenir, bakiye virgullu okunur`() = runTest {
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Garanti BBVA", "Vadesiz", HesapTuru.BANKA, "8.450,00", maasHesabi = true))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("", "Nakit", HesapTuru.NAKIT, "", maasHesabi = false))
        val h = vm.hesaplar.first()
        assertEquals(listOf("Vadesiz", "Nakit"), h.map { it.ad })
        assertEquals(listOf(845000L, 0L), h.map { it.bakiyeKurus })
        assertTrue(h.first().maasHesabi)
        assertEquals(listOf("Garanti BBVA", null), h.map { it.bankaAdi })
    }

    @Test
    fun `hatali hesap girisleri`() = runTest {
        assertIs<Sonuc.Hata>(vm.hesapEkle("", "X", HesapTuru.BANKA, "100", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("Garanti", "X", HesapTuru.BANKA, "12,345", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("Garanti", "X", HesapTuru.KREDI_KARTI, "100", false))
        assertEquals(0, vm.hesaplar.first().size)
        assertEquals(0, vm.bankalar.first().size)
    }

    @Test
    fun `banka bulunur ya da eklenir, buyuk kucuk harf fark etmez`() = runTest {
        assertEquals(Sonuc.Tamam, vm.hesapEkle(" Garanti BBVA ", "Vadesiz", HesapTuru.BANKA, "100", false))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("garanti bbva", "Birikim", HesapTuru.BANKA, "200", false))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("İŞ BANKASI", "Vadesiz", HesapTuru.BANKA, "300", false))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("iş bankası", "Maaş", HesapTuru.BANKA, "300", false))
        assertEquals(listOf("Garanti BBVA", "İŞ BANKASI"), vm.bankalar.first().map { it.ad })
        val h = vm.hesaplar.first()
        assertEquals(2, h.map { it.bankaId }.toSet().size)
        assertEquals(listOf("Garanti BBVA", "Garanti BBVA", "İŞ BANKASI", "İŞ BANKASI"), h.map { it.bankaAdi })
    }

    @Test
    fun `bos hesap adi varsayilan olur`() = runTest {
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Garanti BBVA", "  ", HesapTuru.BANKA, "", false))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Garanti BBVA", "", HesapTuru.NAKIT, "", false))
        val h = vm.hesaplar.first()
        assertEquals(listOf("Vadesiz", "Nakit"), h.map { it.ad })
        assertEquals(null, h.last().bankaId)   // nakit bankayı yok sayar
        assertEquals(1, vm.bankalar.first().size)
    }

    @Test
    fun `banka hesabi bankasiz eklenemez`() = runTest {
        assertEquals(Sonuc.Hata("Hesabın bağlı olduğu bankayı yaz."), vm.hesapEkle("  ", "Vadesiz", HesapTuru.BANKA, "100", false))
        assertEquals(0, vm.hesaplar.first().size)
        assertEquals(0, vm.bankalar.first().size)
    }

    @Test
    fun `ana kart bankaya baglanir, bos banka bankasiz birakir`() = runTest {
        vm.hesapEkle("Garanti BBVA", "Vadesiz", HesapTuru.BANKA, "0", false)
        assertEquals(Sonuc.Tamam, vm.kartEkle(KartGirdisi(ad = "Bonus", bankaAdi = "garanti BBVA", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22")))
        assertEquals(Sonuc.Tamam, vm.kartEkle(KartGirdisi(ad = "Maximum", bankaAdi = "İş Bankası", son4 = "1190", kesimGunu = "5", sonOdemeGunu = "15")))
        assertEquals(Sonuc.Tamam, vm.kartEkle(KartGirdisi(ad = "Diğer", bankaAdi = " ", son4 = "0001", kesimGunu = "5", sonOdemeGunu = "15")))
        val garanti = vm.bankalar.first().first { it.ad == "Garanti BBVA" }.id
        val k = vm.kartlar.first()
        assertEquals(garanti, k.first { it.ad == "Bonus" }.bankaId)
        assertEquals("Garanti BBVA", k.first { it.ad == "Bonus" }.bankaAdi)
        assertEquals("İş Bankası", k.first { it.ad == "Maximum" }.bankaAdi)
        assertEquals(null, k.first { it.ad == "Diğer" }.bankaId)
        assertEquals(listOf("Garanti BBVA", "İş Bankası"), vm.bankalar.first().map { it.ad })
    }

    @Test
    fun `sanal kart ana kartin bankasini alir`() = runTest {
        vm.kartEkle(KartGirdisi(ad = "Bonus", bankaAdi = "Garanti BBVA", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22"))
        val ana = vm.kartlar.first().single()
        assertEquals(Sonuc.Tamam, vm.kartEkle(KartGirdisi(ad = "Bonus Sanal", tur = KartTuru.SANAL, anaKartId = ana.hesapId,
            bankaAdi = "Akbank", son4 = "9054")))
        val s = vm.kartlar.first().first { it.ad == "Bonus Sanal" }
        assertEquals(ana.bankaId, s.bankaId)
        assertEquals("Garanti BBVA", s.bankaAdi)
        assertEquals(listOf("Garanti BBVA"), vm.bankalar.first().map { it.ad })
    }

    @Test
    fun `hatali kartta banka da yazilmaz`() = runTest {
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", bankaAdi = "Akbank", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22",
            kesilmisEkstre = "-")))
        assertEquals(0, vm.bankalar.first().size)
    }

    @Test
    fun `bitir kurulumu tamamlar`() = runTest {
        vm.bitir()
        assertEquals("1", db.ayarDao().oku(Anahtarlar.KURULUM_TAMAM))
    }

    @Test
    fun `maas kurali kaydedilir ve donem hesaplanir`() = runTest {
        vm.hesapEkle("Garanti", "Vadesiz", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        assertEquals(Donem(LocalDate.parse("2026-08-28"), LocalDate.parse("2026-09-29")), vm.donemOnizleme("30", HaftaSonuKurali.ONCEKI))
        assertEquals(Sonuc.Tamam, vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, hesap))
        val k = vm.maasKurali.first()!!
        assertEquals(4_500_000L, k.tutarKurus)
        assertEquals(30, k.gun)
        assertEquals(hesap, k.hesapId)
    }

    @Test
    fun `maas yeniden kaydedilince eskisi pasif olur`() = runTest {
        vm.hesapEkle("Garanti", "Vadesiz", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, hesap)
        vm.maasKaydet("47.500", "1", HaftaSonuKurali.SONRAKI, hesap)
        assertEquals(4_750_000L, vm.maasKurali.first()!!.tutarKurus)
        assertEquals(1, vm.maasKurali.first()!!.gun)
    }

    @Test
    fun `hatali maas girisleri`() = runTest {
        vm.hesapEkle("Garanti", "Vadesiz", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        assertIs<Sonuc.Hata>(vm.maasKaydet("", "30", HaftaSonuKurali.ONCEKI, hesap))
        assertIs<Sonuc.Hata>(vm.maasKaydet("45.000", "32", HaftaSonuKurali.ONCEKI, hesap))
        assertIs<Sonuc.Hata>(vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, null))
        assertEquals(null, vm.donemOnizleme("0", HaftaSonuKurali.ONCEKI))
        assertEquals(null, vm.maasKurali.first())
    }

    @Test
    fun `ana kart acilis borcuyla eklenir`() = runTest {
        val s = vm.kartEkle(KartGirdisi(ad = "Bonus", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22",
            bankaLimiti = "50.000", kendiLimiti = "15.000", kesilmisEkstre = "12.340", donemIci = "5.980"))
        assertEquals(Sonuc.Tamam, s)
        val k = vm.kartlar.first().single()
        assertEquals("Bonus", k.ad)
        assertEquals(1_500_000L, k.kendiLimitiKurus)
        assertEquals(5_000_000L, k.bankaLimitiKurus)
        assertEquals(400, k.asgariOranBinde)
        assertEquals(1_234_000L + 598_000L, k.kullanimKurus)
    }

    @Test
    fun `sanal kart ana kartin gunlerini alir`() = runTest {
        vm.kartEkle(KartGirdisi(ad = "Bonus", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22", kendiLimiti = "15.000"))
        val ana = vm.kartlar.first().single().hesapId
        assertEquals(Sonuc.Tamam, vm.kartEkle(KartGirdisi(ad = "Bonus Sanal", tur = KartTuru.SANAL, anaKartId = ana, son4 = "9054",
            kendiLimiti = "999", kesilmisEkstre = "100")))
        val s = vm.kartlar.first().first { it.ad == "Bonus Sanal" }
        assertEquals(12, s.kesimGunu)
        assertEquals(22, s.sonOdemeGunu)
        assertEquals(null, s.kendiLimitiKurus)
        assertEquals(0L, vm.kartlar.first().first { it.hesapId == ana }.kullanimKurus)
    }

    @Test
    fun `hatali kart girisleri hicbir sey yazmaz`() = runTest {
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", son4 = "48", kesimGunu = "12", sonOdemeGunu = "22")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", son4 = "4821", kesimGunu = "0", sonOdemeGunu = "22")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22", kendiLimiti = "abc")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", tur = KartTuru.EK, son4 = "4821")))   // ana kart seçilmedi
        assertEquals(0, vm.kartlar.first().size)
    }

    private suspend fun bonusEkle(): Long {
        vm.kartEkle(KartGirdisi(ad = "Bonus", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22", kendiLimiti = "15.000"))
        return vm.kartlar.first().single().hesapId
    }

    @Test
    fun `taksit onizlemesi yazmadan plani gosterir`() = runTest {
        val bonus = bonusEkle()
        val p = vm.planOnizleme(TaksitGirdisi(kartId = bonus, mod = TaksitModu.AYLIK, tutar = "1.850", sayi = "12", siradaki = "6"))!!
        assertEquals(12, p.size)
        assertEquals(LocalDate.parse("2027-04-12"), p.last().ekstreKesimTarihi)
        assertEquals(0L, vm.kartlar.first().single().kullanimKurus)
        assertEquals(null, vm.planOnizleme(TaksitGirdisi(kartId = bonus, mod = TaksitModu.AYLIK, tutar = "1.850", sayi = "12", siradaki = "13")))
    }

    @Test
    fun `uc giris yoluyla taksit eklenir`() = runTest {
        val bonus = bonusEkle()
        assertEquals(Sonuc.Tamam, vm.taksitEkle(TaksitGirdisi(kartId = bonus, aciklama = "Telefon", mod = TaksitModu.AYLIK, tutar = "1.850", sayi = "12", siradaki = "6")))
        assertEquals(Sonuc.Tamam, vm.taksitEkle(TaksitGirdisi(kartId = bonus, mod = TaksitModu.TOPLAM, tutar = "3.000", sayi = "3", siradaki = "1")))
        assertEquals(Sonuc.Tamam, vm.taksitEkle(TaksitGirdisi(kartId = bonus, tur = TaksitTuru.EKSTRE_TAKSIT, mod = TaksitModu.KALAN, tutar = "4.000", sayi = "4")))
        assertEquals(7 * 185000L + 300000L + 400000L, vm.kartlar.first().single().kullanimKurus)
        assertEquals(3, vm.taksitler.first().size)
    }

    @Test
    fun `hatali taksit girisleri`() = runTest {
        val bonus = bonusEkle()
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = null, tutar = "100", sayi = "3", siradaki = "1")))
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = bonus, tutar = "", sayi = "3", siradaki = "1")))
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = bonus, tutar = "100", sayi = "", siradaki = "1")))
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = bonus, tutar = "100", sayi = "3", siradaki = "4")))
        assertEquals(0L, vm.kartlar.first().single().kullanimKurus)
    }
}
