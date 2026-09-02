package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph

@Composable
fun TimetableRoute() {
    val context = LocalContext.current
    val vm: TimetableViewModel = viewModel(factory = AppGraph.factory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MUST Timetable", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = { vm.syncSchedule() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text("Programme: ${state.selectedProgramme}")
            Text("Day: ${state.selectedDay}")
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(state.dayItems) { item ->
                    when (item) {
                        is DayItem.Lecture -> {
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(item.entry.courseCode, fontWeight = FontWeight.Bold)
                                    Text(item.entry.courseTitle)
                                    Text("${item.entry.startTime} - ${item.entry.endTime}")
                                    Text("Room: ${item.entry.room}")
                                }
                            }
                        }
                        is DayItem.Event -> {
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(item.event.title, fontWeight = FontWeight.Bold)
                                    Text("${item.event.startTime} - ${item.event.endTime}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
