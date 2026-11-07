import 'package:flutter/material.dart';

import '../services/auth_service.dart';
import '../theme/tokens.dart';
import '../widgets/glass_card.dart';

class SignInScreen extends StatefulWidget {
  const SignInScreen({super.key});

  @override
  State<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends State<SignInScreen> {
  bool _busy = false;
  String? _error;

  Future<void> _signIn() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await AuthService.signInWithGoogle();
    } catch (e) {
      if (mounted) setState(() => _error = AuthService.messageFor(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Image.asset('assets/logo/gelisio_mark.png', width: 112, height: 112),
                  const SizedBox(height: 20),
                  const Text(
                    'Gelisio',
                    style: TextStyle(fontSize: 34, fontWeight: FontWeight.w700, letterSpacing: -0.5),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Kimliğini her gün bir tekrarla inşa et.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 15, color: G.textSecondary),
                  ),
                  const SizedBox(height: 40),
                  GlassCard(
                    radius: G.radiusButton,
                    padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
                    highlighted: true,
                    onTap: _busy ? null : _signIn,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        if (_busy)
                          const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        else
                          const Text('G', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: G.mint)),
                        const SizedBox(width: 12),
                        const Text('Google ile devam et', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 16),
                    Text(_error!, textAlign: TextAlign.center, style: const TextStyle(color: G.critical)),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
