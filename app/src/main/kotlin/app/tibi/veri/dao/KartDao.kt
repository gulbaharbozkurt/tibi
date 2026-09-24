package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.tibi.veri.tablo.Kart

@Dao
interface KartDao {
    @Insert suspend fun ekle(kart: Kart)
    @Update suspend fun guncelle(kart: Kart)
    @Query("SELECT * FROM kart WHERE hesapId = :hesapId") suspend fun getir(hesapId: Long): Kart?

    /** Ana kart ve ona bağlı ek/sanal kartların hesap id'leri (ana kart önce). */
    @Query("SELECT :anaKartId UNION ALL SELECT hesapId FROM kart WHERE anaKartId = :anaKartId")
    suspend fun bagliKartIdleri(anaKartId: Long): List<Long>
}
