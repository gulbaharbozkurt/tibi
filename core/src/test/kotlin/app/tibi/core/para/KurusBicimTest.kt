package app.tibi.core.para

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KurusBicimTest {
    @Test
    fun `turkce bicimle`() {
        assertEquals("1.249,90 ₺", Kurus(124990).bicimle())
        assertEquals("0,00 ₺", Kurus(0).bicimle())
        assertEquals("0,05 ₺", Kurus(5).bicimle())
        assertEquals("79,99 ₺", Kurus(7999).bicimle())
        assertEquals("1.000.000,00 ₺", Kurus(100_000_000).bicimle())
        assertEquals("-79,99 ₺", Kurus(-7999).bicimle())
    }

    @Test
    fun `gecerli girisleri coz`() {
        assertEquals(Kurus(124990), kurusCoz("1.249,90"))
        assertEquals(Kurus(124990), kurusCoz("1249,9"))
        assertEquals(Kurus(7999), kurusCoz("79,99 ₺"))
        assertEquals(Kurus(4500000), kurusCoz("45000"))
        assertEquals(Kurus(4500000), kurusCoz("45.000"))
        assertEquals(Kurus(50), kurusCoz(" 0,5 "))
    }

    @Test
    fun `hatali girislerde null`() {
        assertNull(kurusCoz(""))
        assertNull(kurusCoz("abc"))
        assertNull(kurusCoz("12,345"))   // 3 ondalık hane
        assertNull(kurusCoz("1,2,3"))
        assertNull(kurusCoz("-5"))       // tutarlar pozitif girilir
    }

    @Test
    fun `bicimle ve coz birbirinin tersi`() {
        for (d in listOf(0L, 1L, 99L, 100L, 124990L, 987654321L)) {
            assertEquals(Kurus(d), kurusCoz(Kurus(d).bicimle()))
        }
    }
}
