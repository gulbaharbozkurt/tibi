package app.tibi.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tibi.veri.dao.AyarDao
import app.tibi.veri.dao.DuzenliKuralDao
import app.tibi.veri.dao.EkstreDao
import app.tibi.veri.dao.HareketDao
import app.tibi.veri.dao.HesapDao
import app.tibi.veri.dao.KartDao
import app.tibi.veri.dao.KategoriDao
import app.tibi.veri.dao.TaksitDao
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Ekstre
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.Kategori
import app.tibi.veri.tablo.TaksitSatiri

@Database(
    entities = [
        Hesap::class, Kart::class, Kategori::class, Hareket::class, TaksitSatiri::class,
        Ekstre::class, DuzenliKural::class, Ayar::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Donusturuculer::class)
abstract class TibiVeritabani : RoomDatabase() {
    abstract fun hesapDao(): HesapDao
    abstract fun kartDao(): KartDao
    abstract fun kategoriDao(): KategoriDao
    abstract fun hareketDao(): HareketDao
    abstract fun taksitDao(): TaksitDao
    abstract fun ekstreDao(): EkstreDao
    abstract fun duzenliKuralDao(): DuzenliKuralDao
    abstract fun ayarDao(): AyarDao

    companion object {
        /** İlk oluşturmada varsayılan kategorileri yazar. */
        val TOHUM = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                fun yaz(adlar: List<String>, yon: String) = adlar.forEachIndexed { i, ad ->
                    db.execSQL("INSERT INTO kategori (ad, yon, sira, arsiv) VALUES (?, ?, ?, 0)", arrayOf<Any>(ad, yon, i))
                }
                yaz(VarsayilanKategoriler.GIDER, "GIDER")
                yaz(VarsayilanKategoriler.GELIR, "GELIR")
            }
        }

        /** Telefonda: SQLCipher ile şifreli dosya. */
        fun olustur(
            context: Context,
            ad: String = "tibi.db",
            parola: ByteArray = VeritabaniAnahtari(context).parola(),
        ): TibiVeritabani {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, TibiVeritabani::class.java, ad)
                .openHelperFactory(net.zetetic.database.sqlcipher.SupportOpenHelperFactory(parola))
                .addCallback(TOHUM)
                .build()
        }

        /** Testler için: bellekte, şifresiz. */
        fun bellekte(context: Context): TibiVeritabani =
            Room.inMemoryDatabaseBuilder(context, TibiVeritabani::class.java).addCallback(TOHUM).build()
    }
}
