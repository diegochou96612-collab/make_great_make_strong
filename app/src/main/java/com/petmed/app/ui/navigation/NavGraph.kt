package com.petmed.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petmed.app.ui.screens.AdsScreen
import com.petmed.app.ui.screens.CasePhotoScreen
import com.petmed.app.ui.screens.CaseReportScreen
import com.petmed.app.ui.screens.HomeScreen
import com.petmed.app.ui.screens.InterviewScreen
import com.petmed.app.ui.screens.HospitalSearchScreen
import com.petmed.app.ui.screens.MedicationRecordScreen
import com.petmed.app.ui.screens.ProfileScreen
import com.petmed.app.ui.screens.VaccineGuideScreen
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.White

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Hospital : Screen("hospital")
    object MedRecord : Screen("med_record")
    object Profile : Screen("profile")
    object AIChat : Screen("ai_chat")
    object CaseReport : Screen("case/{id}") {
        fun route(id: Long) = "case/$id"
    }
    object CasePhotos : Screen("case/{id}/photos") {
        fun route(id: Long) = "case/$id/photos"
    }
    object Ads : Screen("ads")
    object Vaccine : Screen("vaccine")
    object CaseChat : Screen("case_chat/{id}") {
        fun route(id: Long) = "case_chat/$id"
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "首頁", Icons.Default.Home),
    BottomNavItem(Screen.Hospital.route, "醫院", Icons.Default.LocalHospital),
    BottomNavItem(Screen.AIChat.route, "AI 記錄助理", Icons.Default.SmartToy),
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
                PetBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Screen.AIChat.route) {
                            navController.navigate(route)
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
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
                    onAdsClick = { navController.navigate(Screen.Ads.route) },
                    onVaccineClick = { navController.navigate(Screen.Vaccine.route) },
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
                InterviewScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCase = { id -> navController.navigate(Screen.CaseReport.route(id)) }
                )
            }
            composable(
                route = Screen.CaseReport.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                CaseReportScreen(
                    caseId = id,
                    onBack = { navController.popBackStack() },
                    onOpenPhotos = { navController.navigate(Screen.CasePhotos.route(id)) },
                    onContinueChat = { navController.navigate(Screen.CaseChat.route(id)) }
                )
            }
            composable(
                route = Screen.CasePhotos.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                CasePhotoScreen(
                    caseId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.CaseChat.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                InterviewScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCase = { navController.popBackStack() },
                    openCaseId = id
                )
            }
            composable(Screen.Vaccine.route) {
                VaccineGuideScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Ads.route) {
                AdsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PetBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .navigationBarsPadding()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                val isCenter = item.route == Screen.AIChat.route
                val tint = if (selected || isCenter) Orange else Color.Gray

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCenter) {
                        // 中間的 AI 按鈕：凸起的橘色圓形
                        Box(
                            modifier = Modifier
                                .offset(y = (-22).dp)
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Orange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .shadow(6.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Orange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = item.label,
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.offset(y = (-18).dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selected) Orange.copy(alpha = 0.15f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = tint)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = tint,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
