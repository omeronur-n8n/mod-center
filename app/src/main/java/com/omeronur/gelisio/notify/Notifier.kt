package com.omeronur.gelisio.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.omeronur.gelisio.MainActivity
import com.omeronur.gelisio.R
import com.omeronur.gelisio.data.Store
import com.omeronur.gelisio.data.isScheduled
import com.omeronur.gelisio.data.parseTime
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Cihaz üzerinde zamanlanan bildirimler (sunucu/FCM yok, internetsiz çalışır).
 * Bütçe: günde en fazla N bildirim (varsayılan 4); sessiz saatler; akşam kapanışından sonra sessizlik.
 * Dil hatırlatır, utandırmaz: kayıp mesajı yok.
 */
object Notifier {
    const val CH_HABITS = "habits"
    const val CH_FOCUS = "focus"
    const val CH_RITUALS = "rituals"
    private const val PREFS = "gelisio_notify"
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_HABITS, "Alışkanlık hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_FOCUS, "Derin çalışma", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_RITUALS, "Akşam kapanışı ve Pazar planı", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Veri değiştikçe çağrılır; art arda değişiklikleri 1,5 sn bekleyip tek seferde planlar. */
    fun requestReschedule(ctx: Context) {
        pending?.let { handler.removeCallbacks(it) }
        pending = Runnable { reschedule(ctx) }.also { handler.postDelayed(it, 1500) }
    }

    // ---------- Plan anlık görüntüsü (uygulama kapalıyken de alarmlar kurulabilsin) ----------
    private fun buildPlan(): JSONObject {
        val p = Store.profile.value
        val habits = JSONArray()
        Store.habits.value.filter { !it.archived && it.cue.type == "time" && it.cue.time.isNotBlank() }.forEach { h ->
            habits.put(
                JSONObject()
                    .put("id", h.id).put("name", h.name).put("time", h.cue.time).put("place", h.cue.place)
                    .put("full", h.fullVersion).put("min", h.twoMinVersion)
                    .put("days", JSONArray(h.days)).put("createdAt", h.createdAt.toString()),
            )
        }
        val blocks = JSONArray()
        p.deepBlocks.forEach { b ->
            val title = Store.projects.value.firstOrNull { it.id == b.projectId }?.title.orEmpty()
            blocks.put(JSONObject().put("day", b.day).put("time", b.time).put("minutes", b.minutes).put("projectId", b.projectId).put("title", title))
        }
        return JSONObject()
            .put("habits", habits).put("blocks", blocks)
            .put("budget", p.notifBudget).put("quietStart", p.quietStart).put("quietEnd", p.quietEnd)
            .put("evening", p.eveningTime).put("sunday", p.sundayPlanning)
            .put("allHabitDays", JSONArray(Store.habits.value.filter { !it.archived }.map { JSONArray(it.days) }))
    }

    private fun plan(ctx: Context): JSONObject? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("plan", null)?.let { runCatching { JSONObject(it) }.getOrNull() }

    fun reschedule(ctx: Context) {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (Store.loaded.value) sp.edit().putString("plan", buildPlan().toString()).apply()
        val plan = plan(ctx) ?: return
        val am = ctx.getSystemService(AlarmManager::class.java)

        // Önceki alarmları iptal et
        sp.getStringSet("codes", emptySet()).orEmpty().forEach { code ->
            am.cancel(alarmIntent(ctx, code.toInt(), Intent(ctx, AlarmReceiver::class.java)))
        }
        val codes = mutableSetOf<String>()
        val now = LocalDateTime.now()
        for (offset in 0..1) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val dow = date.dayOfWeek.value
            val habits = plan.getJSONArray("habits")
            for (i in 0 until habits.length()) {
                val h = habits.getJSONObject(i)
                val days = h.getJSONArray("days").let { a -> (0 until a.length()).map { a.getInt(it) } }
                if (dow !in days || date.isBefore(LocalDate.parse(h.getString("createdAt")))) continue
                val t = parseTime(h.getString("time")) ?: continue
                set(ctx, am, codes, date.atTime(t), "habit", h.getString("id"), date)
            }
            val blocks = plan.getJSONArray("blocks")
            for (i in 0 until blocks.length()) {
                val b = blocks.getJSONObject(i)
                if (b.getInt("day") != dow) continue
                val t = parseTime(b.getString("time")) ?: continue
                set(ctx, am, codes, date.atTime(t), "deep", "$i", date)
            }
            parseTime(plan.optString("evening", "21:00"))?.let { set(ctx, am, codes, date.atTime(it), "evening", "e", date) }
            if (plan.optBoolean("sunday", true) && dow == 7) set(ctx, am, codes, date.atTime(19, 0), "sunday", "s", date)
        }
        sp.edit().putStringSet("codes", codes).apply()
    }

    private fun set(ctx: Context, am: AlarmManager, codes: MutableSet<String>, at: LocalDateTime, type: String, id: String, date: LocalDate) {
        if (!at.isAfter(LocalDateTime.now())) return
        val code = "$type|$id|$date".hashCode()
        val intent = Intent(ctx, AlarmReceiver::class.java).putExtra("type", type).putExtra("id", id).putExtra("date", date.toString())
        val pi = alarmIntent(ctx, code, intent)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
        codes += code.toString()
    }

    private fun alarmIntent(ctx: Context, code: Int, intent: Intent) =
        PendingIntent.getBroadcast(ctx, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Seans bitişi: uygulama arka plandayken de süre dolunca haber verir. Bütçeden yemez (kullanıcı başlattı). */
    fun scheduleSessionEnd(ctx: Context, endMillis: Long, title: String) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val intent = Intent(ctx, AlarmReceiver::class.java).putExtra("type", "deep_end").putExtra("id", title)
        val pi = alarmIntent(ctx, 777_001, intent)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pi)
        }
    }

    fun cancelSessionEnd(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmIntent(ctx, 777_001, Intent(ctx, AlarmReceiver::class.java)))
    }

    // ---------- Bütçe ----------
    private fun inQuiet(plan: JSONObject, t: LocalTime): Boolean {
        val s = parseTime(plan.optString("quietStart", "22:00")) ?: return false
        val e = parseTime(plan.optString("quietEnd", "07:00")) ?: return false
        return if (s.isAfter(e)) !t.isBefore(s) || t.isBefore(e) else !t.isBefore(s) && t.isBefore(e)
    }

    /** Bütçe kontrolü. Akşam kapanışı, Pazar planı ve derin bloklar için gün içinde yer ayrılır. */
    private fun allow(ctx: Context, plan: JSONObject, type: String, date: LocalDate): Boolean {
        val now = LocalTime.now()
        if (inQuiet(plan, now)) return false
        val evening = parseTime(plan.optString("evening", "21:00"))
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (type != "evening" && sp.getString("eveningDone", "") == date.toString()) return false // kapanıştan sonra sessizlik
        if (type != "evening" && evening != null && now.isAfter(evening.plusMinutes(1)) && type != "deep") return false
        val budget = plan.optInt("budget", 4)
        val used = if (sp.getString("countDate", "") == date.toString()) sp.getInt("count", 0) else 0
        val reserved = if (type == "habit") {
            val dow = date.dayOfWeek.value
            val blocks = plan.getJSONArray("blocks")
            var r = 1 // akşam kapanışı
            if (dow == 7 && plan.optBoolean("sunday", true)) r++
            for (i in 0 until blocks.length()) {
                val b = blocks.getJSONObject(i)
                if (b.getInt("day") == dow && (parseTime(b.getString("time")) ?: LocalTime.MIN).isAfter(now)) r++
            }
            r
        } else 0
        if (used + reserved >= budget && type == "habit") return false
        if (used >= budget) return false
        sp.edit().putString("countDate", date.toString()).putInt("count", used + 1).apply()
        return true
    }

    // ---------- Gösterim ----------
    fun openIntent(ctx: Context, open: String, extra: String = "", code: Int = open.hashCode()): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).putExtra("open", open).putExtra("extra", extra)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun post(ctx: Context, id: Int, n: NotificationCompat.Builder) {
        if (!canPost(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(id, n.build())
        } catch (_: SecurityException) {
        }
    }

    private fun base(ctx: Context, ch: String, title: String, text: String) =
        NotificationCompat.Builder(ctx, ch)
            .setSmallIcon(R.drawable.ic_stat_gelisio)
            .setColor(0xFF7CF5D2.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)

    fun handleAlarm(ctx: Context, intent: Intent, done: () -> Unit) {
        ensureChannels(ctx)
        val type = intent.getStringExtra("type") ?: return done()
        val id = intent.getStringExtra("id").orEmpty()
        val date = intent.getStringExtra("date")?.let { LocalDate.parse(it) } ?: LocalDate.now()
        if (type == "deep_end") {
            post(ctx, 9001, base(ctx, CH_FOCUS, "Derin seans bitti", "Süre doldu. Uygulamaya dön ve seansı skor cetveline yaz.")
                .setContentIntent(openIntent(ctx, "deep", "")))
            return done()
        }
        val plan = plan(ctx) ?: return done()
        when (type) {
            "habit" -> habitAlarm(ctx, plan, id, date, done)
            "deep" -> {
                val b = plan.getJSONArray("blocks").optJSONObject(id.toIntOrNull() ?: -1)
                if (b != null && allow(ctx, plan, type, date)) {
                    val t = b.optString("title")
                    post(ctx, 8000 + id.hashCode() % 500, base(ctx, CH_FOCUS, "Derin blok başlıyor · ${b.getInt("minutes")} dk",
                        (if (t.isNotBlank()) "$t. " else "") + "Önce ritüel: yer, süre, kural, destek.")
                        .setContentIntent(openIntent(ctx, "deep", b.optString("projectId"))))
                }
                reschedule(ctx)
                done()
            }
            "evening" -> {
                if (allow(ctx, plan, type, date)) {
                    val tomorrow = date.plusDays(1).dayOfWeek.value
                    val blocks = plan.getJSONArray("blocks")
                    val first = (0 until blocks.length()).map { blocks.getJSONObject(it) }.filter { it.getInt("day") == tomorrow }.minByOrNull { it.getString("time") }
                    val all = plan.getJSONArray("allHabitDays")
                    val count = (0 until all.length()).count { i -> val a = all.getJSONArray(i); (0 until a.length()).any { a.getInt(it) == tomorrow } }
                    val text = (if (first != null) "Yarının ilk derin bloğu ${first.getString("time")}. " else "Yarın için derin blok yok. ") +
                        "$count alışkanlık planlı. Planı kontrol et, sonra zihnini kapat."
                    post(ctx, 7001, base(ctx, CH_RITUALS, "Akşam kapanışı", text).setContentIntent(openIntent(ctx, "week")))
                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("eveningDone", date.toString()).apply()
                }
                reschedule(ctx)
                done()
            }
            "sunday" -> {
                if (allow(ctx, plan, type, date)) {
                    post(ctx, 7002, base(ctx, CH_RITUALS, "Pazar planlaması", "Haftayı gözden geçir, sonra her rol için 2–3 hedef seç.")
                        .setContentIntent(openIntent(ctx, "plan")))
                }
                reschedule(ctx)
                done()
            }
            else -> done()
        }
    }

    /** Niyet hatırlatması; dün kaçırıldıysa aynı saatte kurtarma metniyle gelir. Bugün yapılmışsa sessiz kalır. */
    private fun habitAlarm(ctx: Context, plan: JSONObject, id: String, date: LocalDate, done: () -> Unit) {
        val arr = plan.getJSONArray("habits")
        val h = (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { it.getString("id") == id } ?: return done()
        val uid = if (FirebaseApp.getApps(ctx).isNotEmpty()) FirebaseAuth.getInstance().currentUser?.uid else null
        val finish: (Boolean, Boolean) -> Unit = { doneToday, missedYesterday ->
            if (!doneToday && allow(ctx, plan, "habit", date)) {
                val title = if (missedYesterday) "Bugün 2 dakikalık sürüm yeter" else h.getString("name")
                val text = if (missedYesterday) "${h.getString("name")}: ${h.getString("min")}" else
                    listOf(h.getString("full"), h.optString("place")).filter { it.isNotBlank() }.joinToString(" · ")
                val nid = 1000 + (id.hashCode() and 0x3FFF)
                val n = base(ctx, CH_HABITS, title, text)
                    .setContentIntent(openIntent(ctx, "today"))
                    .addAction(0, "Tam", actionIntent(ctx, id, date, "full", nid))
                    .addAction(0, "Minimum", actionIntent(ctx, id, date, "min", nid))
                post(ctx, nid, n)
            }
            reschedule(ctx)
            done()
        }
        if (uid == null) return finish(false, false)
        val logs = FirebaseFirestore.getInstance().collection("users").document(uid).collection("habitLogs")
        val yesterday = date.minusDays(1)
        val days = h.getJSONArray("days").let { a -> (0 until a.length()).map { a.getInt(it) } }
        val yScheduled = yesterday.dayOfWeek.value in days && !yesterday.isBefore(LocalDate.parse(h.getString("createdAt")))
        logs.document("${id}_$date").get(Source.CACHE).addOnCompleteListener { t ->
            val st = t.result?.getString("status")
            val doneToday = st == "full" || st == "min"
            if (!yScheduled) return@addOnCompleteListener finish(doneToday, false)
            logs.document("${id}_$yesterday").get(Source.CACHE).addOnCompleteListener { y ->
                val ys = y.result?.getString("status")
                finish(doneToday, !(ys == "full" || ys == "min"))
            }
        }
    }

    private fun actionIntent(ctx: Context, habitId: String, date: LocalDate, status: String, nid: Int): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java).putExtra("habitId", habitId).putExtra("date", date.toString())
            .putExtra("status", status).putExtra("nid", nid)
        return PendingIntent.getBroadcast(ctx, "$habitId$status$date".hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun markFromNotification(ctx: Context, intent: Intent, done: () -> Unit) {
        val habitId = intent.getStringExtra("habitId") ?: return done()
        val date = intent.getStringExtra("date") ?: return done()
        val status = intent.getStringExtra("status") ?: return done()
        NotificationManagerCompat.from(ctx).cancel(intent.getIntExtra("nid", 0))
        val uid = if (FirebaseApp.getApps(ctx).isNotEmpty()) FirebaseAuth.getInstance().currentUser?.uid else null
        if (uid == null) return done()
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("habitLogs").document("${habitId}_$date")
            .set(mapOf("habitId" to habitId, "date" to date, "status" to status, "at" to FieldValue.serverTimestamp()))
            .addOnCompleteListener { done() }
        // Çevrimdışıysa yazma kuyruğa alınır; dinleyicinin tamamlanmasını beklemeden süreyi sınırlayalım.
        handler.postDelayed({ done() }, 8000)
    }

    @Suppress("unused")
    private fun scheduled(h: com.omeronur.gelisio.data.Habit, d: LocalDate) = h.isScheduled(d)
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        var finished = false
        Notifier.handleAlarm(context.applicationContext, intent) {
            if (!finished) {
                finished = true
                result.finish()
            }
        }
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        var finished = false
        Notifier.markFromNotification(context.applicationContext, intent) {
            if (!finished) {
                finished = true
                result.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Notifier.reschedule(context.applicationContext)
        }
    }
}
