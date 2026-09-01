package com.must.timetable

import android.app.Application
import com.must.timetable.core.database.AppDatabase

class TimetableApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
}
