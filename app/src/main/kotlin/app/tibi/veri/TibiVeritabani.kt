package app.tibi.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tibi.veri.dao.HesapDao
import app.tibi.veri.dao.KartDao
import app.tibi.veri.dao.KategoriDao
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.Kategori

@Database(entities = [Hesap::class, Kart::class, Kategori::class], version = 1, exportSchema = true)
@TypeConverters(Donusturuculer::class)
abstract class TibiVeritabani : RoomDatabase() {
    abstract fun hesapDao(): HesapDao
    abstract fun kartDao(): KartDao
    abstract fun kategoriDao(): KategoriDao

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

        /** Testler için: bellekte, şifresiz. */
        fun bellekte(context: Context): TibiVeritabani =
            Room.inMemoryDatabaseBuilder(context, TibiVeritabani::class.java).addCallback(TOHUM).build()
    }
}
