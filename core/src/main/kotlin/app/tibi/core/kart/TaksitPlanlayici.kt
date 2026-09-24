package app.tibi.core.kart

import app.tibi.core.para.Kurus
import app.tibi.core.para.topla
import java.time.LocalDate
import java.time.YearMonth

data class PlanliTaksit(
    val sira: Int,
    val toplam: Int,
    val tutar: Kurus,
    val ekstreKesimTarihi: LocalDate,
    val oncedenOdendi: Boolean = false,
)

object TaksitPlanlayici {

    /** Yeni harcama: ilk taksit ilgili kesimden (erteleme kadar ay sonra) başlar. */
    fun yeniHarcama(
        tutar: Kurus,
        taksitSayisi: Int,
        ertelemeAy: Int,
        harcamaTarihi: LocalDate,
        takvim: KartTakvimi,
    ): List<PlanliTaksit> {
        require(taksitSayisi >= 1) { "Taksit sayısı en az 1 olmalı: $taksitSayisi" }
        require(ertelemeAy >= 0) { "Erteleme negatif olamaz: $ertelemeAy" }
        val ilkAy = YearMonth.from(takvim.ilgiliKesim(harcamaTarihi)).plusMonths(ertelemeAy.toLong())
        return tutar.bol(taksitSayisi).mapIndexed { i, parca ->
            PlanliTaksit(i + 1, taksitSayisi, parca, takvim.kesimTarihi(ilkAy.plusMonths(i.toLong())))
        }
    }

    /** Geçmiş taksit, aylık tutar biliniyor. siradakiNo: bir sonraki ekstreye düşecek taksitin numarası. */
    fun gecmisAylik(aylik: Kurus, toplam: Int, siradakiNo: Int, bugun: LocalDate, takvim: KartTakvimi) =
        gecmis(List(toplam) { aylik }, siradakiNo, bugun, takvim)

    /** Geçmiş taksit, toplam tutar biliniyor. */
    fun gecmisToplam(toplamTutar: Kurus, toplam: Int, siradakiNo: Int, bugun: LocalDate, takvim: KartTakvimi) =
        gecmis(toplamTutar.bol(toplam), siradakiNo, bugun, takvim)

    /** Geçmiş taksit, sadece kalan borç ve kalan taksit sayısı biliniyor. */
    fun kalanBorc(kalanTutar: Kurus, kalanSayi: Int, bugun: LocalDate, takvim: KartTakvimi): List<PlanliTaksit> {
        require(kalanSayi >= 1) { "Kalan taksit sayısı en az 1 olmalı: $kalanSayi" }
        return gecmis(kalanTutar.bol(kalanSayi), 1, bugun, takvim)
    }

    private fun gecmis(tutarlar: List<Kurus>, siradakiNo: Int, bugun: LocalDate, takvim: KartTakvimi): List<PlanliTaksit> {
        val toplam = tutarlar.size
        require(toplam >= 1) { "Toplam taksit en az 1 olmalı" }
        require(siradakiNo in 1..toplam) { "Sıradaki taksit 1..$toplam arası olmalı: $siradakiNo" }
        val siradakiAy = YearMonth.from(takvim.ilgiliKesim(bugun))
        return tutarlar.mapIndexed { i, tutar ->
            val sira = i + 1
            PlanliTaksit(
                sira = sira,
                toplam = toplam,
                tutar = tutar,
                ekstreKesimTarihi = takvim.kesimTarihi(siradakiAy.plusMonths((sira - siradakiNo).toLong())),
                oncedenOdendi = sira < siradakiNo,
            )
        }
    }
}

/** Ödenmemiş taksitlerin kesim tarihine göre toplamı: "gelecek ayların yükü". */
fun List<PlanliTaksit>.aylikYuk(): Map<LocalDate, Kurus> =
    filterNot { it.oncedenOdendi }
        .groupBy { it.ekstreKesimTarihi }
        .mapValues { (_, satirlar) -> satirlar.map { it.tutar }.topla() }
        .toSortedMap()
