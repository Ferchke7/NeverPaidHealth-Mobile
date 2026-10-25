package dev.neverpaid.health.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
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
import org.koin.compose.viewmodel.koinViewModel

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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val isWorkoutActiveScreen = currentScreen == Screen.ActiveWorkout

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
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentScreen != screen) {
                                    previousScreen = currentScreen
                                    currentScreen = screen
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> {
                    val viewModel: DashboardViewModel = koinViewModel()
                    DashboardRoute(
                        viewModel = viewModel,
                        onNavigateToActiveWorkout = {
                            previousScreen = currentScreen
                            currentScreen = Screen.ActiveWorkout
                        }
                    )
                }
                Screen.Exercises -> {
                    val viewModel: ExercisesViewModel = koinViewModel()
                    ExercisesRoute(viewModel = viewModel)
                }
                Screen.History -> {
                    val viewModel: HistoryViewModel = koinViewModel()
                    HistoryRoute(viewModel = viewModel)
                }
                Screen.Progress -> {
                    val viewModel: ProgressViewModel = koinViewModel()
                    ProgressRoute(viewModel = viewModel)
                }
                Screen.Coach -> {
                    val viewModel: CoachViewModel = koinViewModel()
                    CoachRoute(viewModel = viewModel)
                }
                Screen.Body -> {
                    val viewModel: BodyViewModel = koinViewModel()
                    BodyRoute(viewModel = viewModel)
                }
                Screen.Profile -> {
                    val viewModel: ProfileViewModel = koinViewModel()
                    ProfileRoute(viewModel = viewModel)
                }
                Screen.ActiveWorkout -> {
                    val viewModel: WorkoutViewModel = koinViewModel()
                    WorkoutRoute(
                        viewModel = viewModel,
                        onNavigateBack = {
                            currentScreen = previousScreen
                        }
                    )
                }
            }
        }
    }
}
