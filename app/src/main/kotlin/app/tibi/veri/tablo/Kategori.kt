package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class KategoriYonu { GIDER, GELIR }

@Entity(tableName = "kategori", indices = [Index("ad", unique = true)])
data class Kategori(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val yon: KategoriYonu,
    val sira: Int = 0,
    val arsiv: Boolean = false,
)
