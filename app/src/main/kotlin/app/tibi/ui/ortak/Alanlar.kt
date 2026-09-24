package app.tibi.ui.ortak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tibi.core.para.kurusCoz

/** Tutar girişi; geçersizse altında nasıl yazılacağını söyler. Boş alan hata sayılmaz. */
@Composable
fun TutarAlani(metin: String, degisti: (String) -> Unit, etiket: String, modifier: Modifier = Modifier, buyuk: Boolean = false) {
    val hatali = metin.isNotBlank() && kurusCoz(metin) == null
    OutlinedTextField(
        value = metin,
        onValueChange = { yeni -> degisti(yeni.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(etiket) },
        suffix = { Text("₺") },
        singleLine = true,
        isError = hatali,
        supportingText = if (hatali) ({ Text("Tutarı 1.249,90 biçiminde yaz") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = if (buyuk) MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.End) else MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SayiAlani(metin: String, degisti: (String) -> Unit, etiket: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = metin,
        onValueChange = { yeni -> degisti(yeni.filter(Char::isDigit).take(4)) },
        label = { Text(etiket) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Secici(
    etiket: String,
    secenekler: List<T>,
    secili: T?,
    ad: (T) -> String,
    secildi: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var acik by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = acik, onExpandedChange = { acik = it }, modifier = modifier) {
        OutlinedTextField(
            value = secili?.let(ad) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(etiket) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = acik) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = acik, onDismissRequest = { acik = false }) {
            secenekler.forEach { s ->
                DropdownMenuItem(text = { Text(ad(s)) }, onClick = { secildi(s); acik = false })
            }
        }
    }
}

/** Başlıklı beyaz blok (taslaktaki .blk). */
@Composable
fun Bolum(baslik: String, modifier: Modifier = Modifier, icerik: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(baslik.uppercase(TR), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            icerik()
        }
    }
}
