package app.tibi.ui.giris

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.ui.Sonuc
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
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

@RunWith(AndroidJUnit4::class)
class HizliGirisVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: HizliGirisVm
    private val bugun = LocalDate.parse("2026-09-24")
    private var nakit = 0L
    private var banka = 0L
    private var bonus = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        vm = HizliGirisVm(db, KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }) { bugun }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisTarihi = bugun))
        nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisBakiyeKurus = 130000, acilisTarihi = bugun))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22))
    }
    @After fun kapat() { db.close() }

    private suspend fun kat(ad: String) = db.kategoriDao().adIle(ad)!!.id

    @Test
    fun `hesap secenekleri once hesaplar sonra kartlar`() = runTest {
        assertEquals(listOf("Garanti" to false, "Nakit" to false, "Bonus" to true), vm.hesaplar.first().map { it.ad to it.kart })
    }

    @Test
    fun `taksitli kart harcamasi`() = runTest {
        val s = vm.kaydet(GirisTuru.HARCAMA, "1.249,90", bonus, null, kat("Giyim"), taksit = 3, erteleme = 0, tarih = bugun, aciklama = " mont ")
        assertEquals(Sonuc.Tamam, s)
        val h = db.hareketDao().satirlar(bugun, bugun).first().single()
        assertEquals(124990L, h.tutarKurus)
        assertEquals("mont", h.aciklama)
        assertEquals(3, db.taksitDao().sayi())
    }

    @Test
    fun `nakit harcamada taksit yok sayilir`() = runTest {
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.HARCAMA, "386,50", nakit, null, kat("Market"), taksit = 3, erteleme = 2, tarih = bugun, aciklama = ""))
        assertEquals(0, db.taksitDao().sayi())
        assertEquals(130000L - 38650L, db.hesapDao().bakiye(nakit).first())
    }

    @Test
    fun `gelir ve transfer`() = runTest {
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.GELIR, "1.050", nakit, null, kat("Özel ders"), 1, 0, bugun, ""))
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.TRANSFER, "500", nakit, banka, null, 1, 0, bugun, ""))
        val turler = db.hareketDao().satirlar(bugun, bugun).first().map { it.tur }.toSet()
        assertEquals(setOf(HareketTuru.GELIR, HareketTuru.TRANSFER), turler)
        assertEquals(130000L + 105000L - 50000L, db.hesapDao().bakiye(nakit).first())
    }

    @Test
    fun `hatali girisler anlasilir mesaj verir`() = runTest {
        assertEquals(Sonuc.Hata("Tutarı 1.249,90 biçiminde yaz."), vm.kaydet(GirisTuru.HARCAMA, "", nakit, null, null, 1, 0, bugun, ""))
        assertEquals(Sonuc.Hata("Hesap ya da kart seç."), vm.kaydet(GirisTuru.HARCAMA, "10", null, null, null, 1, 0, bugun, ""))
        assertEquals(Sonuc.Hata("Paranın gideceği hesabı seç."), vm.kaydet(GirisTuru.TRANSFER, "10", nakit, null, null, 1, 0, bugun, ""))
        assertIs<Sonuc.Hata>(vm.kaydet(GirisTuru.TRANSFER, "10", nakit, nakit, null, 1, 0, bugun, ""))
        assertIs<Sonuc.Hata>(vm.kaydet(GirisTuru.GELIR, "10", bonus, null, null, 1, 0, bugun, ""))
        assertEquals(0, db.hareketDao().sayi())
    }

    @Test
    fun `bankali hesap ve kart secenegi banka adiyla gorunur`() = runTest {
        val garanti = KayitServisi(db).bankaBulVeyaEkle("Garanti BBVA")
        db.hesapDao().ekle(Hesap(ad = "Vadesiz", tur = HesapTuru.BANKA, acilisTarihi = bugun, bankaId = garanti))
        val max = db.hesapDao().ekle(Hesap(ad = "Maximum", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun, bankaId = garanti))
        db.kartDao().ekle(Kart(hesapId = max, son4 = "1190", kesimGunu = 5, sonOdemeGunu = 15))
        assertEquals(
            listOf("Garanti" to false, "Nakit" to false, "Garanti BBVA · Vadesiz" to false, "Bonus" to true, "Garanti BBVA · Maximum" to true),
            vm.hesaplar.first().map { it.ad to it.kart },
        )
    }

    @Test
    fun `avans hesaba yazilir, karta yazilmaz`() = runTest {
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.AVANS, "5.000", banka, null, null, 1, 0, bugun, " eylül "))
        val h = db.hareketDao().satirlar(bugun, bugun).first().single()
        assertEquals(HareketTuru.AVANS, h.tur)
        assertEquals("eylül", h.aciklama)
        assertEquals(500000L, db.avansDao().acikToplam().first())
        assertEquals(Sonuc.Hata("Avans bir banka hesabına ya da elde nakde yatar."),
            vm.kaydet(GirisTuru.AVANS, "100", bonus, null, null, 1, 0, bugun, ""))
        assertEquals(1, db.hareketDao().sayi())
    }
}
