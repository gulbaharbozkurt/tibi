package app.tibi.ui.hareketler

import androidx.lifecycle.ViewModel
import app.tibi.core.donem.Donem
import app.tibi.core.donem.kaydir
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.TR
import app.tibi.ui.ortak.hesapEtiketi
import app.tibi.veri.DonemServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HareketSatiri
import app.tibi.veri.tablo.HareketTuru
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class HareketFiltresi(val turler: Set<HareketTuru>?) {
    TUMU(null),
    HARCAMA(setOf(HareketTuru.HARCAMA)),
    GELIR(setOf(HareketTuru.GELIR, HareketTuru.TAHSILAT, HareketTuru.AVANS)),
    TRANSFER(setOf(HareketTuru.TRANSFER, HareketTuru.KART_ODEME)),
}

data class GunGrubu(val tarih: LocalDate, val satirlar: List<HareketSatiri>)

fun HareketSatiri.eslesir(arama: String): Boolean {
    val a = arama.trim().lowercase(TR)
    if (a.isEmpty()) return true
    val alanlar = listOfNotNull(kalem, kategoriAdi, aciklama, kaynakAdi, hedefAdi, kaynakBankaAdi, hedefBankaAdi,
        Kurus(tutarKurus).bicimle())
    return alanlar.any { it.lowercase(TR).contains(a) }
}

/** Satır başlığı: kalem adı varsa o, yoksa kategori ya da hareket türü. */
fun satirBasligi(s: HareketSatiri): String = s.kalem ?: s.kategoriAdi ?: when (s.tur) {
    HareketTuru.TRANSFER -> "Transfer"
    HareketTuru.KART_ODEME -> "Kart ödemesi"
    HareketTuru.GELIR -> "Gelir"
    HareketTuru.AVANS -> "Avans"
    HareketTuru.TAHSILAT -> "Tahsilat"
    HareketTuru.DUZELTME -> "Bakiye düzeltme"
    else -> s.aciklama ?: "Harcama"
}

/** Kurulumda girilmiş, karta yazılmış geçmiş taksit: bugünün gideri değil, kalan borç olarak gösterilir. */
private val HareketSatiri.kurulumTaksidi: Boolean get() = gecmisAktarim && kalanKurus != null

/** Başlığın altındaki satır: (kalem başlıksa) kategori, hesap(lar), taksit, avans durumu, not. */
fun satirAltMetni(s: HareketSatiri): String = if (s.kurulumTaksidi) {
    "Kurulumda girilen taksit · " + when {
        s.taksitSayisi <= 1 -> "tek çekim"
        (s.kalanTaksit ?: 0) == 0 -> "taksit kalmadı"
        else -> "${s.kalanTaksit} taksit kaldı"
    }
} else buildList {
    if (s.kalem != null) s.kategoriAdi?.let(::add)
    val kaynak = s.kaynakAdi?.let { hesapEtiketi(s.kaynakBankaAdi, it) }
    val hedef = s.hedefAdi?.let { hesapEtiketi(s.hedefBankaAdi, it) }
    when {
        kaynak != null && hedef != null -> add("$kaynak → $hedef")
        else -> (kaynak ?: hedef)?.let(::add)
    }
    if (s.taksitSayisi > 1) add("${s.taksitSayisi} taksit")
    when (s.avansAcik) { true -> add("maaştan düşülecek"); false -> add("maaştan düşüldü"); null -> Unit }
    if (s.kalem != null || s.kategoriAdi != null || s.tur == HareketTuru.AVANS) s.aciklama?.let(::add)
}.joinToString(" · ")

enum class TutarRengi { GIRIS, NORMAL, SOLUK }

/** Tutar metni ve rengi: giriş "+", çıkış "−"; kurulum taksidi kalan tutarıyla, işaretsiz ve soluk. */
fun satirTutari(s: HareketSatiri): Pair<String, TutarRengi> {
    if (s.kurulumTaksidi) return Kurus(s.kalanKurus!!).bicimle() to TutarRengi.SOLUK
    val duzeltme = s.tur == HareketTuru.DUZELTME
    val giris = s.tur in setOf(HareketTuru.GELIR, HareketTuru.TAHSILAT, HareketTuru.AVANS) || (duzeltme && s.hedefAdi != null)
    val cikis = s.tur == HareketTuru.HARCAMA || (duzeltme && s.kaynakAdi != null)
    val metin = (if (giris) "+" else if (cikis) "−" else "") + Kurus(s.tutarKurus).bicimle()
    return metin to if (giris) TutarRengi.GIRIS else TutarRengi.NORMAL
}

class HareketlerVm(
    private val db: TibiVeritabani,
    donemServisi: DonemServisi,
    bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val filtre = MutableStateFlow(HareketFiltresi.TUMU)
    val arama = MutableStateFlow("")
    /** 0 = bu dönem, -1 = önceki, …; hiçbir zaman 0'dan büyük olmaz. */
    val kaydirma = MutableStateFlow(0)
    private val gun = bugun()
    val donem: Flow<Donem> = combine(donemServisi.bulucu, kaydirma) { bul, k -> kaydir(bul(gun), k, bul) }
    val buDonemMi: Flow<Boolean> = kaydirma.map { it == 0 }

    fun onceki() { kaydirma.update { it - 1 } }
    fun sonraki() { kaydirma.update { minOf(0, it + 1) } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val gruplar: Flow<List<GunGrubu>> = donem.distinctUntilChanged().flatMapLatest { d ->
        combine(db.hareketDao().satirlar(d.baslangic, d.bitis), filtre, arama) { satirlar, f, a ->
            satirlar.filter { (f.turler == null || it.tur in f.turler) && it.eslesir(a) }
                .groupBy { it.tarih }
                .map { (tarih, liste) -> GunGrubu(tarih, liste) }
        }
    }

    suspend fun sil(id: Long) = db.hareketDao().sil(id)
}
