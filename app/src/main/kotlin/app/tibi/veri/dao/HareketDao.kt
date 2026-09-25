package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.HareketTuru
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class DonemToplami(val gelirKurus: Long, val giderKurus: Long)

data class HareketSatiri(
    val id: Long,
    val tur: HareketTuru,
    val tarih: LocalDate,
    val tutarKurus: Long,
    val kategoriAdi: String?,
    val kaynakAdi: String?,
    val hedefAdi: String?,
    val taksitSayisi: Int,
    val aciklama: String?,
    val gecmisAktarim: Boolean,
    /** Yalnızca avansta dolu: true = maaştan düşülecek, false = düşüldü. */
    val avansAcik: Boolean? = null,
)

@Dao
interface HareketDao {
    @Insert suspend fun ekle(hareket: Hareket): Long
    @Query("SELECT * FROM hareket WHERE id = :id") suspend fun getir(id: Long): Hareket?
    @Query("SELECT * FROM hareket WHERE tarih BETWEEN :bas AND :bit ORDER BY tarih DESC, id DESC")
    fun aralik(bas: LocalDate, bit: LocalDate): Flow<List<Hareket>>
    @Query("SELECT COUNT(*) FROM hareket") suspend fun sayi(): Int

    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN tur IN ('GELIR', 'TAHSILAT') THEN tutarKurus END), 0) AS gelirKurus,
          COALESCE(SUM(CASE WHEN tur = 'HARCAMA' AND gecmisAktarim = 0 THEN tutarKurus END), 0) AS giderKurus
        FROM hareket WHERE tarih BETWEEN :bas AND :bit
        """
    )
    fun donemToplami(bas: LocalDate, bit: LocalDate): Flow<DonemToplami>

    @Query(
        """
        SELECT h.id, h.tur, h.tarih, h.tutarKurus, k.ad AS kategoriAdi, ks.ad AS kaynakAdi, hd.ad AS hedefAdi,
               h.taksitSayisi, h.aciklama, h.gecmisAktarim,
               CASE WHEN a.hareketId IS NULL THEN NULL WHEN a.mahsupHareketId IS NULL THEN 1 ELSE 0 END AS avansAcik
        FROM hareket h
        LEFT JOIN avans a ON a.hareketId = h.id
        LEFT JOIN kategori k ON k.id = h.kategoriId
        LEFT JOIN hesap ks ON ks.id = h.kaynakHesapId
        LEFT JOIN hesap hd ON hd.id = h.hedefHesapId
        WHERE h.tarih BETWEEN :bas AND :bit
        ORDER BY h.tarih DESC, h.id DESC
        """
    )
    fun satirlar(bas: LocalDate, bit: LocalDate): Flow<List<HareketSatiri>>

    @Query("DELETE FROM hareket WHERE id = :id") suspend fun sil(id: Long)
}
