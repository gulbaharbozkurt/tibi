package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.DuzenliKural

@Dao
interface DuzenliKuralDao {
    @Insert suspend fun ekle(kural: DuzenliKural): Long
    @Query("SELECT * FROM duzenli_kural WHERE maas = 1 AND aktif = 1 ORDER BY id LIMIT 1")
    suspend fun maasKurali(): DuzenliKural?
}
