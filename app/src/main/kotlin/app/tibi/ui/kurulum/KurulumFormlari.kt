package app.tibi.ui.kurulum

import app.tibi.ui.Sonuc
import app.tibi.veri.tablo.HesapTuru

/**
 * Kurulum adımlarının form durumları KurulumAkisi'nde tutulur; böylece alttaki "Devam" butonu
 * doldurulmuş ama kaydedilmemiş formu adımın kendi butonuyla aynı VM çağrısıyla kaydedebilir.
 */
data class HesapFormu(
    val banka: String = "",
    val ad: String = "",
    val tur: HesapTuru = HesapTuru.BANKA,
    val bakiye: String = "",
    val maas: Boolean = true,
    /** "Hesabı ekle" sonrası banka alanı bir sonraki hesap için dolu kalır; o değer tek başına "kaydedilmemiş" sayılmaz. */
    val kalanBanka: String? = null,
) {
    /** Kaydedilmemiş anlamlı girdi var mı? Nakit hesapta gizli banka alanı sayılmaz. */
    fun doluMu(): Boolean =
        ad.isNotBlank() || bakiye.isNotBlank() ||
            (tur == HesapTuru.BANKA && banka.isNotBlank() && banka.trim() != kalanBanka?.trim())

    /** Başarılı kayıttan sonra: ad/bakiye temizlenir, banka ve tür bir sonraki hesap için kalır. */
    fun eklendiktenSonra(): HesapFormu = HesapFormu(banka = banka, tur = tur, maas = false, kalanBanka = banka)
}

suspend fun HesapFormu.kaydet(vm: KurulumVm): Sonuc =
    vm.hesapEkle(banka, ad, tur, bakiye, maas && tur == HesapTuru.BANKA)

/** Form kapalıysa ya da ad ve son 4 hane boşsa kaydedilecek bir şey yok. */
fun KartGirdisi?.doluMu(): Boolean = this != null && (ad.isNotBlank() || son4.isNotBlank())

fun TaksitGirdisi.doluMu(): Boolean = tutar.isNotBlank() || sayi.isNotBlank()
