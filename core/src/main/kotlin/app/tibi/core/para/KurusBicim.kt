package app.tibi.core.para

/** "1.249,90 ₺" biçimi: binlik ayırıcı nokta, ondalık virgül. */
fun Kurus.bicimle(): String {
    val isaret = if (deger < 0) "-" else ""
    val mutlak = kotlin.math.abs(deger)
    val lira = (mutlak / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    val kurus = (mutlak % 100).toString().padStart(2, '0')
    return "$isaret$lira,$kurus ₺"
}

private val GECERLI = Regex("""^\d{1,3}(\.\d{3})*(,\d{1,2})?$|^\d+(,\d{1,2})?$""")

/** Kullanıcı girişini çözer. Nokta binlik, virgül ondalık ayırıcıdır. Geçersizse null. */
fun kurusCoz(metin: String): Kurus? {
    val temiz = metin.replace("₺", "").trim()
    if (!GECERLI.matches(temiz)) return null
    val parcalar = temiz.replace(".", "").split(",")
    val lira = parcalar[0].toLongOrNull() ?: return null
    val kurus = parcalar.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
    return Kurus(lira * 100 + kurus)
}
