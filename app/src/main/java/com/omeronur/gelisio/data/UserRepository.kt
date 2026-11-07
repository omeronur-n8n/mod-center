package com.omeronur.gelisio.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

/** users/{uid}: tüm Gelisio verisi bu belgenin altında. */
object UserRepository {
    fun ensureProfile(user: FirebaseUser) {
        Backend.db.collection("users").document(user.uid).set(
            mapOf(
                "displayName" to user.displayName,
                "email" to user.email,
                "lastSeenAt" to FieldValue.serverTimestamp(),
                "schemaVersion" to 1,
            ),
            SetOptions.merge(),
        )
    }
}
