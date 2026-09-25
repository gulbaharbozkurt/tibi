package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Hesapların ve kredi kartlarının bağlı olduğu banka. Nakit ve bankası girilmemiş kartlar bankasızdır. */
@Entity(tableName = "banka", indices = [Index("ad", unique = true)])
data class Banka(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val sira: Int = 0,
    val arsiv: Boolean = false,
)
