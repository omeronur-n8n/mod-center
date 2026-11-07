#!/usr/bin/env bash
# Gelisio v0.1 kurulumu (CachyOS / Arch). Repo kökünde çalıştır: ./kurulum.sh
set -euo pipefail
cd "$(dirname "$0")"

say() { printf '\n\033[1;36m▶ %s\033[0m\n' "$1"; }

command -v flutter >/dev/null || { echo "Flutter bulunamadı. README > 1. adım."; exit 1; }
flutter --version | head -1

say "Android ve web platform klasörleri oluşturuluyor (mevcut dosyalar korunur)"
flutter create --platforms=android,web --org com.omeronur --project-name gelisio .

say "Android uygulama adı ve ikonları"
sed -i 's/android:label="gelisio"/android:label="Gelisio"/' android/app/src/main/AndroidManifest.xml
cp -r android_res/. android/app/src/main/res/

say "Paketler"
flutter pub get

say "Firebase bağlantısı (Firebase projeni seç)"
export PATH="$PATH:$HOME/.pub-cache/bin"
command -v firebase >/dev/null || { echo "firebase-tools yok: sudo npm i -g firebase-tools && firebase login"; exit 1; }
command -v flutterfire >/dev/null || dart pub global activate flutterfire_cli
flutterfire configure --platforms=android,web --android-package-name=com.omeronur.gelisio

say "Kontrol"
flutter analyze --no-fatal-infos
flutter test

say "Hazır. Web: flutter run -d chrome  |  Android: flutter run (telefon USB ile bağlı)"
