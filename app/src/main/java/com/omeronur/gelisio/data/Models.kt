package com.omeronur.gelisio.data

import java.time.LocalDate

/*
 * Firestore modeli (tümü users/{uid} altında). Eşleme elle yapılır: Firestore'un
 * yansımalı eşleyicisi val alanlı data class'ları dolduramaz.
 */

enum class WorkPhilosophy(val key: String, val label: String, val desc: String) {
    MONASTIC("monastic", "Keşiş", "Sığ işi neredeyse tamamen dışarıda bırak; her gün uzun derin blok."),
    BIMODAL("bimodal", "Çift modlu", "Haftanın belirli günleri tamamen derin, kalan günler açık."),
    RHYTHMIC("rhythmic", "Ritmik", "Her gün aynı saatte sabit derin blok; alışkanlık gibi."),
    JOURNALISTIC("journalistic", "Gazeteci", "Boşluk buldukça derine geç; deneyim ister."),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class Dimension(val key: String, val label: String) {
    PHYSICAL("physical", "Fiziksel"),
    MENTAL("mental", "Zihinsel"),
    SPIRITUAL("spiritual", "Ruhsal"),
    SOCIAL("social", "Sosyal"),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: PHYSICAL
    }
}

/** Derin çalışma bloğu şablonu: haftanın günü (1=Pzt) + saat + süre. */
data class DeepBlock(val day: Int, val time: String, val minutes: Int, val projectId: String = "")

data class Profile(
    val mission: String = "",
    val roles: List<String> = emptyList(),
    val values: List<String> = emptyList(),
    val workPhilosophy: WorkPhilosophy? = null,
    val deepBlocks: List<DeepBlock> = emptyList(),
    val notifBudget: Int = 4,
    val quietStart: String = "22:00",
    val quietEnd: String = "07:00",
    val eveningTime: String = "21:00",
    val sundayPlanning: Boolean = true,
    val onboarded: Boolean = false,
)

data class Identity(val id: String = "", val statement: String = "")

enum class LogStatus(val key: String) {
    FULL("full"), MIN("min"), SKIP("skip");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** İşaret: ya niyet (saat + yer) ya istifleme (şundan sonra). */
data class Cue(
    val type: String = "time", // time | stack
    val time: String = "",
    val place: String = "",
    val afterHabitId: String = "",
)

data class Habit(
    val id: String = "",
    val name: String = "",
    val identityId: String = "",
    val fullVersion: String = "",
    val twoMinVersion: String = "",
    val cue: Cue = Cue(),
    val reward: String = "",
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val difficulty: Int = 1,
    val projectId: String = "",
    val archived: Boolean = false,
    val createdAt: LocalDate = LocalDate.now(),
    val goldilocksAt: LocalDate? = null,
)

data class HabitLog(val habitId: String, val date: LocalDate, val status: LogStatus)

data class LeadMeasure(val unit: String = "derin saat", val weeklyTarget: Double = 10.0)

data class ValueCheck(val reality: Boolean = true, val constructive: Boolean = true, val controllable: Boolean = true) {
    val passes get() = reality && constructive && controllable
}

data class Goal(
    val id: String = "",
    val title: String = "",
    val quarter: String = "",
    val role: String = "",
    val leadMeasure: LeadMeasure = LeadMeasure(),
    val valueCheck: ValueCheck = ValueCheck(),
)

data class Milestone(val title: String, val done: Boolean = false)

data class Project(
    val id: String = "",
    val kind: String = "project", // project | hobby
    val title: String = "",
    val goalId: String = "",
    val role: String = "",
    val milestones: List<Milestone> = emptyList(),
    val leadMeasure: LeadMeasure = LeadMeasure(),
    val dueDate: LocalDate? = null,
    val identityId: String = "",
    val dimension: Dimension = Dimension.MENTAL,
    val skillLevel: Int = 1,
    val weeklyPracticeMin: Int = 120,
    val outputType: String = "",
    val archived: Boolean = false,
) {
    val isHobby get() = kind == "hobby"
}

data class Session(
    val id: String = "",
    val projectId: String = "",
    val startMillis: Long = 0,
    val minutes: Int = 0,
    val ritual: List<String> = emptyList(),
    val kind: String = "deep", // deep | practice
) {
    val date: LocalDate
        get() = java.time.Instant.ofEpochMilli(startMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
}

/** Haftalık plan: rol başına hedefler ve 4 kare. */
data class RoleGoal(
    val id: String,
    val role: String,
    val title: String,
    val quadrant: Int = 2,
    val day: Int = 0, // 0 = gün atanmadı, 1..7
    val done: Boolean = false,
)

data class Review(val worked: String = "", val didnt: String = "", val change: String = "")

data class Week(val key: String, val goals: List<RoleGoal> = emptyList(), val review: Review = Review())

/** Alışkanlık Skor Kartı satırı: + iyi, − kötü, = nötr. */
data class ScoreItem(val id: String = "", val text: String = "", val mark: String = "=", val order: Int = 0)

data class Output(val id: String = "", val projectId: String = "", val note: String = "", val date: LocalDate = LocalDate.now())

data class CustomQuote(val id: String = "", val context: String = "", val text: String = "")

/** Günlük belge: Günlük Özel Zafer yenilenme bloğu. */
data class DayDoc(val date: LocalDate, val renewal: Set<Dimension> = emptySet())
