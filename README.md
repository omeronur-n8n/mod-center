# Gelisio

Kişisel gelişim uygulaması: kimlik → hedef → haftalık plan → günlük alışkanlık → derin çalışma → gözden geçirme.
Native Android (Kotlin + Jetpack Compose), Firebase Auth (Google) + Cloud Firestore ile cihazlar arası senkron.
Yol haritası ve spesifikasyon: Claude Docs'taki "Gelisio — Yol Haritası ve Spesifikasyon".

## Sekmeler
| Sekme | İçerik |
|---|---|
| Bugün | Tamamlanma halkası, alışkanlık kartları (dokun = tam, uzun bas = minimum, sola kaydır = atla), zincir, kurtarma, Goldilocks önerisi, Sadece başla (2 dk sürüm + 5 dk sayaç), sıradaki derin blok, günün cümlesi, Günlük Özel Zafer |
| Hafta | Roller × hedefler, II. Kare yüzdesi, Pazar planlama sihirbazı, haftalık gözden geçirme, zaman yönetimi matrisi |
| Odak | Çeyreğin hayati hedefi + kılavuz ölçüt, skor cetveli (8 hafta), ritüelli derin seans sayacı, çalışma felsefesi → derin blok planı |
| Gelişim | Projeler (kilometre taşı % + haftalık derin saat), hobiler (seviye, ısı haritası, pratik, üretim kaydı, seviye atlama), gönüllü yalnızlık şablonları |
| Ben | Kimlikler ve oylar, misyon, roller (≤7), değerler, skor kartı, bildirim bütçesi ve sessiz saatler, cümleler, Hakkında |

## Kurulum
1. Firebase projesinde Android uygulaması: paket `com.plan.gelisio`, debug SHA-1 ekle.
2. Authentication → Google sağlayıcısını aç. Firestore Database oluştur.
3. Firestore → Kurallar: `firestore.rules` içeriğini yapıştır, yayınla.
4. `google-services.json` dosyasını `app/` altına koy (git'e girmez).
5. `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## Veri modeli
Tümü `users/{uid}` altında: `identities`, `habits`, `habitLogs` (kimlik `habitId_yyyy-MM-dd`), `goals`, `projects`,
`sessions`, `weeks` (`yyyy-Www`), `scorecard`, `outputs`, `quotes`, `days`.

## Bildirimler
Cihaz üzerinde AlarmManager ile; sunucu yok. Günde en fazla 4 (ayarlanabilir), sessiz saatler 22:00–07:00,
akşam kapanışından sonra sessizlik. Kayıp/utandırma dili yok.

## Ekran görüntüleri
`./gradlew :app:recordPaparazziDebug` sahte veriyle tüm sekmelerin görüntüsünü `app/src/test/snapshots/images/` altına üretir.

## Atıflar
Uicons by Flaticon (Regular Rounded) · Inter (SIL OFL) · Haze (Apache 2.0).
İlkeler kitaplardan alınmıştır; uygulamada kitap metni yoktur, cümleler kendi ifademizdir.
