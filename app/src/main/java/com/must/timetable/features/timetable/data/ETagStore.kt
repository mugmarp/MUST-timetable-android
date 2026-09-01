package com.must.timetable.features.timetable.data

import android.content.SharedPreferences

interface ETagStore {
    fun getEtag(programme: String): String?
    fun saveEtag(programme: String, etag: String)
    fun getProgramme(): String
    fun saveProgramme(programme: String)
    var welcomed: Boolean
}

class SharedPrefsEtagStore(private val prefs: SharedPreferences) : ETagStore {
    override fun getEtag(programme: String) = prefs.getString("etag_$programme", null)
    override fun saveEtag(programme: String, etag: String) {
        prefs.edit().putString("etag_$programme", etag).apply()
    }
    override fun getProgramme() = prefs.getString("programme", "MBR I") ?: "MBR I"
    override fun saveProgramme(programme: String) = prefs.edit().putString("programme", programme).apply()
    override var welcomed: Boolean
        get() = prefs.getBoolean("welcomed", false)
        set(value) = prefs.edit().putBoolean("welcomed", value).apply()
}
