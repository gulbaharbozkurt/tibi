package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tibi.ui.Sonuc
import app.tibi.ui.tibiVm
import kotlinx.coroutines.launch

@Composable
fun KurulumAkisi(bitti: () -> Unit) {
    val vm = tibiVm { KurulumVm(it.veritabani, it.kayitServisi) }
    val kapsam = rememberCoroutineScope()
    var adim by rememberSaveable { mutableStateOf(KurulumAdimi.HOS_GELDIN) }
    var hitap by rememberSaveable { mutableStateOf("") }
    var gelir by remember { mutableStateOf(GelirFormu()) }
    var hesapFormu by remember { mutableStateOf(HesapFormu()) }
    var kartFormu by remember { mutableStateOf<KartGirdisi?>(null) }
    var taksitFormu by remember { mutableStateOf(TaksitGirdisi()) }
    var hata by rememberSaveable { mutableStateOf<String?>(null) }
    var calisiyor by remember { mutableStateOf(false) }
    val adimlar = KurulumAdimi.entries
    val sira = adimlar.indexOf(adim)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp)) {
            Text("Kurulum ${sira + 1} / ${adimlar.size}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            Text(adim.baslik, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 6.dp))
            LinearProgressIndicator(progress = { (sira + 1f) / adimlar.size }, modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                when (adim) {
                    KurulumAdimi.HOS_GELDIN -> HosGeldinEkrani(hitap) { hitap = it }
                    KurulumAdimi.HESAPLAR -> HesaplarAdimi(vm, hesapFormu) { hesapFormu = it }
                    KurulumAdimi.GELIR -> GelirAdimi(vm, gelir) { gelir = it }
                    KurulumAdimi.KARTLAR -> KartlarAdimi(vm, kartFormu) { kartFormu = it }
                    KurulumAdimi.TAKSITLER -> TaksitlerAdimi(vm, taksitFormu) { taksitFormu = it }
                }
                hata?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                if (adim == KurulumAdimi.GELIR) {
                    TextButton({ hata = null; adim = adimlar[sira + 1] }, Modifier.padding(top = 8.dp)) { Text("Şimdilik geç") }
                    Text("Maaş girilmezse \"bu dönem\" takvim ayına göre sayılır.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sira > 0) OutlinedButton({ hata = null; adim = adimlar[sira - 1] }, Modifier.weight(1f)) { Text("Geri") }
                Button(enabled = !calisiyor, onClick = {
                    calisiyor = true
                    kapsam.launch {
                        // Doldurulup "ekle/kaydet"e basılmamış form kaybolmasın: adımın kendi butonuyla aynı çağrı.
                        val s = when (adim) {
                            KurulumAdimi.HOS_GELDIN -> vm.hitapKaydet(hitap)
                            KurulumAdimi.HESAPLAR -> {
                                val f = hesapFormu
                                if (!f.doluMu()) Sonuc.Tamam
                                else f.kaydet(vm).also { if (it == Sonuc.Tamam) hesapFormu = HesapFormu(maas = false) }
                            }
                            KurulumAdimi.GELIR -> vm.maasKaydet(gelir.tutar, gelir.gun, gelir.haftaSonu, gelir.hesapId)
                            KurulumAdimi.KARTLAR -> {
                                val f = kartFormu
                                if (f == null || !f.doluMu()) Sonuc.Tamam.also { kartFormu = null }
                                else vm.kartEkle(f).also { if (it == Sonuc.Tamam) kartFormu = null }
                            }
                            KurulumAdimi.TAKSITLER -> {
                                val f = taksitFormu
                                if (!f.doluMu()) Sonuc.Tamam
                                else vm.taksitEkle(f).also { if (it == Sonuc.Tamam) taksitFormu = TaksitGirdisi(kartId = f.kartId) }
                            }
                        }
                        when {
                            s is Sonuc.Hata -> hata = s.mesaj
                            sira == adimlar.lastIndex -> { vm.bitir(); bitti() }
                            else -> { hata = null; adim = adimlar[sira + 1] }
                        }
                        calisiyor = false
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text(if (sira == adimlar.lastIndex) "Kurulumu bitir" else "Devam")
                }
            }
        }
    }
}
