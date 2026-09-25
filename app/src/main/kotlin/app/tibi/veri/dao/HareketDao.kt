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
    val kalem: String? = null,
    /** Banka hesabının bankası; kart ve nakitte null (kart kendi adıyla gösterilir). */
    val kaynakBankaAdi: String? = null,
    val hedefBankaAdi: String? = null,
    /** Karta yazılmış hareketlerde: önceden ödenmemiş taksit satırlarının toplamı ve sayısı; kart dışında null. */
    val kalanKurus: Long? = null,
    val kalanTaksit: Int? = null,
)

/** Hızlı girişte öneri: kalemin en son yazılışı, en son kategorisi ve tutarı, kaç kez girildiği. */
data class KalemOnerisi(val kalem: String, val kalemAnahtar: String, val kategoriId: Long?, val sonTutarKurus: Long, val sayi: Int)

/** Bir kalemin bir takvim ayındaki alımları; ay "2026-09" biçiminde. */
data class AylikKalem(val ay: String, val sayi: Int, val toplamKurus: Long)

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
               h.taksitSayisi, h.aciklama, h.gecmisAktarim, h.kalem,
               CASE WHEN a.hareketId IS NULL THEN NULL WHEN a.mahsupHareketId IS NULL THEN 1 ELSE 0 END AS avansAcik,
               kb.ad AS kaynakBankaAdi, hb.ad AS hedefBankaAdi, ts.kalanKurus, ts.kalanTaksit
        FROM hareket h
        LEFT JOIN avans a ON a.hareketId = h.id
        LEFT JOIN kategori k ON k.id = h.kategoriId
        LEFT JOIN hesap ks ON ks.id = h.kaynakHesapId
        LEFT JOIN hesap hd ON hd.id = h.hedefHesapId
        LEFT JOIN banka kb ON kb.id = ks.bankaId AND ks.tur = 'BANKA'
        LEFT JOIN banka hb ON hb.id = hd.bankaId AND hd.tur = 'BANKA'
        LEFT JOIN (
          SELECT hareketId,
                 COALESCE(SUM(CASE WHEN oncedenOdendi = 0 THEN tutarKurus END), 0) AS kalanKurus,
                 SUM(CASE WHEN oncedenOdendi = 0 THEN 1 ELSE 0 END) AS kalanTaksit
          FROM taksit_satiri GROUP BY hareketId
        ) ts ON ts.hareketId = h.id
        WHERE h.tarih BETWEEN :bas AND :bit
        ORDER BY h.tarih DESC, h.id DESC
        """
    )
    fun satirlar(bas: LocalDate, bit: LocalDate): Flow<List<HareketSatiri>>

    /** [arama] zaten kalemAnahtari() ile normalize edilmiş olmalı; boşsa bütün kalemler. Sık girilen önce. */
    @Query(
        """
        SELECT h.kalem AS kalem, h.kalemAnahtar AS kalemAnahtar, h.kategoriId AS kategoriId,
               h.tutarKurus AS sonTutarKurus, g.sayi AS sayi
        FROM (
          SELECT kalemAnahtar, COUNT(*) AS sayi, MAX(tarih) AS sonTarih FROM hareket
          WHERE tur = 'HARCAMA' AND kalemAnahtar IS NOT NULL AND kalemAnahtar LIKE '%' || :arama || '%'
          GROUP BY kalemAnahtar
        ) g
        JOIN hareket h ON h.id = (
          SELECT s.id FROM hareket s WHERE s.tur = 'HARCAMA' AND s.kalemAnahtar = g.kalemAnahtar
          ORDER BY s.tarih DESC, s.id DESC LIMIT 1
        )
        ORDER BY g.sayi DESC, g.sonTarih DESC
        LIMIT :limit
        """
    )
    suspend fun kalemOnerileri(arama: String, limit: Int = 6): List<KalemOnerisi>

    /** Takvim ayına göre alım sayısı ve toplamı, yeni ay önce; geçmiş aktarım sayılmaz. tarih epochDay'dir. */
    @Query(
        """
        SELECT strftime('%Y-%m', tarih * 86400, 'unixepoch') AS ay, COUNT(*) AS sayi, SUM(tutarKurus) AS toplamKurus
        FROM hareket
        WHERE tur = 'HARCAMA' AND kalemAnahtar = :kalemAnahtar AND gecmisAktarim = 0
        GROUP BY ay ORDER BY ay DESC
        """
    )
    fun kalemOzeti(kalemAnahtar: String): Flow<List<AylikKalem>>

    /** Kalemin en son girildiği yazılış (başlık için). */
    @Query(
        """
        SELECT kalem FROM hareket WHERE tur = 'HARCAMA' AND kalemAnahtar = :kalemAnahtar
        ORDER BY tarih DESC, id DESC LIMIT 1
        """
    )
    fun kalemAdi(kalemAnahtar: String): Flow<String?>

    @Query("DELETE FROM hareket WHERE id = :id") suspend fun sil(id: Long)
}
