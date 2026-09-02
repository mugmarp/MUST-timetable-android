package com.must.timetable.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.must.timetable.features.timetable.ui.TimetableRoute

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("timetable", "Timetable", Icons.Default.Today),
    Tab("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(bottomBar = {
        NavigationBar {
            TABS.forEach { tab ->
                NavigationBarItem(
                    selected = current?.hierarchy?.any { it.route == tab.route } == true,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }) { paddingValues ->
        NavHost(navController, startDestination = "timetable", modifier = Modifier.padding(paddingValues)) {
            composable("timetable") { TimetableRoute() }
            composable("settings") { PlaceholderRoute("Settings") }
        }
    }
}

@Composable
fun PlaceholderRoute(name: String) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(name, style = MaterialTheme.typography.headlineSmall)
        Text("Coming soon...")
    }
}
