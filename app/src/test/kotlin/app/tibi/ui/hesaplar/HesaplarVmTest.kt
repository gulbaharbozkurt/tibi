package app.tibi.ui.hesaplar

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class HesaplarVmTest {
    private lateinit var db: TibiVeritabani
    private val bugun = LocalDate.parse("2026-09-24")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    private suspend fun kart(ad: String, son4: String, banka: Long?, tur: KartTuru = KartTuru.ANA, ana: Long? = null): Long {
        val id = db.hesapDao().ekle(Hesap(ad = ad, tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun, bankaId = banka))
        db.kartDao().ekle(Kart(hesapId = id, kartTuru = tur, anaKartId = ana, son4 = son4, kesimGunu = 12, sonOdemeGunu = 22))
        return id
    }

    @Test
    fun `hesaplar ve kartlar bankaya gore gruplanir`() = runTest {
        val kayit = KayitServisi(db)
        val garanti = kayit.bankaBulVeyaEkle("Garanti BBVA")
        val isb = kayit.bankaBulVeyaEkle("İş Bankası")
        db.bankaDao().ekle(Banka(ad = "Boş Banka"))
        db.hesapDao().ekle(Hesap(ad = "Vadesiz", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun, bankaId = garanti))
        db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisBakiyeKurus = 130000, acilisTarihi = bugun))
        val bonus = kart("Bonus", "4821", garanti)
        kart("Bonus Sanal", "9054", garanti, KartTuru.SANAL, bonus)
        kart("Maximum", "1190", isb)
        kart("Diğer", "0001", null)

        val d = HesaplarVm(db).durum.first()
        assertEquals(listOf("Garanti BBVA", "İş Bankası"), d.bankalar.map { it.banka.ad })
        val g = d.bankalar.first()
        assertEquals(listOf("Vadesiz"), g.hesaplar.map { it.ad })
        assertEquals(listOf(845000L), g.hesaplar.map { it.bakiyeKurus })
        assertEquals(listOf("Bonus", "Bonus Sanal"), g.kartlar.map { it.ad })
        assertEquals(emptyList(), d.bankalar[1].hesaplar)
        assertEquals(listOf("Maximum"), d.bankalar[1].kartlar.map { it.ad })
        assertEquals(listOf("Nakit"), d.nakit.map { it.ad })
        assertEquals(listOf("Diğer"), d.bankasizKartlar.map { it.ad })
    }
}
