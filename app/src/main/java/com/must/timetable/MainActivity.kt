package com.must.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.must.timetable.core.database.AppDatabase
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.ui.TimetableScreen
import com.must.timetable.features.timetable.ui.TimetableViewModel
import com.must.timetable.ui.theme.MUSTTimetableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "must_timetable.db"
        ).build()

        val dao = db.timetableDao()
        val repository = TimetableRepository(
            api = StubApiService(),
            dao = dao,
            etagStore = StubEtagStore()
        )
        val viewModel = ViewModelProvider(
            this,
            TimetableViewModelFactory(repository)
        )[TimetableViewModel::class.java]

        setContent {
            MUSTTimetableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimetableScreen(
                        viewModel = viewModel,
                        onLectureClick = { /* TODO: navigate to detail */ }
                    )
                }
            }
        }
    }
}
