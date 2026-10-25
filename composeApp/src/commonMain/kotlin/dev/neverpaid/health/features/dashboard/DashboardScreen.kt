package dev.neverpaid.health.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.DudaQuickNavPills
import dev.neverpaid.health.core.designsystem.DudaTopHeader
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.QuickNavPillItem
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.domain.model.Routine
import kotlinx.coroutines.launch

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToScreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            DashboardUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
            is DashboardUiEffect.ShowSnackbar -> {
                scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        DashboardScreen(
            state = state,
            onAction = viewModel::onAction,
            onNavigateToScreen = onNavigateToScreen,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    onNavigateToScreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    val secondaryPills = remember {
        listOf(
            QuickNavPillItem("programs", "Программы", Icons.Default.CalendarToday),
            QuickNavPillItem("exercises", "Упражнения", Icons.Default.Layers),
            QuickNavPillItem("history", "История", Icons.Default.History),
            QuickNavPillItem("body", "Тело", Icons.Default.MonitorWeight),
            QuickNavPillItem("profile", "Профиль", Icons.Default.Person)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // duda.uz Top App Header
        item(key = "header") {
            DudaTopHeader(
                streakDays = state.currentStreakDays,
                onLogoClick = {},
                onProfileClick = { onNavigateToScreen("profile") },
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Secondary quick navigation pills (from duda.uz mobile)
        item(key = "quick_pills") {
            DudaQuickNavPills(
                items = secondaryPills,
                selectedId = "workouts",
                onSelect = { onNavigateToScreen(it) },
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Active workout banner if in progress
        if (state.hasActiveWorkout) {
            item(key = "active_workout_banner") {
                ActiveWorkoutBanner(
                    title = state.activeWorkoutTitle,
                    onResumeClick = { onAction(DashboardUiAction.OnResumeWorkoutClick) }
                )
            }
        }

        // HERO CARD: Active Program Split (Push Day) from duda.uz
        item(key = "hero_active_program") {
            ActiveProgramHeroCard(
                routine = state.recentRoutines.firstOrNull(),
                onStartRoutine = { routine ->
                    if (routine != null) {
                        onAction(DashboardUiAction.OnSelectRoutineClick(routine))
                    } else {
                        onAction(DashboardUiAction.OnStartEmptyWorkoutClick)
                    }
                },
                onStartEmpty = { onAction(DashboardUiAction.OnStartEmptyWorkoutClick) }
            )
        }

        // CNS Readiness Score Card (from duda.uz)
        item(key = "cns_readiness_card") {
            CnsReadinessCard(
                score = state.cnsScore,
                level = state.readinessLevel,
                message = state.statusMessage
            )
        }

        // Quick Athlete Stats Row
        item(key = "stats_card") {
            AthleticCard(backgroundColor = DarkSurfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCounter(
                        title = "Сессии",
                        value = "${state.totalWorkouts}",
                        unit = "трен.",
                        accentColor = EmeraldNeon
                    )
                    StatCounter(
                        title = "Общий объём",
                        value = StrengthCalculator.formatVolume(state.totalVolumeKg),
                        unit = "кг",
                        accentColor = IndigoElectric
                    )
                    StatCounter(
                        title = "Серия",
                        value = "${state.currentStreakDays}",
                        unit = "дней",
                        accentColor = AmberWarm
                    )
                }
            }
        }

        // Section: Мои шаблоны (My Routines)
        item(key = "routines_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "МОИ ШАБЛОНЫ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceHigh)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.recentRoutines.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Все программы →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldLight,
                    modifier = Modifier.clickable { onNavigateToScreen("exercises") }
                )
            }
        }

        // Vertical clean list of routines
        items(
            items = state.recentRoutines,
            key = { it.id }
        ) { routine ->
            RoutineListItem(
                routine = routine,
                onStart = { onAction(DashboardUiAction.OnSelectRoutineClick(routine)) }
            )
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ActiveProgramHeroCard(
    routine: Routine?,
    onStartRoutine: (Routine?) -> Unit,
    onStartEmpty: () -> Unit
) {
    val routineName = routine?.title ?: "Push Day (Hypertrophy)"
    val exerciseCount = routine?.exercises?.size ?: 4

    AthleticCard(
        backgroundColor = DarkSurfaceVariant,
        borderColor = BorderHighlight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldSubtle)
                        .border(width = 1.dp, color = BorderHighlight, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "АКТИВНАЯ ПРОГРАММА",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "День 1 из 4",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = routineName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "$exerciseCount упражнений • Жим лежа, Жим гантелей, Разводка, Трицепс",
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(14.dp))
            PrimaryAthleticButton(
                text = "Начать тренировку дня",
                onClick = { onStartRoutine(routine) },
                icon = Icons.Default.PlayArrow
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(10.dp))
                        .clickable { onStartEmpty() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Пустая тренировка",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(10.dp))
                        .clickable { onStartEmpty() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Новый шаблон",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineListItem(
    routine: Routine,
    onStart: () -> Unit
) {
    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStart() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = routine.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${routine.exercises.size} упражнений • ${routine.description}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldNeon)
                    .clickable { onStart() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Старт",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Старт",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutBanner(
    title: String,
    onResumeClick: () -> Unit
) {
    AthleticCard(
        backgroundColor = DarkSurfaceVariant,
        borderColor = BorderHighlight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onResumeClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EmeraldNeon)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ИДЁТ ТРЕНИРОВКА",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldSubtle)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Продолжить →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )
            }
        }
    }
}

@Composable
private fun CnsReadinessCard(
    score: Int,
    level: String,
    message: String
) {
    val levelColor = when (level.lowercase()) {
        "full throttle" -> EmeraldNeon
        "moderate" -> AmberWarm
        else -> CrimsonRed
    }

    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "CNS",
                        tint = EmeraldNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ИНДЕКС ГОТОВНОСТИ ЦНС",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(levelColor.copy(alpha = 0.15f))
                        .border(width = 1.dp, color = levelColor.copy(alpha = 0.35f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = level.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$score",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "/100",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = levelColor,
                trackColor = DarkSurfaceHigh,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}
