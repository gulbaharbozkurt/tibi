package app.tibi.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Logodaki turuncu; yalnızca marka öğelerinde (simge, açılış). Metin taşıyan yüzeylerde koyu tonu kullanılır. */
val LogoTuruncu = Color(0xFFF56A05)

// Açık temada beyaz yazının okunabilmesi için butonlarda logodan koyu bir ton (#B84E00, ~4.9:1).
// Uyarılar turuncudan ayrışsın diye kırmızı.
private val Acik = lightColorScheme(
    primary = Color(0xFFB84E00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE2CC),
    onPrimaryContainer = Color(0xFF6B2C00),
    secondaryContainer = Color(0xFFFFE2CC),
    onSecondaryContainer = Color(0xFF6B2C00),
    background = Color(0xFFF6F2EE),
    onBackground = Color(0xFF1F1A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1A17),
    surfaceContainer = Color(0xFFFFFBF8),
    onSurfaceVariant = Color(0xFF6B625C),
    outline = Color(0xFFE2DAD3),
    error = Color(0xFFC62828),
)

private val Koyu = darkColorScheme(
    primary = Color(0xFFFF9A4D),
    onPrimary = Color(0xFF3A1800),
    primaryContainer = Color(0xFF4A2410),
    onPrimaryContainer = Color(0xFFFFDCC4),
    secondaryContainer = Color(0xFF4A2410),
    onSecondaryContainer = Color(0xFFFFDCC4),
    background = Color(0xFF15110F),
    onBackground = Color(0xFFF1EBE6),
    surface = Color(0xFF201A17),
    onSurface = Color(0xFFF1EBE6),
    surfaceContainer = Color(0xFF201A17),
    onSurfaceVariant = Color(0xFFA89C94),
    outline = Color(0xFF3A302A),
    error = Color(0xFFFF8A80),
)

@Composable
fun TibiTema(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Koyu else Acik, content = content)
}
