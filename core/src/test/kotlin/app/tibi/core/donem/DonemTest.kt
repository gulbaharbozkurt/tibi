package app.tibi.core.donem

import app.tibi.core.tarih.HaftaSonuKurali
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DonemTest {
    private fun t(s: String) = LocalDate.parse(s)
    private val maas30 = MaasKurali(30, HaftaSonuKurali.ONCEKI)

    @Test
    fun `ay ortasinda donem onceki maastan baslar`() {
        // 30 Ağustos 2026 pazar → maaş 28 Ağustos cuma
        val d = DonemHesaplayici.donem(maas30, t("2026-09-24"))
        assertEquals(Donem(t("2026-08-28"), t("2026-09-29")), d)
        assertEquals(33, d.gunSayisi)
    }

    @Test
    fun `maas gunu yeni donemin ilk gunudur`() {
        val d = DonemHesaplayici.donem(maas30, t("2026-09-30"))
        assertEquals(Donem(t("2026-09-30"), t("2026-10-29")), d)
    }

    @Test
    fun `maastan bir gun once eski donemin son gunudur`() {
        val d = DonemHesaplayici.donem(maas30, t("2026-09-29"))
        assertEquals(t("2026-08-28"), d.baslangic)
        assertEquals(t("2026-09-29"), d.bitis)
    }

    @Test
    fun `sonraki is gunune kayan maas ay sinirini asabilir`() {
        // 31 Ekim 2026 cumartesi → 2 Kasım pazartesi; 1 Kasım hâlâ eylül sonunda başlayan dönemde
        val k = MaasKurali(31, HaftaSonuKurali.SONRAKI)
        val d = DonemHesaplayici.donem(k, t("2026-11-01"))
        assertEquals(Donem(t("2026-09-30"), t("2026-11-01")), d)
    }

    @Test
    fun `ayni gun kurali kaydirmaz`() {
        val d = DonemHesaplayici.donem(MaasKurali(15, HaftaSonuKurali.AYNI), t("2026-09-15"))
        assertEquals(Donem(t("2026-09-15"), t("2026-10-14")), d)
    }

    @Test
    fun `icerir ve ilerleme`() {
        val d = Donem(t("2026-08-28"), t("2026-09-29"))
        assertTrue(d.icerir(t("2026-08-28")))
        assertTrue(d.icerir(t("2026-09-29")))
        assertFalse(d.icerir(t("2026-09-30")))
        assertEquals(848, d.ilerlemeBinde(t("2026-09-24")))  // 28/33 gün
        assertEquals(1000, d.ilerlemeBinde(t("2026-09-29")))
        assertEquals(30, d.ilerlemeBinde(t("2026-08-28")))   // 1/33
    }
}
