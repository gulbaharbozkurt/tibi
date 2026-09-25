package app.tibi.ui.hesaplar

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.ui.Sonuc
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HesapDuzenleVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var kayit: KayitServisi
    private val bugun = LocalDate.parse("2026-09-24")
    private var garanti = 0L
    private var vadesiz = 0L
    private var birikim = 0L
    private var nakit = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        garanti = db.bankaDao().ekle(Banka(ad = "Garanti BBVA"))
        vadesiz = db.hesapDao().ekle(Hesap(ad = "Vadesiz", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845_000, acilisTarihi = bugun,
            maasHesabi = true, bankaId = garanti))
        birikim = db.hesapDao().ekle(Hesap(ad = "Birikim", tur = HesapTuru.BANKA, acilisBakiyeKurus = 100_000, acilisTarihi = bugun, bankaId = garanti))
        nakit = db.hesapDao().ekle(Hesap(ad = "Elde nakit", tur = HesapTuru.NAKIT, acilisBakiyeKurus = 50_000, acilisTarihi = bugun))
    }
    @After fun kapat() { db.close() }

    private fun vm(id: Long) = HesapDuzenleVm(db, kayit, id) { bugun }

    @Test
    fun `durum hesabi ve bakiyeyi verir`() = runTest {
        val d = vm(vadesiz).durum.filterNotNull().first()
        assertEquals("Vadesiz", d.ad)
        assertEquals(HesapTuru.BANKA, d.tur)
        assertEquals("Garanti BBVA", d.bankaAdi)
        assertTrue(d.maasHesabi)
        assertEquals(845_000L, d.bakiyeKurus)
    }

    @Test
    fun `ad ve banka degisir, banka bulunur ya da eklenir`() = runTest {
        assertEquals(Sonuc.Tamam, vm(birikim).kaydet("Altın hesabı", "İş Bankası", false))
        var h = db.hesapDao().getir(birikim)!!
        assertEquals("Altın hesabı", h.ad)
        val is_ = db.bankaDao().adIle("İş Bankası")!!.id
        assertEquals(is_, h.bankaId)
        assertEquals(Sonuc.Tamam, vm(birikim).kaydet("Altın hesabı", "garanti bbva", false))
        h = db.hesapDao().getir(birikim)!!
        assertEquals(garanti, h.bankaId)
        assertEquals(2, db.bankaDao().hepsi().size)
    }

    @Test
    fun `bos ad varsayilan olur, banka hesabi bankasiz kalamaz`() = runTest {
        assertEquals(Sonuc.Tamam, vm(birikim).kaydet("  ", "Garanti BBVA", false))
        assertEquals("Vadesiz hesap", db.hesapDao().getir(birikim)!!.ad)
        assertEquals(Sonuc.Tamam, vm(nakit).kaydet("", "Akbank", true))
        val n = db.hesapDao().getir(nakit)!!
        assertEquals("Elde nakit", n.ad)
        assertNull(n.bankaId)
        assertFalse(n.maasHesabi)
        assertEquals(Sonuc.Hata("Hesabın bağlı olduğu bankayı yaz."), vm(birikim).kaydet("Birikim", " ", false))
        assertEquals(garanti, db.hesapDao().getir(birikim)!!.bankaId)
        assertNull(db.bankaDao().adIle("Akbank"))
    }

    @Test
    fun `yalnizca bir maas hesabi olur`() = runTest {
        assertEquals(Sonuc.Tamam, vm(birikim).kaydet("Birikim", "Garanti BBVA", true))
        val maaslar = db.hesapDao().bakiyeler().first().filter { it.maasHesabi }.map { it.id }
        assertEquals(listOf(birikim), maaslar)
    }

    @Test
    fun `bakiye duzeltme yukari ve asagi`() = runTest {
        val v = vm(vadesiz)
        assertEquals(Sonuc.Tamam, v.bakiyeDuzelt("9.000,50"))
        assertEquals(900_050L, db.hesapDao().bakiye(vadesiz).first())
        assertEquals(Sonuc.Tamam, v.bakiyeDuzelt("0"))
        assertEquals(0L, db.hesapDao().bakiye(vadesiz).first())
        val satirlar = db.hareketDao().aralik(bugun, bugun).first()
        assertEquals(2, satirlar.size)
        assertTrue(satirlar.all { it.tur == HareketTuru.DUZELTME && it.aciklama == "Bakiye düzeltme" })
        val artis = satirlar.single { it.hedefHesapId == vadesiz }
        assertEquals(55_050L, artis.tutarKurus)
        val azalis = satirlar.single { it.kaynakHesapId == vadesiz }
        assertEquals(900_050L, azalis.tutarKurus)
        val toplam = db.hareketDao().donemToplami(bugun, bugun).first()
        assertEquals(0L, toplam.gelirKurus)
        assertEquals(0L, toplam.giderKurus)
    }

    @Test
    fun `fark yoksa bir sey yazilmaz, hatali metin hata`() = runTest {
        assertEquals(Sonuc.Tamam, vm(vadesiz).bakiyeDuzelt("8.450,00"))
        assertEquals(0, db.hareketDao().sayi())
        assertIs<Sonuc.Hata>(vm(vadesiz).bakiyeDuzelt("12,345"))
        assertIs<Sonuc.Hata>(vm(vadesiz).bakiyeDuzelt(""))
        assertEquals(0, db.hareketDao().sayi())
    }

    @Test
    fun `kapatilan hesap listeden cikar`() = runTest {
        assertEquals(Sonuc.Tamam, vm(birikim).kapat())
        assertEquals(listOf(vadesiz, nakit), db.hesapDao().bakiyeler().first().map { it.id })
        assertTrue(db.hesapDao().getir(birikim)!!.arsiv)
        assertNull(vm(birikim).durum.first())
    }

    @Test
    fun `maas kuralinin hesabi kapatilamaz`() = runTest {
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = vadesiz, baslangic = bugun))
        assertEquals(Sonuc.Hata("Maaşın bu hesaba yatıyor; önce maaş hesabını değiştir."), vm(vadesiz).kapat())
        assertFalse(db.hesapDao().getir(vadesiz)!!.arsiv)
        // Maaş hesabı değişince kural da yeni hesaba geçer; eski hesap kapatılabilir.
        assertEquals(Sonuc.Tamam, vm(birikim).kaydet("Birikim", "Garanti BBVA", true))
        assertEquals(birikim, db.duzenliKuralDao().maasKurali()!!.hesapId)
        assertEquals(Sonuc.Tamam, vm(vadesiz).kapat())
    }

    @Test
    fun `duzeltme kredi kartina yazilmaz`() = runTest {
        val kart = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        val hata = runCatching { kayit.duzeltme(Kurus(100), bugun, kart, artis = true) }.exceptionOrNull()
        assertIs<app.tibi.veri.KayitHatasi>(hata)
        assertEquals(0, db.hareketDao().sayi())
    }
}
