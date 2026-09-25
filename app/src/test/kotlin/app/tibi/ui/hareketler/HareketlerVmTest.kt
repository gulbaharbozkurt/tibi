package app.tibi.ui.hareketler

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.DonemServisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
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

@RunWith(AndroidJUnit4::class)
class HareketlerVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: HareketlerVm
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = bugun))
        val banka = db.hesapDao().ekle(Hesap(ad = "İş Bankası", tur = HesapTuru.BANKA, acilisTarihi = bugun))
        kayit.harcama(Kurus(38650), bugun, nakit, db.kategoriDao().adIle("Market")!!.id, aciklama = "haftalık")
        kayit.harcama(Kurus(12000), t("2026-09-23"), nakit, db.kategoriDao().adIle("Yemek/Kafe")!!.id)
        kayit.gelir(Kurus(105000), t("2026-09-23"), nakit, db.kategoriDao().adIle("Özel ders")!!.id)
        kayit.transfer(Kurus(50000), t("2026-09-02"), banka, nakit)
        vm = HareketlerVm(db, DonemServisi(db)) { bugun }
    }
    @After fun kapat() { db.close() }

    private suspend fun avansDurumu() = vm.gruplar.first().flatMap { it.satirlar }.associate { it.tutarKurus to it.avansAcik }

    @Test
    fun `gunlere gore gruplar yeniden eskiye`() = runTest {
        val g = vm.gruplar.first()
        assertEquals(listOf(bugun, t("2026-09-23"), t("2026-09-02")), g.map { it.tarih })
        assertEquals(2, g[1].satirlar.size)
    }

    @Test
    fun `filtre ve arama`() = runTest {
        vm.filtre.value = HareketFiltresi.GELIR
        assertEquals(listOf(105000L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.filtre.value = HareketFiltresi.TUMU
        vm.arama.value = "HAFTA"
        assertEquals(listOf(38650L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.arama.value = "iş bank"
        assertEquals(listOf(50000L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.arama.value = "386,50"
        assertEquals(1, vm.gruplar.first().size)
    }

    @Test
    fun `silme`() = runTest {
        val id = vm.gruplar.first().first().satirlar.first().id
        vm.sil(id)
        assertEquals(3, vm.gruplar.first().sumOf { it.satirlar.size })
    }

    @Test
    fun `avans satiri acik ya da mahsup edilmis olarak isaretlenir`() = runTest {
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        val banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisTarihi = bugun))
        kayit.avans(Kurus(300000), t("2026-09-10"), banka)
        assertEquals(true, avansDurumu()[300000L])
        assertEquals(null, avansDurumu()[38650L])
        kayit.gelir(Kurus(4_000_000), t("2026-09-20"), banka, db.kategoriDao().adIle("Maaş")!!.id)
        assertEquals(false, avansDurumu()[300000L])
        assertEquals(null, avansDurumu()[4_000_000L])
    }

    @Test
    fun `onceki donemdeki harcama yalnizca oncekiye gecince gorunur`() = runTest {
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        val nakit = db.hesapDao().ekle(Hesap(ad = "Cüzdan", tur = HesapTuru.NAKIT, acilisTarihi = t("2026-08-01")))
        kayit.harcama(Kurus(7777), t("2026-08-20"), nakit, db.kategoriDao().adIle("Market")!!.id)
        assertEquals(false, 7777L in vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        assertEquals(true, vm.buDonemMi.first())

        vm.onceki()
        assertEquals(-1, vm.kaydirma.value)
        assertEquals(false, vm.buDonemMi.first())
        assertEquals(t("2026-08-01"), vm.donem.first().baslangic)
        assertEquals(listOf(7777L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.arama.value = "market"
        assertEquals(listOf(7777L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.arama.value = ""

        vm.sonraki()
        assertEquals(0, vm.kaydirma.value)
        assertEquals(false, 7777L in vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
    }

    @Test
    fun `bu donemde sonraki bir sey yapmaz`() = runTest {
        val once = vm.donem.first()
        vm.sonraki()
        assertEquals(0, vm.kaydirma.value)
        assertEquals(once, vm.donem.first())
        assertEquals(4, vm.gruplar.first().sumOf { it.satirlar.size })
    }

    @Test
    fun `kalem aramada bulunur, baslik olur, kategori alt satira iner`() = runTest {
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        val nakit = db.hesapDao().ekle(Hesap(ad = "Cüzdan", tur = HesapTuru.NAKIT, acilisTarihi = bugun))
        kayit.harcama(Kurus(9000), bugun, nakit, db.kategoriDao().adIle("Market")!!.id, kalem = "Probis")
        vm.arama.value = "PROB"
        val s = vm.gruplar.first().flatMap { it.satirlar }.single()
        assertEquals(9000L, s.tutarKurus)
        assertEquals("Probis", satirBasligi(s))
        assertEquals("Market · Cüzdan", satirAltMetni(s))
        vm.arama.value = "haftal"
        val eski = vm.gruplar.first().flatMap { it.satirlar }.single()
        assertEquals("Market", satirBasligi(eski))
        assertEquals("Nakit · haftalık", satirAltMetni(eski))
    }
}
