# tibi: Gereksinim Dokümanı

- **Tarih:** 2026-09-24
- **Durum:** Taslak, kullanıcı onayı bekliyor
- **Ekran taslağı:** https://claude.ai/artifact/KYr1V7rGKu32icN8Dy7tXm

## 1. Amaç

Tek kişinin kullanacağı Android uygulaması. Harcamaları, taksitleri, gelirleri (maaş, avans, özel ders), kredi kartlarının ekstre ve son ödeme günlerini, bütçe cüzdanlarını ve birikimleri takip eder. Asıl hedef, kredi kartı limitlerini aşmayı önlemek: uygulama harcama anında kullanıcıyı frenler.

## 2. Temel Kararlar

| Konu | Karar |
|---|---|
| Kullanıcı | Tek kişi. Hesap veya giriş yok. |
| Dağıtım | APK elle kurulur. Play Store'a konmaz. |
| Teknoloji | Kotlin, yerel Android (Jetpack Compose, Room/SQLite) |
| Veri | Sadece telefonda durur. Bulut ve otomatik yedek yok. |
| İnternet | Yalnızca kur ve fon fiyatını çekmek için. Kullanıcı verisi dışarı gönderilmez. |
| Harcama girişi | Tamamen elle. Bankanın bildirimi veya SMS'i okunmaz. |
| Para birimi | TL. Döviz yalnızca yatırım varlığı olarak tutulur. |
| Dönem | "Bu dönem" maaş gününden bir sonraki maaş gününün bir gün öncesine kadardır (ör. 30 Ağu – 29 Eyl). Bütçe zarfları, harcama hızı, toplam kart tavanı ve Özet/Hareketler/Raporlar bu döneme göre çalışır. Kart ekstreleri kendi kesim günlerine, ders takvimi ve ay sonu tahsilatı takvim ayına göre kalır. |

## 3. Fonksiyonel Gereksinimler

### 3.1 Hesaplar
- **H1.** Üç hesap türü olacak: kredi kartı, banka hesabı (vadesiz veya banka kartı) ve nakit.
- **H2.** Her hesabın güncel bakiyesi veya borcu görünür.
- **H3.** Hesaplar arasında transfer yapılabilir: hesaptan hesaba, hesaptan cüzdana, hesaptan kart ödemesine.

### 3.2 Harcama
- **E1.** Harcama kaydında şu alanlar olur: tutar, kategori, hesap, cüzdan (isteğe bağlı), tarih (varsayılan: bugün), not (isteğe bağlı).
- **E2.** Kredi kartı harcaması tek çekim ya da N taksit olabilir. Taksitli harcamada erteleme girilebilir (ör. "ilk taksit 3 ay sonra"); ilk taksit o kadar ay ileri kayar.
- **E3.** Kategoriler kullanıcı tarafından tanımlanır, düzenlenir ve silinir. Hazır bir başlangıç seti gelir.
- **E4.** Hızlı giriş formunda en sık kullanılan kategoriler önde durur.

### 3.3 Banka uygulaması tetikleyicisi
- **T1.** Kullanıcı ayarlardan izlenecek banka uygulamalarını seçer ve her uygulamayı bir hesaba veya karta bağlar.
- **T2.** Seçili bir banka uygulaması kapatıldığında "Harcama yaptın mı?" bildirimi gelir. Bunun için Kullanım Erişimi (UsageStats) izni kullanılır.
- **T3.** Bildirimde "Evet" seçilince hızlı giriş formu açılır, bağlı kart önceden seçili gelir. "Hayır" seçilince bildirim kapanır.
- **T4.** Aynı uygulama kısa sürede tekrar açılıp kapatılırsa bildirim tekrarlanmaz. Bekleme süresi ayarlanabilir, varsayılanı 10 dakika.

### 3.4 Kredi kartı
- **K1.** Kartın şu bilgileri tutulur: ad, renk, son 4 hane, bağlı banka uygulaması, kesim günü, son ödeme günü, banka limiti, kullanıcının kendi limiti. Kart numarasının tamamı, son kullanma tarihi ve CVV istenmez ve saklanmaz.
- **K2.** Taksitli bir harcama, kesim gününe göre gelecek ekstrelere otomatik olarak dağıtılır.
- **K3.** Kesim gününde ekstre tutarı hesaplanır. Asgari ödeme tutarı kartın asgari oranından (ör. %40) hesaplanır, istenirse elle düzeltilir.
- **K4.** Ödeme üç şekilde işaretlenebilir: tamamı, asgari veya kısmi. Ödeme bir banka hesabından yapılır. Ödenmeyen kalan bir sonraki döneme devreder.
- **K5.** Kart ekranında şunlar görünür: toplam borç, kendi limitine göre kalan tutar, dönem içi harcamalar, aktif taksitler (ör. 5/12). Banka limiti küçük yazıyla görünür.
- **K6.** Son ödeme gününden 3 gün ve 1 gün önce hatırlatma gelir. Gün sayıları ayarlanabilir.
- **K7.** Faiz hesabı yapılmaz.
- **K8. Geçmişe dönük taksit:** Uygulamaya başlamadan önce yapılmış ve hâlâ ekstreye yansıyan taksitler girilebilir. Taksit türü seçilir: alışveriş, ekstre taksitlendirme veya nakit avans; raporlarda ayrı gösterilir. Tutar üç yoldan biriyle girilir: aylık taksit tutarı + toplam taksit sayısı, toplam tutar + toplam taksit sayısı, ya da kalan borç + kalan taksit sayısı. İlk iki yolda sıradaki ekstrede kaçıncı taksit olduğu (ya da ilk taksit ayı) ve varsa erteleme de girilir. Ayrıca açıklama, kart ve kategori girilir. Kalan taksitler gelecek ekstrelere dağıtılır ve kalan plan hemen gösterilir. Ödenmiş taksitler geçmiş aylara gider olarak yazılmaz, raporları bozmaz.
- **K9. Açılış borcu:** Kart eklenirken kesilmiş ama ödenmemiş ekstre tutarı ve dönem içindeki tek çekimlerin toplamı girilir. Son ödeme tarihi kartın son ödeme gününden hesaplanır. Uygulama bugünkü durumdan başlar.
- **K10. Ortak limit:** Kart türü ana, ek veya sanal olabilir. Ek ve sanal kart bir ana karta bağlanır ve onun limitini paylaşır; limit ve borç ana kartta birlikte hesaplanır, iki kez sayılmaz. Harcamalar yine hangi karttan yapıldıysa o kartta listelenir.
- **K11. Otomatik ödeme talimatı:** Kartta talimat işaretliyse son ödeme gününde ekstre bağlı banka hesabından "tamamı ödendi" olarak kendiliğinden işaretlenir. Kullanıcı sonradan düzeltebilir.
- **K12. Yıllık aidat:** Aidat ayı ve tutarı girilirse o aydan önce hatırlatma gelir (iade istemek ya da kartı kapatmak için).

### 3.5 Harcama freni
- **F1. Kendi limitin:** Her karta banka limitinden bağımsız bir kişisel sınır konur. Kalan tutar bu sınıra göre gösterilir.
- **F2. Toplam kart tavanı:** Tüm kartların toplam borcu için tek bir sınır konur. Sınır sabit tutar olarak ya da net gelirin yüzdesi olarak belirlenir.
- **F3. Kademeli uyarı:** Sınırın %70'inde sarı, %90'ında kırmızı uyarı çıkar. Sınır aşılınca Özet ekranının en üstünde kalıcı bir uyarı durur. Eşikler ayarlanabilir.
- **F4. Kayıt anında etki:** Kaydetmeden önce şunlar gösterilir: bu karttaki kalan kişisel limit ve gelecek ayki kart ödemesinin beklenen maaşa oranı.
- **F5. Taksit uyarısı:** Taksitli girişte, gelecek aylara eklenecek aylık tutar ve bitiş ayı gösterilir.
- **F6. Bekleme listesi:** Belirli bir tutarın üstündeki alışverişler "48 saat beklet" ile listeye alınır. Süre dolunca "Hâlâ istiyor musun?" bildirimi gelir. Cevap "Evet" ise harcama formu açılır, "Hayır" ise kayıt vazgeçilenlere taşınır. Tutar eşiği ve süre ayarlanabilir.
- **F7. Harcama hızı:** Özet ekranında şöyle bir cümle görünür: "Ayın %X'i geçti, kart bütçenin %Y'sini harcadın."
- **F8. Vazgeçilenler sayacı:** Bekleme listesinde bu ay vazgeçilen alışverişlerin toplamı ve adedi gösterilir.
- **F9. Bekleme listesinin gelecek aylara etkisi:** Bekleyen her ürün, alınması planlanan kart ve taksit sayısıyla kaydedilir. Ürünün altında aylık taksit tutarı, hangi aylara yansıyacağı ve kendi limitini aşıp aşmayacağı görünür. "Hepsini alırsan" tablosu, bekleyen ürünlerin toplam etkisini gelecek ayların mevcut taksit yüküne ekleyerek gösterir (şu an → alırsan), ayrıca gelecek ayki kart ödemesinin maaşa oranındaki değişimi verir.

### 3.6 Gelir
- **G1.** Gelir kaynakları kullanıcı tarafından tanımlanır, ör. Maaş, Özel ders, Diğer.
- **G2.** Düzenli gelirin tekrar kuralı (aylık, haftalık, belirli bir gün) ve beklenen tutarı tanımlanır. Maaş günü hafta sonuna denk gelirse ne olacağı seçilir (önceki iş günü / sonraki iş günü / aynı gün); dönem başlangıcı buna göre kayar. Beklenen günde "Yattı mı?" hatırlatması gelir. Tutar o an düzeltilebilir.
- **G3.** Düzensiz gelir tek seferlik girilir.
- **G4.** Her gelir bir hesaba veya nakde yatar.
- **G5.** Gelirin bir kısmı cüzdanlara otomatik dağıtılabilir, ör. maaşın %10'u Birikim'e. Bu isteğe bağlıdır.

### 3.7 Avans
- **A1.** Avans kaydında tutar, tarih ve yattığı hesap girilir. Bakiye o gün artar. Avans normal gelir sayılmaz, "maaştan düşülecek" olarak işaretlenir.
- **A2.** Avans her zaman bir sonraki maaştan tek seferde düşülür. Beklenen maaş tutarı kendiliğinden azalır ve "Yattı mı?" hatırlatması bu azalmış tutarla gelir.
- **A3.** Özet ekranında "Maaştan düşülecek avans" satırı görünür.
- **A4.** F2 ve F4'teki oranlar, maaşın avans düşülmüş hâline göre hesaplanır.

### 3.8 Özel ders
- **D1.** Öğrenci kaydında şunlar tutulur: ad, saatlik ücret, ödeme şekli (ders başı, ay sonu veya N derslik paket).
- **D2.** Ücret değişince eski dersler eski ücretle kalır. Ücretin geçmişi saklanır.
- **D3.** Ders kaydında tarih, öğrenci ve süre (saat, ondalıklı olabilir) girilir. Tutar otomatik hesaplanır. Planlanan ders "Yapıldı" veya "İptal" olarak işaretlenir.
- **D4.** Her öğrencinin alacağı hesaplanır: yapılan derslerin toplamından tahsilatlar düşülür.
- **D5.** Tahsilat gelir olarak kaydedilir ve bir hesaba veya nakde yatar. Tutar alacaktan düşer.
- **D6.** Ödeme şekline göre tahsilat hatırlatması gelir: ay sonunda, paket bitince veya ders sonrasında.
- **D7.** Ders ekranında aylık takvim, ders saati toplamı ve kazanç görünür.

### 3.9 Cüzdanlar ve birikim
- **C1. Bütçe zarfı:** Bir veya daha fazla kategoriye aylık limit konur. Harcandıkça limitten düşer. Uyarılar F3'teki eşiklerle çalışır.
- **C2. Para kasası:** Gerçek bakiyesi olan cüzdandır. Hesaptan para aktarılarak dolar, harcamalar bu cüzdandan yapılabilir.
- **C3. Hedef:** Hedef tutar ve tarih konur. Ne kadar biriktiği, ayda ne kadar ayırmak gerektiği ve hedefe yetişip yetişilmediği görünür.
- **C4. Yatırım:** Varlık türleri altın (gram), döviz (USD, EUR vb.) ve fondur. Alış tarihi, miktar ve maliyet girilir. Güncel değer internetten çekilen kurla hesaplanır, kâr/zarar TL ve yüzde olarak görünür.
- **C5.** Kur çekilemezse son bilinen kur, tarihiyle birlikte gösterilir.

### 3.10 Raporlar
- **R1.** Kategorilere göre harcama dağılımı (aylık)
- **R2.** Aylık gelir ve gider. Gelir kaynaklara göre ayrılır, avans ayrı gösterilir.
- **R3.** Gelecek ayların taksit yükü
- **R4.** Ders kazancı ve saatleri, öğrencilere göre dağılım
- **R5.** Kartlara göre borç gelişimi

### 3.11 İlk kurulum
- **S-1. Açılış:** Uygulama ilk açıldığında iki seçenek sunar: "Yeni başla" (kuruluma gider) ve "Yedekten geri yükle" (şifreli yedek dosyası ve yedek şifresi istenir, kurulum atlanır).
- **S0.** Hoş geldin adımında hitap adı girilir (bildirimlerde kullanılır) ve uygulama kilidi kurulur: PIN zorunlu, parmak izi isteğe bağlı, arka plana alınınca kilitlenme süresi.
- **S1.** İlk açılışta 7 adımlı kurulum çalışır: Hoş geldin, Hesaplar ve nakit, Gelir, Kartlar, Devam eden taksitler, Harcama freni, İzinler. Hesaplar önce gelir, çünkü maaş ve kart ödemeleri hesaplara bağlanır. Hoş geldin dışındaki her adım atlanabilir ve sonradan Ayarlar'dan açılabilir.
- **S2.** Kart sayısında sınır yoktur. Her kart için K1 ve K9'daki alanlar girilir.
- **S3.** Banka hesapları ve nakit için açılış bakiyesi girilir.
- **S4.** Harcama freni değerleri (F2, F3, F6) kullanıcı tarafından belirlenir. Önerilen başlangıç değerleri dolu gelir: tavan net gelirin %40'ı, uyarı eşikleri %70 ve %90, bekleme eşiği 2.000 ₺, süre 48 saat.
- **S5.** İzinler adımında bildirim izni ve kullanım erişimi izni istenir; her iznin neden gerektiği tek cümleyle açıklanır. Kullanım erişimi verilmezse yalnızca banka tetikleyicisi çalışmaz.

### 3.12 Güvenlik ve veri
- **V1.** Uygulama her açılışta ve arka plandan dönünce (ayarlanan süreden sonra) kilit ekranı gösterir: parmak izi ya da PIN. Kilit ekranında tutar veya bakiye görünmez.
- **V1a.** PIN unutulursa parmak iziyle girilip PIN değiştirilebilir. Parmak izi de yoksa kurtarma yoktur: uygulama sıfırlanır ve yedekten geri yüklenir. Kullanıcı kurulumda bu konuda uyarılır.
- **V2.** Ayarlardan elle şifreli yedek dosyası alınır ve cihaz hafızasına kaydedilir. Aynı dosyadan geri yükleme yapılabilir.
- **V3.** Uygulama hiçbir kullanıcı verisini ağ üzerinden göndermez.

## 4. Ekranlar

Alt menüde 5 sekme olacak, her ekranda bir "+" butonu duracak. "+" ile açılan hızlı giriş: Harcama, Gelir, Avans, Transfer, Ders, Tahsilat.

| Sekme | İçerik |
|---|---|
| Özet | Kart tavanı ve harcama hızı uyarısı, bu ay kalan para, gelir ve gider, yaklaşan ödeme ve tahsilatlar, düşülecek avans, bütçe zarfları, raporlara geçiş |
| Hareketler | Tüm kayıtlar; filtre (tarih, hesap, kategori, tür) ve arama |
| Hesaplar | Kartlar (detayda ekstre, ödeme, taksitler), banka hesapları, nakit |
| Cüzdanlar | Bütçe zarfları, para kasaları, hedefler, yatırımlar, bekleme listesi |
| Dersler | Takvim, öğrenciler, alacaklar |

Ayarlar sağ üstteki menüden açılır: kilit, yedek, izlenen banka uygulamaları, kategoriler, uyarı eşikleri.

## 5. Bildirimler

| Tetikleyici | Mesaj |
|---|---|
| Banka uygulaması kapandı | Harcama yaptın mı? |
| Son ödeme günü yaklaştı | X kartı: N gün, tutar |
| Düzenli gelir günü geldi | Maaş yattı mı? (avans düşülmüş tutarla) |
| Tahsilat zamanı geldi | Ayşe'den 4.800 TL tahsil edilecek |
| Bekleme süresi doldu | Hâlâ istiyor musun? |
| Kart sınırında %70 veya %90 aşıldı | Sarı veya kırmızı uyarı |

## 6. Kapsam Dışı (ilk sürüm)

Bildirim veya SMS okuma, faiz hesabı, krediler (ihtiyaç, konut, KMH), birden fazla kullanıcı, bulut yedek, iOS.

## 7. Açık Konular

1. **Kur kaynağı:** Hangi ücretsiz API kullanılacak? Teknik tasarımda seçilecek.
2. **Varsayılan kategoriler:** Başlangıç seti nasıl olsun?

Kapandı: fren eşiklerinin başlangıç değerleri kurulumda kullanıcı tarafından girilir (S4). "Bu ay" maaş döneminden maaşa sayılır (bkz. Temel Kararlar, Dönem).
