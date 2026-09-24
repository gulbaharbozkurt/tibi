package app.tibi.core.tarih

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class TakvimTest {
    private fun t(s: String) = LocalDate.parse(s)

    @Test
    fun `ayin gunu kisa ayda son gune sabitlenir`() {
        assertEquals(t("2026-02-28"), YearMonth.of(2026, 2).gun(31))
        assertEquals(t("2028-02-29"), YearMonth.of(2028, 2).gun(30))
        assertEquals(t("2026-09-30"), YearMonth.of(2026, 9).gun(31))
        assertEquals(t("2026-09-12"), YearMonth.of(2026, 9).gun(12))
    }

    @Test
    fun `cumartesi kaydirma`() {
        assertEquals(t("2026-09-25"), t("2026-09-26").haftaSonuKaydir(HaftaSonuKurali.ONCEKI))
        assertEquals(t("2026-09-28"), t("2026-09-26").haftaSonuKaydir(HaftaSonuKurali.SONRAKI))
        assertEquals(t("2026-09-26"), t("2026-09-26").haftaSonuKaydir(HaftaSonuKurali.AYNI))
    }

    @Test
    fun `pazar kaydirma`() {
        assertEquals(t("2026-09-25"), t("2026-09-27").haftaSonuKaydir(HaftaSonuKurali.ONCEKI))
        assertEquals(t("2026-09-28"), t("2026-09-27").haftaSonuKaydir(HaftaSonuKurali.SONRAKI))
    }

    @Test
    fun `hafta ici degismez`() {
        for (k in HaftaSonuKurali.entries) {
            assertEquals(t("2026-09-24"), t("2026-09-24").haftaSonuKaydir(k))
        }
    }
}
