package dev.neverpaid.health.features.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.designsystem.ConsistencyHeatmap84Day
import dev.neverpaid.health.core.designsystem.HeatmapDayEntry
import dev.neverpaid.health.core.designsystem.InteractiveAnatomyHeatmap
import dev.neverpaid.health.core.designsystem.InteractiveVolumeChart
import dev.neverpaid.health.core.designsystem.VolumeDataPoint
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.PersonalRecord

@Composable
fun ProgressRoute(
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        ProgressScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onAction: (ProgressUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    val tabs = listOf(
        strings.progressTabVolume,
        strings.progressTabMuscles,
        strings.progressTabPRs
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.progressTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = strings.progressSubtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Single Choice Segmented Button: One Job per Tab!
        item(key = "tabs_row") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = state.selectedTab == index
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onAction(ProgressUiAction.OnSelectTab(index)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = EmeraldNeon,
                            activeContentColor = DarkBackground,
                            inactiveContainerColor = DarkSurfaceVariant,
                            inactiveContentColor = TextSecondary
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        when (state.selectedTab) {
            // TAB 0: Volume Progression & 84-Day Athletic Discipline Matrix
            0 -> {
                val hasAnyVolume = state.weeklyVolumeBars.any { it.second > 0.0 } || state.totalWorkouts > 0

                if (hasAnyVolume) {
                    item(key = "interactive_volume_chart") {
                        val volumePoints = remember(state.weeklyVolumeBars) {
                            state.weeklyVolumeBars.map { (dateLabel, vol) ->
                                VolumeDataPoint(
                                    date = dateLabel,
                                    volumeKg = vol,
                                    sessionTitle = "Недельный объём"
                                )
                            }
                        }
                        InteractiveVolumeChart(dataPoints = volumePoints)
                    }

                    item(key = "consistency_heatmap_84") {
                        val heatmapData = remember(state.workoutHistory) {
                            val dayMs = 86_400_000L
                            val now = PlatformBridge.currentTimeMillis()
                            (83 downTo 0).map { daysAgo ->
                                val startOfDay = now - ((daysAgo + 1) * dayMs)
                                val endOfDay = now - (daysAgo * dayMs)
                                val dayWorkouts = state.workoutHistory.filter {
                                    it.startTimeEpochMs in startOfDay..endOfDay
                                }
                                val dayVol = dayWorkouts.sumOf { it.totalVolumeKg }
                                val tier = when {
                                    dayVol > 8000.0 -> 4
                                    dayVol > 5000.0 -> 3
                                    dayVol > 2000.0 -> 2
                                    dayVol > 0.0 -> 1
                                    else -> 0
                                }
                                HeatmapDayEntry(
                                    date = "D-$daysAgo",
                                    tier = tier,
                                    volumeKg = dayVol,
                                    workoutCount = dayWorkouts.size
                                )
                            }
                        }
                        ConsistencyHeatmap84Day(days = heatmapData)
                    }

                    item(key = "volume_metrics_summary") {
                        VolumeSummaryMetricsCard(
                            totalVolumeKg = state.totalVolumeKg,
                            totalWorkouts = state.totalWorkouts,
                            totalReps = state.totalReps,
                            currentStreakDays = state.currentStreakDays
                        )
                    }
                } else {
                    item(key = "empty_volume_state") {
                        AthleticCard(backgroundColor = DarkSurface) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = EmeraldNeon,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Нет завершённых тренировок",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Завершите тренировку во вкладке «Тренировка», чтобы увидеть реальный недельный тоннаж, прогресс нагрузок и 84-дневную тепловую карту дисциплины.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // TAB 1: 2D Vector Muscular Anatomy Heatmap & Hypertrophy Sets
            1 -> {
                item(key = "interactive_anatomy_heatmap") {
                    InteractiveAnatomyHeatmap(weeklyMuscleVolumes = state.muscleHypertrophySets)
                }

                item(key = "muscles_distribution") {
                    HypertrophyDistributionCard(muscleSets = state.muscleHypertrophySets)
                }
            }

            // TAB 2: Personal Records Hall of Fame
            2 -> {
                item(key = "prs_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.progressHallOfFame,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (state.personalRecords.isEmpty()) {
                    item(key = "prs_empty") {
                        AthleticCard(backgroundColor = DarkSurfaceVariant) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Завершите тренировку с максимальными весами, чтобы зафиксировать первые рекорды!",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = state.personalRecords,
                        key = { it.exerciseName }
                    ) { pr ->
                        PersonalRecordItemCard(pr = pr)
                    }
                }
            }
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VolumeSummaryMetricsCard(
    totalVolumeKg: Double,
    totalWorkouts: Int,
    totalReps: Int,
    currentStreakDays: Int
) {
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
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.progressWeeklyTonnage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "$totalWorkouts сессий",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Общий тоннаж",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${StrengthCalculator.formatVolume(totalVolumeKg)} кг",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Повторений",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalReps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Дисциплина",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currentStreakDays дн.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarm
                    )
                }
            }
        }
    }
}

@Composable
private fun HypertrophyDistributionCard(muscleSets: Map<String, Int>) {
    val strings = LocalDudaStrings.current

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.progressHypertrophySets,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (muscleSets.values.all { it == 0 }) {
                Text(
                    text = "Нет завершённых подходов за последние 7 дней. Выполните тренировку, чтобы наполнить мышечные объёмы.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            } else {
                muscleSets.forEach { (muscle, sets) ->
                val progress = (sets / 20f).coerceIn(0f, 1f)
                val statusColor = when {
                    sets >= 12 -> EmeraldNeon
                    sets >= 8 -> IndigoElectric
                    else -> AmberWarm
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = muscle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.width(96.dp)
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = DarkSurfaceHigh
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$sets сеты",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.width(52.dp)
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun PersonalRecordItemCard(pr: PersonalRecord) {
    val strings = LocalDudaStrings.current

    AthleticCard(backgroundColor = DarkSurfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AmberWarm.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = pr.exerciseName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${pr.weightKg} кг × ${pr.reps} повт.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = strings.progressE1rm,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "${pr.e1rmKg} кг",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }
        }
    }
}
