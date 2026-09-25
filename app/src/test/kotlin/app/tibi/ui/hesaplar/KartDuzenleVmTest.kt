package app.tibi.ui.hesaplar

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.ui.Sonuc
import app.tibi.veri.GecmisTaksitGirisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.flow.filterNotNull
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KartDuzenleVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var kayit: KayitServisi
    private val bugun = LocalDate.parse("2026-09-24")
    private var garanti = 0L
    private var bonus = 0L
    private var sanal = 0L
    private var telefon = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        garanti = db.bankaDao().ekle(Banka(ad = "Garanti BBVA"))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun, bankaId = garanti))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, bankaLimitiKurus = 5_000_000,
            kendiLimitiKurus = 1_500_000, asgariOranBinde = 400))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun, bankaId = garanti))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        telefon = kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
    }
    @After fun kapat() { db.close() }

    private fun vm(id: Long) = KartDuzenleVm(db, kayit, id)

    @Test
    fun `girdi veritabanindan doldurulur`() = runTest {
        val g = vm(bonus).girdi.filterNotNull().first()
        assertEquals("Bonus", g.ad)
        assertEquals("Garanti BBVA", g.bankaAdi)
        assertEquals(KartTuru.ANA, g.tur)
        assertEquals("4821", g.son4)
        assertEquals("12", g.kesimGunu)
        assertEquals("22", g.sonOdemeGunu)
        assertEquals("50.000,00", g.bankaLimiti)
        assertEquals("15.000,00", g.kendiLimiti)
        assertEquals("40", g.asgariOran)
        val s = vm(sanal).girdi.filterNotNull().first()
        assertEquals(KartTuru.SANAL, s.tur)
        assertEquals(bonus, s.anaKartId)
    }

    @Test
    fun `limit ve kesim gunu degisir, taksit tarihleri yerinde kalir`() = runTest {
        val onceki = db.taksitDao().hareketin(telefon).map { it.ekstreKesimTarihi }
        val v = vm(bonus)
        val g = v.girdi.filterNotNull().first()
        assertEquals(Sonuc.Tamam, v.kaydet(g.copy(kendiLimiti = "20.000", kesimGunu = "5", sonOdemeGunu = "15", asgariOran = "30", son4 = "4822")))
        val k = db.kartDao().kartlar().first().first { it.hesapId == bonus }
        assertEquals(2_000_000L, k.kendiLimitiKurus)
        assertEquals(5, k.kesimGunu)
        assertEquals(15, k.sonOdemeGunu)
        assertEquals(300, k.asgariOranBinde)
        assertEquals("4822", k.son4)
        assertEquals(KartTuru.ANA, k.kartTuru)
        assertEquals(onceki, db.taksitDao().hareketin(telefon).map { it.ekstreKesimTarihi })
        // Bağlı sanal kart ana kartın günlerini izler.
        assertEquals(5, db.kartDao().getir(sanal)!!.kesimGunu)
    }

    @Test
    fun `tur ve ana kart degismez, acilis alanlari yok sayilir`() = runTest {
        val v = vm(bonus)
        val g = v.girdi.filterNotNull().first()
        val kullanimOnce = db.kartDao().kartlar().first().first { it.hesapId == bonus }.kullanimKurus
        assertEquals(Sonuc.Tamam, v.kaydet(g.copy(tur = KartTuru.EK, anaKartId = sanal, kesilmisEkstre = "1.000", donemIci = "500")))
        val k = db.kartDao().getir(bonus)!!
        assertEquals(KartTuru.ANA, k.kartTuru)
        assertNull(k.anaKartId)
        assertEquals(kullanimOnce, db.kartDao().kartlar().first().first { it.hesapId == bonus }.kullanimKurus)
    }

    @Test
    fun `sanal kartta yalnizca ad ve son4 degisir`() = runTest {
        val v = vm(sanal)
        val g = v.girdi.filterNotNull().first()
        assertEquals(Sonuc.Tamam, v.kaydet(g.copy(ad = "İnternet", son4 = "1111", kesimGunu = "3", kendiLimiti = "999", bankaAdi = "Akbank",
            asgariOran = "10")))
        val k = db.kartDao().getir(sanal)!!
        assertEquals("1111", k.son4)
        assertEquals(12, k.kesimGunu)
        assertNull(k.kendiLimitiKurus)
        assertEquals(400, k.asgariOranBinde)
        val h = db.hesapDao().getir(sanal)!!
        assertEquals("İnternet", h.ad)
        assertEquals(garanti, h.bankaId)
        assertNull(db.bankaDao().adIle("Akbank"))
    }

    @Test
    fun `hatali giris hata verir, hicbir sey degismez`() = runTest {
        val v = vm(bonus)
        val g = v.girdi.filterNotNull().first()
        assertIs<Sonuc.Hata>(v.kaydet(g.copy(ad = " ")))
        assertIs<Sonuc.Hata>(v.kaydet(g.copy(son4 = "12")))
        assertIs<Sonuc.Hata>(v.kaydet(g.copy(kesimGunu = "32")))
        assertIs<Sonuc.Hata>(v.kaydet(g.copy(kendiLimiti = "abc", bankaAdi = "Akbank")))
        assertIs<Sonuc.Hata>(v.kaydet(g.copy(asgariOran = "101")))
        val k = db.kartDao().getir(bonus)!!
        assertEquals(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, bankaLimitiKurus = 5_000_000,
            kendiLimitiKurus = 1_500_000, asgariOranBinde = 400), k)
        assertEquals("Bonus", db.hesapDao().getir(bonus)!!.ad)
        assertNull(db.bankaDao().adIle("Akbank"))
    }

    @Test
    fun `ana kart bankasi degisince bagli kartlar da tasinir`() = runTest {
        val v = vm(bonus)
        val g = v.girdi.filterNotNull().first()
        assertEquals(Sonuc.Tamam, v.kaydet(g.copy(bankaAdi = "Akbank")))
        val akbank = db.bankaDao().adIle("Akbank")!!.id
        assertEquals(akbank, db.hesapDao().getir(bonus)!!.bankaId)
        assertEquals(akbank, db.hesapDao().getir(sanal)!!.bankaId)
    }

    @Test
    fun `ana kart kapatilinca sanal karti da kapanir`() = runTest {
        assertEquals(Sonuc.Tamam, vm(bonus).kapat())
        assertTrue(db.kartDao().kartlar().first().isEmpty())
        assertTrue(db.hesapDao().getir(bonus)!!.arsiv)
        assertTrue(db.hesapDao().getir(sanal)!!.arsiv)
        assertEquals(12, db.taksitDao().hareketin(telefon).size)   // geçmiş durur
    }

    @Test
    fun `sanal kart kapatilinca ana kart kalir`() = runTest {
        assertEquals(Sonuc.Tamam, vm(sanal).kapat())
        assertEquals(listOf(bonus), db.kartDao().kartlar().first().map { it.hesapId })
    }
}
