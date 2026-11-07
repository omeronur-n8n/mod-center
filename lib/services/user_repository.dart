import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

/// users/{uid} belgesi. Tüm Gelisio verisi bu belgenin altında durur.
abstract final class UserRepository {
  static FirebaseFirestore get _db => FirebaseFirestore.instance;

  static DocumentReference<Map<String, dynamic>> doc(String uid) => _db.collection('users').doc(uid);

  static final Set<String> _ensured = {};

  /// İlk girişte profili oluşturur, sonraki girişlerde son görülmeyi günceller.
  static Future<void> ensureProfile(User user) async {
    if (!_ensured.add(user.uid)) return;
    await doc(user.uid).set({
      'displayName': user.displayName,
      'email': user.email,
      'lastSeenAt': FieldValue.serverTimestamp(),
      'schemaVersion': 1,
    }, SetOptions(merge: true));
  }
}
