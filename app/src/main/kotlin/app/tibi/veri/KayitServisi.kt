package app.tibi.veri

import androidx.room.withTransaction
import app.tibi.core.donem.MaasKurali
import app.tibi.core.kart.KartTakvimi
import app.tibi.core.kart.PlanliTaksit
import app.tibi.core.kart.TaksitPlanlayici
import app.tibi.core.para.Kurus
import app.tibi.core.para.topla
import app.tibi.veri.tablo.Avans
import app.tibi.veri.tablo.Banka
import app.tibi.veri.tablo.Ekstre
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.TaksitSatiri
import app.tibi.veri.tablo.TaksitTuru
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val TURKCE: Locale = Locale.forLanguageTag("tr")
private const val MAAS_KATEGORISI = "Maaş"

class KayitHatasi(mesaj: String) : IllegalArgumentException(mesaj)

/** K8: geçmişte başlamış taksitin üç giriş yolu. */
sealed interface GecmisTaksitGirisi {
    data class Aylik(val aylik: Kurus, val toplam: Int, val siradakiNo: Int) : GecmisTaksitGirisi
    data class Toplam(val tutar: Kurus, val toplam: Int, val siradakiNo: Int) : GecmisTaksitGirisi
    data class KalanBorc(val tutar: Kurus, val kalanSayi: Int) : GecmisTaksitGirisi
}

/** Bütün yazma işlemleri burada; her biri tek transaction. Biri başarısız olursa hiçbir satır yazılmaz. */
class KayitServisi(
    private val db: TibiVeritabani,
    private val saat: () -> Instant = Instant::now,
) {
    suspend fun harcama(
        tutar: Kurus,
        tarih: LocalDate,
        hesapId: Long,
        kategoriId: Long?,
        taksitSayisi: Int = 1,
        ertelemeAy: Int = 0,
        aciklama: String? = null,
        kalem: String? = null,
    ): Long = db.withTransaction {
        pozitif(tutar)
        val hesap = hesapGetir(hesapId)
        if (hesap.tur != HesapTuru.KREDI_KARTI && (taksitSayisi != 1 || ertelemeAy != 0)) {
            throw KayitHatasi("Taksit ve erteleme yalnızca kredi kartında olur")
        }
        val hareketId = db.hareketDao().ekle(
            Hareket(
                tur = HareketTuru.HARCAMA, tarih = tarih, tutarKurus = tutar.deger,
                kaynakHesapId = hesapId, kategoriId = kategoriId,
                taksitSayisi = taksitSayisi, ertelemeAy = ertelemeAy,
                aciklama = aciklama, olusturma = saat(),
                kalem = kalem?.let(::kalemAdi), kalemAnahtar = kalem?.let(::kalemAnahtari),
            )
        )
        if (hesap.tur == HesapTuru.KREDI_KARTI) kartaYansit(hareketId, hesapId, tutar, taksitSayisi, ertelemeAy, tarih)
        hareketId
    }

    /** A2: "Maaş" kategorisindeki gelir, tarihi o güne kadar olan bütün açık avansları mahsup eder. */
    suspend fun gelir(tutar: Kurus, tarih: LocalDate, hesapId: Long, kategoriId: Long?, aciklama: String? = null): Long =
        db.withTransaction {
            pozitif(tutar)
            hesapGetir(hesapId)
            val id = db.hareketDao().ekle(
                Hareket(tur = HareketTuru.GELIR, tarih = tarih, tutarKurus = tutar.deger, hedefHesapId = hesapId,
                    kategoriId = kategoriId, aciklama = aciklama, olusturma = saat())
            )
            val maas = kategoriId?.let { db.kategoriDao().getir(it) }?.ad == MAAS_KATEGORISI
            if (maas) {
                val acik = db.avansDao().acikAvansIdleri(tarih)
                if (acik.isNotEmpty()) db.avansDao().mahsupEt(acik, id)
            }
            id
        }

    /** A1: avans hesaba yatar, bakiye artar; gelir sayılmaz, bir sonraki maaştan düşülecek diye işaretlenir. */
    suspend fun avans(tutar: Kurus, tarih: LocalDate, hesapId: Long, aciklama: String? = null): Long =
        db.withTransaction {
            pozitif(tutar)
            if (hesapGetir(hesapId).tur == HesapTuru.KREDI_KARTI) throw KayitHatasi("Avans bir banka hesabına ya da elde nakde yatar.")
            val id = db.hareketDao().ekle(
                Hareket(tur = HareketTuru.AVANS, tarih = tarih, tutarKurus = tutar.deger, hedefHesapId = hesapId,
                    aciklama = aciklama, olusturma = saat())
            )
            db.avansDao().ekle(Avans(hareketId = id, dusulecekMaasTarihi = sonrakiMaas(tarih)))
            id
        }

    /** Maaş kuralına göre verilen tarihten kesin sonraki ilk maaş günü; kural yoksa null. */
    private suspend fun sonrakiMaas(tarih: LocalDate): LocalDate? {
        val k = db.duzenliKuralDao().maasKurali() ?: return null
        val kural = MaasKurali(k.gun, k.haftaSonuKurali)
        val ay = YearMonth.from(tarih)
        return listOf(ay, ay.plusMonths(1), ay.plusMonths(2)).map(kural::tarih).first { it.isAfter(tarih) }
    }

    suspend fun transfer(tutar: Kurus, tarih: LocalDate, kaynakHesapId: Long, hedefHesapId: Long, aciklama: String? = null): Long =
        db.withTransaction {
            pozitif(tutar)
            if (kaynakHesapId == hedefHesapId) throw KayitHatasi("Aynı hesaba transfer yapılamaz")
            hesapGetir(kaynakHesapId)
            hesapGetir(hedefHesapId)
            db.hareketDao().ekle(
                Hareket(tur = HareketTuru.TRANSFER, tarih = tarih, tutarKurus = tutar.deger,
                    kaynakHesapId = kaynakHesapId, hedefHesapId = hedefHesapId, aciklama = aciklama, olusturma = saat())
            )
        }

    /** Bakiyeyi gerçeğe eşitleyen düzeltme; artis ise para hesaba girer, değilse çıkar. Dönem toplamına sayılmaz. */
    suspend fun duzeltme(tutar: Kurus, tarih: LocalDate, hesapId: Long, artis: Boolean): Long = db.withTransaction {
        pozitif(tutar)
        if (hesapGetir(hesapId).tur == HesapTuru.KREDI_KARTI) throw KayitHatasi("Kredi kartında bakiye düzeltilmez")
        db.hareketDao().ekle(
            Hareket(tur = HareketTuru.DUZELTME, tarih = tarih, tutarKurus = tutar.deger,
                hedefHesapId = if (artis) hesapId else null, kaynakHesapId = if (artis) null else hesapId,
                aciklama = "Bakiye düzeltme", olusturma = saat())
        )
    }

    /** Adı büyük/küçük harf farkı gözetmeden (Türkçe kurallarla) eşleşen bankanın id'si; yoksa yeni banka açar. */
    suspend fun bankaBulVeyaEkle(ad: String): Long = db.withTransaction {
        val temiz = ad.trim()
        if (temiz.isEmpty()) throw KayitHatasi("Banka adını yaz.")
        val anahtar = temiz.lowercase(TURKCE)
        db.bankaDao().adIle(temiz)?.id
            ?: db.bankaDao().hepsi().firstOrNull { it.ad.trim().lowercase(TURKCE) == anahtar }?.id
            ?: db.bankaDao().ekle(Banka(ad = temiz))
    }

    internal suspend fun kartaYansit(hareketId: Long, kartId: Long, tutar: Kurus, taksitSayisi: Int, ertelemeAy: Int, tarih: LocalDate) {
        val plan = try {
            TaksitPlanlayici.yeniHarcama(tutar, taksitSayisi, ertelemeAy, tarih, takvim(kartId))
        } catch (e: IllegalArgumentException) {
            throw KayitHatasi(e.message ?: "Geçersiz taksit")
        }
        satirlariYaz(hareketId, kartId, plan)
    }

    suspend fun kartOdemesi(tutar: Kurus, tarih: LocalDate, bankaHesapId: Long, kartId: Long, ekstreId: Long? = null): Long =
        db.withTransaction {
            pozitif(tutar)
            hesapGetir(bankaHesapId)
            if (hesapGetir(kartId).tur != HesapTuru.KREDI_KARTI) throw KayitHatasi("Ödeme yalnızca kredi kartına yapılır")
            db.hareketDao().ekle(
                Hareket(tur = HareketTuru.KART_ODEME, tarih = tarih, tutarKurus = tutar.deger,
                    kaynakHesapId = bankaHesapId, hedefHesapId = kartId, ekstreId = ekstreId, olusturma = saat())
            )
        }

    suspend fun gecmisTaksit(
        giris: GecmisTaksitGirisi,
        kartId: Long,
        kategoriId: Long?,
        taksitTuru: TaksitTuru,
        bugun: LocalDate,
        aciklama: String? = null,
    ): Long = db.withTransaction {
        if (hesapGetir(kartId).tur != HesapTuru.KREDI_KARTI) throw KayitHatasi("Geçmiş taksit yalnızca kredi kartına girilir")
        val tk = takvim(kartId)
        val plan = try {
            when (giris) {
                is GecmisTaksitGirisi.Aylik -> { pozitif(giris.aylik); TaksitPlanlayici.gecmisAylik(giris.aylik, giris.toplam, giris.siradakiNo, bugun, tk) }
                is GecmisTaksitGirisi.Toplam -> { pozitif(giris.tutar); TaksitPlanlayici.gecmisToplam(giris.tutar, giris.toplam, giris.siradakiNo, bugun, tk) }
                is GecmisTaksitGirisi.KalanBorc -> { pozitif(giris.tutar); TaksitPlanlayici.kalanBorc(giris.tutar, giris.kalanSayi, bugun, tk) }
            }
        } catch (e: IllegalArgumentException) {
            if (e is KayitHatasi) throw e
            throw KayitHatasi(e.message ?: "Geçersiz taksit")
        }
        val hareketId = db.hareketDao().ekle(
            Hareket(
                tur = HareketTuru.HARCAMA, tarih = bugun, tutarKurus = plan.map { it.tutar }.topla().deger,
                kaynakHesapId = kartId, kategoriId = kategoriId, taksitSayisi = plan.size,
                taksitTuru = taksitTuru, gecmisAktarim = true, aciklama = aciklama, olusturma = saat(),
            )
        )
        satirlariYaz(hareketId, kartId, plan)
        hareketId
    }

    /** K9: kurulumda girilen, kesilmiş ama ödenmemiş ekstre. Kesim tarihi bugüne kadarki son kesimdir. */
    suspend fun acilisEkstresi(kartId: Long, toplam: Kurus, bugun: LocalDate): Long = db.withTransaction {
        pozitif(toplam)
        val kart = db.kartDao().getir(kartId) ?: throw KayitHatasi("Kart bulunamadı: $kartId")
        val tk = takvim(kartId)
        val buAy = tk.kesimTarihi(YearMonth.from(bugun))
        val kesim = if (buAy.isAfter(bugun)) tk.kesimTarihi(YearMonth.from(bugun).minusMonths(1)) else buAy
        db.ekstreDao().ekle(
            Ekstre(
                kartId = kart.anaKartId ?: kartId, kesimTarihi = kesim, sonOdemeTarihi = tk.sonOdeme(kesim),
                donemTutariKurus = toplam.deger, toplamKurus = toplam.deger,
                asgariKurus = toplam.oran(kart.asgariOranBinde).deger, acilis = true,
            )
        )
    }

    /** Ek/sanal kart ana kartın kesim ve son ödeme günlerini kullanır. */
    private suspend fun takvim(kartId: Long): KartTakvimi {
        val kart = db.kartDao().getir(kartId) ?: throw KayitHatasi("Kart bilgisi yok: $kartId")
        val ana = kart.anaKartId?.let { db.kartDao().getir(it) ?: throw KayitHatasi("Ana kart yok: $it") } ?: kart
        return KartTakvimi(ana.kesimGunu, ana.sonOdemeGunu)
    }

    private suspend fun satirlariYaz(hareketId: Long, kartId: Long, plan: List<PlanliTaksit>) {
        db.taksitDao().ekle(plan.map {
            TaksitSatiri(hareketId = hareketId, kartId = kartId, sira = it.sira, toplam = it.toplam,
                tutarKurus = it.tutar.deger, ekstreKesimTarihi = it.ekstreKesimTarihi, oncedenOdendi = it.oncedenOdendi)
        })
    }

    private fun pozitif(tutar: Kurus) {
        if (tutar.deger <= 0) throw KayitHatasi("Tutar sıfırdan büyük olmalı")
    }

    private suspend fun hesapGetir(id: Long): Hesap =
        db.hesapDao().getir(id) ?: throw KayitHatasi("Hesap bulunamadı: $id")
}
