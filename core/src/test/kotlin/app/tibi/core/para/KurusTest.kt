package app.tibi.core.para

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KurusTest {
    @Test
    fun `toplama ve cikarma`() {
        assertEquals(Kurus(1500), Kurus(1000) + Kurus(500))
        assertEquals(Kurus(500), Kurus(1000) - Kurus(500))
        assertEquals(Kurus(3000), Kurus(1000) * 3)
    }

    @Test
    fun `tl yardimcisi`() {
        assertEquals(Kurus(124990), Kurus.tl(1249, 90))
        assertEquals(Kurus(4500000), Kurus.tl(45000))
    }

    @Test
    fun `bolmede artan kurus ilk parcaya eklenir`() {
        assertEquals(listOf(Kurus(3334), Kurus(3333), Kurus(3333)), Kurus(10000).bol(3))
        assertEquals(listOf(Kurus(41664), Kurus(41663), Kurus(41663)), Kurus(124990).bol(3))
    }

    @Test
    fun `bolme parcalarinin toplami asli verir`() {
        for (n in 1..24) {
            assertEquals(Kurus(1295001), Kurus(1295001).bol(n).topla())
        }
    }

    @Test
    fun `bolme en az bir parca ister`() {
        assertFailsWith<IllegalArgumentException> { Kurus(100).bol(0) }
    }

    @Test
    fun `binde oran yarim kurusu yukari yuvarlar`() {
        assertEquals(Kurus(4936), Kurus(12340).oran(400))   // %40 asgari
        assertEquals(Kurus(1), Kurus(1).oran(500))          // 0,5 kuruş → 1
        assertEquals(Kurus(0), Kurus(1).oran(499))
    }

    @Test
    fun `karsilastirma`() {
        assertTrue(Kurus(100) < Kurus(200))
        assertEquals(Kurus(200), listOf(Kurus(100), Kurus(200)).max())
    }
}
