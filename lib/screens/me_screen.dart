import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

import '../icons/gelisio_icons.dart';
import '../services/auth_service.dart';
import '../theme/tokens.dart';
import '../widgets/glass_card.dart';
import 'screen_frame.dart';

const appVersion = '0.1.0';

class MeScreen extends StatelessWidget {
  const MeScreen({super.key, required this.user});

  final User user;

  @override
  Widget build(BuildContext context) {
    final name = user.displayName ?? 'Gelisio kullanıcısı';
    final initial = name.isNotEmpty ? name.characters.first.toUpperCase() : 'G';
    return ScreenFrame(
      title: 'Ben',
      subtitle: 'Kimlik, misyon ve ayarlar',
      children: [
        GlassCard(
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                alignment: Alignment.center,
                decoration: const BoxDecoration(shape: BoxShape.circle, gradient: G.accent),
                child: Text(initial, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: G.bgBottom)),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                    if (user.email != null)
                      Text(user.email!, style: const TextStyle(fontSize: 13, color: G.textSecondary)),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        const GlassCard(
          child: Row(
            children: [
              Icon(GelisioIcons.mission, size: 20, color: G.textSecondary),
              SizedBox(width: 14),
              Expanded(
                child: Text(
                  'Misyon, roller, değerler ve bildirim bütçesi v1.0 kurulum akışında.',
                  style: TextStyle(fontSize: 13, color: G.textSecondary, height: 1.4),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        GlassCard(
          onTap: AuthService.signOut,
          child: const Row(
            children: [
              Icon(GelisioIcons.signOut, size: 20, color: G.critical),
              SizedBox(width: 14),
              Text('Çıkış yap', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
            ],
          ),
        ),
        const SizedBox(height: 24),
        const Text(
          'Gelisio $appVersion\nİkonlar: Uicons by Flaticon (flaticon.com)',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 12, color: G.textSecondary, height: 1.5),
        ),
      ],
    );
  }
}
