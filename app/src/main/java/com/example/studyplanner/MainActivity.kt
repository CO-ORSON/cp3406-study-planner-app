package com.example.studyplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studyplanner.ui.NavRoute
import com.example.studyplanner.ui.screens.CalendarScreen
import com.example.studyplanner.ui.screens.FocusScreen
import com.example.studyplanner.ui.screens.InsightsScreen
import com.example.studyplanner.ui.screens.PlanScreen
import com.example.studyplanner.ui.theme.StudyPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyPlannerTheme {
                AppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val navController = rememberNavController()

    val items = listOf(
        NavRoute.Plan,
        NavRoute.Calendar,
        NavRoute.Focus,
        NavRoute.Insights
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Study Planner") }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            // Keep-alive navigation for top-level destinations
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (item) {
                                NavRoute.Plan -> Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = item.label)
                                NavRoute.Calendar -> Icon(Icons.Filled.CalendarMonth, contentDescription = item.label)
                                NavRoute.Focus -> Icon(Icons.Filled.Timer, contentDescription = item.label)
                                NavRoute.Insights -> Icon(Icons.Filled.Insights, contentDescription = item.label)
                            }
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Plan.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(NavRoute.Plan.route) { PlanScreen() }
            composable(NavRoute.Calendar.route) { CalendarScreen() }
            composable(NavRoute.Focus.route) { FocusScreen() }
            composable(NavRoute.Insights.route) { InsightsScreen() }
        }
    }
}
