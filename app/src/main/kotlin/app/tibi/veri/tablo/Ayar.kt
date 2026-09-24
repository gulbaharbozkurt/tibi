package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ayar")
data class Ayar(@PrimaryKey val anahtar: String, val deger: String)
