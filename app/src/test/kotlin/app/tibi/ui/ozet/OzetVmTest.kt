package app.tibi.ui.ozet

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.Anahtarlar
import app.tibi.veri.DonemServisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
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

@RunWith(AndroidJUnit4::class)
class OzetVmTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    @Test
    fun `ozet donem toplamlarini kartlari ve taksit yukunu toplar`() = runTest {
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        db.ayarDao().yaz(Ayar(Anahtarlar.HITAP, "Gülbahar"))
        val banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun, maasHesabi = true))
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = banka, baslangic = bugun))
        val bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, kendiLimitiKurus = 1_500_000))
        val sanal = db.hesapDao().ekle(Hesap(ad = "Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        kayit.gelir(Kurus(4_500_000), t("2026-08-28"), banka, null)
        kayit.harcama(Kurus(124990), bugun, bonus, null, taksitSayisi = 3)
        kayit.harcama(Kurus(10000), bugun, banka, null)

        val d = OzetVm(db, DonemServisi(db)) { bugun }.durum.first()
        assertEquals("Gülbahar", d.hitap)
        assertEquals(t("2026-08-28"), d.donem.baslangic)
        assertEquals(4_500_000L, d.gelirKurus)
        assertEquals(134990L, d.giderKurus)
        assertEquals(4_500_000L - 134990L, d.kalanKurus)
        assertEquals(listOf(KartOzeti("Bonus", 124990L, 1_500_000L)), d.kartlar)
        assertEquals(3, d.taksitYuku.size)
        assertEquals(41664L, d.taksitYuku.first().toplamKurus)
        assertEquals(listOf(845000L + 4_500_000L - 10000L), d.hesaplar.map { it.bakiyeKurus })
    }
}
