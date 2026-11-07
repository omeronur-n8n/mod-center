import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

import '../icons/gelisio_icons.dart';
import '../widgets/glass_nav_bar.dart';
import 'me_screen.dart';
import 'roadmap_screen.dart';
import 'today_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.user});

  final User user;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  static const _items = [
    GlassNavItem(GelisioIcons.today, 'Bugün'),
    GlassNavItem(GelisioIcons.week, 'Hafta'),
    GlassNavItem(GelisioIcons.focus, 'Odak'),
    GlassNavItem(GelisioIcons.growth, 'Gelişim'),
    GlassNavItem(GelisioIcons.me, 'Ben'),
  ];

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      TodayScreen(user: widget.user),
      const RoadmapScreen.week(),
      const RoadmapScreen.focus(),
      const RoadmapScreen.growth(),
      MeScreen(user: widget.user),
    ];
    return Scaffold(
      extendBody: true,
      body: SafeArea(
        bottom: false,
        child: IndexedStack(index: _index, children: pages),
      ),
      bottomNavigationBar: GlassNavBar(
        items: _items,
        index: _index,
        onChanged: (i) => setState(() => _index = i),
      ),
    );
  }
}
