package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.Sonuc
import app.tibi.ui.ortak.Bolum
import app.tibi.veri.tablo.KartTuru
import kotlinx.coroutines.launch

@Composable
fun KartlarAdimi(vm: KurulumVm) {
    val kartlar by vm.kartlar.collectAsStateWithLifecycle(emptyList())
    val bankalar by vm.bankalar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var form by remember { mutableStateOf<KartGirdisi?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("İstediğin kadar kart ekle; sonra da değiştirebilirsin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (kartlar.isNotEmpty()) Bolum("Eklenen kartlar") {
            kartlar.forEach { k ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(kartEtiketi(k) + if (k.kartTuru != KartTuru.ANA) " (ortak limit)" else "")
                    Text(k.kendiLimitiKurus?.let { Kurus(it).bicimle() } ?: "")
                }
            }
        }
        val f = form
        if (f == null) {
            OutlinedButton({ form = KartGirdisi(); hata = null }, Modifier.fillMaxWidth()) { Text("+ Kart ekle") }
        } else {
            KartFormu(f, kartlar.filter { it.kartTuru == KartTuru.ANA }, bankalar) { form = it }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ form = null; hata = null }, Modifier.weight(1f)) { Text("Vazgeç") }
                Button({
                    kapsam.launch {
                        when (val s = vm.kartEkle(f)) {
                            Sonuc.Tamam -> { form = null; hata = null }
                            is Sonuc.Hata -> hata = s.mesaj
                        }
                    }
                }, Modifier.weight(1f)) { Text("Kartı kaydet") }
            }
        }
    }
}
