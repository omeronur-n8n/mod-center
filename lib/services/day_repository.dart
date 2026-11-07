import 'package:cloud_firestore/cloud_firestore.dart';

import 'user_repository.dart';

/// Günün kaydı: users/{uid}/days/{yyyy-MM-dd}
/// v0.1'de iki cihaz arası senkronu kanıtlamak için "hayati iş" alanını taşır
/// (Pürdikkat: en hayati hedef). v1.0'da alışkanlık kayıtları buraya bağlanır.
class DayEntry {
  const DayEntry({required this.vitalTask, required this.vitalDone});

  final String vitalTask;
  final bool vitalDone;

  static const empty = DayEntry(vitalTask: '', vitalDone: false);

  factory DayEntry.fromMap(Map<String, dynamic>? m) => DayEntry(
        vitalTask: (m?['vitalTask'] as String?) ?? '',
        vitalDone: (m?['vitalDone'] as bool?) ?? false,
      );
}

abstract final class DayRepository {
  static String keyFor(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  static DocumentReference<Map<String, dynamic>> _doc(String uid, DateTime day) =>
      UserRepository.doc(uid).collection('days').doc(keyFor(day));

  static Stream<DayEntry> watch(String uid, DateTime day) =>
      _doc(uid, day).snapshots().map((s) => DayEntry.fromMap(s.data()));

  static Future<void> setVitalTask(String uid, DateTime day, String text) => _doc(uid, day).set({
        'vitalTask': text.trim(),
        'updatedAt': FieldValue.serverTimestamp(),
      }, SetOptions(merge: true));

  static Future<void> setVitalDone(String uid, DateTime day, bool done) => _doc(uid, day).set({
        'vitalDone': done,
        'updatedAt': FieldValue.serverTimestamp(),
      }, SetOptions(merge: true));
}
