package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.tibi.veri.tablo.DuzenliKural
import kotlinx.coroutines.flow.Flow

@Dao
interface DuzenliKuralDao {
    @Insert suspend fun ekle(kural: DuzenliKural): Long
    @Update suspend fun guncelle(kural: DuzenliKural)
    @Query("SELECT * FROM duzenli_kural WHERE maas = 1 AND aktif = 1 ORDER BY id LIMIT 1")
    suspend fun maasKurali(): DuzenliKural?

    @Query("SELECT * FROM duzenli_kural WHERE maas = 1 AND aktif = 1 ORDER BY id LIMIT 1")
    fun maasKuraliAkisi(): Flow<DuzenliKural?>
}
