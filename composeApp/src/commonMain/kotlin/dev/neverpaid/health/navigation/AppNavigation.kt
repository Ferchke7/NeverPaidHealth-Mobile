package dev.neverpaid.health.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DudaBrandLogo
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.FloatingActiveWorkoutPill
import dev.neverpaid.health.core.designsystem.FloatingFocusTimerOverlay
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
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
    data object Todo : Screen("todo", Icons.Default.CalendarToday)
    data object Nutrition : Screen("nutrition", Icons.Default.Restaurant)
    data object Coach : Screen("coach", Icons.Default.AutoAwesome)
    data object Progress : Screen("progress", Icons.Default.ShowChart)

    // Sub-destinations
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
    Screen.Workouts,
    Screen.Programs,
    Screen.History,
    Screen.Exercises,
    Screen.Body,
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
    val currentUser by repository.getCurrentUser().collectAsState(initial = null)
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

        val isWorkoutActiveScreen = currentScreen == Screen.ActiveWorkout
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
                Screen.Exercises -> "Упражнения"
                Screen.History -> strings.historyTitle
                Screen.Body -> strings.bodyTitle
                Screen.Profile -> strings.profileTitle
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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            topBar = {
                if (!isWorkoutActiveScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .statusBarsPadding()
                    ) {
                        if (isSecondaryScreen) {
                            // Secondary Screen Header: Back Arrow + Page Title
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentScreen = previousScreen },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.back,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = getScreenTitle(currentScreen),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        } else {
                            // Primary Screen Header: Brand Logo + Status Pills (LIVE, Focus) + Profile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.clickable { navigateTo(Screen.Workouts) }
                                ) {
                                    DudaBrandLogo(compact = false)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Active Workout pulsing header chip
                                    if (activeWorkout != null) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldNeon)
                                                .clickable { navigateTo(Screen.ActiveWorkout) }
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                            Text(
                                                text = "LIVE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    // Focus Timer quick launcher button
                                    if (focusTimerState.isOpen) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(IndigoSubtle)
                                                .clickable {
                                                    repository.setFocusModalOpen(!focusTimerState.isModalOpen)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Фокус-таймер",
                                                tint = IndigoElectric,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // User Profile Avatar Button
                                    val isGuest = repository.isGuestUser()
                                    val userInitials = if (isGuest) "G" else currentUser?.name?.trim()?.take(1)?.uppercase().orEmpty()
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (currentScreen == Screen.Profile) EmeraldNeon else DarkSurfaceHigh
                                            )
                                            .border(
                                                1.dp,
                                                if (isGuest) dev.neverpaid.health.core.designsystem.AmberWarm.copy(alpha = 0.6f) else EmeraldNeon.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                            .clickable { navigateTo(Screen.Profile) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (userInitials.isNotEmpty()) {
                                            Text(
                                                text = userInitials,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (currentScreen == Screen.Profile) Color.Black else if (isGuest) dev.neverpaid.health.core.designsystem.AmberWarm else EmeraldNeon
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = strings.profileTitle,
                                                tint = if (currentScreen == Screen.Profile) Color.Black else EmeraldNeon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                }
            },
            bottomBar = {
                if (!isWorkoutActiveScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Global Minimized Focus Timer Pill
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

                        // Global Floating Active Workout Pill
                        if (activeWorkout != null) {
                            FloatingActiveWorkoutPill(
                                workout = activeWorkout!!,
                                onExpand = { navigateTo(Screen.ActiveWorkout) }
                            )
                        }

                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = TextPrimary,
                            tonalElevation = 8.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
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
            }
        ) { innerPadding ->
            // Fullscreen Focus Timer Modal when not minimized
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
                        CoachRoute(
                            viewModel = viewModel,
                            onNavigateToActiveWorkout = { navigateTo(Screen.ActiveWorkout) }
                        )
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
