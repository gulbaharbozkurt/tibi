package app.tibi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.TibiUygulama
import app.tibi.ui.kurulum.KurulumAkisi
import app.tibi.veri.Anahtarlar

private object Yukleniyor

@Composable
fun KokEkran() {
    val uygulama = LocalContext.current.applicationContext as TibiUygulama
    val akis = remember { uygulama.veritabani.ayarDao().okuAkis(Anahtarlar.KURULUM_TAMAM) }
    val durum by akis.collectAsStateWithLifecycle(initialValue = Yukleniyor)
    when (durum) {
        Yukleniyor -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        "1" -> Kabuk()
        else -> KurulumAkisi(bitti = {})   // ayar "1" olunca akış kendiliğinden Kabuk'a geçer
    }
}
