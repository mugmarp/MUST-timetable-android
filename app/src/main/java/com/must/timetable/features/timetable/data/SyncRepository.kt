package com.must.timetable.features.timetable.data

import com.google.firebase.firestore.DocumentChange.Type.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.must.timetable.core.database.AppDatabase
import com.must.timetable.features.timetable.domain.LectureNote
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listeners = mutableListOf<ListenerRegistration>()

    /** Starts snapshot listeners. Call this immediately upon successful Auth. */
    fun startAllSync(userId: String, scope: CoroutineScope) {
        stopAllSync()

        val userDoc = firestore.collection("users").document(userId)

        // 1. Sync global Timetable collection (Read-Only catalog)
        listeners.add(
            firestore.collection("timetable")
                .addSnapshotListener { snap, _ ->
                    snap?.documentChanges?.forEach { change ->
                        val entry = change.document.toTimetableEntry() ?: return@forEach
                        scope.launch(Dispatchers.IO) {
                            when (change.type) {
                                ADDED, MODIFIED -> db.timetableDao().upsertEntries(listOf(entry))
                                REMOVED -> db.timetableDao().delete(entry.naturalKey)
                            }
                        }
                    }
                }
        )

        // 2. Sync user-specific Lecture Notes (Bi-directional)
        listeners.add(
            userDoc.collection("notes")
                .addSnapshotListener { snap, _ ->
                    snap?.documentChanges?.forEach { change ->
                        val note = change.document.toLectureNote() ?: return@forEach
                        scope.launch(Dispatchers.IO) {
                            if (change.type != REMOVED) {
                                db.timetableDao().upsertNote(note)
                            } else {
                                db.timetableDao().deleteNote(note.naturalKey)
                            }
                        }
                    }
                }
        )
    }

    /** Clears active cloud listeners. Call this on user logout. */
    fun stopAllSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    /** Pushes local Room updates to the Firestore collection. */
    suspend fun pushNoteToCloud(note: LectureNote, userId: String) {
        val noteMap = mapOf(
            "content" to note.content,
            "alarm_minutes" to note.alarmMinutes,
            "updated_at" to FieldValue.serverTimestamp()
        )

        try {
            firestore.collection("users").document(userId)
                .collection("notes").document(note.naturalKey)
                .set(noteMap, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            // Firestore SDK automatically caches writes offline if network fails
        }
    }
}

/** Extension: Convert Firestore document to TimetableEntry */
private fun com.google.firebase.firestore.DocumentSnapshot.toTimetableEntry(): TimetableEntry? {
    return try {
        TimetableEntry(
            id = 0,
            programmeGroup = getString("program_group") ?: return null,
            courseCode = getString("course_code") ?: return null,
            dayOfWeek = getString("day") ?: return null,
            startTime = getString("start_time") ?: "",
            endTime = getString("end_time") ?: "",
            courseTitle = getString("course_title") ?: "",
            sessionType = getString("session_type"),
            lecturer = getString("lecturer") ?: "",
            room = getString("room") ?: "",
            timeSlot = getString("time_slot") ?: "",
            sharedWithRaw = (get("shared_with") as? List<*>)?.joinToString(",") { it.toString() } ?: "",
            draftVersion = getString("draft_version") ?: "FINAL",
            lastSyncedAt = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

/** Extension: Convert Firestore document to LectureNote */
private fun com.google.firebase.firestore.DocumentSnapshot.toLectureNote(): LectureNote? {
    return try {
        LectureNote(
            id = 0,
            naturalKey = getString("natural_key") ?: id,
            content = getString("content") ?: "",
            updatedAt = getTimestamp("updated_at")?.toDate()?.time ?: System.currentTimeMillis(),
            alarmMinutes = getLong("alarm_minutes")?.toInt()
        )
    } catch (e: Exception) {
        null
    }
}
