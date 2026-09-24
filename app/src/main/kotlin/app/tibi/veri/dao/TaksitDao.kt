package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.TaksitSatiri
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class AylikYuk(val ekstreKesimTarihi: LocalDate, val toplamKurus: Long)

@Dao
interface TaksitDao {
    @Insert suspend fun ekle(satirlar: List<TaksitSatiri>)

    @Query("SELECT * FROM taksit_satiri WHERE hareketId = :hareketId ORDER BY sira")
    suspend fun hareketin(hareketId: Long): List<TaksitSatiri>

    @Query("SELECT COUNT(*) FROM taksit_satiri") suspend fun sayi(): Int

    /**
     * Ana kart + bağlı ek/sanal kartlar: ödenmemiş bütün taksit satırları (gelecek dahil)
     * + açılış ekstresi − kart ödemeleri.
     */
    @Query(
        """
        SELECT
          COALESCE((SELECT SUM(t.tutarKurus) FROM taksit_satiri t
                    WHERE t.oncedenOdendi = 0
                      AND t.kartId IN (SELECT :anaKartId UNION SELECT hesapId FROM kart WHERE anaKartId = :anaKartId)), 0)
        + COALESCE((SELECT SUM(e.toplamKurus) FROM ekstre e WHERE e.acilis = 1 AND e.kartId = :anaKartId), 0)
        - COALESCE((SELECT SUM(h.tutarKurus) FROM hareket h
                    WHERE h.tur = 'KART_ODEME'
                      AND h.hedefHesapId IN (SELECT :anaKartId UNION SELECT hesapId FROM kart WHERE anaKartId = :anaKartId)), 0)
        """
    )
    fun limitKullanimi(anaKartId: Long): Flow<Long>

    /** Bütün kartlarda, bugün ve sonrasına düşen ödenmemiş taksitlerin kesim tarihine göre toplamı. */
    @Query(
        """
        SELECT ekstreKesimTarihi, SUM(tutarKurus) AS toplamKurus FROM taksit_satiri
        WHERE oncedenOdendi = 0 AND ekstreKesimTarihi >= :bugun
        GROUP BY ekstreKesimTarihi ORDER BY ekstreKesimTarihi
        """
    )
    fun aylikYuk(bugun: LocalDate): Flow<List<AylikYuk>>
}
