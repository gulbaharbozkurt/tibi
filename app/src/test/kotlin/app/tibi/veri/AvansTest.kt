package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
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
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class AvansTest {
    private lateinit var db: TibiVeritabani
    private lateinit var servis: KayitServisi
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var banka = 0L
    private var bonus = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        servis = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22))
    }
    @After fun kapat() { db.close() }

    private suspend fun maasKurali() {
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = banka, baslangic = t("2026-01-01")))
    }
    private suspend fun kat(ad: String) = db.kategoriDao().adIle(ad)!!.id
    private suspend fun acik() = db.avansDao().acikToplam().first()

    @Test
    fun `avans bakiyeyi artirir ama donem geliri sayilmaz`() = runTest {
        val id = servis.avans(Kurus(500000), bugun, banka, "eylül avansı")
        assertEquals(845000L + 500000L, db.hesapDao().bakiye(banka).first())
        assertEquals(0L, db.hareketDao().donemToplami(t("2026-09-01"), t("2026-09-30")).first().gelirKurus)
        val h = db.hareketDao().getir(id)!!
        assertEquals(HareketTuru.AVANS, h.tur)
        assertEquals(banka, h.hedefHesapId)
        assertEquals("eylül avansı", h.aciklama)
        assertEquals(500000L, acik())
        assertEquals(listOf(id), db.avansDao().acikAvanslar().map { it.hareketId })
    }

    @Test
    fun `dusulecek maas tarihi maas kuralindan gelir`() = runTest {
        maasKurali()
        val a = servis.avans(Kurus(1000), t("2026-09-10"), banka)
        val b = servis.avans(Kurus(1000), t("2026-09-30"), banka)
        val tarihler = db.avansDao().acikAvanslar().associate { it.hareketId to it.dusulecekMaasTarihi }
        assertEquals(t("2026-09-30"), tarihler[a])
        assertEquals(t("2026-10-30"), tarihler[b])
    }

    @Test
    fun `maas kurali yoksa dusulecek tarih bos`() = runTest {
        servis.avans(Kurus(1000), bugun, banka)
        assertNull(db.avansDao().acikAvanslar().single().dusulecekMaasTarihi)
    }

    @Test
    fun `karta ya da sifir tutarla avans yazilmaz`() = runTest {
        val e = assertFailsWith<KayitHatasi> { servis.avans(Kurus(1000), bugun, bonus) }
        assertEquals("Avans bir banka hesabına ya da elde nakde yatar.", e.message)
        assertFailsWith<KayitHatasi> { servis.avans(Kurus(0), bugun, banka) }
        assertFailsWith<KayitHatasi> { servis.avans(Kurus(1000), bugun, 999) }
        assertEquals(0, db.hareketDao().sayi())
        assertEquals(0, db.avansDao().acikAvanslar().size)
        assertEquals(0L, acik())
    }

    @Test
    fun `maas geliri acik avansi mahsup eder, silinince geri acilir`() = runTest {
        servis.avans(Kurus(300000), t("2026-09-10"), banka)
        servis.avans(Kurus(200000), t("2026-09-15"), banka)
        assertEquals(500000L, acik())
        val maas = servis.gelir(Kurus(4_000_000), t("2026-09-30"), banka, kat("Maaş"))
        assertEquals(0L, acik())
        assertEquals(0, db.avansDao().acikAvanslar().size)
        db.hareketDao().sil(maas)
        assertEquals(500000L, acik())
        assertEquals(2, db.avansDao().acikAvanslar().size)
    }

    @Test
    fun `maas olmayan gelir avansi kapatmaz`() = runTest {
        servis.avans(Kurus(300000), t("2026-09-10"), banka)
        servis.gelir(Kurus(105000), t("2026-09-30"), banka, kat("Özel ders"))
        servis.gelir(Kurus(105000), t("2026-09-30"), banka, null)
        assertEquals(300000L, acik())
    }

    @Test
    fun `maastan sonra alinan avans acik kalir`() = runTest {
        servis.avans(Kurus(300000), t("2026-09-10"), banka)
        servis.avans(Kurus(100000), t("2026-10-02"), banka)
        servis.gelir(Kurus(4_000_000), t("2026-09-30"), banka, kat("Maaş"))
        assertEquals(100000L, acik())
    }
}
