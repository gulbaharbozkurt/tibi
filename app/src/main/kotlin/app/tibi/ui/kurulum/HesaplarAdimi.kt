package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.launch

@Composable
fun HesaplarAdimi(vm: KurulumVm, form: HesapFormu, degisti: (HesapFormu) -> Unit) {
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val bankalar by vm.bankalar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var hata by remember { mutableStateOf<String?>(null) }
    val tur = form.tur

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bugünkü bakiyeleri gir; uygulama buradan başlar. Kredi kartları sonraki adımda.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hesaplar.isNotEmpty()) Bolum("Eklenenler") {
            hesaplar.filter { it.bankaId != null }.groupBy { it.bankaAdi.orEmpty() }.forEach { (bankaAdi, liste) ->
                Text(bankaAdi, style = MaterialTheme.typography.titleSmall)
                liste.forEach { EklenenHesap(it, Modifier.padding(start = 16.dp)) }
            }
            hesaplar.filter { it.bankaId == null }.forEach { EklenenHesap(it) }
        }
        Bolum("Yeni hesap") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(tur == HesapTuru.BANKA, { degisti(form.copy(tur = HesapTuru.BANKA)) }, label = { Text("Banka hesabı") })
                FilterChip(tur == HesapTuru.NAKIT, { degisti(form.copy(tur = HesapTuru.NAKIT)) }, label = { Text("Elde nakit") })
            }
            if (tur == HesapTuru.BANKA) {
                OutlinedTextField(form.banka, { degisti(form.copy(banka = it)) }, label = { Text("Banka") }, placeholder = { Text("Garanti BBVA") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                BankaCipleri(bankalar, form.banka) { degisti(form.copy(banka = it)) }
            }
            OutlinedTextField(form.ad, { degisti(form.copy(ad = it)) }, label = { Text("Hesap adı") },
                placeholder = { Text(if (tur == HesapTuru.BANKA) "Vadesiz hesap" else "Elde nakit") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            TutarAlani(form.bakiye, { degisti(form.copy(bakiye = it)) }, "Bugünkü bakiye")
            if (tur == HesapTuru.BANKA) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(form.maas, { degisti(form.copy(maas = it)) }); Text("Maaşım bu hesaba yatıyor")
            }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val f = form
                kapsam.launch {
                    when (val s = f.kaydet(vm)) {
                        Sonuc.Tamam -> { degisti(f.eklendiktenSonra()); hata = null }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Hesabı ekle") }
        }
    }
}

@Composable
private fun EklenenHesap(h: HesapBakiyesi, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(h.ad + if (h.maasHesabi) " · maaş" else "")
        Text(Kurus(h.bakiyeKurus).bicimle())
    }
}

/** Daha önce yazılmış bankalar; dokununca alana yazılır. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BankaCipleri(bankalar: List<Banka>, secili: String, secildi: (String) -> Unit) {
    if (bankalar.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bankalar.forEach { b ->
            FilterChip(b.ad.equals(secili.trim(), ignoreCase = true), { secildi(b.ad) }, label = { Text(b.ad) })
        }
    }
}
