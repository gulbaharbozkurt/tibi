package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.SayiAlani
import app.tibi.ui.ortak.Secici
import app.tibi.ui.ortak.TutarAlani
import app.tibi.ui.ortak.aralik

/** Form alanları dışarıda tutulur; "Devam" butonu KurulumAkisi'nde maasKaydet'i çağırır. */
class GelirFormu(val tutar: String = "", val gun: String = "", val haftaSonu: HaftaSonuKurali = HaftaSonuKurali.ONCEKI, val hesapId: Long? = null) {
    fun kopya(tutar: String = this.tutar, gun: String = this.gun, haftaSonu: HaftaSonuKurali = this.haftaSonu, hesapId: Long? = this.hesapId) =
        GelirFormu(tutar, gun, haftaSonu, hesapId)
}

private val HAFTA_SONU_ADI = mapOf(
    HaftaSonuKurali.ONCEKI to "Önceki iş günü",
    HaftaSonuKurali.SONRAKI to "Sonraki iş günü",
    HaftaSonuKurali.AYNI to "Aynı gün",
)

@Composable
fun GelirAdimi(vm: KurulumVm, form: GelirFormu, degisti: (GelirFormu) -> Unit) {
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val bankalar = hesaplar.filter { it.tur == app.tibi.veri.tablo.HesapTuru.BANKA }
    androidx.compose.runtime.LaunchedEffect(bankalar) {
        if (form.hesapId == null) bankalar.firstOrNull { it.maasHesabi }?.let { degisti(form.kopya(hesapId = it.id)) }
    }
    val donem = vm.donemOnizleme(form.gun, form.haftaSonu)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("\"Bu dönem\" maaş gününden maaş gününe sayılır. Yan gelirleri (özel ders gibi) uygulamada girersin.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bolum("Maaş") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TutarAlani(form.tutar, { degisti(form.kopya(tutar = it)) }, "Tutar", Modifier.weight(1.4f))
                SayiAlani(form.gun, { degisti(form.kopya(gun = it)) }, "Yattığı gün", Modifier.weight(1f))
            }
            Secici("Hafta sonuna denk gelirse", HaftaSonuKurali.entries, form.haftaSonu, { HAFTA_SONU_ADI.getValue(it) },
                { degisti(form.kopya(haftaSonu = it)) })
            Secici("Yattığı hesap", bankalar, bankalar.firstOrNull { it.id == form.hesapId }, { it.ad },
                { degisti(form.kopya(hesapId = it.id)) })
        }
        donem?.let {
            Bolum("Bu dönem") {
                Text(it.aralik(), style = MaterialTheme.typography.titleMedium)
                Text("Bütçeler ve \"bu dönem kalan\" bu aralığa göre hesaplanır.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
