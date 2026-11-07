import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// Gelisio'nun temel cam yüzeyi. Ekranda aynı anda 3–4'ten fazla kullanma:
/// her BackdropFilter bir arka plan kopyası demek.
class GlassCard extends StatelessWidget {
  const GlassCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(20),
    this.radius = G.radiusCard,
    this.onTap,
    this.highlighted = false,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final double radius;
  final VoidCallback? onTap;

  /// Vurgulu kart: kenar çizgisi nane rengine döner.
  final bool highlighted;

  @override
  Widget build(BuildContext context) {
    final shape = BorderRadius.circular(radius);
    return ClipRRect(
      borderRadius: shape,
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: G.blur, sigmaY: G.blur),
        child: Material(
          type: MaterialType.transparency,
          child: InkWell(
            onTap: onTap,
            borderRadius: shape,
            splashColor: G.mint.withValues(alpha: 0.08),
            highlightColor: Colors.white.withValues(alpha: 0.03),
            child: Ink(
              decoration: BoxDecoration(
                borderRadius: shape,
                gradient: const LinearGradient(
                  colors: [G.glassTop, G.glassBottom],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
                border: Border.all(
                  color: highlighted ? G.mint.withValues(alpha: 0.55) : G.glassBorder,
                ),
              ),
              child: Padding(padding: padding, child: child),
            ),
          ),
        ),
      ),
    );
  }
}
