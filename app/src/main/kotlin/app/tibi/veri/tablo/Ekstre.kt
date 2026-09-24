package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class EkstreDurumu { KESILDI, KISMI, ODENDI }

/** Kesilmiş ekstrenin dondurulmuş özeti. acilis = kurulumda girilen ödenmemiş ekstre. */
@Entity(
    tableName = "ekstre",
    foreignKeys = [ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["kartId"])],
    indices = [Index("kartId")],
)
data class Ekstre(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kartId: Long,
    val kesimTarihi: LocalDate,
    val sonOdemeTarihi: LocalDate,
    val donemTutariKurus: Long,
    val devredenKurus: Long = 0,
    val toplamKurus: Long,
    val asgariKurus: Long,
    val durum: EkstreDurumu = EkstreDurumu.KESILDI,
    val acilis: Boolean = false,
)
