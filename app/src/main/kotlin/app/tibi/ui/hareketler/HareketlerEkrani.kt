package app.tibi.ui.hareketler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.tibi.ui.ortak.aralik
import app.tibi.ui.ortak.gunBasligi
import app.tibi.ui.tibiVm
import app.tibi.veri.dao.HareketSatiri
import app.tibi.veri.kalemAnahtari
import app.tibi.veri.tablo.HareketTuru
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FILTRE_ADI = mapOf(
    HareketFiltresi.TUMU to "Tümü", HareketFiltresi.HARCAMA to "Harcama",
    HareketFiltresi.GELIR to "Gelir", HareketFiltresi.TRANSFER to "Transfer",
)

@Composable
fun HareketlerEkrani(kalemAc: (kalemAnahtar: String) -> Unit = {}) {
    val vm = tibiVm { HareketlerVm(it.veritabani, it.donemServisi) }
    val gruplar by vm.gruplar.collectAsStateWithLifecycle(emptyList())
    val donem by vm.donem.collectAsStateWithLifecycle(null)
    val filtre by vm.filtre.collectAsStateWithLifecycle()
    val arama by vm.arama.collectAsStateWithLifecycle()
    val kaydirma by vm.kaydirma.collectAsStateWithLifecycle()
    var silinecek by remember { mutableStateOf<HareketSatiri?>(null) }
    val kapsam = rememberCoroutineScope()
    val bugun = remember { LocalDate.now() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Hareketler", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        donem?.let { d ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.onceki() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki dönem") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.aralik(), style = MaterialTheme.typography.titleMedium)
                        if (kaydirma == 0) Text("Bu dönem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("${gruplar.sumOf { g -> g.satirlar.size }} kayıt", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton({ vm.sonraki() }, enabled = kaydirma < 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki dönem")
                }
            }
        }
        OutlinedTextField(arama, { vm.arama.value = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Ara: not, kategori, hesap, tutar") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            HareketFiltresi.entries.forEach { f -> FilterChip(filtre == f, { vm.filtre.value = f }, label = { Text(FILTRE_ADI.getValue(f)) }) }
        }
        if (gruplar.isEmpty()) Text(if (kaydirma == 0) "Bu dönemde kayıt yok. \"+\" ile ekleyebilirsin." else "Bu dönemde kayıt yok.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            gruplar.forEach { g ->
                item(key = "g${g.tarih}") {
                    Text(g.tarih.gunBasligi(bugun).uppercase(app.tibi.ui.ortak.TR), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                }
                items(g.satirlar, key = { it.id }) { s ->
                    HareketSatirKutusu(s, ac = s.kalem?.let(::kalemAnahtari)?.let { a -> { kalemAc(a) } }) { silinecek = s }
                }
            }
        }
    }

    silinecek?.let { s ->
        AlertDialog(
            onDismissRequest = { silinecek = null },
            title = { Text("Kayıt silinsin mi?") },
            text = { Text("${satirBasligi(s)} · ${Kurus(s.tutarKurus).bicimle()}" + if (s.taksitSayisi > 1) "\nBütün taksitleri de silinir." else "") },
            confirmButton = { TextButton({ kapsam.launch { vm.sil(s.id) }; silinecek = null }) { Text("Sil") } },
            dismissButton = { TextButton({ silinecek = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
/** [ac] doluysa (kalemli satır) satıra dokunmak kalem özetini açar; çöp kutusu kendi tıklamasını alır. */
private fun HareketSatirKutusu(s: HareketSatiri, ac: (() -> Unit)?, sil: () -> Unit) {
    val duzeltme = s.tur == HareketTuru.DUZELTME
    val giris = s.tur in setOf(HareketTuru.GELIR, HareketTuru.TAHSILAT, HareketTuru.AVANS) || (duzeltme && s.hedefAdi != null)
    val cikis = s.tur == HareketTuru.HARCAMA || (duzeltme && s.kaynakAdi != null)
    val alt = satirAltMetni(s)
    Row(Modifier.fillMaxWidth().then(if (ac != null) Modifier.clickable(onClick = ac) else Modifier).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(satirBasligi(s))
            if (alt.isNotEmpty()) Text(alt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text((if (giris) "+" else if (cikis) "−" else "") + Kurus(s.tutarKurus).bicimle(),
            color = if (giris) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        IconButton(sil) { Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
