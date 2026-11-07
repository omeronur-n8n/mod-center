# Gelisio

Girişimci için alışkanlık, proje ve hobi gelişim takibi. Yöntem 5 kitaptan geliyor
(Atomik Alışkanlıklar, Etkili İnsanların 7 Alışkanlığı, Pürdikkat, Dijital Minimalizm,
Kafaya Takmama Sanatı). Tam spesifikasyon: "Gelisio — Yol Haritası ve Spesifikasyon" dokümanı.

**Sürüm: v0.1 (altyapı).** İçinde: Google ile giriş, Firestore senkronu, Liquid Glass koyu
tema, 5 sekmeli kabuk, Flaticon ikon fontu, logo ve uygulama ikonları. Senkron testi için
Bugün ekranında "hayati iş" alanı var: telefonda yaz, CachyOS'ta anında gör.

| Platform | Nasıl çalışır |
| --- | --- |
| Android | Native Flutter uygulaması |
| CachyOS | Flutter web, Chromium'a PWA olarak kurulur. Firestore Linux masaüstünü resmi desteklemediği için. |

## Kurulum (bir kez)

### 1. Araçlar (CachyOS)
```bash
# Flutter (stable, >= 3.38)
git clone -b stable https://github.com/flutter/flutter.git ~/development/flutter
echo 'export PATH="$PATH:$HOME/development/flutter/bin:$HOME/.pub-cache/bin"' >> ~/.bashrc && source ~/.bashrc
# Web için tarayıcı (Chromium kullanıyorsan)
echo 'export CHROME_EXECUTABLE=$(command -v chromium)' >> ~/.bashrc
# Android derlemesi için Android Studio (SDK + lisanslar)
paru -S android-studio   # veya yay -S android-studio
flutter doctor --android-licenses
# Firebase CLI
sudo npm i -g firebase-tools && firebase login
flutter doctor
```

### 2. Firebase konsolu (console.firebase.google.com)
1. Yeni proje: **gelisio** (PremiApp'ten ayrı).
2. Authentication → Sign-in method → **Google**'ı aç.
3. Firestore Database → Oluştur → konum `eur3 (europe-west)`.
4. Firestore → Kurallar → `firestore.rules` içeriğini yapıştır → Yayınla
   (ya da `firebase deploy --only firestore:rules`).

### 3. Projeyi bağla
```bash
./kurulum.sh
```
Betik platform klasörlerini oluşturur, ikonları yerleştirir, `flutterfire configure` ile
`lib/firebase_options.dart` ve `android/app/google-services.json` dosyalarını üretir,
sonra `flutter analyze` ve `flutter test` çalıştırır.

### 4. Android için SHA-1
```bash
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | grep SHA1
```
Firebase → Proje ayarları → Android uygulaması → **SHA sertifika parmak izi ekle**.
Sonra `flutterfire configure`'ı bir kez daha çalıştır.

## Çalıştırma
```bash
flutter run -d chrome          # CachyOS'ta deneme
flutter run                    # USB'li Android telefon
flutter build apk --release    # APK: build/app/outputs/flutter-apk/
```

## CachyOS'a uygulama olarak kur
```bash
flutter build web --release
firebase deploy --only hosting
```
`https://<proje-id>.web.app` adresini Chromium'da aç → adres çubuğundaki **Yükle** simgesi.
Gelisio uygulama menüsüne kendi penceresiyle eklenir.

## v0.1 kapısı
Giriş ve senkron iki cihazda çalışıyor: telefonda hayati işi yaz ve tamamla, CachyOS'ta
anında görünsün. Bu geçince v1.0 (alışkanlık motoru, bildirimler, Hafta, Odak) başlar.

## Klasörler
| Yol | İçerik |
| --- | --- |
| `lib/theme/` | Renk tokenları ve tema |
| `lib/widgets/` | GlassCard, GlassNavBar, ProgressRing, arka plan |
| `lib/screens/` | Giriş, Bugün, Hafta/Odak/Gelişim (yol haritası), Ben |
| `lib/services/` | Google girişi, kullanıcı ve gün kayıtları (Firestore) |
| `lib/data/principles.dart` | 18 ilke cümlesi (kitap alıntısı değil, kendi ifademiz) |
| `lib/icons/` + `assets/fonts/` | Flaticon UIcons alt kümesi (31 ikon) |
| `android_res/` | Android uygulama ikonları (kurulum betiği kopyalar) |
| `tool/` | İkon fontu ve uygulama ikonu üretim betikleri |

İkonlar: [Uicons by Flaticon](https://www.flaticon.com/uicons), atıfla ücretsiz lisans.
