package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.donem.Donem
import app.tibi.core.para.Kurus
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class EkranSorgulariTest {
    private lateinit var db: TibiVeritabani
    private lateinit var kayit: KayitServisi
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var banka = 0L
    private var bonus = 0L
    private var sanal = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun, maasHesabi = true))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, bankaLimitiKurus = 5_000_000, kendiLimitiKurus = 1_500_000))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
    }
    @After fun kapat() { db.close() }

    private suspend fun kat(ad: String) = db.kategoriDao().adIle(ad)!!.id

    @Test
    fun `donem toplami gelir ve gideri ayirir`() = runTest {
        kayit.gelir(Kurus(4_500_000), t("2026-08-28"), banka, kat("Maaş"))
        kayit.harcama(Kurus(38650), t("2026-09-10"), banka, kat("Market"))
        kayit.harcama(Kurus(124990), bugun, bonus, kat("Giyim"), taksitSayisi = 3)
        kayit.transfer(Kurus(50000), bugun, banka, banka.let { db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = bugun)) })
        kayit.kartOdemesi(Kurus(10000), bugun, banka, bonus)
        kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun)
        kayit.harcama(Kurus(999), t("2026-09-30"), banka, kat("Market"))   // dönem dışında
        val d = db.hareketDao().donemToplami(t("2026-08-28"), t("2026-09-29")).first()
        assertEquals(4_500_000L, d.gelirKurus)
        assertEquals(38650L + 124990L, d.giderKurus)
    }

    @Test
    fun `hareket satirlari adlarla gelir ve silinir`() = runTest {
        val id = kayit.harcama(Kurus(124990), bugun, bonus, kat("Giyim"), taksitSayisi = 3, aciklama = "mont")
        val s = db.hareketDao().satirlar(t("2026-09-01"), t("2026-09-30")).first().single()
        assertEquals("Giyim", s.kategoriAdi)
        assertEquals("Bonus", s.kaynakAdi)
        assertNull(s.hedefAdi)
        assertEquals(3, s.taksitSayisi)
        assertEquals(HareketTuru.HARCAMA, s.tur)
        db.hareketDao().sil(id)
        assertEquals(0, db.taksitDao().sayi())
        assertEquals(0L, db.taksitDao().limitKullanimi(bonus).first())
    }

    @Test
    fun `kategoriler kullanima gore siralanir`() = runTest {
        kayit.harcama(Kurus(100), bugun, banka, kat("Ev"))
        kayit.harcama(Kurus(100), bugun, banka, kat("Ev"))
        kayit.harcama(Kurus(100), bugun, banka, kat("Sağlık"))
        val adlar = db.kategoriDao().kullanimSirali(KategoriYonu.GIDER).first().map { it.ad }
        assertEquals(listOf("Ev", "Sağlık", "Market"), adlar.take(3))
        assertEquals(14, adlar.size)
    }

    @Test
    fun `hesap bakiyeleri kartlari icermez`() = runTest {
        kayit.harcama(Kurus(5000), bugun, banka, null)
        val b = db.hesapDao().bakiyeler().first()
        assertEquals(listOf("Garanti"), b.map { it.ad })
        assertEquals(840000L, b.single().bakiyeKurus)
    }

    @Test
    fun `kart bilgisi limit kullanimini ana kartta toplar`() = runTest {
        kayit.harcama(Kurus(7999), bugun, sanal, null)
        kayit.harcama(Kurus(100000), bugun, bonus, null)
        val k = db.kartDao().kartlar().first()
        val ana = k.first { it.hesapId == bonus }
        assertEquals(107999L, ana.kullanimKurus)
        assertEquals(db.taksitDao().limitKullanimi(bonus).first(), ana.kullanimKurus)
        assertEquals(0L, k.first { it.hesapId == sanal }.kullanimKurus)
        assertEquals(bonus, k.first { it.hesapId == sanal }.anaKartId)
    }

    @Test
    fun `aktif taksitler ve kesim tutari`() = runTest {
        kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
        kayit.harcama(Kurus(38650), bugun, bonus, null)                       // tek çekim, listede yok
        val a = db.taksitDao().aktifTaksitler(bonus, bugun).first().single()
        assertEquals("Telefon", a.aciklama)
        assertEquals(12, a.toplam)
        assertEquals(6, a.siradakiSira)
        assertEquals(185000L, a.aylikKurus)
        assertEquals(7 * 185000L, a.kalanKurus)
        assertEquals(t("2027-04-12"), a.sonKesim)
        assertEquals(185000L + 38650L, db.taksitDao().kesimTutari(bonus, t("2026-10-12")).first())
    }

    @Test
    fun `donem maas kuralindan yoksa takvim ayindan`() = runTest {
        val servis = DonemServisi(db)
        assertEquals(Donem(t("2026-09-01"), t("2026-09-30")), servis.donem(bugun).first())
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = banka, baslangic = bugun))
        assertEquals(Donem(t("2026-08-28"), t("2026-09-29")), servis.donem(bugun).first())
    }

    @Test
    fun `ayar akisi`() = runTest {
        assertNull(db.ayarDao().okuAkis(Anahtarlar.KURULUM_TAMAM).first())
        db.ayarDao().yaz(app.tibi.veri.tablo.Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        assertEquals("1", db.ayarDao().okuAkis(Anahtarlar.KURULUM_TAMAM).first())
    }
}
