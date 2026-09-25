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

    @Test fun limitMetni() {
        assertEquals("1.294,26 ₺ kaldı", limitMetni(129426L))
        assertEquals("1.705,74 ₺ aşıldı", limitMetni(-170574L))
        assertEquals("0,00 ₺ kaldı", limitMetni(0L))
    }

    @Test fun utcMilisTarih() {
        val gun = t("2026-09-25")
        assertEquals(1790294400000L, gun.utcMillis())
        assertEquals(gun, 1790294400000L.utcTarih())
        assertEquals(gun, (1790294400000L + 86_399_999L).utcTarih())
        listOf("2000-01-01", "2024-02-29", "2026-03-29", "2026-10-25", "2027-12-31").forEach { assertEquals(t(it), t(it).utcMillis().utcTarih()) }
    }
}
