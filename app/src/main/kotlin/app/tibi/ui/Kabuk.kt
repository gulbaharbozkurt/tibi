package app.tibi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.tibi.ui.giris.HizliGirisSayfasi
import app.tibi.ui.hareketler.HareketlerEkrani
import app.tibi.ui.hesaplar.HesaplarEkrani
import app.tibi.ui.hesaplar.KartDetayEkrani
import app.tibi.ui.ozet.OzetEkrani
import kotlinx.coroutines.launch

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
    val yigin by nav.currentBackStackEntryAsState()
    val aktif = yigin?.destination?.route
    var giris by remember { mutableStateOf(false) }
    val bildirim = remember { SnackbarHostState() }
    val kapsam = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { SnackbarHost(bildirim) },
        floatingActionButton = {
            if (aktif in Sekme.entries.map { it.rota }) FloatingActionButton(onClick = { giris = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Yeni kayıt")
            }
        },
        bottomBar = {
            NavigationBar {
                Sekme.entries.forEach { s ->
                    NavigationBarItem(
                        selected = aktif == s.rota || (s == Sekme.HESAPLAR && aktif?.startsWith("kart/") == true),
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
            composable(Sekme.OZET.rota) { OzetEkrani() }
            composable(Sekme.HAREKETLER.rota) { HareketlerEkrani() }
            composable(Sekme.HESAPLAR.rota) { HesaplarEkrani(kartAc = { nav.navigate("kart/$it") }) }
            composable("kart/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { giris ->
                KartDetayEkrani(giris.arguments!!.getLong("id"), geri = { nav.popBackStack() })
            }
            listOf(Sekme.CUZDANLAR, Sekme.DERSLER).forEach { s -> composable(s.rota) { YerTutucu(s.baslik) } }
        }
    }
    if (giris) HizliGirisSayfasi { kaydedildi ->
        giris = false
        if (kaydedildi) kapsam.launch { bildirim.showSnackbar("Kaydedildi") }
    }
}

@Composable
private fun YerTutucu(baslik: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(baslik, style = MaterialTheme.typography.headlineSmall)
        Text("Bu ekran sonraki planlarda gelecek.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
