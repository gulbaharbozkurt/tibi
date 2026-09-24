package app.tibi.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Acik = lightColorScheme(
    primary = Color(0xFF1F5A50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEBE6),
    onPrimaryContainer = Color(0xFF1F5A50),
    background = Color(0xFFEEF1EC),
    onBackground = Color(0xFF18231F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18231F),
    onSurfaceVariant = Color(0xFF5E6B65),
    outline = Color(0xFFD6DDD7),
    error = Color(0xFFB4531A),
)

private val Koyu = darkColorScheme(
    primary = Color(0xFF6FC2AE),
    onPrimary = Color(0xFF121816),
    primaryContainer = Color(0xFF1F3631),
    onPrimaryContainer = Color(0xFF6FC2AE),
    background = Color(0xFF121816),
    onBackground = Color(0xFFE6ECE8),
    surface = Color(0xFF1B2320),
    onSurface = Color(0xFFE6ECE8),
    onSurfaceVariant = Color(0xFF9AA8A1),
    outline = Color(0xFF2D3834),
    error = Color(0xFFF08A4B),
)

@Composable
fun TibiTema(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Koyu else Acik, content = content)
}
