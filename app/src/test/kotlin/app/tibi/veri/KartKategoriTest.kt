package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class KartKategoriTest {
    private lateinit var db: TibiVeritabani
    private val gun = LocalDate.parse("2026-09-24")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    private suspend fun kartHesabi(ad: String) =
        db.hesapDao().ekle(Hesap(ad = ad, tur = HesapTuru.KREDI_KARTI, acilisTarihi = gun))

    @Test
    fun `kart ekle ve oku`() = runTest {
        val id = kartHesabi("Bonus")
        db.kartDao().ekle(Kart(hesapId = id, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22,
            bankaLimitiKurus = 5_000_000, kendiLimitiKurus = 1_500_000))
        val k = db.kartDao().getir(id)!!
        assertEquals(KartTuru.ANA, k.kartTuru)
        assertEquals(400, k.asgariOranBinde)
        assertEquals(1_500_000L, k.kendiLimitiKurus)
    }

    @Test
    fun `ek ve sanal kartlar ana karta baglanir`() = runTest {
        val ana = kartHesabi("Bonus")
        db.kartDao().ekle(Kart(hesapId = ana, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22))
        val sanal = kartHesabi("Bonus Sanal")
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = ana, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        assertEquals(listOf(ana, sanal), db.kartDao().bagliKartIdleri(ana))
    }

    @Test
    fun `ilk acilista varsayilan kategoriler hazir`() = runTest {
        val gider = db.kategoriDao().tumu(KategoriYonu.GIDER).first().map { it.ad }
        val gelir = db.kategoriDao().tumu(KategoriYonu.GELIR).first().map { it.ad }
        assertEquals(VarsayilanKategoriler.GIDER, gider)
        assertEquals(VarsayilanKategoriler.GELIR, gelir)
        assertEquals(14, gider.size)
        assertNotNull(db.kategoriDao().adIle("Market"))
    }
}
