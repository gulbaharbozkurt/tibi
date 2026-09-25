package app.tibi.ui.hareketler

import androidx.lifecycle.ViewModel
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.TR
import app.tibi.veri.TibiVeritabani
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

data class KalemAyi(val ay: YearMonth, val etiket: String, val sayi: Int, val toplamKurus: Long, val metin: String)

data class KalemOzeti(val baslik: String, val son12Ay: String, val aylar: List<KalemAyi>)

/** "6 kez · 540,00 ₺ · ort. 90,00 ₺"; ortalama kuruş üzerinden tam bölmeyle. */
fun kalemMetni(sayi: Int, toplamKurus: Long): String {
    val ort = if (sayi > 0) toplamKurus / sayi else 0
    return "$sayi kez · ${Kurus(toplamKurus).bicimle()} · ort. ${Kurus(ort).bicimle()}"
}

/** "Eylül 2026" */
fun YearMonth.ayEtiketi(): String =
    "${month.getDisplayName(TextStyle.FULL_STANDALONE, TR).replaceFirstChar { it.titlecase(TR) }} $year"

class KalemOzetiVm(
    db: TibiVeritabani,
    private val kalemAnahtar: String,
    bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val ilkAy = YearMonth.from(bugun()).minusMonths(11)

    val ozet: Flow<KalemOzeti> = combine(db.hareketDao().kalemAdi(kalemAnahtar), db.hareketDao().kalemOzeti(kalemAnahtar)) { ad, aylar ->
        val satirlar = aylar.map {
            val ay = YearMonth.parse(it.ay)
            KalemAyi(ay, ay.ayEtiketi(), it.sayi, it.toplamKurus, kalemMetni(it.sayi, it.toplamKurus))
        }
        val son12 = satirlar.filter { !it.ay.isBefore(ilkAy) }
        KalemOzeti(
            baslik = ad ?: kalemAnahtar,
            son12Ay = "Son 12 ay: ${son12.sumOf { it.sayi }} kez · ${Kurus(son12.sumOf { it.toplamKurus }).bicimle()}",
            aylar = satirlar,
        )
    }
}
