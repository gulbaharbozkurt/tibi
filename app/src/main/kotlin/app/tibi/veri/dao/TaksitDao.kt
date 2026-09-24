package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.TaksitSatiri

@Dao
interface TaksitDao {
    @Insert suspend fun ekle(satirlar: List<TaksitSatiri>)
    @Query("SELECT * FROM taksit_satiri WHERE hareketId = :hareketId ORDER BY sira")
    suspend fun hareketin(hareketId: Long): List<TaksitSatiri>
    @Query("SELECT COUNT(*) FROM taksit_satiri") suspend fun sayi(): Int
}
