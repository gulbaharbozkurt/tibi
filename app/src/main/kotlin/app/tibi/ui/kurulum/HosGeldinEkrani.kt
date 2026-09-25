package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.tibi.R
import androidx.compose.foundation.Image

@Composable
fun HosGeldinEkrani(hitap: String, degisti: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(120.dp))
        Text("tibi her şeyi bu telefonda tutar, hiçbir yere göndermez.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = hitap, onValueChange = degisti, singleLine = true,
            label = { Text("Sana nasıl hitap edelim?") },
            supportingText = { Text("Bildirimlerde kullanılır: \"${hitap.trim().ifEmpty { "Adın" }}, Bonus'un son ödemesine 3 gün var.\"") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
