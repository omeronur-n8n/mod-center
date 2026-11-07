import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

import '../data/principles.dart';
import '../icons/gelisio_icons.dart';
import '../services/day_repository.dart';
import '../theme/tokens.dart';
import '../widgets/glass_card.dart';
import '../widgets/gradient_icon.dart';
import '../widgets/progress_ring.dart';
import 'screen_frame.dart';

const _months = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];
const _weekdays = ['Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi', 'Pazar'];

String turkishDate(DateTime d) => '${d.day} ${_months[d.month - 1]}, ${_weekdays[d.weekday - 1]}';

class TodayScreen extends StatelessWidget {
  const TodayScreen({super.key, required this.user});

  final User user;

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final firstName = (user.displayName ?? '').split(' ').first;
    return StreamBuilder<DayEntry>(
      stream: DayRepository.watch(user.uid, now),
      builder: (context, snap) {
        final day = snap.data ?? DayEntry.empty;
        return ScreenFrame(
          title: firstName.isEmpty ? 'Bugün' : 'Merhaba $firstName',
          subtitle: turkishDate(now),
          children: [
            Center(
              child: ProgressRing(
                value: day.vitalDone ? 1 : 0,
                size: 176,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      day.vitalDone ? '%100' : '%0',
                      style: const TextStyle(fontSize: 32, fontWeight: FontWeight.w700),
                    ),
                    const Text('bugün', style: TextStyle(fontSize: 13, color: G.textSecondary)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            _VitalTaskCard(uid: user.uid, day: now, entry: day),
            const SizedBox(height: 12),
            _PrincipleCard(principle: principleFor(now)),
            const SizedBox(height: 12),
            const _ComingCard(),
          ],
        );
      },
    );
  }
}

class _VitalTaskCard extends StatelessWidget {
  const _VitalTaskCard({required this.uid, required this.day, required this.entry});

  final String uid;
  final DateTime day;
  final DayEntry entry;

  Future<void> _edit(BuildContext context) async {
    final controller = TextEditingController(text: entry.vitalTask);
    final text = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: G.bgTop,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(G.radiusCard)),
        title: const Text('Bugünün tek hayati işi'),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLength: 120,
          decoration: const InputDecoration(hintText: 'Örn. Excentri ödeme ekranını bitir'),
          onSubmitted: (v) => Navigator.pop(ctx, v),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Vazgeç')),
          FilledButton(onPressed: () => Navigator.pop(ctx, controller.text), child: const Text('Kaydet')),
        ],
      ),
    );
    controller.dispose();
    if (text != null) await DayRepository.setVitalTask(uid, day, text);
  }

  @override
  Widget build(BuildContext context) {
    final hasTask = entry.vitalTask.isNotEmpty;
    return GlassCard(
      highlighted: entry.vitalDone,
      onTap: () => _edit(context),
      child: Row(
        children: [
          const GradientIcon(GelisioIcons.focus, size: 24),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('HAYATİ İŞ', style: TextStyle(fontSize: 11, letterSpacing: 1.2, color: G.textSecondary)),
                const SizedBox(height: 4),
                Text(
                  hasTask ? entry.vitalTask : 'Bugünün tek hayati işini yaz',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: hasTask ? G.text : G.textSecondary,
                    decoration: entry.vitalDone ? TextDecoration.lineThrough : null,
                  ),
                ),
              ],
            ),
          ),
          if (hasTask)
            IconButton(
              tooltip: entry.vitalDone ? 'Geri al' : 'Tamamlandı',
              onPressed: () => DayRepository.setVitalDone(uid, day, !entry.vitalDone),
              icon: Icon(
                GelisioIcons.done,
                color: entry.vitalDone ? G.mint : G.textSecondary,
              ),
            ),
        ],
      ),
    );
  }
}

class _PrincipleCard extends StatelessWidget {
  const _PrincipleCard({required this.principle});

  final Principle principle;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(GelisioIcons.quote, size: 18, color: G.lavender),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(principle.text, style: const TextStyle(fontSize: 15, height: 1.4)),
                const SizedBox(height: 8),
                Text('İlke: ${principle.source}', style: const TextStyle(fontSize: 12, color: G.textSecondary)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ComingCard extends StatelessWidget {
  const _ComingCard();

  @override
  Widget build(BuildContext context) {
    return const GlassCard(
      child: Row(
        children: [
          Icon(GelisioIcons.streak, size: 20, color: G.textSecondary),
          SizedBox(width: 14),
          Expanded(
            child: Text(
              'Alışkanlık kartları v1.0\'da burada: kimlik oyu, 2 dakikalık sürüm, tutarlılık yüzdesi.',
              style: TextStyle(fontSize: 13, color: G.textSecondary, height: 1.4),
            ),
          ),
        ],
      ),
    );
  }
}
