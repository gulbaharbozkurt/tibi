package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Hareket
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HareketDao {
    @Insert suspend fun ekle(hareket: Hareket): Long
    @Query("SELECT * FROM hareket WHERE id = :id") suspend fun getir(id: Long): Hareket?
    @Query("SELECT * FROM hareket WHERE tarih BETWEEN :bas AND :bit ORDER BY tarih DESC, id DESC")
    fun aralik(bas: LocalDate, bit: LocalDate): Flow<List<Hareket>>
    @Query("SELECT COUNT(*) FROM hareket") suspend fun sayi(): Int
}
