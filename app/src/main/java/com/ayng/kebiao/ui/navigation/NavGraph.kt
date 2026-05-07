package com.ayng.kebiao.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayng.kebiao.KebiaoApp
import com.ayng.kebiao.ui.attendance.AttendanceScreen
import com.ayng.kebiao.ui.attendance.AttendanceViewModel
import com.ayng.kebiao.ui.salary.SalaryScreen
import com.ayng.kebiao.ui.salary.SalaryViewModel
import com.ayng.kebiao.ui.schedule.ScheduleScreen
import com.ayng.kebiao.ui.schedule.ScheduleViewModel
import com.ayng.kebiao.ui.settings.SettingsScreen
import com.ayng.kebiao.ui.settings.SettingsViewModel

sealed class Screen(val route: String, val label: String) {
    data object Schedule : Screen("schedule", "课表")
    data object Attendance : Screen("attendance", "出勤")
    data object Salary : Screen("salary", "工资")
    data object Settings : Screen("settings", "设置")
}

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Schedule, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Attendance, Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(Screen.Salary, Icons.Filled.MonetizationOn, Icons.Outlined.MonetizationOn),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun KebiaoNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as KebiaoApp

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.screen.label
                            )
                        },
                        label = { Text(item.screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Schedule.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Schedule.route) {
                val vm: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModel.Factory(app.repository)
                )
                ScheduleScreen(vm)
            }
            composable(Screen.Attendance.route) {
                val vm: AttendanceViewModel = viewModel(
                    factory = AttendanceViewModel.Factory(app.repository)
                )
                AttendanceScreen(vm)
            }
            composable(Screen.Salary.route) {
                val vm: SalaryViewModel = viewModel(
                    factory = SalaryViewModel.Factory(app.repository)
                )
                SalaryScreen(vm)
            }
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(app.repository)
                )
                SettingsScreen(vm)
            }
        }
    }
}
