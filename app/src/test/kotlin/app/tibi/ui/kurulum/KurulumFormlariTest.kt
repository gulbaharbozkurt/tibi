package app.tibi.ui.kurulum

import app.tibi.veri.tablo.HesapTuru
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KurulumFormlariTest {

    @Test
    fun `bos hesap formu dolu sayilmaz`() {
        assertFalse(HesapFormu().doluMu())
        assertFalse(HesapFormu(ad = "  ", bakiye = " ", banka = "").doluMu())
        // Onay kutusu ve tür seçimi tek başına kaydedilecek girdi değildir.
        assertFalse(HesapFormu(maas = false, tur = HesapTuru.NAKIT).doluMu())
    }

    @Test
    fun `hesap formunda banka, ad ya da bakiye dolu sayilir`() {
        assertTrue(HesapFormu(banka = "Garanti BBVA").doluMu())
        assertTrue(HesapFormu(ad = "Vadesiz").doluMu())
        assertTrue(HesapFormu(bakiye = "8.450,00").doluMu())
        assertTrue(HesapFormu(tur = HesapTuru.NAKIT, bakiye = "500").doluMu())
    }

    @Test
    fun `nakit hesapta gizli banka alani sayilmaz`() {
        assertFalse(HesapFormu(tur = HesapTuru.NAKIT, banka = "Garanti BBVA").doluMu())
    }

    @Test
    fun `hesap eklendikten sonra kalan banka tek basina dolu sayilmaz`() {
        val sonra = HesapFormu(banka = "Garanti BBVA", ad = "Vadesiz", bakiye = "100").eklendiktenSonra()
        assertFalse(sonra.doluMu())
        assertTrue(sonra.copy(banka = "Akbank").doluMu())
        assertTrue(sonra.copy(ad = "Birikim").doluMu())
    }

    @Test
    fun `kart formu kapaliysa ya da ad ve son4 bossa dolu sayilmaz`() {
        assertFalse((null as KartGirdisi?).doluMu())
        assertFalse(KartGirdisi().doluMu())
        // Varsayılan asgari oran ya da yalnızca takvim alanları kaydı tetiklemez.
        assertFalse(KartGirdisi(kesimGunu = "12", bankaAdi = "Garanti BBVA").doluMu())
        assertTrue(KartGirdisi(ad = "Bonus").doluMu())
        assertTrue(KartGirdisi(son4 = "4821").doluMu())
    }

    @Test
    fun `taksit formunda tutar ya da sayi dolu sayilir`() {
        assertFalse(TaksitGirdisi().doluMu())
        assertFalse(TaksitGirdisi(kartId = 3L, aciklama = "Telefon", siradaki = "2").doluMu())
        assertTrue(TaksitGirdisi(tutar = "1.850").doluMu())
        assertTrue(TaksitGirdisi(sayi = "6").doluMu())
    }
}
