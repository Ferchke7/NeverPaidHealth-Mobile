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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.ObsidianCard
import dev.neverpaid.health.core.designsystem.ObsidianDivider
import dev.neverpaid.health.core.designsystem.ObsidianEmptyCard
import dev.neverpaid.health.core.designsystem.ObsidianLoadingCard
import dev.neverpaid.health.core.designsystem.ObsidianOutlineButton
import dev.neverpaid.health.core.designsystem.ObsidianPageHeader
import dev.neverpaid.health.core.designsystem.ObsidianPrimaryButton
import dev.neverpaid.health.core.designsystem.ObsidianInnerWell
import dev.neverpaid.health.core.designsystem.ObsidianWhiteButton
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                ObsidianPageHeader(
                    title = strings.historyTitle,
                    subtitle = "Тренировки, календарь и регулярность"
                )
            }
            item(key = "loading") {
                ObsidianLoadingCard(
                    title = strings.historyTitle,
                    hint = strings.stateLoadingHint
                )
            }
        }
        return
    }

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
        item(key = "header", contentType = "header") {
            ObsidianPageHeader(
                title = strings.historyTitle,
                subtitle = "Тренировки, календарь и регулярность"
            )
        }

        // Card 1: Monthly KPI & Export
        item(key = "kpi_card", contentType = "kpi") {
            ObsidianCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ТРЕНИРОВОК",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${monthWorkouts.size}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ОБЩИЙ ОБЪЁМ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatTonsOrKg(monthTotalVolumeKg),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ObsidianOutlineButton(
                        text = monthHeaderLabel,
                        onClick = {
                            displayedMonth = LocalDate(today.year, today.monthNumber, 1)
                            selectedDate = today
                        }
                    )
                    ObsidianOutlineButton(
                        text = "Экспорт",
                        onClick = { showExportSheet = true }
                    )
                }
            }
        }

        // Card 2: Monthly Calendar
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

        // Card 3: Chronological Divided Workout Log
        item(key = "sessions_card", contentType = "sessions") {
            ObsidianCard {
                Text(
                    text = "Тренировки",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.workouts.isEmpty()) {
                    ObsidianInnerWell {
                        Text(
                            text = "История тренировок пока пуста",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Завершите первую тренировку, чтобы видеть календарь, сеты и общий тоннаж.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else if (monthWorkouts.isEmpty()) {
                    ObsidianInnerWell {
                        Text(
                            text = "Нет тренировок за $monthHeaderLabel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Всего в архиве: ${state.workouts.size}. Переключите месяц или откройте последнюю тренировку.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ObsidianOutlineButton(
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
                } else {
                    shownWorkouts.forEachIndexed { idx, workout ->
                        val isExpanded = state.expandedWorkoutId == workout.id
                        val wDate = workoutLocalDate(workout, tz)
                        val titleDate = if (wDate != null) formatFullRuDate(wDate) else workout.title
                        val volStr = formatTonsOrKg(workout.totalVolume)
                        val statusSub = if (isExpanded) {
                            "$volStr · детали открыты"
                        } else {
                            "$volStr · завершена"
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = titleDate,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "$statusSub · ${workout.title}",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            ObsidianOutlineButton(
                                text = if (isExpanded) "Свернуть детали" else "Показать детали",
                                onClick = { onAction(HistoryUiAction.OnToggleWorkoutExpand(workout.id)) }
                            )

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    ObsidianInnerWell {
                                        Text(
                                            text = workout.title.ifBlank { "Детализация тренировки" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EmeraldNeon
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        workout.exercises.forEachIndexed { exIdx, ex ->
                                            val exVol = ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
                                                .let { if (it > 0.0) it else ex.sets.sumOf { s -> s.weightKg * s.reps } }
                                            Text(
                                                text = ex.exerciseName.ifBlank { ex.exercise.name },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            ex.sets.forEach { s ->
                                                Text(
                                                    text = "${s.setNumber} · ${formatCleanKg(s.weightKg)} кг × ${s.reps}",
                                                    fontSize = 13.sp,
                                                    color = TextSoft,
                                                    modifier = Modifier.padding(vertical = 1.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Итого · ${formatSpacedInt(exVol)} кг",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            if (exIdx < workout.exercises.lastIndex) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                ObsidianDivider()
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        ObsidianDivider()
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Общий объём · ${formatSpacedInt(workout.totalVolume)} кг",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ObsidianPrimaryButton(
                                            text = "Повторить",
                                            onClick = { onAction(HistoryUiAction.OnRepeatWorkout(workout)) }
                                        )
                                        ObsidianOutlineButton(
                                            text = strings.delete,
                                            onClick = { workoutPendingDelete = workout }
                                        )
                                    }
                                }
                            }
                        }

                        if (idx < shownWorkouts.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                            ObsidianDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (monthWorkouts.size > shownWorkouts.size) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ObsidianOutlineButton(
                            text = "Загрузить ещё",
                            onClick = { visibleLimit += 6 }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${monthWorkouts.size} тренировки за выбранный месяц",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Card 4: 52-week Consistency Heatmap
        item(key = "yearly_heatmap_card", contentType = "heatmap") {
            HistoryYearlyHeatmapCard(
                workouts = state.workouts,
                today = today,
                tz = tz
            )
        }
    }

    if (showExportSheet) {
        val clipboard = LocalClipboardManager.current
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
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "ЭКСПОРТ АРХИВА",
                    fontSize = 11.sp,
                    color = EmeraldNeon,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CSV выгрузка (${state.workouts.size} тренировок)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                ObsidianInnerWell {
                    Text(
                        text = csvPayload.lines().take(12).joinToString("\n"),
                        fontSize = 12.sp,
                        color = TextSoft
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ObsidianPrimaryButton(
                        text = "Скопировать CSV",
                        onClick = {
                            clipboard.setText(AnnotatedString(csvPayload))
                            PlatformBridge.showToast("CSV скопирован")
                            showExportSheet = false
                        }
                    )
                    ObsidianOutlineButton(
                        text = strings.cancel,
                        onClick = { showExportSheet = false }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
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
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "УДАЛЕНИЕ ЗАПИСИ",
                    fontSize = 11.sp,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Удалить «${pendingDelete.title}»?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Тренировка будет удалена из журнала и статистики.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                ObsidianDivider()
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ObsidianOutlineButton(
                        text = strings.cancel,
                        onClick = { workoutPendingDelete = null }
                    )
                    ObsidianWhiteButton(
                        text = strings.delete,
                        onClick = {
                            onAction(HistoryUiAction.OnDeleteWorkout(pendingDelete.id))
                            workoutPendingDelete = null
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    ObsidianCard {
        Text(
            text = monthTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianOutlineButton(text = "←", onClick = onPrevMonth)
            ObsidianOutlineButton(text = "Сегодня", onClick = onToday)
            ObsidianOutlineButton(text = "→", onClick = onNextMonth)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { wd ->
                Text(
                    text = wd,
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

                            val bg = if (hasWorkout) EmeraldNeon else DarkBackground
                            val borderColor = when {
                                isSelected -> TextPrimary
                                isToday -> EmeraldNeon
                                hasWorkout -> EmeraldNeon
                                else -> BorderSubtle
                            }
                            val borderWidth = if (isSelected) 2.dp else 1.dp
                            val textColor = if (hasWorkout) Color.Black else TextSoft

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    fontSize = 14.sp,
                                    fontWeight = if (hasWorkout || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldNeon)
                )
                Text(
                    text = "Тренировка",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (selectedDate != null) {
                Text(
                    text = "${selectedDate.dayOfMonth} — выбранная дата",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
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

    // Align grid end to the end of current week (Sunday), covering 52 weeks (364 days)
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

    ObsidianCard {
        Text(
            text = "Год в тренировках",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "52 недели · прокрутка",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

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
                            day > today -> DarkBackground
                            count >= 2 -> EmeraldNeon
                            count == 1 -> EmeraldNeon.copy(alpha = 0.72f)
                            else -> DarkSurfaceHigh
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Нет записей",
                fontSize = 12.sp,
                color = TextMuted
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldNeon)
                )
                Text(
                    text = "Есть тренировка",
                    fontSize = 12.sp,
                    color = EmeraldNeon
                )
            }
        }
    }
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
