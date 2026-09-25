package app.tibi.ui.hesaplar

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.GecmisTaksitGirisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class KartDetayVmTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var bonus = 0L
    private var sanal = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, bankaLimitiKurus = 5_000_000, kendiLimitiKurus = 1_500_000))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
        kayit.harcama(Kurus(7999), bugun, sanal, null)
        kayit.harcama(Kurus(90000), bugun, bonus, db.kategoriDao().adIle("Market")!!.id, taksitSayisi = 3, aciklama = "kışlık", kalem = "Mont")
        kayit.harcama(Kurus(60000), bugun, bonus, db.kategoriDao().adIle("Market")!!.id, taksitSayisi = 2)
    }
    @After fun kapat() { db.close() }

    @Test
    fun `ana kart detayi`() = runTest {
        val d = KartDetayVm(db, bonus) { bugun }.detay.filterNotNull().first()
        assertEquals(7 * 185000L + 7999L + 150000L, d.kart.kullanimKurus)
        assertEquals(1_500_000L - (7 * 185000L + 7999L + 150000L), d.kalanKurus)
        assertEquals(t("2026-10-12"), d.siradakiKesim)
        assertEquals(t("2026-10-22"), d.sonOdeme)
        assertEquals(185000L + 7999L + 60000L, d.acikDonemKurus)
        assertEquals(listOf("Market · 1/2", "Mont · 1/3", "Telefon · 6/12"), d.taksitler.map(::taksitEtiketi))
        assertEquals("Mont", d.taksitler[1].kalem)
        assertEquals(listOf("Bonus Sanal"), d.bagliKartlar.map { it.ad })
    }

    @Test
    fun `sanal kart detayi ana kartin rakamlarini gosterir`() = runTest {
        val d = KartDetayVm(db, sanal) { bugun }.detay.filterNotNull().first()
        assertEquals("Bonus Sanal", d.kart.ad)
        assertEquals("Bonus", d.ana.ad)
        assertEquals(1_500_000L - (7 * 185000L + 7999L + 150000L), d.kalanKurus)
        assertEquals(185000L + 7999L + 60000L, d.acikDonemKurus)
    }
}
