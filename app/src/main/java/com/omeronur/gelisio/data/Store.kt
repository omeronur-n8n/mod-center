package com.omeronur.gelisio.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Tek veri kaynağı: users/{uid} altındaki koleksiyonları gerçek zamanlı dinler.
 * Telefonda işaretlenen her şey diğer cihazda anında görünür; Firestore çevrimdışı önbelleği açık.
 */
object Store {
    private val regs = mutableListOf<ListenerRegistration>()
    private var uid: String? = null
    var onChange: (() -> Unit)? = null

    val loaded = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val profile = MutableStateFlow(Profile())
    val identities = MutableStateFlow<List<Identity>>(emptyList())
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val logs = MutableStateFlow<List<HabitLog>>(emptyList())
    val goals = MutableStateFlow<List<Goal>>(emptyList())
    val projects = MutableStateFlow<List<Project>>(emptyList())
    val sessions = MutableStateFlow<List<Session>>(emptyList())
    val weeks = MutableStateFlow<Map<String, Week>>(emptyMap())
    val scorecard = MutableStateFlow<List<ScoreItem>>(emptyList())
    val outputs = MutableStateFlow<List<Output>>(emptyList())
    val quotes = MutableStateFlow<List<CustomQuote>>(emptyList())
    val days = MutableStateFlow<Map<LocalDate, DayDoc>>(emptyMap())

    val logIndex: StateFlow<LogIndex> get() = _logIndex
    private val _logIndex = MutableStateFlow<LogIndex>(emptyMap())

    private fun user() = Backend.db.collection("users").document(uid ?: error("oturum yok"))
    private fun col(name: String): CollectionReference = user().collection(name)

    fun start(uid: String) {
        if (this.uid == uid) return
        stop()
        this.uid = uid
        regs += user().addSnapshotListener { s, e ->
            if (handle(e)) return@addSnapshotListener
            profile.value = s?.toProfile() ?: Profile()
            loaded.value = true
            onChange?.invoke()
        }
        listen("identities") { identities.value = it.map { d -> Identity(d.id, d.str("statement")) } }
        listen("habits") { habits.value = it.map { d -> d.toHabit() }.sortedBy { h -> h.cue.time.ifBlank { "99" } } }
        listen("habitLogs") {
            logs.value = it.mapNotNull { d ->
                val st = LogStatus.of(d.getString("status")) ?: return@mapNotNull null
                val date = d.date("date") ?: return@mapNotNull null
                HabitLog(d.str("habitId"), date, st)
            }
            _logIndex.value = logs.value.index()
        }
        listen("goals") { goals.value = it.map { d -> d.toGoal() } }
        listen("projects") { projects.value = it.map { d -> d.toProject() } }
        listen("sessions") { sessions.value = it.map { d -> d.toSession() }.sortedByDescending { s -> s.startMillis } }
        listen("weeks") { weeks.value = it.associate { d -> d.id to d.toWeek() } }
        listen("scorecard") {
            scorecard.value = it.map { d -> ScoreItem(d.id, d.str("text"), d.str("mark", "="), d.int("order")) }.sortedBy { s -> s.order }
        }
        listen("outputs") { outputs.value = it.map { d -> Output(d.id, d.str("projectId"), d.str("note"), d.date("date") ?: LocalDate.now()) }.sortedByDescending { o -> o.date } }
        listen("quotes") { quotes.value = it.map { d -> CustomQuote(d.id, d.str("context"), d.str("text")) } }
        listen("days") {
            days.value = it.mapNotNull { d ->
                val date = runCatching { LocalDate.parse(d.id) }.getOrNull() ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val r = (d.get("renewal") as? List<String>).orEmpty().map { k -> Dimension.of(k) }.toSet()
                date to DayDoc(date, r)
            }.toMap()
        }
    }

    fun stop() {
        regs.forEach { it.remove() }
        regs.clear()
        uid = null
        loaded.value = false
    }

    private fun handle(e: FirebaseFirestoreException?): Boolean {
        if (e == null) return false
        error.value = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "Firestore kuralları erişimi engelliyor. Ben → Hakkında'daki kuralları Firebase konsoluna yapıştır."
        } else {
            e.message
        }
        return true
    }

    private fun listen(name: String, onDocs: (List<DocumentSnapshot>) -> Unit) {
        regs += col(name).addSnapshotListener { s, e ->
            if (handle(e)) return@addSnapshotListener
            onDocs(s?.documents.orEmpty())
            onChange?.invoke()
        }
    }

    fun newId(collection: String): String = col(collection).document().id

    // ---------- Profil ----------
    fun saveProfile(p: Profile) {
        user().set(
            mapOf(
                "mission" to p.mission,
                "roles" to p.roles.take(7),
                "values" to p.values,
                "workPhilosophy" to p.workPhilosophy?.key,
                "deepBlocks" to p.deepBlocks.map { mapOf("day" to it.day, "time" to it.time, "minutes" to it.minutes, "projectId" to it.projectId) },
                "notifBudget" to p.notifBudget,
                "quietHours" to mapOf("start" to p.quietStart, "end" to p.quietEnd),
                "eveningTime" to p.eveningTime,
                "sundayPlanning" to p.sundayPlanning,
                "onboarded" to p.onboarded,
                "schemaVersion" to 2,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
    }

    fun touchUser(displayName: String?, email: String?) {
        user().set(
            mapOf("displayName" to displayName, "email" to email, "lastSeenAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        )
    }

    // ---------- Kimlikler ----------
    fun saveIdentity(i: Identity): String {
        val id = i.id.ifBlank { newId("identities") }
        col("identities").document(id).set(mapOf("statement" to i.statement.trim()))
        return id
    }

    fun deleteIdentity(id: String) = col("identities").document(id).delete()

    // ---------- Alışkanlıklar ----------
    fun saveHabit(h: Habit): String {
        val id = h.id.ifBlank { newId("habits") }
        col("habits").document(id).set(
            mapOf(
                "name" to h.name.trim(),
                "identityId" to h.identityId,
                "type" to "build",
                "fullVersion" to h.fullVersion.trim(),
                "twoMinVersion" to h.twoMinVersion.trim(),
                "cue" to mapOf("type" to h.cue.type, "time" to h.cue.time, "place" to h.cue.place, "afterHabitId" to h.cue.afterHabitId),
                "reward" to h.reward.trim(),
                "days" to h.days.sorted(),
                "difficulty" to h.difficulty,
                "projectId" to h.projectId,
                "archived" to h.archived,
                "createdAt" to h.createdAt.key(),
                "goldilocksAt" to h.goldilocksAt?.key(),
            ),
        )
        return id
    }

    fun deleteHabit(id: String) {
        col("habits").document(id).delete()
        logs.value.filter { it.habitId == id }.forEach { col("habitLogs").document(logId(id, it.date)).delete() }
    }

    private fun logId(habitId: String, date: LocalDate) = "${habitId}_${date.key()}"

    /** Belge kimliği habitId_tarih: aynı gün için çift kayıt olmaz. null = kaydı sil. */
    fun setLog(habitId: String, date: LocalDate, status: LogStatus?) {
        val ref = col("habitLogs").document(logId(habitId, date))
        if (status == null) {
            ref.delete()
        } else {
            ref.set(mapOf("habitId" to habitId, "date" to date.key(), "status" to status.key, "at" to FieldValue.serverTimestamp()))
        }
    }

    // ---------- Hedefler, projeler, seanslar ----------
    fun saveGoal(g: Goal): String {
        val id = g.id.ifBlank { newId("goals") }
        col("goals").document(id).set(
            mapOf(
                "title" to g.title.trim(),
                "quarter" to g.quarter,
                "role" to g.role,
                "leadMeasure" to mapOf("unit" to g.leadMeasure.unit, "weeklyTarget" to g.leadMeasure.weeklyTarget),
                "valueCheck" to mapOf("reality" to g.valueCheck.reality, "constructive" to g.valueCheck.constructive, "controllable" to g.valueCheck.controllable),
            ),
        )
        return id
    }

    fun deleteGoal(id: String) = col("goals").document(id).delete()

    fun saveProject(p: Project): String {
        val id = p.id.ifBlank { newId("projects") }
        col("projects").document(id).set(
            mapOf(
                "kind" to p.kind,
                "title" to p.title.trim(),
                "goalId" to p.goalId,
                "role" to p.role,
                "milestones" to p.milestones.map { mapOf("title" to it.title, "done" to it.done) },
                "leadMeasure" to mapOf("unit" to p.leadMeasure.unit, "weeklyTarget" to p.leadMeasure.weeklyTarget),
                "dueDate" to p.dueDate?.key(),
                "identityId" to p.identityId,
                "dimension" to p.dimension.key,
                "skillLevel" to p.skillLevel,
                "weeklyPracticeMin" to p.weeklyPracticeMin,
                "outputType" to p.outputType,
                "archived" to p.archived,
            ),
        )
        return id
    }

    fun deleteProject(id: String) = col("projects").document(id).delete()

    fun addSession(s: Session) {
        col("sessions").document(s.id.ifBlank { newId("sessions") }).set(
            mapOf(
                "projectId" to s.projectId,
                "start" to s.startMillis,
                "end" to s.startMillis + s.minutes * 60_000L,
                "minutes" to s.minutes,
                "ritualChecks" to s.ritual,
                "kind" to s.kind,
            ),
        )
    }

    fun deleteSession(id: String) = col("sessions").document(id).delete()

    // ---------- Hafta ----------
    fun saveWeek(w: Week) {
        col("weeks").document(w.key).set(
            mapOf(
                "roleGoals" to w.goals.map {
                    mapOf("id" to it.id, "role" to it.role, "title" to it.title, "quadrant" to it.quadrant, "day" to it.day, "done" to it.done)
                },
                "quadrant2Pct" to q2Pct(w),
                "review" to mapOf("worked" to w.review.worked, "didnt" to w.review.didnt, "change" to w.review.change),
            ),
        )
    }

    // ---------- Skor kartı, üretimler, cümleler, gün ----------
    fun saveScoreItem(i: ScoreItem) {
        col("scorecard").document(i.id.ifBlank { newId("scorecard") })
            .set(mapOf("text" to i.text.trim(), "mark" to i.mark, "order" to i.order))
    }

    fun deleteScoreItem(id: String) = col("scorecard").document(id).delete()

    fun addOutput(o: Output) {
        col("outputs").document(newId("outputs")).set(mapOf("projectId" to o.projectId, "note" to o.note.trim(), "date" to o.date.key(), "photoUrl" to null))
    }

    fun deleteOutput(id: String) = col("outputs").document(id).delete()

    fun saveQuote(q: CustomQuote) {
        col("quotes").document(q.id.ifBlank { newId("quotes") }).set(mapOf("context" to q.context, "text" to q.text.trim()))
    }

    fun deleteQuote(id: String) = col("quotes").document(id).delete()

    fun setRenewal(date: LocalDate, dims: Set<Dimension>) {
        col("days").document(date.key()).set(mapOf("renewal" to dims.map { it.key }), SetOptions.merge())
    }

    // ---------- Eşleme ----------
    private fun DocumentSnapshot.str(k: String, def: String = "") = getString(k) ?: def
    private fun DocumentSnapshot.int(k: String, def: Int = 0) = getLong(k)?.toInt() ?: def
    private fun DocumentSnapshot.date(k: String) = getString(k)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.map(k: String) = (get(k) as? Map<String, Any?>).orEmpty()

    private fun Map<String, Any?>.s(k: String, def: String = "") = (this[k] as? String) ?: def
    private fun Map<String, Any?>.n(k: String, def: Double = 0.0) = (this[k] as? Number)?.toDouble() ?: def
    private fun Map<String, Any?>.b(k: String, def: Boolean = true) = (this[k] as? Boolean) ?: def

    private fun lead(m: Map<String, Any?>) =
        if (m.isEmpty()) LeadMeasure() else LeadMeasure(m.s("unit", "derin saat"), m.n("weeklyTarget", 10.0))

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toProfile(): Profile {
        val quiet = map("quietHours")
        return Profile(
            mission = str("mission"),
            roles = (get("roles") as? List<String>).orEmpty(),
            values = (get("values") as? List<String>).orEmpty(),
            workPhilosophy = WorkPhilosophy.of(getString("workPhilosophy")),
            deepBlocks = (get("deepBlocks") as? List<Map<String, Any?>>).orEmpty().map {
                DeepBlock(it.n("day", 1.0).toInt(), it.s("time", "09:00"), it.n("minutes", 90.0).toInt(), it.s("projectId"))
            },
            notifBudget = int("notifBudget", 4),
            quietStart = quiet.s("start", "22:00"),
            quietEnd = quiet.s("end", "07:00"),
            eveningTime = str("eveningTime", "21:00"),
            sundayPlanning = getBoolean("sundayPlanning") ?: true,
            onboarded = getBoolean("onboarded") ?: false,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toHabit(): Habit {
        val c = map("cue")
        return Habit(
            id = id,
            name = str("name"),
            identityId = str("identityId"),
            fullVersion = str("fullVersion"),
            twoMinVersion = str("twoMinVersion"),
            cue = Cue(c.s("type", "time"), c.s("time"), c.s("place"), c.s("afterHabitId")),
            reward = str("reward"),
            days = (get("days") as? List<Number>)?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5, 6, 7),
            difficulty = int("difficulty", 1),
            projectId = str("projectId"),
            archived = getBoolean("archived") ?: false,
            createdAt = date("createdAt") ?: LocalDate.now(),
            goldilocksAt = date("goldilocksAt"),
        )
    }

    private fun DocumentSnapshot.toGoal(): Goal {
        val v = map("valueCheck")
        return Goal(
            id = id,
            title = str("title"),
            quarter = str("quarter"),
            role = str("role"),
            leadMeasure = lead(map("leadMeasure")),
            valueCheck = ValueCheck(v.b("reality"), v.b("constructive"), v.b("controllable")),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toProject() = Project(
        id = id,
        kind = str("kind", "project"),
        title = str("title"),
        goalId = str("goalId"),
        role = str("role"),
        milestones = (get("milestones") as? List<Map<String, Any?>>).orEmpty().map { Milestone(it.s("title"), it.b("done", false)) },
        leadMeasure = lead(map("leadMeasure")),
        dueDate = date("dueDate"),
        identityId = str("identityId"),
        dimension = Dimension.of(getString("dimension")),
        skillLevel = int("skillLevel", 1),
        weeklyPracticeMin = int("weeklyPracticeMin", 120),
        outputType = str("outputType"),
        archived = getBoolean("archived") ?: false,
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toSession() = Session(
        id = id,
        projectId = str("projectId"),
        startMillis = getLong("start") ?: 0L,
        minutes = int("minutes"),
        ritual = (get("ritualChecks") as? List<String>).orEmpty(),
        kind = str("kind", "deep"),
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toWeek(): Week {
        val r = map("review")
        return Week(
            key = id,
            goals = (get("roleGoals") as? List<Map<String, Any?>>).orEmpty().map {
                RoleGoal(it.s("id"), it.s("role"), it.s("title"), it.n("quadrant", 2.0).toInt(), it.n("day", 0.0).toInt(), it.b("done", false))
            },
            review = Review(r.s("worked"), r.s("didnt"), r.s("change")),
        )
    }
}
