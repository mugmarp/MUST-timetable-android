package com.must.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.ui.TimetableViewModel

class TimetableViewModelFactory(
    private val repository: TimetableRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimetableViewModel(repository) as T
    }
}
