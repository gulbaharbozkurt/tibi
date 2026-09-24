package app.tibi.ui.kurulum

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KurulumVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: KurulumVm
    private val bugun = LocalDate.parse("2026-09-24")

    @Before fun ac() {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        vm = KurulumVm(db, KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }) { bugun }
    }
    @After fun kapat() { db.close() }

    @Test
    fun `hitap kaydedilir, bos olamaz`() = runTest {
        assertIs<Sonuc.Hata>(vm.hitapKaydet("   "))
        assertEquals(Sonuc.Tamam, vm.hitapKaydet("  Gülbahar "))
        assertEquals("Gülbahar", db.ayarDao().oku(Anahtarlar.HITAP))
    }

    @Test
    fun `hesap eklenir, bakiye virgullu okunur`() = runTest {
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Garanti vadesiz", HesapTuru.BANKA, "8.450,00", maasHesabi = true))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Nakit", HesapTuru.NAKIT, "", maasHesabi = false))
        val h = vm.hesaplar.first()
        assertEquals(listOf("Garanti vadesiz", "Nakit"), h.map { it.ad })
        assertEquals(listOf(845000L, 0L), h.map { it.bakiyeKurus })
        assertTrue(h.first().maasHesabi)
    }

    @Test
    fun `hatali hesap girisleri`() = runTest {
        assertIs<Sonuc.Hata>(vm.hesapEkle("", HesapTuru.BANKA, "100", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("X", HesapTuru.BANKA, "12,345", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("X", HesapTuru.KREDI_KARTI, "100", false))
        assertEquals(0, vm.hesaplar.first().size)
    }

    @Test
    fun `bitir kurulumu tamamlar`() = runTest {
        vm.bitir()
        assertEquals("1", db.ayarDao().oku(Anahtarlar.KURULUM_TAMAM))
    }
}
