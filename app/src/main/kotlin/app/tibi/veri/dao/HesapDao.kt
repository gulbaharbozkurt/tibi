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
    /** açılış + girenler − çıkanlar. Banka ve nakit için anlamlı; kartlar için limit kullanımı ayrı hesaplanır. */
    @Query(
        """
        SELECT h.acilisBakiyeKurus
             + COALESCE((SELECT SUM(g.tutarKurus) FROM hareket g WHERE g.hedefHesapId = h.id), 0)
             - COALESCE((SELECT SUM(c.tutarKurus) FROM hareket c WHERE c.kaynakHesapId = h.id), 0)
        FROM hesap h WHERE h.id = :hesapId
        """
    )
    fun bakiye(hesapId: Long): Flow<Long>
}
