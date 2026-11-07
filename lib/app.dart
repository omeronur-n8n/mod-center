import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

import 'screens/home_shell.dart';
import 'screens/sign_in_screen.dart';
import 'services/auth_service.dart';
import 'services/user_repository.dart';
import 'theme/theme.dart';
import 'widgets/gelisio_background.dart';

class GelisioApp extends StatelessWidget {
  const GelisioApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Gelisio',
      debugShowCheckedModeBanner: false,
      theme: buildGelisioTheme(),
      themeMode: ThemeMode.dark,
      builder: (context, child) => GelisioBackground(child: child ?? const SizedBox()),
      home: const AuthGate(),
    );
  }
}

/// Oturum durumuna göre giriş ekranını ya da ana kabuğu gösterir.
class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<User?>(
      stream: AuthService.authChanges,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        final user = snapshot.data;
        if (user == null) return const SignInScreen();
        UserRepository.ensureProfile(user);
        return HomeShell(user: user);
      },
    );
  }
}
