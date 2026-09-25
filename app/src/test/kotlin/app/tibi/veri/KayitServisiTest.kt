package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
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
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class KayitServisiTest {
    private lateinit var db: TibiVeritabani
    private lateinit var servis: KayitServisi
    private val bugun = LocalDate.parse("2026-09-24")
    private var banka = 0L
    private var nakit = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        servis = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun))
        nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisBakiyeKurus = 130000, acilisTarihi = bugun))
    }
    @After fun kapat() { db.close() }

    private suspend fun bakiye(id: Long) = db.hesapDao().bakiye(id).first()
    private suspend fun market() = db.kategoriDao().adIle("Market")!!.id

    @Test
    fun `nakit harcama bakiyeden duser`() = runTest {
        val id = servis.harcama(Kurus(38650), bugun, nakit, market(), aciklama = "haftalık")
        assertEquals(130000L - 38650L, bakiye(nakit))
        val h = db.hareketDao().getir(id)!!
        assertEquals(HareketTuru.HARCAMA, h.tur)
        assertEquals(nakit, h.kaynakHesapId)
        assertEquals(null, h.hedefHesapId)
        assertEquals(0, db.taksitDao().sayi())
    }

    @Test
    fun `harcama kalem adini temizleyip anahtariyla yazar`() = runTest {
        val id = servis.harcama(Kurus(9000), bugun, nakit, market(), kalem = "  Ülker   PROBİS ")
        val h = db.hareketDao().getir(id)!!
        assertEquals("Ülker PROBİS", h.kalem)
        assertEquals("ülker probis", h.kalemAnahtar)
        val bos = db.hareketDao().getir(servis.harcama(Kurus(100), bugun, nakit, null, kalem = "  "))!!
        assertEquals(null, bos.kalem)
        assertEquals(null, bos.kalemAnahtar)
    }

    @Test
    fun `gelir bakiyeye eklenir`() = runTest {
        servis.gelir(Kurus(105000), bugun, nakit, db.kategoriDao().adIle("Özel ders")!!.id)
        assertEquals(130000L + 105000L, bakiye(nakit))
    }

    @Test
    fun `transfer iki hesabi birlikte degistirir`() = runTest {
        servis.transfer(Kurus(50000), bugun, banka, nakit)
        assertEquals(845000L - 50000L, bakiye(banka))
        assertEquals(130000L + 50000L, bakiye(nakit))
    }

    @Test
    fun `gecersiz kayitlar hicbir sey yazmaz`() = runTest {
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(0), bugun, nakit, market()) }
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(-5), bugun, nakit, market()) }
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(100), bugun, 999, market()) }
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(100), bugun, nakit, market(), taksitSayisi = 3) }
        assertFailsWith<KayitHatasi> { servis.transfer(Kurus(100), bugun, nakit, nakit) }
        assertEquals(0, db.hareketDao().sayi())
        assertEquals(130000L, bakiye(nakit))
    }

    @Test
    fun `banka bul veya ekle turkce harfleri esler`() = runTest {
        val garanti = servis.bankaBulVeyaEkle("  Garanti BBVA ")
        assertEquals(garanti, servis.bankaBulVeyaEkle("garanti bbva"))
        val is1 = servis.bankaBulVeyaEkle("İŞ BANKASI")
        assertEquals(is1, servis.bankaBulVeyaEkle("iş bankası"))
        assertEquals(listOf("Garanti BBVA", "İŞ BANKASI"), db.bankaDao().tumu().first().map { it.ad })
        assertEquals("Banka adını yaz.", assertFailsWith<KayitHatasi> { servis.bankaBulVeyaEkle("  ") }.message)
    }
}
