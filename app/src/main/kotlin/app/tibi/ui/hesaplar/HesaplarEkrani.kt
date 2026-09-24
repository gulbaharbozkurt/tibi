package app.tibi.ui.hesaplar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.tibiVm
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KartTuru

@Composable
fun HesaplarEkrani(kartAc: (Long) -> Unit) {
    val vm = tibiVm { HesaplarVm(it.veritabani) }
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val kartlar by vm.kartlar.collectAsStateWithLifecycle(emptyList())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Hesaplar", style = MaterialTheme.typography.headlineSmall)
        Bolum("Kredi kartları") {
            if (kartlar.isEmpty()) Text("Kart yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            kartlar.forEach { k ->
                Row(Modifier.fillMaxWidth().clickable { kartAc(k.hesapId) }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${k.ad} ··${k.son4}")
                        Text(if (k.kartTuru == KartTuru.ANA) "Kesim ${k.kesimGunu} · Son ödeme ${k.sonOdemeGunu}" else "Ortak limit",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (k.kartTuru == KartTuru.ANA) Text(k.kendiLimitiKurus?.let { "${Kurus(it - k.kullanimKurus).bicimle()} kaldı" }
                        ?: "borç ${Kurus(k.kullanimKurus).bicimle()}")
                }
            }
        }
        Bolum("Banka hesapları ve nakit") {
            hesaplar.forEach { h ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(h.ad + if (h.tur == HesapTuru.NAKIT) "" else if (h.maasHesabi) " · maaş" else "")
                    Text(Kurus(h.bakiyeKurus).bicimle())
                }
            }
        }
    }
}
