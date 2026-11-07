import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

/// Google ile giriş. Ek paket gerekmez:
/// Android'de Firebase'in kendi OAuth akışı (signInWithProvider),
/// web'de (CachyOS) açılır pencere (signInWithPopup).
abstract final class AuthService {
  static FirebaseAuth get _auth => FirebaseAuth.instance;

  static Stream<User?> get authChanges => _auth.authStateChanges();

  static Future<void> signInWithGoogle() async {
    final provider = GoogleAuthProvider()..setCustomParameters({'prompt': 'select_account'});
    if (kIsWeb) {
      await _auth.signInWithPopup(provider);
    } else {
      await _auth.signInWithProvider(provider);
    }
  }

  static Future<void> signOut() => _auth.signOut();

  /// Firebase hata kodlarını kısa Türkçe mesajlara çevirir.
  static String messageFor(Object error) {
    if (error is FirebaseAuthException) {
      switch (error.code) {
        case 'popup-closed-by-user':
        case 'cancelled-popup-request':
        case 'web-context-canceled':
          return 'Giriş iptal edildi.';
        case 'network-request-failed':
          return 'İnternet bağlantısı yok.';
        case 'operation-not-allowed':
          return 'Google girişi Firebase konsolunda açık değil.';
        case 'unauthorized-domain':
          return 'Bu alan adı Firebase Auth\'ta yetkili değil.';
        default:
          return 'Giriş başarısız (${error.code}).';
      }
    }
    return 'Giriş başarısız.';
  }
}
