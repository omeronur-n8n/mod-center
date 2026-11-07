import 'package:flutter/material.dart';

import 'tokens.dart';

ThemeData buildGelisioTheme() {
  const scheme = ColorScheme.dark(
    primary: G.mint,
    secondary: G.lavender,
    surface: G.bgBottom,
    error: G.critical,
    onPrimary: G.bgBottom,
    onSecondary: G.bgBottom,
    onSurface: G.text,
  );
  final base = ThemeData(useMaterial3: true, colorScheme: scheme, brightness: Brightness.dark);
  return base.copyWith(
    scaffoldBackgroundColor: Colors.transparent,
    textTheme: base.textTheme.apply(bodyColor: G.text, displayColor: G.text),
    progressIndicatorTheme: const ProgressIndicatorThemeData(color: G.mint),
    snackBarTheme: const SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: G.bgTop,
      contentTextStyle: TextStyle(color: G.text),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: G.glassTop,
      hintStyle: const TextStyle(color: G.textSecondary),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(G.radiusButton),
        borderSide: const BorderSide(color: G.glassBorder),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(G.radiusButton),
        borderSide: const BorderSide(color: G.glassBorder),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(G.radiusButton),
        borderSide: const BorderSide(color: G.mint),
      ),
    ),
  );
}
