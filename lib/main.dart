import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';

import 'app.dart';
import 'firebase_options.dart'; // `flutterfire configure` üretir.

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  // Android'de varsayılan açık; web'de (CachyOS) IndexedDB önbelleğini açar.
  FirebaseFirestore.instance.settings = const Settings(persistenceEnabled: true);
  runApp(const GelisioApp());
}
