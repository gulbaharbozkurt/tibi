package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Banka
import kotlinx.coroutines.flow.Flow

@Dao
interface BankaDao {
    @Insert suspend fun ekle(banka: Banka): Long
    @Query("SELECT * FROM banka WHERE ad = :ad") suspend fun adIle(ad: String): Banka?
    /** Arşivliler dahil; ad eşleştirmesi için. */
    @Query("SELECT * FROM banka") suspend fun hepsi(): List<Banka>
    @Query("SELECT * FROM banka WHERE arsiv = 0 ORDER BY sira, id") fun tumu(): Flow<List<Banka>>
}
