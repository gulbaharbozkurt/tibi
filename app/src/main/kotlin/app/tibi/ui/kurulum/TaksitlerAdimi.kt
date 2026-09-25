package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.kart.PlanliTaksit
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.core.para.topla
import app.tibi.ui.Sonuc
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.SayiAlani
import app.tibi.ui.ortak.Secici
import app.tibi.ui.ortak.TR
import app.tibi.ui.ortak.TutarAlani
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val AY_YIL = DateTimeFormatter.ofPattern("MMMM yyyy", TR)

@Composable
fun TaksitlerAdimi(vm: KurulumVm, g: TaksitGirdisi, degisti: (TaksitGirdisi) -> Unit) {
    val kartListesi by vm.kartlar.collectAsStateWithLifecycle(null)
    val kartlar = kartListesi.orEmpty()
    val taksitler by vm.taksitler.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var plan by remember { mutableStateOf<List<PlanliTaksit>?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(g) { plan = vm.planOnizleme(g) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Geçmişte yapılmış, hâlâ ekstreye yansıyan taksitler. Yoksa bu adımı geç.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (taksitler.isNotEmpty()) Bolum("Eklenenler") {
            taksitler.forEach { t ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${t.aciklama ?: "Taksit"} · ${t.siradakiSira}/${t.toplam}")
                    Text(Kurus(t.kalanKurus).bicimle())
                }
            }
        }
        Bolum("Yeni taksit") {
            if (kartListesi != null && kartlar.isEmpty()) Text("Önce bir kart ekle ya da bu adımı geç.", color = MaterialTheme.colorScheme.error)
            else if (kartlar.isNotEmpty()) Secici("Kart", kartlar, kartlar.firstOrNull { it.hesapId == g.kartId }, { kartEtiketi(it) }, { degisti(g.copy(kartId = it.hesapId)) })
            OutlinedTextField(g.aciklama, { degisti(g.copy(aciklama = it)) }, label = { Text("Ne? (ör. Telefon)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(TaksitTuru.ALISVERIS to "Alışveriş", TaksitTuru.EKSTRE_TAKSIT to "Ekstre taksit", TaksitTuru.NAKIT_AVANS to "Nakit avans").forEach { (t, ad) ->
                    FilterChip(g.tur == t, { degisti(g.copy(tur = t)) }, label = { Text(ad) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(TaksitModu.AYLIK to "Aylık tutar", TaksitModu.TOPLAM to "Toplam tutar", TaksitModu.KALAN to "Kalan borç").forEach { (m, ad) ->
                    FilterChip(g.mod == m, { degisti(g.copy(mod = m)) }, label = { Text(ad) })
                }
            }
            TutarAlani(g.tutar, { degisti(g.copy(tutar = it)) }, when (g.mod) { TaksitModu.AYLIK -> "Aylık taksit"; TaksitModu.TOPLAM -> "Toplam tutar"; TaksitModu.KALAN -> "Kalan borç" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SayiAlani(g.sayi, { degisti(g.copy(sayi = it)) }, if (g.mod == TaksitModu.KALAN) "Kalan taksit" else "Toplam taksit", Modifier.weight(1f))
                if (g.mod != TaksitModu.KALAN) SayiAlani(g.siradaki, { degisti(g.copy(siradaki = it)) }, "Sıradaki kaçıncı?", Modifier.weight(1f))
            }
            plan?.let { p ->
                val kalan = p.filterNot { it.oncedenOdendi }
                Text("Kalan ${kalan.size} taksit · ${kalan.map { it.tutar }.topla().bicimle()} · son taksit ${p.last().ekstreKesimTarihi.format(AY_YIL)}",
                    color = MaterialTheme.colorScheme.primary)
            }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button({
                val f = g
                kapsam.launch {
                    when (val s = vm.taksitEkle(f)) {
                        Sonuc.Tamam -> { degisti(TaksitGirdisi(kartId = f.kartId)); hata = null }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, Modifier.fillMaxWidth()) { Text("Taksiti kaydet") }
        }
    }
}
