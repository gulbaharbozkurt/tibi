package app.tibi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tibi.core.donem.DonemHesaplayici
import app.tibi.core.donem.MaasKurali
import app.tibi.core.tarih.HaftaSonuKurali
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Sekme(val rota: String, val baslik: String, val ikon: ImageVector) {
    OZET("ozet", "Özet", Icons.Filled.Home),
    HAREKETLER("hareketler", "Hareketler", Icons.AutoMirrored.Filled.List),
    HESAPLAR("hesaplar", "Hesaplar", Icons.Filled.AccountBox),
    CUZDANLAR("cuzdanlar", "Cüzdanlar", Icons.Filled.ShoppingCart),
    DERSLER("dersler", "Dersler", Icons.Filled.DateRange),
}

@Composable
fun Kabuk() {
    val nav = rememberNavController()
    val giris by nav.currentBackStackEntryAsState()
    val aktif = giris?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                Sekme.entries.forEach { s ->
                    NavigationBarItem(
                        selected = aktif == s.rota,
                        onClick = {
                            nav.navigate(s.rota) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(s.ikon, contentDescription = null) },
                        label = { Text(s.baslik) },
                    )
                }
            }
        },
    ) { ic ->
        NavHost(nav, startDestination = Sekme.OZET.rota, modifier = Modifier.padding(ic)) {
            composable(Sekme.OZET.rota) { OzetYerTutucu() }
            Sekme.entries.drop(1).forEach { s -> composable(s.rota) { YerTutucu(s.baslik) } }
        }
    }
}

@Composable
private fun OzetYerTutucu() {
    val d = DonemHesaplayici.donem(MaasKurali(30, HaftaSonuKurali.ONCEKI), LocalDate.now())
    val f = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("tr"))
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Özet", style = MaterialTheme.typography.headlineSmall)
        Text("Bu dönem: ${d.baslangic.format(f)} – ${d.bitis.format(f)} (örnek maaş günü: 30)")
    }
}

@Composable
private fun YerTutucu(baslik: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(baslik, style = MaterialTheme.typography.headlineSmall)
        Text("Bu ekran sonraki planlarda gelecek.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
