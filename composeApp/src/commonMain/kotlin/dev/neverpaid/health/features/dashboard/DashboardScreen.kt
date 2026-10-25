package dev.neverpaid.health.features.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.domain.model.Routine
import kotlinx.coroutines.launch

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onNavigateToActiveWorkout: () -> Unit,
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
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(12.dp))
            DashboardHeader(streakDays = state.currentStreakDays)
        }

        if (state.hasActiveWorkout) {
            item(key = "active_workout_banner") {
                ActiveWorkoutBanner(
                    title = state.activeWorkoutTitle,
                    onResumeClick = { onAction(DashboardUiAction.OnResumeWorkoutClick) }
                )
            }
        }

        item(key = "cns_readiness_card") {
            CnsReadinessCard(
                score = state.cnsScore,
                level = state.readinessLevel,
                message = state.statusMessage
            )
        }

        item(key = "quick_start_button") {
            PrimaryAthleticButton(
                text = "Start Empty Workout",
                onClick = { onAction(DashboardUiAction.OnStartEmptyWorkoutClick) },
                icon = Icons.Default.Add
            )
        }

        item(key = "stats_card") {
            AthleticCard(backgroundColor = DarkSurfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCounter(
                        title = "Completed",
                        value = "${state.totalWorkouts}",
                        unit = "sessions",
                        accentColor = EmeraldNeon
                    )
                    StatCounter(
                        title = "Volume",
                        value = dev.neverpaid.health.core.math.StrengthCalculator.formatVolume(state.totalVolumeKg),
                        unit = "kg",
                        accentColor = IndigoElectric
                    )
                    StatCounter(
                        title = "Current Streak",
                        value = "${state.currentStreakDays}",
                        unit = "days",
                        accentColor = AmberWarm
                    )
                }
            }
        }

        item(key = "routines_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKOUT ROUTINES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        item(key = "routines_list") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = state.recentRoutines,
                    key = { it.id }
                ) { routine ->
                    RoutineCard(
                        routine = routine,
                        onClick = { onAction(DashboardUiAction.OnSelectRoutineClick(routine)) }
                    )
                }
            }
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHeader(streakDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "NEVERPAIDHEALTH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldNeon,
                letterSpacing = 2.sp
            )
            Text(
                text = "Athlete Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streak",
                    tint = AmberWarm,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$streakDays Days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
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
        borderColor = EmeraldDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "WORKOUT IN PROGRESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title.ifBlank { "Active Workout" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldNeon)
                    .clickable { onResumeClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Resume",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
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
    val levelColor = when {
        score >= 80 -> EmeraldNeon
        score >= 60 -> AmberWarm
        else -> CrimsonRed
    }

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CNS READINESS SCORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(levelColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = level.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$score",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "/100",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 2.dp, top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    onClick: () -> Unit
) {
    AthleticCard(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        backgroundColor = DarkSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndigoSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = IndigoElectric,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = routine.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${routine.exercises.size} exercises",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start Routine →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoElectric
                )
            }
        }
    }
}
