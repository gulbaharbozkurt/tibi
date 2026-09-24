package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class TablolarTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val an = Instant.parse("2026-09-24T09:00:00Z")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    @Test
    fun `hareketler tarih araliginda yeniden eskiye`() = runTest {
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = t("2026-09-01")))
        fun h(tarih: String, tutar: Long) = Hareket(tur = HareketTuru.HARCAMA, tarih = t(tarih), tutarKurus = tutar,
            kaynakHesapId = nakit, olusturma = an)
        db.hareketDao().ekle(h("2026-09-10", 100))
        db.hareketDao().ekle(h("2026-09-20", 200))
        db.hareketDao().ekle(h("2026-10-01", 300))
        val liste = db.hareketDao().aralik(t("2026-09-01"), t("2026-09-30")).first()
        assertEquals(listOf(200L, 100L), liste.map { it.tutarKurus })
    }

    @Test
    fun `maas kurali`() = runTest {
        assertNull(db.duzenliKuralDao().maasKurali())
        val hesap = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisTarihi = t("2026-09-01")))
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = hesap, baslangic = t("2026-09-01")))
        val k = db.duzenliKuralDao().maasKurali()!!
        assertEquals(30, k.gun)
        assertEquals(HaftaSonuKurali.ONCEKI, k.haftaSonuKurali)
    }

    @Test
    fun `ayar yaz uzerine yaz oku`() = runTest {
        assertNull(db.ayarDao().oku("hitap"))
        db.ayarDao().yaz(Ayar("hitap", "Gülbahar"))
        db.ayarDao().yaz(Ayar("hitap", "Gül"))
        assertEquals("Gül", db.ayarDao().oku("hitap"))
    }
}
