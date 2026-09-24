package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class HesapTuru { BANKA, NAKIT, KREDI_KARTI }

@Entity(tableName = "hesap")
data class Hesap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val tur: HesapTuru,
    val acilisBakiyeKurus: Long = 0,
    val acilisTarihi: LocalDate,
    val maasHesabi: Boolean = false,
    val renk: Int? = null,
    val sira: Int = 0,
    val arsiv: Boolean = false,
)
