package app.tibi.core.kart

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class KartTakvimiTest {
    private fun t(s: String) = LocalDate.parse(s)
    private val bonus = KartTakvimi(kesimGunu = 12, sonOdemeGunu = 22)

    @Test
    fun `kesim gunu yapilan harcama o ekstreye girer`() {
        assertEquals(t("2026-09-12"), bonus.ilgiliKesim(t("2026-09-12")))
        assertEquals(t("2026-09-12"), bonus.ilgiliKesim(t("2026-09-01")))
    }

    @Test
    fun `kesimden sonraki harcama sonraki ekstreye girer`() {
        assertEquals(t("2026-10-12"), bonus.ilgiliKesim(t("2026-09-13")))
        assertEquals(t("2027-01-12"), bonus.ilgiliKesim(t("2026-12-24")))
    }

    @Test
    fun `kesim 31 kisa ayda son gune sabitlenir`() {
        val k = KartTakvimi(31, 10)
        assertEquals(t("2027-02-28"), k.kesimTarihi(YearMonth.of(2027, 2)))
        assertEquals(t("2027-02-28"), k.ilgiliKesim(t("2027-02-01")))
    }

    @Test
    fun `son odeme ayni ayda`() {
        assertEquals(t("2026-09-22"), bonus.sonOdeme(t("2026-09-12")))  // salı
    }

    @Test
    fun `son odeme sonraki ayda`() {
        val k = KartTakvimi(20, 5)
        assertEquals(t("2026-10-05"), k.sonOdeme(t("2026-09-20")))       // pazartesi
    }

    @Test
    fun `son odeme hafta sonuysa pazartesiye kayar`() {
        val k = KartTakvimi(12, 26)
        assertEquals(t("2026-09-28"), k.sonOdeme(t("2026-09-12")))       // 26 Eylül cumartesi
    }
}
