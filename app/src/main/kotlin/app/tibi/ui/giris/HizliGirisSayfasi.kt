package app.tibi.ui.giris

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.ui.Sonuc
import app.tibi.ui.ortak.Secici
import app.tibi.ui.ortak.TutarAlani
import app.tibi.ui.ortak.kisa
import app.tibi.ui.tibiVm
import kotlinx.coroutines.launch

private val TAKSITLER = listOf(1, 2, 3, 4, 5, 6, 9, 12, 18, 24)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HizliGirisSayfasi(kapat: (kaydedildi: Boolean) -> Unit) {
    val vm = tibiVm { HizliGirisVm(it.veritabani, it.kayitServisi) }
    val durum = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kapsam = rememberCoroutineScope()
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val gider by vm.giderKategorileri.collectAsStateWithLifecycle(emptyList())
    val gelir by vm.gelirKategorileri.collectAsStateWithLifecycle(emptyList())

    var tur by remember { mutableStateOf(GirisTuru.HARCAMA) }
    var tutar by remember { mutableStateOf("") }
    var hesapId by remember { mutableStateOf<Long?>(null) }
    var hedefId by remember { mutableStateOf<Long?>(null) }
    var kategoriId by remember { mutableStateOf<Long?>(null) }
    var taksit by remember { mutableIntStateOf(1) }
    var erteleme by remember { mutableIntStateOf(0) }
    var dun by remember { mutableStateOf(false) }
    var aciklama by remember { mutableStateOf("") }
    var hata by remember { mutableStateOf<String?>(null) }

    val hesapListesi = if (tur == GirisTuru.HARCAMA) hesaplar else hesaplar.filterNot { it.kart }
    val secili = hesapListesi.firstOrNull { it.id == hesapId }
    val kategoriler = if (tur == GirisTuru.GELIR) gelir else gider

    ModalBottomSheet(onDismissRequest = { kapat(false) }, sheetState = durum) {
        Column(
            Modifier.padding(horizontal = 16.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                GirisTuru.entries.forEachIndexed { i, t ->
                    SegmentedButton(tur == t, { tur = t; kategoriId = null; hata = null }, SegmentedButtonDefaults.itemShape(i, GirisTuru.entries.size)) {
                        Text(when (t) { GirisTuru.HARCAMA -> "Harcama"; GirisTuru.GELIR -> "Gelir"; GirisTuru.TRANSFER -> "Transfer" })
                    }
                }
            }
            TutarAlani(tutar, { tutar = it }, "Tutar", buyuk = true)
            if (tur != GirisTuru.TRANSFER) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                kategoriler.forEach { k -> FilterChip(kategoriId == k.id, { kategoriId = if (kategoriId == k.id) null else k.id }, label = { Text(k.ad) }) }
            }
            Secici(if (tur == GirisTuru.TRANSFER) "Nereden" else "Hesap / kart", hesapListesi, secili, { it.ad }, { hesapId = it.id })
            if (tur == GirisTuru.TRANSFER) {
                val hedefler = hesaplar.filter { it.id != hesapId }
                Secici("Nereye (kart seçersen kart ödemesi olur)", hedefler, hedefler.firstOrNull { it.id == hedefId }, { it.ad }, { hedefId = it.id })
            }
            if (tur == GirisTuru.HARCAMA && secili?.kart == true) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Secici("Taksit", TAKSITLER, taksit, { if (it == 1) "Tek çekim" else "$it taksit" }, { taksit = it }, Modifier.weight(1f))
                Secici("Erteleme", listOf(0, 1, 2, 3), erteleme, { if (it == 0) "Yok" else "$it ay" }, { erteleme = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!dun, { dun = false }, label = { Text("Bugün · ${vm.bugun().kisa()}") })
                FilterChip(dun, { dun = true }, label = { Text("Dün") })
            }
            OutlinedTextField(aciklama, { aciklama = it }, label = { Text("Not (isteğe bağlı)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                kapsam.launch {
                    val tarih = if (dun) vm.bugun().minusDays(1) else vm.bugun()
                    when (val s = vm.kaydet(tur, tutar, hesapId, hedefId, kategoriId, taksit, erteleme, tarih, aciklama)) {
                        Sonuc.Tamam -> { durum.hide(); kapat(true) }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Kaydet") }
        }
    }
}
