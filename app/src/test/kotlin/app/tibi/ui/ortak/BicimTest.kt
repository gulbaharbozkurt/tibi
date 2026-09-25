package app.tibi.ui.ortak

import app.tibi.core.donem.Donem
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class BicimTest {
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Test fun kisa() {
        assertEquals("24 Eyl", t("2026-09-24").kisa())
        assertEquals("1 Oca", t("2027-01-01").kisa())
    }

    @Test fun gunBasligi() {
        assertEquals("Bugün · 24 Eyl", bugun.gunBasligi(bugun))
        assertEquals("Dün · 23 Eyl", t("2026-09-23").gunBasligi(bugun))
        assertEquals("20 Eyl, Pazar", t("2026-09-20").gunBasligi(bugun))
    }

    @Test fun aralik() {
        assertEquals("28 Ağu – 29 Eyl", Donem(t("2026-08-28"), t("2026-09-29")).aralik())
    }

    @Test fun hesapEtiketi() {
        assertEquals("Garanti BBVA · Vadesiz", hesapEtiketi("Garanti BBVA", "Vadesiz"))
        assertEquals("Nakit", hesapEtiketi(null, "Nakit"))
    }
}
