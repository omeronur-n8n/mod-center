package com.omeronur.gelisio.data

import android.content.Context
import java.time.LocalDate

/** Kitap alıntısı DEĞİL: her cümle ilgili kitabın fikrinin kendi ifademizle yazılmış hâli. */
enum class PrincipleContext(val key: String, val label: String) {
    MORNING("morning", "Sabah"),
    AFTER_MISS("after_miss", "Kaçırma sonrası"),
    NO_MOTIVATION("no_motivation", "Motivasyonsuz an"),
    DEEP_WORK("deep_work", "Derin çalışma öncesi"),
    WEEKLY_PLAN("weekly_plan", "Pazar planı"),
    HOBBY("hobby", "Hobi"),
    EVENING("evening", "Akşam"),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: MORNING
    }
}

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

/**
 * Cümle seçimi: rastgele değil, bağlama göre. Aynı gün aynı bağlam için aynı cümle döner
 * (günde en fazla 1 kez), aynı cümle 14 gün içinde tekrar etmez. Kendi cümlelerin havuza katılır.
 */
object Quotes {
    private const val PREFS = "gelisio_quotes"

    fun pool(context: PrincipleContext, custom: List<CustomQuote>): List<Principle> =
        principles.filter { it.context == context } +
            custom.filter { PrincipleContext.of(it.context) == context && it.text.isNotBlank() }
                .map { Principle(context, it.text, "Kendi cümlen") }

    fun pick(ctx: Context, context: PrincipleContext, custom: List<CustomQuote>, today: LocalDate = LocalDate.now()): Principle =
        runCatching { pickStored(ctx, context, custom, today) }.getOrElse { pool(context, custom)[today.dayOfYear % pool(context, custom).size] }

    private fun pickStored(ctx: Context, context: PrincipleContext, custom: List<CustomQuote>, today: LocalDate): Principle {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pool = pool(context, custom)
        val todayKey = "pick_${context.key}_$today"
        sp.getString(todayKey, null)?.let { saved -> pool.firstOrNull { it.text == saved }?.let { return it } }
        val history = sp.getStringSet("history", emptySet()).orEmpty()
            .mapNotNull { e -> e.split("|", limit = 2).takeIf { it.size == 2 }?.let { LocalDate.parse(it[0]) to it[1] } }
            .filter { it.first.isAfter(today.minusDays(14)) }
        val recent = history.map { it.second }.toSet()
        val fresh = pool.filter { it.text !in recent }
        val choice = (fresh.ifEmpty { pool })[(today.dayOfYear + context.ordinal) % (fresh.ifEmpty { pool }).size]
        val newHistory = history.map { "${it.first}|${it.second}" }.toMutableSet().apply { add("$today|${choice.text}") }
        sp.edit().putString(todayKey, choice.text).putStringSet("history", newHistory).apply()
        return choice
    }

    /** Günün cümlesi kartı için bağlam: dün kaçırma varsa kurtarma, Pazar akşamı plan, yoksa saate göre. */
    fun dayContext(now: java.time.LocalDateTime, missedYesterday: Boolean): PrincipleContext = when {
        missedYesterday -> PrincipleContext.AFTER_MISS
        now.dayOfWeek == java.time.DayOfWeek.SUNDAY && now.hour >= 17 -> PrincipleContext.WEEKLY_PLAN
        now.hour < 12 -> PrincipleContext.MORNING
        now.hour >= 18 -> PrincipleContext.EVENING
        else -> PrincipleContext.DEEP_WORK
    }
}
