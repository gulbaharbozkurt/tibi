package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.tibi.veri.tablo.Hesap
import kotlinx.coroutines.flow.Flow

@Dao
interface HesapDao {
    @Insert suspend fun ekle(hesap: Hesap): Long
    @Update suspend fun guncelle(hesap: Hesap)
    @Query("SELECT * FROM hesap WHERE id = :id") suspend fun getir(id: Long): Hesap?
    @Query("SELECT * FROM hesap WHERE arsiv = 0 ORDER BY sira, id") fun tumu(): Flow<List<Hesap>>
}
