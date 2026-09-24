package app.tibi.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.tibi.veri.dao.HesapDao
import app.tibi.veri.tablo.Hesap

@Database(entities = [Hesap::class], version = 1, exportSchema = true)
@TypeConverters(Donusturuculer::class)
abstract class TibiVeritabani : RoomDatabase() {
    abstract fun hesapDao(): HesapDao

    companion object {
        /** Testler için: bellekte, şifresiz. */
        fun bellekte(context: Context): TibiVeritabani =
            Room.inMemoryDatabaseBuilder(context, TibiVeritabani::class.java).build()
    }
}
