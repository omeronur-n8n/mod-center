/// Gelisio ilke cümleleri. Kitaplardan alıntı DEĞİLDİR: her biri ilgili kitabın
/// fikrinin kendi ifademizle yazılmış hâlidir (telif nedeniyle).
enum PrincipleContext { morning, afterMiss, noMotivation, deepWork, weeklyPlan, hobby, evening }

class Principle {
  const Principle(this.context, this.text, this.source);
  final PrincipleContext context;
  final String text;
  final String source;
}

const principles = <Principle>[
  Principle(PrincipleContext.morning, 'Bugünkü her tekrar, olmak istediğin kişiye verilmiş bir oy.', 'Atomik Alışkanlıklar'),
  Principle(PrincipleContext.morning, 'Hedef yönü gösterir; seni taşıyan sistemdir.', 'Atomik Alışkanlıklar'),
  Principle(PrincipleContext.morning, 'Bugünün tek hayati işi hangisi?', 'Pürdikkat'),
  Principle(PrincipleContext.afterMiss, 'Bir kez kaçırmak kaza, iki kez kaçırmak yeni alışkanlık. Bugün 2 dakika yeter.', 'Atomik Alışkanlıklar'),
  Principle(PrincipleContext.afterMiss, 'Mükemmel gün gerekmez; zinciri tutan minimum yeter.', 'Atomik Alışkanlıklar'),
  Principle(PrincipleContext.afterMiss, 'Hata bir veri satırıdır. Kaydet, düzelt, devam et.', 'Kafaya Takmama Sanatı'),
  Principle(PrincipleContext.noMotivation, 'Motivasyon eylemden sonra gelir. Önce başla.', 'Kafaya Takmama Sanatı'),
  Principle(PrincipleContext.noMotivation, 'İlk iki dakikayı kazan; gerisi arkasından gelir.', 'Atomik Alışkanlıklar'),
  Principle(PrincipleContext.deepWork, 'Meşgul görünmek üretmek değildir.', 'Pürdikkat'),
  Principle(PrincipleContext.deepWork, 'Odak bir beceridir; her seans onu büyütür.', 'Pürdikkat'),
  Principle(PrincipleContext.deepWork, 'Az hedef, derin iş. Her şeye evet diyen hiçbir şeyi bitirmez.', 'Pürdikkat'),
  Principle(PrincipleContext.weeklyPlan, 'Acil olan kendini hatırlatır; önemli olanı sen planlamazsan kimse planlamaz.', '7 Alışkanlık'),
  Principle(PrincipleContext.weeklyPlan, 'Önce rollerini, sonra haftanı planla.', '7 Alışkanlık'),
  Principle(PrincipleContext.weeklyPlan, 'Kontrol edemediğin bir ölçüyü hedef edinme.', 'Kafaya Takmama Sanatı'),
  Principle(PrincipleContext.hobby, 'Boş zaman tüketerek değil, üreterek dolar.', 'Dijital Minimalizm'),
  Principle(PrincipleContext.hobby, 'Ellerinle yaptığın şey ekrandaki saatlerden uzun kalır.', 'Dijital Minimalizm'),
  Principle(PrincipleContext.evening, 'Yarını bu akşam planla, zihnini bu akşam kapat.', 'Pürdikkat'),
  Principle(PrincipleContext.evening, 'Hangi zorluğa razısın? Hedefin aslında o.', 'Kafaya Takmama Sanatı'),
];

/// Günün cümlesi: saate göre bağlam seçer, aynı bağlamda günden güne döner.
Principle principleFor(DateTime now) {
  final ctx = now.hour < 12
      ? PrincipleContext.morning
      : now.hour < 18
          ? PrincipleContext.deepWork
          : PrincipleContext.evening;
  final pool = principles.where((p) => p.context == ctx).toList();
  final dayOfYear = now.difference(DateTime(now.year)).inDays;
  return pool[dayOfYear % pool.length];
}
