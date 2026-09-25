package app.tibi.veri

import org.junit.Test
import kotlin.test.assertEquals

class KalemTest {
    @Test
    fun `anahtar bosluk ve buyuk harf farkini turkce kurallarla siler`() {
        assertEquals("probis", kalemAnahtari("  Probis "))
        assertEquals("probis", kalemAnahtari("PROBİS"))
        assertEquals("probis", kalemAnahtari("probis"))
        assertEquals("içecek", kalemAnahtari("İçecek"))
        assertEquals("ülker probis", kalemAnahtari(" Ülker   Probis "))
    }

    @Test
    fun `bos ad anahtar vermez`() {
        assertEquals(null, kalemAnahtari(""))
        assertEquals(null, kalemAnahtari("   "))
        assertEquals(null, kalemAdi("  "))
        assertEquals("Ülker Probis", kalemAdi(" Ülker   Probis "))
    }
}
