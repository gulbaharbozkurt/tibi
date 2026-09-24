package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.dao.AylikYuk
import app.tibi.veri.tablo.EkstreDurumu
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KartKayitTest {
    private lateinit var db: TibiVeritabani
    private lateinit var servis: KayitServisi
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var banka = 0L
    private var bonus = 0L
    private var sanal = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        servis = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 2_000_000, acilisTarihi = bugun))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, kendiLimitiKurus = 1_500_000))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        // Ek/sanal kartın kendi kesim günü ana karttan farklı yazılsa bile ana kartınki kullanılır.
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 1, sonOdemeGunu = 11))
    }
    @After fun kapat() { db.close() }

    private suspend fun kullanim() = db.taksitDao().limitKullanimi(bonus).first()
    private suspend fun giyim() = db.kategoriDao().adIle("Giyim")!!.id

    @Test
    fun `taksitli kart harcamasi satirlara bolunur`() = runTest {
        val id = servis.harcama(Kurus(124990), bugun, bonus, giyim(), taksitSayisi = 3)
        val s = db.taksitDao().hareketin(id)
        assertEquals(listOf(41664L, 41663L, 41663L), s.map { it.tutarKurus })
        assertEquals(listOf(t("2026-10-12"), t("2026-11-12"), t("2026-12-12")), s.map { it.ekstreKesimTarihi })
        assertTrue(s.all { it.kartId == bonus })
        assertEquals(124990L, kullanim())
    }

    @Test
    fun `sanal kart harcamasi ana kartin takvimi ve limitiyle`() = runTest {
        val id = servis.harcama(Kurus(7999), bugun, sanal, null)
        val s = db.taksitDao().hareketin(id).single()
        assertEquals(sanal, s.kartId)
        assertEquals(t("2026-10-12"), s.ekstreKesimTarihi)
        assertEquals(7999L, kullanim())
    }

    @Test
    fun `kart odemesi limit kullanimini azaltir ve bankadan cikar`() = runTest {
        servis.harcama(Kurus(500000), bugun, bonus, giyim())
        servis.kartOdemesi(Kurus(200000), bugun, banka, bonus)
        assertEquals(300000L, kullanim())
        assertEquals(2_000_000L - 200_000L, db.hesapDao().bakiye(banka).first())
    }

    @Test
    fun `gecmis taksit sadece odenmemisleri sayar`() = runTest {
        servis.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
        assertEquals(12, db.taksitDao().sayi())
        assertEquals(7 * 185000L, kullanim())
    }

    @Test
    fun `gecmis taksit kalan borc ile`() = runTest {
        servis.gecmisTaksit(GecmisTaksitGirisi.KalanBorc(Kurus(1295000), 7), bonus, null, TaksitTuru.EKSTRE_TAKSIT, bugun)
        assertEquals(1_295_000L, kullanim())
    }

    @Test
    fun `acilis ekstresi limit kullanimina eklenir`() = runTest {
        val id = servis.acilisEkstresi(bonus, Kurus(1234000), bugun)
        val e = db.ekstreDao().getir(id)!!
        assertEquals(t("2026-09-12"), e.kesimTarihi)
        assertEquals(t("2026-09-22"), e.sonOdemeTarihi)
        assertEquals(493600L, e.asgariKurus)   // %40
        assertEquals(EkstreDurumu.KESILDI, e.durum)
        assertTrue(e.acilis)
        assertEquals(1_234_000L, kullanim())
    }

    @Test
    fun `aylik taksit yuku bugunden itibaren`() = runTest {
        servis.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun)
        servis.harcama(Kurus(124990), bugun, bonus, giyim(), taksitSayisi = 3)
        val yuk = db.taksitDao().aylikYuk(bugun).first()
        assertEquals(AylikYuk(t("2026-10-12"), 185000L + 41664L), yuk.first())
        assertEquals(AylikYuk(t("2027-04-12"), 185000L), yuk.last())
        assertEquals(7, yuk.size)
    }

    @Test
    fun `nakit hesaba kart odemesi yapilamaz`() = runTest {
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = bugun))
        kotlin.test.assertFailsWith<KayitHatasi> { servis.kartOdemesi(Kurus(100), bugun, banka, nakit) }
    }
}
