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
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Garanti vadesiz", HesapTuru.BANKA, "8.450,00", maasHesabi = true))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Nakit", HesapTuru.NAKIT, "", maasHesabi = false))
        val h = vm.hesaplar.first()
        assertEquals(listOf("Garanti vadesiz", "Nakit"), h.map { it.ad })
        assertEquals(listOf(845000L, 0L), h.map { it.bakiyeKurus })
        assertTrue(h.first().maasHesabi)
    }

    @Test
    fun `hatali hesap girisleri`() = runTest {
        assertIs<Sonuc.Hata>(vm.hesapEkle("", HesapTuru.BANKA, "100", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("X", HesapTuru.BANKA, "12,345", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("X", HesapTuru.KREDI_KARTI, "100", false))
        assertEquals(0, vm.hesaplar.first().size)
    }

    @Test
    fun `bitir kurulumu tamamlar`() = runTest {
        vm.bitir()
        assertEquals("1", db.ayarDao().oku(Anahtarlar.KURULUM_TAMAM))
    }

    @Test
    fun `maas kurali kaydedilir ve donem hesaplanir`() = runTest {
        vm.hesapEkle("Garanti", HesapTuru.BANKA, "0", true)
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
        vm.hesapEkle("Garanti", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, hesap)
        vm.maasKaydet("47.500", "1", HaftaSonuKurali.SONRAKI, hesap)
        assertEquals(4_750_000L, vm.maasKurali.first()!!.tutarKurus)
        assertEquals(1, vm.maasKurali.first()!!.gun)
    }

    @Test
    fun `hatali maas girisleri`() = runTest {
        vm.hesapEkle("Garanti", HesapTuru.BANKA, "0", true)
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
}
