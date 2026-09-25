package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import kotlinx.coroutines.flow.Flow

data class KartBilgisi(
    val hesapId: Long,
    val ad: String,
    val son4: String,
    val kartTuru: KartTuru,
    val anaKartId: Long?,
    val kesimGunu: Int,
    val sonOdemeGunu: Int,
    val bankaLimitiKurus: Long?,
    val kendiLimitiKurus: Long?,
    val asgariOranBinde: Int,
    val kullanimKurus: Long,
    val bankaId: Long? = null,
    val bankaAdi: String? = null,
)

@Dao
interface KartDao {
    @Insert suspend fun ekle(kart: Kart)
    @Update suspend fun guncelle(kart: Kart)
    @Query("SELECT * FROM kart WHERE hesapId = :hesapId") suspend fun getir(hesapId: Long): Kart?

    /** Ana kart ve ona bağlı ek/sanal kartların hesap id'leri (ana kart önce). */
    @Query("SELECT :anaKartId UNION ALL SELECT hesapId FROM kart WHERE anaKartId = :anaKartId")
    suspend fun bagliKartIdleri(anaKartId: Long): List<Long>

    /** kullanimKurus: TaksitDao.limitKullanimi ile aynı formül; yalnızca ana kartta dolu. */
    @Query(
        """
        SELECT h.id AS hesapId, h.ad, k.son4, k.kartTuru, k.anaKartId, k.kesimGunu, k.sonOdemeGunu,
               k.bankaLimitiKurus, k.kendiLimitiKurus, k.asgariOranBinde, h.bankaId, b.ad AS bankaAdi,
               CASE WHEN k.anaKartId IS NULL THEN
                   COALESCE((SELECT SUM(t.tutarKurus) FROM taksit_satiri t WHERE t.oncedenOdendi = 0
                             AND (t.kartId = k.hesapId OR t.kartId IN (SELECT e.hesapId FROM kart e WHERE e.anaKartId = k.hesapId))), 0)
                 + COALESCE((SELECT SUM(x.toplamKurus) FROM ekstre x WHERE x.acilis = 1 AND x.kartId = k.hesapId), 0)
                 - COALESCE((SELECT SUM(o.tutarKurus) FROM hareket o WHERE o.tur = 'KART_ODEME'
                             AND (o.hedefHesapId = k.hesapId OR o.hedefHesapId IN (SELECT e.hesapId FROM kart e WHERE e.anaKartId = k.hesapId))), 0)
               ELSE 0 END AS kullanimKurus
        FROM kart k JOIN hesap h ON h.id = k.hesapId LEFT JOIN banka b ON b.id = h.bankaId
        WHERE h.arsiv = 0 ORDER BY h.sira, h.id
        """
    )
    fun kartlar(): Flow<List<KartBilgisi>>
}
