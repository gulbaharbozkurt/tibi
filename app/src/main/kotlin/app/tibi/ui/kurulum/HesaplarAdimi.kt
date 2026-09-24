package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.Sonuc
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.TutarAlani
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.launch

@Composable
fun HesaplarAdimi(vm: KurulumVm) {
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var ad by remember { mutableStateOf("") }
    var tur by remember { mutableStateOf(HesapTuru.BANKA) }
    var bakiye by remember { mutableStateOf("") }
    var maas by remember { mutableStateOf(hesaplar.none { it.maasHesabi }) }
    var hata by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bugünkü bakiyeleri gir; uygulama buradan başlar. Kredi kartları sonraki adımda.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hesaplar.isNotEmpty()) Bolum("Eklenenler") {
            hesaplar.forEach { h ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(h.ad + if (h.maasHesabi) " · maaş" else "")
                    Text(Kurus(h.bakiyeKurus).bicimle())
                }
            }
        }
        Bolum("Yeni hesap") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(tur == HesapTuru.BANKA, { tur = HesapTuru.BANKA }, label = { Text("Banka hesabı") })
                FilterChip(tur == HesapTuru.NAKIT, { tur = HesapTuru.NAKIT; if (ad.isBlank()) ad = "Nakit" }, label = { Text("Nakit") })
            }
            OutlinedTextField(ad, { ad = it }, label = { Text("Ad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            TutarAlani(bakiye, { bakiye = it }, "Bugünkü bakiye")
            if (tur == HesapTuru.BANKA) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(maas, { maas = it }); Text("Maaşım bu hesaba yatıyor")
            }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                kapsam.launch {
                    when (val s = vm.hesapEkle(ad, tur, bakiye, maas && tur == HesapTuru.BANKA)) {
                        Sonuc.Tamam -> { ad = ""; bakiye = ""; maas = false; hata = null }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Hesabı ekle") }
        }
    }
}
