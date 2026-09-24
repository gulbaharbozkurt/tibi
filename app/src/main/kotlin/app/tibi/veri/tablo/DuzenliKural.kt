package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.tibi.core.tarih.HaftaSonuKurali
import java.time.LocalDate

enum class Periyot { HAFTALIK, AYLIK, YILLIK }

/** Maaş, abonelik, kira gibi tekrarlayan gelir/gider. maas = true olan tek kural dönemi belirler. */
@Entity(
    tableName = "duzenli_kural",
    foreignKeys = [
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["hesapId"]),
        ForeignKey(entity = Kategori::class, parentColumns = ["id"], childColumns = ["kategoriId"]),
    ],
    indices = [Index("hesapId"), Index("kategoriId")],
)
data class DuzenliKural(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val yon: KategoriYonu,
    val maas: Boolean = false,
    val tutarKurus: Long,
    val degisken: Boolean = false,
    val periyot: Periyot,
    val gun: Int,
    val ay: Int? = null,
    val haftaSonuKurali: HaftaSonuKurali = HaftaSonuKurali.AYNI,
    val hesapId: Long,
    val kategoriId: Long? = null,
    val otomatik: Boolean = true,
    val baslangic: LocalDate,
    val bitis: LocalDate? = null,
    val aktif: Boolean = true,
)
