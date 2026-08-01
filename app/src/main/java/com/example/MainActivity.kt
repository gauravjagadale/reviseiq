package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ReviseViewModel
import com.example.ui.components.ThemeFloatingActionButton
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.DecksScreen
import com.example.ui.screens.FlashcardReviewScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.ReviseIQTheme

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.ui.components.QuickAddFlashcardBottomSheet
import com.example.ui.components.QuickAddFloatingActionButton

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    private var triggerQuickAddShortcutState = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkQuickAddIntent(intent)

        setContent {
            val viewModel: ReviseViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            var showQuickAddSheet by remember { mutableStateOf(false) }

            // Handle shortcut intent trigger
            LaunchedEffect(triggerQuickAddShortcutState.value) {
                if (triggerQuickAddShortcutState.value) {
                    showQuickAddSheet = true
                    triggerQuickAddShortcutState.value = false
                }
            }

            ReviseIQTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                val bottomItems = listOf(
                    BottomNavItem(NavRoutes.Dashboard.route, "Home", Icons.Default.Home),
                    BottomNavItem(NavRoutes.Calendar.route, "Calendar", Icons.Default.CalendarMonth),
                    BottomNavItem(NavRoutes.Decks.route, "Decks", Icons.Default.Folder),
                    BottomNavItem(NavRoutes.Statistics.route, "Stats", Icons.Default.BarChart)
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Only show bottom bar on main tabs
                val showBottomBar = bottomItems.any { it.route == currentRoute }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeFloatingActionButton(
                                isDarkMode = isDarkMode,
                                onToggleTheme = { viewModel.toggleDarkMode() }
                            )

                            QuickAddFloatingActionButton(
                                onClick = { showQuickAddSheet = true }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            ) {
                                bottomItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title
                                            )
                                        },
                                        label = { Text(item.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToReview = { deckId ->
                                    navController.navigate(NavRoutes.FlashcardReview.createRoute(deckId))
                                },
                                onNavigateToQuiz = { deckId ->
                                    navController.navigate(NavRoutes.QuizEngine.createRoute(deckId))
                                },
                                onNavigateToCalendar = {
                                    navController.navigate(NavRoutes.Calendar.route)
                                },
                                onNavigateToDecks = {
                                    navController.navigate(NavRoutes.Decks.route)
                                },
                                onOpenAiGenerator = {
                                    navController.navigate(NavRoutes.Decks.route)
                                }
                            )
                        }

                        composable(NavRoutes.Calendar.route) {
                            CalendarScreen(
                                viewModel = viewModel,
                                onNavigateToReview = { deckId ->
                                    navController.navigate(NavRoutes.FlashcardReview.createRoute(deckId))
                                }
                            )
                        }

                        composable(NavRoutes.Decks.route) {
                            DecksScreen(
                                viewModel = viewModel,
                                onNavigateToDeckDetail = { deckId ->
                                    navController.navigate(NavRoutes.DeckDetail.createRoute(deckId))
                                },
                                onNavigateToReview = { deckId ->
                                    navController.navigate(NavRoutes.FlashcardReview.createRoute(deckId))
                                },
                                onNavigateToQuiz = { deckId ->
                                    navController.navigate(NavRoutes.QuizEngine.createRoute(deckId))
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.DeckDetail.route,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                        ) { backStack ->
                            val deckId = backStack.arguments?.getLong("deckId") ?: 0L
                            DeckDetailScreen(
                                deckId = deckId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToReview = { dId ->
                                    navController.navigate(NavRoutes.FlashcardReview.createRoute(dId))
                                },
                                onNavigateToQuiz = { dId ->
                                    navController.navigate(NavRoutes.QuizEngine.createRoute(dId))
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.FlashcardReview.route,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                        ) { backStack ->
                            val deckId = backStack.arguments?.getLong("deckId") ?: 0L
                            FlashcardReviewScreen(
                                deckId = deckId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = NavRoutes.QuizEngine.route,
                            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
                        ) { backStack ->
                            val deckId = backStack.arguments?.getLong("deckId") ?: 0L
                            QuizScreen(
                                deckId = deckId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.Statistics.route) {
                            StatisticsScreen(viewModel = viewModel)
                        }
                    }

                    if (showQuickAddSheet) {
                        QuickAddFlashcardBottomSheet(
                            viewModel = viewModel,
                            onDismiss = { showQuickAddSheet = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkQuickAddIntent(intent)
    }

    private fun checkQuickAddIntent(intent: Intent?) {
        val action = intent?.getStringExtra("action")
        if (action == "quick_add") {
            triggerQuickAddShortcutState.value = true
        }
    }
}
