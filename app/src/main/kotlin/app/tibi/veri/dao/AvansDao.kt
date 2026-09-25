package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Avans
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AvansDao {
    @Insert suspend fun ekle(avans: Avans)

    @Query("SELECT * FROM avans WHERE mahsupHareketId IS NULL ORDER BY hareketId")
    suspend fun acikAvanslar(): List<Avans>

    /** Maaştan henüz düşülmemiş avansların toplamı. */
    @Query(
        """
        SELECT COALESCE(SUM(h.tutarKurus), 0) FROM avans a JOIN hareket h ON h.id = a.hareketId
        WHERE a.mahsupHareketId IS NULL
        """
    )
    fun acikToplam(): Flow<Long>

    @Query("UPDATE avans SET mahsupHareketId = :maasHareketId WHERE hareketId IN (:hareketIdler)")
    suspend fun mahsupEt(hareketIdler: List<Long>, maasHareketId: Long)

    /** Tarihi verilen günden sonra olmayan açık avansların hareket id'leri. */
    @Query(
        """
        SELECT a.hareketId FROM avans a JOIN hareket h ON h.id = a.hareketId
        WHERE a.mahsupHareketId IS NULL AND h.tarih <= :tarih
        """
    )
    suspend fun acikAvansIdleri(tarih: LocalDate): List<Long>
}
