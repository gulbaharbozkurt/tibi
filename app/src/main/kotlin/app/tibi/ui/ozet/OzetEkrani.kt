package app.tibi.ui.ozet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.aralik
import app.tibi.ui.ortak.hesapEtiketi
import app.tibi.ui.ortak.kisa
import app.tibi.ui.ortak.limitMetni
import app.tibi.ui.tibiVm

@Composable
fun OzetEkrani() {
    val vm = tibiVm { OzetVm(it.veritabani, it.donemServisi) }
    val d by vm.durum.collectAsStateWithLifecycle(initialValue = null)
    val o = d ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(o.hitap?.let { "Merhaba $it" } ?: "Özet", style = MaterialTheme.typography.headlineSmall)
        Text("Bu dönem · ${o.donem.aralik()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bolum("Bu dönem kalan") {
            Text(Kurus(o.kalanKurus).bicimle(), style = MaterialTheme.typography.headlineMedium,
                color = if (o.kalanKurus < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Satir("Gelir", Kurus(o.gelirKurus).bicimle())
            Satir("Gider", Kurus(o.giderKurus).bicimle())
            if (o.dusulecekAvansKurus > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Maaştan düşülecek avans")
                    Text("−" + Kurus(o.dusulecekAvansKurus).bicimle(), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                }
                Text("Bir sonraki maaşın bu kadar az yatar.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (o.kartlar.isNotEmpty()) Bolum("Kartlar · kendi limitine göre") {
            o.kartlar.forEach { k ->
                val limit = k.kendiLimitiKurus
                if (limit != null && limit > 0) {
                    val kalan = limit - k.kullanimKurus
                    Satir(k.ad, limitMetni(kalan), if (kalan < 0) MaterialTheme.colorScheme.error else Color.Unspecified)
                    val oran = (k.kullanimKurus.toFloat() / limit).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { oran }, modifier = Modifier.fillMaxWidth(),
                        color = if (oran >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                } else {
                    Satir(k.ad, "borç ${Kurus(k.kullanimKurus).bicimle()}")
                }
            }
        }
        if (o.taksitYuku.isNotEmpty()) Bolum("Taksit yükü · önümüzdeki ekstreler") {
            o.taksitYuku.forEach { Satir(it.ekstreKesimTarihi.kisa(), Kurus(it.toplamKurus).bicimle()) }
        }
        if (o.hesaplar.isNotEmpty()) Bolum("Hesaplar") {
            o.hesaplar.forEach { Satir(hesapEtiketi(it.bankaAdi, it.ad), Kurus(it.bakiyeKurus).bicimle()) }
        }
    }
}

@Composable
internal fun Satir(sol: String, sag: String, renk: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(sol); Text(sag, style = MaterialTheme.typography.bodyLarge, color = renk)
    }
}
