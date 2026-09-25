package app.tibi.ui.hareketler

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class KalemOzetiVmTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = t("2025-01-01")))
        kayit.harcama(Kurus(8000), t("2026-08-05"), nakit, null, kalem = "probis")
        kayit.harcama(Kurus(8500), t("2026-08-31"), nakit, null, kalem = "PROBİS")
        kayit.harcama(Kurus(9000), t("2026-09-01"), nakit, null, kalem = "Probis")
        kayit.harcama(Kurus(9000), t("2026-09-20"), nakit, null, kalem = "Probis ")
        kayit.harcama(Kurus(1001), t("2026-09-21"), nakit, null, kalem = "Ekmek")
        kayit.harcama(Kurus(5000), t("2025-08-10"), nakit, null, kalem = "Probis")
        // Geçmiş aktarım sayılmaz.
        db.hareketDao().ekle(Hareket(tur = HareketTuru.HARCAMA, tarih = t("2026-09-15"), tutarKurus = 99999,
            kaynakHesapId = nakit, gecmisAktarim = true, kalem = "Probis", kalemAnahtar = "probis", olusturma = Instant.EPOCH))
    }
    @After fun kapat() { db.close() }

    @Test
    fun `aylara gore sayi toplam ortalama, gecmis aktarim haric`() = runTest {
        val o = KalemOzetiVm(db, "probis") { bugun }.ozet.first()
        assertEquals("Probis", o.baslik)
        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2025, 8)), o.aylar.map { it.ay })
        val eylul = o.aylar[0]
        assertEquals("Eylül 2026", eylul.etiket)
        assertEquals(2, eylul.sayi)
        assertEquals(18000L, eylul.toplamKurus)
        assertEquals("2 kez · 180,00 ₺ · ort. 90,00 ₺", eylul.metin)
        assertEquals("2 kez · 165,00 ₺ · ort. 82,50 ₺", o.aylar[1].metin)
        assertEquals("Ağustos 2025", o.aylar[2].etiket)
        // 2025-08 son 12 ayın dışında kalır.
        assertEquals("Son 12 ay: 4 kez · 345,00 ₺", o.son12Ay)
    }

    @Test
    fun `ortalama kurus tam bolmeyle`() {
        assertEquals("3 kez · 10,00 ₺ · ort. 3,33 ₺", kalemMetni(3, 1000))
    }
}
