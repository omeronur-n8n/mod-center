import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gelisio/data/principles.dart';
import 'package:gelisio/services/day_repository.dart';
import 'package:gelisio/theme/theme.dart';
import 'package:gelisio/widgets/glass_card.dart';
import 'package:gelisio/widgets/progress_ring.dart';

void main() {
  test('gün anahtarı yyyy-MM-dd biçiminde', () {
    expect(DayRepository.keyFor(DateTime(2026, 3, 7)), '2026-03-07');
  });

  test('her bağlam için en az bir ilke cümlesi var', () {
    for (final ctx in PrincipleContext.values) {
      expect(principles.where((p) => p.context == ctx), isNotEmpty, reason: ctx.name);
    }
    expect(principleFor(DateTime(2026, 11, 7, 9)).context, PrincipleContext.morning);
    expect(principleFor(DateTime(2026, 11, 7, 21)).context, PrincipleContext.evening);
  });

  testWidgets('cam kart ve ilerleme halkası çiziliyor', (tester) async {
    await tester.pumpWidget(MaterialApp(
      theme: buildGelisioTheme(),
      home: const Scaffold(
        body: Column(children: [
          GlassCard(child: Text('Hayati iş')),
          ProgressRing(value: 0.5, child: Text('%50')),
        ]),
      ),
    ));
    await tester.pumpAndSettle();
    expect(find.text('Hayati iş'), findsOneWidget);
    expect(find.text('%50'), findsOneWidget);
  });
}
