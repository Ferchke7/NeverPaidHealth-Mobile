package dev.neverpaid.health.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.i18n.getDudaStrings
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import dev.neverpaid.health.features.auth.AuthRoute
import dev.neverpaid.health.features.auth.AuthViewModel
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
import dev.neverpaid.health.features.nutrition.NutritionRoute
import dev.neverpaid.health.features.nutrition.NutritionViewModel
import dev.neverpaid.health.features.profile.ProfileRoute
import dev.neverpaid.health.features.profile.ProfileViewModel
import dev.neverpaid.health.features.programs.ProgramsRoute
import dev.neverpaid.health.features.programs.ProgramsViewModel
import dev.neverpaid.health.features.progress.ProgressRoute
import dev.neverpaid.health.features.progress.ProgressViewModel
import dev.neverpaid.health.features.todo.TodoRoute
import dev.neverpaid.health.features.todo.TodoViewModel
import dev.neverpaid.health.features.workout.WorkoutRoute
import dev.neverpaid.health.features.workout.WorkoutViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

sealed class Screen(val route: String, val icon: ImageVector) {
    data object Workouts : Screen("workouts", Icons.Default.FitnessCenter)
    data object Todo : Screen("todo", Icons.Default.CalendarToday)
    data object Nutrition : Screen("nutrition", Icons.Default.Restaurant)
    data object Coach : Screen("coach", Icons.Default.AutoAwesome)
    data object Progress : Screen("progress", Icons.Default.ShowChart)

    // Sub-destinations
    data object Programs : Screen("programs", Icons.Default.CalendarToday)
    data object Exercises : Screen("exercises", Icons.Default.Layers)
    data object History : Screen("history", Icons.Default.CalendarToday)
    data object Body : Screen("body", Icons.Default.MonitorWeight)
    data object Profile : Screen("profile", Icons.Default.Person)
    data object ActiveWorkout : Screen("active_workout", Icons.Default.FitnessCenter)
    data object Auth : Screen("auth", Icons.Default.Lock)
}

val mobileBottomNavItems = listOf(
    Screen.Workouts,
    Screen.Todo,
    Screen.Nutrition,
    Screen.Coach,
    Screen.Progress
)

@Composable
fun NeverPaidHealthApp() {
    DudaMobileApp()
}

@Composable
fun DudaMobileApp() {
    val repository: NeverPaidRepository = koinInject()
    val preferences by repository.getUserPreferences().collectAsState(initial = UserPreferencesData())
    val strings = remember(preferences.languageCode) { getDudaStrings(preferences.languageCode) }

    CompositionLocalProvider(LocalDudaStrings provides strings) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Workouts) }
        var previousScreen by remember { mutableStateOf<Screen>(Screen.Workouts) }

        val isWorkoutActiveScreen = currentScreen == Screen.ActiveWorkout
        val isAuthScreen = currentScreen == Screen.Auth
        val isSecondaryScreen = currentScreen in listOf(
            Screen.Programs,
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

        fun getScreenTitle(screen: Screen): String {
            return when (screen) {
                Screen.Workouts -> strings.navWorkouts
                Screen.Todo -> strings.navTodo
                Screen.Nutrition -> strings.navNutrition
                Screen.Coach -> strings.navCoach
                Screen.Progress -> strings.navProgress
                Screen.Programs -> strings.programsTitle
                Screen.Exercises -> strings.workoutExercisesCount
                Screen.History -> strings.historyTitle
                Screen.Body -> strings.bodyTitle
                Screen.Profile -> strings.profileTitle
                Screen.ActiveWorkout -> strings.dashboardActiveBanner
                Screen.Auth -> strings.authTitle
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            bottomBar = {
                if (!isWorkoutActiveScreen && !isAuthScreen) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.height(64.dp)
                    ) {
                        mobileBottomNavItems.forEach { screen ->
                            val isSelected = currentScreen == screen
                            val label = getScreenTitle(screen)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { navigateTo(screen) },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
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
                // Secondary Top Bar with back button
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
                                contentDescription = strings.back,
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = getScreenTitle(currentScreen),
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
                                        "programs" -> navigateTo(Screen.Programs)
                                        "exercises" -> navigateTo(Screen.Exercises)
                                        "history" -> navigateTo(Screen.History)
                                        "body" -> navigateTo(Screen.Body)
                                        "profile" -> navigateTo(Screen.Profile)
                                        "todo" -> navigateTo(Screen.Todo)
                                        "nutrition" -> navigateTo(Screen.Nutrition)
                                        "coach" -> navigateTo(Screen.Coach)
                                        "progress" -> navigateTo(Screen.Progress)
                                        "auth" -> navigateTo(Screen.Auth)
                                    }
                                }
                            )
                        }
                        Screen.Todo -> {
                            val viewModel: TodoViewModel = koinViewModel()
                            TodoRoute(
                                viewModel = viewModel,
                                onStartWorkout = { _ -> navigateTo(Screen.ActiveWorkout) },
                                onNavigateToProfile = { navigateTo(Screen.Profile) }
                            )
                        }
                        Screen.Nutrition -> {
                            val viewModel: NutritionViewModel = koinViewModel()
                            NutritionRoute(
                                viewModel = viewModel,
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
                        Screen.Programs -> {
                            val viewModel: ProgramsViewModel = koinViewModel()
                            ProgramsRoute(
                                viewModel = viewModel,
                                onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) }
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
                        Screen.Body -> {
                            val viewModel: BodyViewModel = koinViewModel()
                            BodyRoute(viewModel = viewModel)
                        }
                        Screen.Profile -> {
                            val viewModel: ProfileViewModel = koinViewModel()
                            ProfileRoute(
                                viewModel = viewModel,
                                onNavigateToAuth = { navigateTo(Screen.Auth) }
                            )
                        }
                        Screen.ActiveWorkout -> {
                            val viewModel: WorkoutViewModel = koinViewModel()
                            WorkoutRoute(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = previousScreen }
                            )
                        }
                        Screen.Auth -> {
                            val viewModel: AuthViewModel = koinViewModel()
                            AuthRoute(
                                viewModel = viewModel,
                                onNavigateToHome = { navigateTo(Screen.Workouts) }
                            )
                        }
                    }
                }
            }
        }
    }
}
