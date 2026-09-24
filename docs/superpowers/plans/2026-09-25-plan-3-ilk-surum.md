# tibi Plan 3: İlk Kullanılabilir Sürüm — Uygulama Planı

> **Ajanlar için:** GEREKLİ ALT BECERİ: superpowers:subagent-driven-development (önerilen) ya da superpowers:executing-plans ile görev görev uygulayın. Adımlar `- [ ]` onay kutularıyla takip edilir.

**Hedef:** Kullanıcının gerçek verisini girmeye başlayabileceği ilk sürüm (v0.2.0).
- **Kurulum:** Hoş geldin (hitap adı), hesaplar ve nakit, gelir (maaş), kartlar (açılış borcuyla), devam eden taksitler.
- **Hızlı giriş:** "+" butonuyla harcama, gelir ve transfer.
- **Ekranlar:** Özet (maaş dönemine göre), Hareketler (liste, filtre, arama, silme), Hesaplar sekmesi ve kart detayı.

**Mimari:**
- **Arayüz:** Jetpack Compose.
- **Ekran modelleri:** Her ekranın bir ViewModel'i var. ViewModel okumayı `Flow` ile, yazmayı `suspend` fonksiyonlarla yapar. Yazma fonksiyonları `Sonuc` döndürür ve bu yüzden bellekteki veritabanıyla Robolectric'te test edilir.
- **Bağımlılık:** Kütüphane yok, `TibiUygulama` üzerinden elle verilir.
- **İlk açılış:** Kurulum bitmediyse kurulum akışı, bittiyse 5 sekmeli kabuk açılır. Hangisinin açılacağına `ayar` tablosundaki `kurulum_tamam` anahtarı karar verir.

**Teknoloji:** Mevcut yığın ve lifecycle-viewmodel-compose / lifecycle-runtime-compose 2.8.7.

**Kapsam dışı (sonraki planlar):**
- PIN kilidi ve yedek: Plan 4.
- Ekstre kapatma, ödeme işaretleme, bildirimler: Plan 5.
- Harcama freni, bekleme listesi: Plan 6.
- Banka tetikleyicisi, İzinler adımı: Plan 7.
- Avans, düzenli harcama, yan gelir kuralları: Plan 8.
- Cüzdanlar ve Dersler sekmeleri Plan 9 ve 10'a kadar yer tutucu olarak kalır.

## Genel Kısıtlar

- Plan 1 ve 2'nin Genel Kısıtları geçerli: tutar `Long` kuruş, testlerde `L` soneki, **her komuttan önce** `source ~/.tibi-env`, telefon `"$WADB"` ile, APK `wslpath -w` ile verilir.
- Commit mesajının sonunda iki satır bulunur:
  ```
  Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq
  ```
- **Veritabanı sürümü 1 kalır.** Şemaya yeni tablo ya da sütun eklenmez, bu planda yalnızca sorgular eklenir. v0.2.0 telefona kurulduktan sonra şemadaki **her** değişiklik `version` artışı ve migration ister. Bu kural Plan 4'ten itibaren geçerli.
- Arayüz metinleri Türkçe; butonlar ne yapacağını söyler ("Kaydet", "Kartı kaydet"). Hata mesajı sorunu ve çözümü söyler.
- Tutar gösterimi `Kurus.bicimle()` ile yapılır ("1.249,90 ₺"). Giriş `kurusCoz()` ile okunur.
- Renkler `MaterialTheme.colorScheme` üzerinden alınır, koda sabit renk yazılmaz.
- Kategori, hesap ve kart adları kullanıcıdan gelir. Ekranda metin olarak gösterilir, başka bir yerde yorumlanmaz.

## Dosya Yapısı

```
app/src/main/kotlin/app/tibi/
├── TibiUygulama.kt                    + donemServisi
├── MainActivity.kt                    KokEkran'ı açar
├── veri/
│   ├── DonemServisi.kt                maaş kuralından dönem akışı
│   ├── Anahtarlar.kt                  ayar anahtarları
│   └── dao/…                          yeni sorgular (Görev 1)
└── ui/
    ├── KokEkran.kt                    kurulum mu kabuk mu
    ├── Kabuk.kt                       5 sekme + "+" butonu + rotalar
    ├── Vm.kt                          tibiVm { } yardımcısı, Sonuc
    ├── ortak/Bicim.kt                 tarih/gün başlığı biçimleri
    ├── ortak/Alanlar.kt               TutarAlani, SayiAlani, Secici, Bolum
    ├── kurulum/KurulumVm.kt
    ├── kurulum/KurulumAkisi.kt        adım gezgini
    ├── kurulum/HosGeldinEkrani.kt  HesaplarAdimi.kt  GelirAdimi.kt
    ├── kurulum/KartlarAdimi.kt  KartFormu.kt  TaksitlerAdimi.kt
    ├── giris/HizliGirisVm.kt  giris/HizliGirisSayfasi.kt
    ├── ozet/OzetVm.kt  ozet/OzetEkrani.kt
    ├── hareketler/HareketlerVm.kt  hareketler/HareketlerEkrani.kt
    └── hesaplar/HesaplarVm.kt  HesaplarEkrani.kt  KartDetayVm.kt  KartDetayEkrani.kt
app/src/test/kotlin/app/tibi/…         her Vm ve yeni sorgu için Robolectric testi
```

---

### Görev 1: Ekranların ihtiyaç duyduğu sorgular ve dönem servisi

**Dosyalar:**
- Değiştir: `veri/dao/HareketDao.kt`, `KategoriDao.kt`, `HesapDao.kt`, `KartDao.kt`, `TaksitDao.kt`, `DuzenliKuralDao.kt`, `AyarDao.kt`
- Oluştur: `veri/DonemServisi.kt`, `veri/Anahtarlar.kt`
- Değiştir: `TibiUygulama.kt`
- Test: `app/src/test/kotlin/app/tibi/veri/EkranSorgulariTest.kt`

**Arayüzler:**
- Üretir:
  - `data class DonemToplami(val gelirKurus: Long, val giderKurus: Long)`; `HareketDao.donemToplami(bas, bit): Flow<DonemToplami>` (gelir = GELIR + TAHSILAT; gider = HARCAMA, `gecmisAktarim = 0`; AVANS, TRANSFER, KART_ODEME sayılmaz)
  - `data class HareketSatiri(id, tur, tarih, tutarKurus, kategoriAdi: String?, kaynakAdi: String?, hedefAdi: String?, taksitSayisi, aciklama: String?, gecmisAktarim)`; `HareketDao.satirlar(bas, bit): Flow<List<HareketSatiri>>`; `HareketDao.sil(id)`
  - `KategoriDao.kullanimSirali(yon): Flow<List<Kategori>>` (en çok kullanılan önce, E4)
  - `data class HesapBakiyesi(id, ad, tur, maasHesabi, bakiyeKurus)`; `HesapDao.bakiyeler(): Flow<List<HesapBakiyesi>>` (kredi kartı hariç)
  - `data class KartBilgisi(hesapId, ad, son4, kartTuru, anaKartId: Long?, kesimGunu, sonOdemeGunu, bankaLimitiKurus: Long?, kendiLimitiKurus: Long?, asgariOranBinde, kullanimKurus)`; `KartDao.kartlar(): Flow<List<KartBilgisi>>` (ek/sanal kartta `kullanimKurus = 0`)
  - `data class KartTaksidi(hareketId, aciklama: String?, kategoriAdi: String?, toplam, siradakiSira, aylikKurus, kalanKurus, sonKesim: LocalDate)`; `TaksitDao.aktifTaksitler(anaKartId, bugun): Flow<List<KartTaksidi>>` (yalnızca `toplam > 1`); `TaksitDao.kesimTutari(anaKartId, kesim): Flow<Long>`
  - `DuzenliKuralDao.maasKuraliAkisi(): Flow<DuzenliKural?>`; `AyarDao.okuAkis(anahtar): Flow<String?>`
  - `object Anahtarlar { const val KURULUM_TAMAM = "kurulum_tamam"; const val HITAP = "hitap" }`
  - `class DonemServisi(db)` + `fun donem(bugun: LocalDate): Flow<Donem>` (maaş kuralı yoksa takvim ayı) + `companion fun takvimAyi(bugun): Donem`
  - `TibiUygulama.donemServisi: DonemServisi`

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/veri/EkranSorgulariTest.kt`:
```kotlin
package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.donem.Donem
import app.tibi.core.para.Kurus
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class EkranSorgulariTest {
    private lateinit var db: TibiVeritabani
    private lateinit var kayit: KayitServisi
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var banka = 0L
    private var bonus = 0L
    private var sanal = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun, maasHesabi = true))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, bankaLimitiKurus = 5_000_000, kendiLimitiKurus = 1_500_000))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
    }
    @After fun kapat() { db.close() }

    private suspend fun kat(ad: String) = db.kategoriDao().adIle(ad)!!.id

    @Test
    fun `donem toplami gelir ve gideri ayirir`() = runTest {
        kayit.gelir(Kurus(4_500_000), t("2026-08-28"), banka, kat("Maaş"))
        kayit.harcama(Kurus(38650), t("2026-09-10"), banka, kat("Market"))
        kayit.harcama(Kurus(124990), bugun, bonus, kat("Giyim"), taksitSayisi = 3)
        kayit.transfer(Kurus(50000), bugun, banka, banka.let { db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = bugun)) })
        kayit.kartOdemesi(Kurus(10000), bugun, banka, bonus)
        kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun)
        kayit.harcama(Kurus(999), t("2026-09-30"), banka, kat("Market"))   // dönem dışında
        val d = db.hareketDao().donemToplami(t("2026-08-28"), t("2026-09-29")).first()
        assertEquals(4_500_000L, d.gelirKurus)
        assertEquals(38650L + 124990L, d.giderKurus)
    }

    @Test
    fun `hareket satirlari adlarla gelir ve silinir`() = runTest {
        val id = kayit.harcama(Kurus(124990), bugun, bonus, kat("Giyim"), taksitSayisi = 3, aciklama = "mont")
        val s = db.hareketDao().satirlar(t("2026-09-01"), t("2026-09-30")).first().single()
        assertEquals("Giyim", s.kategoriAdi)
        assertEquals("Bonus", s.kaynakAdi)
        assertNull(s.hedefAdi)
        assertEquals(3, s.taksitSayisi)
        assertEquals(HareketTuru.HARCAMA, s.tur)
        db.hareketDao().sil(id)
        assertEquals(0, db.taksitDao().sayi())
        assertEquals(0L, db.taksitDao().limitKullanimi(bonus).first())
    }

    @Test
    fun `kategoriler kullanima gore siralanir`() = runTest {
        kayit.harcama(Kurus(100), bugun, banka, kat("Ev"))
        kayit.harcama(Kurus(100), bugun, banka, kat("Ev"))
        kayit.harcama(Kurus(100), bugun, banka, kat("Sağlık"))
        val adlar = db.kategoriDao().kullanimSirali(KategoriYonu.GIDER).first().map { it.ad }
        assertEquals(listOf("Ev", "Sağlık", "Market"), adlar.take(3))
        assertEquals(14, adlar.size)
    }

    @Test
    fun `hesap bakiyeleri kartlari icermez`() = runTest {
        kayit.harcama(Kurus(5000), bugun, banka, null)
        val b = db.hesapDao().bakiyeler().first()
        assertEquals(listOf("Garanti"), b.map { it.ad })
        assertEquals(840000L, b.single().bakiyeKurus)
    }

    @Test
    fun `kart bilgisi limit kullanimini ana kartta toplar`() = runTest {
        kayit.harcama(Kurus(7999), bugun, sanal, null)
        kayit.harcama(Kurus(100000), bugun, bonus, null)
        val k = db.kartDao().kartlar().first()
        val ana = k.first { it.hesapId == bonus }
        assertEquals(107999L, ana.kullanimKurus)
        assertEquals(db.taksitDao().limitKullanimi(bonus).first(), ana.kullanimKurus)
        assertEquals(0L, k.first { it.hesapId == sanal }.kullanimKurus)
        assertEquals(bonus, k.first { it.hesapId == sanal }.anaKartId)
    }

    @Test
    fun `aktif taksitler ve kesim tutari`() = runTest {
        kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
        kayit.harcama(Kurus(38650), bugun, bonus, null)                       // tek çekim, listede yok
        val a = db.taksitDao().aktifTaksitler(bonus, bugun).first().single()
        assertEquals("Telefon", a.aciklama)
        assertEquals(12, a.toplam)
        assertEquals(6, a.siradakiSira)
        assertEquals(185000L, a.aylikKurus)
        assertEquals(7 * 185000L, a.kalanKurus)
        assertEquals(t("2027-04-12"), a.sonKesim)
        assertEquals(185000L + 38650L, db.taksitDao().kesimTutari(bonus, t("2026-10-12")).first())
    }

    @Test
    fun `donem maas kuralindan yoksa takvim ayindan`() = runTest {
        val servis = DonemServisi(db)
        assertEquals(Donem(t("2026-09-01"), t("2026-09-30")), servis.donem(bugun).first())
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = banka, baslangic = bugun))
        assertEquals(Donem(t("2026-08-28"), t("2026-09-29")), servis.donem(bugun).first())
    }

    @Test
    fun `ayar akisi`() = runTest {
        assertNull(db.ayarDao().okuAkis(Anahtarlar.KURULUM_TAMAM).first())
        db.ayarDao().yaz(app.tibi.veri.tablo.Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        assertEquals("1", db.ayarDao().okuAkis(Anahtarlar.KURULUM_TAMAM).first())
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*EkranSorgulariTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'donemToplami'`.

- [ ] **Adım 3: Sorguları ekle**

`HareketDao.kt`'ye ekle (dosyanın üstüne veri sınıfları, arayüze sorgular; gerekli importlar: `app.tibi.veri.tablo.HareketTuru`):
```kotlin
data class DonemToplami(val gelirKurus: Long, val giderKurus: Long)

data class HareketSatiri(
    val id: Long,
    val tur: HareketTuru,
    val tarih: LocalDate,
    val tutarKurus: Long,
    val kategoriAdi: String?,
    val kaynakAdi: String?,
    val hedefAdi: String?,
    val taksitSayisi: Int,
    val aciklama: String?,
    val gecmisAktarim: Boolean,
)
```
```kotlin
    @Query(
        """
        SELECT
          COALESCE(SUM(CASE WHEN tur IN ('GELIR', 'TAHSILAT') THEN tutarKurus END), 0) AS gelirKurus,
          COALESCE(SUM(CASE WHEN tur = 'HARCAMA' AND gecmisAktarim = 0 THEN tutarKurus END), 0) AS giderKurus
        FROM hareket WHERE tarih BETWEEN :bas AND :bit
        """
    )
    fun donemToplami(bas: LocalDate, bit: LocalDate): Flow<DonemToplami>

    @Query(
        """
        SELECT h.id, h.tur, h.tarih, h.tutarKurus, k.ad AS kategoriAdi, ks.ad AS kaynakAdi, hd.ad AS hedefAdi,
               h.taksitSayisi, h.aciklama, h.gecmisAktarim
        FROM hareket h
        LEFT JOIN kategori k ON k.id = h.kategoriId
        LEFT JOIN hesap ks ON ks.id = h.kaynakHesapId
        LEFT JOIN hesap hd ON hd.id = h.hedefHesapId
        WHERE h.tarih BETWEEN :bas AND :bit
        ORDER BY h.tarih DESC, h.id DESC
        """
    )
    fun satirlar(bas: LocalDate, bit: LocalDate): Flow<List<HareketSatiri>>

    @Query("DELETE FROM hareket WHERE id = :id") suspend fun sil(id: Long)
```

`KategoriDao.kt`'ye ekle:
```kotlin
    /** E4: en çok kullanılan kategori önce; eşitlikte varsayılan sıra. */
    @Query(
        """
        SELECT k.* FROM kategori k LEFT JOIN hareket h ON h.kategoriId = k.id
        WHERE k.yon = :yon AND k.arsiv = 0
        GROUP BY k.id ORDER BY COUNT(h.id) DESC, k.sira, k.id
        """
    )
    fun kullanimSirali(yon: KategoriYonu): Flow<List<Kategori>>
```

`HesapDao.kt`'ye ekle (üstte veri sınıfı; import `app.tibi.veri.tablo.HesapTuru`):
```kotlin
data class HesapBakiyesi(val id: Long, val ad: String, val tur: HesapTuru, val maasHesabi: Boolean, val bakiyeKurus: Long)
```
```kotlin
    @Query(
        """
        SELECT h.id, h.ad, h.tur, h.maasHesabi,
               h.acilisBakiyeKurus
             + COALESCE((SELECT SUM(g.tutarKurus) FROM hareket g WHERE g.hedefHesapId = h.id), 0)
             - COALESCE((SELECT SUM(c.tutarKurus) FROM hareket c WHERE c.kaynakHesapId = h.id), 0) AS bakiyeKurus
        FROM hesap h WHERE h.arsiv = 0 AND h.tur != 'KREDI_KARTI' ORDER BY h.sira, h.id
        """
    )
    fun bakiyeler(): Flow<List<HesapBakiyesi>>
```

`KartDao.kt`'ye ekle (üstte veri sınıfı; importlar `app.tibi.veri.tablo.KartTuru`, `kotlinx.coroutines.flow.Flow`):
```kotlin
data class KartBilgisi(
    val hesapId: Long,
    val ad: String,
    val son4: String,
    val kartTuru: KartTuru,
    val anaKartId: Long?,
    val kesimGunu: Int,
    val sonOdemeGunu: Int,
    val bankaLimitiKurus: Long?,
    val kendiLimitiKurus: Long?,
    val asgariOranBinde: Int,
    val kullanimKurus: Long,
)
```
```kotlin
    /** kullanimKurus: TaksitDao.limitKullanimi ile aynı formül; yalnızca ana kartta dolu. */
    @Query(
        """
        SELECT h.id AS hesapId, h.ad, k.son4, k.kartTuru, k.anaKartId, k.kesimGunu, k.sonOdemeGunu,
               k.bankaLimitiKurus, k.kendiLimitiKurus, k.asgariOranBinde,
               CASE WHEN k.anaKartId IS NULL THEN
                   COALESCE((SELECT SUM(t.tutarKurus) FROM taksit_satiri t WHERE t.oncedenOdendi = 0
                             AND (t.kartId = k.hesapId OR t.kartId IN (SELECT e.hesapId FROM kart e WHERE e.anaKartId = k.hesapId))), 0)
                 + COALESCE((SELECT SUM(x.toplamKurus) FROM ekstre x WHERE x.acilis = 1 AND x.kartId = k.hesapId), 0)
                 - COALESCE((SELECT SUM(o.tutarKurus) FROM hareket o WHERE o.tur = 'KART_ODEME'
                             AND (o.hedefHesapId = k.hesapId OR o.hedefHesapId IN (SELECT e.hesapId FROM kart e WHERE e.anaKartId = k.hesapId))), 0)
               ELSE 0 END AS kullanimKurus
        FROM kart k JOIN hesap h ON h.id = k.hesapId
        WHERE h.arsiv = 0 ORDER BY h.sira, h.id
        """
    )
    fun kartlar(): Flow<List<KartBilgisi>>
```

`TaksitDao.kt`'ye ekle:
```kotlin
data class KartTaksidi(
    val hareketId: Long,
    val aciklama: String?,
    val kategoriAdi: String?,
    val toplam: Int,
    val siradakiSira: Int,
    val aylikKurus: Long,
    val kalanKurus: Long,
    val sonKesim: LocalDate,
)
```
```kotlin
    @Query(
        """
        SELECT t.hareketId, h.aciklama, k.ad AS kategoriAdi, MAX(t.toplam) AS toplam, MIN(t.sira) AS siradakiSira,
               MIN(t.tutarKurus) AS aylikKurus, SUM(t.tutarKurus) AS kalanKurus, MAX(t.ekstreKesimTarihi) AS sonKesim
        FROM taksit_satiri t
        JOIN hareket h ON h.id = t.hareketId
        LEFT JOIN kategori k ON k.id = h.kategoriId
        WHERE t.oncedenOdendi = 0 AND t.toplam > 1 AND t.ekstreKesimTarihi >= :bugun
          AND (t.kartId = :anaKartId OR t.kartId IN (SELECT hesapId FROM kart WHERE anaKartId = :anaKartId))
        GROUP BY t.hareketId ORDER BY sonKesim, t.hareketId
        """
    )
    fun aktifTaksitler(anaKartId: Long, bugun: LocalDate): Flow<List<KartTaksidi>>

    @Query(
        """
        SELECT COALESCE(SUM(tutarKurus), 0) FROM taksit_satiri
        WHERE oncedenOdendi = 0 AND ekstreKesimTarihi = :kesim
          AND (kartId = :anaKartId OR kartId IN (SELECT hesapId FROM kart WHERE anaKartId = :anaKartId))
        """
    )
    fun kesimTutari(anaKartId: Long, kesim: LocalDate): Flow<Long>
```

`DuzenliKuralDao.kt`'ye ekle (import `kotlinx.coroutines.flow.Flow`):
```kotlin
    @Query("SELECT * FROM duzenli_kural WHERE maas = 1 AND aktif = 1 ORDER BY id LIMIT 1")
    fun maasKuraliAkisi(): Flow<DuzenliKural?>
```

`AyarDao.kt`'ye ekle (import `kotlinx.coroutines.flow.Flow`):
```kotlin
    @Query("SELECT deger FROM ayar WHERE anahtar = :anahtar") fun okuAkis(anahtar: String): Flow<String?>
```

- [ ] **Adım 4: Dönem servisi ve anahtarlar**

`app/src/main/kotlin/app/tibi/veri/Anahtarlar.kt`:
```kotlin
package app.tibi.veri

/** ayar tablosundaki anahtarlar. */
object Anahtarlar {
    const val KURULUM_TAMAM = "kurulum_tamam"
    const val HITAP = "hitap"
}
```

`app/src/main/kotlin/app/tibi/veri/DonemServisi.kt`:
```kotlin
package app.tibi.veri

import app.tibi.core.donem.Donem
import app.tibi.core.donem.DonemHesaplayici
import app.tibi.core.donem.MaasKurali
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** "Bu dönem": maaş kuralından maaştan maaşa; kural yoksa takvim ayı. */
class DonemServisi(private val db: TibiVeritabani) {
    fun donem(bugun: LocalDate): Flow<Donem> = db.duzenliKuralDao().maasKuraliAkisi().map { k ->
        if (k == null) takvimAyi(bugun) else DonemHesaplayici.donem(MaasKurali(k.gun, k.haftaSonuKurali), bugun)
    }

    companion object {
        fun takvimAyi(bugun: LocalDate) = Donem(bugun.withDayOfMonth(1), bugun.withDayOfMonth(bugun.lengthOfMonth()))
    }
}
```

`TibiUygulama.kt` içine ekle:
```kotlin
    val donemServisi: DonemServisi by lazy { DonemServisi(veritabani) }
```
(import `app.tibi.veri.DonemServisi`)

- [ ] **Adım 5: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, 29 test (21 eski + 8 yeni). `app/schemas/.../1.json` değişmemeli (yalnızca sorgu eklendi); `git diff --stat app/schemas` boş olmalı.

- [ ] **Adım 6: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(veri): ekran sorguları (dönem toplamı, hareket satırları, bakiyeler, kart bilgisi, aktif taksitler) ve dönem servisi

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 2: Ortak arayüz parçaları, kök ekran ve kurulum kapısı

**Dosyalar:**
- Değiştir: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `MainActivity.kt`, `ui/Kabuk.kt`
- Oluştur: `ui/Vm.kt`, `ui/KokEkran.kt`, `ui/ortak/Bicim.kt`, `ui/ortak/Alanlar.kt`, `ui/kurulum/KurulumAkisi.kt` (geçici tek adım)
- Test: `app/src/test/kotlin/app/tibi/ui/ortak/BicimTest.kt`

**Arayüzler:**
- Üretir:
  - `sealed interface Sonuc { data object Tamam : Sonuc; data class Hata(val mesaj: String) : Sonuc }`
  - `@Composable inline fun <reified T : ViewModel> tibiVm(crossinline yap: (TibiUygulama) -> T): T`
  - `fun LocalDate.kisa(): String` → "24 Eyl"; `fun LocalDate.gunBasligi(bugun: LocalDate): String` → "Bugün · 24 Eyl" / "Dün · 23 Eyl" / "20 Eyl, Pazar"; `fun Donem.aralik(): String` → "28 Ağu – 29 Eyl"
  - `@Composable fun TutarAlani(metin: String, degisti: (String) -> Unit, etiket: String, modifier: Modifier = Modifier, buyuk: Boolean = false)`; `@Composable fun SayiAlani(metin, degisti, etiket, modifier)`; `@Composable fun <T> Secici(etiket: String, secenekler: List<T>, secili: T?, ad: (T) -> String, secildi: (T) -> Unit, modifier: Modifier = Modifier)`; `@Composable fun Bolum(baslik: String, modifier: Modifier = Modifier, icerik: @Composable ColumnScope.() -> Unit)`
  - `@Composable fun KokEkran()` — `kurulum_tamam` = "1" ise `Kabuk()`, değilse `KurulumAkisi(bitti = {})`; okunana kadar boş ekran.

- [ ] **Adım 1: Bağımlılıkları ekle**

`libs.versions.toml` `[versions]`: `lifecycle = "2.8.7"`; `[libraries]`:
```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
```
`app/build.gradle.kts` `dependencies`:
```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
```

- [ ] **Adım 2: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/ui/ortak/BicimTest.kt`:
```kotlin
package app.tibi.ui.ortak

import app.tibi.core.donem.Donem
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class BicimTest {
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Test fun kisa() {
        assertEquals("24 Eyl", t("2026-09-24").kisa())
        assertEquals("1 Oca", t("2027-01-01").kisa())
    }

    @Test fun gunBasligi() {
        assertEquals("Bugün · 24 Eyl", bugun.gunBasligi(bugun))
        assertEquals("Dün · 23 Eyl", t("2026-09-23").gunBasligi(bugun))
        assertEquals("20 Eyl, Pazar", t("2026-09-20").gunBasligi(bugun))
    }

    @Test fun aralik() {
        assertEquals("28 Ağu – 29 Eyl", Donem(t("2026-08-28"), t("2026-09-29")).aralik())
    }
}
```

- [ ] **Adım 3: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*BicimTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'kisa'`.

- [ ] **Adım 4: Biçimleri yaz**

`app/src/main/kotlin/app/tibi/ui/ortak/Bicim.kt`:
```kotlin
package app.tibi.ui.ortak

import app.tibi.core.donem.Donem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val TR: Locale = Locale.forLanguageTag("tr")
private val KISA = DateTimeFormatter.ofPattern("d MMM", TR)

fun LocalDate.kisa(): String = format(KISA)

fun LocalDate.gunBasligi(bugun: LocalDate): String = when (this) {
    bugun -> "Bugün · ${kisa()}"
    bugun.minusDays(1) -> "Dün · ${kisa()}"
    else -> "${kisa()}, ${dayOfWeek.getDisplayName(TextStyle.FULL, TR)}"
}

fun Donem.aralik(): String = "${baslangic.kisa()} – ${bitis.kisa()}"
```

- [ ] **Adım 5: Vm yardımcısı ve ortak alanlar**

`app/src/main/kotlin/app/tibi/ui/Vm.kt`:
```kotlin
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
```

`app/src/main/kotlin/app/tibi/ui/ortak/Alanlar.kt`:
```kotlin
package app.tibi.ui.ortak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tibi.core.para.kurusCoz

/** Tutar girişi; geçersizse altında nasıl yazılacağını söyler. Boş alan hata sayılmaz. */
@Composable
fun TutarAlani(metin: String, degisti: (String) -> Unit, etiket: String, modifier: Modifier = Modifier, buyuk: Boolean = false) {
    val hatali = metin.isNotBlank() && kurusCoz(metin) == null
    OutlinedTextField(
        value = metin,
        onValueChange = { yeni -> degisti(yeni.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(etiket) },
        suffix = { Text("₺") },
        singleLine = true,
        isError = hatali,
        supportingText = if (hatali) ({ Text("Tutarı 1.249,90 biçiminde yaz") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = if (buyuk) MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.End) else MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SayiAlani(metin: String, degisti: (String) -> Unit, etiket: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = metin,
        onValueChange = { yeni -> degisti(yeni.filter(Char::isDigit).take(4)) },
        label = { Text(etiket) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Secici(
    etiket: String,
    secenekler: List<T>,
    secili: T?,
    ad: (T) -> String,
    secildi: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var acik by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = acik, onExpandedChange = { acik = it }, modifier = modifier) {
        OutlinedTextField(
            value = secili?.let(ad) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(etiket) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = acik) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = acik, onDismissRequest = { acik = false }) {
            secenekler.forEach { s ->
                DropdownMenuItem(text = { Text(ad(s)) }, onClick = { secildi(s); acik = false })
            }
        }
    }
}

/** Başlıklı beyaz blok (taslaktaki .blk). */
@Composable
fun Bolum(baslik: String, modifier: Modifier = Modifier, icerik: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(baslik.uppercase(TR), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            icerik()
        }
    }
}
```

- [ ] **Adım 6: Kök ekran, geçici kurulum, MainActivity**

`app/src/main/kotlin/app/tibi/ui/kurulum/KurulumAkisi.kt` (Görev 3'te gerçek hâliyle değişir):
```kotlin
package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun KurulumAkisi(bitti: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Kurulum") }
}
```

`app/src/main/kotlin/app/tibi/ui/KokEkran.kt`:
```kotlin
package app.tibi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.TibiUygulama
import app.tibi.ui.kurulum.KurulumAkisi
import app.tibi.veri.Anahtarlar

private object Yukleniyor

@Composable
fun KokEkran() {
    val uygulama = LocalContext.current.applicationContext as TibiUygulama
    val akis = remember { uygulama.veritabani.ayarDao().okuAkis(Anahtarlar.KURULUM_TAMAM) }
    val durum by akis.collectAsStateWithLifecycle(initialValue = Yukleniyor)
    when (durum) {
        Yukleniyor -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        "1" -> Kabuk()
        else -> KurulumAkisi(bitti = {})   // ayar "1" olunca akış kendiliğinden Kabuk'a geçer
    }
}
```

`MainActivity.kt` içinde `setContent { TibiTema { Kabuk() } }` satırını değiştir:
```kotlin
        setContent { TibiTema { KokEkran() } }
```
(import `app.tibi.ui.KokEkran`, `Kabuk` importunu kaldır)

`Kabuk.kt`'deki `OzetYerTutucu` örnek maaş kuralını kullanıyor; Görev 8 onu gerçek Özet'le değiştirecek, bu görevde dokunma.

- [ ] **Adım 7: Test, derleme, telefonda bak**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")"
"$WADB" uninstall app.tibi.debug; "$WADB" install "$(wslpath -w "$W/app-debug.apk")"
"$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
```
Beklenen: testler geçer (32); `pm clear` debug uygulamanın verisini siler (yalnızca deneme uygulaması; gerçek uygulama `app.tibi` dokunulmaz); ekranda "Kurulum" yazar; çökme yok.

- [ ] **Adım 8: Commit**

```bash
cd ~/tibi && git add gradle/libs.versions.toml app/
git commit -m "feat(ui): ortak alanlar, biçimler, kök ekran ve kurulum kapısı

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 3: Kurulum modeli, adım gezgini, Hoş geldin ve Hesaplar

**Dosyalar:**
- Oluştur: `ui/kurulum/KurulumVm.kt`, `ui/kurulum/HosGeldinEkrani.kt`, `ui/kurulum/HesaplarAdimi.kt`
- Değiştir: `ui/kurulum/KurulumAkisi.kt` (tamamı)
- Test: `app/src/test/kotlin/app/tibi/ui/kurulum/KurulumVmTest.kt`

**Arayüzler:**
- Tüketir: `Sonuc`, `tibiVm`, `TutarAlani`, `Secici`, `Bolum` (Görev 2); `HesapDao.bakiyeler`, `KartDao.kartlar`, `AyarDao`, `Anahtarlar` (Görev 1)
- Üretir: `class KurulumVm(db: TibiVeritabani, kayit: KayitServisi, bugun: () -> LocalDate = LocalDate::now) : ViewModel` ile
  - `val hesaplar: Flow<List<HesapBakiyesi>>`, `val kartlar: Flow<List<KartBilgisi>>`
  - `suspend fun hitapKaydet(ad: String): Sonuc`
  - `suspend fun hesapEkle(ad: String, tur: HesapTuru, bakiyeMetni: String, maasHesabi: Boolean): Sonuc` (tur yalnızca BANKA/NAKIT; boş bakiye = 0)
  - `suspend fun bitir(): Sonuc` (ayar `kurulum_tamam = "1"`)
  - `enum class KurulumAdimi(val baslik: String) { HOS_GELDIN("Hoş geldin"), HESAPLAR("Hesaplar ve nakit"), GELIR("Gelir"), KARTLAR("Kartlar"), TAKSITLER("Devam eden taksitler") }` — Görev 4–6 aynı enum'u kullanır.
  - `@Composable fun KurulumAkisi(bitti: () -> Unit)`: üstte "Kurulum N / 5" ve ilerleme çubuğu, altta Geri / Devam. Son adımda "Kurulumu bitir" → `vm.bitir()`.

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/ui/kurulum/KurulumVmTest.kt`:
```kotlin
package app.tibi.ui.kurulum

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KurulumVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: KurulumVm
    private val bugun = LocalDate.parse("2026-09-24")

    @Before fun ac() {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        vm = KurulumVm(db, KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }) { bugun }
    }
    @After fun kapat() { db.close() }

    @Test
    fun `hitap kaydedilir, bos olamaz`() = runTest {
        assertIs<Sonuc.Hata>(vm.hitapKaydet("   "))
        assertEquals(Sonuc.Tamam, vm.hitapKaydet("  Gülbahar "))
        assertEquals("Gülbahar", db.ayarDao().oku(Anahtarlar.HITAP))
    }

    @Test
    fun `hesap eklenir, bakiye virgullu okunur`() = runTest {
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Garanti vadesiz", HesapTuru.BANKA, "8.450,00", maasHesabi = true))
        assertEquals(Sonuc.Tamam, vm.hesapEkle("Nakit", HesapTuru.NAKIT, "", maasHesabi = false))
        val h = vm.hesaplar.first()
        assertEquals(listOf("Garanti vadesiz", "Nakit"), h.map { it.ad })
        assertEquals(listOf(845000L, 0L), h.map { it.bakiyeKurus })
        assertTrue(h.first().maasHesabi)
    }

    @Test
    fun `hatali hesap girisleri`() = runTest {
        assertIs<Sonuc.Hata>(vm.hesapEkle("", HesapTuru.BANKA, "100", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("X", HesapTuru.BANKA, "12,345", false))
        assertIs<Sonuc.Hata>(vm.hesapEkle("X", HesapTuru.KREDI_KARTI, "100", false))
        assertEquals(0, vm.hesaplar.first().size)
    }

    @Test
    fun `bitir kurulumu tamamlar`() = runTest {
        vm.bitir()
        assertEquals("1", db.ayarDao().oku(Anahtarlar.KURULUM_TAMAM))
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KurulumVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'KurulumVm'`.

- [ ] **Adım 3: Modeli yaz**

`app/src/main/kotlin/app/tibi/ui/kurulum/KurulumVm.kt`:
```kotlin
package app.tibi.ui.kurulum

import androidx.lifecycle.ViewModel
import app.tibi.core.para.Kurus
import app.tibi.core.para.kurusCoz
import app.tibi.ui.Sonuc
import app.tibi.veri.Anahtarlar
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class KurulumAdimi(val baslik: String) {
    HOS_GELDIN("Hoş geldin"),
    HESAPLAR("Hesaplar ve nakit"),
    GELIR("Gelir"),
    KARTLAR("Kartlar"),
    TAKSITLER("Devam eden taksitler"),
}

class KurulumVm(
    internal val db: TibiVeritabani,
    internal val kayit: KayitServisi,
    internal val bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val hesaplar: Flow<List<HesapBakiyesi>> = db.hesapDao().bakiyeler()
    val kartlar: Flow<List<KartBilgisi>> = db.kartDao().kartlar()

    suspend fun hitapKaydet(ad: String): Sonuc {
        val temiz = ad.trim()
        if (temiz.isEmpty()) return Sonuc.Hata("Sana nasıl hitap edelim? Bir ad yaz.")
        db.ayarDao().yaz(Ayar(Anahtarlar.HITAP, temiz))
        return Sonuc.Tamam
    }

    suspend fun hesapEkle(ad: String, tur: HesapTuru, bakiyeMetni: String, maasHesabi: Boolean): Sonuc {
        if (ad.isBlank()) return Sonuc.Hata("Hesaba bir ad ver, ör. \"Garanti vadesiz\".")
        if (tur == HesapTuru.KREDI_KARTI) return Sonuc.Hata("Kredi kartları Kartlar adımında eklenir.")
        val bakiye = if (bakiyeMetni.isBlank()) Kurus.SIFIR else kurusCoz(bakiyeMetni)
            ?: return Sonuc.Hata("Bakiyeyi 8.450,00 biçiminde yaz.")
        db.hesapDao().ekle(Hesap(ad = ad.trim(), tur = tur, acilisBakiyeKurus = bakiye.deger, acilisTarihi = bugun(), maasHesabi = maasHesabi))
        return Sonuc.Tamam
    }

    suspend fun bitir(): Sonuc {
        db.ayarDao().yaz(Ayar(Anahtarlar.KURULUM_TAMAM, "1"))
        return Sonuc.Tamam
    }
}
```

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, 36 test.

- [ ] **Adım 5: Ekranları yaz**

`app/src/main/kotlin/app/tibi/ui/kurulum/HosGeldinEkrani.kt`:
```kotlin
package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.tibi.R
import androidx.compose.foundation.Image

@Composable
fun HosGeldinEkrani(hitap: String, degisti: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(120.dp))
        Text("tibi her şeyi bu telefonda tutar, hiçbir yere göndermez.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = hitap, onValueChange = degisti, singleLine = true,
            label = { Text("Sana nasıl hitap edelim?") },
            supportingText = { Text("Bildirimlerde kullanılır: \"$hitap, Bonus'un son ödemesine 3 gün var.\"") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

`app/src/main/kotlin/app/tibi/ui/kurulum/HesaplarAdimi.kt`:
```kotlin
package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.Sonuc
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.TutarAlani
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.launch

@Composable
fun HesaplarAdimi(vm: KurulumVm) {
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var ad by remember { mutableStateOf("") }
    var tur by remember { mutableStateOf(HesapTuru.BANKA) }
    var bakiye by remember { mutableStateOf("") }
    var maas by remember { mutableStateOf(hesaplar.none { it.maasHesabi }) }
    var hata by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bugünkü bakiyeleri gir; uygulama buradan başlar. Kredi kartları sonraki adımda.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hesaplar.isNotEmpty()) Bolum("Eklenenler") {
            hesaplar.forEach { h ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(h.ad + if (h.maasHesabi) " · maaş" else "")
                    Text(Kurus(h.bakiyeKurus).bicimle())
                }
            }
        }
        Bolum("Yeni hesap") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(tur == HesapTuru.BANKA, { tur = HesapTuru.BANKA }, label = { Text("Banka hesabı") })
                FilterChip(tur == HesapTuru.NAKIT, { tur = HesapTuru.NAKIT; if (ad.isBlank()) ad = "Nakit" }, label = { Text("Nakit") })
            }
            OutlinedTextField(ad, { ad = it }, label = { Text("Ad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            TutarAlani(bakiye, { bakiye = it }, "Bugünkü bakiye")
            if (tur == HesapTuru.BANKA) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(maas, { maas = it }); Text("Maaşım bu hesaba yatıyor")
            }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                kapsam.launch {
                    when (val s = vm.hesapEkle(ad, tur, bakiye, maas && tur == HesapTuru.BANKA)) {
                        Sonuc.Tamam -> { ad = ""; bakiye = ""; maas = false; hata = null }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Hesabı ekle") }
        }
    }
}
```

`app/src/main/kotlin/app/tibi/ui/kurulum/KurulumAkisi.kt` (tamamını değiştir):
```kotlin
package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tibi.ui.Sonuc
import app.tibi.ui.tibiVm
import kotlinx.coroutines.launch

@Composable
fun KurulumAkisi(bitti: () -> Unit) {
    val vm = tibiVm { KurulumVm(it.veritabani, it.kayitServisi) }
    val kapsam = rememberCoroutineScope()
    var adim by rememberSaveable { mutableStateOf(KurulumAdimi.HOS_GELDIN) }
    var hitap by rememberSaveable { mutableStateOf("") }
    var hata by rememberSaveable { mutableStateOf<String?>(null) }
    val adimlar = KurulumAdimi.entries
    val sira = adimlar.indexOf(adim)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp)) {
            Text("Kurulum ${sira + 1} / ${adimlar.size}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            Text(adim.baslik, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 6.dp))
            LinearProgressIndicator(progress = { (sira + 1f) / adimlar.size }, modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                when (adim) {
                    KurulumAdimi.HOS_GELDIN -> HosGeldinEkrani(hitap) { hitap = it }
                    KurulumAdimi.HESAPLAR -> HesaplarAdimi(vm)
                    else -> Text("Bu adım sonraki görevde eklenecek.")
                }
                hata?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sira > 0) OutlinedButton({ hata = null; adim = adimlar[sira - 1] }, Modifier.weight(1f)) { Text("Geri") }
                Button(onClick = {
                    kapsam.launch {
                        val s = when (adim) {
                            KurulumAdimi.HOS_GELDIN -> vm.hitapKaydet(hitap)
                            else -> Sonuc.Tamam
                        }
                        when {
                            s is Sonuc.Hata -> hata = s.mesaj
                            sira == adimlar.lastIndex -> { vm.bitir(); bitti() }
                            else -> { hata = null; adim = adimlar[sira + 1] }
                        }
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text(if (sira == adimlar.lastIndex) "Kurulumu bitir" else "Devam")
                }
            }
        }
    }
}
```

- [ ] **Adım 6: Telefonda bak**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" uninstall app.tibi.debug; "$WADB" install "$(wslpath -w "$W/app-debug.apk")"
"$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
S=/tmp/claude-1000/-home-setenay/c37b55b1-13ad-43b6-addd-4be6ca4e544d/scratchpad
"$WADB" exec-out screencap -p > $S/p3-g3.png
"$WADB" logcat -d -b crash | tail -5
```
Beklenen: "Kurulum 1 / 5 · Hoş geldin", kuş simgesi, ad alanı; çökme yok. Ekran görüntüsünü Read ile aç, bak.

- [ ] **Adım 7: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(kurulum): adım gezgini, Hoş geldin ve Hesaplar adımları

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 4: Kurulum Gelir adımı (maaş kuralı)

**Dosyalar:**
- Değiştir: `ui/kurulum/KurulumVm.kt`, `ui/kurulum/KurulumAkisi.kt`
- Oluştur: `ui/kurulum/GelirAdimi.kt`
- Test: `KurulumVmTest.kt`'ye ekle

**Arayüzler:**
- Üretir: `suspend fun KurulumVm.maasKaydet(tutarMetni: String, gunMetni: String, haftaSonu: HaftaSonuKurali, hesapId: Long?): Sonuc` (varsa eski maaş kuralını `aktif = false` yapar, yenisini ekler); `fun KurulumVm.donemOnizleme(gunMetni: String, haftaSonu: HaftaSonuKurali): Donem?`; `val KurulumVm.maasKurali: Flow<DuzenliKural?>`
- Tüketir: `DuzenliKuralDao.maasKuraliAkisi` (Görev 1). DAO'ya `@Update suspend fun guncelle(kural: DuzenliKural)` eklenir.

Kaynak: G2 (maaş günü, hafta sonu kuralı, hesap), Temel kararlar "Dönem".

- [ ] **Adım 1: Başarısız testleri ekle**

`KurulumVmTest.kt` sınıfına ekle (importlar: `app.tibi.core.donem.Donem`, `app.tibi.core.tarih.HaftaSonuKurali`):
```kotlin
    @Test
    fun `maas kurali kaydedilir ve donem hesaplanir`() = runTest {
        vm.hesapEkle("Garanti", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        assertEquals(Donem(LocalDate.parse("2026-08-28"), LocalDate.parse("2026-09-29")), vm.donemOnizleme("30", HaftaSonuKurali.ONCEKI))
        assertEquals(Sonuc.Tamam, vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, hesap))
        val k = vm.maasKurali.first()!!
        assertEquals(4_500_000L, k.tutarKurus)
        assertEquals(30, k.gun)
        assertEquals(hesap, k.hesapId)
    }

    @Test
    fun `maas yeniden kaydedilince eskisi pasif olur`() = runTest {
        vm.hesapEkle("Garanti", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, hesap)
        vm.maasKaydet("47.500", "1", HaftaSonuKurali.SONRAKI, hesap)
        assertEquals(4_750_000L, vm.maasKurali.first()!!.tutarKurus)
        assertEquals(1, vm.maasKurali.first()!!.gun)
    }

    @Test
    fun `hatali maas girisleri`() = runTest {
        vm.hesapEkle("Garanti", HesapTuru.BANKA, "0", true)
        val hesap = vm.hesaplar.first().single().id
        assertIs<Sonuc.Hata>(vm.maasKaydet("", "30", HaftaSonuKurali.ONCEKI, hesap))
        assertIs<Sonuc.Hata>(vm.maasKaydet("45.000", "32", HaftaSonuKurali.ONCEKI, hesap))
        assertIs<Sonuc.Hata>(vm.maasKaydet("45.000", "30", HaftaSonuKurali.ONCEKI, null))
        assertEquals(null, vm.donemOnizleme("0", HaftaSonuKurali.ONCEKI))
        assertEquals(null, vm.maasKurali.first())
    }
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `./gradlew :app:testDebugUnitTest --tests '*KurulumVmTest*'` (önce `source ~/.tibi-env && cd ~/tibi`)
Beklenen: DERLEME HATASI, `Unresolved reference 'donemOnizleme'`.

- [ ] **Adım 3: Uygula**

`DuzenliKuralDao.kt`'ye ekle: `@Update suspend fun guncelle(kural: DuzenliKural)` (import `androidx.room.Update`).

`KurulumVm.kt`'ye ekle (importlar: `androidx.room.withTransaction`, `app.tibi.core.donem.Donem`, `app.tibi.core.donem.DonemHesaplayici`, `app.tibi.core.donem.MaasKurali`, `app.tibi.core.tarih.HaftaSonuKurali`, `app.tibi.veri.tablo.DuzenliKural`, `app.tibi.veri.tablo.KategoriYonu`, `app.tibi.veri.tablo.Periyot`):
```kotlin
    val maasKurali: Flow<DuzenliKural?> = db.duzenliKuralDao().maasKuraliAkisi()

    fun donemOnizleme(gunMetni: String, haftaSonu: HaftaSonuKurali): Donem? {
        val gun = gunMetni.toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        return DonemHesaplayici.donem(MaasKurali(gun, haftaSonu), bugun())
    }

    suspend fun maasKaydet(tutarMetni: String, gunMetni: String, haftaSonu: HaftaSonuKurali, hesapId: Long?): Sonuc {
        val tutar = kurusCoz(tutarMetni)?.takeIf { it.deger > 0 } ?: return Sonuc.Hata("Maaş tutarını 45.000 biçiminde yaz.")
        val gun = gunMetni.toIntOrNull()?.takeIf { it in 1..31 } ?: return Sonuc.Hata("Maaşın yattığı gün 1 ile 31 arasında olmalı.")
        if (hesapId == null) return Sonuc.Hata("Maaşın yattığı hesabı seç.")
        db.withTransaction {
            db.duzenliKuralDao().maasKuraliAkisi().first()?.let { db.duzenliKuralDao().guncelle(it.copy(aktif = false)) }
            db.duzenliKuralDao().ekle(
                DuzenliKural(
                    ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = tutar.deger,
                    periyot = Periyot.AYLIK, gun = gun, haftaSonuKurali = haftaSonu, hesapId = hesapId,
                    kategoriId = db.kategoriDao().adIle("Maaş")?.id, baslangic = bugun(),
                )
            )
        }
        return Sonuc.Tamam
    }
```
(`kotlinx.coroutines.flow.first` importunu ekle.)

`app/src/main/kotlin/app/tibi/ui/kurulum/GelirAdimi.kt`:
```kotlin
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
```

`KurulumAkisi.kt`'de:
- `var hitap …` satırının altına: `var gelir by remember { mutableStateOf(GelirFormu()) }` (import `androidx.compose.runtime.remember`)
- `when (adim)` içine: `KurulumAdimi.GELIR -> GelirAdimi(vm, gelir) { gelir = it }`
- Devam'daki `val s = when (adim)` içine: `KurulumAdimi.GELIR -> vm.maasKaydet(gelir.tutar, gelir.gun, gelir.haftaSonu, gelir.hesapId)`

- [ ] **Adım 4: Test ve derleme**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug`
Beklenen: 39 test geçer, derleme başarılı.

- [ ] **Adım 5: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(kurulum): Gelir adımı, maaş kuralı ve dönem önizlemesi

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 5: Kurulum Kartlar adımı ve kart formu

**Dosyalar:**
- Değiştir: `ui/kurulum/KurulumVm.kt`, `ui/kurulum/KurulumAkisi.kt`
- Oluştur: `ui/kurulum/KartFormu.kt`, `ui/kurulum/KartlarAdimi.kt`
- Test: `KurulumVmTest.kt`'ye ekle

**Arayüzler:**
- Üretir:
  - `data class KartGirdisi(val ad: String = "", val tur: KartTuru = KartTuru.ANA, val anaKartId: Long? = null, val son4: String = "", val kesimGunu: String = "", val sonOdemeGunu: String = "", val bankaLimiti: String = "", val kendiLimiti: String = "", val asgariOran: String = "40", val kesilmisEkstre: String = "", val donemIci: String = "")`
  - `suspend fun KurulumVm.kartEkle(g: KartGirdisi): Sonuc`: tek transaction'da hesap (KREDI_KARTI), kart, `kesilmisEkstre > 0` ise `kayit.acilisEkstresi`, `donemIci > 0` ise `kayit.gecmisTaksit(KalanBorc(donemIci, 1), …, "Kurulum: dönem içi harcamalar")` yazar. Ek/sanal kartta kesim/son ödeme ana karttan kopyalanır, limit ve açılış alanları yok sayılır.
  - `@Composable fun KartFormu(g: KartGirdisi, anaKartlar: List<KartBilgisi>, degisti: (KartGirdisi) -> Unit)`

Kaynak: K1, K9, K10, S2; ekran taslağı 12.

- [ ] **Adım 1: Başarısız testleri ekle**

`KurulumVmTest.kt`'ye ekle (importlar: `app.tibi.veri.tablo.KartTuru`):
```kotlin
    @Test
    fun `ana kart acilis borcuyla eklenir`() = runTest {
        val s = vm.kartEkle(KartGirdisi(ad = "Bonus", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22",
            bankaLimiti = "50.000", kendiLimiti = "15.000", kesilmisEkstre = "12.340", donemIci = "5.980"))
        assertEquals(Sonuc.Tamam, s)
        val k = vm.kartlar.first().single()
        assertEquals("Bonus", k.ad)
        assertEquals(1_500_000L, k.kendiLimitiKurus)
        assertEquals(5_000_000L, k.bankaLimitiKurus)
        assertEquals(400, k.asgariOranBinde)
        assertEquals(1_234_000L + 598_000L, k.kullanimKurus)
    }

    @Test
    fun `sanal kart ana kartin gunlerini alir`() = runTest {
        vm.kartEkle(KartGirdisi(ad = "Bonus", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22", kendiLimiti = "15.000"))
        val ana = vm.kartlar.first().single().hesapId
        assertEquals(Sonuc.Tamam, vm.kartEkle(KartGirdisi(ad = "Bonus Sanal", tur = KartTuru.SANAL, anaKartId = ana, son4 = "9054",
            kendiLimiti = "999", kesilmisEkstre = "100")))
        val s = vm.kartlar.first().first { it.ad == "Bonus Sanal" }
        assertEquals(12, s.kesimGunu)
        assertEquals(22, s.sonOdemeGunu)
        assertEquals(null, s.kendiLimitiKurus)
        assertEquals(0L, vm.kartlar.first().first { it.hesapId == ana }.kullanimKurus)
    }

    @Test
    fun `hatali kart girisleri hicbir sey yazmaz`() = runTest {
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", son4 = "48", kesimGunu = "12", sonOdemeGunu = "22")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", son4 = "4821", kesimGunu = "0", sonOdemeGunu = "22")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22", kendiLimiti = "abc")))
        assertIs<Sonuc.Hata>(vm.kartEkle(KartGirdisi(ad = "X", tur = KartTuru.EK, son4 = "4821")))   // ana kart seçilmedi
        assertEquals(0, vm.kartlar.first().size)
    }
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KurulumVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'KartGirdisi'`.

- [ ] **Adım 3: Modele ekle**

`KurulumVm.kt`'ye ekle (importlar: `app.tibi.veri.GecmisTaksitGirisi`, `app.tibi.veri.KayitHatasi`, `app.tibi.veri.tablo.Kart`, `app.tibi.veri.tablo.KartTuru`, `app.tibi.veri.tablo.TaksitTuru`):
```kotlin
data class KartGirdisi(
    val ad: String = "",
    val tur: KartTuru = KartTuru.ANA,
    val anaKartId: Long? = null,
    val son4: String = "",
    val kesimGunu: String = "",
    val sonOdemeGunu: String = "",
    val bankaLimiti: String = "",
    val kendiLimiti: String = "",
    val asgariOran: String = "40",
    val kesilmisEkstre: String = "",
    val donemIci: String = "",
)
```
Sınıfın içine:
```kotlin
    private class GirdiHatasi(mesaj: String) : Exception(mesaj)

    private fun tutarVeyaBos(metin: String, alan: String): Long? =
        if (metin.isBlank()) null else (kurusCoz(metin) ?: throw GirdiHatasi("$alan tutarını 15.000 biçiminde yaz.")).deger

    private fun gun(metin: String, alan: String): Int =
        metin.toIntOrNull()?.takeIf { it in 1..31 } ?: throw GirdiHatasi("$alan 1 ile 31 arasında olmalı.")

    suspend fun kartEkle(g: KartGirdisi): Sonuc = try {
        if (g.ad.isBlank()) throw GirdiHatasi("Karta bir ad ver, ör. \"Bonus\".")
        if (!Regex("""\d{4}""").matches(g.son4)) throw GirdiHatasi("Kartın son 4 hanesini yaz.")
        val ana = if (g.tur == KartTuru.ANA) null else {
            val id = g.anaKartId ?: throw GirdiHatasi("Ek ve sanal kart için ana kartı seç.")
            db.kartDao().getir(id) ?: throw GirdiHatasi("Seçilen ana kart bulunamadı.")
        }
        val kesim = ana?.kesimGunu ?: gun(g.kesimGunu, "Kesim günü")
        val sonOdeme = ana?.sonOdemeGunu ?: gun(g.sonOdemeGunu, "Son ödeme günü")
        val bankaLimiti = if (ana == null) tutarVeyaBos(g.bankaLimiti, "Banka limiti") else null
        val kendiLimiti = if (ana == null) tutarVeyaBos(g.kendiLimiti, "Kendi limitin") else null
        val oran = g.asgariOran.toIntOrNull()?.takeIf { it in 0..100 } ?: throw GirdiHatasi("Asgari ödeme oranı 0 ile 100 arasında olmalı.")
        val kesilmis = if (ana == null) tutarVeyaBos(g.kesilmisEkstre, "Kesilmiş ekstre") else null
        val donemIci = if (ana == null) tutarVeyaBos(g.donemIci, "Dönem içi harcama") else null
        db.withTransaction {
            val id = db.hesapDao().ekle(Hesap(ad = g.ad.trim(), tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun()))
            db.kartDao().ekle(
                Kart(hesapId = id, kartTuru = g.tur, anaKartId = ana?.hesapId, son4 = g.son4, kesimGunu = kesim, sonOdemeGunu = sonOdeme,
                    bankaLimitiKurus = bankaLimiti, kendiLimitiKurus = kendiLimiti, asgariOranBinde = oran * 10)
            )
            if (kesilmis != null && kesilmis > 0) kayit.acilisEkstresi(id, Kurus(kesilmis), bugun())
            if (donemIci != null && donemIci > 0) kayit.gecmisTaksit(GecmisTaksitGirisi.KalanBorc(Kurus(donemIci), 1), id, null,
                TaksitTuru.ALISVERIS, bugun(), "Kurulum: dönem içi harcamalar")
        }
        Sonuc.Tamam
    } catch (e: GirdiHatasi) {
        Sonuc.Hata(e.message!!)
    } catch (e: KayitHatasi) {
        Sonuc.Hata(e.message ?: "Kart kaydedilemedi.")
    }
```

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: 42 test geçer.

- [ ] **Adım 5: Ekranlar**

`app/src/main/kotlin/app/tibi/ui/kurulum/KartFormu.kt`:
```kotlin
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
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.tablo.KartTuru

@Composable
fun KartFormu(g: KartGirdisi, anaKartlar: List<KartBilgisi>, degisti: (KartGirdisi) -> Unit) {
    Bolum("Kart") {
        OutlinedTextField(g.ad, { degisti(g.copy(ad = it)) }, label = { Text("Kart adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(KartTuru.ANA to "Ana kart", KartTuru.EK to "Ek kart", KartTuru.SANAL to "Sanal kart").forEach { (t, ad) ->
                FilterChip(g.tur == t, { degisti(g.copy(tur = t)) }, label = { Text(ad) })
            }
        }
        if (g.tur != KartTuru.ANA) {
            Secici("Limitini paylaştığı ana kart", anaKartlar, anaKartlar.firstOrNull { it.hesapId == g.anaKartId }, { "${it.ad} ··${it.son4}" },
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
        }
        Bolum("Şu anki borç") {
            TutarAlani(g.kesilmisEkstre, { degisti(g.copy(kesilmisEkstre = it)) }, "Kesilmiş ekstre (ödenmemiş)")
            TutarAlani(g.donemIci, { degisti(g.copy(donemIci = it)) }, "Dönem içi tek çekimler")
            Text("Taksitler kurulumun son adımında ayrıca girilir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

`app/src/main/kotlin/app/tibi/ui/kurulum/KartlarAdimi.kt`:
```kotlin
package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.tibi.ui.ortak.Bolum
import app.tibi.veri.tablo.KartTuru
import kotlinx.coroutines.launch

@Composable
fun KartlarAdimi(vm: KurulumVm) {
    val kartlar by vm.kartlar.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var form by remember { mutableStateOf<KartGirdisi?>(null) }
    var hata by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("İstediğin kadar kart ekle; sonra da değiştirebilirsin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (kartlar.isNotEmpty()) Bolum("Eklenen kartlar") {
            kartlar.forEach { k ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${k.ad} ··${k.son4}" + if (k.kartTuru != KartTuru.ANA) " (ortak limit)" else "")
                    Text(k.kendiLimitiKurus?.let { Kurus(it).bicimle() } ?: "")
                }
            }
        }
        val f = form
        if (f == null) {
            OutlinedButton({ form = KartGirdisi(); hata = null }, Modifier.fillMaxWidth()) { Text("+ Kart ekle") }
        } else {
            KartFormu(f, kartlar.filter { it.kartTuru == KartTuru.ANA }) { form = it }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ form = null; hata = null }, Modifier.weight(1f)) { Text("Vazgeç") }
                Button({
                    kapsam.launch {
                        when (val s = vm.kartEkle(f)) {
                            Sonuc.Tamam -> { form = null; hata = null }
                            is Sonuc.Hata -> hata = s.mesaj
                        }
                    }
                }, Modifier.weight(1f)) { Text("Kartı kaydet") }
            }
        }
    }
}
```

`KurulumAkisi.kt` `when (adim)` içine: `KurulumAdimi.KARTLAR -> KartlarAdimi(vm)`

- [ ] **Adım 6: Derle ve telefonda kurulumu Kartlar adımına kadar gez**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")" && "$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
```
Beklenen: çökme yok. (Xiaomi dokunma olayı göndermeye izin vermediği için adımlar arasında otomatik gezinme yapılmaz; akışın elle denemesi Görev 11'de kullanıcıyla yapılır.)

- [ ] **Adım 7: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(kurulum): Kartlar adımı, kart formu, açılış borcu ve ortak limit

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 6: Kurulum Devam eden taksitler adımı

**Dosyalar:**
- Değiştir: `ui/kurulum/KurulumVm.kt`, `ui/kurulum/KurulumAkisi.kt`
- Oluştur: `ui/kurulum/TaksitlerAdimi.kt`
- Test: `KurulumVmTest.kt`'ye ekle

**Arayüzler:**
- Üretir:
  - `enum class TaksitModu { AYLIK, TOPLAM, KALAN }`
  - `data class TaksitGirdisi(val kartId: Long? = null, val aciklama: String = "", val tur: TaksitTuru = TaksitTuru.ALISVERIS, val mod: TaksitModu = TaksitModu.AYLIK, val tutar: String = "", val sayi: String = "", val siradaki: String = "")` (KALAN modunda `sayi` = kalan taksit sayısı, `siradaki` kullanılmaz)
  - `suspend fun KurulumVm.planOnizleme(g: TaksitGirdisi): List<PlanliTaksit>?` (geçersizse `null`, yazmaz)
  - `suspend fun KurulumVm.taksitEkle(g: TaksitGirdisi): Sonuc`
  - `val KurulumVm.taksitler: Flow<List<KartTaksidi>>` — kurulumda eklenen, bütün kartlardaki aktif taksitler
- Tüketir: `TaksitPlanlayici`, `KartTakvimi` (`:core`); `KayitServisi.gecmisTaksit`; `TaksitDao.aktifTaksitler` (Görev 1)

Kaynak: K8; ekran taslağı 13.

- [ ] **Adım 1: Başarısız testleri ekle**

`KurulumVmTest.kt`'ye ekle (importlar: `app.tibi.veri.tablo.TaksitTuru`):
```kotlin
    private suspend fun bonusEkle(): Long {
        vm.kartEkle(KartGirdisi(ad = "Bonus", son4 = "4821", kesimGunu = "12", sonOdemeGunu = "22", kendiLimiti = "15.000"))
        return vm.kartlar.first().single().hesapId
    }

    @Test
    fun `taksit onizlemesi yazmadan plani gosterir`() = runTest {
        val bonus = bonusEkle()
        val p = vm.planOnizleme(TaksitGirdisi(kartId = bonus, mod = TaksitModu.AYLIK, tutar = "1.850", sayi = "12", siradaki = "6"))!!
        assertEquals(12, p.size)
        assertEquals(LocalDate.parse("2027-04-12"), p.last().ekstreKesimTarihi)
        assertEquals(0L, vm.kartlar.first().single().kullanimKurus)
        assertEquals(null, vm.planOnizleme(TaksitGirdisi(kartId = bonus, mod = TaksitModu.AYLIK, tutar = "1.850", sayi = "12", siradaki = "13")))
    }

    @Test
    fun `uc giris yoluyla taksit eklenir`() = runTest {
        val bonus = bonusEkle()
        assertEquals(Sonuc.Tamam, vm.taksitEkle(TaksitGirdisi(kartId = bonus, aciklama = "Telefon", mod = TaksitModu.AYLIK, tutar = "1.850", sayi = "12", siradaki = "6")))
        assertEquals(Sonuc.Tamam, vm.taksitEkle(TaksitGirdisi(kartId = bonus, mod = TaksitModu.TOPLAM, tutar = "3.000", sayi = "3", siradaki = "1")))
        assertEquals(Sonuc.Tamam, vm.taksitEkle(TaksitGirdisi(kartId = bonus, tur = TaksitTuru.EKSTRE_TAKSIT, mod = TaksitModu.KALAN, tutar = "4.000", sayi = "4")))
        assertEquals(7 * 185000L + 300000L + 400000L, vm.kartlar.first().single().kullanimKurus)
        assertEquals(3, vm.taksitler.first().size)
    }

    @Test
    fun `hatali taksit girisleri`() = runTest {
        val bonus = bonusEkle()
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = null, tutar = "100", sayi = "3", siradaki = "1")))
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = bonus, tutar = "", sayi = "3", siradaki = "1")))
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = bonus, tutar = "100", sayi = "", siradaki = "1")))
        assertIs<Sonuc.Hata>(vm.taksitEkle(TaksitGirdisi(kartId = bonus, tutar = "100", sayi = "3", siradaki = "4")))
        assertEquals(0L, vm.kartlar.first().single().kullanimKurus)
    }
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KurulumVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'TaksitGirdisi'`.

- [ ] **Adım 3: Modele ekle**

`KurulumVm.kt`'ye (sınıf dışına):
```kotlin
enum class TaksitModu { AYLIK, TOPLAM, KALAN }

data class TaksitGirdisi(
    val kartId: Long? = null,
    val aciklama: String = "",
    val tur: TaksitTuru = TaksitTuru.ALISVERIS,
    val mod: TaksitModu = TaksitModu.AYLIK,
    val tutar: String = "",
    val sayi: String = "",
    val siradaki: String = "",
)
```
Sınıfın içine (importlar: `app.tibi.core.kart.KartTakvimi`, `app.tibi.core.kart.PlanliTaksit`, `app.tibi.core.kart.TaksitPlanlayici`, `app.tibi.veri.dao.KartTaksidi`, `kotlinx.coroutines.flow.flatMapLatest`, `kotlinx.coroutines.flow.combine`, `kotlinx.coroutines.flow.flowOf`, `kotlinx.coroutines.ExperimentalCoroutinesApi`):
```kotlin
    @OptIn(ExperimentalCoroutinesApi::class)
    val taksitler: Flow<List<KartTaksidi>> = kartlar.flatMapLatest { liste ->
        val analar = liste.filter { it.anaKartId == null }
        if (analar.isEmpty()) flowOf(emptyList())
        else combine(analar.map { db.taksitDao().aktifTaksitler(it.hesapId, bugun()) }) { it.toList().flatten() }
    }

    private fun taksitGirisi(g: TaksitGirdisi): GecmisTaksitGirisi {
        val tutar = kurusCoz(g.tutar)?.takeIf { it.deger > 0 } ?: throw GirdiHatasi("Tutarı 1.850 biçiminde yaz.")
        val sayi = g.sayi.toIntOrNull()?.takeIf { it >= 1 } ?: throw GirdiHatasi("Taksit sayısını yaz.")
        return when (g.mod) {
            TaksitModu.KALAN -> GecmisTaksitGirisi.KalanBorc(tutar, sayi)
            else -> {
                val siradaki = g.siradaki.toIntOrNull()?.takeIf { it in 1..sayi }
                    ?: throw GirdiHatasi("Sıradaki ekstrede kaçıncı taksit olduğu 1 ile $sayi arasında olmalı.")
                if (g.mod == TaksitModu.AYLIK) GecmisTaksitGirisi.Aylik(tutar, sayi, siradaki)
                else GecmisTaksitGirisi.Toplam(tutar, sayi, siradaki)
            }
        }
    }

    private suspend fun takvim(kartId: Long): KartTakvimi {
        val k = db.kartDao().getir(kartId) ?: throw GirdiHatasi("Kart bulunamadı.")
        val ana = k.anaKartId?.let { db.kartDao().getir(it) } ?: k
        return KartTakvimi(ana.kesimGunu, ana.sonOdemeGunu)
    }

    suspend fun planOnizleme(g: TaksitGirdisi): List<PlanliTaksit>? = try {
        val kartId = g.kartId ?: throw GirdiHatasi("Kart seç.")
        val tk = takvim(kartId)
        when (val giris = taksitGirisi(g)) {
            is GecmisTaksitGirisi.Aylik -> TaksitPlanlayici.gecmisAylik(giris.aylik, giris.toplam, giris.siradakiNo, bugun(), tk)
            is GecmisTaksitGirisi.Toplam -> TaksitPlanlayici.gecmisToplam(giris.tutar, giris.toplam, giris.siradakiNo, bugun(), tk)
            is GecmisTaksitGirisi.KalanBorc -> TaksitPlanlayici.kalanBorc(giris.tutar, giris.kalanSayi, bugun(), tk)
        }
    } catch (e: GirdiHatasi) { null } catch (e: IllegalArgumentException) { null }

    suspend fun taksitEkle(g: TaksitGirdisi): Sonuc = try {
        val kartId = g.kartId ?: throw GirdiHatasi("Taksitin yansıdığı kartı seç.")
        kayit.gecmisTaksit(taksitGirisi(g), kartId, null, g.tur, bugun(), g.aciklama.trim().ifEmpty { null })
        Sonuc.Tamam
    } catch (e: GirdiHatasi) {
        Sonuc.Hata(e.message!!)
    } catch (e: KayitHatasi) {
        Sonuc.Hata(e.message ?: "Taksit kaydedilemedi.")
    }
```

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: 45 test geçer.

- [ ] **Adım 5: Ekran**

`app/src/main/kotlin/app/tibi/ui/kurulum/TaksitlerAdimi.kt`:
```kotlin
package app.tibi.ui.kurulum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun TaksitlerAdimi(vm: KurulumVm) {
    val kartlar by vm.kartlar.collectAsStateWithLifecycle(emptyList())
    val taksitler by vm.taksitler.collectAsStateWithLifecycle(emptyList())
    val kapsam = rememberCoroutineScope()
    var g by remember { mutableStateOf(TaksitGirdisi()) }
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
            Secici("Kart", kartlar, kartlar.firstOrNull { it.hesapId == g.kartId }, { "${it.ad} ··${it.son4}" }, { g = g.copy(kartId = it.hesapId) })
            OutlinedTextField(g.aciklama, { g = g.copy(aciklama = it) }, label = { Text("Ne? (ör. Telefon)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(TaksitTuru.ALISVERIS to "Alışveriş", TaksitTuru.EKSTRE_TAKSIT to "Ekstre taksit", TaksitTuru.NAKIT_AVANS to "Nakit avans").forEach { (t, ad) ->
                    FilterChip(g.tur == t, { g = g.copy(tur = t) }, label = { Text(ad) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(TaksitModu.AYLIK to "Aylık tutar", TaksitModu.TOPLAM to "Toplam tutar", TaksitModu.KALAN to "Kalan borç").forEach { (m, ad) ->
                    FilterChip(g.mod == m, { g = g.copy(mod = m) }, label = { Text(ad) })
                }
            }
            TutarAlani(g.tutar, { g = g.copy(tutar = it) }, when (g.mod) { TaksitModu.AYLIK -> "Aylık taksit"; TaksitModu.TOPLAM -> "Toplam tutar"; TaksitModu.KALAN -> "Kalan borç" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SayiAlani(g.sayi, { g = g.copy(sayi = it) }, if (g.mod == TaksitModu.KALAN) "Kalan taksit" else "Toplam taksit", Modifier.weight(1f))
                if (g.mod != TaksitModu.KALAN) SayiAlani(g.siradaki, { g = g.copy(siradaki = it) }, "Sıradaki kaçıncı?", Modifier.weight(1f))
            }
            plan?.let { p ->
                val kalan = p.filterNot { it.oncedenOdendi }
                Text("Kalan ${kalan.size} taksit · ${kalan.map { it.tutar }.topla().bicimle()} · son taksit ${p.last().ekstreKesimTarihi.format(AY_YIL)}",
                    color = MaterialTheme.colorScheme.primary)
            }
            hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button({
                kapsam.launch {
                    when (val s = vm.taksitEkle(g)) {
                        Sonuc.Tamam -> { g = TaksitGirdisi(kartId = g.kartId); hata = null }
                        is Sonuc.Hata -> hata = s.mesaj
                    }
                }
            }, Modifier.fillMaxWidth()) { Text("Taksiti kaydet") }
        }
    }
}
```
(`OutlinedButton` importu kullanılmıyorsa kaldır.)

`KurulumAkisi.kt` `when (adim)` içine: `KurulumAdimi.TAKSITLER -> TaksitlerAdimi(vm)`; artık `else ->` dalı kalmadıysa sil.

- [ ] **Adım 6: Derle, kur, çökme kontrolü, commit**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")" && "$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
git add app/
git commit -m "feat(kurulum): devam eden taksitler adımı ve plan önizlemesi

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 7: Hızlı giriş (harcama, gelir, transfer) ve "+" butonu

**Dosyalar:**
- Oluştur: `ui/giris/HizliGirisVm.kt`, `ui/giris/HizliGirisSayfasi.kt`
- Değiştir: `ui/Kabuk.kt` (FAB, sayfa, Snackbar)
- Test: `app/src/test/kotlin/app/tibi/ui/giris/HizliGirisVmTest.kt`

**Arayüzler:**
- Üretir:
  - `enum class GirisTuru { HARCAMA, GELIR, TRANSFER }`
  - `data class HesapSecenegi(val id: Long, val ad: String, val kart: Boolean)`
  - `class HizliGirisVm(db, kayit, bugun: () -> LocalDate = LocalDate::now) : ViewModel` ile
    - `val giderKategorileri: Flow<List<Kategori>>`, `val gelirKategorileri: Flow<List<Kategori>>` (kullanım sırasıyla)
    - `val hesaplar: Flow<List<HesapSecenegi>>` (önce banka/nakit, sonra kartlar)
    - `suspend fun kaydet(tur: GirisTuru, tutarMetni: String, hesapId: Long?, hedefHesapId: Long?, kategoriId: Long?, taksit: Int, erteleme: Int, tarih: LocalDate, aciklama: String): Sonuc`
  - `@Composable fun HizliGirisSayfasi(kapat: (kaydedildi: Boolean) -> Unit)` (ModalBottomSheet)

Kaynak: E1, E2 (taksit + erteleme), E4, H3; ekran taslağı 3. Harcama freni etki satırı Plan 6'da bu sayfaya eklenir.

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/ui/giris/HizliGirisVmTest.kt`:
```kotlin
package app.tibi.ui.giris

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.ui.Sonuc
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class HizliGirisVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: HizliGirisVm
    private val bugun = LocalDate.parse("2026-09-24")
    private var nakit = 0L
    private var banka = 0L
    private var bonus = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        vm = HizliGirisVm(db, KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }) { bugun }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisTarihi = bugun))
        nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisBakiyeKurus = 130000, acilisTarihi = bugun))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22))
    }
    @After fun kapat() { db.close() }

    private suspend fun kat(ad: String) = db.kategoriDao().adIle(ad)!!.id

    @Test
    fun `hesap secenekleri once hesaplar sonra kartlar`() = runTest {
        assertEquals(listOf("Garanti" to false, "Nakit" to false, "Bonus" to true), vm.hesaplar.first().map { it.ad to it.kart })
    }

    @Test
    fun `taksitli kart harcamasi`() = runTest {
        val s = vm.kaydet(GirisTuru.HARCAMA, "1.249,90", bonus, null, kat("Giyim"), taksit = 3, erteleme = 0, tarih = bugun, aciklama = " mont ")
        assertEquals(Sonuc.Tamam, s)
        val h = db.hareketDao().satirlar(bugun, bugun).first().single()
        assertEquals(124990L, h.tutarKurus)
        assertEquals("mont", h.aciklama)
        assertEquals(3, db.taksitDao().sayi())
    }

    @Test
    fun `nakit harcamada taksit yok sayilir`() = runTest {
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.HARCAMA, "386,50", nakit, null, kat("Market"), taksit = 3, erteleme = 2, tarih = bugun, aciklama = ""))
        assertEquals(0, db.taksitDao().sayi())
        assertEquals(130000L - 38650L, db.hesapDao().bakiye(nakit).first())
    }

    @Test
    fun `gelir ve transfer`() = runTest {
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.GELIR, "1.050", nakit, null, kat("Özel ders"), 1, 0, bugun, ""))
        assertEquals(Sonuc.Tamam, vm.kaydet(GirisTuru.TRANSFER, "500", nakit, banka, null, 1, 0, bugun, ""))
        val turler = db.hareketDao().satirlar(bugun, bugun).first().map { it.tur }.toSet()
        assertEquals(setOf(HareketTuru.GELIR, HareketTuru.TRANSFER), turler)
        assertEquals(130000L + 105000L - 50000L, db.hesapDao().bakiye(nakit).first())
    }

    @Test
    fun `hatali girisler anlasilir mesaj verir`() = runTest {
        assertEquals(Sonuc.Hata("Tutarı 1.249,90 biçiminde yaz."), vm.kaydet(GirisTuru.HARCAMA, "", nakit, null, null, 1, 0, bugun, ""))
        assertEquals(Sonuc.Hata("Hesap ya da kart seç."), vm.kaydet(GirisTuru.HARCAMA, "10", null, null, null, 1, 0, bugun, ""))
        assertEquals(Sonuc.Hata("Paranın gideceği hesabı seç."), vm.kaydet(GirisTuru.TRANSFER, "10", nakit, null, null, 1, 0, bugun, ""))
        assertIs<Sonuc.Hata>(vm.kaydet(GirisTuru.TRANSFER, "10", nakit, nakit, null, 1, 0, bugun, ""))
        assertIs<Sonuc.Hata>(vm.kaydet(GirisTuru.GELIR, "10", bonus, null, null, 1, 0, bugun, ""))
        assertEquals(0, db.hareketDao().sayi())
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*HizliGirisVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'HizliGirisVm'`.

- [ ] **Adım 3: Modeli yaz**

`app/src/main/kotlin/app/tibi/ui/giris/HizliGirisVm.kt`:
```kotlin
package app.tibi.ui.giris

import androidx.lifecycle.ViewModel
import app.tibi.core.para.kurusCoz
import app.tibi.ui.Sonuc
import app.tibi.veri.KayitHatasi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kategori
import app.tibi.veri.tablo.KategoriYonu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class GirisTuru { HARCAMA, GELIR, TRANSFER }

data class HesapSecenegi(val id: Long, val ad: String, val kart: Boolean)

class HizliGirisVm(
    private val db: TibiVeritabani,
    private val kayit: KayitServisi,
    val bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val giderKategorileri: Flow<List<Kategori>> = db.kategoriDao().kullanimSirali(KategoriYonu.GIDER)
    val gelirKategorileri: Flow<List<Kategori>> = db.kategoriDao().kullanimSirali(KategoriYonu.GELIR)
    val hesaplar: Flow<List<HesapSecenegi>> = db.hesapDao().tumu().map { liste ->
        liste.sortedBy { it.tur == HesapTuru.KREDI_KARTI }.map { HesapSecenegi(it.id, it.ad, it.tur == HesapTuru.KREDI_KARTI) }
    }

    suspend fun kaydet(
        tur: GirisTuru, tutarMetni: String, hesapId: Long?, hedefHesapId: Long?, kategoriId: Long?,
        taksit: Int, erteleme: Int, tarih: LocalDate, aciklama: String,
    ): Sonuc {
        val tutar = kurusCoz(tutarMetni)?.takeIf { it.deger > 0 } ?: return Sonuc.Hata("Tutarı 1.249,90 biçiminde yaz.")
        val kaynak = hesapId ?: return Sonuc.Hata("Hesap ya da kart seç.")
        val not = aciklama.trim().ifEmpty { null }
        return try {
            when (tur) {
                GirisTuru.HARCAMA -> {
                    val kart = db.hesapDao().getir(kaynak)?.tur == HesapTuru.KREDI_KARTI
                    kayit.harcama(tutar, tarih, kaynak, kategoriId, if (kart) taksit else 1, if (kart) erteleme else 0, not)
                }
                GirisTuru.GELIR -> {
                    if (db.hesapDao().getir(kaynak)?.tur == HesapTuru.KREDI_KARTI) return Sonuc.Hata("Gelir bir banka hesabına ya da nakde girer.")
                    kayit.gelir(tutar, tarih, kaynak, kategoriId, not)
                }
                GirisTuru.TRANSFER -> {
                    val hedef = hedefHesapId ?: return Sonuc.Hata("Paranın gideceği hesabı seç.")
                    if (db.hesapDao().getir(hedef)?.tur == HesapTuru.KREDI_KARTI) kayit.kartOdemesi(tutar, tarih, kaynak, hedef)
                    else kayit.transfer(tutar, tarih, kaynak, hedef, not)
                }
            }
            Sonuc.Tamam
        } catch (e: KayitHatasi) {
            Sonuc.Hata(e.message ?: "Kaydedilemedi.")
        }
    }
}
```
(Transferin hedefi kartsa kart ödemesi olarak yazılır; ayrıntılı ödeme ekranı Plan 5'te.)

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: 50 test geçer.

- [ ] **Adım 5: Sayfa ve Kabuk bağlantısı**

`app/src/main/kotlin/app/tibi/ui/giris/HizliGirisSayfasi.kt`:
```kotlin
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
```

`Kabuk.kt` değişiklikleri:
- `Scaffold(` çağrısına ekle:
  ```kotlin
        snackbarHost = { SnackbarHost(bildirim) },
        floatingActionButton = {
            if (aktif in Sekme.entries.map { it.rota }) FloatingActionButton(onClick = { giris = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Yeni kayıt")
            }
        },
  ```
- `Kabuk()` başına:
  ```kotlin
    var giris by remember { mutableStateOf(false) }
    val bildirim = remember { SnackbarHostState() }
    val kapsam = rememberCoroutineScope()
  ```
- Scaffold'un içeriğinden sonra (fonksiyonun sonuna):
  ```kotlin
    if (giris) HizliGirisSayfasi { kaydedildi ->
        giris = false
        if (kaydedildi) kapsam.launch { bildirim.showSnackbar("Kaydedildi") }
    }
  ```
- Importlar: `androidx.compose.material.icons.filled.Add`, `androidx.compose.material3.FloatingActionButton`, `SnackbarHost`, `SnackbarHostState`, `androidx.compose.runtime.mutableStateOf`, `remember`, `rememberCoroutineScope`, `setValue`, `app.tibi.ui.giris.HizliGirisSayfasi`, `kotlinx.coroutines.launch`.

- [ ] **Adım 6: Derle, kur, commit**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")" && "$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
git add app/
git commit -m "feat(giris): hızlı harcama, gelir ve transfer girişi; + butonu

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 8: Özet ekranı

**Dosyalar:**
- Oluştur: `ui/ozet/OzetVm.kt`, `ui/ozet/OzetEkrani.kt`
- Değiştir: `ui/Kabuk.kt` (`OzetYerTutucu` → `OzetEkrani`)
- Test: `app/src/test/kotlin/app/tibi/ui/ozet/OzetVmTest.kt`

**Arayüzler:**
- Üretir:
  - `data class KartOzeti(val ad: String, val kullanimKurus: Long, val kendiLimitiKurus: Long?)`
  - `data class OzetDurumu(val hitap: String?, val donem: Donem, val gelirKurus: Long, val giderKurus: Long, val kalanKurus: Long, val hesaplar: List<HesapBakiyesi>, val kartlar: List<KartOzeti>, val taksitYuku: List<AylikYuk>)`
  - `class OzetVm(db, donemServisi, bugun) : ViewModel { val durum: Flow<OzetDurumu> }` (kartlar yalnızca ana kartlar; taksit yükü ilk 3 ay)
  - `@Composable fun OzetEkrani()`

Kaynak: ekran taslağı 5 (Özet); harcama hızı, tavan ve yaklaşan ödemeler Plan 5–6'da eklenir.

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/ui/ozet/OzetVmTest.kt`:
```kotlin
package app.tibi.ui.ozet

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.Anahtarlar
import app.tibi.veri.DonemServisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import app.tibi.veri.tablo.Periyot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class OzetVmTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    @Test
    fun `ozet donem toplamlarini kartlari ve taksit yukunu toplar`() = runTest {
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        db.ayarDao().yaz(Ayar(Anahtarlar.HITAP, "Gülbahar"))
        val banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun, maasHesabi = true))
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = banka, baslangic = bugun))
        val bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, kendiLimitiKurus = 1_500_000))
        val sanal = db.hesapDao().ekle(Hesap(ad = "Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        kayit.gelir(Kurus(4_500_000), t("2026-08-28"), banka, null)
        kayit.harcama(Kurus(124990), bugun, bonus, null, taksitSayisi = 3)
        kayit.harcama(Kurus(10000), bugun, banka, null)

        val d = OzetVm(db, DonemServisi(db)) { bugun }.durum.first()
        assertEquals("Gülbahar", d.hitap)
        assertEquals(t("2026-08-28"), d.donem.baslangic)
        assertEquals(4_500_000L, d.gelirKurus)
        assertEquals(134990L, d.giderKurus)
        assertEquals(4_500_000L - 134990L, d.kalanKurus)
        assertEquals(listOf(KartOzeti("Bonus", 124990L, 1_500_000L)), d.kartlar)
        assertEquals(3, d.taksitYuku.size)
        assertEquals(41664L, d.taksitYuku.first().toplamKurus)
        assertEquals(listOf(845000L + 4_500_000L - 10000L), d.hesaplar.map { it.bakiyeKurus })
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*OzetVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'OzetVm'`.

- [ ] **Adım 3: Modeli yaz**

`app/src/main/kotlin/app/tibi/ui/ozet/OzetVm.kt`:
```kotlin
package app.tibi.ui.ozet

import androidx.lifecycle.ViewModel
import app.tibi.core.donem.Donem
import app.tibi.veri.Anahtarlar
import app.tibi.veri.DonemServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.AylikYuk
import app.tibi.veri.dao.DonemToplami
import app.tibi.veri.dao.HesapBakiyesi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class KartOzeti(val ad: String, val kullanimKurus: Long, val kendiLimitiKurus: Long?)

data class OzetDurumu(
    val hitap: String?,
    val donem: Donem,
    val gelirKurus: Long,
    val giderKurus: Long,
    val kalanKurus: Long,
    val hesaplar: List<HesapBakiyesi>,
    val kartlar: List<KartOzeti>,
    val taksitYuku: List<AylikYuk>,
)

class OzetVm(
    db: TibiVeritabani,
    donemServisi: DonemServisi,
    bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val donemVeToplam: Flow<Pair<Donem, DonemToplami>> = donemServisi.donem(bugun()).flatMapLatest { d ->
        db.hareketDao().donemToplami(d.baslangic, d.bitis).map { d to it }
    }

    val durum: Flow<OzetDurumu> = combine(
        donemVeToplam,
        db.ayarDao().okuAkis(Anahtarlar.HITAP),
        db.hesapDao().bakiyeler(),
        db.kartDao().kartlar(),
        db.taksitDao().aylikYuk(bugun()),
    ) { (donem, toplam), hitap, hesaplar, kartlar, yuk ->
        OzetDurumu(
            hitap = hitap,
            donem = donem,
            gelirKurus = toplam.gelirKurus,
            giderKurus = toplam.giderKurus,
            kalanKurus = toplam.gelirKurus - toplam.giderKurus,
            hesaplar = hesaplar,
            kartlar = kartlar.filter { it.anaKartId == null }.map { KartOzeti(it.ad, it.kullanimKurus, it.kendiLimitiKurus) },
            taksitYuku = yuk.take(3),
        )
    }
}
```

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: 51 test geçer.

- [ ] **Adım 5: Ekran**

`app/src/main/kotlin/app/tibi/ui/ozet/OzetEkrani.kt`:
```kotlin
package app.tibi.ui.ozet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.aralik
import app.tibi.ui.ortak.kisa
import app.tibi.ui.tibiVm

@Composable
fun OzetEkrani() {
    val vm = tibiVm { OzetVm(it.veritabani, it.donemServisi) }
    val d by vm.durum.collectAsStateWithLifecycle(initialValue = null)
    val o = d ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(o.hitap?.let { "Merhaba $it" } ?: "Özet", style = MaterialTheme.typography.headlineSmall)
        Text("Bu dönem · ${o.donem.aralik()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bolum("Bu dönem kalan") {
            Text(Kurus(o.kalanKurus).bicimle(), style = MaterialTheme.typography.headlineMedium,
                color = if (o.kalanKurus < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Satir("Gelir", Kurus(o.gelirKurus).bicimle())
            Satir("Gider", Kurus(o.giderKurus).bicimle())
        }
        if (o.kartlar.isNotEmpty()) Bolum("Kartlar · kendi limitine göre") {
            o.kartlar.forEach { k ->
                val limit = k.kendiLimitiKurus
                if (limit != null && limit > 0) {
                    Satir(k.ad, "${Kurus(limit - k.kullanimKurus).bicimle()} kaldı")
                    val oran = (k.kullanimKurus.toFloat() / limit).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { oran }, modifier = Modifier.fillMaxWidth(),
                        color = if (oran >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                } else {
                    Satir(k.ad, "borç ${Kurus(k.kullanimKurus).bicimle()}")
                }
            }
        }
        if (o.taksitYuku.isNotEmpty()) Bolum("Taksit yükü · önümüzdeki ekstreler") {
            o.taksitYuku.forEach { Satir(it.ekstreKesimTarihi.kisa(), Kurus(it.toplamKurus).bicimle()) }
        }
        if (o.hesaplar.isNotEmpty()) Bolum("Hesaplar") {
            o.hesaplar.forEach { Satir(it.ad, Kurus(it.bakiyeKurus).bicimle()) }
        }
    }
}

@Composable
internal fun Satir(sol: String, sag: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(sol); Text(sag, style = MaterialTheme.typography.bodyLarge)
    }
}
```

`Kabuk.kt`: `composable(Sekme.OZET.rota) { OzetYerTutucu() }` → `composable(Sekme.OZET.rota) { OzetEkrani() }`; `OzetYerTutucu` fonksiyonunu ve artık kullanılmayan importları (`DonemHesaplayici`, `MaasKurali`, `HaftaSonuKurali`, `LocalDate`, `DateTimeFormatter`, `Locale`) sil; `import app.tibi.ui.ozet.OzetEkrani` ekle.

- [ ] **Adım 6: Derle, kur, commit**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")" && "$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
git add app/
git commit -m "feat(ozet): Özet ekranı (dönem kalan, kartlar, taksit yükü, hesaplar)

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 9: Hareketler ekranı

**Dosyalar:**
- Oluştur: `ui/hareketler/HareketlerVm.kt`, `ui/hareketler/HareketlerEkrani.kt`
- Değiştir: `ui/Kabuk.kt` (Hareketler sekmesi)
- Test: `app/src/test/kotlin/app/tibi/ui/hareketler/HareketlerVmTest.kt`

**Arayüzler:**
- Üretir:
  - `enum class HareketFiltresi { TUMU, HARCAMA, GELIR, TRANSFER }` (TRANSFER = TRANSFER + KART_ODEME; GELIR = GELIR + TAHSILAT + AVANS)
  - `data class GunGrubu(val tarih: LocalDate, val satirlar: List<HareketSatiri>)`
  - `class HareketlerVm(db, donemServisi, bugun) : ViewModel` ile `val filtre: MutableStateFlow<HareketFiltresi>`, `val arama: MutableStateFlow<String>`, `val donem: Flow<Donem>`, `val gruplar: Flow<List<GunGrubu>>`, `suspend fun sil(id: Long)`
  - `fun HareketSatiri.eslesir(arama: String): Boolean` (kategori, açıklama, kaynak/hedef adı veya tutar metni; büyük/küçük harf ve Türkçe İ/ı duyarsız)

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/ui/hareketler/HareketlerVmTest.kt`:
```kotlin
package app.tibi.ui.hareketler

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.DonemServisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class HareketlerVmTest {
    private lateinit var db: TibiVeritabani
    private lateinit var vm: HareketlerVm
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = bugun))
        val banka = db.hesapDao().ekle(Hesap(ad = "İş Bankası", tur = HesapTuru.BANKA, acilisTarihi = bugun))
        kayit.harcama(Kurus(38650), bugun, nakit, db.kategoriDao().adIle("Market")!!.id, aciklama = "haftalık")
        kayit.harcama(Kurus(12000), t("2026-09-23"), nakit, db.kategoriDao().adIle("Yemek/Kafe")!!.id)
        kayit.gelir(Kurus(105000), t("2026-09-23"), nakit, db.kategoriDao().adIle("Özel ders")!!.id)
        kayit.transfer(Kurus(50000), t("2026-09-02"), banka, nakit)
        vm = HareketlerVm(db, DonemServisi(db)) { bugun }
    }
    @After fun kapat() { db.close() }

    @Test
    fun `gunlere gore gruplar yeniden eskiye`() = runTest {
        val g = vm.gruplar.first()
        assertEquals(listOf(bugun, t("2026-09-23"), t("2026-09-02")), g.map { it.tarih })
        assertEquals(2, g[1].satirlar.size)
    }

    @Test
    fun `filtre ve arama`() = runTest {
        vm.filtre.value = HareketFiltresi.GELIR
        assertEquals(listOf(105000L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.filtre.value = HareketFiltresi.TUMU
        vm.arama.value = "HAFTA"
        assertEquals(listOf(38650L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.arama.value = "iş bank"
        assertEquals(listOf(50000L), vm.gruplar.first().flatMap { it.satirlar }.map { it.tutarKurus })
        vm.arama.value = "386,50"
        assertEquals(1, vm.gruplar.first().size)
    }

    @Test
    fun `silme`() = runTest {
        val id = vm.gruplar.first().first().satirlar.first().id
        vm.sil(id)
        assertEquals(3, vm.gruplar.first().sumOf { it.satirlar.size })
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*HareketlerVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'HareketlerVm'`.

- [ ] **Adım 3: Modeli yaz**

`app/src/main/kotlin/app/tibi/ui/hareketler/HareketlerVm.kt`:
```kotlin
package app.tibi.ui.hareketler

import androidx.lifecycle.ViewModel
import app.tibi.core.donem.Donem
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.TR
import app.tibi.veri.DonemServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HareketSatiri
import app.tibi.veri.tablo.HareketTuru
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

enum class HareketFiltresi(val turler: Set<HareketTuru>?) {
    TUMU(null),
    HARCAMA(setOf(HareketTuru.HARCAMA)),
    GELIR(setOf(HareketTuru.GELIR, HareketTuru.TAHSILAT, HareketTuru.AVANS)),
    TRANSFER(setOf(HareketTuru.TRANSFER, HareketTuru.KART_ODEME)),
}

data class GunGrubu(val tarih: LocalDate, val satirlar: List<HareketSatiri>)

fun HareketSatiri.eslesir(arama: String): Boolean {
    val a = arama.trim().lowercase(TR)
    if (a.isEmpty()) return true
    val alanlar = listOfNotNull(kategoriAdi, aciklama, kaynakAdi, hedefAdi, Kurus(tutarKurus).bicimle())
    return alanlar.any { it.lowercase(TR).contains(a) }
}

class HareketlerVm(
    private val db: TibiVeritabani,
    donemServisi: DonemServisi,
    bugun: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    val filtre = MutableStateFlow(HareketFiltresi.TUMU)
    val arama = MutableStateFlow("")
    val donem: Flow<Donem> = donemServisi.donem(bugun())

    @OptIn(ExperimentalCoroutinesApi::class)
    val gruplar: Flow<List<GunGrubu>> = donem.flatMapLatest { d ->
        combine(db.hareketDao().satirlar(d.baslangic, d.bitis), filtre, arama) { satirlar, f, a ->
            satirlar.filter { (f.turler == null || it.tur in f.turler) && it.eslesir(a) }
                .groupBy { it.tarih }
                .map { (tarih, liste) -> GunGrubu(tarih, liste) }
        }
    }

    suspend fun sil(id: Long) = db.hareketDao().sil(id)
}
```

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: 54 test geçer.

- [ ] **Adım 5: Ekran**

`app/src/main/kotlin/app/tibi/ui/hareketler/HareketlerEkrani.kt`:
```kotlin
package app.tibi.ui.hareketler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.aralik
import app.tibi.ui.ortak.gunBasligi
import app.tibi.ui.tibiVm
import app.tibi.veri.dao.HareketSatiri
import app.tibi.veri.tablo.HareketTuru
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FILTRE_ADI = mapOf(
    HareketFiltresi.TUMU to "Tümü", HareketFiltresi.HARCAMA to "Harcama",
    HareketFiltresi.GELIR to "Gelir", HareketFiltresi.TRANSFER to "Transfer",
)

@Composable
fun HareketlerEkrani() {
    val vm = tibiVm { HareketlerVm(it.veritabani, it.donemServisi) }
    val gruplar by vm.gruplar.collectAsStateWithLifecycle(emptyList())
    val donem by vm.donem.collectAsStateWithLifecycle(null)
    val filtre by vm.filtre.collectAsStateWithLifecycle()
    val arama by vm.arama.collectAsStateWithLifecycle()
    var silinecek by remember { mutableStateOf<HareketSatiri?>(null) }
    val kapsam = rememberCoroutineScope()
    val bugun = remember { LocalDate.now() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Hareketler", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        donem?.let { Text("${it.aralik()} dönemi · ${gruplar.sumOf { g -> g.satirlar.size }} kayıt", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        OutlinedTextField(arama, { vm.arama.value = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Ara: not, kategori, hesap, tutar") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            HareketFiltresi.entries.forEach { f -> FilterChip(filtre == f, { vm.filtre.value = f }, label = { Text(FILTRE_ADI.getValue(f)) }) }
        }
        if (gruplar.isEmpty()) Text("Bu dönemde kayıt yok. \"+\" ile ekleyebilirsin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            gruplar.forEach { g ->
                item(key = "g${g.tarih}") {
                    Text(g.tarih.gunBasligi(bugun).uppercase(app.tibi.ui.ortak.TR), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                }
                items(g.satirlar, key = { it.id }) { s -> HareketSatirKutusu(s) { silinecek = s } }
            }
        }
    }

    silinecek?.let { s ->
        AlertDialog(
            onDismissRequest = { silinecek = null },
            title = { Text("Kayıt silinsin mi?") },
            text = { Text("${baslik(s)} · ${Kurus(s.tutarKurus).bicimle()}" + if (s.taksitSayisi > 1) "\nBütün taksitleri de silinir." else "") },
            confirmButton = { TextButton({ kapsam.launch { vm.sil(s.id) }; silinecek = null }) { Text("Sil") } },
            dismissButton = { TextButton({ silinecek = null }) { Text("Vazgeç") } },
        )
    }
}

private fun baslik(s: HareketSatiri) = s.kategoriAdi ?: when (s.tur) {
    HareketTuru.TRANSFER -> "Transfer"
    HareketTuru.KART_ODEME -> "Kart ödemesi"
    HareketTuru.GELIR -> "Gelir"
    HareketTuru.AVANS -> "Avans"
    HareketTuru.TAHSILAT -> "Tahsilat"
    else -> s.aciklama ?: "Harcama"
}

@Composable
private fun HareketSatirKutusu(s: HareketSatiri, sil: () -> Unit) {
    val giris = s.tur in setOf(HareketTuru.GELIR, HareketTuru.TAHSILAT, HareketTuru.AVANS)
    val cikis = s.tur == HareketTuru.HARCAMA
    val alt = buildList {
        when {
            s.kaynakAdi != null && s.hedefAdi != null -> add("${s.kaynakAdi} → ${s.hedefAdi}")
            else -> (s.kaynakAdi ?: s.hedefAdi)?.let(::add)
        }
        if (s.taksitSayisi > 1) add("${s.taksitSayisi} taksit")
        if (s.kategoriAdi != null) s.aciklama?.let(::add)
    }.joinToString(" · ")
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(baslik(s))
            if (alt.isNotEmpty()) Text(alt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text((if (giris) "+" else if (cikis) "−" else "") + Kurus(s.tutarKurus).bicimle(),
            color = if (giris) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        IconButton(sil) { Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
```

`Kabuk.kt`: `Sekme.entries.drop(1).forEach { … YerTutucu … }` satırını şöyle değiştir:
```kotlin
            composable(Sekme.HAREKETLER.rota) { HareketlerEkrani() }
            listOf(Sekme.HESAPLAR, Sekme.CUZDANLAR, Sekme.DERSLER).forEach { s -> composable(s.rota) { YerTutucu(s.baslik) } }
```
(import `app.tibi.ui.hareketler.HareketlerEkrani`)

- [ ] **Adım 6: Derle, kur, commit**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")" && "$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
git add app/
git commit -m "feat(hareketler): gün gruplu liste, filtre, arama ve silme

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 10: Hesaplar sekmesi ve kart detayı

**Dosyalar:**
- Oluştur: `ui/hesaplar/HesaplarVm.kt`, `ui/hesaplar/HesaplarEkrani.kt`, `ui/hesaplar/KartDetayVm.kt`, `ui/hesaplar/KartDetayEkrani.kt`
- Değiştir: `ui/Kabuk.kt` (Hesaplar sekmesi + `kart/{id}` rotası)
- Test: `app/src/test/kotlin/app/tibi/ui/hesaplar/KartDetayVmTest.kt`

**Arayüzler:**
- Üretir:
  - `class HesaplarVm(db) : ViewModel { val hesaplar: Flow<List<HesapBakiyesi>>; val kartlar: Flow<List<KartBilgisi>> }`
  - `data class KartDetayi(val kart: KartBilgisi, val ana: KartBilgisi, val kalanKurus: Long?, val siradakiKesim: LocalDate, val sonOdeme: LocalDate, val acikDonemKurus: Long, val taksitler: List<KartTaksidi>, val bagliKartlar: List<KartBilgisi>)` (ek/sanal kartta limit, dönem ve taksitler ana karttan)
  - `class KartDetayVm(db, kartId: Long, bugun) : ViewModel { val detay: Flow<KartDetayi?> }`
  - `@Composable fun HesaplarEkrani(kartAc: (Long) -> Unit)`, `@Composable fun KartDetayEkrani(kartId: Long, geri: () -> Unit)`

Kaynak: K5 (toplam borç, kalan kişisel limit, dönem içi, aktif taksitler, banka limiti küçük yazı), ekran taslağı 16.

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/ui/hesaplar/KartDetayVmTest.kt`:
```kotlin
package app.tibi.ui.hesaplar

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.GecmisTaksitGirisi
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.TaksitTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class KartDetayVmTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var bonus = 0L
    private var sanal = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        val kayit = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, bankaLimitiKurus = 5_000_000, kendiLimitiKurus = 1_500_000))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        kayit.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
        kayit.harcama(Kurus(7999), bugun, sanal, null)
    }
    @After fun kapat() { db.close() }

    @Test
    fun `ana kart detayi`() = runTest {
        val d = KartDetayVm(db, bonus) { bugun }.detay.filterNotNull().first()
        assertEquals(7 * 185000L + 7999L, d.kart.kullanimKurus)
        assertEquals(1_500_000L - (7 * 185000L + 7999L), d.kalanKurus)
        assertEquals(t("2026-10-12"), d.siradakiKesim)
        assertEquals(t("2026-10-22"), d.sonOdeme)
        assertEquals(185000L + 7999L, d.acikDonemKurus)
        assertEquals(listOf("Telefon"), d.taksitler.map { it.aciklama })
        assertEquals(listOf("Bonus Sanal"), d.bagliKartlar.map { it.ad })
    }

    @Test
    fun `sanal kart detayi ana kartin rakamlarini gosterir`() = runTest {
        val d = KartDetayVm(db, sanal) { bugun }.detay.filterNotNull().first()
        assertEquals("Bonus Sanal", d.kart.ad)
        assertEquals("Bonus", d.ana.ad)
        assertEquals(1_500_000L - (7 * 185000L + 7999L), d.kalanKurus)
        assertEquals(185000L + 7999L, d.acikDonemKurus)
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KartDetayVmTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'KartDetayVm'`.

- [ ] **Adım 3: Modelleri yaz**

`app/src/main/kotlin/app/tibi/ui/hesaplar/HesaplarVm.kt`:
```kotlin
package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.HesapBakiyesi
import app.tibi.veri.dao.KartBilgisi
import kotlinx.coroutines.flow.Flow

class HesaplarVm(db: TibiVeritabani) : ViewModel() {
    val hesaplar: Flow<List<HesapBakiyesi>> = db.hesapDao().bakiyeler()
    val kartlar: Flow<List<KartBilgisi>> = db.kartDao().kartlar()
}
```

`app/src/main/kotlin/app/tibi/ui/hesaplar/KartDetayVm.kt`:
```kotlin
package app.tibi.ui.hesaplar

import androidx.lifecycle.ViewModel
import app.tibi.core.kart.KartTakvimi
import app.tibi.veri.TibiVeritabani
import app.tibi.veri.dao.KartBilgisi
import app.tibi.veri.dao.KartTaksidi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

data class KartDetayi(
    val kart: KartBilgisi,
    val ana: KartBilgisi,
    val kalanKurus: Long?,
    val siradakiKesim: LocalDate,
    val sonOdeme: LocalDate,
    val acikDonemKurus: Long,
    val taksitler: List<KartTaksidi>,
    val bagliKartlar: List<KartBilgisi>,
)

class KartDetayVm(db: TibiVeritabani, kartId: Long, bugun: () -> LocalDate = LocalDate::now) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val detay: Flow<KartDetayi?> = db.kartDao().kartlar().flatMapLatest { liste ->
        val kart = liste.firstOrNull { it.hesapId == kartId } ?: return@flatMapLatest flowOf(null)
        val ana = kart.anaKartId?.let { id -> liste.firstOrNull { it.hesapId == id } } ?: kart
        val takvim = KartTakvimi(ana.kesimGunu, ana.sonOdemeGunu)
        val kesim = takvim.ilgiliKesim(bugun())
        combine(db.taksitDao().kesimTutari(ana.hesapId, kesim), db.taksitDao().aktifTaksitler(ana.hesapId, bugun())) { donem, taksitler ->
            KartDetayi(
                kart = kart.copy(kullanimKurus = ana.kullanimKurus),
                ana = ana,
                kalanKurus = ana.kendiLimitiKurus?.let { it - ana.kullanimKurus },
                siradakiKesim = kesim,
                sonOdeme = takvim.sonOdeme(kesim),
                acikDonemKurus = donem,
                taksitler = taksitler,
                bagliKartlar = liste.filter { it.anaKartId == ana.hesapId },
            )
        }
    }
}
```

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: 56 test geçer.

- [ ] **Adım 5: Ekranlar ve rota**

`app/src/main/kotlin/app/tibi/ui/hesaplar/HesaplarEkrani.kt`:
```kotlin
package app.tibi.ui.hesaplar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.tibiVm
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.KartTuru

@Composable
fun HesaplarEkrani(kartAc: (Long) -> Unit) {
    val vm = tibiVm { HesaplarVm(it.veritabani) }
    val hesaplar by vm.hesaplar.collectAsStateWithLifecycle(emptyList())
    val kartlar by vm.kartlar.collectAsStateWithLifecycle(emptyList())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Hesaplar", style = MaterialTheme.typography.headlineSmall)
        Bolum("Kredi kartları") {
            if (kartlar.isEmpty()) Text("Kart yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            kartlar.forEach { k ->
                Row(Modifier.fillMaxWidth().clickable { kartAc(k.hesapId) }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${k.ad} ··${k.son4}")
                        Text(if (k.kartTuru == KartTuru.ANA) "Kesim ${k.kesimGunu} · Son ödeme ${k.sonOdemeGunu}" else "Ortak limit",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (k.kartTuru == KartTuru.ANA) Text(k.kendiLimitiKurus?.let { "${Kurus(it - k.kullanimKurus).bicimle()} kaldı" }
                        ?: "borç ${Kurus(k.kullanimKurus).bicimle()}")
                }
            }
        }
        Bolum("Banka hesapları ve nakit") {
            hesaplar.forEach { h ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(h.ad + if (h.tur == HesapTuru.NAKIT) "" else if (h.maasHesabi) " · maaş" else "")
                    Text(Kurus(h.bakiyeKurus).bicimle())
                }
            }
        }
    }
}
```

`app/src/main/kotlin/app/tibi/ui/hesaplar/KartDetayEkrani.kt`:
```kotlin
package app.tibi.ui.hesaplar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tibi.core.para.Kurus
import app.tibi.core.para.bicimle
import app.tibi.ui.ortak.Bolum
import app.tibi.ui.ortak.kisa
import app.tibi.ui.tibiVm

@Composable
fun KartDetayEkrani(kartId: Long, geri: () -> Unit) {
    val vm = tibiVm { KartDetayVm(it.veritabani, kartId) }
    val d by vm.detay.collectAsStateWithLifecycle(null)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(geri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text(d?.let { "${it.kart.ad} ··${it.kart.son4}" } ?: "", style = MaterialTheme.typography.headlineSmall)
        }
        val x = d ?: return
        Text("Kesim ${x.ana.kesimGunu} · Son ödeme ${x.ana.sonOdemeGunu}" + if (x.kart.hesapId != x.ana.hesapId) " · ${x.ana.ad} ile ortak limit" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Bolum("Kendi limitine göre") {
            Text(Kurus(x.kart.kullanimKurus).bicimle(), style = MaterialTheme.typography.headlineMedium)
            Text("toplam borç", color = MaterialTheme.colorScheme.onSurfaceVariant)
            x.ana.kendiLimitiKurus?.let { limit ->
                val oran = if (limit > 0) (x.kart.kullanimKurus.toFloat() / limit).coerceIn(0f, 1f) else 1f
                LinearProgressIndicator(progress = { oran }, modifier = Modifier.fillMaxWidth(),
                    color = if (oran >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("${Kurus(x.kalanKurus ?: 0).bicimle()} kaldı / ${Kurus(limit).bicimle()}")
            }
            x.ana.bankaLimitiKurus?.let {
                Text("Banka limiti ${Kurus(it).bicimle()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Bolum("Açık dönem · kesim ${x.siradakiKesim.kisa()}") {
            Text(Kurus(x.acikDonemKurus).bicimle(), style = MaterialTheme.typography.titleLarge)
            Text("Son ödeme ${x.sonOdeme.kisa()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (x.taksitler.isNotEmpty()) Bolum("Taksitler") {
            x.taksitler.forEach { t ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${t.aciklama ?: t.kategoriAdi ?: "Taksit"} · ${t.siradakiSira}/${t.toplam}")
                    Text("${Kurus(t.aylikKurus).bicimle()} / ay")
                }
            }
        }
        if (x.bagliKartlar.isNotEmpty()) Bolum("Aynı limiti kullanan kartlar") {
            x.bagliKartlar.forEach { Text("${it.ad} ··${it.son4}") }
        }
    }
}
```

`Kabuk.kt`:
- Hesaplar rotasını yer tutucudan çıkar: `listOf(Sekme.CUZDANLAR, Sekme.DERSLER).forEach { … YerTutucu … }`
- Ekle:
  ```kotlin
            composable(Sekme.HESAPLAR.rota) { HesaplarEkrani(kartAc = { nav.navigate("kart/$it") }) }
            composable("kart/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { giris ->
                KartDetayEkrani(giris.arguments!!.getLong("id"), geri = { nav.popBackStack() })
            }
  ```
- `NavigationBarItem`'da `selected = aktif == s.rota` yerine `selected = aktif == s.rota || (s == Sekme.HESAPLAR && aktif?.startsWith("kart/") == true)`
- Importlar: `androidx.navigation.NavType`, `androidx.navigation.navArgument`, `app.tibi.ui.hesaplar.HesaplarEkrani`, `app.tibi.ui.hesaplar.KartDetayEkrani`.

- [ ] **Adım 6: Derle, kur, commit**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"; cp app/build/outputs/apk/debug/app-debug.apk "$W/"
"$WADB" install -r "$(wslpath -w "$W/app-debug.apk")" && "$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
git add app/
git commit -m "feat(hesaplar): Hesaplar sekmesi ve kart detayı

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_0185ka4qkbLW4rdK5CMc4fuq"
```

---

### Görev 11: Uçtan uca deneme ve v0.2.0

Bu görevi **ana oturum** yürütür, alt ajana verilmez. Kullanıcıyla birlikte yapılır, çünkü Xiaomi otomatik dokunmaya izin vermiyor.

- [ ] **Adım 1:** Debug uygulamayı temizle ve aç (MIUI `pm clear`'i engelliyor: `"$WADB" uninstall app.tibi.debug` ve yeniden kur). Kullanıcı kurulumu kendi gerçek verisiyle **deneme amaçlı** yapar: bir banka hesabı, maaş, bir kart ve bir taksit girer. Ardından "+" ile birer harcama, gelir ve transfer girer, Özet, Hareketler ve kart detayı ekranlarına bakar. Her adımda ekran görüntüsü alınıp kontrol edilir. Çökme kaydı temiz olmalı.
- [ ] **Adım 2:** Bulunan hatalar düzeltilir. Her düzeltmenin kendi testi ve commit'i olur.
- [ ] **Adım 3:** `app/build.gradle.kts`'de `versionName = "0.2.0"`, commit, push, CI yeşil. Ardından `git tag v0.2.0 && git push origin v0.2.0` ve CI yeşil.
- [ ] **Adım 4:** Release APK'yı indirip telefona kur: `gh release download v0.2.0` → `"$WADB" install -r`. Aynı anahtarla imzalı olduğu için v0.1.0'ın üzerine kurulur. Kullanıcı **gerçek kurulumunu** bu uygulamada (`app.tibi`) yapar. Debug uygulama (`app.tibi.debug`) isteğe göre silinir: `"$WADB" uninstall app.tibi.debug`.
- [ ] **Adım 5:** Kullanıcıya hatırlat: veriler şu an yalnızca telefonda duruyor ve yedek alma özelliği Plan 4'te geliyor.

---

## Öz Denetim

- **Kapsam:** Yol haritasındaki Plan 3 gereksinimlerinin hepsi bir göreve bağlı:

  | Gereksinim | Görev |
  |---|---|
  | S-1 (açılış yerine doğrudan kurulum kapısı) | Görev 2 |
  | S1 ve S2 (5 adımlı kurulum, sınırsız kart) | Görev 3–6 |
  | S3 (açılış bakiyesi) | Görev 3 |
  | E1–E4 (hızlı giriş) | Görev 7 |
  | K1, K9, K10 (kart formu) | Görev 5 |
  | K5 (kart detayı) | Görev 10 |
  | K8 (devam eden taksitler) | Görev 6 |
  | G1–G4 (maaş, gelir) | Görev 4 ve 7 |
  | Dönem (Özet, Hareketler) | Görev 1, 8, 9 |

  Kurulumun Hoş geldin (kilit), Harcama freni ve İzinler adımları, adım enum'una kendi planlarında eklenecek. Açılış ekranındaki "yedekten geri yükle" Plan 4'te.
- **Tip tutarlılığı:** `Sonuc`, `KurulumVm`, `KartGirdisi`, `TaksitGirdisi`, `TaksitModu`, `HesapBakiyesi`, `KartBilgisi`, `KartTaksidi`, `DonemServisi.donem`, `tibiVm` ve `Bolum` bütün görevlerde aynı adla kullanılıyor.
- **Bilinen sınırlar:**
  - Kayıt düzenleme yok, silip yeniden ekleniyor. Düzenleme Plan 5'te kart ödemesi ekranıyla birlikte gelecek.
  - Tarih seçimi Bugün ve Dün ile sınırlı. Takvimden tarih seçme Plan 8'de gelecek.
