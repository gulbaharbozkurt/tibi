package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class KartTuru { ANA, EK, SANAL }

/** Kredi kartı; Hesap'ın uzantısı (hesapId hem birincil hem yabancı anahtar). */
@Entity(
    tableName = "kart",
    foreignKeys = [
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["hesapId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["anaKartId"]),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["odemeHesapId"]),
    ],
    indices = [Index("anaKartId"), Index("odemeHesapId")],
)
data class Kart(
    @PrimaryKey val hesapId: Long,
    val kartTuru: KartTuru = KartTuru.ANA,
    val anaKartId: Long? = null,
    val son4: String,
    val kesimGunu: Int,
    val sonOdemeGunu: Int,
    val bankaLimitiKurus: Long? = null,
    val kendiLimitiKurus: Long? = null,
    val asgariOranBinde: Int = 400,
    val otoOdeme: Boolean = false,
    val odemeHesapId: Long? = null,
    val aidatAy: Int? = null,
    val aidatTutarKurus: Long? = null,
)
