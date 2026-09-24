package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.tibi.veri.tablo.Ayar

@Dao
interface AyarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun yaz(ayar: Ayar)
    @Query("SELECT deger FROM ayar WHERE anahtar = :anahtar") suspend fun oku(anahtar: String): String?
}
