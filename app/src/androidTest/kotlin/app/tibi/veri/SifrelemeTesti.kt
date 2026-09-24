package app.tibi.veri

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class SifrelemeTesti {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val ad = "sifreleme-testi.db"

    @Before fun temizle() { ctx.deleteDatabase(ad) }

    @Test
    fun dosyaSifreliVeAyniAnahtarlaAcilir() = runTest {
        val parola = VeritabaniAnahtari(ctx).parola()
        TibiVeritabani.olustur(ctx, ad, parola).let { db ->
            db.hesapDao().ekle(Hesap(ad = "Gizli hesap", tur = HesapTuru.NAKIT, acilisTarihi = LocalDate.parse("2026-09-24")))
            db.close()
        }
        val basik = ByteArray(16).also { b -> ctx.getDatabasePath(ad).inputStream().use { it.read(b) } }
        assertFalse(basik.contentEquals("SQLite format 3\u0000".toByteArray()), "Dosya düz SQLite, şifreli değil")
        val icerik = ctx.getDatabasePath(ad).readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(icerik.contains("Gizli hesap"), "Hesap adı dosyada okunabiliyor")

        TibiVeritabani.olustur(ctx, ad, parola).let { db ->
            assertEquals("Gizli hesap", db.hesapDao().getir(1)!!.ad)
            db.close()
        }
    }

    @Test
    fun yanlisAnahtarlaAcilmaz() = runTest {
        TibiVeritabani.olustur(ctx, ad, VeritabaniAnahtari(ctx).parola()).let { db ->
            db.hesapDao().ekle(Hesap(ad = "x", tur = HesapTuru.NAKIT, acilisTarihi = LocalDate.parse("2026-09-24")))
            db.close()
        }
        val yanlis = TibiVeritabani.olustur(ctx, ad, ByteArray(32))
        assertFails { yanlis.hesapDao().getir(1) }
        yanlis.close()
    }

    @Test
    fun anahtarKaliciDir() {
        assertContentEquals(VeritabaniAnahtari(ctx).parola(), VeritabaniAnahtari(ctx).parola())
        assertEquals(32, VeritabaniAnahtari(ctx).parola().size)
    }
}
