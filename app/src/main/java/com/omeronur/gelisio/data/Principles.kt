package com.omeronur.gelisio.data

import java.time.LocalDateTime

/** Kitap alıntısı DEĞİL: her cümle ilgili kitabın fikrinin kendi ifademizle yazılmış hâli. */
enum class PrincipleContext { MORNING, AFTER_MISS, NO_MOTIVATION, DEEP_WORK, WEEKLY_PLAN, HOBBY, EVENING }

data class Principle(val context: PrincipleContext, val text: String, val source: String)

val principles = listOf(
    Principle(PrincipleContext.MORNING, "Bugünkü her tekrar, olmak istediğin kişiye verilmiş bir oy.", "Atomik Alışkanlıklar"),
    Principle(PrincipleContext.MORNING, "Hedef yönü gösterir; seni taşıyan sistemdir.", "Atomik Alışkanlıklar"),
    Principle(PrincipleContext.MORNING, "Bugünün tek hayati işi hangisi?", "Pürdikkat"),
    Principle(PrincipleContext.AFTER_MISS, "Bir kez kaçırmak kaza, iki kez kaçırmak yeni alışkanlık. Bugün 2 dakika yeter.", "Atomik Alışkanlıklar"),
    Principle(PrincipleContext.AFTER_MISS, "Mükemmel gün gerekmez; zinciri tutan minimum yeter.", "Atomik Alışkanlıklar"),
    Principle(PrincipleContext.AFTER_MISS, "Hata bir veri satırıdır. Kaydet, düzelt, devam et.", "Kafaya Takmama Sanatı"),
    Principle(PrincipleContext.NO_MOTIVATION, "Motivasyon eylemden sonra gelir. Önce başla.", "Kafaya Takmama Sanatı"),
    Principle(PrincipleContext.NO_MOTIVATION, "İlk iki dakikayı kazan; gerisi arkasından gelir.", "Atomik Alışkanlıklar"),
    Principle(PrincipleContext.DEEP_WORK, "Meşgul görünmek üretmek değildir.", "Pürdikkat"),
    Principle(PrincipleContext.DEEP_WORK, "Odak bir beceridir; her seans onu büyütür.", "Pürdikkat"),
    Principle(PrincipleContext.DEEP_WORK, "Az hedef, derin iş. Her şeye evet diyen hiçbir şeyi bitirmez.", "Pürdikkat"),
    Principle(PrincipleContext.WEEKLY_PLAN, "Acil olan kendini hatırlatır; önemli olanı sen planlamazsan kimse planlamaz.", "7 Alışkanlık"),
    Principle(PrincipleContext.WEEKLY_PLAN, "Önce rollerini, sonra haftanı planla.", "7 Alışkanlık"),
    Principle(PrincipleContext.WEEKLY_PLAN, "Kontrol edemediğin bir ölçüyü hedef edinme.", "Kafaya Takmama Sanatı"),
    Principle(PrincipleContext.HOBBY, "Boş zaman tüketerek değil, üreterek dolar.", "Dijital Minimalizm"),
    Principle(PrincipleContext.HOBBY, "Ellerinle yaptığın şey ekrandaki saatlerden uzun kalır.", "Dijital Minimalizm"),
    Principle(PrincipleContext.EVENING, "Yarını bu akşam planla, zihnini bu akşam kapat.", "Pürdikkat"),
    Principle(PrincipleContext.EVENING, "Hangi zorluğa razısın? Hedefin aslında o.", "Kafaya Takmama Sanatı"),
)

/** Günün cümlesi: saate göre bağlam, aynı bağlamda güne göre dönüşümlü. */
fun principleFor(now: LocalDateTime): Principle {
    val ctx = when {
        now.hour < 12 -> PrincipleContext.MORNING
        now.hour < 18 -> PrincipleContext.DEEP_WORK
        else -> PrincipleContext.EVENING
    }
    val pool = principles.filter { it.context == ctx }
    return pool[now.dayOfYear % pool.size]
}
