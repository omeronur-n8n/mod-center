package com.omeronur.gelisio.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/* Alışkanlık motorunun saf hesapları. Ekranlar ve bildirimler aynı fonksiyonları kullanır. */

typealias LogIndex = Map<String, Map<LocalDate, LogStatus>>

fun List<HabitLog>.index(): LogIndex =
    groupBy { it.habitId }.mapValues { (_, v) -> v.associate { it.date to it.status } }

fun Habit.isScheduled(date: LocalDate): Boolean =
    date.dayOfWeek.value in days && !date.isBefore(createdAt)

fun LogIndex.status(habitId: String, date: LocalDate): LogStatus? = this[habitId]?.get(date)

fun LogStatus?.isDone() = this == LogStatus.FULL || this == LogStatus.MIN

enum class Cell { FULL, MIN, MISS, MISS_TWICE, TODAY_OPEN, OFF, FUTURE }

/** Zincir hücresi: tek kaçırma sarı (MISS), art arda ikinci kaçırma kırmızı (MISS_TWICE). */
fun cell(habit: Habit, logs: LogIndex, date: LocalDate, today: LocalDate): Cell {
    if (date.isAfter(today)) return Cell.FUTURE
    if (!habit.isScheduled(date)) return Cell.OFF
    when (logs.status(habit.id, date)) {
        LogStatus.FULL -> return Cell.FULL
        LogStatus.MIN -> return Cell.MIN
        else -> Unit
    }
    if (date == today && logs.status(habit.id, date) == null) return Cell.TODAY_OPEN
    val prev = previousScheduled(habit, date) ?: return Cell.MISS
    val prevDone = logs.status(habit.id, prev).isDone()
    return if (prevDone) Cell.MISS else Cell.MISS_TWICE
}

fun previousScheduled(habit: Habit, date: LocalDate): LocalDate? {
    var d = date.minusDays(1)
    repeat(7) {
        if (d.isBefore(habit.createdAt)) return null
        if (habit.isScheduled(d)) return d
        d = d.minusDays(1)
    }
    return null
}

/** Ana metrik: son N günün tutarlılığı. Bugün, yapıldıysa sayılır; yapılmadıysa henüz kaçırma değildir. */
fun consistency(habit: Habit, logs: LogIndex, today: LocalDate, window: Int = 30): Double? {
    var scheduled = 0
    var done = 0
    for (i in 0 until window) {
        val d = today.minusDays(i.toLong())
        if (!habit.isScheduled(d)) continue
        val s = logs.status(habit.id, d)
        if (d == today && !s.isDone()) continue
        scheduled++
        if (s.isDone()) done++
    }
    return if (scheduled == 0) null else done.toDouble() / scheduled
}

/** Kırılgan seri (ikincil metrik): art arda yapılan planlı gün sayısı. */
fun streak(habit: Habit, logs: LogIndex, today: LocalDate): Int {
    var d = if (logs.status(habit.id, today).isDone()) today else today.minusDays(1)
    var n = 0
    while (!d.isBefore(habit.createdAt)) {
        if (habit.isScheduled(d)) {
            if (logs.status(habit.id, d).isDone()) n++ else break
        }
        d = d.minusDays(1)
    }
    return n
}

fun reps(habit: Habit, logs: LogIndex): Int = logs[habit.id]?.values?.count { it.isDone() } ?: 0

enum class Stage(val label: String, val range: String) {
    FORMING("Kuruluyor", "0–20 tekrar"),
    SETTLING("Pekişiyor", "21–60 tekrar"),
    AUTOMATIC("Otomatik", "60+ tekrar"),
}

fun stageOf(reps: Int) = when {
    reps <= 20 -> Stage.FORMING
    reps <= 60 -> Stage.SETTLING
    else -> Stage.AUTOMATIC
}

enum class Goldilocks { LEVEL_UP, SHRINK }

/** 14 günde bir: %95+ zorluğu artır, %60 altı 2 dakikalık sürüme in. */
fun goldilocks(habit: Habit, logs: LogIndex, today: LocalDate): Goldilocks? {
    if (ChronoUnit.DAYS.between(habit.createdAt, today) < 14) return null
    habit.goldilocksAt?.let { if (ChronoUnit.DAYS.between(it, today) < 14) return null }
    val c = consistency(habit, logs, today.minusDays(1), 14) ?: return null
    return when {
        c >= 0.95 -> Goldilocks.LEVEL_UP
        c < 0.60 -> Goldilocks.SHRINK
        else -> null
    }
}

fun missedYesterday(habit: Habit, logs: LogIndex, today: LocalDate): Boolean {
    val y = today.minusDays(1)
    return habit.isScheduled(y) && !logs.status(habit.id, y).isDone()
}

/** Kimlik oyu: o kimliğe bağlı alışkanlıkların tüm tam ve minimum tamamlamaları. */
fun votes(identityId: String, habits: List<Habit>, logs: LogIndex): Int =
    habits.filter { it.identityId == identityId }.sumOf { reps(it, logs) }

fun weekKey(date: LocalDate): String =
    "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun quarterKey(date: LocalDate): String = "${date.year}-Q${(date.monthValue - 1) / 3 + 1}"

fun sessionsInWeek(sessions: List<Session>, anyDayInWeek: LocalDate, kind: String? = "deep", projectId: String? = null): List<Session> {
    val start = weekStart(anyDayInWeek)
    val end = start.plusDays(7)
    return sessions.filter {
        val d = it.date
        !d.isBefore(start) && d.isBefore(end) &&
            (kind == null || it.kind == kind) &&
            (projectId == null || it.projectId == projectId)
    }
}

fun deepHours(sessions: List<Session>, anyDayInWeek: LocalDate, projectId: String? = null): Double =
    sessionsInWeek(sessions, anyDayInWeek, "deep", projectId).sumOf { it.minutes } / 60.0

fun q2Pct(week: Week?): Int? {
    val goals = week?.goals.orEmpty()
    if (goals.isEmpty()) return null
    return goals.count { it.quadrant == 2 } * 100 / goals.size
}

/** Hobi pratiği: bir gündeki pratik dakikası (seanslar) + bağlı alışkanlık tamamlaması. */
fun practiceMinutes(project: Project, date: LocalDate, sessions: List<Session>, habits: List<Habit>, logs: LogIndex): Int {
    val fromSessions = sessions.filter { it.projectId == project.id && it.date == date }.sumOf { it.minutes }
    val fromHabits = habits.filter { it.projectId == project.id && logs.status(it.id, date).isDone() }.size
    return fromSessions + fromHabits * 15
}

fun weeklyPractice(project: Project, anyDayInWeek: LocalDate, sessions: List<Session>, habits: List<Habit>, logs: LogIndex): Int {
    val start = weekStart(anyDayInWeek)
    return (0..6).sumOf { practiceMinutes(project, start.plusDays(it.toLong()), sessions, habits, logs) }
}

/** Goldilocks (hobi): son 4 tam hafta haftalık pratik hedefini tuttuysa seviye atla önerisi. */
fun hobbyLevelUpReady(project: Project, today: LocalDate, sessions: List<Session>, habits: List<Habit>, logs: LogIndex): Boolean {
    if (!project.isHobby || project.skillLevel >= 10) return false
    return (1..4).all { w ->
        weeklyPractice(project, today.minusWeeks(w.toLong()), sessions, habits, logs) >= project.weeklyPracticeMin
    }
}

fun milestonePct(project: Project): Double =
    if (project.milestones.isEmpty()) 0.0 else project.milestones.count { it.done }.toDouble() / project.milestones.size

fun parseTime(s: String): LocalTime? = runCatching { LocalTime.parse(s) }.getOrNull()

fun LocalDate.key(): String = toString()

val TR_DAYS = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
