package app.tibi.ui.hesaplar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.kisa
import app.tibi.ui.tibiVm

@Composable
fun KartDetayEkrani(kartId: Long, geri: () -> Unit, duzenle: () -> Unit) {
    val vm = tibiVm { KartDetayVm(it.veritabani, kartId) }
    val d by vm.detay.collectAsStateWithLifecycle(null)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text(d?.let { "${it.kart.ad} ··${it.kart.son4}" } ?: "", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (d != null) IconButton(duzenle) { Icon(Icons.Filled.Edit, contentDescription = "Düzenle") }
        }
        val x = d ?: return
        Text("Kesim ${x.ana.kesimGunu} · Son ödeme ${x.ana.sonOdemeGunu}" + if (x.kart.hesapId != x.ana.hesapId) " · ${x.ana.ad} ile ortak limit" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bolum("Kendi limitine göre") {
            Text(Kurus(x.kart.kullanimKurus).bicimle(), style = MaterialTheme.typography.headlineMedium)
            Text("toplam borç", color = MaterialTheme.colorScheme.onSurfaceVariant)
            x.ana.kendiLimitiKurus?.let { limit ->
                val oran = if (limit > 0) (x.kart.kullanimKurus.toFloat() / limit).coerceIn(0f, 1f) else 1f
                LinearProgressIndicator(progress = { oran }, modifier = Modifier.fillMaxWidth(),
                    color = if (oran >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("${Kurus(x.kalanKurus ?: 0).bicimle()} kaldı / ${Kurus(limit).bicimle()}")
            }
            x.ana.bankaLimitiKurus?.let {
                Text("Banka limiti ${Kurus(it).bicimle()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Bolum("Açık dönem · kesim ${x.siradakiKesim.kisa()}") {
            Text(Kurus(x.acikDonemKurus).bicimle(), style = MaterialTheme.typography.titleLarge)
            Text("Son ödeme ${x.sonOdeme.kisa()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (x.taksitler.isNotEmpty()) Bolum("Taksitler") {
            x.taksitler.forEach { t ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${t.aciklama ?: t.kategoriAdi ?: "Taksit"} · ${t.siradakiSira}/${t.toplam}")
                    Text("${Kurus(t.aylikKurus).bicimle()} / ay")
                }
            }
        }
        if (x.bagliKartlar.isNotEmpty()) Bolum("Aynı limiti kullanan kartlar") {
            x.bagliKartlar.forEach { Text("${it.ad} ··${it.son4}") }
        }
    }
}
