package com.must.timetable.features.timetable.ui

import androidx.compose.ui.graphics.Color
import com.must.timetable.features.timetable.domain.TimetableEntry

data class SessionStyle(val label: String, val badge: Color, val accent: Color, val text: Color)

fun sessionStyle(entry: TimetableEntry): SessionStyle = when (entry.sessionType?.uppercase()) {
    "PRACTICAL", "LAB" -> SessionStyle("Practical", Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF16A34A))
    "CLINICAL", "WARD" -> SessionStyle("Clinical", Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFD97706))
    "THEORY" -> SessionStyle("Theory", Color(0xFF2563EB), Color(0xFF3B82F6), Color(0xFF2563EB))
    else -> SessionStyle("Class", Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF64748B))
}
