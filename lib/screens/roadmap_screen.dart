import 'package:flutter/material.dart';

import '../icons/gelisio_icons.dart';
import '../theme/tokens.dart';
import '../widgets/glass_card.dart';
import '../widgets/gradient_icon.dart';
import 'screen_frame.dart';

/// v0.1'de henüz yapılmamış sekmeler: ne geleceğini ve hangi ilkeden geldiğini gösterir.
class RoadmapScreen extends StatelessWidget {
  const RoadmapScreen._(this.title, this.subtitle, this.icon, this.items);

  const RoadmapScreen.week()
      : this._('Hafta', 'Önce roller, sonra hafta', GelisioIcons.week, const [
          ('Pazar planlama sihirbazı', 'Her rol için 2–3 hedef · 7 Alışkanlık'),
          ('II. Kare yüzdesi', 'Acil olmayan ama önemli işlerin payı · 7 Alışkanlık'),
          ('Haftalık gözden geçirme', 'Ne işe yaradı, ne yaramadı, neyi değiştiriyorum'),
        ]);

  const RoadmapScreen.focus()
      : this._('Odak', 'Derin çalışma ve skor cetveli', GelisioIcons.focus, const [
          ('Derin çalışma sayacı', 'Keşiş, çift modlu, ritmik veya gazeteci şablonu · Pürdikkat'),
          ('Ritüel kontrolü', 'Yer, süre, kural, destek · Pürdikkat'),
          ('Skor cetveli', 'Haftalık kılavuz ölçüt: derin saat · Pürdikkat'),
        ]);

  const RoadmapScreen.growth()
      : this._('Gelişim', 'Projeler ve hobiler', GelisioIcons.growth, const [
          ('Projeler', 'Kilometre taşları + kılavuz ölçüt · v1.1'),
          ('Hobiler', 'Beceri seviyesi, pratik ısı haritası, üretim galerisi · Dijital Minimalizm'),
          ('Seviye atlama', 'Goldilocks kuralı: 4 hafta hedef tutunca · Atomik Alışkanlıklar'),
        ]);

  final String title;
  final String subtitle;
  final IconData icon;
  final List<(String, String)> items;

  @override
  Widget build(BuildContext context) {
    return ScreenFrame(
      title: title,
      subtitle: subtitle,
      children: [
        for (final (name, detail) in items) ...[
          GlassCard(
            child: Row(
              children: [
                GradientIcon(icon, size: 22),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                      const SizedBox(height: 4),
                      Text(detail, style: const TextStyle(fontSize: 13, color: G.textSecondary, height: 1.35)),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
        ],
        const Padding(
          padding: EdgeInsets.only(top: 8),
          child: Text('Yol haritası: v1.0', textAlign: TextAlign.center, style: TextStyle(fontSize: 12, color: G.textSecondary)),
        ),
      ],
    );
  }
}
