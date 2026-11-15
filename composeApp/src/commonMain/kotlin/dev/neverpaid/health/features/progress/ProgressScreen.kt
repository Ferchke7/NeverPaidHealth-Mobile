package dev.neverpaid.health.features.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.BorderDivider
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.InteractiveAnatomyHeatmap
import dev.neverpaid.health.core.designsystem.ObsidianCard
import dev.neverpaid.health.core.designsystem.ObsidianDivider
import dev.neverpaid.health.core.designsystem.ObsidianLabeledField
import dev.neverpaid.health.core.designsystem.ObsidianLoadingCard
import dev.neverpaid.health.core.designsystem.ObsidianOutlineButton
import dev.neverpaid.health.core.designsystem.ObsidianPageHeader
import dev.neverpaid.health.core.designsystem.ObsidianPillTab
import dev.neverpaid.health.core.designsystem.ObsidianProgressBar
import dev.neverpaid.health.core.designsystem.ObsidianInnerWell
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.PersonalRecord
import dev.neverpaid.health.domain.model.Workout
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round

@Composable
fun ProgressRoute(
    viewModel: ProgressViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToBody: () -> Unit = {},
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
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToBody = onNavigateToBody,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

private data class ChartSamplePoint(
    val dateLabel: String,
    val maxWeightKg: Double,
    val bestE1rmKg: Double,
    val totalVolumeKg: Double
)

private data class EnrichedPrItem(
    val pr: PersonalRecord,
    val muscleBucket: String
)

private data class TonnageBarSample(
    val label: String,
    val subLabel: String,
    val volumeKg: Double
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onAction: (ProgressUiAction) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToBody: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current

    if (state.isLoading && state.totalWorkouts == 0 && state.workoutHistory.isEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                ObsidianPageHeader(
                    title = strings.progressTitle,
                    subtitle = "Динамика силы и персональные рекорды"
                )
            }
            item(key = "loading") {
                ObsidianLoadingCard(
                    title = strings.progressTitle,
                    hint = strings.stateLoadingHint
                )
            }
        }
        return
    }

    val topTabs = listOf(
        "Прогрессия & PR",
        "Баланс & Тоннаж",
        "1RM & Стандарты"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "page_header", contentType = "header") {
            val headerTitle = if (state.selectedTab == 0) strings.progressTitle else "Баланс и 1RM"
            val headerSubtitle = if (state.selectedTab == 0) {
                "Динамика силы и персональные рекорды"
            } else {
                "Нагрузка, тоннаж и силовые стандарты"
            }
            ObsidianPageHeader(
                title = headerTitle,
                subtitle = headerSubtitle
            )
        }

        item(key = "top_tabs", contentType = "tabs") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topTabs.forEachIndexed { idx, label ->
                    ObsidianPillTab(
                        text = label,
                        selected = state.selectedTab == idx,
                        onClick = { onAction(ProgressUiAction.OnSelectTab(idx)) }
                    )
                }
            }
        }

        when (state.selectedTab) {
            0 -> {
                item(key = "kpi_grid", contentType = "kpi") {
                    ProgressKpiGrid(state = state)
                }
                item(key = "progression_card", contentType = "progression") {
                    ExerciseProgressionCard(
                        workoutHistory = state.workoutHistory,
                        personalRecords = state.personalRecords
                    )
                }
                item(key = "records_card", contentType = "records") {
                    PersonalRecordsSectionCard(
                        personalRecords = state.personalRecords,
                        workoutHistory = state.workoutHistory
                    )
                }
                item(key = "footer_links", contentType = "links") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianOutlineButton(
                            text = "История тренировок",
                            onClick = onNavigateToHistory
                        )
                        ObsidianOutlineButton(
                            text = "Метрики тела",
                            onClick = onNavigateToBody
                        )
                    }
                }
            }

            1 -> {
                item(key = "weekly_balance_card", contentType = "balance") {
                    WeeklyBalanceCard(
                        muscleHypertrophySets = state.muscleHypertrophySets,
                        workoutHistory = state.workoutHistory
                    )
                }
                item(key = "tonnage_card", contentType = "tonnage") {
                    TonnageTrendCard(
                        weeklyVolumeBars = state.weeklyVolumeBars,
                        workoutHistory = state.workoutHistory
                    )
                }
                item(key = "footer_links_balance", contentType = "links") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianOutlineButton(
                            text = "История тренировок",
                            onClick = onNavigateToHistory
                        )
                        ObsidianOutlineButton(
                            text = "1RM & Стандарты",
                            onClick = { onAction(ProgressUiAction.OnSelectTab(2)) }
                        )
                    }
                }
            }

            else -> {
                item(key = "calculator_standards_card", contentType = "1rm") {
                    OneRepMaxAndStandardsCard(personalRecords = state.personalRecords)
                }
                item(key = "footer_links_1rm", contentType = "links") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianOutlineButton(
                            text = "История тренировок",
                            onClick = onNavigateToHistory
                        )
                        ObsidianOutlineButton(
                            text = "Прогрессия & PR",
                            onClick = { onAction(ProgressUiAction.OnSelectTab(0)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressKpiGrid(state: ProgressUiState) {
    val totalHours = remember(state.workoutHistory) {
        val totalSec = state.workoutHistory.sumOf { it.durationSeconds.coerceAtLeast(0L) }
        val hrs = totalSec / 3600.0
        val rounded = round(hrs * 10.0) / 10.0
        if (rounded % 1.0 == 0.0) "${rounded.toInt()} ч" else "$rounded ч"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiStatCard(
                value = "${state.totalWorkouts}",
                label = "Тренировки",
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                value = formatTonsCompact(state.totalVolumeKg),
                label = "Общий тоннаж",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiStatCard(
                value = "${state.personalRecords.size}",
                label = "Личные рекорды",
                modifier = Modifier.weight(1f)
            )
            KpiStatCard(
                value = totalHours,
                label = "В зале",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    ObsidianCard(modifier = modifier) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseProgressionCard(
    workoutHistory: List<Workout>,
    personalRecords: List<PersonalRecord>
) {
    val exerciseNames = remember(workoutHistory, personalRecords) {
        val fromHistory = workoutHistory.flatMap { it.exercises }.map { it.exerciseName }.filter { it.isNotBlank() }
        val fromPrs = personalRecords.map { it.exerciseName }.filter { it.isNotBlank() }
        (fromPrs + fromHistory).distinct().ifEmpty { listOf("Жим штанги лёжа") }
    }

    var selectedExerciseName by remember(exerciseNames) {
        mutableStateOf(exerciseNames.first())
    }
    var showExerciseSelectorSheet by remember { mutableStateOf(false) }
    var metricIndex by remember { mutableIntStateOf(0) } // 0: weight, 1: e1rm, 2: volume
    var rangeIndex by remember { mutableIntStateOf(1) } // 0: 1m, 1: 3m, 2: All

    val samples = remember(workoutHistory, selectedExerciseName, rangeIndex) {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = PlatformBridge.currentTimeMillis()
        val cutoffMs = when (rangeIndex) {
            0 -> nowMs - 30L * 86_400_000L
            1 -> nowMs - 90L * 86_400_000L
            else -> 0L
        }

        workoutHistory
            .filter { it.isFinished && it.startTimeEpochMs >= cutoffMs }
            .sortedBy { it.startTimeEpochMs }
            .mapNotNull { w ->
                val matchingEx = w.exercises.filter {
                    it.exerciseName.equals(selectedExerciseName, ignoreCase = true)
                }
                if (matchingEx.isEmpty()) return@mapNotNull null

                val sets = matchingEx.flatMap { it.sets }.filter { it.weightKg > 0 && it.reps > 0 }
                if (sets.isEmpty()) return@mapNotNull null

                val maxW = sets.maxOf { it.weightKg }
                val best1RM = sets.maxOf { it.weightKg * (1.0 + it.reps / 30.0) }
                val vol = sets.sumOf { it.weightKg * it.reps }
                val date = Instant.fromEpochMilliseconds(w.startTimeEpochMs).toLocalDateTime(tz).date
                val label = "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}"

                ChartSamplePoint(
                    dateLabel = label,
                    maxWeightKg = maxW,
                    bestE1rmKg = round(best1RM * 10.0) / 10.0,
                    totalVolumeKg = round(vol)
                )
            }
    }

    val values = samples.map { s ->
        when (metricIndex) {
            0 -> s.maxWeightKg
            1 -> s.bestE1rmKg
            else -> s.totalVolumeKg
        }
    }
    val currentMax = values.maxOrNull() ?: 0.0
    val firstVal = values.firstOrNull() ?: 0.0
    val deltaPct = if (firstVal > 0.0 && values.size >= 2) {
        round(((currentMax - firstVal) / firstVal) * 100.0).toInt()
    } else 0

    ObsidianCard {
        Text(
            text = "Прогрессия упражнения",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        ObsidianOutlineButton(
            text = selectedExerciseName,
            onClick = { showExerciseSelectorSheet = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Вес снаряда", "Расчётный 1RM", "Объём").forEachIndexed { idx, label ->
                ObsidianPillTab(
                    text = label,
                    selected = metricIndex == idx,
                    onClick = { metricIndex = idx }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1 мес", "3 мес", "Все").forEachIndexed { idx, label ->
                ObsidianPillTab(
                    text = label,
                    selected = rangeIndex == idx,
                    onClick = { rangeIndex = idx }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (values.isEmpty()) {
            ObsidianInnerWell {
                Text(
                    text = "Нет данных за выбранный период",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Выполните хотя бы одну тренировку с «$selectedExerciseName», чтобы построить кривую прогрессии.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        } else {
            ObsidianLineChart(
                labels = samples.map { it.dateLabel },
                values = values,
                unitLabel = "кг"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${formatCleanKg(currentMax)} кг",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Текущий рекорд",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val sign = if (deltaPct >= 0) "+" else ""
                    Text(
                        text = "$sign$deltaPct%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldNeon
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "От начала периода",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${formatCleanKg(firstVal)} кг",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Стартовый вес",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }

    if (showExerciseSelectorSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showExerciseSelectorSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Выбрать упражнение",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                exerciseNames.forEachIndexed { idx, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedExerciseName = name
                                showExerciseSelectorSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = if (name == selectedExerciseName) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (name == selectedExerciseName) EmeraldNeon else TextPrimary
                        )
                    }
                    if (idx < exerciseNames.lastIndex) {
                        ObsidianDivider()
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ObsidianLineChart(
    labels: List<String>,
    values: List<Double>,
    unitLabel: String
) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = TextMuted)
    val pointStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = unitLabel,
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val leftPad = 40.dp.toPx()
            val rightPad = 18.dp.toPx()
            val topPad = 22.dp.toPx()
            val bottomPad = 26.dp.toPx()

            val chartW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

            val rawMin = values.minOrNull() ?: 0.0
            val rawMax = values.maxOrNull() ?: 100.0
            val span = (rawMax - rawMin).coerceAtLeast(10.0)
            val minVal = (rawMin - span * 0.15).coerceAtLeast(0.0)
            val maxVal = rawMax + span * 0.15
            val valRange = (maxVal - minVal).coerceAtLeast(1.0)

            // 3 horizontal dashed lines
            val steps = 2
            for (i in 0..steps) {
                val frac = i / steps.toFloat()
                val y = topPad + chartH * (1f - frac)
                drawLine(
                    color = BorderDivider,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartW, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                val tickVal = minVal + valRange * frac
                val labelText = formatCleanKg(tickVal)
                val measured = textMeasurer.measure(labelText, axisStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(0f, y - measured.size.height / 2f)
                )
            }

            val coords = values.mapIndexed { idx, v ->
                val x = if (values.size <= 1) {
                    leftPad + chartW / 2f
                } else {
                    leftPad + (idx.toFloat() / (values.size - 1).toFloat()) * chartW
                }
                val normY = ((v - minVal) / valRange).toFloat().coerceIn(0f, 1f)
                val y = topPad + chartH * (1f - normY)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = EmeraldNeon,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            val maxLabelPoints = coords.size.coerceAtMost(6)
            val step = if (coords.size > 6) (coords.size / 5).coerceAtLeast(1) else 1
            coords.forEachIndexed { idx, pt ->
                drawCircle(
                    color = EmeraldNeon,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                if (idx % step == 0 || idx == coords.lastIndex) {
                    val vLabel = formatCleanKg(values[idx])
                    val measuredV = textMeasurer.measure(vLabel, pointStyle)
                    drawText(
                        textLayoutResult = measuredV,
                        topLeft = Offset(
                            (pt.x - measuredV.size.width / 2f).coerceIn(leftPad, size.width - measuredV.size.width),
                            (pt.y - measuredV.size.height - 6.dp.toPx()).coerceAtLeast(0f)
                        )
                    )
                    if (idx < labels.size) {
                        val dMeasured = textMeasurer.measure(labels[idx], axisStyle)
                        drawText(
                            textLayoutResult = dMeasured,
                            topLeft = Offset(
                                (pt.x - dMeasured.size.width / 2f).coerceIn(leftPad, size.width - dMeasured.size.width),
                                topPad + chartH + 6.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalRecordsSectionCard(
    personalRecords: List<PersonalRecord>,
    workoutHistory: List<Workout>
) {
    val enriched = remember(personalRecords, workoutHistory) {
        val muscleLookup = mutableMapOf<String, String>()
        workoutHistory.forEach { w ->
            w.exercises.forEach { ex ->
                muscleLookup[ex.exerciseName.lowercase()] = mapMuscleBucket(ex.primaryMuscleGroup)
            }
        }
        personalRecords.map { pr ->
            EnrichedPrItem(
                pr = pr,
                muscleBucket = muscleLookup[pr.exerciseName.lowercase()] ?: "Другое"
            )
        }
    }

    val filterTabs = listOf("Все", "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс")
    var selectedFilter by remember { mutableStateOf("Все") }

    val filtered = remember(enriched, selectedFilter) {
        if (selectedFilter == "Все") enriched
        else enriched.filter { it.muscleBucket == selectedFilter }
    }

    ObsidianCard {
        Text(
            text = "Личные рекорды",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${filtered.size} упражнений · лучшие значения по каждому показателю",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterTabs.forEach { tab ->
                ObsidianPillTab(
                    text = tab,
                    selected = selectedFilter == tab,
                    onClick = { selectedFilter = tab }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        ObsidianDivider()

        if (filtered.isEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Нет зафиксированных рекордов в этой категории.",
                fontSize = 14.sp,
                color = TextMuted
            )
        } else {
            filtered.forEachIndexed { idx, item ->
                val pr = item.pr
                val maxSetVol = if (pr.maxVolumeSetKg > 0.0) pr.maxVolumeSetKg else pr.weightKg * pr.reps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = pr.exerciseName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Макс. вес", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${formatCleanKg(pr.weightKg)} кг",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Расчётный 1RM", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${formatCleanKg(pr.e1rmKg)} кг",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Макс. объём сета", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${formatCleanKg(maxSetVol)} кг",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Макс. повторений", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${pr.reps} повт.",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
                if (idx < filtered.lastIndex) {
                    ObsidianDivider()
                }
            }
        }
    }
}

@Composable
private fun WeeklyBalanceCard(
    muscleHypertrophySets: Map<String, Int>,
    workoutHistory: List<Workout>
) {
    var showAnatomyMap by remember { mutableStateOf(false) }

    val groups = remember(muscleHypertrophySets, workoutHistory) {
        val legSets = (muscleHypertrophySets["Квадрицепсы"] ?: 0) +
            (muscleHypertrophySets["Бицепс бедра"] ?: 0) +
            (muscleHypertrophySets["Ноги"] ?: 0)

        val nowMs = PlatformBridge.currentTimeMillis()
        val weekAgoMs = nowMs - 7L * 86_400_000L
        val absSets = workoutHistory
            .filter { it.startTimeEpochMs >= weekAgoMs }
            .flatMap { it.exercises }
            .filter { mapMuscleBucket(it.primaryMuscleGroup) == "Пресс" }
            .sumOf { ex -> ex.sets.count { it.isCompleted && it.type != "warmup" } }

        listOf(
            "Грудь" to (muscleHypertrophySets["Грудь"] ?: 0),
            "Спина" to (muscleHypertrophySets["Спина"] ?: 0),
            "Ноги" to legSets,
            "Плечи" to (muscleHypertrophySets["Плечи"] ?: 0),
            "Руки" to (muscleHypertrophySets["Руки"] ?: 0),
            "Пресс" to (muscleHypertrophySets["Пресс"] ?: absSets)
        )
    }

    ObsidianCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Недельный баланс",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Текущая неделя · рабочие подходы",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            ObsidianOutlineButton(
                text = if (showAnatomyMap) "Скрыть карту" else "Карта мышц",
                onClick = { showAnatomyMap = !showAnatomyMap }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Ориентир: 12–16 сетов на группу в неделю",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            groups.forEach { (group, sets) ->
                val statusText = when {
                    sets >= 12 -> "Оптимум"
                    sets > 0 -> "Поддержание"
                    else -> "Нет нагрузки"
                }
                val isOptimal = sets >= 12

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "$sets / 12–16 сетов",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOptimal) EmeraldNeon else TextPrimary
                        )
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = if (isOptimal) EmeraldNeon else TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ObsidianProgressBar(
                        progress = (sets / 16f).coerceIn(0f, 1f),
                        color = EmeraldNeon
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Шкала: 0–16 сетов · зелёный столбец — фактический объём",
            fontSize = 12.sp,
            color = TextSecondary
        )

        AnimatedVisibility(visible = showAnatomyMap) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveAnatomyHeatmap(weeklyMuscleVolumes = muscleHypertrophySets)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TonnageTrendCard(
    weeklyVolumeBars: List<Pair<String, Double>>,
    workoutHistory: List<Workout>
) {
    var modeIndex by remember { mutableIntStateOf(1) } // 0: weekly, 1: per workout

    val bars = remember(modeIndex, weeklyVolumeBars, workoutHistory) {
        if (modeIndex == 0) {
            weeklyVolumeBars.map { (label, vol) ->
                TonnageBarSample(
                    label = label,
                    subLabel = "",
                    volumeKg = vol
                )
            }
        } else {
            val tz = TimeZone.currentSystemDefault()
            workoutHistory
                .filter { it.isFinished && it.totalVolume > 0.0 }
                .sortedBy { it.startTimeEpochMs }
                .takeLast(5)
                .map { w ->
                    val d = Instant.fromEpochMilliseconds(w.startTimeEpochMs).toLocalDateTime(tz).date
                    val dLabel = "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthNumber.toString().padStart(2, '0')}"
                    val setsCount = w.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
                        .let { if (it > 0) it else w.completedSetsCount }
                    TonnageBarSample(
                        label = dLabel,
                        subLabel = "${setsCount} с.",
                        volumeKg = w.totalVolume
                    )
                }
        }
    }

    val totalVol = bars.sumOf { it.volumeKg }
    val avgVol = if (bars.isNotEmpty()) totalVol / bars.size else 0.0
    val peakVol = bars.maxOfOrNull { it.volumeKg } ?: 0.0

    ObsidianCard {
        Text(
            text = "Тоннаж",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianPillTab(
                text = "По неделям",
                selected = modeIndex == 0,
                onClick = { modeIndex = 0 }
            )
            ObsidianPillTab(
                text = "По тренировкам",
                selected = modeIndex == 1,
                onClick = { modeIndex = 1 }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTonsCompact(totalVol),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text("Всего", fontSize = 12.sp, color = TextSecondary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTonsCompact(avgVol),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text("В среднем", fontSize = 12.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = formatTonsCompact(peakVol),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text("Пиковая тренировка", fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        if (bars.isEmpty() || bars.all { it.volumeKg <= 0.0 }) {
            ObsidianInnerWell {
                Text(
                    text = "Пока нет данных о тоннаже",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        } else {
            ObsidianTonnageBarChart(samples = bars)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Столбцы — тоннаж · подписи: т · «с.» — сеты",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun ObsidianTonnageBarChart(samples: List<TonnageBarSample>) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = TextMuted)
    val valStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextPrimary)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "кг", fontSize = 11.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            val leftPad = 48.dp.toPx()
            val topPad = 24.dp.toPx()
            val bottomPad = 36.dp.toPx()
            val chartW = (size.width - leftPad - 8.dp.toPx()).coerceAtLeast(10f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(10f)

            val maxVal = (samples.maxOfOrNull { it.volumeKg } ?: 1000.0).coerceAtLeast(100.0)
            val yCap = maxVal * 1.12

            for (i in 0..3) {
                val frac = i / 3f
                val y = topPad + chartH * (1f - frac)
                drawLine(
                    color = BorderDivider,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartW, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                val tick = round(yCap * frac).toInt()
                val m = textMeasurer.measure("$tick", axisStyle)
                drawText(
                    textLayoutResult = m,
                    topLeft = Offset(0f, y - m.size.height / 2f)
                )
            }

            val slotW = chartW / samples.size.coerceAtLeast(1)
            val barW = (slotW * 0.48f).coerceIn(16.dp.toPx(), 42.dp.toPx())

            samples.forEachIndexed { idx, sample ->
                val centerX = leftPad + idx * slotW + slotW / 2f
                val barLeft = centerX - barW / 2f
                val frac = (sample.volumeKg / yCap).toFloat().coerceIn(0.04f, 1f)
                val barH = chartH * frac
                val barTop = topPad + chartH - barH

                drawRoundRect(
                    color = EmeraldNeon,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                val topLabel = formatTonsCompact(sample.volumeKg)
                val topMeasured = textMeasurer.measure(topLabel, valStyle)
                drawText(
                    textLayoutResult = topMeasured,
                    topLeft = Offset(
                        centerX - topMeasured.size.width / 2f,
                        (barTop - topMeasured.size.height - 4.dp.toPx()).coerceAtLeast(0f)
                    )
                )

                val xMeasured = textMeasurer.measure(sample.label, axisStyle)
                drawText(
                    textLayoutResult = xMeasured,
                    topLeft = Offset(centerX - xMeasured.size.width / 2f, topPad + chartH + 4.dp.toPx())
                )

                if (sample.subLabel.isNotBlank()) {
                    val sMeasured = textMeasurer.measure(sample.subLabel, axisStyle)
                    drawText(
                        textLayoutResult = sMeasured,
                        topLeft = Offset(centerX - sMeasured.size.width / 2f, topPad + chartH + 18.dp.toPx())
                    )
                }
            }
        }
    }
}

private data class StrengthExerciseStandard(
    val name: String,
    val thresholdsKg: List<Int>
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OneRepMaxAndStandardsCard(personalRecords: List<PersonalRecord>) {
    val standardsCatalog = remember {
        listOf(
            StrengthExerciseStandard("Жим лёжа", listOf(50, 75, 100, 125, 155)),
            StrengthExerciseStandard("Приседания со штангой", listOf(65, 95, 130, 165, 200)),
            StrengthExerciseStandard("Становая тяга", listOf(80, 115, 155, 195, 235)),
            StrengthExerciseStandard("Жим стоя", listOf(35, 50, 68, 88, 110)),
            StrengthExerciseStandard("Тяга штанги в наклоне", listOf(45, 68, 92, 115, 140))
        )
    }

    var selectedStandard by remember { mutableStateOf(standardsCatalog.first()) }
    var showStandardSheet by remember { mutableStateOf(false) }

    val matchingPr = remember(selectedStandard, personalRecords) {
        personalRecords.firstOrNull {
            it.exerciseName.contains(selectedStandard.name.substringBefore(" "), ignoreCase = true)
        }
    }

    var weightText by remember(matchingPr?.exerciseId) {
        mutableStateOf(
            matchingPr?.let { formatCleanKg(it.weightKg) } ?: "100"
        )
    }
    var repsText by remember(matchingPr?.exerciseId) {
        mutableStateOf(
            matchingPr?.let { it.reps.toString() } ?: "8"
        )
    }
    var useBrzycki by remember { mutableStateOf(true) }

    val wKg = weightText.replace(",", ".").toDoubleOrNull()?.coerceIn(1.0, 600.0) ?: 100.0
    val reps = repsText.toIntOrNull()?.coerceIn(1, 30) ?: 8

    val brzycki1RM = if (reps == 1) wKg else if (reps >= 37) wKg * 2.0 else wKg * 36.0 / (37.0 - reps)
    val epley1RM = if (reps == 1) wKg else wKg * (1.0 + reps / 30.0)
    val primary1RM = round((if (useBrzycki) brzycki1RM else epley1RM) * 10.0) / 10.0
    val compare1RM = round((if (useBrzycki) epley1RM else brzycki1RM) * 10.0) / 10.0

    ObsidianCard {
        Text(
            text = "1RM & Стандарты",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Калькулятор одноповторного максимума",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        ObsidianOutlineButton(
            text = selectedStandard.name,
            onClick = { showStandardSheet = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ObsidianLabeledField(
                label = "Вес снаряда, кг",
                value = weightText,
                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            ObsidianLabeledField(
                label = "Повторения",
                value = repsText,
                onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianPillTab(
                text = "Brzycki",
                selected = useBrzycki,
                onClick = { useBrzycki = true }
            )
            ObsidianPillTab(
                text = "Epley",
                selected = !useBrzycki,
                onClick = { useBrzycki = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${formatCleanKg(primary1RM)} кг",
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = EmeraldNeon
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (useBrzycki) {
                "Brzycki · ${formatCleanKg(wKg)} × 36 / (37 − $reps)"
            } else {
                "Epley · ${formatCleanKg(wKg)} × (1 + $reps / 30)"
            },
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (useBrzycki) {
                "Для сравнения: Epley ${formatCleanKg(compare1RM)} кг"
            } else {
                "Для сравнения: Brzycki ${formatCleanKg(compare1RM)} кг"
            },
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Рабочие проценты",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(50, 60, 70, 80, 90).forEach { pct ->
                val targetKg = round((primary1RM * (pct / 100.0)) * 10.0) / 10.0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "$pct% · ${formatCleanKg(targetKg)} кг",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Силовые стандарты",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Шкала уровней для выбранного упражнения",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        val levels = listOf("Beginner", "Novice", "Intermediate", "Advanced", "Elite")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (rowIdx in levels.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StrengthStandardCell(
                        numberLabel = "0${rowIdx + 1}",
                        levelName = levels[rowIdx],
                        thresholdKg = selectedStandard.thresholdsKg[rowIdx],
                        achieved = primary1RM >= selectedStandard.thresholdsKg[rowIdx],
                        modifier = Modifier.weight(1f)
                    )
                    if (rowIdx + 1 < levels.size) {
                        StrengthStandardCell(
                            numberLabel = "0${rowIdx + 2}",
                            levelName = levels[rowIdx + 1],
                            thresholdKg = selectedStandard.thresholdsKg[rowIdx + 1],
                            achieved = primary1RM >= selectedStandard.thresholdsKg[rowIdx + 1],
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Пороговые веса и текущий уровень — по существующим таблицам",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }

    if (showStandardSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStandardSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Выбрать стандарт",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                standardsCatalog.forEachIndexed { idx, std ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStandard = std
                                showStandardSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = std.name,
                            fontSize = 15.sp,
                            color = if (std.name == selectedStandard.name) EmeraldNeon else TextPrimary
                        )
                    }
                    if (idx < standardsCatalog.lastIndex) ObsidianDivider()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StrengthStandardCell(
    numberLabel: String,
    levelName: String,
    thresholdKg: Int,
    achieved: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = numberLabel,
            fontSize = 11.sp,
            color = if (achieved) EmeraldNeon else TextMuted
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = levelName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (achieved) EmeraldNeon else TextPrimary
            )
            Text(
                text = "$thresholdKg кг",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (achieved) EmeraldNeon else DarkSurfaceHigh)
        )
    }
}

private fun mapMuscleBucket(raw: String): String {
    return when (raw.lowercase()) {
        "chest", "грудь" -> "Грудь"
        "back", "lats", "traps", "спина" -> "Спина"
        "quads", "hamstrings", "glutes", "calves", "legs", "ноги" -> "Ноги"
        "shoulders", "delts", "плечи" -> "Плечи"
        "biceps", "triceps", "forearms", "arms", "руки" -> "Руки"
        "abs", "core", "obliques", "пресс" -> "Пресс"
        else -> "Другое"
    }
}

private fun formatTonsCompact(kg: Double): String {
    return if (kg >= 1000.0) {
        val tons = round((kg / 1000.0) * 10.0) / 10.0
        "${tons} т"
    } else {
        "${round(kg).toInt()} кг"
    }
}

private fun formatCleanKg(value: Double): String {
    val r = round(value * 10.0) / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}
