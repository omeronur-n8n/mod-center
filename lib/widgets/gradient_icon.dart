import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// Vurgu gradyanıyla boyanmış ikon (seçili sekme, ana eylemler).
class GradientIcon extends StatelessWidget {
  const GradientIcon(this.icon, {super.key, this.size = 22});

  final IconData icon;
  final double size;

  @override
  Widget build(BuildContext context) {
    return ShaderMask(
      blendMode: BlendMode.srcIn,
      shaderCallback: (rect) => G.accent.createShader(rect),
      child: Icon(icon, size: size, color: Colors.white),
    );
  }
}
