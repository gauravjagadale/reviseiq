package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.notification.StudyNotificationScheduler
import com.example.ui.ReviseViewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.AccountSwitch
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.DecksScreen
import com.example.ui.screens.FlashcardReviewScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.ReviseIQTheme

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.QuickAddFlashcardBottomSheet

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

        // Restore the daily study alarm whenever the app starts, so a cleared
        // or missed alarm (e.g. force-stop, reboot edge cases) is re-armed.
        StudyNotificationScheduler.rescheduleReminderIfEnabled(this)

        setContent {
            val viewModel: ReviseViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            var showQuickAddSheet by remember { mutableStateOf(false) }
            var pendingAccountSwitch by remember { mutableStateOf<AccountSwitch?>(null) }

            // A different account signed in: ask how to handle the leftover
            // local data of the previous account before merging cloud data.
            LaunchedEffect(Unit) {
                authViewModel.accountSwitchRequest.collect { request ->
                    pendingAccountSwitch = request
                }
            }

            // Handle shortcut intent trigger
            LaunchedEffect(triggerQuickAddShortcutState.value) {
                if (triggerQuickAddShortcutState.value) {
                    showQuickAddSheet = true
                    triggerQuickAddShortcutState.value = false
                }
            }

            ReviseIQTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val context = LocalContext.current

                // Pomodoro survives the background: arm the completion alarm on
                // stop, finalize any finished session + cancel stale alarms on resume.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded(context.applicationContext)
                            Lifecycle.Event.ON_RESUME -> viewModel.onAppForegrounded(context.applicationContext)
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val bottomItems = listOf(
                    BottomNavItem(NavRoutes.Dashboard.route, "Home", Icons.Default.Home),
                    BottomNavItem(NavRoutes.Calendar.route, "Calendar", Icons.Default.CalendarMonth),
                    BottomNavItem(NavRoutes.Decks.route, "Decks", Icons.Default.Folder),
                    BottomNavItem(NavRoutes.Statistics.route, "Stats", Icons.Default.BarChart),
                    BottomNavItem(NavRoutes.Settings.route, "Settings", Icons.Default.Settings)
                )

                val tabRoutes = bottomItems.map { it.route }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Only show bottom bar on main tabs
                val showBottomBar = bottomItems.any { it.route == currentRoute }

                // Main tabs live in a swipeable HorizontalPager; detail screens
                // (deck detail / review / quiz) are regular NavHost destinations.
                val showPager = currentRoute in tabRoutes
                val pagerState = rememberPagerState(pageCount = { tabRoutes.size })

                // Bottom bar tracks the pager page instantly (mid-drag), so the
                // highlight never lags behind the finger.
                val selectedRoute = if (showPager) {
                    tabRoutes.getOrNull(pagerState.currentPage) ?: currentRoute
                } else {
                    currentRoute
                }

                // Route change (bottom bar tap, AI tile, calendar shortcuts) → scroll pager.
                LaunchedEffect(currentRoute) {
                    val index = tabRoutes.indexOf(currentRoute)
                    if (index >= 0 && pagerState.currentPage != index) {
                        pagerState.scrollToPage(index)
                    }
                }

                // Settled swipe → navigate so the route + bottom bar stay in sync.
                LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (!pagerState.isScrollInProgress) {
                        val route = tabRoutes.getOrNull(pagerState.currentPage)
                        if (route != null && currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            ) {
                                bottomItems.forEach { item ->
                                    val isSelected = selectedRoute == item.route
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
                    val contentModifier = Modifier.padding(innerPadding)

                    Box(modifier = contentModifier.fillMaxSize()) {
                        // NavHost is ALWAYS composed so the navigation graph stays
                        // initialized; tab destinations are empty stubs (invisible),
                        // the HorizontalPager below draws on top of them.
                        NavHost(
                            navController = navController,
                            startDestination = NavRoutes.Dashboard.route,
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = { fadeIn(tween(220)) },
                            exitTransition = { fadeOut(tween(180)) },
                            popEnterTransition = { fadeIn(tween(220)) },
                            popExitTransition = { fadeOut(tween(180)) }
                        ) {
                            // Tab routes stay in the graph (empty) so every
                            // navigate() call keeps working; tabs render inside
                            // the HorizontalPager above.
                            tabRoutes.forEach { route ->
                                composable(route) {}
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

                            composable(NavRoutes.Login.route) {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoggedIn = {
                                        // Pull the user's cloud data immediately after a
                                        // successful sign-in, instead of waiting for the
                                        // next app foreground event.
                                        viewModel.syncNow()
                                        navController.popBackStack()
                                    },
                                    onGuest = { navController.popBackStack() }
                                )
                            }
                        }

                        pendingAccountSwitch?.let { request ->
                            AlertDialog(
                                onDismissRequest = { pendingAccountSwitch = null },
                                title = { Text("Switch accounts?") },
                                text = {
                                    Text(
                                        "You signed in with a different account. What should " +
                                            "happen to the study data saved on this device?"
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.wipeLocalDataForNewAccount(request.previousUserId)
                                            pendingAccountSwitch = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = androidx.compose.material3.MaterialTheme
                                                .colorScheme.error
                                        )
                                    ) {
                                        Text("Wipe & restore from cloud")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        viewModel.syncNow()
                                        pendingAccountSwitch = null
                                    }) {
                                        Text("Keep & merge")
                                    }
                                }
                            )
                        }

                        if (showPager) {
                            // Main tabs: swipe left/right to move between screens.
                            // Adjacent pages are composed offscreen so swiping
                            // across a page boundary never shows a blank frame.
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (tabRoutes[page]) {
                                    NavRoutes.Dashboard.route -> DashboardScreen(
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
                                        }
                                    )

                                    NavRoutes.Calendar.route -> CalendarScreen(
                                        viewModel = viewModel,
                                        onNavigateToReview = { deckId ->
                                            navController.navigate(NavRoutes.FlashcardReview.createRoute(deckId))
                                        }
                                    )

                                    NavRoutes.Decks.route -> DecksScreen(
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

                                    NavRoutes.Statistics.route -> StatisticsScreen(viewModel = viewModel)

                                    else -> SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateToLogin = {
                                            navController.navigate(NavRoutes.Login.route)
                                        }
                                    )
                                }
                            }
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
