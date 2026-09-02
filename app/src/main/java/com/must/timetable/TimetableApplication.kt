package com.must.timetable

import android.app.Application
import com.must.timetable.core.database.AppDatabase
import com.must.timetable.features.timetable.data.DataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimetableApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            DataLoader.ensureDataLoaded(this@TimetableApplication)
        }
    }
}
