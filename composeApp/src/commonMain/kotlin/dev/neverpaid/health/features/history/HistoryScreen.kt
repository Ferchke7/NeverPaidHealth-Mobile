package dev.neverpaid.health.features.history

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Workout

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onNavigateToActiveWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            HistoryUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
            is HistoryUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        HistoryScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onAction: (HistoryUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current

    if (state.isLoading && state.workouts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.historyTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "${state.totalWorkoutsLogged} завершённых тренировок в архиве",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item(key = "heatmap_card") {
            ConsistencyHeatmapCard(densityMatrix = state.heatmapDensityMatrix)
        }

        item(key = "log_header") {
            Text(
                text = strings.historyLoggedSessions,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        if (state.workouts.isEmpty()) {
            item(key = "empty_history") {
                AthleticCard(backgroundColor = DarkSurface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("История тренировок пока пуста", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Завершите первую тренировку, чтобы видеть полный архив подходов и весов.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(
                items = state.workouts,
                key = { it.id }
            ) { workout ->
                val isExpanded = state.expandedWorkoutId == workout.id
                HistoryWorkoutCard(
                    workout = workout,
                    isExpanded = isExpanded,
                    onToggleExpand = { onAction(HistoryUiAction.OnToggleWorkoutExpand(workout.id)) },
                    onRepeatWorkout = { onAction(HistoryUiAction.OnRepeatWorkout(workout)) },
                    onDeleteWorkout = { onAction(HistoryUiAction.OnDeleteWorkout(workout.id)) }
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConsistencyHeatmapCard(densityMatrix: List<Int>) {
    val strings = LocalDudaStrings.current

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.historyDensity,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = strings.historyLess, fontSize = 10.sp, color = TextMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(DarkSurfaceHigh))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(EmeraldSubtle))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(EmeraldDark))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(EmeraldNeon))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.historyMore, fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 12) {
                            val index = col * 7 + row
                            val intensity = if (index < densityMatrix.size) densityMatrix[index] else 0
                            val cellColor = when (intensity) {
                                3 -> EmeraldNeon
                                2 -> EmeraldDark
                                1 -> EmeraldDark.copy(alpha = 0.4f)
                                else -> DarkSurfaceHigh
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryWorkoutCard(
    workout: Workout,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRepeatWorkout: () -> Unit,
    onDeleteWorkout: () -> Unit
) {
    val dateStr = PlatformBridge.formatDate(workout.startTimeEpochMs, "EEE, dd MMM yyyy • HH:mm")
    val durationMin = (workout.durationSeconds / 60).coerceAtLeast(1)

    AthleticCard(
        modifier = Modifier.clickable { onToggleExpand() },
        backgroundColor = DarkSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCounter(
                    title = "Тоннаж",
                    value = StrengthCalculator.formatVolume(workout.totalVolumeKg),
                    unit = "кг",
                    accentColor = EmeraldNeon
                )
                StatCounter(
                    title = "Упражнения",
                    value = "${workout.exercises.size}",
                    accentColor = IndigoElectric
                )
                StatCounter(
                    title = "Время",
                    value = "$durationMin",
                    unit = "мин",
                    accentColor = TextPrimary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    workout.exercises.forEach { ex ->
                        Text(
                            text = ex.exerciseName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        ex.sets.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Сет ${s.setNumber} (${if (s.isWarmup) "Разминка" else "Рабочий"})",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${s.weightKg.toInt()} кг × ${s.reps} повт.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDeleteWorkout,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить", color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onRepeatWorkout,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldNeon,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Повторить тренировку", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
