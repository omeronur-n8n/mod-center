package com.omeronur.gelisio.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

/** users/{uid}/days/{yyyy-MM-dd}. v0.1'de senkronu kanıtlamak için "hayati iş" alanı var. */
data class DayEntry(val vitalTask: String = "", val vitalDone: Boolean = false) {
    companion object {
        val EMPTY = DayEntry()
    }
}

object DayRepository {
    fun dayKey(date: LocalDate): String = date.toString() // ISO: yyyy-MM-dd

    private fun dayRef(uid: String, key: String) =
        Backend.db.collection("users").document(uid).collection("days").document(key)

    fun listen(uid: String, key: String, onChange: (DayEntry) -> Unit): ListenerRegistration =
        dayRef(uid, key).addSnapshotListener { snap, _ ->
            onChange(
                DayEntry(
                    vitalTask = snap?.getString("vitalTask").orEmpty(),
                    vitalDone = snap?.getBoolean("vitalDone") == true,
                ),
            )
        }

    fun setVitalTask(uid: String, key: String, text: String) {
        dayRef(uid, key).set(
            mapOf("vitalTask" to text.trim(), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        )
    }

    fun setVitalDone(uid: String, key: String, done: Boolean) {
        dayRef(uid, key).set(
            mapOf("vitalDone" to done, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        )
    }
}
