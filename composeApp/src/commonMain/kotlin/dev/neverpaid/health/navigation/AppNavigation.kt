package dev.neverpaid.health.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.FloatingActiveWorkoutPill
import dev.neverpaid.health.core.designsystem.FloatingFocusTimerOverlay
import dev.neverpaid.health.core.designsystem.ObsidianBottomNavBar
import dev.neverpaid.health.core.designsystem.ObsidianHeaderBar
import dev.neverpaid.health.core.designsystem.ObsidianNavDestination
import dev.neverpaid.health.core.designsystem.ObsidianSectionMenuSheet
import dev.neverpaid.health.core.designsystem.WorkoutSummaryBottomSheet
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.i18n.getDudaStrings
import dev.neverpaid.health.domain.model.FocusTimerState
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

sealed class Screen(val route: String, val icon: ImageVector) {
    data object Workouts : Screen("workouts", Icons.Default.FitnessCenter)
    data object Todo : Screen("todo", Icons.Default.Checklist)
    data object Nutrition : Screen("nutrition", Icons.Default.Restaurant)
    data object Coach : Screen("coach", Icons.Default.AutoAwesome)
    data object Progress : Screen("progress", Icons.AutoMirrored.Filled.ShowChart)

    // Menu sub-destinations (all reachable via "Меню" -> "Все разделы")
    data object Programs : Screen("programs", Icons.Default.ViewAgenda)
    data object Exercises : Screen("exercises", Icons.Default.Layers)
    data object History : Screen("history", Icons.Default.History)
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

val quickTopNavPills = listOf(
    Screen.Programs,
    Screen.Exercises,
    Screen.History,
    Screen.Body
)

val allMenuDestinations = listOf(
    Screen.Workouts,
    Screen.Programs,
    Screen.Exercises,
    Screen.History,
    Screen.Progress,
    Screen.Body,
    Screen.Nutrition,
    Screen.Todo,
    Screen.Coach,
    Screen.Profile
)

@Composable
fun NeverPaidHealthApp() {
    DudaMobileApp()
}

@Composable
fun DudaMobileApp() {
    val repository: NeverPaidRepository = koinInject()
    val scope = rememberCoroutineScope()
    val isAuthenticated by repository.isUserAuthenticated().collectAsState(initial = false)
    val preferences by repository.getUserPreferences().collectAsState(initial = UserPreferencesData())
    val activeWorkout by repository.getActiveWorkout().collectAsState(initial = null)
    val lastFinishedWorkout by repository.getLastFinishedWorkoutSummary().collectAsState(initial = null)
    val focusTimerState by repository.getFocusTimerState().collectAsState(initial = FocusTimerState())
    val strings = remember(preferences.languageCode) { getDudaStrings(preferences.languageCode) }

    CompositionLocalProvider(LocalDudaStrings provides strings) {
        if (!isAuthenticated) {
            val authViewModel: AuthViewModel = koinViewModel()
            AuthRoute(
                viewModel = authViewModel,
                onNavigateToHome = {}
            )
            return@CompositionLocalProvider
        }

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Workouts) }
        var previousScreen by remember { mutableStateOf<Screen>(Screen.Workouts) }
        var isMenuSheetOpen by remember { mutableStateOf(false) }

        val isWorkoutActiveScreen = currentScreen == Screen.ActiveWorkout

        fun navigateTo(target: Screen) {
            if (currentScreen != target) {
                previousScreen = currentScreen
                currentScreen = target
            }
        }

        fun navigateByRoute(route: String) {
            val target = allMenuDestinations.find { it.route == route } ?: when (route) {
                "active_workout" -> Screen.ActiveWorkout
                "auth" -> Screen.Auth
                else -> Screen.Workouts
            }
            navigateTo(target)
        }

        fun getScreenTitle(screen: Screen): String {
            return when (screen) {
                Screen.Workouts -> strings.navWorkouts
                Screen.Todo -> strings.navTodo
                Screen.Nutrition -> strings.navNutrition
                Screen.Coach -> strings.navCoach
                Screen.Progress -> strings.navProgress
                Screen.Programs -> strings.navPrograms
                Screen.Exercises -> strings.navExercises
                Screen.History -> strings.navHistory
                Screen.Body -> strings.navBody
                Screen.Profile -> strings.navProfile
                Screen.ActiveWorkout -> strings.dashboardActiveBanner
                Screen.Auth -> strings.authTitle
            }
        }

        if (lastFinishedWorkout != null) {
            WorkoutSummaryBottomSheet(
                workout = lastFinishedWorkout!!,
                onDismiss = {
                    repository.dismissWorkoutSummary()
                }
            )
        }

        if (isMenuSheetOpen) {
            val highlightedRoute = if (currentScreen == Screen.ActiveWorkout) Screen.Workouts.route else currentScreen.route
            ObsidianSectionMenuSheet(
                title = strings.menuAllSections,
                subtitle = strings.menuAllSectionsSubtitle,
                destinations = allMenuDestinations.map { screen ->
                    ObsidianNavDestination(
                        route = screen.route,
                        label = getScreenTitle(screen),
                        icon = screen.icon
                    )
                },
                currentRoute = highlightedRoute,
                onSelectRoute = { route -> navigateByRoute(route) },
                onDismiss = { isMenuSheetOpen = false }
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .statusBarsPadding()
                ) {
                    ObsidianHeaderBar(
                        menuLabel = strings.menuButton,
                        hasActiveWorkout = activeWorkout != null,
                        isWorkoutScreen = isWorkoutActiveScreen,
                        hasFocusOpen = focusTimerState.isOpen,
                        onLogoClick = { navigateTo(Screen.Workouts) },
                        onLiveClick = { navigateTo(Screen.ActiveWorkout) },
                        onFocusClick = { repository.setFocusModalOpen(!focusTimerState.isModalOpen) },
                        onMenuClick = { isMenuSheetOpen = true }
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                ) {
                    if (focusTimerState.isOpen && focusTimerState.isMinimized) {
                        FloatingFocusTimerOverlay(
                            state = focusTimerState,
                            onToggleRunning = {
                                if (focusTimerState.isRunning) repository.pauseFocusTimer()
                                else repository.resumeFocusTimer()
                            },
                            onReset = { repository.resetFocusPhase() },
                            onMinimize = { min -> repository.setFocusModalOpen(!min) },
                            onClose = { repository.discardFocusTimer() },
                            onSetMode = { mode, _, mins ->
                                repository.setFocusMode(mode)
                                repository.setFocusSplitStrategy("${mins}m")
                            },
                            onCompleteSession = {
                                scope.launch { repository.finishAndLogFocusSession(markCompleted = true) }
                            }
                        )
                    }

                    if (activeWorkout != null && !isWorkoutActiveScreen) {
                        FloatingActiveWorkoutPill(
                            workout = activeWorkout!!,
                            onExpand = { navigateTo(Screen.ActiveWorkout) }
                        )
                    }

                    ObsidianBottomNavBar(
                        items = mobileBottomNavItems.map { screen ->
                            ObsidianNavDestination(
                                route = screen.route,
                                label = getScreenTitle(screen),
                                icon = screen.icon
                            )
                        },
                        selectedRoute = when (currentScreen) {
                            Screen.Workouts, Screen.Programs, Screen.Exercises, Screen.ActiveWorkout -> Screen.Workouts.route
                            Screen.Todo -> Screen.Todo.route
                            Screen.Nutrition -> Screen.Nutrition.route
                            Screen.Coach -> Screen.Coach.route
                            Screen.Progress, Screen.History, Screen.Body -> Screen.Progress.route
                            else -> ""
                        },
                        onSelectRoute = { route -> navigateByRoute(route) }
                    )
                }
            }
        ) { innerPadding ->
            if (focusTimerState.isOpen && !focusTimerState.isMinimized) {
                FloatingFocusTimerOverlay(
                    state = focusTimerState,
                    onToggleRunning = {
                        if (focusTimerState.isRunning) repository.pauseFocusTimer()
                        else repository.resumeFocusTimer()
                    },
                    onReset = { repository.resetFocusPhase() },
                    onMinimize = { min -> repository.setFocusModalOpen(!min) },
                    onClose = { repository.discardFocusTimer() },
                    onSetMode = { mode, _, mins ->
                        repository.setFocusMode(mode)
                        repository.setFocusSplitStrategy("${mins}m")
                    },
                    onCompleteSession = {
                        scope.launch { repository.finishAndLogFocusSession(markCompleted = true) }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Workouts -> {
                        val viewModel: DashboardViewModel = koinViewModel()
                        DashboardRoute(
                            viewModel = viewModel,
                            onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) },
                            onNavigateToScreen = { routeName -> navigateByRoute(routeName) }
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
                        CoachRoute(
                            viewModel = viewModel,
                            onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) }
                        )
                    }
                    Screen.Progress -> {
                        val viewModel: ProgressViewModel = koinViewModel()
                        ProgressRoute(
                            viewModel = viewModel,
                            onNavigateToHistory = { navigateTo(Screen.History) },
                            onNavigateToBody = { navigateTo(Screen.Body) }
                        )
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
                        ExercisesRoute(
                            viewModel = viewModel,
                            onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) }
                        )
                    }
                    Screen.History -> {
                        val viewModel: HistoryViewModel = koinViewModel()
                        HistoryRoute(
                            viewModel = viewModel,
                            onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) }
                        )
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
