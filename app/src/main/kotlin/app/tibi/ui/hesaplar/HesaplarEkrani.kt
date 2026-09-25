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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.limitMetni
import app.tibi.ui.tibiVm
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KartTuru

@Composable
fun HesaplarEkrani(kartAc: (Long) -> Unit, hesapAc: (Long) -> Unit) {
    val vm = tibiVm { HesaplarVm(it.veritabani) }
    val d by vm.durum.collectAsStateWithLifecycle(null)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Hesaplar", style = MaterialTheme.typography.headlineSmall)
        val durum = d ?: return@Column
        if (durum.bankalar.isEmpty() && durum.nakit.isEmpty() && durum.bankasizKartlar.isEmpty()) {
            Text("Hesap ya da kart yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        durum.bankalar.forEach { g ->
            Bolum(g.banka.ad) {
                g.hesaplar.forEach { HesapSatiri(it, hesapAc) }
                g.kartlar.forEach { KartSatiri(it, kartAc) }
            }
        }
        if (durum.nakit.isNotEmpty()) Bolum("Elde nakit") { durum.nakit.forEach { HesapSatiri(it, hesapAc) } }
        if (durum.bankasizKartlar.isNotEmpty()) Bolum("Diğer kartlar") { durum.bankasizKartlar.forEach { KartSatiri(it, kartAc) } }
    }
}

@Composable
private fun HesapSatiri(h: HesapBakiyesi, hesapAc: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { hesapAc(h.id) }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(h.ad + if (h.tur != HesapTuru.NAKIT && h.maasHesabi) " · maaş" else "")
        Text(Kurus(h.bakiyeKurus).bicimle())
    }
}

@Composable
private fun KartSatiri(k: KartBilgisi, kartAc: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { kartAc(k.hesapId) }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("${k.ad} ··${k.son4}")
            Text(if (k.kartTuru == KartTuru.ANA) "Kesim ${k.kesimGunu} · Son ödeme ${k.sonOdemeGunu}" else "Ortak limit",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (k.kartTuru == KartTuru.ANA) {
            val kalan = k.kendiLimitiKurus?.let { it - k.kullanimKurus }
            Text(kalan?.let { limitMetni(it) } ?: "borç ${Kurus(k.kullanimKurus).bicimle()}",
                color = if (kalan != null && kalan < 0) MaterialTheme.colorScheme.error else Color.Unspecified)
        }
    }
}
