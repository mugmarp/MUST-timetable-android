package com.must.timetable.features.timetable.data

import android.content.SharedPreferences

interface ETagStore {
    fun getEtag(programme: String): String?
    fun saveEtag(programme: String, etag: String)
}

class SharedPrefsEtagStore(private val prefs: SharedPreferences) : ETagStore {
    override fun getEtag(programme: String) = prefs.getString("etag_$programme", null)
    override fun saveEtag(programme: String, etag: String) {
        prefs.edit().putString("etag_$programme", etag).apply()
    }
}
