# Gelisio — PC (web) sürümü

Android uygulamasıyla aynı Firebase projesini (`gelisio-1`) ve aynı Firestore verisini (`users/{uid}/...`) kullanır. Aynı Google hesabıyla girince alışkanlıklar, projeler ve seanslar iki cihazda da aynı görünür.

## Çalıştırma

Dosyaları bir HTTP sunucusuyla aç (Google girişi `file://` üzerinde çalışmaz):

```bash
cd web
python3 -m http.server 8080
```

Sonra tarayıcıda `http://localhost:8080` adresini aç. Chrome/Brave/Edge'de adres çubuğundaki "Uygulamayı yükle" ile masaüstü uygulaması gibi kurulabilir.

## Yayınlama

Firebase Hosting ile yayınlarsan (`firebase deploy --only hosting`) çıkan alan adını Firebase konsolunda **Authentication → Settings → Authorized domains** listesine eklemen gerekir; yoksa Google girişi reddedilir. `localhost` varsayılan olarak yetkilidir.

## Firestore kuralları

Web sürümü de aynı kuralları kullanır (depodaki `firestore.rules`). Kurallar yayınlanmamışsa veriler okunmaz ve yazılmaz; uygulama kırmızı bir uyarı gösterir.

## Bilinen sınırlar

- API anahtarı Android yapılandırmasından alındı. Google Cloud'da anahtar yalnızca Android uygulamalarına kısıtlıysa web çağrıları "API key not valid" ile reddedilir. Çözüm: Google Cloud Console → Credentials altında "Browser key (auto created by Firebase)" anahtarını kullanıp `app.js` içindeki `apiKey` alanını değiştirmek.
- Bildirimler yok (web'de zamanlanmış bildirim yapılamaz).
- Derin seans sayacı bu tarayıcı sekmesinde çalışır; sekme kapanırsa seans kaydedilmemiş kalır.
- Derin blok şablonları, hedefler, sihirbazlar ve skor kartı yalnızca Android'de düzenlenir; web bunları okur ama değiştirmez.
- Hobi fotoğrafları yok (Firebase Storage ücretli plan gerektiriyor).
