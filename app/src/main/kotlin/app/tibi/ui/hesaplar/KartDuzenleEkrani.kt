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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import app.tibi.ui.Sonuc
import app.tibi.ui.kurulum.KartFormu
import app.tibi.ui.kurulum.KartGirdisi
import app.tibi.ui.tibiVm
import app.tibi.veri.tablo.KartTuru
import kotlinx.coroutines.launch

/** kapatildi: kart kapatılınca çağrılır (detay ekranı da artık boş olduğu için Hesaplar'a dönülür). */
@Composable
fun KartDuzenleEkrani(kartId: Long, geri: () -> Unit, kapatildi: () -> Unit = geri) {
    val vm = tibiVm { KartDuzenleVm(it.veritabani, it.kayitServisi, kartId) }
    val ilk by vm.girdi.collectAsStateWithLifecycle(null)
    val bankalar by vm.bankalar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var form by remember { mutableStateOf<KartGirdisi?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    var kapatSor by remember { mutableStateOf(false) }

    LaunchedEffect(ilk) { if (form == null) form = ilk }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text("Kartı düzenle", style = MaterialTheme.typography.headlineSmall)
        }
        val f = form ?: return
        KartFormu(f, emptyList(), bankalar, duzenleme = true) { form = it }
        hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            kapsam.launch {
                when (val s = vm.kaydet(f)) {
                    Sonuc.Tamam -> geri()
                    is Sonuc.Hata -> hata = s.mesaj
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
        TextButton({ kapatSor = true }, Modifier.fillMaxWidth()) { Text("Kartı kapat", color = MaterialTheme.colorScheme.error) }
    }
    if (kapatSor) {
        AlertDialog(
            onDismissRequest = { kapatSor = false },
            title = { Text("Kart kapatılsın mı?") },
            text = {
                Text("Kart listelerden kalkar; geçmiş harcamaları ve taksitleri durur." +
                    if (form?.tur == KartTuru.ANA) "\nBu karta bağlı ek ve sanal kartlar da kapanır." else "")
            },
            confirmButton = {
                TextButton({
                    kapatSor = false
                    kapsam.launch {
                        when (val s = vm.kapat()) {
                            Sonuc.Tamam -> kapatildi()
                            is Sonuc.Hata -> hata = s.mesaj
                        }
                    }
                }) { Text("Kartı kapat") }
            },
            dismissButton = { TextButton({ kapatSor = false }) { Text("Vazgeç") } },
        )
    }
}
