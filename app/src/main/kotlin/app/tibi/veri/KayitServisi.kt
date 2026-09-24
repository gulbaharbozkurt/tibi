package app.tibi.veri

import androidx.room.withTransaction
import app.tibi.core.para.Kurus
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import java.time.Instant
import java.time.LocalDate

class KayitHatasi(mesaj: String) : IllegalArgumentException(mesaj)

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
            )
        )
        if (hesap.tur == HesapTuru.KREDI_KARTI) kartaYansit(hareketId, hesapId, tutar, taksitSayisi, ertelemeAy, tarih)
        hareketId
    }

    suspend fun gelir(tutar: Kurus, tarih: LocalDate, hesapId: Long, kategoriId: Long?, aciklama: String? = null): Long =
        db.withTransaction {
            pozitif(tutar)
            hesapGetir(hesapId)
            db.hareketDao().ekle(
                Hareket(tur = HareketTuru.GELIR, tarih = tarih, tutarKurus = tutar.deger, hedefHesapId = hesapId,
                    kategoriId = kategoriId, aciklama = aciklama, olusturma = saat())
            )
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

    /** Görev 5'te taksit satırlarını üretir. */
    internal suspend fun kartaYansit(hareketId: Long, kartId: Long, tutar: Kurus, taksitSayisi: Int, ertelemeAy: Int, tarih: LocalDate) {
        throw KayitHatasi("Kart harcaması henüz desteklenmiyor")
    }

    private fun pozitif(tutar: Kurus) {
        if (tutar.deger <= 0) throw KayitHatasi("Tutar sıfırdan büyük olmalı")
    }

    private suspend fun hesapGetir(id: Long): Hesap =
        db.hesapDao().getir(id) ?: throw KayitHatasi("Hesap bulunamadı: $id")
}
