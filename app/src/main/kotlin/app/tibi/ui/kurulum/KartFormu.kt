package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.SayiAlani
import app.tibi.ui.ortak.Secici
import app.tibi.ui.ortak.TutarAlani
import app.tibi.ui.ortak.hesapEtiketi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.KartTuru

@Composable
/** duzenleme: tür/ana kart seçimi ve "Şu anki borç" bölümü gizlenir (bunlar kart eklenirken bir kez girilir). */
fun KartFormu(
    g: KartGirdisi,
    anaKartlar: List<KartBilgisi>,
    bankalar: List<Banka>,
    duzenleme: Boolean = false,
    degisti: (KartGirdisi) -> Unit,
) {
    Bolum("Kart") {
        OutlinedTextField(g.ad, { degisti(g.copy(ad = it)) }, label = { Text("Kart adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (g.tur == KartTuru.ANA) {
            OutlinedTextField(g.bankaAdi, { degisti(g.copy(bankaAdi = it)) }, label = { Text("Banka (isteğe bağlı)") },
                placeholder = { Text("Garanti BBVA") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            BankaCipleri(bankalar, g.bankaAdi) { degisti(g.copy(bankaAdi = it)) }
        }
        if (!duzenleme) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(KartTuru.ANA to "Ana kart", KartTuru.EK to "Ek kart", KartTuru.SANAL to "Sanal kart").forEach { (t, ad) ->
                FilterChip(g.tur == t, { degisti(g.copy(tur = t)) }, label = { Text(ad) })
            }
        }
        if (g.tur != KartTuru.ANA) {
            if (!duzenleme) Secici("Limitini paylaştığı ana kart", anaKartlar, anaKartlar.firstOrNull { it.hesapId == g.anaKartId }, { kartEtiketi(it) },
                { degisti(g.copy(anaKartId = it.hesapId)) })
            Text("Ek ve sanal kart ana kartın limitini, kesim ve son ödeme günlerini kullanır.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SayiAlani(g.son4, { degisti(g.copy(son4 = it)) }, "Son 4 hane", Modifier.fillMaxWidth())
        Text("Kart numarasının tamamı, son kullanma tarihi ve CVV istenmez.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (g.tur == KartTuru.ANA) {
        Bolum("Takvim ve limit") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SayiAlani(g.kesimGunu, { degisti(g.copy(kesimGunu = it)) }, "Kesim günü", Modifier.weight(1f))
                SayiAlani(g.sonOdemeGunu, { degisti(g.copy(sonOdemeGunu = it)) }, "Son ödeme günü", Modifier.weight(1f))
            }
            TutarAlani(g.bankaLimiti, { degisti(g.copy(bankaLimiti = it)) }, "Banka limiti")
            TutarAlani(g.kendiLimiti, { degisti(g.copy(kendiLimiti = it)) }, "Kendi limitin")
            Text("Ekranda bu sınır görünür, uyarılar buna göre çalışır.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SayiAlani(g.asgariOran, { degisti(g.copy(asgariOran = it)) }, "Asgari ödeme oranı (%)", Modifier.fillMaxWidth())
            if (duzenleme) Text("Kesim günü değişirse mevcut taksitler eski tarihlerinde kalır.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!duzenleme) Bolum("Şu anki borç") {
            TutarAlani(g.kesilmisEkstre, { degisti(g.copy(kesilmisEkstre = it)) }, "Kesilmiş ekstre (ödenmemiş)")
            TutarAlani(g.donemIci, { degisti(g.copy(donemIci = it)) }, "Dönem içi tek çekimler")
            if (g.borcLimitiAsiyor()) Text("Borç banka limitinden yüksek görünüyor, tutarı kontrol et.", color = MaterialTheme.colorScheme.error)
            Text("Taksitler kurulumun son adımında ayrıca girilir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Garanti BBVA · Bonus ··4821"; bankasız kartta yalnızca "Bonus ··4821". */
internal fun kartEtiketi(k: KartBilgisi): String = hesapEtiketi(k.bankaAdi, "${k.ad} ··${k.son4}")
