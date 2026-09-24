package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Karta yazılan her tutar; tek çekim = 1 satır. Hangi ekstreye düşeceği baştan bellidir. */
@Entity(
    tableName = "taksit_satiri",
    foreignKeys = [
        ForeignKey(entity = Hareket::class, parentColumns = ["id"], childColumns = ["hareketId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["kartId"]),
        ForeignKey(entity = Ekstre::class, parentColumns = ["id"], childColumns = ["ekstreId"]),
    ],
    indices = [Index("hareketId"), Index("kartId"), Index("ekstreId"), Index("ekstreKesimTarihi")],
)
data class TaksitSatiri(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hareketId: Long,
    val kartId: Long,
    val sira: Int,
    val toplam: Int,
    val tutarKurus: Long,
    val ekstreKesimTarihi: LocalDate,
    val ekstreId: Long? = null,
    val oncedenOdendi: Boolean = false,
)
