# tibi: Yol Haritası

Gereksinimler (`docs/specs/2026-09-24-tibi-gereksinimler.md`) birçok ayrı alt sistem içeriyor. Bu yüzden iş tek bir büyük plan yerine sıralı planlara bölündü. Her plan kendi başına çalışan ve test edilebilen bir sürüm çıkarır. Bir planın ayrıntılı görevleri, bir önceki plan bittiğinde yazılır. Böylece her plan, gerçekte yazılmış koda göre hazırlanır.

| # | Plan | Sonunda elde edilen | Gereksinimler |
|---|---|---|---|
| 1 | **Temel** | Derleme araçları, `:core` modülü (para, tarih, dönem, ekstre takvimi, taksit planlayıcı, hepsi testli), 5 sekmeli boş uygulama, GitHub'da imzalı APK üretimi | Temel kararlar, K2, K8 (hesap kısmı), Dönem |
| 2 | Veri katmanı | Room + SQLCipher; bütün tablolar, DAO'lar, Kayıt servisi (tek transaction), bakiye/borç sorguları, testli | Veri modeli, H1–H3, E1–E2, V3 |
| 3 | **İlk kullanılabilir sürüm** | Kurulum (Hoş geldin adı, Hesaplar, Gelir, Kartlar, Devam eden taksitler), hızlı harcama girişi, Özet, Hareketler, kart detayı. Gerçek veriyle kullanmaya bu planın sonunda başlanır. | S-1…S3, E1–E4, K1, K5, K8–K10, G1–G4 |
| 4 | Kilit ve yedek | PIN + parmak izi kilidi, şifreli yedek al / geri yükle, açılışta "yedekten dön" | V1, V1a, V2, S0, S-1 |
| 5 | Ekstre, ödeme, hatırlatma | Kesimde ekstre kapatma, ödeme işaretleme (tam/asgari/kısmi), otomatik ödeme, bildirim altyapısı, son ödeme ve aidat hatırlatması | K3, K4, K6, K11, K12, Bildirimler |
| 6 | Harcama freni | Kendi limitin, toplam tavan, kademeli uyarı, kayıt anında etki, taksit uyarısı, bekleme listesi ve etkisi, vazgeçilenler, harcama hızı, fren ayarları | F1–F9, S4 |
| 7 | Banka tetikleyicisi | Erişilebilirlik servisi, izlenen uygulama → kart eşlemesi, 10 dk kuralı, İzinler adımı | T1–T4, S5 |
| 8 | Düzenli gider/gelir ve avans | Düzenli kurallar, otomatik kayıt ya da "önce sor", maaş "yattı mı?", avans mahsubu | E5–E8, G2, G5, A1–A4 |
| 9 | Cüzdanlar ve birikim | Zarf, kasa, hedef; varlıklar; kur çekici ve önbellek | C1–C5 |
| 10 | Dersler | Öğrenci, ücret geçmişi, ders takvimi, alacak, tahsilat hatırlatması | D1–D7 |
| 11 | Raporlar | Kategori, gelir-gider, taksit yükü, ders, kart borcu raporları | R1–R5 |

Sıralama mantığı:
- **Plan 3'ün sonunda** gerçek veri girilmeye başlanır.
- **Plan 4** hemen arkasından gelir. Gerçek veri telefona girdikten sonra kilit ve yedek ertelenmemeli.
- **Frenin (Plan 6)** doğru çalışması için ekstre ve ödeme bilgisinin (Plan 5) önceden hazır olması gerekir.
