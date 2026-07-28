package com.petmed.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.petmed.app.ui.screens.AIChatScreen
import com.petmed.app.ui.screens.HomeScreen
import com.petmed.app.ui.screens.HospitalSearchScreen
import com.petmed.app.ui.screens.MedicationRecordScreen
import com.petmed.app.ui.screens.ProfileScreen
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.White

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Hospital : Screen("hospital")
    object MedRecord : Screen("med_record")
    object Profile : Screen("profile")
    object AIChat : Screen("ai_chat")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "首頁", Icons.Default.Home),
    BottomNavItem(Screen.Hospital.route, "醫院", Icons.Default.LocalHospital),
    BottomNavItem(Screen.MedRecord.route, "提醒", Icons.Default.Notifications),
    BottomNavItem(Screen.Profile.route, "個人", Icons.Default.Person),
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val mainRoutes = listOf(Screen.Home.route, Screen.Hospital.route, Screen.MedRecord.route, Screen.Profile.route)
    val showBottomBar = currentRoute in mainRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = White) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Orange,
                                selectedTextColor = Orange,
                                indicatorColor = Orange.copy(alpha = 0.15f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAIChatClick = { navController.navigate(Screen.AIChat.route) },
                    onMedRecordClick = {
                        navController.navigate(Screen.MedRecord.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onHospitalClick = {
                        navController.navigate(Screen.Hospital.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Hospital.route) {
                HospitalSearchScreen()
            }
            composable(Screen.MedRecord.route) {
                MedicationRecordScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            composable(Screen.AIChat.route) {
                AIChatScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
