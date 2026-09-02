package com.must.timetable.features.timetable.data

import android.content.Context
import com.must.timetable.core.database.AppDatabase
import com.must.timetable.features.timetable.domain.LectureNote
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object DataLoader {

    private const val PREFS = "must_prefs"
    private const val KEY_DATA_LOADED = "data_loaded"
    private const val TIMETABLE_FILE = "timetable_export.json"
    private const val NOTES_FILE = "notes_users_export.json"

    suspend fun ensureDataLoaded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DATA_LOADED, false)) {
            return@withContext
        }

        val db = AppDatabase.get(context)
        val dao = db.timetableDao()

        loadTimetableFromAssets(context, dao)
        loadNotesFromAssets(context, dao)

        prefs.edit().putBoolean(KEY_DATA_LOADED, true).apply()
    }

    private fun loadTimetableFromAssets(context: Context, dao: TimetableDao) {
        val json = context.assets.open(TIMETABLE_FILE).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val now = System.currentTimeMillis()

        val entries = ArrayList<TimetableEntry>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val sharedWith = obj.optJSONArray("shared_with") ?: JSONArray()
            val sharedRaw = buildString {
                for (j in 0 until sharedWith.length()) {
                    if (j > 0) append(",")
                    append(sharedWith.getString(j))
                }
            }

            entries.add(
                TimetableEntry(
                    id = 0,
                    programmeGroup = obj.optString("program_group", ""),
                    courseCode = obj.optString("course_code", ""),
                    dayOfWeek = obj.optString("day", ""),
                    startTime = normalizeTime(obj.optString("start_time", "")),
                    endTime = normalizeTime(obj.optString("end_time", "")),
                    courseTitle = obj.optString("course_title", ""),
                    sessionType = obj.optString("session_type", null),
                    lecturer = obj.optString("lecturer", ""),
                    room = obj.optString("room", ""),
                    timeSlot = obj.optString("time_slot", ""),
                    sharedWithRaw = sharedRaw,
                    draftVersion = "FINAL",
                    lastSyncedAt = now
                )
            )
        }

        kotlinx.coroutines.runBlocking {
            dao.upsertEntries(entries)
        }
    }

    private fun loadNotesFromAssets(context: Context, dao: TimetableDao) {
        val json = context.assets.open(NOTES_FILE).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val notesArray = root.optJSONArray("LectureNote") ?: return

        val notes = ArrayList<LectureNote>(notesArray.length())
        for (i in 0 until notesArray.length()) {
            val obj = notesArray.getJSONObject(i)
            notes.add(
                LectureNote(
                    id = 0,
                    naturalKey = obj.optString("natural_key", ""),
                    content = obj.optString("content", ""),
                    updatedAt = System.currentTimeMillis(),
                    alarmMinutes = if (obj.has("alarm_minutes") && !obj.isNull("alarm_minutes"))
                        obj.getInt("alarm_minutes") else null
                )
            )
        }

        kotlinx.coroutines.runBlocking {
            notes.forEach { dao.upsertNote(it) }
        }
    }

    private fun normalizeTime(time: String): String {
        if (time.isBlank()) return time
        return if (time.matches(Regex("^\\d:\\d{2}$"))) "0$time" else time
    }
}
