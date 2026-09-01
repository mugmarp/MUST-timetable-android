package com.must.timetable

import android.content.SharedPreferences
import com.must.timetable.core.network.ApiService
import com.must.timetable.core.network.ScheduleResponse
import com.must.timetable.core.network.ScheduleMetadata
import com.must.timetable.features.timetable.data.ETagStore
import com.must.timetable.features.timetable.data.SharedPrefsEtagStore
import retrofit2.Response

class StubApiService : ApiService {
    override suspend fun fetchSchedule(
        programmeCode: String,
        etag: String?
    ): Response<ScheduleResponse> {
        return Response.success(
            ScheduleResponse(
                metadata = ScheduleMetadata(
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

class StubEtagStore : ETagStore {
    private val map = mutableMapOf<String, String>()
    override fun getEtag(programme: String): String? = map[programme]
    override fun saveEtag(programme: String, etag: String) {
        map[programme] = etag
    }
}
