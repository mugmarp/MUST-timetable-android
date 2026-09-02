package com.must.timetable

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.must.timetable.core.database.AppDatabase
import com.must.timetable.features.timetable.data.ETagStore
import com.must.timetable.features.timetable.data.SharedPrefsEtagStore
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.ui.TimetableViewModel

object AppGraph {

    fun repository(context: Context): TimetableRepository {
        val db = AppDatabase.get(context)
        val prefs = context.getSharedPreferences("must_prefs", Context.MODE_PRIVATE)
        return TimetableRepository(
            StubApiService(),
            db.timetableDao(),
            db.customEventDao(),
            db.assignmentDao(),
            SharedPrefsEtagStore(prefs)
        )
    }

    fun factory(context: Context) = viewModelFactory {
        initializer { TimetableViewModel(repository(context)) }
    }
}
