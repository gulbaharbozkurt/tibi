# tibi Plan 2: Veri Katmanı — Uygulama Planı

> **Ajanlar için:** GEREKLİ ALT BECERİ: superpowers:subagent-driven-development (önerilen) ya da superpowers:executing-plans ile görev görev uygulayın. Adımlar `- [ ]` onay kutularıyla takip edilir.

**Hedef:** Telefonda şifreli bir veritabanı kurmak. Plan 3'ün ekranlarının ihtiyaç duyduğu tablolar (hesap, kart, kategori, hareket, taksit satırı, ekstre, düzenli kural, ayar), tek transaction'da çalışan Kayıt servisi ve bakiye, limit ve taksit yükü sorguları bu planda yazılır. Her şey testlidir.

**Mimari:** Room (SQLite) + SQLCipher. Tablolar `app/src/main/kotlin/app/tibi/veri/` altında. Hesap kuralları `:core`'dan gelir (TaksitPlanlayici, KartTakvimi); veri katmanı onları çağırır, kural tekrar yazmaz. Testler JVM'de Robolectric ile, bellekte ve şifresiz veritabanı üzerinde koşar. Şifreleme ayrıca telefonda, enstrümanlı testle doğrulanır.

**Teknoloji:** Room 2.6.1 (KSP 2.1.0-1.0.29), SQLCipher for Android 4.6.1, androidx.sqlite 2.4.0, kotlinx-coroutines 1.9.0, Robolectric 4.14.1 (SDK 34 ile), JUnit 4.13.2.

**Kapsam dışı:** Avans, öğrenci/ders, cüzdan, varlık, kur, bekleme listesi, izlenen uygulama ve bildirim kaydı tabloları. Bunlar yol haritasında kendi planlarında eklenir; eklenirken Room migration yazılır.

## Genel Kısıtlar

- Plan 1'in Genel Kısıtları geçerli: tutar `Long` kuruş, oran binde `Int`, `java.time`, ASCII Türkçe adlar, **her komuttan önce** `source ~/.tibi-env`, commit biçimi ve `Co-Authored-By` satırı.
- Tabloda tutar sütunları `Long` ve adı `…Kurus` ile biter (`tutarKurus`). `Kurus` tipine dönüşüm servis sınırında yapılır.
- `LocalDate` sütunları epochDay (`Long`), `Instant` sütunları epoch milisaniye olarak saklanır.
- Enum sütunları Room'un yerleşik desteğiyle ad olarak (`"HARCAMA"`) saklanır.
- Testlerde `Long` karşılaştırmalarında beklenen değer `L` sonekiyle yazılır (`assertEquals(845000L, …)`); aksi hâlde `Int` ile `Long` eşit sayılmaz.
- Bakiye, borç, limit kullanımı **saklanmaz**, sorgu ile hesaplanır (veri modeli sayfası, "Saklanmayan rakamlar").
- Debug derlemesi `app.tibi.debug` paket adıyla kurulur; telefondaki release sürümüyle (`app.tibi`) çakışmaz.
- Telefon Windows `adb.exe` ile görülür: `"$WADB"` (`~/.tibi-env` tanımlar). Telefona kurulacak APK önce Windows klasörüne kopyalanır: `"/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar/"`.

## Dosya Yapısı

```
app/
├── build.gradle.kts                          + KSP, Room, SQLCipher, test bağımlılıkları
├── schemas/                                  Room şema JSON'ları (commit edilir)
└── src/
    ├── main/kotlin/app/tibi/
    │   ├── TibiUygulama.kt                   Application: veritabanı tekil nesnesi
    │   └── veri/
    │       ├── Donusturuculer.kt             LocalDate/Instant ↔ Long
    │       ├── TibiVeritabani.kt             @Database, oluşturucu, tohum callback
    │       ├── VarsayilanKategoriler.kt      ilk açılış kategorileri
    │       ├── VeritabaniAnahtari.kt         Keystore ile korunan SQLCipher parolası
    │       ├── KayitServisi.kt               tek transaction'lı yazma işlemleri
    │       ├── tablo/                        @Entity sınıfları + enum'lar
    │       │   ├── Hesap.kt  Kart.kt  Kategori.kt  Hareket.kt
    │       │   ├── TaksitSatiri.kt  Ekstre.kt  DuzenliKural.kt  Ayar.kt
    │       └── dao/                          @Dao arayüzleri
    │           ├── HesapDao.kt  KartDao.kt  KategoriDao.kt  HareketDao.kt
    │           ├── TaksitDao.kt  EkstreDao.kt  DuzenliKuralDao.kt  AyarDao.kt
    ├── test/
    │   ├── resources/robolectric.properties  sdk=34
    │   └── kotlin/app/tibi/veri/…            Robolectric testleri
    └── androidTest/kotlin/app/tibi/veri/SifrelemeTesti.kt   telefonda
```

---

### Görev 1: Room kurulumu, dönüştürücüler, Hesap tablosu

**Dosyalar:**
- Değiştir: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`
- Oluştur: `app/src/main/kotlin/app/tibi/veri/Donusturuculer.kt`, `veri/tablo/Hesap.kt`, `veri/dao/HesapDao.kt`, `veri/TibiVeritabani.kt`
- Oluştur: `app/src/test/resources/robolectric.properties`
- Test: `app/src/test/kotlin/app/tibi/veri/HesapDaoTest.kt`

**Arayüzler:**
- Üretir: `enum class HesapTuru { BANKA, NAKIT, KREDI_KARTI }`; `@Entity data class Hesap(id, ad, tur, acilisBakiyeKurus, acilisTarihi, maasHesabi, renk, sira, arsiv)`; `HesapDao.ekle(h): Long`, `getir(id): Hesap?`, `tumu(): Flow<List<Hesap>>`; `abstract class TibiVeritabani : RoomDatabase` + `companion fun bellekte(context): TibiVeritabani`.

- [ ] **Adım 1: Sürüm kataloğuna ekle**

`gradle/libs.versions.toml` `[versions]` altına:
```toml
ksp = "2.1.0-1.0.29"
room = "2.6.1"
sqlcipher = "4.6.1"
sqlite = "2.4.0"
coroutines = "1.9.0"
robolectric = "4.14.1"
junit4 = "4.13.2"
androidxTestCore = "1.6.1"
androidxTestExt = "1.2.1"
androidxTestRunner = "1.6.2"
```
`[libraries]` altına:
```toml
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
sqlcipher-android = { group = "net.zetetic", name = "sqlcipher-android", version.ref = "sqlcipher" }
androidx-sqlite = { group = "androidx.sqlite", name = "sqlite", version.ref = "sqlite" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
junit4 = { group = "junit", name = "junit", version.ref = "junit4" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTestRunner" }
```
`[plugins]` altına:
```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Kök `build.gradle.kts` `plugins { }` içine:
```kotlin
    alias(libs.plugins.ksp) apply false
```

- [ ] **Adım 2: `app/build.gradle.kts`**

`plugins { }` içine ekle:
```kotlin
    alias(libs.plugins.ksp)
```
`defaultConfig { }` içine ekle:
```kotlin
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```
`buildTypes { }` içine, `release`'ten önce ekle:
```kotlin
        debug {
            applicationIdSuffix = ".debug"
        }
```
`android { }` içine, `buildFeatures`'tan sonra ekle:
```kotlin
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
```
`android { }` bloğunun dışına, `kotlin { }`'den sonra ekle:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}
```
`dependencies { }` içine ekle:
```kotlin
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.kotlinx.coroutines.test)
```

`app/src/test/resources/robolectric.properties`:
```properties
sdk=34
```
(Robolectric SDK 35 için Java 21 ister; testlerde SDK 34 yeterli.)

- [ ] **Adım 3: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/veri/HesapDaoTest.kt`:
```kotlin
package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class HesapDaoTest {
    private lateinit var db: TibiVeritabani

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    @Test
    fun `hesap ekle ve oku`() = runTest {
        val id = db.hesapDao().ekle(
            Hesap(ad = "Garanti vadesiz", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000,
                acilisTarihi = LocalDate.parse("2026-09-24"), maasHesabi = true)
        )
        val h = db.hesapDao().getir(id)!!
        assertEquals("Garanti vadesiz", h.ad)
        assertEquals(HesapTuru.BANKA, h.tur)
        assertEquals(845000L, h.acilisBakiyeKurus)
        assertEquals(LocalDate.parse("2026-09-24"), h.acilisTarihi)
        assertEquals(listOf(h), db.hesapDao().tumu().first())
    }

    @Test
    fun `olmayan hesap null`() = runTest {
        assertNull(db.hesapDao().getir(999))
    }

    @Test
    fun `arsivdeki hesap listede yok`() = runTest {
        db.hesapDao().ekle(Hesap(ad = "Eski", tur = HesapTuru.NAKIT, acilisTarihi = LocalDate.parse("2026-01-01"), arsiv = true))
        assertEquals(emptyList(), db.hesapDao().tumu().first())
    }
}
```

- [ ] **Adım 4: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*HesapDaoTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'TibiVeritabani'`.

- [ ] **Adım 5: Uygulamayı yaz**

`app/src/main/kotlin/app/tibi/veri/Donusturuculer.kt`:
```kotlin
package app.tibi.veri

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Donusturuculer {
    @TypeConverter fun tarihtenSayiya(t: LocalDate?): Long? = t?.toEpochDay()
    @TypeConverter fun sayidanTarihe(g: Long?): LocalDate? = g?.let(LocalDate::ofEpochDay)
    @TypeConverter fun andanSayiya(a: Instant?): Long? = a?.toEpochMilli()
    @TypeConverter fun sayidanAna(ms: Long?): Instant? = ms?.let(Instant::ofEpochMilli)
}
```

`app/src/main/kotlin/app/tibi/veri/tablo/Hesap.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class HesapTuru { BANKA, NAKIT, KREDI_KARTI }

@Entity(tableName = "hesap")
data class Hesap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val tur: HesapTuru,
    val acilisBakiyeKurus: Long = 0,
    val acilisTarihi: LocalDate,
    val maasHesabi: Boolean = false,
    val renk: Int? = null,
    val sira: Int = 0,
    val arsiv: Boolean = false,
)
```

`app/src/main/kotlin/app/tibi/veri/dao/HesapDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.tibi.veri.tablo.Hesap
import kotlinx.coroutines.flow.Flow

@Dao
interface HesapDao {
    @Insert suspend fun ekle(hesap: Hesap): Long
    @Update suspend fun guncelle(hesap: Hesap)
    @Query("SELECT * FROM hesap WHERE id = :id") suspend fun getir(id: Long): Hesap?
    @Query("SELECT * FROM hesap WHERE arsiv = 0 ORDER BY sira, id") fun tumu(): Flow<List<Hesap>>
}
```

`app/src/main/kotlin/app/tibi/veri/TibiVeritabani.kt`:
```kotlin
package app.tibi.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.tibi.veri.dao.HesapDao
import app.tibi.veri.tablo.Hesap

@Database(entities = [Hesap::class], version = 1, exportSchema = true)
@TypeConverters(Donusturuculer::class)
abstract class TibiVeritabani : RoomDatabase() {
    abstract fun hesapDao(): HesapDao

    companion object {
        /** Testler için: bellekte, şifresiz. */
        fun bellekte(context: Context): TibiVeritabani =
            Room.inMemoryDatabaseBuilder(context, TibiVeritabani::class.java).build()
    }
}
```

- [ ] **Adım 6: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, HesapDaoTest 3 test geçti. `app/schemas/app.tibi.veri.TibiVeritabani/1.json` oluştu.

- [ ] **Adım 7: Commit**

```bash
cd ~/tibi && git add gradle/libs.versions.toml build.gradle.kts app/
git commit -m "feat(veri): Room kurulumu ve hesap tablosu

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Görev 2: Kart ve kategori tabloları, varsayılan kategoriler

**Dosyalar:**
- Oluştur: `veri/tablo/Kart.kt`, `veri/tablo/Kategori.kt`, `veri/dao/KartDao.kt`, `veri/dao/KategoriDao.kt`, `veri/VarsayilanKategoriler.kt`
- Değiştir: `veri/TibiVeritabani.kt`
- Test: `app/src/test/kotlin/app/tibi/veri/KartKategoriTest.kt`

**Arayüzler:**
- Tüketir: `Hesap`, `HesapDao` (Görev 1)
- Üretir: `enum class KartTuru { ANA, EK, SANAL }`; `@Entity data class Kart(hesapId PK, kartTuru, anaKartId, son4, kesimGunu, sonOdemeGunu, bankaLimitiKurus, kendiLimitiKurus, asgariOranBinde, otoOdeme, odemeHesapId, aidatAy, aidatTutarKurus)`; `KartDao.ekle(k)`, `getir(hesapId): Kart?`, `bagliKartIdleri(anaKartId): List<Long>`; `enum class KategoriYonu { GIDER, GELIR }`; `@Entity data class Kategori(id, ad, yon, sira, arsiv)`; `KategoriDao.tumu(yon): Flow<List<Kategori>>`, `adIle(ad): Kategori?`; `object VarsayilanKategoriler { val GIDER: List<String>; val GELIR: List<String> }`; `TibiVeritabani.TOHUM: RoomDatabase.Callback`.

Kaynak: K1, K10 (ortak limit), E3 (kategori seti), G1 (gelir kaynakları kategori olarak).

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/veri/KartKategoriTest.kt`:
```kotlin
package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
import app.tibi.veri.tablo.KategoriYonu
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class KartKategoriTest {
    private lateinit var db: TibiVeritabani
    private val gun = LocalDate.parse("2026-09-24")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    private suspend fun kartHesabi(ad: String) =
        db.hesapDao().ekle(Hesap(ad = ad, tur = HesapTuru.KREDI_KARTI, acilisTarihi = gun))

    @Test
    fun `kart ekle ve oku`() = runTest {
        val id = kartHesabi("Bonus")
        db.kartDao().ekle(Kart(hesapId = id, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22,
            bankaLimitiKurus = 5_000_000, kendiLimitiKurus = 1_500_000))
        val k = db.kartDao().getir(id)!!
        assertEquals(KartTuru.ANA, k.kartTuru)
        assertEquals(400, k.asgariOranBinde)
        assertEquals(1_500_000L, k.kendiLimitiKurus)
    }

    @Test
    fun `ek ve sanal kartlar ana karta baglanir`() = runTest {
        val ana = kartHesabi("Bonus")
        db.kartDao().ekle(Kart(hesapId = ana, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22))
        val sanal = kartHesabi("Bonus Sanal")
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = ana, son4 = "9054", kesimGunu = 12, sonOdemeGunu = 22))
        assertEquals(listOf(ana, sanal), db.kartDao().bagliKartIdleri(ana))
    }

    @Test
    fun `ilk acilista varsayilan kategoriler hazir`() = runTest {
        val gider = db.kategoriDao().tumu(KategoriYonu.GIDER).first().map { it.ad }
        val gelir = db.kategoriDao().tumu(KategoriYonu.GELIR).first().map { it.ad }
        assertEquals(VarsayilanKategoriler.GIDER, gider)
        assertEquals(VarsayilanKategoriler.GELIR, gelir)
        assertEquals(14, gider.size)
        assertNotNull(db.kategoriDao().adIle("Market"))
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KartKategoriTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'Kart'`.

- [ ] **Adım 3: Uygulamayı yaz**

`app/src/main/kotlin/app/tibi/veri/tablo/Kart.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class KartTuru { ANA, EK, SANAL }

/** Kredi kartı; Hesap'ın uzantısı (hesapId hem birincil hem yabancı anahtar). */
@Entity(
    tableName = "kart",
    foreignKeys = [
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["hesapId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["anaKartId"]),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["odemeHesapId"]),
    ],
    indices = [Index("anaKartId"), Index("odemeHesapId")],
)
data class Kart(
    @PrimaryKey val hesapId: Long,
    val kartTuru: KartTuru = KartTuru.ANA,
    val anaKartId: Long? = null,
    val son4: String,
    val kesimGunu: Int,
    val sonOdemeGunu: Int,
    val bankaLimitiKurus: Long? = null,
    val kendiLimitiKurus: Long? = null,
    val asgariOranBinde: Int = 400,
    val otoOdeme: Boolean = false,
    val odemeHesapId: Long? = null,
    val aidatAy: Int? = null,
    val aidatTutarKurus: Long? = null,
)
```

`app/src/main/kotlin/app/tibi/veri/tablo/Kategori.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class KategoriYonu { GIDER, GELIR }

@Entity(tableName = "kategori", indices = [Index("ad", unique = true)])
data class Kategori(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val yon: KategoriYonu,
    val sira: Int = 0,
    val arsiv: Boolean = false,
)
```

`app/src/main/kotlin/app/tibi/veri/dao/KartDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import app.tibi.veri.tablo.Kart

@Dao
interface KartDao {
    @Insert suspend fun ekle(kart: Kart)
    @Update suspend fun guncelle(kart: Kart)
    @Query("SELECT * FROM kart WHERE hesapId = :hesapId") suspend fun getir(hesapId: Long): Kart?

    /** Ana kart ve ona bağlı ek/sanal kartların hesap id'leri (ana kart önce). */
    @Query("SELECT :anaKartId UNION ALL SELECT hesapId FROM kart WHERE anaKartId = :anaKartId")
    suspend fun bagliKartIdleri(anaKartId: Long): List<Long>
}
```

`app/src/main/kotlin/app/tibi/veri/dao/KategoriDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Kategori
import app.tibi.veri.tablo.KategoriYonu
import kotlinx.coroutines.flow.Flow

@Dao
interface KategoriDao {
    @Insert suspend fun ekle(kategori: Kategori): Long
    @Query("SELECT * FROM kategori WHERE yon = :yon AND arsiv = 0 ORDER BY sira, id")
    fun tumu(yon: KategoriYonu): Flow<List<Kategori>>
    @Query("SELECT * FROM kategori WHERE ad = :ad") suspend fun adIle(ad: String): Kategori?
    @Query("SELECT * FROM kategori WHERE id = :id") suspend fun getir(id: Long): Kategori?
}
```

`app/src/main/kotlin/app/tibi/veri/VarsayilanKategoriler.kt`:
```kotlin
package app.tibi.veri

/** E3: ilk açılışta hazır gelen kategoriler. Sıra, listedeki sıradır. */
object VarsayilanKategoriler {
    val GIDER = listOf(
        "Market", "Yemek/Kafe", "Fatura", "Kira/Aidat", "Ulaşım/Yakıt", "Giyim", "Elektronik",
        "Sağlık", "Kişisel bakım", "Eğlence/Abonelik", "Eğitim", "Hediye", "Ev", "Diğer",
    )
    val GELIR = listOf("Maaş", "Özel ders", "Diğer gelir")
}
```

`app/src/main/kotlin/app/tibi/veri/TibiVeritabani.kt` (tamamını değiştir):
```kotlin
package app.tibi.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import app.tibi.veri.dao.HesapDao
import app.tibi.veri.dao.KartDao
import app.tibi.veri.dao.KategoriDao
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.Kategori

@Database(entities = [Hesap::class, Kart::class, Kategori::class], version = 1, exportSchema = true)
@TypeConverters(Donusturuculer::class)
abstract class TibiVeritabani : RoomDatabase() {
    abstract fun hesapDao(): HesapDao
    abstract fun kartDao(): KartDao
    abstract fun kategoriDao(): KategoriDao

    companion object {
        /** İlk oluşturmada varsayılan kategorileri yazar. */
        val TOHUM = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                fun yaz(adlar: List<String>, yon: String) = adlar.forEachIndexed { i, ad ->
                    db.execSQL("INSERT INTO kategori (ad, yon, sira, arsiv) VALUES (?, ?, ?, 0)", arrayOf(ad, yon, i))
                }
                yaz(VarsayilanKategoriler.GIDER, "GIDER")
                yaz(VarsayilanKategoriler.GELIR, "GELIR")
            }
        }

        /** Testler için: bellekte, şifresiz. */
        fun bellekte(context: Context): TibiVeritabani =
            Room.inMemoryDatabaseBuilder(context, TibiVeritabani::class.java).addCallback(TOHUM).build()
    }
}
```
(Veritabanı henüz hiçbir cihazda yok; bu yüzden sürüm 1'de kalır, migration gerekmez. Plan 3'ten sonra her şema değişikliği yeni sürüm + migration ister.)

- [ ] **Adım 4: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, 6 test geçti.

- [ ] **Adım 5: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(veri): kart ve kategori tabloları, varsayılan kategoriler

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Görev 3: Hareket, taksit satırı, ekstre, düzenli kural ve ayar tabloları

**Dosyalar:**
- Oluştur: `veri/tablo/Hareket.kt`, `TaksitSatiri.kt`, `Ekstre.kt`, `DuzenliKural.kt`, `Ayar.kt`
- Oluştur: `veri/dao/HareketDao.kt`, `TaksitDao.kt`, `EkstreDao.kt`, `DuzenliKuralDao.kt`, `AyarDao.kt`
- Değiştir: `veri/TibiVeritabani.kt`
- Test: `app/src/test/kotlin/app/tibi/veri/TablolarTest.kt`

**Arayüzler:**
- Tüketir: Görev 1–2 tabloları; `HaftaSonuKurali` (`:core`)
- Üretir:
  - `enum class HareketTuru { HARCAMA, GELIR, AVANS, TAHSILAT, TRANSFER, KART_ODEME, CUZDAN_AKTAR, DUZELTME }`, `enum class TaksitTuru { ALISVERIS, EKSTRE_TAKSIT, NAKIT_AVANS }`
  - `@Entity data class Hareket(id, tur, tarih, tutarKurus, kaynakHesapId, hedefHesapId, kategoriId, cuzdanId, taksitSayisi, ertelemeAy, taksitTuru, gecmisAktarim, ekstreId, ogrenciId, duzenliKuralId, beklenenTarih, aciklama, olusturma)`
  - `@Entity data class TaksitSatiri(id, hareketId, kartId, sira, toplam, tutarKurus, ekstreKesimTarihi, ekstreId, oncedenOdendi)`
  - `enum class EkstreDurumu { KESILDI, KISMI, ODENDI }`, `@Entity data class Ekstre(id, kartId, kesimTarihi, sonOdemeTarihi, donemTutariKurus, devredenKurus, toplamKurus, asgariKurus, durum, acilis)`
  - `enum class Periyot { HAFTALIK, AYLIK, YILLIK }`, `@Entity data class DuzenliKural(id, ad, yon: KategoriYonu, maas, tutarKurus, degisken, periyot, gun, ay, haftaSonuKurali, hesapId, kategoriId, otomatik, baslangic, bitis, aktif)`
  - `@Entity data class Ayar(anahtar PK, deger)`
  - DAO: `HareketDao.ekle(h): Long`, `getir(id)`, `aralik(bas, bit): Flow<List<Hareket>>`; `TaksitDao.ekle(list)`, `hareketin(hareketId): List<TaksitSatiri>`; `EkstreDao.ekle(e): Long`; `DuzenliKuralDao.ekle(k): Long`, `maasKurali(): DuzenliKural?`; `AyarDao.yaz(a)`, `oku(anahtar): String?`

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/veri/TablolarTest.kt`:
```kotlin
package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.tarih.HaftaSonuKurali
import app.tibi.veri.tablo.Ayar
import app.tibi.veri.tablo.DuzenliKural
import app.tibi.veri.tablo.Hareket
import app.tibi.veri.tablo.HareketTuru
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
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
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class TablolarTest {
    private lateinit var db: TibiVeritabani
    private fun t(s: String) = LocalDate.parse(s)
    private val an = Instant.parse("2026-09-24T09:00:00Z")

    @Before fun ac() { db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext()) }
    @After fun kapat() { db.close() }

    @Test
    fun `hareketler tarih araliginda yeniden eskiye`() = runTest {
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = t("2026-09-01")))
        fun h(tarih: String, tutar: Long) = Hareket(tur = HareketTuru.HARCAMA, tarih = t(tarih), tutarKurus = tutar,
            kaynakHesapId = nakit, olusturma = an)
        db.hareketDao().ekle(h("2026-09-10", 100))
        db.hareketDao().ekle(h("2026-09-20", 200))
        db.hareketDao().ekle(h("2026-10-01", 300))
        val liste = db.hareketDao().aralik(t("2026-09-01"), t("2026-09-30")).first()
        assertEquals(listOf(200L, 100L), liste.map { it.tutarKurus })
    }

    @Test
    fun `maas kurali`() = runTest {
        assertNull(db.duzenliKuralDao().maasKurali())
        val hesap = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisTarihi = t("2026-09-01")))
        db.duzenliKuralDao().ekle(DuzenliKural(ad = "Maaş", yon = KategoriYonu.GELIR, maas = true, tutarKurus = 4_500_000,
            periyot = Periyot.AYLIK, gun = 30, haftaSonuKurali = HaftaSonuKurali.ONCEKI, hesapId = hesap, baslangic = t("2026-09-01")))
        val k = db.duzenliKuralDao().maasKurali()!!
        assertEquals(30, k.gun)
        assertEquals(HaftaSonuKurali.ONCEKI, k.haftaSonuKurali)
    }

    @Test
    fun `ayar yaz uzerine yaz oku`() = runTest {
        assertNull(db.ayarDao().oku("hitap"))
        db.ayarDao().yaz(Ayar("hitap", "Gülbahar"))
        db.ayarDao().yaz(Ayar("hitap", "Gül"))
        assertEquals("Gül", db.ayarDao().oku("hitap"))
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*TablolarTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'Ayar'`.

- [ ] **Adım 3: Tabloları yaz**

`app/src/main/kotlin/app/tibi/veri/tablo/Hareket.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class HareketTuru { HARCAMA, GELIR, AVANS, TAHSILAT, TRANSFER, KART_ODEME, CUZDAN_AKTAR, DUZELTME }
enum class TaksitTuru { ALISVERIS, EKSTRE_TAKSIT, NAKIT_AVANS }

/** Tek kaynak: her para hareketi. Yönü kaynak (çıktığı) ve hedef (girdiği) hesap belirler. */
@Entity(
    tableName = "hareket",
    foreignKeys = [
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["kaynakHesapId"]),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["hedefHesapId"]),
        ForeignKey(entity = Kategori::class, parentColumns = ["id"], childColumns = ["kategoriId"]),
        ForeignKey(entity = Ekstre::class, parentColumns = ["id"], childColumns = ["ekstreId"]),
    ],
    indices = [Index("kaynakHesapId"), Index("hedefHesapId"), Index("kategoriId"), Index("ekstreId"), Index("tarih")],
)
data class Hareket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tur: HareketTuru,
    val tarih: LocalDate,
    val tutarKurus: Long,
    val kaynakHesapId: Long? = null,
    val hedefHesapId: Long? = null,
    val kategoriId: Long? = null,
    val cuzdanId: Long? = null,
    val taksitSayisi: Int = 1,
    val ertelemeAy: Int = 0,
    val taksitTuru: TaksitTuru? = null,
    val gecmisAktarim: Boolean = false,
    val ekstreId: Long? = null,
    val ogrenciId: Long? = null,
    val duzenliKuralId: Long? = null,
    val beklenenTarih: LocalDate? = null,
    val aciklama: String? = null,
    val olusturma: Instant,
)
```
(`cuzdanId`, `ogrenciId`, `duzenliKuralId` yabancı anahtarları ilgili tablolar kendi planlarında eklenince migration ile bağlanır.)

`app/src/main/kotlin/app/tibi/veri/tablo/TaksitSatiri.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Karta yazılan her tutar; tek çekim = 1 satır. Hangi ekstreye düşeceği baştan bellidir. */
@Entity(
    tableName = "taksit_satiri",
    foreignKeys = [
        ForeignKey(entity = Hareket::class, parentColumns = ["id"], childColumns = ["hareketId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["kartId"]),
        ForeignKey(entity = Ekstre::class, parentColumns = ["id"], childColumns = ["ekstreId"]),
    ],
    indices = [Index("hareketId"), Index("kartId"), Index("ekstreId"), Index("ekstreKesimTarihi")],
)
data class TaksitSatiri(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hareketId: Long,
    val kartId: Long,
    val sira: Int,
    val toplam: Int,
    val tutarKurus: Long,
    val ekstreKesimTarihi: LocalDate,
    val ekstreId: Long? = null,
    val oncedenOdendi: Boolean = false,
)
```

`app/src/main/kotlin/app/tibi/veri/tablo/Ekstre.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class EkstreDurumu { KESILDI, KISMI, ODENDI }

/** Kesilmiş ekstrenin dondurulmuş özeti. acilis = kurulumda girilen ödenmemiş ekstre. */
@Entity(
    tableName = "ekstre",
    foreignKeys = [ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["kartId"])],
    indices = [Index("kartId")],
)
data class Ekstre(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kartId: Long,
    val kesimTarihi: LocalDate,
    val sonOdemeTarihi: LocalDate,
    val donemTutariKurus: Long,
    val devredenKurus: Long = 0,
    val toplamKurus: Long,
    val asgariKurus: Long,
    val durum: EkstreDurumu = EkstreDurumu.KESILDI,
    val acilis: Boolean = false,
)
```

`app/src/main/kotlin/app/tibi/veri/tablo/DuzenliKural.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.tibi.core.tarih.HaftaSonuKurali
import java.time.LocalDate

enum class Periyot { HAFTALIK, AYLIK, YILLIK }

/** Maaş, abonelik, kira gibi tekrarlayan gelir/gider. maas = true olan tek kural dönemi belirler. */
@Entity(
    tableName = "duzenli_kural",
    foreignKeys = [
        ForeignKey(entity = Hesap::class, parentColumns = ["id"], childColumns = ["hesapId"]),
        ForeignKey(entity = Kategori::class, parentColumns = ["id"], childColumns = ["kategoriId"]),
    ],
    indices = [Index("hesapId"), Index("kategoriId")],
)
data class DuzenliKural(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val yon: KategoriYonu,
    val maas: Boolean = false,
    val tutarKurus: Long,
    val degisken: Boolean = false,
    val periyot: Periyot,
    val gun: Int,
    val ay: Int? = null,
    val haftaSonuKurali: HaftaSonuKurali = HaftaSonuKurali.AYNI,
    val hesapId: Long,
    val kategoriId: Long? = null,
    val otomatik: Boolean = true,
    val baslangic: LocalDate,
    val bitis: LocalDate? = null,
    val aktif: Boolean = true,
)
```

`app/src/main/kotlin/app/tibi/veri/tablo/Ayar.kt`:
```kotlin
package app.tibi.veri.tablo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ayar")
data class Ayar(@PrimaryKey val anahtar: String, val deger: String)
```

- [ ] **Adım 4: DAO'ları yaz**

`app/src/main/kotlin/app/tibi/veri/dao/HareketDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Hareket
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HareketDao {
    @Insert suspend fun ekle(hareket: Hareket): Long
    @Query("SELECT * FROM hareket WHERE id = :id") suspend fun getir(id: Long): Hareket?
    @Query("SELECT * FROM hareket WHERE tarih BETWEEN :bas AND :bit ORDER BY tarih DESC, id DESC")
    fun aralik(bas: LocalDate, bit: LocalDate): Flow<List<Hareket>>
    @Query("SELECT COUNT(*) FROM hareket") suspend fun sayi(): Int
}
```

`app/src/main/kotlin/app/tibi/veri/dao/TaksitDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.TaksitSatiri

@Dao
interface TaksitDao {
    @Insert suspend fun ekle(satirlar: List<TaksitSatiri>)
    @Query("SELECT * FROM taksit_satiri WHERE hareketId = :hareketId ORDER BY sira")
    suspend fun hareketin(hareketId: Long): List<TaksitSatiri>
    @Query("SELECT COUNT(*) FROM taksit_satiri") suspend fun sayi(): Int
}
```

`app/src/main/kotlin/app/tibi/veri/dao/EkstreDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.Ekstre

@Dao
interface EkstreDao {
    @Insert suspend fun ekle(ekstre: Ekstre): Long
    @Query("SELECT * FROM ekstre WHERE id = :id") suspend fun getir(id: Long): Ekstre?
}
```

`app/src/main/kotlin/app/tibi/veri/dao/DuzenliKuralDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.DuzenliKural

@Dao
interface DuzenliKuralDao {
    @Insert suspend fun ekle(kural: DuzenliKural): Long
    @Query("SELECT * FROM duzenli_kural WHERE maas = 1 AND aktif = 1 ORDER BY id LIMIT 1")
    suspend fun maasKurali(): DuzenliKural?
}
```

`app/src/main/kotlin/app/tibi/veri/dao/AyarDao.kt`:
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.tibi.veri.tablo.Ayar

@Dao
interface AyarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun yaz(ayar: Ayar)
    @Query("SELECT deger FROM ayar WHERE anahtar = :anahtar") suspend fun oku(anahtar: String): String?
}
```

- [ ] **Adım 5: Veritabanına kaydet**

`TibiVeritabani.kt` içinde `@Database` satırını ve DAO listesini şöyle yap (companion aynı kalır):
```kotlin
@Database(
    entities = [
        Hesap::class, Kart::class, Kategori::class, Hareket::class, TaksitSatiri::class,
        Ekstre::class, DuzenliKural::class, Ayar::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Donusturuculer::class)
abstract class TibiVeritabani : RoomDatabase() {
    abstract fun hesapDao(): HesapDao
    abstract fun kartDao(): KartDao
    abstract fun kategoriDao(): KategoriDao
    abstract fun hareketDao(): HareketDao
    abstract fun taksitDao(): TaksitDao
    abstract fun ekstreDao(): EkstreDao
    abstract fun duzenliKuralDao(): DuzenliKuralDao
    abstract fun ayarDao(): AyarDao
```
ve gerekli `import app.tibi.veri.dao.*` / `import app.tibi.veri.tablo.*` satırlarını ekle (tek tek, joker kullanmadan).

- [ ] **Adım 6: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, 9 test geçti.

- [ ] **Adım 7: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(veri): hareket, taksit satırı, ekstre, düzenli kural ve ayar tabloları

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Görev 4: Kayıt servisi (nakit/banka) ve hesap bakiyesi

**Dosyalar:**
- Oluştur: `app/src/main/kotlin/app/tibi/veri/KayitServisi.kt`
- Değiştir: `veri/dao/HesapDao.kt` (bakiye sorgusu)
- Test: `app/src/test/kotlin/app/tibi/veri/KayitServisiTest.kt`

**Arayüzler:**
- Tüketir: `Kurus` (`:core`); Görev 1–3 tabloları/DAO'ları
- Üretir: `class KayitServisi(db: TibiVeritabani, saat: () -> Instant = Instant::now)` →
  `suspend fun harcama(tutar: Kurus, tarih: LocalDate, hesapId: Long, kategoriId: Long?, taksitSayisi: Int = 1, ertelemeAy: Int = 0, aciklama: String? = null): Long`,
  `suspend fun gelir(tutar: Kurus, tarih: LocalDate, hesapId: Long, kategoriId: Long?, aciklama: String? = null): Long`,
  `suspend fun transfer(tutar: Kurus, tarih: LocalDate, kaynakHesapId: Long, hedefHesapId: Long, aciklama: String? = null): Long`;
  `class KayitHatasi(mesaj: String) : IllegalArgumentException(mesaj)`;
  `HesapDao.bakiye(hesapId): Flow<Long>`.
  Karttan harcama bu görevde `KayitHatasi` fırlatmaz ama taksit satırı üretimi Görev 5'te eklenir; bu görevde karttan harcama testi yok.

Kaynak: E1, H2, H3; veri modeli "Hesap bakiyesi" formülü.

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/veri/KayitServisiTest.kt`:
```kotlin
package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.tablo.HareketTuru
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
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class KayitServisiTest {
    private lateinit var db: TibiVeritabani
    private lateinit var servis: KayitServisi
    private val bugun = LocalDate.parse("2026-09-24")
    private var banka = 0L
    private var nakit = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        servis = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 845000, acilisTarihi = bugun))
        nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisBakiyeKurus = 130000, acilisTarihi = bugun))
    }
    @After fun kapat() { db.close() }

    private suspend fun bakiye(id: Long) = db.hesapDao().bakiye(id).first()
    private suspend fun market() = db.kategoriDao().adIle("Market")!!.id

    @Test
    fun `nakit harcama bakiyeden duser`() = runTest {
        val id = servis.harcama(Kurus(38650), bugun, nakit, market(), aciklama = "haftalık")
        assertEquals(130000L - 38650L, bakiye(nakit))
        val h = db.hareketDao().getir(id)!!
        assertEquals(HareketTuru.HARCAMA, h.tur)
        assertEquals(nakit, h.kaynakHesapId)
        assertEquals(null, h.hedefHesapId)
        assertEquals(0, db.taksitDao().sayi())
    }

    @Test
    fun `gelir bakiyeye eklenir`() = runTest {
        servis.gelir(Kurus(105000), bugun, nakit, db.kategoriDao().adIle("Özel ders")!!.id)
        assertEquals(130000L + 105000L, bakiye(nakit))
    }

    @Test
    fun `transfer iki hesabi birlikte degistirir`() = runTest {
        servis.transfer(Kurus(50000), bugun, banka, nakit)
        assertEquals(845000L - 50000L, bakiye(banka))
        assertEquals(130000L + 50000L, bakiye(nakit))
    }

    @Test
    fun `gecersiz kayitlar hicbir sey yazmaz`() = runTest {
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(0), bugun, nakit, market()) }
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(-5), bugun, nakit, market()) }
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(100), bugun, 999, market()) }
        assertFailsWith<KayitHatasi> { servis.harcama(Kurus(100), bugun, nakit, market(), taksitSayisi = 3) }
        assertFailsWith<KayitHatasi> { servis.transfer(Kurus(100), bugun, nakit, nakit) }
        assertEquals(0, db.hareketDao().sayi())
        assertEquals(130000L, bakiye(nakit))
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KayitServisiTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'KayitServisi'`.

- [ ] **Adım 3: Bakiye sorgusunu ekle**

`HesapDao.kt` içine:
```kotlin
    /** açılış + girenler − çıkanlar. Banka ve nakit için anlamlı; kartlar için limit kullanımı ayrı hesaplanır. */
    @Query(
        """
        SELECT h.acilisBakiyeKurus
             + COALESCE((SELECT SUM(g.tutarKurus) FROM hareket g WHERE g.hedefHesapId = h.id), 0)
             - COALESCE((SELECT SUM(c.tutarKurus) FROM hareket c WHERE c.kaynakHesapId = h.id), 0)
        FROM hesap h WHERE h.id = :hesapId
        """
    )
    fun bakiye(hesapId: Long): Flow<Long>
```

- [ ] **Adım 4: Servisi yaz**

`app/src/main/kotlin/app/tibi/veri/KayitServisi.kt`:
```kotlin
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
```

- [ ] **Adım 5: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, 13 test geçti.

- [ ] **Adım 6: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(veri): kayıt servisi (harcama, gelir, transfer) ve hesap bakiyesi

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Görev 5: Kart kayıtları ve limit/taksit sorguları

**Dosyalar:**
- Değiştir: `veri/KayitServisi.kt`, `veri/dao/TaksitDao.kt`
- Test: `app/src/test/kotlin/app/tibi/veri/KartKayitTest.kt`

**Arayüzler:**
- Tüketir: `TaksitPlanlayici`, `KartTakvimi`, `PlanliTaksit` (`:core`); Görev 4 servisi
- Üretir:
  - `KayitServisi.kartaYansit(...)` gerçek hâli (ek/sanal kart ana kartın takvimini kullanır)
  - `suspend fun kartOdemesi(tutar: Kurus, tarih: LocalDate, bankaHesapId: Long, kartId: Long, ekstreId: Long? = null): Long`
  - `sealed interface GecmisTaksitGirisi { data class Aylik(val aylik: Kurus, val toplam: Int, val siradakiNo: Int); data class Toplam(val tutar: Kurus, val toplam: Int, val siradakiNo: Int); data class KalanBorc(val tutar: Kurus, val kalanSayi: Int) }`
  - `suspend fun gecmisTaksit(giris: GecmisTaksitGirisi, kartId: Long, kategoriId: Long?, taksitTuru: TaksitTuru, bugun: LocalDate, aciklama: String? = null): Long`
  - `suspend fun acilisEkstresi(kartId: Long, toplam: Kurus, bugun: LocalDate): Long` — kartın bugüne kadarki son kesim tarihiyle, `acilis = true`
  - `TaksitDao.limitKullanimi(anaKartId): Flow<Long>`, `TaksitDao.aylikYuk(bugun): Flow<List<AylikYuk>>`, `data class AylikYuk(val ekstreKesimTarihi: LocalDate, val toplamKurus: Long)`

Kaynak: K2, K8, K9, K10; veri modeli "Kart limit kullanımı" ve "Taksit yükü".

- [ ] **Adım 1: Başarısız testi yaz**

`app/src/test/kotlin/app/tibi/veri/KartKayitTest.kt`:
```kotlin
package app.tibi.veri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.core.para.Kurus
import app.tibi.veri.dao.AylikYuk
import app.tibi.veri.tablo.EkstreDurumu
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import app.tibi.veri.tablo.Kart
import app.tibi.veri.tablo.KartTuru
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KartKayitTest {
    private lateinit var db: TibiVeritabani
    private lateinit var servis: KayitServisi
    private fun t(s: String) = LocalDate.parse(s)
    private val bugun = t("2026-09-24")
    private var banka = 0L
    private var bonus = 0L
    private var sanal = 0L

    @Before fun ac() = runTest {
        db = TibiVeritabani.bellekte(ApplicationProvider.getApplicationContext())
        servis = KayitServisi(db) { Instant.parse("2026-09-24T09:00:00Z") }
        banka = db.hesapDao().ekle(Hesap(ad = "Garanti", tur = HesapTuru.BANKA, acilisBakiyeKurus = 2_000_000, acilisTarihi = bugun))
        bonus = db.hesapDao().ekle(Hesap(ad = "Bonus", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        db.kartDao().ekle(Kart(hesapId = bonus, son4 = "4821", kesimGunu = 12, sonOdemeGunu = 22, kendiLimitiKurus = 1_500_000))
        sanal = db.hesapDao().ekle(Hesap(ad = "Bonus Sanal", tur = HesapTuru.KREDI_KARTI, acilisTarihi = bugun))
        // Ek/sanal kartın kendi kesim günü ana karttan farklı yazılsa bile ana kartınki kullanılır.
        db.kartDao().ekle(Kart(hesapId = sanal, kartTuru = KartTuru.SANAL, anaKartId = bonus, son4 = "9054", kesimGunu = 1, sonOdemeGunu = 11))
    }
    @After fun kapat() { db.close() }

    private suspend fun kullanim() = db.taksitDao().limitKullanimi(bonus).first()
    private suspend fun giyim() = db.kategoriDao().adIle("Giyim")!!.id

    @Test
    fun `taksitli kart harcamasi satirlara bolunur`() = runTest {
        val id = servis.harcama(Kurus(124990), bugun, bonus, giyim(), taksitSayisi = 3)
        val s = db.taksitDao().hareketin(id)
        assertEquals(listOf(41664L, 41663L, 41663L), s.map { it.tutarKurus })
        assertEquals(listOf(t("2026-10-12"), t("2026-11-12"), t("2026-12-12")), s.map { it.ekstreKesimTarihi })
        assertTrue(s.all { it.kartId == bonus })
        assertEquals(124990L, kullanim())
    }

    @Test
    fun `sanal kart harcamasi ana kartin takvimi ve limitiyle`() = runTest {
        val id = servis.harcama(Kurus(7999), bugun, sanal, null)
        val s = db.taksitDao().hareketin(id).single()
        assertEquals(sanal, s.kartId)
        assertEquals(t("2026-10-12"), s.ekstreKesimTarihi)
        assertEquals(7999L, kullanim())
    }

    @Test
    fun `kart odemesi limit kullanimini azaltir ve bankadan cikar`() = runTest {
        servis.harcama(Kurus(500000), bugun, bonus, giyim())
        servis.kartOdemesi(Kurus(200000), bugun, banka, bonus)
        assertEquals(300000L, kullanim())
        assertEquals(2_000_000L - 200_000L, db.hesapDao().bakiye(banka).first())
    }

    @Test
    fun `gecmis taksit sadece odenmemisleri sayar`() = runTest {
        servis.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun, "Telefon")
        assertEquals(12, db.taksitDao().sayi())
        assertEquals(7 * 185000L, kullanim())
    }

    @Test
    fun `gecmis taksit kalan borc ile`() = runTest {
        servis.gecmisTaksit(GecmisTaksitGirisi.KalanBorc(Kurus(1295000), 7), bonus, null, TaksitTuru.EKSTRE_TAKSIT, bugun)
        assertEquals(1_295_000L, kullanim())
    }

    @Test
    fun `acilis ekstresi limit kullanimina eklenir`() = runTest {
        val id = servis.acilisEkstresi(bonus, Kurus(1234000), bugun)
        val e = db.ekstreDao().getir(id)!!
        assertEquals(t("2026-09-12"), e.kesimTarihi)
        assertEquals(t("2026-09-22"), e.sonOdemeTarihi)
        assertEquals(493600L, e.asgariKurus)   // %40
        assertEquals(EkstreDurumu.KESILDI, e.durum)
        assertTrue(e.acilis)
        assertEquals(1_234_000L, kullanim())
    }

    @Test
    fun `aylik taksit yuku bugunden itibaren`() = runTest {
        servis.gecmisTaksit(GecmisTaksitGirisi.Aylik(Kurus(185000), 12, 6), bonus, null, TaksitTuru.ALISVERIS, bugun)
        servis.harcama(Kurus(124990), bugun, bonus, giyim(), taksitSayisi = 3)
        val yuk = db.taksitDao().aylikYuk(bugun).first()
        assertEquals(AylikYuk(t("2026-10-12"), 185000L + 41664L), yuk.first())
        assertEquals(AylikYuk(t("2027-04-12"), 185000L), yuk.last())
        assertEquals(7, yuk.size)
    }

    @Test
    fun `nakit hesaba kart odemesi yapilamaz`() = runTest {
        val nakit = db.hesapDao().ekle(Hesap(ad = "Nakit", tur = HesapTuru.NAKIT, acilisTarihi = bugun))
        kotlin.test.assertFailsWith<KayitHatasi> { servis.kartOdemesi(Kurus(100), bugun, banka, nakit) }
    }
}
```

- [ ] **Adım 2: Testin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest --tests '*KartKayitTest*'`
Beklenen: DERLEME HATASI, `Unresolved reference 'AylikYuk'`.

- [ ] **Adım 3: Sorguları ekle**

`app/src/main/kotlin/app/tibi/veri/dao/TaksitDao.kt` (tamamını değiştir):
```kotlin
package app.tibi.veri.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tibi.veri.tablo.TaksitSatiri
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class AylikYuk(val ekstreKesimTarihi: LocalDate, val toplamKurus: Long)

@Dao
interface TaksitDao {
    @Insert suspend fun ekle(satirlar: List<TaksitSatiri>)

    @Query("SELECT * FROM taksit_satiri WHERE hareketId = :hareketId ORDER BY sira")
    suspend fun hareketin(hareketId: Long): List<TaksitSatiri>

    @Query("SELECT COUNT(*) FROM taksit_satiri") suspend fun sayi(): Int

    /**
     * Ana kart + bağlı ek/sanal kartlar: ödenmemiş bütün taksit satırları (gelecek dahil)
     * + açılış ekstresi − kart ödemeleri.
     */
    @Query(
        """
        SELECT
          COALESCE((SELECT SUM(t.tutarKurus) FROM taksit_satiri t
                    WHERE t.oncedenOdendi = 0
                      AND t.kartId IN (SELECT :anaKartId UNION SELECT hesapId FROM kart WHERE anaKartId = :anaKartId)), 0)
        + COALESCE((SELECT SUM(e.toplamKurus) FROM ekstre e WHERE e.acilis = 1 AND e.kartId = :anaKartId), 0)
        - COALESCE((SELECT SUM(h.tutarKurus) FROM hareket h
                    WHERE h.tur = 'KART_ODEME'
                      AND h.hedefHesapId IN (SELECT :anaKartId UNION SELECT hesapId FROM kart WHERE anaKartId = :anaKartId)), 0)
        """
    )
    fun limitKullanimi(anaKartId: Long): Flow<Long>

    /** Bütün kartlarda, bugün ve sonrasına düşen ödenmemiş taksitlerin kesim tarihine göre toplamı. */
    @Query(
        """
        SELECT ekstreKesimTarihi, SUM(tutarKurus) AS toplamKurus FROM taksit_satiri
        WHERE oncedenOdendi = 0 AND ekstreKesimTarihi >= :bugun
        GROUP BY ekstreKesimTarihi ORDER BY ekstreKesimTarihi
        """
    )
    fun aylikYuk(bugun: LocalDate): Flow<List<AylikYuk>>
}
```

- [ ] **Adım 4: Servise kart işlemlerini ekle**

`KayitServisi.kt` içinde:

1. Dosyanın başına importları ekle:
```kotlin
import app.tibi.core.kart.KartTakvimi
import app.tibi.core.kart.PlanliTaksit
import app.tibi.core.kart.TaksitPlanlayici
import app.tibi.core.para.topla
import app.tibi.veri.tablo.Ekstre
import app.tibi.veri.tablo.TaksitSatiri
import app.tibi.veri.tablo.TaksitTuru
import java.time.YearMonth
```

2. `class KayitHatasi` satırının altına ekle:
```kotlin
/** K8: geçmişte başlamış taksitin üç giriş yolu. */
sealed interface GecmisTaksitGirisi {
    data class Aylik(val aylik: Kurus, val toplam: Int, val siradakiNo: Int) : GecmisTaksitGirisi
    data class Toplam(val tutar: Kurus, val toplam: Int, val siradakiNo: Int) : GecmisTaksitGirisi
    data class KalanBorc(val tutar: Kurus, val kalanSayi: Int) : GecmisTaksitGirisi
}
```

3. `kartaYansit` yer tutucusunu şununla değiştir ve altına yeni fonksiyonları ekle:
```kotlin
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
```

Not: `KayitHatasi`, `IllegalArgumentException`'dan türediği için `catch` bloğu kendi fırlattığımız hatayı yeniden sarmalamamalı; `gecmisTaksit`'teki `if (e is KayitHatasi) throw e` bunu sağlar. `kartaYansit`'te `pozitif` zaten `harcama` içinde çağrıldığı için gerekmez.

- [ ] **Adım 5: Testin geçtiğini gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest`
Beklenen: `BUILD SUCCESSFUL`, 21 test geçti (Görev 4'ün testleri dahil).

- [ ] **Adım 6: Commit**

```bash
cd ~/tibi && git add app/
git commit -m "feat(veri): kart harcaması, ödeme, geçmiş taksit, açılış ekstresi, limit ve taksit yükü sorguları

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
```

---

### Görev 6: Şifreleme, uygulamaya bağlama, telefonda doğrulama ve CI

**Dosyalar:**
- Oluştur: `veri/VeritabaniAnahtari.kt`, `app/src/main/kotlin/app/tibi/TibiUygulama.kt`
- Değiştir: `veri/TibiVeritabani.kt` (`olustur`), `app/src/main/AndroidManifest.xml`, `.github/workflows/derle.yml`
- Test: `app/src/androidTest/kotlin/app/tibi/veri/SifrelemeTesti.kt` (telefonda)

**Arayüzler:**
- Tüketir: Görev 1–5
- Üretir: `class VeritabaniAnahtari(context: Context) { fun parola(): ByteArray }`; `TibiVeritabani.olustur(context: Context, ad: String = "tibi.db", parola: ByteArray = VeritabaniAnahtari(context).parola()): TibiVeritabani`; `class TibiUygulama : Application { val veritabani: TibiVeritabani; val kayitServisi: KayitServisi }`.

Kaynak: Temel kararlar "Veri" + veri akışı "Veritabanı şifreli" (SQLCipher, anahtar Android Keystore'da).

- [ ] **Adım 1: Telefonda çalışacak başarısız testi yaz**

`app/src/androidTest/kotlin/app/tibi/veri/SifrelemeTesti.kt`:
```kotlin
package app.tibi.veri

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tibi.veri.tablo.Hesap
import app.tibi.veri.tablo.HesapTuru
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class SifrelemeTesti {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val ad = "sifreleme-testi.db"

    @Before fun temizle() { ctx.deleteDatabase(ad) }

    @Test
    fun dosyaSifreliVeAyniAnahtarlaAcilir() = runTest {
        val parola = VeritabaniAnahtari(ctx).parola()
        TibiVeritabani.olustur(ctx, ad, parola).let { db ->
            db.hesapDao().ekle(Hesap(ad = "Gizli hesap", tur = HesapTuru.NAKIT, acilisTarihi = LocalDate.parse("2026-09-24")))
            db.close()
        }
        val basik = ByteArray(16).also { b -> ctx.getDatabasePath(ad).inputStream().use { it.read(b) } }
        assertFalse(basik.contentEquals("SQLite format 3\u0000".toByteArray()), "Dosya düz SQLite, şifreli değil")
        val icerik = ctx.getDatabasePath(ad).readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(icerik.contains("Gizli hesap"), "Hesap adı dosyada okunabiliyor")

        TibiVeritabani.olustur(ctx, ad, parola).let { db ->
            assertEquals("Gizli hesap", db.hesapDao().getir(1)!!.ad)
            db.close()
        }
    }

    @Test
    fun yanlisAnahtarlaAcilmaz() = runTest {
        TibiVeritabani.olustur(ctx, ad, VeritabaniAnahtari(ctx).parola()).let { db ->
            db.hesapDao().ekle(Hesap(ad = "x", tur = HesapTuru.NAKIT, acilisTarihi = LocalDate.parse("2026-09-24")))
            db.close()
        }
        val yanlis = TibiVeritabani.olustur(ctx, ad, ByteArray(32))
        assertFails { yanlis.hesapDao().getir(1) }
        yanlis.close()
    }

    @Test
    fun anahtarKaliciDir() {
        assertContentEquals(VeritabaniAnahtari(ctx).parola(), VeritabaniAnahtari(ctx).parola())
        assertEquals(32, VeritabaniAnahtari(ctx).parola().size)
    }
}
```

- [ ] **Adım 2: Derlemenin başarısız olduğunu gör**

Çalıştır: `source ~/.tibi-env && cd ~/tibi && ./gradlew :app:assembleDebugAndroidTest`
Beklenen: DERLEME HATASI, `Unresolved reference 'VeritabaniAnahtari'`.

- [ ] **Adım 3: Anahtarı yaz**

`app/src/main/kotlin/app/tibi/veri/VeritabaniAnahtari.kt`:
```kotlin
package app.tibi.veri

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SQLCipher parolası: 32 rastgele bayt. Parola, Android Keystore'daki (uygulama dışına çıkmayan)
 * AES anahtarıyla şifrelenip uygulamanın özel tercih dosyasında saklanır.
 */
class VeritabaniAnahtari(private val context: Context) {
    private val tercih get() = context.getSharedPreferences("tibi_anahtar", Context.MODE_PRIVATE)

    fun parola(): ByteArray {
        tercih.getString(ALAN, null)?.let { return coz(Base64.decode(it, Base64.NO_WRAP)) }
        val yeni = ByteArray(32).also { SecureRandom().nextBytes(it) }
        tercih.edit().putString(ALAN, Base64.encodeToString(sifrele(yeni), Base64.NO_WRAP)).commit()
        return yeni
    }

    private fun keystoreAnahtari(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(TAKMA_AD, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(TAKMA_AD, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    /** Çıktı: 12 bayt IV + şifreli metin. */
    private fun sifrele(duz: ByteArray): ByteArray {
        val c = Cipher.getInstance(DONUSUM).apply { init(Cipher.ENCRYPT_MODE, keystoreAnahtari()) }
        return c.iv + c.doFinal(duz)
    }

    private fun coz(veri: ByteArray): ByteArray {
        val c = Cipher.getInstance(DONUSUM)
        c.init(Cipher.DECRYPT_MODE, keystoreAnahtari(), GCMParameterSpec(128, veri, 0, 12))
        return c.doFinal(veri, 12, veri.size - 12)
    }

    private companion object {
        const val TAKMA_AD = "tibi_veritabani_anahtari"
        const val ALAN = "sifreli_parola"
        const val DONUSUM = "AES/GCM/NoPadding"
    }
}
```

- [ ] **Adım 4: Şifreli oluşturucuyu ve Application'ı yaz**

`TibiVeritabani.kt` `companion object` içine, `bellekte`'nin üstüne ekle:
```kotlin
        /** Telefonda: SQLCipher ile şifreli dosya. */
        fun olustur(
            context: Context,
            ad: String = "tibi.db",
            parola: ByteArray = VeritabaniAnahtari(context).parola(),
        ): TibiVeritabani {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, TibiVeritabani::class.java, ad)
                .openHelperFactory(net.zetetic.database.sqlcipher.SupportOpenHelperFactory(parola))
                .addCallback(TOHUM)
                .build()
        }
```

`app/src/main/kotlin/app/tibi/TibiUygulama.kt`:
```kotlin
package app.tibi

import android.app.Application
import app.tibi.veri.KayitServisi
import app.tibi.veri.TibiVeritabani

class TibiUygulama : Application() {
    val veritabani: TibiVeritabani by lazy { TibiVeritabani.olustur(this) }
    val kayitServisi: KayitServisi by lazy { KayitServisi(veritabani) }
}
```

`AndroidManifest.xml` içinde `<application` etiketine şu özniteliği ekle:
```xml
        android:name=".TibiUygulama"
```

- [ ] **Adım 5: JVM testlerini ve telefon testini koş**

```bash
source ~/.tibi-env && cd ~/tibi && ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
W="/mnt/c/Users/EZALCO GÜLBAHAR/tibi-araclar"
cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk "$W/"
"$WADB" devices
"$WADB" install -r "$W/app-debug.apk"
"$WADB" install -r "$W/app-debug-androidTest.apk"
"$WADB" shell am instrument -w app.tibi.debug.test/androidx.test.runner.AndroidJUnitRunner
```
Beklenen: JVM tarafında 21 test geçer; telefonda `OK (3 tests)`.
`"$WADB" devices` listesi boşsa telefon bağlı değildir: dur, kullanıcıdan kabloyu takıp USB hata ayıklama iznini onaylamasını iste. Bu adımı atlama.

- [ ] **Adım 6: Uygulamanın veritabanıyla açıldığını doğrula**

`Kabuk.kt`'de değişiklik yok; veritabanı `lazy` olduğu için henüz açılmaz. Açılış çökmesi olmadığını görmek yeterli:
```bash
"$WADB" shell am start -W -n app.tibi.debug/app.tibi.MainActivity | grep Status
"$WADB" logcat -d -b crash | tail -5
```
Beklenen: `Status: ok`, çökme kaydı yok.

- [ ] **Adım 7: CI'a uygulama testlerini ekle**

`.github/workflows/derle.yml` içinde `Çekirdek testleri` adımını şöyle değiştir:
```yaml
      - name: Testler
        run: ./gradlew :core:test :app:testDebugUnitTest
```

- [ ] **Adım 8: Commit, push, CI**

```bash
cd ~/tibi && git add app/ .github/
git commit -m "feat(veri): SQLCipher şifreleme, Keystore anahtarı, uygulamaya bağlama; CI'da uygulama testleri

Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>"
git push
gh run watch --exit-status $(gh run list --limit 1 --json databaseId -q '.[0].databaseId')
```
Beklenen: CI yeşil.

---

## Öz Denetim

- **Kapsam:** Veri modelindeki 8 tablo (Hesap, Kart, Kategori, Hareket, TaksitSatiri, Ekstre, DuzenliKural, Ayar) ve Plan 3'ün kullanacağı yazma/okuma işlemleri: H1–H3, E1–E3, K2, K8, K9, K10, V3 (şifreli, veri dışarı çıkmaz). Maaş kuralı (Dönem) tablo olarak hazır; dönemi okuyan servis Plan 3'te ekranla birlikte gelir. Kapsam dışı tablolar yol haritasındaki planlarında.
- **Yer tutucu:** Görev 4'teki `kartaYansit` bilinçli bir ara adım; Görev 5 Adım 4 onu gerçek hâliyle değiştirir.
- **Tip tutarlılığı:** `tutarKurus`/`…Kurus` adları, `KayitServisi` imzaları, `AylikYuk`, `GecmisTaksitGirisi` ve `TibiVeritabani.bellekte/olustur` bütün görevlerde aynı.
- **Yol haritası güncellemesi:** Plan 2 satırı "bütün tablolar" yerine "Plan 3'ün tabloları" olarak düzeltilir.
