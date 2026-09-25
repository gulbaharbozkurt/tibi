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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.Sonuc
import app.tibi.ui.ortak.Secici
import app.tibi.ui.ortak.TutarAlani
import app.tibi.ui.ortak.kisa
import app.tibi.ui.ortak.utcMillis
import app.tibi.ui.ortak.utcTarih
import app.tibi.ui.tibiVm
import app.tibi.veri.dao.KalemOnerisi
import app.tibi.veri.kalemAnahtari
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val bugun = remember { vm.bugun() }
    var tarih by remember { mutableStateOf(bugun) }
    var takvimAcik by remember { mutableStateOf(false) }
    var aciklama by remember { mutableStateOf("") }
    var kalem by remember { mutableStateOf("") }
    var oneriler by remember { mutableStateOf(emptyList<KalemOnerisi>()) }
    LaunchedEffect(tur, kalem) { oneriler = if (tur == GirisTuru.HARCAMA) vm.oneriler(kalem) else emptyList() }
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
                    // Dört bölüm telefon genişliğine sığsın diye seçim işareti gösterilmez, etiket tek satır kalır.
                    SegmentedButton(tur == t, { tur = t; kategoriId = null; hata = null }, SegmentedButtonDefaults.itemShape(i, GirisTuru.entries.size), icon = {}) {
                        Text(when (t) {
                            GirisTuru.HARCAMA -> "Harcama"; GirisTuru.GELIR -> "Gelir"
                            GirisTuru.TRANSFER -> "Transfer"; GirisTuru.AVANS -> "Avans"
                        }, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (tur == GirisTuru.HARCAMA) {
                OutlinedTextField(kalem, { kalem = it }, label = { Text("Ne aldın? (ör. Probis)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (oneriler.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val anahtar = kalemAnahtari(kalem)
                    oneriler.forEach { o ->
                        FilterChip(anahtar == o.kalemAnahtar, {
                            kalem = o.kalem
                            o.kategoriId?.let { kategoriId = it }
                            if (tutar.isBlank()) tutar = Kurus(o.sonTutarKurus).bicimle().removeSuffix(" ₺")
                        }, label = { Text(o.kalem) })
                    }
                }
            }
            TutarAlani(tutar, { tutar = it }, "Tutar", buyuk = true)
            if (tur == GirisTuru.HARCAMA || tur == GirisTuru.GELIR) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                kategoriler.forEach { k -> FilterChip(kategoriId == k.id, { kategoriId = if (kategoriId == k.id) null else k.id }, label = { Text(k.ad) }) }
            }
            if (tur == GirisTuru.AVANS) Text("Avans gelir sayılmaz; bir sonraki maaştan düşülür.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val hesapEtiketi = when (tur) {
                GirisTuru.TRANSFER -> "Nereden"; GirisTuru.HARCAMA -> "Hesap / kart"
                GirisTuru.GELIR -> "Hesap"; GirisTuru.AVANS -> "Yattığı hesap"
            }
            Secici(hesapEtiketi, hesapListesi, secili, { it.ad }, { hesapId = it.id })
            if (tur == GirisTuru.TRANSFER) {
                val hedefler = hesaplar.filter { it.id != hesapId }
                Secici("Nereye (kart seçersen kart ödemesi olur)", hedefler, hedefler.firstOrNull { it.id == hedefId }, { it.ad }, { hedefId = it.id })
            }
            if (tur == GirisTuru.HARCAMA && secili?.kart == true) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Secici("Taksit", TAKSITLER, taksit, { if (it == 1) "Tek çekim" else "$it taksit" }, { taksit = it }, Modifier.weight(1f))
                Secici("Erteleme", listOf(0, 1, 2, 3), erteleme, { if (it == 0) "Yok" else "$it ay" }, { erteleme = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dun = bugun.minusDays(1)
                val ozel = tarih != bugun && tarih != dun
                FilterChip(tarih == bugun, { tarih = bugun }, label = { Text("Bugün · ${bugun.kisa()}") })
                FilterChip(tarih == dun, { tarih = dun }, label = { Text("Dün") })
                FilterChip(ozel, { takvimAcik = true }, label = { Text(if (ozel) tarih.kisa() else "Tarih seç") })
            }
            OutlinedTextField(aciklama, { aciklama = it }, label = { Text("Not (isteğe bağlı)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                kapsam.launch {
                    when (val s = vm.kaydet(tur, tutar, hesapId, hedefId, kategoriId, taksit, erteleme, tarih, aciklama, kalem)) {
                        Sonuc.Tamam -> { durum.hide(); kapat(true) }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Kaydet") }
        }
    }
    if (takvimAcik) TarihSecici(tarih, bugun, { tarih = it; takvimAcik = false }, { takvimAcik = false })
}

/** Takvimden geçmiş bir gün seçtirir; bugünden sonrası seçilemez. DatePicker UTC gece yarısıyla çalışır. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarihSecici(secili: LocalDate, bugun: LocalDate, sec: (LocalDate) -> Unit, vazgec: () -> Unit) {
    val sinir = bugun.utcMillis()
    val durum = rememberDatePickerState(
        initialSelectedDateMillis = secili.utcMillis(),
        yearRange = 2000..bugun.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= sinir
            override fun isSelectableYear(year: Int) = year <= bugun.year
        },
    )
    DatePickerDialog(
        onDismissRequest = vazgec,
        confirmButton = {
            TextButton({ durum.selectedDateMillis?.let { sec(it.utcTarih()) } ?: vazgec() }) { Text("Tamam") }
        },
        dismissButton = { TextButton(vazgec) { Text("Vazgeç") } },
    ) {
        DatePicker(durum, title = { Text("Tarih seç", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) })
    }
}
