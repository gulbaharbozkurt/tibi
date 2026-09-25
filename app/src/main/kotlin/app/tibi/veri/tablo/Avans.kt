package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A1–A2: AVANS türündeki hareketin "maaştan düşülecek" durumu.
 * mahsupHareketId, avansı kapatan maaş gelirinin id'si; o gelir silinirse avans yeniden açılır.
 */
@Entity(
    tableName = "avans",
    foreignKeys = [
        ForeignKey(entity = Hareket::class, parentColumns = ["id"], childColumns = ["hareketId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Hareket::class, parentColumns = ["id"], childColumns = ["mahsupHareketId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("mahsupHareketId")],
)
data class Avans(
    @PrimaryKey val hareketId: Long,
    val dusulecekMaasTarihi: LocalDate?,
    val mahsupHareketId: Long? = null,
)
