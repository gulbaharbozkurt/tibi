package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Kategori
import app.tibi.veri.tablo.KategoriYonu
import kotlinx.coroutines.flow.Flow

@Dao
interface KategoriDao {
    @Insert suspend fun ekle(kategori: Kategori): Long
    @Query("SELECT * FROM kategori WHERE yon = :yon AND arsiv = 0 ORDER BY sira, id")
    fun tumu(yon: KategoriYonu): Flow<List<Kategori>>
    @Query("SELECT * FROM kategori WHERE ad = :ad") suspend fun adIle(ad: String): Kategori?
    @Query("SELECT * FROM kategori WHERE id = :id") suspend fun getir(id: Long): Kategori?

    /** E4: en çok kullanılan kategori önce; eşitlikte varsayılan sıra. */
    @Query(
        """
        SELECT k.* FROM kategori k LEFT JOIN hareket h ON h.kategoriId = k.id
        WHERE k.yon = :yon AND k.arsiv = 0
        GROUP BY k.id ORDER BY COUNT(h.id) DESC, k.sira, k.id
        """
    )
    fun kullanimSirali(yon: KategoriYonu): Flow<List<Kategori>>
}
