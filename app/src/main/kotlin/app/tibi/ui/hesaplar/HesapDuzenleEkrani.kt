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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.tibi.ui.kurulum.BankaCipleri
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.TutarAlani
import app.tibi.ui.tibiVm
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.launch

@Composable
fun HesapDuzenleEkrani(hesapId: Long, geri: () -> Unit) {
    val vm = tibiVm { HesapDuzenleVm(it.veritabani, it.kayitServisi, hesapId) }
    val d by vm.durum.collectAsStateWithLifecycle(null)
    val bankalar by vm.bankalar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var yuklendi by remember { mutableStateOf(false) }
    var ad by remember { mutableStateOf("") }
    var banka by remember { mutableStateOf("") }
    var maas by remember { mutableStateOf(false) }
    var gercek by remember { mutableStateOf("") }
    var hata by remember { mutableStateOf<String?>(null) }
    var bakiyeHata by remember { mutableStateOf<String?>(null) }
    var bilgi by remember { mutableStateOf<String?>(null) }
    var kapatSor by remember { mutableStateOf(false) }

    LaunchedEffect(d) {
        val x = d
        if (x != null && !yuklendi) { ad = x.ad; banka = x.bankaAdi.orEmpty(); maas = x.maasHesabi; yuklendi = true }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text(d?.ad ?: "", style = MaterialTheme.typography.headlineSmall)
        }
        val x = d ?: return
        if (!yuklendi) return
        val bankaHesabi = x.tur == HesapTuru.BANKA
        Bolum("Hesap") {
            OutlinedTextField(ad, { ad = it }, label = { Text("Hesap adı") },
                placeholder = { Text(if (bankaHesabi) "Vadesiz hesap" else "Elde nakit") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (bankaHesabi) {
                OutlinedTextField(banka, { banka = it }, label = { Text("Banka") }, placeholder = { Text("Garanti BBVA") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                BankaCipleri(bankalar, banka) { banka = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(maas, { maas = it }); Text("Maaşım bu hesaba yatıyor")
                }
            }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                kapsam.launch {
                    when (val s = vm.kaydet(ad, banka, maas)) {
                        Sonuc.Tamam -> geri()
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
        }
        Bolum("Bakiye") {
            Text(Kurus(x.bakiyeKurus).bicimle(), style = MaterialTheme.typography.headlineMedium)
            TutarAlani(gercek, { gercek = it; bilgi = null }, "Gerçek bakiye")
            Text("Fark bir düzeltme kaydı olarak eklenir; geçmiş kayıtlar değişmez.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            bakiyeHata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            bilgi?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            OutlinedButton(onClick = {
                kapsam.launch {
                    when (val s = vm.bakiyeDuzelt(gercek)) {
                        Sonuc.Tamam -> { gercek = ""; bakiyeHata = null; bilgi = "Bakiye güncellendi." }
                        is Sonuc.Hata -> bakiyeHata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Bakiyeyi düzelt") }
        }
        TextButton({ kapatSor = true }, Modifier.fillMaxWidth()) { Text("Hesabı kapat", color = MaterialTheme.colorScheme.error) }
    }
    if (kapatSor) {
        AlertDialog(
            onDismissRequest = { kapatSor = false },
            title = { Text("Hesap kapatılsın mı?") },
            text = { Text("Hesap listelerden kalkar; geçmiş kayıtları durur.") },
            confirmButton = {
                TextButton({
                    kapatSor = false
                    kapsam.launch {
                        when (val s = vm.kapat()) {
                            Sonuc.Tamam -> geri()
                            is Sonuc.Hata -> hata = s.mesaj
                        }
                    }
                }) { Text("Hesabı kapat") }
            },
            dismissButton = { TextButton({ kapatSor = false }) { Text("Vazgeç") } },
        )
    }
}
