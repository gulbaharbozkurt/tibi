package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class HareketTuru { HARCAMA, GELIR, AVANS, TAHSILAT, TRANSFER, KART_ODEME, CUZDAN_AKTAR, DUZELTME }
enum class TaksitTuru { ALISVERIS, EKSTRE_TAKSIT, NAKIT_AVANS }

/** Tek kaynak: her para hareketi. Yönü kaynak (çıktığı) ve hedef (girdiği) hesap belirler. */
@Entity(
    tableName = "hareket",
    foreignKeys = [
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["kaynakHesapId"]),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["hedefHesapId"]),
        ForeignKey(entity = Kategori::class, parentColumns = ["id"], childColumns = ["kategoriId"]),
        ForeignKey(entity = Ekstre::class, parentColumns = ["id"], childColumns = ["ekstreId"]),
    ],
    indices = [Index("kaynakHesapId"), Index("hedefHesapId"), Index("kategoriId"), Index("ekstreId"), Index("tarih"), Index("kalemAnahtar")],
)
data class Hareket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tur: HareketTuru,
    val tarih: LocalDate,
    val tutarKurus: Long,
    val kaynakHesapId: Long? = null,
    val hedefHesapId: Long? = null,
    val kategoriId: Long? = null,
    val cuzdanId: Long? = null,
    val taksitSayisi: Int = 1,
    val ertelemeAy: Int = 0,
    val taksitTuru: TaksitTuru? = null,
    val gecmisAktarim: Boolean = false,
    val ekstreId: Long? = null,
    val ogrenciId: Long? = null,
    val duzenliKuralId: Long? = null,
    val beklenenTarih: LocalDate? = null,
    val aciklama: String? = null,
    val olusturma: Instant,
    /** Harcamanın adı (ör. "Probis"), yazıldığı gibi. */
    val kalem: String? = null,
    /** Kalemin gruplama anahtarı: Türkçe küçük harf; bkz. kalemAnahtari(). */
    val kalemAnahtar: String? = null,
)
