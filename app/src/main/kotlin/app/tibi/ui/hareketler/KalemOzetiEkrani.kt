package app.tibi.ui.hareketler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.ui.tibiVm

@Composable
fun KalemOzetiEkrani(kalemAnahtar: String, geri: () -> Unit) {
    val vm = tibiVm { KalemOzetiVm(it.veritabani, kalemAnahtar) }
    val o by vm.ozet.collectAsStateWithLifecycle(null)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text(o?.baslik ?: "", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        val x = o ?: return
        Text(x.son12Ay, style = MaterialTheme.typography.titleMedium)
        if (x.aylar.isEmpty()) Text("Bu kalem için kayıt yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(x.aylar, key = { it.ay.toString() }) { a ->
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(14.dp),
                ) {
                    Text(a.etiket, style = MaterialTheme.typography.titleMedium)
                    Text(a.metin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
