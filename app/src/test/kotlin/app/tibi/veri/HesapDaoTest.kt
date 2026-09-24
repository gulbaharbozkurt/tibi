package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class HesapDaoTest {
    private lateinit var db: TibiVeritabani

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    @Test
    fun `hesap ekle ve oku`() = runTest {
        val id = db.hesapDao().ekle(
            Hesap(ad = "Garanti vadesiz", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000,
                acilisTarihi = LocalDate.parse("2026-09-24"), maasHesabi = true)
        )
        val h = db.hesapDao().getir(id)!!
        assertEquals("Garanti vadesiz", h.ad)
        assertEquals(HesapTuru.BANKA, h.tur)
        assertEquals(845000L, h.acilisBakiyeKurus)
        assertEquals(LocalDate.parse("2026-09-24"), h.acilisTarihi)
        assertEquals(listOf(h), db.hesapDao().tumu().first())
    }

    @Test
    fun `olmayan hesap null`() = runTest {
        assertNull(db.hesapDao().getir(999))
    }

    @Test
    fun `arsivdeki hesap listede yok`() = runTest {
        db.hesapDao().ekle(Hesap(ad = "Eski", tur = HesapTuru.NAKIT, acilisTarihi = LocalDate.parse("2026-01-01"), arsiv = true))
        assertEquals(emptyList(), db.hesapDao().tumu().first())
    }
}
