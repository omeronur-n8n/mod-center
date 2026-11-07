import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// Sekme sayfalarının ortak iskeleti: başlık + kaydırılabilir içerik.
/// Alttaki boşluk cam sekme çubuğunun arkasında içerik kalmasın diye.
class ScreenFrame extends StatelessWidget {
  const ScreenFrame({super.key, required this.title, this.subtitle, required this.children});

  final String title;
  final String? subtitle;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 640),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 24, 20, 120),
          children: [
            Text(title, style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w700, letterSpacing: -0.5)),
            if (subtitle != null) ...[
              const SizedBox(height: 4),
              Text(subtitle!, style: const TextStyle(fontSize: 14, color: G.textSecondary)),
            ],
            const SizedBox(height: 20),
            ...children,
          ],
        ),
      ),
    );
  }
}
