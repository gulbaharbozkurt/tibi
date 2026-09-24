package app.tibi.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.tibi.TibiUygulama

sealed interface Sonuc {
    data object Tamam : Sonuc
    data class Hata(val mesaj: String) : Sonuc
}

@Composable
inline fun <reified T : ViewModel> tibiVm(crossinline yap: (TibiUygulama) -> T): T {
    val uygulama = LocalContext.current.applicationContext as TibiUygulama
    return viewModel(factory = viewModelFactory { initializer { yap(uygulama) } })
}
