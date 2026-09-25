package app.tibi.ui.kurulum

import app.tibi.core.para.kurusCoz

/** Kart girdisinde kullanıcıya gösterilecek hata; mesaj sorunu ve çözümü söyler. */
internal class GirdiHatasi(mesaj: String) : Exception(mesaj)

/** Ana kartın takvim, limit ve asgari oran alanları, çözülmüş hâliyle. */
internal data class KartAyarlari(
    val kesimGunu: Int,
    val sonOdemeGunu: Int,
    val bankaLimitiKurus: Long?,
    val kendiLimitiKurus: Long?,
    val asgariOranBinde: Int,
)

/** Kart ekleme (kurulum) ve kart düzenlemenin ortak doğrulaması. Hatada GirdiHatasi atar. */
internal object KartDogrulayici {
    fun kimlik(g: KartGirdisi) {
        if (g.ad.isBlank()) throw GirdiHatasi("Karta bir ad ver, ör. \"Bonus\".")
        if (!Regex("""\d{4}""").matches(g.son4)) throw GirdiHatasi("Kartın son 4 hanesini yaz.")
    }

    fun tutarVeyaBos(metin: String, alan: String): Long? =
        if (metin.isBlank()) null else (kurusCoz(metin) ?: throw GirdiHatasi("$alan tutarını 15.000 biçiminde yaz.")).deger

    fun gun(metin: String, alan: String): Int =
        metin.toIntOrNull()?.takeIf { it in 1..31 } ?: throw GirdiHatasi("$alan 1 ile 31 arasında olmalı.")

    fun asgariOranBinde(metin: String): Int =
        (metin.toIntOrNull()?.takeIf { it in 0..100 } ?: throw GirdiHatasi("Asgari ödeme oranı 0 ile 100 arasında olmalı.")) * 10

    /** Ana kartın kendi girdiği alanlar (ek/sanal kart bunları ana karttan alır). */
    fun anaKartAyarlari(g: KartGirdisi): KartAyarlari {
        val kesim = gun(g.kesimGunu, "Kesim günü")
        val sonOdeme = gun(g.sonOdemeGunu, "Son ödeme günü")
        val bankaLimiti = tutarVeyaBos(g.bankaLimiti, "Banka limiti")
        val kendiLimiti = tutarVeyaBos(g.kendiLimiti, "Kendi limitin")
        return KartAyarlari(kesim, sonOdeme, bankaLimiti, kendiLimiti, asgariOranBinde(g.asgariOran))
    }
}
