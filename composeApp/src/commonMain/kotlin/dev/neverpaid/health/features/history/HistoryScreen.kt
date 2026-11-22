package dev.neverpaid.health.features.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.ExerciseThumbnail
import dev.neverpaid.health.core.designsystem.ExerciseThumbnailSize
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymStatTile
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Workout
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onAction: (HistoryUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val clipboard = LocalClipboardManager.current
    val tz = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }

    val workoutsByDate = remember(state.workouts) {
        state.workouts.groupBy { workout ->
            workoutLocalDate(workout, tz)
        }
    }

    val initialMonth = remember(state.workouts) {
        val latest = state.workouts.maxByOrNull { it.startTimeEpochMs }
        val d = latest?.let { workoutLocalDate(it, tz) } ?: today
        LocalDate(d.year, d.monthNumber, 1)
    }

    var displayedMonth by remember(initialMonth) { mutableStateOf(initialMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var visibleLimit by remember { mutableIntStateOf(6) }
    var showExportSheet by remember { mutableStateOf(false) }
    var workoutPendingDelete by remember { mutableStateOf<Workout?>(null) }

    val monthWorkouts = remember(state.workouts, displayedMonth) {
        state.workouts.filter { w ->
            val d = workoutLocalDate(w, tz)
            d != null && d.year == displayedMonth.year && d.monthNumber == displayedMonth.monthNumber
        }.sortedByDescending { it.startTimeEpochMs }
    }

    val monthTotalVolumeKg = remember(monthWorkouts) {
        monthWorkouts.sumOf { it.totalVolume }
    }

    val shownWorkouts = monthWorkouts.take(visibleLimit)
    val monthHeaderLabel = formatMonthYearRu(displayedMonth.year, displayedMonth.monthNumber)

    if (state.isLoading && state.workouts.isEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "header") {
                GymPageHeader(
                    title = strings.historyTitle,
                    subtitle = "Журнал тренировок и регулярность"
                )
            }
            item(key = "loading") {
                GymLoadingCard(
                    title = strings.historyTitle,
                    hint = strings.stateLoadingHint
                )
            }
        }
        return
    }

    val (monthVolValue, monthVolUnit) = remember(monthTotalVolumeKg) {
        if (monthTotalVolumeKg >= 1000.0) {
            val tons = round((monthTotalVolumeKg / 1000.0) * 10.0) / 10.0
            val num = if (tons % 1.0 == 0.0) "${tons.toInt()}" else tons.toString().replace('.', ',')
            num to "т"
        } else {
            "${round(monthTotalVolumeKg).toInt()}" to "кг"
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header", contentType = "header") {
            RoyalHeroCard(bottomExtraPadding = 20.dp) {
                Text(
                    text = strings.historyTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.7).sp,
                    color = InkWhite
                )
                Text(
                    text = "$monthHeaderLabel · ${monthWorkouts.size} / $monthVolValue $monthVolUnit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HeroMuted,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }

        // Monthly KPI Grid
        item(key = "kpi_row", contentType = "kpi") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GymStatTile(
                    label = "Тренировок за месяц",
                    value = "${monthWorkouts.size}",
                    modifier = Modifier.weight(1f)
                )
                GymStatTile(
                    label = "Объём за месяц",
                    value = monthVolValue,
                    unit = monthVolUnit,
                    valueColor = AccentLime,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Monthly Calendar Card
        item(key = "calendar_card", contentType = "calendar") {
            HistoryMonthCalendarCard(
                monthStart = displayedMonth,
                today = today,
                selectedDate = selectedDate,
                workoutDates = workoutsByDate.keys.filterNotNull().toSet(),
                onPrevMonth = {
                    displayedMonth = displayedMonth.minus(1, DateTimeUnit.MONTH)
                    selectedDate = null
                    visibleLimit = 6
                },
                onToday = {
                    displayedMonth = LocalDate(today.year, today.monthNumber, 1)
                    selectedDate = today
                    visibleLimit = 6
                },
                onNextMonth = {
                    displayedMonth = displayedMonth.plus(1, DateTimeUnit.MONTH)
                    selectedDate = null
                    visibleLimit = 6
                },
                onSelectDate = { date ->
                    selectedDate = if (selectedDate == date) null else date
                    val workoutOnDate = workoutsByDate[date]?.firstOrNull()
                    if (workoutOnDate != null && state.expandedWorkoutId != workoutOnDate.id) {
                        onAction(HistoryUiAction.OnToggleWorkoutExpand(workoutOnDate.id))
                    }
                }
            )
        }

        // Section Header for Workouts
        item(key = "sessions_section_header", contentType = "section_header") {
            GymSectionTitle(
                title = "Тренировки · $monthHeaderLabel",
                actionLabel = "Экспорт CSV",
                onAction = { showExportSheet = true }
            )
        }

        if (state.workouts.isEmpty()) {
            item(key = "empty_all", contentType = "empty") {
                GymCard {
                    Text(
                        text = "История тренировок пока пуста",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Завершите первую тренировку, чтобы видеть календарь, сеты и общий тоннаж.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        } else if (monthWorkouts.isEmpty()) {
            item(key = "empty_month", contentType = "empty") {
                GymCard {
                    Text(
                        text = "Нет тренировок за $monthHeaderLabel",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Всего в архиве: ${state.workouts.size}. Переключите месяц или перейдите к последней записи.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    GymOutlineButton(
                        text = "К последней тренировке",
                        onClick = {
                            val latest = state.workouts.maxByOrNull { it.startTimeEpochMs }
                            val d = latest?.let { workoutLocalDate(it, tz) }
                            if (d != null) {
                                displayedMonth = LocalDate(d.year, d.monthNumber, 1)
                                selectedDate = d
                            }
                        }
                    )
                }
            }
        } else {
            itemsIndexed(
                items = shownWorkouts,
                key = { idx, it -> "${it.id}_$idx" },
                contentType = { _, _ -> "workout_card" }
            ) { _, workout ->
                CompletedWorkoutCard(
                    workout = workout,
                    tz = tz,
                    isExpanded = state.expandedWorkoutId == workout.id,
                    deleteLabel = strings.delete,
                    onToggleExpand = { onAction(HistoryUiAction.OnToggleWorkoutExpand(workout.id)) },
                    onRepeat = { onAction(HistoryUiAction.OnRepeatWorkout(workout)) },
                    onShare = {
                        val shareText = formatWorkoutShareSummary(workout, tz)
                        clipboard.setText(AnnotatedString(shareText))
                        PlatformBridge.showToast("Тренировка скопирована")
                    },
                    onDelete = { workoutPendingDelete = workout }
                )
            }

            if (monthWorkouts.size > shownWorkouts.size) {
                item(key = "load_more_btn", contentType = "load_more") {
                    GymOutlineButton(
                        text = "Показать ещё (${monthWorkouts.size - shownWorkouts.size})",
                        onClick = { visibleLimit += 6 },
                        fillWidth = true
                    )
                }
            }
        }

        // 52-week Consistency Heatmap
        item(key = "yearly_heatmap_card", contentType = "heatmap") {
            HistoryYearlyHeatmapCard(
                workouts = state.workouts,
                today = today,
                tz = tz
            )
        }
    }

    if (showExportSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val csvPayload = remember(state.workouts) {
            buildString {
                appendLine("date,title,volume_kg,exercises")
                state.workouts.forEach { w ->
                    val d = workoutLocalDate(w, tz)?.toString() ?: ""
                    appendLine("$d,\"${w.title.replace("\"", "'")}\",${round(w.totalVolume).toInt()},${w.exercises.size}")
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CSV выгрузка (${state.workouts.size})",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                GymInnerWell {
                    Text(
                        text = csvPayload.lines().take(12).joinToString("\n"),
                        fontSize = 12.sp,
                        color = TextSoft,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GymOutlineButton(
                        text = strings.cancel,
                        onClick = { showExportSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                    GymPrimaryButton(
                        text = "Скопировать CSV",
                        onClick = {
                            clipboard.setText(AnnotatedString(csvPayload))
                            PlatformBridge.showToast("CSV скопирован")
                            showExportSheet = false
                        },
                        modifier = Modifier.weight(1.4f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val pendingDelete = workoutPendingDelete
    if (pendingDelete != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { workoutPendingDelete = null },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Удалить «${pendingDelete.title.ifBlank { "Тренировку" }}»?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Запись о тренировке будет удалена из журнала и статистики.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GymOutlineButton(
                        text = strings.cancel,
                        onClick = { workoutPendingDelete = null },
                        modifier = Modifier.weight(1f)
                    )
                    GymOutlineButton(
                        text = strings.delete,
                        onClick = {
                            onAction(HistoryUiAction.OnDeleteWorkout(pendingDelete.id))
                            workoutPendingDelete = null
                        },
                        textColor = CrimsonRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletedWorkoutCard(
    workout: Workout,
    tz: TimeZone,
    isExpanded: Boolean,
    deleteLabel: String,
    onToggleExpand: () -> Unit,
    onRepeat: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val wDate = workoutLocalDate(workout, tz)
    val dateLabel = if (wDate != null) formatFullRuDate(wDate) else workout.startedAt.take(10)
    val displayTitle = workout.title.ifBlank { "Силовая тренировка" }
    val setsCount = remember(workout) {
        workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
            .let { if (it > 0) it else workout.completedSetsCount.coerceAtLeast(workout.exercises.sumOf { e -> e.sets.size }) }
    }
    val durationLabel = remember(workout.durationSeconds) {
        formatDurationCompact(workout.durationSeconds)
    }
    val volumeLabel = remember(workout.totalVolume) {
        formatTonsOrKg(workout.totalVolume)
    }

    GymCard {
        // Header: Bold Title + Date + Expand Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleExpand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isExpanded) AccentLimeSubtle else DarkSurfaceHigh)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (isExpanded) "Свернуть" else "Детали",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpanded) AccentLime else TextSecondary
                )
            }
        }

        // 3 Inline Heavy-Numeral Stats: Duration, Volume, Sets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkoutInlineStat(
                label = "Время",
                value = durationLabel,
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            WorkoutInlineStat(
                label = "Объём",
                value = volumeLabel,
                valueColor = AccentLime,
                modifier = Modifier.weight(1f)
            )
            WorkoutInlineStat(
                label = "Сеты",
                value = "$setsCount",
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Expandable Exercise Breakdown with ExerciseThumbnail(SM)
        AnimatedVisibility(visible = isExpanded) {
            GymInnerWell {
                workout.exercises.forEachIndexed { exIdx, ex ->
                    val exVol = ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
                        .let { if (it > 0.0) it else ex.sets.sumOf { s -> s.weightKg * s.reps } }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExerciseThumbnail(
                            exerciseName = ex.exerciseName.ifBlank { ex.exercise.name },
                            sourceId = ex.exercise.sourceId,
                            exerciseId = ex.exerciseId,
                            fallbackMuscle = ex.primaryMuscleGroup,
                            size = ExerciseThumbnailSize.SM
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = ex.exerciseName.ifBlank { ex.exercise.name },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ex.sets.size} подх. · ${formatSpacedInt(exVol)} кг",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    if (ex.sets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ex.sets.forEach { s ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${s.setNumber} · ${formatCleanKg(s.weightKg)} кг × ${s.reps}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSoft,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }
                    }

                    if (exIdx < workout.exercises.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GymDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Compact Repeat / Share / Delete Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymPrimaryButton(
                text = "Повторить",
                onClick = onRepeat,
                modifier = Modifier.weight(1.2f)
            )
            GymOutlineButton(
                text = "Поделиться",
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )
            GymOutlineButton(
                text = deleteLabel,
                onClick = onDelete,
                textColor = CrimsonRed
            )
        }
    }
}

@Composable
private fun WorkoutInlineStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = valueColor,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryMonthCalendarCard(
    monthStart: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate?,
    workoutDates: Set<LocalDate>,
    onPrevMonth: () -> Unit,
    onToday: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val monthTitle = formatMonthYearRu(monthStart.year, monthStart.monthNumber)
    val daysInMonth = remember(monthStart) {
        monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    }
    val leadingBlanks = remember(monthStart) {
        monthStart.dayOfWeek.ordinal // 0 = Monday .. 6 = Sunday
    }
    val totalSlots = remember(leadingBlanks, daysInMonth) {
        val raw = leadingBlanks + daysInMonth
        ((raw + 6) / 7) * 7
    }
    val weeks = remember(totalSlots, leadingBlanks, daysInMonth, monthStart) {
        (0 until totalSlots).chunked(7).map { weekIndices ->
            weekIndices.map { slot ->
                val dayNum = slot - leadingBlanks + 1
                if (dayNum in 1..daysInMonth) {
                    LocalDate(monthStart.year, monthStart.monthNumber, dayNum)
                } else null
            }
        }
    }

    GymCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarNavChip(text = "←", onClick = onPrevMonth)
                CalendarNavChip(text = "Сегодня", onClick = onToday)
                CalendarNavChip(text = "→", onClick = onNextMonth)
            }
        }

        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { wd ->
                Text(
                    text = wd,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).height(40.dp))
                        } else {
                            val hasWorkout = date in workoutDates
                            val isSelected = selectedDate == date
                            val isToday = date == today

                            val bg = when {
                                hasWorkout -> AccentLime
                                isSelected -> DarkSurfaceElevated
                                isToday -> DarkSurfaceElevated
                                else -> DarkSurfaceHigh
                            }
                            val textColor = when {
                                hasWorkout -> GymInkOnLight
                                isSelected || isToday -> TextPrimary
                                else -> TextSoft
                            }
                            val cellShape = RoundedCornerShape(12.dp)
                            val borderMod = when {
                                isSelected && hasWorkout -> Modifier.border(2.dp, TextPrimary, cellShape)
                                isSelected -> Modifier.border(2.dp, AccentLime, cellShape)
                                isToday && !hasWorkout -> Modifier.border(1.dp, BorderHighlight, cellShape)
                                else -> Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(cellShape)
                                    .background(bg)
                                    .then(borderMod)
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    fontSize = 14.sp,
                                    fontWeight = if (hasWorkout || isSelected || isToday) FontWeight.Black else FontWeight.Medium,
                                    color = textColor,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentLime)
                )
                Text(
                    text = if (selectedDate != null) {
                        "Выбрано: ${formatFullRuDate(selectedDate)}"
                    } else {
                        "Дни с тренировкой отмечены фиолетовым"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CalendarNavChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(DarkSurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun HistoryYearlyHeatmapCard(
    workouts: List<Workout>,
    today: LocalDate,
    tz: TimeZone
) {
    val workoutsCountByDate = remember(workouts) {
        val map = mutableMapOf<LocalDate, Int>()
        workouts.forEach { w ->
            val d = workoutLocalDate(w, tz)
            if (d != null) {
                map[d] = (map[d] ?: 0) + 1
            }
        }
        map
    }

    val sundayOffset = 6 - today.dayOfWeek.ordinal
    val gridEnd = remember(today) { today.plus(sundayOffset, DateTimeUnit.DAY) }
    val gridStart = remember(gridEnd) { gridEnd.minus(52 * 7 - 1, DateTimeUnit.DAY) }

    val weeksColumns = remember(gridStart) {
        (0 until 52).map { weekIdx ->
            (0 until 7).map { dayIdx ->
                gridStart.plus(weekIdx * 7 + dayIdx, DateTimeUnit.DAY)
            }
        }
    }

    GymCard {
        GymSectionTitle(title = "Год в тренировках")
        Text(
            text = "Активность за последние 52 недели",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weeksColumns.forEach { weekDays ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekDays.forEach { day ->
                        val count = workoutsCountByDate[day] ?: 0
                        val cellColor = when {
                            day > today -> GymWell
                            count >= 2 -> AccentLime
                            count == 1 -> AccentLime.copy(alpha = 0.72f)
                            else -> DarkSurfaceHigh
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(cellColor)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkSurfaceHigh)
                )
                Text(
                    text = "Отдых",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AccentLime)
                )
                Text(
                    text = "Тренировка",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentLime
                )
            }
        }
    }
}

private fun formatDurationCompact(durationSeconds: Long): String {
    if (durationSeconds <= 0L) return "45 мин"
    val totalMin = (durationSeconds / 60L).coerceAtLeast(1L)
    val hrs = totalMin / 60L
    val mins = totalMin % 60L
    return if (hrs > 0L) {
        "${hrs}ч ${mins}м"
    } else {
        "$totalMin мин"
    }
}

private fun formatWorkoutShareSummary(workout: Workout, tz: TimeZone): String {
    val d = workoutLocalDate(workout, tz)
    val dateStr = if (d != null) formatFullRuDate(d) else ""
    val title = workout.title.ifBlank { "Тренировка" }
    return buildString {
        appendLine("$title · $dateStr")
        appendLine("Объём: ${formatTonsOrKg(workout.totalVolume)} · Время: ${formatDurationCompact(workout.durationSeconds)}")
        workout.exercises.forEach { ex ->
            val name = ex.exerciseName.ifBlank { ex.exercise.name }
            appendLine("• $name (${ex.sets.size} подх.)")
        }
    }.trim()
}

private fun workoutLocalDate(workout: Workout, tz: TimeZone): LocalDate? {
    val epoch = workout.startTimeEpochMs
    if (epoch > 0L) {
        return Instant.fromEpochMilliseconds(epoch).toLocalDateTime(tz).date
    }
    return try {
        if (workout.startedAt.isNotBlank()) {
            Instant.parse(workout.startedAt).toLocalDateTime(tz).date
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun formatMonthYearRu(year: Int, monthNumber: Int): String {
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val name = months.getOrElse(monthNumber - 1) { "" }
    return "$name $year"
}

private fun formatFullRuDate(date: LocalDate): String {
    val monthsGenitive = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )
    val m = monthsGenitive.getOrElse(date.monthNumber - 1) { "" }
    return "${date.dayOfMonth} $m ${date.year}"
}

private fun formatTonsOrKg(kg: Double): String {
    return if (kg >= 1000.0) {
        val tons = round((kg / 1000.0) * 10.0) / 10.0
        "${tons.toString().replace('.', ',')} т"
    } else {
        "${round(kg).toInt()} кг"
    }
}

private fun formatCleanKg(weight: Double): String {
    val r = round(weight * 10.0) / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

private fun formatSpacedInt(value: Double): String {
    val raw = round(value).toLong().toString()
    if (raw.length <= 3) return raw
    val sb = StringBuilder()
    var count = 0
    for (i in raw.length - 1 downTo 0) {
        sb.append(raw[i])
        count++
        if (count % 3 == 0 && i > 0) sb.append(' ')
    }
    return sb.reverse().toString()
}
