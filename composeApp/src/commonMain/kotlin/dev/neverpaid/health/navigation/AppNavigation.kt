package dev.neverpaid.health.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.features.body.BodyRoute
import dev.neverpaid.health.features.body.BodyViewModel
import dev.neverpaid.health.features.coach.CoachRoute
import dev.neverpaid.health.features.coach.CoachViewModel
import dev.neverpaid.health.features.dashboard.DashboardRoute
import dev.neverpaid.health.features.dashboard.DashboardViewModel
import dev.neverpaid.health.features.exercises.ExercisesRoute
import dev.neverpaid.health.features.exercises.ExercisesViewModel
import dev.neverpaid.health.features.history.HistoryRoute
import dev.neverpaid.health.features.history.HistoryViewModel
import dev.neverpaid.health.features.profile.ProfileRoute
import dev.neverpaid.health.features.profile.ProfileViewModel
import dev.neverpaid.health.features.progress.ProgressRoute
import dev.neverpaid.health.features.progress.ProgressViewModel
import dev.neverpaid.health.features.workout.WorkoutRoute
import dev.neverpaid.health.features.workout.WorkoutViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Exercises : Screen("exercises", "Exercises", Icons.Default.FitnessCenter)
    data object History : Screen("history", "History", Icons.Default.CalendarToday)
    data object Progress : Screen("progress", "Analytics", Icons.Default.ShowChart)
    data object Coach : Screen("coach", "AI Coach", Icons.Default.AutoAwesome)
    data object Body : Screen("body", "Body", Icons.Default.MonitorWeight)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object ActiveWorkout : Screen("active_workout", "Workout", Icons.Default.FitnessCenter)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Exercises,
    Screen.History,
    Screen.Progress,
    Screen.Coach,
    Screen.Body,
    Screen.Profile
)

@Composable
fun NeverPaidHealthApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isWorkoutActiveScreen = currentRoute == Screen.ActiveWorkout.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (!isWorkoutActiveScreen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier.height(68.dp)
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
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
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldNeon,
                                selectedTextColor = EmeraldNeon,
                                indicatorColor = EmeraldSubtle,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = koinViewModel()
                DashboardRoute(
                    viewModel = viewModel,
                    onNavigateToActiveWorkout = {
                        navController.navigate(Screen.ActiveWorkout.route)
                    }
                )
            }

            composable(Screen.Exercises.route) {
                val viewModel: ExercisesViewModel = koinViewModel()
                ExercisesRoute(viewModel = viewModel)
            }

            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = koinViewModel()
                HistoryRoute(viewModel = viewModel)
            }

            composable(Screen.Progress.route) {
                val viewModel: ProgressViewModel = koinViewModel()
                ProgressRoute(viewModel = viewModel)
            }

            composable(Screen.Coach.route) {
                val viewModel: CoachViewModel = koinViewModel()
                CoachRoute(viewModel = viewModel)
            }

            composable(Screen.Body.route) {
                val viewModel: BodyViewModel = koinViewModel()
                BodyRoute(viewModel = viewModel)
            }

            composable(Screen.Profile.route) {
                val viewModel: ProfileViewModel = koinViewModel()
                ProfileRoute(viewModel = viewModel)
            }

            composable(Screen.ActiveWorkout.route) {
                val viewModel: WorkoutViewModel = koinViewModel()
                WorkoutRoute(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
