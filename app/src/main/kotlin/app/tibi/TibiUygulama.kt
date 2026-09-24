package app.tibi

import android.app.Application
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani

class TibiUygulama : Application() {
    val veritabani: TibiVeritabani by lazy { TibiVeritabani.olustur(this) }
    val kayitServisi: KayitServisi by lazy { KayitServisi(veritabani) }
}
