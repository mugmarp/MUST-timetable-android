package com.must.timetable

import com.must.timetable.core.network.ApiService
import com.must.timetable.core.network.ScheduleResponse
import com.must.timetable.features.timetable.data.ETagStore
import retrofit2.Response

class StubApiService : ApiService {
    override suspend fun fetchSchedule(
        programmeCode: String,
        etag: String?
    ): Response<ScheduleResponse> {
        return Response.success(
            ScheduleResponse(
                metadata = com.must.timetable.core.network.ScheduleMetadata(
                    institution = "MUST",
                    academicYear = "2026/2027",
                    semester = "Semester I",
                    draftVersion = "FINAL",
                    generatedOn = "2026-08-15"
                ),
                lessons = emptyList()
            )
        )
    }
}

class StubEtagStore : ETagStore(object : android.content.SharedPreferences {
    override fun getAll(): MutableMap<String, *> = mutableMapOf()
    override fun getString(key: String?, defValue: String?): String? = defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = defValue
    override fun getLong(key: String?, defValue: Long): Long = defValue
    override fun getFloat(key: String?, defValue: Float): Float = defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
    override fun contains(key: String?): Boolean = false
    override fun edit(): android.content.SharedPreferences.Editor = StubEditor()
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
}) {
    private class StubEditor : android.content.SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor = this
        override fun putStringSet(key: String?, values: MutableSet<String>?): android.content.SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor = this
        override fun remove(key: String?): android.content.SharedPreferences.Editor = this
        override fun clear(): android.content.SharedPreferences.Editor = this
        override fun commit(): Boolean = true
        override fun apply() {}
    }
}
