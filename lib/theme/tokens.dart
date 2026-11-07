import 'package:flutter/material.dart';

/// Gelisio tasarım tokenları (Liquid Glass, koyu tema).
abstract final class G {
  // Zemin
  static const bgTop = Color(0xFF1A1F2E);
  static const bgBottom = Color(0xFF07090F);

  // Vurgu (logo gradyanı)
  static const mint = Color(0xFF7CF5D2);
  static const lavender = Color(0xFF8B7CFF);
  static const accent = LinearGradient(
    colors: [mint, lavender],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  // Durum
  static const success = mint;
  static const warning = Color(0xFFFFC56B); // tek kaçırma
  static const critical = Color(0xFFFF6B81); // art arda ikinci kaçırma

  // Yazı
  static const text = Color(0xFFF2F4F8);
  static const textSecondary = Color(0x99F2F4F8); // %60

  // Cam
  static const glassTop = Color(0x17FFFFFF); // %9
  static const glassBottom = Color(0x08FFFFFF); // %3
  static const glassBorder = Color(0x1AFFFFFF); // %10
  static const blur = 24.0;

  // Biçim
  static const radiusCard = 24.0;
  static const radiusButton = 16.0;
  static const motion = Duration(milliseconds: 220);
}
