package app.tibi.core.kart

import app.tibi.core.para.Kurus
import app.tibi.core.para.topla
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaksitPlanlayiciTest {
    private fun t(s: String) = LocalDate.parse(s)
    private val bonus = KartTakvimi(12, 22)
    private val bugun = t("2026-09-24")

    @Test
    fun `uc taksit sonraki kesimden baslar, artan kurus ilk taksitte`() {
        val p = TaksitPlanlayici.yeniHarcama(Kurus(124990), 3, 0, bugun, bonus)
        assertEquals(listOf(Kurus(41664), Kurus(41663), Kurus(41663)), p.map { it.tutar })
        assertEquals(listOf(t("2026-10-12"), t("2026-11-12"), t("2026-12-12")), p.map { it.ekstreKesimTarihi })
        assertEquals(listOf(1, 2, 3), p.map { it.sira })
        assertTrue(p.all { it.toplam == 3 && !it.oncedenOdendi })
    }

    @Test
    fun `tek cekim tek satirdir`() {
        val p = TaksitPlanlayici.yeniHarcama(Kurus(38650), 1, 0, bugun, bonus)
        assertEquals(listOf(PlanliTaksit(1, 1, Kurus(38650), t("2026-10-12"))), p)
    }

    @Test
    fun `erteleme ilk taksiti ileri atar`() {
        val p = TaksitPlanlayici.yeniHarcama(Kurus(420000), 6, 3, bugun, bonus)
        assertEquals(t("2027-01-12"), p.first().ekstreKesimTarihi)
        assertEquals(t("2027-06-12"), p.last().ekstreKesimTarihi)
    }

    @Test
    fun `kesim 31 kisa aylarda kaymaz`() {
        val k = KartTakvimi(31, 10)
        val p = TaksitPlanlayici.yeniHarcama(Kurus(30000), 3, 0, t("2027-01-31"), k)
        assertEquals(listOf(t("2027-01-31"), t("2027-02-28"), t("2027-03-31")), p.map { it.ekstreKesimTarihi })
    }

    @Test
    fun `gecmis taksit aylik tutarla, siradaki 6 dan 12`() {
        val p = TaksitPlanlayici.gecmisAylik(Kurus(185000), 12, 6, bugun, bonus)
        assertEquals(12, p.size)
        assertEquals(t("2026-05-12"), p[0].ekstreKesimTarihi)
        assertTrue(p.take(5).all { it.oncedenOdendi })
        assertTrue(p.drop(5).none { it.oncedenOdendi })
        assertEquals(t("2026-10-12"), p[5].ekstreKesimTarihi)
        assertEquals(t("2027-04-12"), p[11].ekstreKesimTarihi)
        assertEquals(Kurus(1295000), p.filterNot { it.oncedenOdendi }.map { it.tutar }.topla())
    }

    @Test
    fun `gecmis taksit toplam tutarla`() {
        val p = TaksitPlanlayici.gecmisToplam(Kurus(2220000), 12, 6, bugun, bonus)
        assertEquals(Kurus(2220000), p.map { it.tutar }.topla())
        assertEquals(Kurus(185000), p[5].tutar)
    }

    @Test
    fun `kalan borc ile giris`() {
        val p = TaksitPlanlayici.kalanBorc(Kurus(1295000), 7, bugun, bonus)
        assertEquals(7, p.size)
        assertTrue(p.all { it.tutar == Kurus(185000) && it.toplam == 7 })
        assertEquals(t("2026-10-12"), p.first().ekstreKesimTarihi)
        assertEquals(t("2027-04-12"), p.last().ekstreKesimTarihi)
    }

    @Test
    fun `gecersiz girisler`() {
        assertFailsWith<IllegalArgumentException> { TaksitPlanlayici.yeniHarcama(Kurus(100), 0, 0, bugun, bonus) }
        assertFailsWith<IllegalArgumentException> { TaksitPlanlayici.yeniHarcama(Kurus(100), 1, -1, bugun, bonus) }
        assertFailsWith<IllegalArgumentException> { TaksitPlanlayici.gecmisAylik(Kurus(100), 12, 13, bugun, bonus) }
        assertFailsWith<IllegalArgumentException> { TaksitPlanlayici.kalanBorc(Kurus(100), 0, bugun, bonus) }
    }

    @Test
    fun `aylik yuk odenmemisleri kesime gore toplar`() {
        val a = TaksitPlanlayici.gecmisAylik(Kurus(185000), 12, 6, bugun, bonus)
        val b = TaksitPlanlayici.yeniHarcama(Kurus(124990), 3, 0, bugun, bonus)
        val yuk = (a + b).aylikYuk()
        assertEquals(Kurus(185000 + 41664), yuk[t("2026-10-12")])
        assertEquals(Kurus(185000), yuk[t("2027-04-12")])
        assertEquals(null, yuk[t("2026-05-12")])  // önceden ödenmiş
    }
}
