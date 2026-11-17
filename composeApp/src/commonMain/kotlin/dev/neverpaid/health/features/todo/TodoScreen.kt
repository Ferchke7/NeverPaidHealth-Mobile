package dev.neverpaid.health.features.todo

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymEmptyCard
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymWhiteButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.DudaStrings
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.data.catalog.StandardHabitsCatalog
import dev.neverpaid.health.domain.model.AthleteHabit
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private enum class FocusMode {
    POMODORO,
    TIMER,
    STOPWATCH
}

private enum class FocusPhase {
    WORK,
    BREAK
}

@Composable
fun TodoRoute(
    viewModel: TodoViewModel,
    onStartWorkout: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TodoUiEffect.StartWorkoutSession -> onStartWorkout(effect.title)
                is TodoUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
                TodoUiEffect.TriggerHaptic -> PlatformBridge.vibrate()
            }
        }
    }

    TodoScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToProfile = onNavigateToProfile,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoScreen(
    state: TodoUiState,
    onAction: (TodoUiAction) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = strings.languageCode

    var showHabitsSection by rememberSaveable { mutableStateOf(false) }
    var habits by remember { mutableStateOf(StandardHabitsCatalog.standardPresets.take(4)) }
    var inlineEditingTask by remember { mutableStateOf<TodoTask?>(null) }

    // Inline Focus Timer State
    var focusMode by rememberSaveable { mutableStateOf(FocusMode.POMODORO) }
    var focusPhase by rememberSaveable { mutableStateOf(FocusPhase.WORK) }
    var activeTaskTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var timerSecondsLeft by rememberSaveable { mutableIntStateOf(25 * 60) }
    var stopwatchElapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var hasStartedSession by rememberSaveable { mutableStateOf(false) }
    var soundEnabled by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isTimerRunning, focusMode, focusPhase) {
        while (isTimerRunning) {
            delay(1000L)
            if (focusMode == FocusMode.STOPWATCH) {
                stopwatchElapsedSeconds += 1
            } else {
                if (timerSecondsLeft > 0) {
                    timerSecondsLeft -= 1
                } else {
                    isTimerRunning = false
                    if (soundEnabled) {
                        AthleteSoundManager.playIntervalGong()
                    }
                    PlatformBridge.showToast(
                        when (lang) {
                            "uz" -> "Fokus sessiyasi yakunlandi"
                            "en" -> "Focus session finished"
                            else -> "Сессия фокуса завершена"
                        }
                    )
                }
            }
        }
    }

    // Keep inline edit selection synchronized if task is updated/deleted
    LaunchedEffect(state.allTasks) {
        val currentEdit = inlineEditingTask
        if (currentEdit != null) {
            inlineEditingTask = state.allTasks.firstOrNull { it.id == currentEdit.id }
        }
    }

    val activeDate = state.selectedDate.ifBlank { currentTodayIso() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "page_header", contentType = "header") {
                GymPageHeader(
                    title = strings.todoTitle,
                    subtitle = strings.todoSubtitle
                )
            }

            if (state.isLoading && state.allTasks.isEmpty()) {
                item(key = "loading_card", contentType = "loading") {
                    GymLoadingCard(
                        title = strings.stateLoadingTitle,
                        hint = strings.stateLoadingHint
                    )
                }
            } else {
                // Date Navigation & 7-Day Strip (Figma 09)
                item(key = "date_strip", contentType = "date_strip") {
                    GymCard(contentPadding = PaddingValues(18.dp)) {
                        Text(
                            text = formatFullPlanDate(activeDate, lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymOutlineButton(
                                text = when (lang) {
                                    "uz" -> "← Oldinga"
                                    "en" -> "← Back"
                                    else -> "← Назад"
                                },
                                onClick = { onAction(TodoUiAction.OnSelectDate(shiftIsoDate(activeDate, -1))) }
                            )
                            GymOutlineButton(
                                text = when (lang) {
                                    "uz" -> "Bugun"
                                    "en" -> "Today"
                                    else -> "Сегодня"
                                },
                                onClick = { onAction(TodoUiAction.OnSelectDate(currentTodayIso())) }
                            )
                            GymOutlineButton(
                                text = when (lang) {
                                    "uz" -> "Keyingi →"
                                    "en" -> "Forward →"
                                    else -> "Вперёд →"
                                },
                                onClick = { onAction(TodoUiAction.OnSelectDate(shiftIsoDate(activeDate, 1))) }
                            )
                            GymPillTab(
                                text = when (lang) {
                                    "uz" -> "Odatlar (${habits.count { it.isCompletedOn(activeDate) }}/${habits.size})"
                                    "en" -> "Habits (${habits.count { it.isCompletedOn(activeDate) }}/${habits.size})"
                                    else -> "Привычки (${habits.count { it.isCompletedOn(activeDate) }}/${habits.size})"
                                },
                                selected = showHabitsSection,
                                onClick = { showHabitsSection = !showHabitsSection }
                            )
                        }

                        if (state.calendarDays.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.calendarDays.forEach { day ->
                                    val dayShape = RoundedCornerShape(10.dp)
                                    val bgColor = when {
                                        day.isSelected -> AccentLimeSubtle
                                        else -> DarkBackground
                                    }
                                    val borderColor = when {
                                        day.isSelected -> BorderHighlight
                                        day.isToday -> AccentLime.copy(alpha = 0.45f)
                                        else -> BorderSubtle
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 54.dp)
                                            .clip(dayShape)
                                            .background(bgColor)
                                            .border(1.dp, borderColor, dayShape)
                                            .clickable { onAction(TodoUiAction.OnSelectDate(day.dateString)) }
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.dayName.take(2),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (day.isSelected) AccentLime else TextMuted,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = day.dayNumber,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Default,
                                            color = if (day.isSelected) AccentLime else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Toggleable Habit Tracker section (preserves existing capability without cluttering Figma baseline)
                if (showHabitsSection) {
                    item(key = "habits_section", contentType = "habits") {
                        GymCard(contentPadding = PaddingValues(18.dp)) {
                            HabitTrackerView(
                                habits = habits,
                                currentDateIso = activeDate,
                                onToggleHabitToday = { habitId ->
                                    habits = habits.map { item ->
                                        if (item.id == habitId) {
                                            val updatedDates = if (activeDate in item.completedDates) {
                                                item.completedDates - activeDate
                                            } else {
                                                item.completedDates + activeDate
                                            }
                                            item.copy(completedDates = updatedDates)
                                        } else {
                                            item
                                        }
                                    }
                                },
                                onAddHabitPreset = { preset ->
                                    if (habits.none { it.id == preset.id }) {
                                        habits = habits + preset
                                    }
                                }
                            )
                        }
                    }
                }

                // Card 1: Checklist ("Небольшие шаги каждый день") — Figma 09
                item(key = "checklist_card", contentType = "checklist") {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Har kuni kichik qadamlar"
                                "en" -> "Small steps every day"
                                else -> "Небольшие шаги каждый день"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (lang) {
                                "uz" -> "${state.totalCount} ta vazifadan ${state.completedCount} tasi bajarildi"
                                "en" -> "${state.completedCount} of ${state.totalCount} tasks completed"
                                else -> "${state.completedCount} из ${state.totalCount} задач выполнена"
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val progress = if (state.totalCount > 0) {
                            (state.completedCount.toFloat() / state.totalCount.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        GymProgressBar(
                            progress = progress,
                            heightDp = 6,
                            color = AccentLime
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val categories = listOf(
                            "all" to when (lang) { "uz" -> "Hammasi"; "en" -> "All"; else -> "Все" },
                            "workout" to when (lang) { "uz" -> "Mashg'ulot"; "en" -> "Workout"; else -> "Тренировка" },
                            "nutrition" to when (lang) { "uz" -> "Oziqlanish"; "en" -> "Nutrition"; else -> "Питание" },
                            "supplement" to when (lang) { "uz" -> "Qo'shimcha"; "en" -> "Supps"; else -> "Добавки" },
                            "work" to when (lang) { "uz" -> "Ish"; "en" -> "Work"; else -> "Работа" },
                            "recovery" to when (lang) { "uz" -> "Tiklanish"; "en" -> "Recovery"; else -> "Восстановление" }
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it.first }) { (key, label) ->
                                GymPillTab(
                                    text = label,
                                    selected = state.selectedCategoryFilter == key,
                                    onClick = { onAction(TodoUiAction.OnSelectCategoryFilter(key)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        GymPrimaryButton(
                            text = "+ ${strings.todoAddTask}",
                            onClick = { onAction(TodoUiAction.OnOpenTaskModal(null)) },
                            fillWidth = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.tasksForSelectedDate.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Tanlangan kun uchun vazifalar yo'q"
                                        "en" -> "No tasks for selected day"
                                        else -> "На выбранный день задач нет"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Mashg'ulot maqsadi, kreatin yoki fokus sessiyasini qo'shing"
                                        "en" -> "Add a workout goal, supplement, or focus session"
                                        else -> "Добавьте тренировочную цель, приём добавок или сессию фокуса"
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            state.tasksForSelectedDate.forEachIndexed { index, task ->
                                if (index > 0) {
                                    GymDivider()
                                }
                                val isCompleted = task.isCompletedOn(activeDate)
                                TodoChecklistRow(
                                    task = task,
                                    isCompleted = isCompleted,
                                    lang = lang,
                                    onToggle = { onAction(TodoUiAction.OnToggleTask(task.id)) },
                                    onEdit = { inlineEditingTask = task },
                                    onMoveTomorrow = {
                                        val nextDate = shiftIsoDate(
                                            task.scheduledDate.ifBlank { activeDate },
                                            1
                                        )
                                        onAction(
                                            TodoUiAction.OnSaveTask(
                                                request = SaveTodoRequestDto(
                                                    title = task.title,
                                                    notes = task.notes,
                                                    category = task.category,
                                                    priority = task.priority,
                                                    scheduledDate = nextDate,
                                                    scheduledTime = task.startTime,
                                                    repeatDaily = task.repeatDaily,
                                                    durationMinutes = task.durationMinutes
                                                ),
                                                editingId = task.id
                                            )
                                        )
                                        PlatformBridge.showToast(
                                            when (lang) {
                                                "uz" -> "Ertaga ko'chirildi ($nextDate)"
                                                "en" -> "Moved to next day ($nextDate)"
                                                else -> "Перенесено на $nextDate"
                                            }
                                        )
                                    },
                                    onStartFocus = {
                                        activeTaskTitle = task.title
                                        focusMode = FocusMode.POMODORO
                                        focusPhase = FocusPhase.WORK
                                        timerSecondsLeft = task.pomodoroMinutes * 60
                                        hasStartedSession = true
                                        isTimerRunning = true
                                        onAction(TodoUiAction.OnStartFocusTimer(task))
                                    }
                                )
                            }
                        }
                    }
                }

                // Card 2: Focus Timer ("Время для фокуса") — Figma 09
                item(key = "focus_timer_card", contentType = "focus_timer") {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Fokus vaqti"
                                "en" -> "Time for focus"
                                else -> "Время для фокуса"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPillTab(
                                text = when (lang) { "uz" -> "Pomodoro"; "en" -> "Pomodoro"; else -> "Помодоро" },
                                selected = focusMode == FocusMode.POMODORO,
                                onClick = {
                                    focusMode = FocusMode.POMODORO
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    timerSecondsLeft = if (focusPhase == FocusPhase.WORK) 25 * 60 else 5 * 60
                                }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Taymer"; "en" -> "Timer"; else -> "Таймер" },
                                selected = focusMode == FocusMode.TIMER,
                                onClick = {
                                    focusMode = FocusMode.TIMER
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    timerSecondsLeft = 15 * 60
                                }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Sekundomer"; "en" -> "Stopwatch"; else -> "Секундомер" },
                                selected = focusMode == FocusMode.STOPWATCH,
                                onClick = {
                                    focusMode = FocusMode.STOPWATCH
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    stopwatchElapsedSeconds = 0
                                }
                            )
                        }

                        if (focusMode == FocusMode.POMODORO) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GymPillTab(
                                    text = when (lang) { "uz" -> "Ish"; "en" -> "Work"; else -> "Работа" },
                                    selected = focusPhase == FocusPhase.WORK,
                                    onClick = {
                                        focusPhase = FocusPhase.WORK
                                        isTimerRunning = false
                                        timerSecondsLeft = 25 * 60
                                    }
                                )
                                GymPillTab(
                                    text = when (lang) { "uz" -> "Tanaffus"; "en" -> "Break"; else -> "Перерыв" },
                                    selected = focusPhase == FocusPhase.BREAK,
                                    onClick = {
                                        focusPhase = FocusPhase.BREAK
                                        isTimerRunning = false
                                        timerSecondsLeft = 5 * 60
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val displayedSeconds = if (focusMode == FocusMode.STOPWATCH) {
                            stopwatchElapsedSeconds
                        } else {
                            timerSecondsLeft
                        }
                        Text(
                            text = formatMmSs(displayedSeconds),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Default,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val statusText = when {
                            activeTaskTitle != null -> activeTaskTitle!!
                            focusMode == FocusMode.STOPWATCH -> when (lang) {
                                "uz" -> "Sekundomer"
                                "en" -> "Stopwatch"
                                else -> "Секундомер"
                            }
                            focusPhase == FocusPhase.WORK -> when (lang) {
                                "uz" -> "Ish sessiyasi (25 min)"
                                "en" -> "Work session (25 min)"
                                else -> "Рабочая сессия (25 мин)"
                            }
                            else -> when (lang) {
                                "uz" -> "Tanaffus (5 min)"
                                "en" -> "Break (5 min)"
                                else -> "Перерыв (5 мин)"
                            }
                        }
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymWhiteButton(
                                text = when {
                                    isTimerRunning -> when (lang) { "uz" -> "Faol"; "en" -> "Running"; else -> "Идёт" }
                                    hasStartedSession -> when (lang) { "uz" -> "Davom etish"; "en" -> "Resume"; else -> "Продолжить" }
                                    else -> strings.todoPomodoroStart
                                },
                                onClick = {
                                    hasStartedSession = true
                                    isTimerRunning = true
                                },
                                enabled = !isTimerRunning
                            )
                            GymOutlineButton(
                                text = strings.todoPomodoroPause,
                                onClick = { isTimerRunning = false },
                                enabled = isTimerRunning
                            )
                            GymOutlineButton(
                                text = strings.todoPomodoroReset,
                                onClick = {
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    activeTaskTitle = null
                                    stopwatchElapsedSeconds = 0
                                    timerSecondsLeft = when (focusMode) {
                                        FocusMode.POMODORO -> if (focusPhase == FocusPhase.WORK) 25 * 60 else 5 * 60
                                        FocusMode.TIMER -> 15 * 60
                                        FocusMode.STOPWATCH -> 0
                                    }
                                }
                            )
                            GymOutlineButton(
                                text = when {
                                    lang == "uz" -> if (soundEnabled) "Ovoz: yoq." else "Ovoz: o'ch."
                                    lang == "en" -> if (soundEnabled) "Sound: on" else "Sound: off"
                                    else -> if (soundEnabled) "Звук: вкл." else "Звук: выкл."
                                },
                                onClick = {
                                    soundEnabled = !soundEnabled
                                    if (soundEnabled) {
                                        AthleteSoundManager.playIntervalGong()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when (lang) {
                                "uz" -> "Yakunlangan sessiyalar faoliyat jurnalida ko'rinadi."
                                "en" -> "Completed sessions will appear in the activity log."
                                else -> "Завершённые сессии появятся в журнале."
                            },
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                // Card 3: Inline Task Editor ("Редактирование задачи") — Figma 09
                inlineEditingTask?.let { editing ->
                    item(key = "inline_task_editor_${editing.id}", contentType = "inline_editor") {
                        InlineTaskEditorCard(
                            task = editing,
                            strings = strings,
                            onSave = { request ->
                                onAction(TodoUiAction.OnSaveTask(request, editing.id))
                                inlineEditingTask = null
                            },
                            onDelete = {
                                onAction(TodoUiAction.OnDeleteTask(editing.id))
                                inlineEditingTask = null
                            },
                            onCancel = { inlineEditingTask = null }
                        )
                    }
                }

                // Card 4: Activity Log ("Журнал активности") — Figma 09
                item(key = "activity_log_card", contentType = "activity_log") {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Faoliyat jurnali"
                                "en" -> "Activity log"
                                else -> "Журнал активности"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val completedForDay = state.tasksForSelectedDate.filter { it.isCompletedOn(activeDate) }

                        if (state.activityLogs.isEmpty() && completedForDay.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Hozircha yozuvlar yo'q"
                                        "en" -> "No activity entries yet"
                                        else -> "Пока нет записей активности"
                                    },
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            state.activityLogs.take(8).forEachIndexed { idx, log ->
                                if (idx > 0) GymDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val timeLabel = log.startedAt
                                            .substringAfter("T", "")
                                            .take(5)
                                            .ifBlank { log.loggedDate }
                                        Text(
                                            text = timeLabel.ifBlank { "●" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Default,
                                            color = AccentLime
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = log.taskTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (log.durationMinutes > 0) "${log.durationMinutes} мин" else log.actionType,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Default,
                                        color = TextSecondary
                                    )
                                }
                            }

                            completedForDay.take(5).forEachIndexed { idx, task ->
                                if (state.activityLogs.isNotEmpty() || idx > 0) {
                                    GymDivider()
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = task.scheduledTime.ifBlank { "✓" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Default,
                                            color = AccentLime
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = task.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (lang) {
                                            "uz" -> "Bajarildi"
                                            "en" -> "Done"
                                            else -> "Выполнена"
                                        },
                                        fontSize = 12.sp,
                                        color = AccentLime
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (state.isTaskModalOpen) {
        CreateOrEditTaskBottomSheet(
            existingTask = state.editingTask,
            selectedDate = activeDate,
            strings = strings,
            onDismiss = { onAction(TodoUiAction.OnCloseTaskModal) },
            onSave = { request ->
                onAction(TodoUiAction.OnSaveTask(request, state.editingTask?.id))
            },
            onDelete = state.editingTask?.let { task ->
                {
                    onAction(TodoUiAction.OnDeleteTask(task.id))
                    onAction(TodoUiAction.OnCloseTaskModal)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodoChecklistRow(
    task: TodoTask,
    isCompleted: Boolean,
    lang: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMoveTomorrow: () -> Unit,
    onStartFocus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.CenterStart
            ) {
                val boxShape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(boxShape)
                        .background(if (isCompleted) TextPrimary else DarkBackground)
                        .border(
                            width = 1.dp,
                            color = if (isCompleted) TextPrimary else BorderSubtle,
                            shape = boxShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GymInkOnLight,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) TextSecondary else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(3.dp))
                val statusLabel = if (isCompleted) {
                    when (lang) { "uz" -> "bajarildi"; "en" -> "completed"; else -> "выполнена" }
                } else {
                    when (lang) { "uz" -> "bajarilmagan"; "en" -> "pending"; else -> "не выполнена" }
                }
                val catLabel = localizeTodoCategory(task.category, lang)
                Text(
                    text = "$catLabel · ${task.durationMinutes} мин · $statusLabel",
                    fontSize = 12.sp,
                    color = if (isCompleted) AccentLime else TextSecondary
                )
                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.notes,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.padding(start = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymOutlineButton(
                text = when (lang) { "uz" -> "Tahrirlash"; "en" -> "Edit"; else -> "Изменить" },
                onClick = onEdit
            )
            GymOutlineButton(
                text = when (lang) { "uz" -> "Ko'chirish"; "en" -> "Move +1d"; else -> "Перенести" },
                onClick = onMoveTomorrow
            )
            GymOutlineButton(
                text = when (lang) { "uz" -> "Jurnal / Fokus"; "en" -> "Focus"; else -> "Журнал" },
                onClick = onStartFocus
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineTaskEditorCard(
    task: TodoTask,
    strings: DudaStrings,
    onSave: (SaveTodoRequestDto) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val lang = strings.languageCode
    var title by remember(task.id) { mutableStateOf(task.title) }
    var date by remember(task.id) { mutableStateOf(task.scheduledDate) }
    var category by remember(task.id) { mutableStateOf(task.category) }
    var durationText by remember(task.id) { mutableStateOf(task.durationMinutes.toString()) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }

    GymCard(contentPadding = PaddingValues(20.dp)) {
        Text(
            text = when (lang) {
                "uz" -> "Vazifani tahrirlash"
                "en" -> "Edit task"
                else -> "Редактирование задачи"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        GymLabeledField(
            label = strings.todoTaskTitleLabel,
            value = title,
            onValueChange = { title = it },
            placeholder = strings.todoTaskTitleLabel
        )

        Spacer(modifier = Modifier.height(12.dp))

        GymLabeledField(
            label = when (lang) { "uz" -> "Sana"; "en" -> "Date"; else -> "Дата" },
            value = date,
            onValueChange = { date = it },
            placeholder = "YYYY-MM-DD"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.todoCategoryLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("workout", "nutrition", "supplement", "work", "recovery").forEach { key ->
                GymPillTab(
                    text = localizeTodoCategory(key, lang),
                    selected = category == key,
                    onClick = { category = key }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GymLabeledField(
            label = when (lang) {
                "uz" -> "Davomiyligi, min"
                "en" -> "Duration, min"
                else -> "Длительность, мин"
            },
            value = durationText,
            onValueChange = { input -> durationText = input.filter { it.isDigit() } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = "30"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = strings.todoPriorityLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("low", "medium", "high").forEach { p ->
                GymPillTab(
                    text = localizePriority(p, lang),
                    selected = priority == p,
                    onClick = { priority = p }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymPrimaryButton(
                text = strings.save,
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            SaveTodoRequestDto(
                                title = title.trim(),
                                notes = task.notes,
                                category = category,
                                priority = priority,
                                scheduledDate = date.trim().ifBlank { task.scheduledDate },
                                scheduledTime = task.startTime,
                                repeatDaily = task.repeatDaily,
                                durationMinutes = durationText.toIntOrNull()?.coerceAtLeast(1) ?: task.durationMinutes
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            )
            GymOutlineButton(
                text = strings.delete,
                onClick = onDelete,
                textColor = CrimsonRed,
                borderColor = CrimsonRed.copy(alpha = 0.5f)
            )
            GymOutlineButton(
                text = strings.cancel,
                onClick = onCancel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateOrEditTaskBottomSheet(
    existingTask: TodoTask?,
    selectedDate: String,
    strings: DudaStrings,
    onDismiss: () -> Unit,
    onSave: (SaveTodoRequestDto) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = strings.languageCode
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var scheduledDate by remember { mutableStateOf(existingTask?.scheduledDate?.ifBlank { selectedDate } ?: selectedDate) }
    var scheduledTime by remember { mutableStateOf(existingTask?.scheduledTime ?: "") }
    var durationText by remember { mutableStateOf((existingTask?.durationMinutes ?: 30).toString()) }
    var category by remember { mutableStateOf(existingTask?.category ?: "workout") }
    var priority by remember { mutableStateOf(existingTask?.priority ?: "medium") }
    var repeatDaily by remember { mutableStateOf(existingTask?.repeatDaily ?: false) }

    val quickTemplates = listOf(
        Triple(
            when (lang) { "uz" -> "Kreatin 5g ichish"; "en" -> "Take 5g Creatine"; else -> "Приём креатина 5г" },
            when (lang) { "uz" -> "Mashg'ulotdan keyin suv bilan"; "en" -> "With water post-workout"; else -> "С водой после тренировки" },
            "supplement"
        ),
        Triple(
            when (lang) { "uz" -> "3L suv ichish"; "en" -> "Drink 3L Water"; else -> "Выпить 3 литра воды" },
            when (lang) { "uz" -> "Kun davomida gidratatsiya"; "en" -> "Hydration throughout the day"; else -> "Гидратация в течение дня" },
            "nutrition"
        ),
        Triple(
            when (lang) { "uz" -> "20 min cho'zilish"; "en" -> "20 min Mobility"; else -> "Растяжка и МФР 20 мин" },
            when (lang) { "uz" -> "Yelka va bel"; "en" -> "Shoulders & Lower back"; else -> "Плечи и поясница" },
            "recovery"
        ),
        Triple(
            when (lang) { "uz" -> "8 soat uyqu"; "en" -> "8 Hours Sleep"; else -> "Сон 8+ часов" },
            when (lang) { "uz" -> "23:00 gacha uxlash"; "en" -> "Sleep before 23:00"; else -> "Отбой до 23:00" },
            "recovery"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTask != null) {
                        when (lang) {
                            "uz" -> "Vazifani tahrirlash"
                            "en" -> "Edit Task"
                            else -> "Редактировать задачу"
                        }
                    } else {
                        strings.todoNewTaskTitle
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                }
            }

            if (existingTask == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (lang) {
                        "uz" -> "Tezkor shablonlar"
                        "en" -> "Quick templates"
                        else -> "Быстрые шаблоны"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickTemplates, key = { it.first }) { (tplTitle, tplNotes, tplCat) ->
                        GymPillTab(
                            text = "+ $tplTitle",
                            selected = false,
                            onClick = {
                                title = tplTitle
                                notes = tplNotes
                                category = tplCat
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GymLabeledField(
                label = strings.todoTaskTitleLabel,
                value = title,
                onValueChange = { title = it },
                placeholder = strings.todoTaskTitleLabel
            )

            Spacer(modifier = Modifier.height(10.dp))

            GymLabeledField(
                label = strings.todoTargetLabel,
                value = notes,
                onValueChange = { notes = it },
                placeholder = strings.todoTargetLabel
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GymLabeledField(
                    label = when (lang) { "uz" -> "Sana"; "en" -> "Date"; else -> "Дата" },
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
                GymLabeledField(
                    label = when (lang) { "uz" -> "Vaqt"; "en" -> "Time"; else -> "Время" },
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    placeholder = "18:00",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GymLabeledField(
                label = when (lang) {
                    "uz" -> "Davomiyligi, min"
                    "en" -> "Duration, min"
                    else -> "Длительность, мин"
                },
                value = durationText,
                onValueChange = { input -> durationText = input.filter { it.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "30"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.todoCategoryLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("workout", "nutrition", "supplement", "work", "recovery").forEach { key ->
                    GymPillTab(
                        text = localizeTodoCategory(key, lang),
                        selected = category == key,
                        onClick = { category = key }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.todoPriorityLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low", "medium", "high").forEach { key ->
                    GymPillTab(
                        text = localizePriority(key, lang),
                        selected = priority == key,
                        onClick = { priority = key }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.todoRecurringDaily,
                    fontSize = 14.sp,
                    color = TextSoft
                )
                Switch(
                    checked = repeatDaily,
                    onCheckedChange = { repeatDaily = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = AccentLime,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceHigh
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onDelete != null) {
                    GymOutlineButton(
                        text = strings.delete,
                        onClick = onDelete,
                        textColor = CrimsonRed,
                        borderColor = CrimsonRed.copy(alpha = 0.5f)
                    )
                }
                GymOutlineButton(
                    text = strings.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
                GymPrimaryButton(
                    text = strings.save,
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                SaveTodoRequestDto(
                                    title = title.trim(),
                                    notes = notes.trim(),
                                    category = category,
                                    priority = priority,
                                    scheduledDate = scheduledDate.trim().ifBlank { selectedDate },
                                    scheduledTime = scheduledTime.trim().ifBlank { null },
                                    repeatDaily = repeatDaily,
                                    durationMinutes = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                                )
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
            }
        }
    }
}

private fun localizeTodoCategory(category: String, lang: String): String = when (category.lowercase()) {
    "workout" -> when (lang) { "uz" -> "Mashg'ulot"; "en" -> "Workout"; else -> "Тренировка" }
    "nutrition" -> when (lang) { "uz" -> "Oziqlanish"; "en" -> "Nutrition"; else -> "Питание" }
    "supplement" -> when (lang) { "uz" -> "Qo'shimcha"; "en" -> "Supps"; else -> "Добавки" }
    "cardio" -> when (lang) { "uz" -> "Kardio"; "en" -> "Cardio"; else -> "Кардио" }
    "recovery" -> when (lang) { "uz" -> "Tiklanish"; "en" -> "Recovery"; else -> "Восстановление" }
    else -> when (lang) { "uz" -> "Ish"; "en" -> "Work"; else -> "Работа" }
}

private fun localizePriority(priority: String, lang: String): String = when (priority.lowercase()) {
    "high" -> when (lang) { "uz" -> "Yuqori"; "en" -> "High"; else -> "Высокий" }
    "low" -> when (lang) { "uz" -> "Past"; "en" -> "Low"; else -> "Низкий" }
    else -> when (lang) { "uz" -> "O'rta"; "en" -> "Medium"; else -> "Средний" }
}

private fun formatMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun currentTodayIso(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
    val base = runCatching { LocalDate.parse(isoDate) }
        .getOrElse { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    return if (deltaDays >= 0) {
        base.plus(deltaDays, DateTimeUnit.DAY).toString()
    } else {
        base.minus(-deltaDays, DateTimeUnit.DAY).toString()
    }
}

private fun formatFullPlanDate(isoDate: String, lang: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val weekday = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> when (lang) { "uz" -> "Dushanba"; "en" -> "Monday"; else -> "Понедельник" }
        DayOfWeek.TUESDAY -> when (lang) { "uz" -> "Seshanba"; "en" -> "Tuesday"; else -> "Вторник" }
        DayOfWeek.WEDNESDAY -> when (lang) { "uz" -> "Chorshanba"; "en" -> "Wednesday"; else -> "Среда" }
        DayOfWeek.THURSDAY -> when (lang) { "uz" -> "Payshanba"; "en" -> "Thursday"; else -> "Четверг" }
        DayOfWeek.FRIDAY -> when (lang) { "uz" -> "Juma"; "en" -> "Friday"; else -> "Пятница" }
        DayOfWeek.SATURDAY -> when (lang) { "uz" -> "Shanba"; "en" -> "Saturday"; else -> "Суббота" }
        DayOfWeek.SUNDAY -> when (lang) { "uz" -> "Yakshanba"; "en" -> "Sunday"; else -> "Воскресенье" }
        else -> ""
    }
    val month = when (date.monthNumber) {
        1 -> when (lang) { "uz" -> "yanvar"; "en" -> "January"; else -> "января" }
        2 -> when (lang) { "uz" -> "fevral"; "en" -> "February"; else -> "февраля" }
        3 -> when (lang) { "uz" -> "mart"; "en" -> "March"; else -> "марта" }
        4 -> when (lang) { "uz" -> "aprel"; "en" -> "April"; else -> "апреля" }
        5 -> when (lang) { "uz" -> "may"; "en" -> "May"; else -> "мая" }
        6 -> when (lang) { "uz" -> "iyun"; "en" -> "June"; else -> "июня" }
        7 -> when (lang) { "uz" -> "iyul"; "en" -> "July"; else -> "июля" }
        8 -> when (lang) { "uz" -> "avgust"; "en" -> "August"; else -> "августа" }
        9 -> when (lang) { "uz" -> "sentyabr"; "en" -> "September"; else -> "сентября" }
        10 -> when (lang) { "uz" -> "oktyabr"; "en" -> "October"; else -> "октября" }
        11 -> when (lang) { "uz" -> "noyabr"; "en" -> "November"; else -> "ноября" }
        else -> when (lang) { "uz" -> "dekabr"; "en" -> "December"; else -> "декабря" }
    }
    return "$weekday, ${date.dayOfMonth} $month ${date.year}"
}
