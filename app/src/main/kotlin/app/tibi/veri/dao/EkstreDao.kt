package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Ekstre

@Dao
interface EkstreDao {
    @Insert suspend fun ekle(ekstre: Ekstre): Long
    @Query("SELECT * FROM ekstre WHERE id = :id") suspend fun getir(id: Long): Ekstre?
}
