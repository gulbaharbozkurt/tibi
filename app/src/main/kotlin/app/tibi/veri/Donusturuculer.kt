package app.tibi.veri

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Donusturuculer {
    @TypeConverter fun tarihtenSayiya(t: LocalDate?): Long? = t?.toEpochDay()
    @TypeConverter fun sayidanTarihe(g: Long?): LocalDate? = g?.let(LocalDate::ofEpochDay)
    @TypeConverter fun andanSayiya(a: Instant?): Long? = a?.toEpochMilli()
    @TypeConverter fun sayidanAna(ms: Long?): Instant? = ms?.let(Instant::ofEpochMilli)
}
