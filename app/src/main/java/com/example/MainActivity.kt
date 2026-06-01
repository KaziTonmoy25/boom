package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.screens.*
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val userState by viewModel.userState.collectAsState()
            val isDarkMode = userState?.isDarkMode ?: true

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("onboarding") }
                    var bottomNavTab by remember { mutableStateOf("home") }

                    // Sync login state automatically to skip login if already active
                    LaunchedEffect(userState) {
                        if (userState != null && currentScreen == "onboarding") {
                            currentScreen = "main"
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "onboarding" -> {
                                OnboardingAuthScreen(
                                    viewModel = viewModel,
                                    onAuthSuccess = {
                                        currentScreen = "main"
                                        bottomNavTab = "home"
                                    }
                                )
                            }
                            "main" -> {
                                Scaffold(
                                    bottomBar = {
                                        NavigationBar(
                                            containerColor = CosmicSurface,
                                            contentColor = TextPrimary,
                                            tonalElevation = 8.dp,
                                            windowInsets = WindowInsets.navigationBars
                                        ) {
                                            NavigationBarItem(
                                                selected = bottomNavTab == "home",
                                                onClick = { bottomNavTab = "home" },
                                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                                label = { Text("Home", fontSize = 11.sp) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = PrimaryPurple,
                                                    selectedTextColor = PrimaryPurple,
                                                    indicatorColor = CosmicSurfaceVariant,
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = bottomNavTab == "chat",
                                                onClick = { bottomNavTab = "chat" },
                                                icon = { Icon(Icons.Default.Chat, contentDescription = "Ask AI") },
                                                label = { Text("Ask AI", fontSize = 11.sp) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = PrimaryPurple,
                                                    selectedTextColor = PrimaryPurple,
                                                    indicatorColor = CosmicSurfaceVariant,
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = bottomNavTab == "planner",
                                                onClick = { bottomNavTab = "planner" },
                                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Planner") },
                                                label = { Text("Planner", fontSize = 11.sp) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = PrimaryPurple,
                                                    selectedTextColor = PrimaryPurple,
                                                    indicatorColor = CosmicSurfaceVariant,
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = bottomNavTab == "settings",
                                                onClick = { bottomNavTab = "settings" },
                                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                                label = { Text("Settings", fontSize = 11.sp) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = PrimaryPurple,
                                                    selectedTextColor = PrimaryPurple,
                                                    indicatorColor = CosmicSurfaceVariant,
                                                    unselectedIconColor = TextSecondary,
                                                    unselectedTextColor = TextSecondary
                                                )
                                            )
                                        }
                                    }
                                ) { paddingValues ->
                                    Box(modifier = Modifier.padding(paddingValues)) {
                                        when (bottomNavTab) {
                                            "home" -> {
                                                DashboardScreen(
                                                    viewModel = viewModel,
                                                    onNavigateToChat = { bottomNavTab = "chat" },
                                                    onNavigateToQuiz = { currentScreen = "quiz" },
                                                    onNavigateToNotes = { currentScreen = "notes" },
                                                    onNavigateToIelts = { currentScreen = "ielts" },
                                                    onNavigateToSat = { currentScreen = "sat" },
                                                    onNavigateToPlanner = { bottomNavTab = "planner" },
                                                    onNavigateToPremium = { currentScreen = "premium" }
                                                )
                                            }
                                            "chat" -> {
                                                ChatScreen(
                                                    viewModel = viewModel,
                                                    onNavigateBack = { bottomNavTab = "home" }
                                                )
                                            }
                                            "planner" -> {
                                                PlannerScreen(
                                                    viewModel = viewModel,
                                                    onNavigateBack = { bottomNavTab = "home" }
                                                )
                                            }
                                            "settings" -> {
                                                SettingsScreen(
                                                    viewModel = viewModel,
                                                    onLogoutPressed = { currentScreen = "onboarding" }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "quiz" -> {
                                QuizScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "main" }
                                )
                            }
                            "notes" -> {
                                NotesScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "main" }
                                )
                            }
                            "ielts" -> {
                                IeltsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "main" }
                                )
                            }
                            "sat" -> {
                                SatScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "main" }
                                )
                            }
                            "premium" -> {
                                PremiumScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "main" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
