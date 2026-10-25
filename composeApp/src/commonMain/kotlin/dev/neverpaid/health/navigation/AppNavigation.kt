package dev.neverpaid.health.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
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
import dev.neverpaid.health.features.nutrition.NutritionScreen
import dev.neverpaid.health.features.profile.ProfileRoute
import dev.neverpaid.health.features.profile.ProfileViewModel
import dev.neverpaid.health.features.progress.ProgressRoute
import dev.neverpaid.health.features.progress.ProgressViewModel
import dev.neverpaid.health.features.todo.TodoScreen
import dev.neverpaid.health.features.workout.WorkoutRoute
import dev.neverpaid.health.features.workout.WorkoutViewModel
import org.koin.compose.viewmodel.koinViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Workouts : Screen("workouts", "Тренировка", Icons.Default.FitnessCenter)
    data object Todo : Screen("todo", "План", Icons.Default.CalendarToday)
    data object Nutrition : Screen("nutrition", "Питание", Icons.Default.Restaurant)
    data object Coach : Screen("coach", "AI Тренер", Icons.Default.AutoAwesome)
    data object Progress : Screen("progress", "Прогресс", Icons.Default.ShowChart)

    // Secondary sub-destinations
    data object Exercises : Screen("exercises", "Упражнения", Icons.Default.FitnessCenter)
    data object History : Screen("history", "История", Icons.Default.CalendarToday)
    data object Body : Screen("body", "Тело", Icons.Default.MonitorWeight)
    data object Profile : Screen("profile", "Профиль", Icons.Default.Person)
    data object ActiveWorkout : Screen("active_workout", "Сессия", Icons.Default.FitnessCenter)
}

// Exactly 5 high-frequency mobile bottom tabs matching duda.uz
val mobileBottomNavItems = listOf(
    Screen.Workouts,
    Screen.Todo,
    Screen.Nutrition,
    Screen.Coach,
    Screen.Progress
)

@Composable
fun NeverPaidHealthApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Workouts) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Workouts) }

    val isWorkoutActiveScreen = currentScreen == Screen.ActiveWorkout
    val isSecondaryScreen = currentScreen in listOf(
        Screen.Exercises,
        Screen.History,
        Screen.Body,
        Screen.Profile
    )

    fun navigateTo(target: Screen) {
        if (currentScreen != target) {
            previousScreen = currentScreen
            currentScreen = target
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (!isWorkoutActiveScreen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.height(64.dp)
                ) {
                    mobileBottomNavItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { navigateTo(screen) },
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
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldLight,
                                selectedTextColor = EmeraldLight,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Secondary Header with Back button if on secondary screen
            if (isSecondaryScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentScreen = previousScreen }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentScreen.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (currentScreen) {
                    Screen.Workouts -> {
                        val viewModel: DashboardViewModel = koinViewModel()
                        DashboardRoute(
                            viewModel = viewModel,
                            onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) },
                            onNavigateToScreen = { routeName ->
                                when (routeName) {
                                    "exercises" -> navigateTo(Screen.Exercises)
                                    "history" -> navigateTo(Screen.History)
                                    "body" -> navigateTo(Screen.Body)
                                    "profile" -> navigateTo(Screen.Profile)
                                    "todo" -> navigateTo(Screen.Todo)
                                    "nutrition" -> navigateTo(Screen.Nutrition)
                                    "coach" -> navigateTo(Screen.Coach)
                                    "progress" -> navigateTo(Screen.Progress)
                                }
                            }
                        )
                    }
                    Screen.Todo -> {
                        TodoScreen(
                            onStartWorkout = { _ -> navigateTo(Screen.ActiveWorkout) },
                            onNavigateToProfile = { navigateTo(Screen.Profile) }
                        )
                    }
                    Screen.Nutrition -> {
                        NutritionScreen(
                            onNavigateToProfile = { navigateTo(Screen.Profile) }
                        )
                    }
                    Screen.Coach -> {
                        val viewModel: CoachViewModel = koinViewModel()
                        CoachRoute(viewModel = viewModel)
                    }
                    Screen.Progress -> {
                        val viewModel: ProgressViewModel = koinViewModel()
                        ProgressRoute(viewModel = viewModel)
                    }
                    Screen.Exercises -> {
                        val viewModel: ExercisesViewModel = koinViewModel()
                        ExercisesRoute(viewModel = viewModel)
                    }
                    Screen.History -> {
                        val viewModel: HistoryViewModel = koinViewModel()
                        HistoryRoute(viewModel = viewModel)
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
                            onNavigateBack = { currentScreen = previousScreen }
                        )
                    }
                }
            }
        }
    }
}
