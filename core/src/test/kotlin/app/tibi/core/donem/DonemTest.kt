package app.tibi.core.donem

import app.tibi.core.tarih.HaftaSonuKurali
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DonemTest {
    private fun t(s: String) = LocalDate.parse(s)
    private val maas30 = MaasKurali(30, HaftaSonuKurali.ONCEKI)

    @Test
    fun `ay ortasinda donem onceki maastan baslar`() {
        // 30 Ağustos 2026 pazar → maaş 28 Ağustos cuma
        val d = DonemHesaplayici.donem(maas30, t("2026-09-24"))
        assertEquals(Donem(t("2026-08-28"), t("2026-09-29")), d)
        assertEquals(33, d.gunSayisi)
    }

    @Test
    fun `maas gunu yeni donemin ilk gunudur`() {
        val d = DonemHesaplayici.donem(maas30, t("2026-09-30"))
        assertEquals(Donem(t("2026-09-30"), t("2026-10-29")), d)
    }

    @Test
    fun `maastan bir gun once eski donemin son gunudur`() {
        val d = DonemHesaplayici.donem(maas30, t("2026-09-29"))
        assertEquals(t("2026-08-28"), d.baslangic)
        assertEquals(t("2026-09-29"), d.bitis)
    }

    @Test
    fun `sonraki is gunune kayan maas ay sinirini asabilir`() {
        // 31 Ekim 2026 cumartesi → 2 Kasım pazartesi; 1 Kasım hâlâ eylül sonunda başlayan dönemde
        val k = MaasKurali(31, HaftaSonuKurali.SONRAKI)
        val d = DonemHesaplayici.donem(k, t("2026-11-01"))
        assertEquals(Donem(t("2026-09-30"), t("2026-11-01")), d)
    }

    @Test
    fun `ayni gun kurali kaydirmaz`() {
        val d = DonemHesaplayici.donem(MaasKurali(15, HaftaSonuKurali.AYNI), t("2026-09-15"))
        assertEquals(Donem(t("2026-09-15"), t("2026-10-14")), d)
    }

    @Test
    fun `icerir ve ilerleme`() {
        val d = Donem(t("2026-08-28"), t("2026-09-29"))
        assertTrue(d.icerir(t("2026-08-28")))
        assertTrue(d.icerir(t("2026-09-29")))
        assertFalse(d.icerir(t("2026-09-30")))
        assertEquals(848, d.ilerlemeBinde(t("2026-09-24")))  // 28/33 gün
        assertEquals(1000, d.ilerlemeBinde(t("2026-09-29")))
        assertEquals(30, d.ilerlemeBinde(t("2026-08-28")))   // 1/33
    }

    private val maas10 = MaasKurali(10, HaftaSonuKurali.ONCEKI)
    private val maasla = { t: LocalDate -> DonemHesaplayici.donem(maas10, t) }
    private val takvimAyi = { t: LocalDate -> Donem(t.withDayOfMonth(1), t.withDayOfMonth(t.lengthOfMonth())) }

    @Test
    fun `maas kuraliyla onceki donemlere kaydirma`() {
        val bu = maasla(t("2026-09-25"))
        // 10 Eylül 2026 perşembe; 10 Ekim cumartesi → maaş 9 Ekim cuma, dönem 8 Ekim'de biter
        assertEquals(Donem(t("2026-09-10"), t("2026-10-08")), bu)
        assertEquals(bu, kaydir(bu, 0, maasla))
        // 10 Ağustos pazartesi: kayma yok, dönem Eylül maaşından bir gün önce biter
        assertEquals(Donem(t("2026-08-10"), t("2026-09-09")), kaydir(bu, -1, maasla))
        // 10 Temmuz cuma: kayma yok
        assertEquals(Donem(t("2026-07-10"), t("2026-08-09")), kaydir(bu, -2, maasla))
        // 10 Mayıs pazar → 8 Mayıs cuma; Nisan dönemi 7 Mayıs'ta biter, Mayıs dönemi 8'inde başlar
        assertEquals(Donem(t("2026-05-08"), t("2026-06-09")), kaydir(bu, -4, maasla))
        assertEquals(Donem(t("2026-04-10"), t("2026-05-07")), kaydir(bu, -5, maasla))
    }

    @Test
    fun `geri sonra ileri kaydirma bu doneme doner`() {
        val bu = maasla(t("2026-09-25"))
        assertEquals(bu, kaydir(kaydir(bu, -3, maasla), 3, maasla))
        assertEquals(Donem(t("2026-10-09"), t("2026-11-09")), kaydir(bu, 1, maasla))
        val ay = takvimAyi(t("2026-09-25"))
        assertEquals(ay, kaydir(kaydir(ay, -7, takvimAyi), 7, takvimAyi))
    }

    @Test
    fun `kural yokken takvim aylari`() {
        val bu = takvimAyi(t("2026-09-25"))
        assertEquals(Donem(t("2026-08-01"), t("2026-08-31")), kaydir(bu, -1, takvimAyi))
        assertEquals(Donem(t("2026-02-01"), t("2026-02-28")), kaydir(bu, -7, takvimAyi))
        assertEquals(Donem(t("2025-12-01"), t("2025-12-31")), kaydir(bu, -9, takvimAyi))
    }
}
